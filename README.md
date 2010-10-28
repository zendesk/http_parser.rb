# http_parser.rb

A simple callback-based HTTP request/response parser for writing http
servers, clients and proxies.

This gem is built on top of [ry/http-parser](http://github.com/ry/http-parser) and its java port [a2800276/http-parser.java](http://github.com/a2800276/http-parser.java).

## Supported Platforms

This gem aims to work on all major Ruby platforms, including:

- MRI 1.8 and 1.9
- Rubinius
- JRuby
- win32

## Usage

    require "http/parser"

    parser = Http::Parser.new

    parser.on_headers_complete = proc do |headers|
      p parser.http_method
      p parser.http_version

      p parser.request_url # for requests
      p parser.status_code # for responses

      p headers
    end

    parser.on_body = proc do |chunk|
      # One chunk of the body
      p chunk
    end

    parser.on_message_complete = proc do |env|
      # Headers and body is all parsed
      puts "Done!"
    end

    # Feed raw data from the socket to the parser
    parser << raw_data

