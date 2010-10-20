package org.ruby_http_parser;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;

import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;

import java.nio.ByteBuffer;
import http_parser.*;
import http_parser.lolevel.ParserSettings;
import http_parser.lolevel.HTTPCallback;
import http_parser.lolevel.HTTPDataCallback;

public class RubyHttpParser extends RubyObject {

  public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
    public IRubyObject allocate(Ruby runtime, RubyClass klass) {
      return new RubyHttpParser(runtime, klass);
    }
  };

  byte[] fetchBytes (ByteBuffer b, int pos, int len) {
    byte[] by = new byte[len];
    int saved = b.position();
    b.position(pos);
    b.get(by);
    b.position(saved);
    return by;
  }

  HTTPDataCallback dataCallback(final String key, final RubyHttpParser obj) {
    return new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        ThreadContext context = obj.getEnv().getRuntime().getCurrentContext();
        IRubyObject val = obj.getEnv().op_aref(context, (IRubyObject)runtime.newString(key));
        byte[] data = fetchBytes(buf, pos, len);

        if (val.isNil())
          obj.getEnv().op_aset(context, (IRubyObject)runtime.newString(key), runtime.newString(new String(data)));
        else
          ((RubyString)val).cat(data);

        return 0;
      }
    };
  }

  private Ruby runtime;
  private HTTPParser parser;
  private ParserSettings settings;

  private RubyClass eParseError;

  private RubyHash _env;
  private IRubyObject _on_message_complete;
  private IRubyObject _on_headers_complete;
  private IRubyObject _on_body;

  private String _current_header;
  private String _last_header;

  public RubyHttpParser(final Ruby runtime, RubyClass clazz) {
    super(runtime,clazz);

    this.runtime = runtime;
    this.parser = new HTTPParser();

    this.settings = new ParserSettings();

    this.settings.on_url          = dataCallback("REQUEST_URI",  this);
    this.settings.on_path         = dataCallback("PATH_INFO",    this);
    this.settings.on_fragment     = dataCallback("FRAGMENT",     this);
    this.settings.on_query_string = dataCallback("QUERY_STRING", this);

    this.settings.on_header_field = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);

        if (_current_header == null)
          _current_header = new String(data);
        else
          _current_header.concat(new String(data));

        return 0;
      }
    };
    this.settings.on_header_value = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);

        if (_current_header != null) {
          _last_header = "HTTP_".concat(_current_header).toUpperCase().replace('-','_');
          _current_header = null;
        }

        IRubyObject key = (IRubyObject)runtime.newString(_last_header);

        ThreadContext context = _env.getRuntime().getCurrentContext();
        IRubyObject val = _env.op_aref(context, key);

        if (val.isNil())
          _env.op_aset(context, key, runtime.newString(new String(data)));
        else
          ((RubyString)val).cat(data);

        return 0;
      }
    };

    this.settings.on_message_begin = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        _env = new RubyHash(runtime);
        return 0;
      }
    };
    this.settings.on_message_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        if (_on_message_complete != null) {
          ThreadContext context = _on_message_complete.getRuntime().getCurrentContext();
          _on_message_complete.callMethod(context, "call");
        }
        return 0;
      }
    };
    this.settings.on_headers_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        if (_on_headers_complete != null) {
          ThreadContext context = _on_headers_complete.getRuntime().getCurrentContext();
          _on_headers_complete.callMethod(context, "call", _env);
        }
        return 0;
      }
    };
    this.settings.on_body = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        if (_on_body != null) {
          byte[] data = fetchBytes(buf, pos, len);
          ThreadContext context = _on_body.getRuntime().getCurrentContext();
          _on_body.callMethod(context, "call", _on_body.getRuntime().newString(new String(data)));
        }
        return 0;
      }
    };

    this.eParseError = (RubyClass)runtime.getModule("HTTP").getConstant("ParseError");

    this._env = null;
    this._on_message_complete = null;
    this._on_headers_complete = null;
    this._on_body = null;
  }

  public RubyHash getEnv() {
    return _env;
  }

  @JRubyMethod(name = "on_message_complete=")
  public IRubyObject set_on_message_complete(IRubyObject cb) {
    _on_message_complete = cb;
    return cb;
  }

  @JRubyMethod(name = "on_headers_complete=")
  public IRubyObject set_on_headers_complete(IRubyObject cb) {
    _on_headers_complete = cb;
    return cb;
  }

  @JRubyMethod(name = "on_body=")
  public IRubyObject set_on_body(IRubyObject cb) {
    _on_body = cb;
    return cb;
  }

  @JRubyMethod(name = "<<")
  public IRubyObject execute(IRubyObject data) {
    RubyString str = (RubyString)data;
    ByteBuffer buf = ByteBuffer.wrap(str.getBytes());

    try {
      this.parser.execute(this.settings, buf);
    } catch (HTTPException e) {
      throw new RaiseException(runtime, eParseError, e.getMessage(), true);
    }

    if (buf.position() != buf.limit())
      throw new RaiseException(runtime, eParseError, "Could not parse data entirely", true);

    return runtime.getTrue();
  }

  @JRubyMethod(name = "keep_alive?")
  public IRubyObject shouldKeepAlive() {
    // return parser.shouldKeepAlive() ? runtime.getTrue() : runtime.getFalse();
    return runtime.getFalse();
  }

  @JRubyMethod(name = "upgrade?")
  public IRubyObject shouldUpgrade() {
    return parser.getUpgrade() ? runtime.getTrue() : runtime.getFalse();
  }

  @JRubyMethod(name = "http_major")
  public IRubyObject httpMajor() {
    return RubyNumeric.int2fix(runtime, parser.getMajor());
  }

  @JRubyMethod(name = "http_minor")
  public IRubyObject httpMinor() {
    return RubyNumeric.int2fix(runtime, parser.getMinor());
  }

  @JRubyMethod(name = "http_method")
  public IRubyObject httpMethod() {
    HTTPMethod method = parser.getHTTPMethod();
    if (method != null)
      return runtime.newString(new String(method.bytes));
    else
      return runtime.getNil();
  }

  @JRubyMethod(name = "status_code")
  public IRubyObject statusCode() {
    int code = parser.getStatusCode();
    if (code != 0)
      return RubyNumeric.int2fix(runtime, code);
    else
      return runtime.getNil();
  }
}
