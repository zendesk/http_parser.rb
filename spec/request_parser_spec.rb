require "spec_helper"

describe HTTP::Parser do
  before do
    @parser = HTTP::Parser.new
  end

  it "should parse GET" do
    env = nil
    body = ""
    started = false
    done = false

    @parser.on_message_begin = proc{ started = true }
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

    started.should be_true
    done.should be_true

    @parser.http_major.should == 1
    @parser.http_minor.should == 1
    @parser.http_version.should == [1,1]
    @parser.http_method.should == 'GET'
    @parser.status_code.should be_nil

    @parser.request_url.should == '/test?ok=1'
    @parser.request_path.should == '/test'
    @parser.query_string.should == 'ok=1'
    @parser.fragment.should be_empty

    @parser.headers.should == env
    @parser.headers['User-Agent'].should == 'curl/7.18.0'
    @parser.headers['Host'].should == '0.0.0.0:5000'

    body.should == "World"
  end
end
