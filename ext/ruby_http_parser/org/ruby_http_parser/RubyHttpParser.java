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

  private Ruby runtime;
  private HTTPParser parser;
  private ParserSettings settings;

  private RubyClass eParseError;

  private RubyHash headers;

  private IRubyObject on_message_begin;
  private IRubyObject on_headers_complete;
  private IRubyObject on_body;
  private IRubyObject on_message_complete;

  private IRubyObject requestUrl;
  private IRubyObject requestPath;
  private IRubyObject queryString;
  private IRubyObject fragment;

  private String _current_header;
  private String _last_header;

  public RubyHttpParser(final Ruby runtime, RubyClass clazz) {
    super(runtime,clazz);

    this.runtime = runtime;
    this.eParseError = (RubyClass)runtime.getModule("HTTP").getConstant("ParseError");

    initSettings();
    init();
  }

  private void initSettings() {
    this.settings = new ParserSettings();

    this.settings.on_url = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)requestUrl).concat(runtime.newString(new String(data)));
        return 0;
      }
    };
    this.settings.on_path = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)requestPath).concat(runtime.newString(new String(data)));
        return 0;
      }
    };
    this.settings.on_query_string = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)queryString).concat(runtime.newString(new String(data)));
        return 0;
      }
    };
    this.settings.on_fragment = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)fragment).concat(runtime.newString(new String(data)));
        return 0;
      }
    };

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
          _last_header = _current_header;
          _current_header = null;
        }

        IRubyObject key = (IRubyObject)runtime.newString(_last_header);

        ThreadContext context = headers.getRuntime().getCurrentContext();
        IRubyObject val = headers.op_aref(context, key);

        if (val.isNil())
          headers.op_aset(context, key, runtime.newString(new String(data)));
        else
          ((RubyString)val).cat(data);

        return 0;
      }
    };

    this.settings.on_message_begin = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        headers = new RubyHash(runtime);

        requestUrl = runtime.newString("");
        requestPath = runtime.newString("");
        queryString = runtime.newString("");
        fragment = runtime.newString("");

        if (on_message_begin != null) {
          ThreadContext context = on_message_begin.getRuntime().getCurrentContext();
          on_message_begin.callMethod(context, "call");
        }

        return 0;
      }
    };
    this.settings.on_message_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        if (on_message_complete != null) {
          ThreadContext context = on_message_complete.getRuntime().getCurrentContext();
          on_message_complete.callMethod(context, "call");
        }
        return 0;
      }
    };
    this.settings.on_headers_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        if (on_headers_complete != null) {
          ThreadContext context = on_headers_complete.getRuntime().getCurrentContext();
          on_headers_complete.callMethod(context, "call", headers);
        }
        return 0;
      }
    };
    this.settings.on_body = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        if (on_body != null) {
          byte[] data = fetchBytes(buf, pos, len);
          ThreadContext context = on_body.getRuntime().getCurrentContext();
          on_body.callMethod(context, "call", on_body.getRuntime().newString(new String(data)));
        }
        return 0;
      }
    };
  }

  private void init() {
    this.parser = new HTTPParser();
    this.headers = null;

    this.on_message_begin = null;
    this.on_headers_complete = null;
    this.on_body = null;
    this.on_message_complete = null;

    this.requestUrl = runtime.getNil();
    this.requestPath = runtime.getNil();
    this.queryString = runtime.getNil();
    this.fragment = runtime.getNil();
  }

  @JRubyMethod(name = "on_message_begin=")
  public IRubyObject set_on_message_begin(IRubyObject cb) {
    on_message_begin = cb;
    return cb;
  }

  @JRubyMethod(name = "on_headers_complete=")
  public IRubyObject set_on_headers_complete(IRubyObject cb) {
    on_headers_complete = cb;
    return cb;
  }

  @JRubyMethod(name = "on_body=")
  public IRubyObject set_on_body(IRubyObject cb) {
    on_body = cb;
    return cb;
  }

  @JRubyMethod(name = "on_message_complete=")
  public IRubyObject set_on_message_complete(IRubyObject cb) {
    on_message_complete = cb;
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

    if (parser.getUpgrade()) {
      // upgrade request
    } else if (buf.hasRemaining()) {
      throw new RaiseException(runtime, eParseError, "Could not parse data entirely", true);
    }

    return runtime.getTrue();
  }

  @JRubyMethod(name = "keep_alive?")
  public IRubyObject shouldKeepAlive() {
    return parser.shouldKeepAlive() ? runtime.getTrue() : runtime.getFalse();
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

  @JRubyMethod(name = "http_version")
  public IRubyObject httpVersion() {
    return runtime.newArray(httpMajor(), httpMinor());
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

  @JRubyMethod(name = "headers")
  public RubyHash getHeaders() {
    return headers;
  }

  @JRubyMethod(name = "request_url")
  public IRubyObject getRequestUrl() {
    return requestUrl;
  }

  @JRubyMethod(name = "request_path")
  public IRubyObject getRequestPath() {
    return requestPath;
  }

  @JRubyMethod(name = "query_string")
  public IRubyObject getQueryString() {
    return queryString;
  }

  @JRubyMethod(name = "fragment")
  public IRubyObject getFragment() {
    return fragment;
  }

  @JRubyMethod(name = "reset!")
  public IRubyObject reset() {
    return runtime.getTrue();
  }

}
