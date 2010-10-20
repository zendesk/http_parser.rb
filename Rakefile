require "rake/extensiontask"
require 'rake/javaextensiontask'
require "rspec/core/rake_task"

if RUBY_PLATFORM =~ /java/
  Rake::JavaExtensionTask.new("ruby_http_parser")
else
  Rake::ExtensionTask.new("ruby_http_parser")
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
