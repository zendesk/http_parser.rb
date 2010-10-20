import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.runtime.load.BasicLibraryService;

import org.ruby_http_parser.*;

public class RubyHttpParserService implements BasicLibraryService {
  public boolean basicLoad(final Ruby runtime) throws IOException {
    RubyModule mHTTP = runtime.defineModule("HTTP");

    mHTTP.defineClassUnder("ParseError", runtime.getClass("IOError"),runtime.getClass("IOError").getAllocator());

    RubyClass cParser = mHTTP.defineClassUnder("Parser", runtime.getObject(), RubyHttpParser.ALLOCATOR);
    cParser.defineAnnotatedMethods(RubyHttpParser.class);
    return true;
  }
}
