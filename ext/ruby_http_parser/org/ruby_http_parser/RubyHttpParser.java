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

public class RubyHttpParser extends RubyObject {

  public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
    public IRubyObject allocate(Ruby runtime, RubyClass klass) {
      return new RubyHttpParser(runtime, klass);
    }
  };

  private Ruby runtime;
  private HTTPParser parser;
  private RubyClass eParseError;
  private ParserSettings settings;

  public RubyHttpParser(Ruby runtime, RubyClass clazz) {
    super(runtime,clazz);
    this.runtime = runtime;
    this.eParseError = (RubyClass)runtime.getModule("HTTP").getConstant("ParseError");
    this.parser = new HTTPParser();
    this.settings = new ParserSettings();
  }

  @JRubyMethod
  public IRubyObject initialize() {
    return this;
  }

  @JRubyMethod
  public IRubyObject execute(IRubyObject data) {
    RubyString str = (RubyString)data;
    try {
      this.parser.execute(this.settings, ByteBuffer.wrap(str.getBytes()));
    } catch (HTTPException e) {
      throw new RaiseException(runtime, eParseError, e.getMessage(), true);
    }
    return runtime.getTrue();
  }

}
