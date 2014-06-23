Gem::Specification.new do |s|
  s.name = "http_parser.rb"
  s.version = "0.6.0"
  s.summary = "Simple callback-based HTTP request/response parser"
  s.description = "Ruby bindings to https://github.com/joyent/http-parser and https://github.com/http-parser/http-parser.java"

  s.authors = ["Marc-Andre Cournoyer", "Aman Gupta"]
  s.email   = ["macournoyer@gmail.com", "aman@tmm1.net"]
  s.license = 'MIT'

  s.homepage = "https://github.com/tmm1/http_parser.rb"
  s.files = `git ls-files`.split("\n") + Dir['ext/ruby_http_parser/vendor/**/*']

  s.require_paths = ["lib"]
  s.extensions    = ["ext/ruby_http_parser/extconf.rb"]

  s.add_development_dependency 'rake-compiler', '>= 0.7.9'
  s.add_development_dependency 'rspec', '>= 3'
  s.add_development_dependency 'json', '>= 1.4.6'
  s.add_development_dependency 'benchmark_suite'
  s.add_development_dependency 'ffi'

  if RUBY_PLATFORM =~ /java/
    s.add_development_dependency 'jruby-openssl'
  else
    s.add_development_dependency 'yajl-ruby', '>= 0.8.1'
  end
end
