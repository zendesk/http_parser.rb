require "spec_helper"

describe HTTP::Parser do
  before do
    @parser = HTTP::Parser.new
  end

  it "should parse GET" do
    env = nil
    body = ""
    done = false

    @parser.on_headers_complete = proc { |e| env = e }
    @parser.on_body = proc { |chunk| body << chunk }
    @parser.on_message_complete = proc{ done = true }

    @parser << "GET /test?ok=1 HTTP/1.1\r\n" +
               "User-Agent: curl/7.18.0\r\n" +
               "Host: 0.0.0.0:5000\r\n" +
               "Accept: */*\r\n" +
               "Content-Length: 5\r\n" +
               "\r\n" +
               "World"

    @parser.http_major.should == 1
    @parser.http_minor.should == 1
    @parser.http_method.should == 'GET'
    @parser.status_code.should be_nil

    env["PATH_INFO"].should == "/test"
    env["QUERY_STRING"].should == "ok=1"
    env["HTTP_HOST"].should == "0.0.0.0:5000"

    body.should == "World"
    done.should be_true
  end
end
