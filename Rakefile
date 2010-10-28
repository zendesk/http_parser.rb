require "rake/extensiontask"
require 'rake/javaextensiontask'
require "rspec/core/rake_task"

def gemspec
  @clean_gemspec ||= eval(File.read(File.expand_path('../http_parser.rb.gemspec', __FILE__)))
end

if RUBY_PLATFORM =~ /java/
  Rake::JavaExtensionTask.new("ruby_http_parser", gemspec)
else
  Rake::ExtensionTask.new("ruby_http_parser", gemspec) do |ext|
    unless RUBY_PLATFORM =~ /mswin|mingw/
      ext.cross_compile = true
      ext.cross_platform = ['x86-mingw32', 'x86-mswin32-60']

      # inject 1.8/1.9 pure-ruby entry point when cross compiling only
      ext.cross_compiling do |spec|
        spec.files << 'lib/ruby_http_parser.rb'
      end
    end
  end
end

file 'lib/ruby_http_parser.rb' do |t|
  File.open(t.name, 'wb') do |f|
    f.write <<-eoruby
RUBY_VERSION =~ /(\\d+.\\d+)/
require "\#{$1}/ruby_http_parser"
    eoruby
  end
end

if Rake::Task.task_defined?(:cross)
  task :cross => "lib/ruby_http_parser.rb"
end

RSpec::Core::RakeTask.new do |t|
  t.rspec_opts = %w(-fs -c)
end

task :default => [:compile, :spec]

desc "Fetch upstream submodules"
task :init_submodules do
  if Dir['ext/ruby_http_parser/vendor/http-parser/*'].empty?
    sh 'git submodule init'
    sh 'git submodule update'
  end
end

task :compile => :init_submodules
