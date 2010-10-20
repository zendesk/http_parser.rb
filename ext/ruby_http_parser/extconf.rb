require 'mkmf'

http_parser_dir = File.expand_path('../vendor/http-parser', __FILE__)
$CFLAGS << " -I#{http_parser_dir} "

dir_config("ruby_http_parser")
create_makefile("ruby_http_parser")
