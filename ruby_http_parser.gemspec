Gem::Specification.new do |s|
  s.name = "ruby_http_parser"
  s.version = "0.5.0"
  s.summary = "Simple callback-based HTTP request/response parser"
  s.description = "Ruby bindings to http://github.com/ry/http-parser and http://github.com/a2800276/http-parser.java"

  s.authors = ["Marc-Andre Cournoyer", "Aman Gupta"]
  s.email   = ["macournoyer@gmail.com", "aman@tmm1.net"]

  s.homepage = "http://github.com/tmm1/ruby_http_parser"
  s.files = `git ls-files`.split("\n") + Dir['ext/ruby_http_parser/vendor/**/*']

  s.require_paths = ["lib"]
  s.extensions    = ["ext/ruby_http_parser/extconf.rb"]
end
