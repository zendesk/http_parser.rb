# Ruby bindings to Ryan Dahl's http-parser

Ruby bindings to http://github.com/ry/http-parser

## Overview

This gem aims to provide a simple Ruby HTTP parser API that can be used
to build HTTP servers, clients and proxies. The gem will support all
major Ruby platforms (JRuby, MRI 1.8 and 1.9, win32 and Rubinius).

## Usage

    require "http_parser"

    parser = HTTP::RequestParser.new

    parser.on_headers_complete = proc do |env|
      # Rack formatted env hash
      p env
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
