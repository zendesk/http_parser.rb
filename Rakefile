require "rake/extensiontask"
require 'rake/javaextensiontask'
require "spec/rake/spectask"

if RUBY_PLATFORM =~ /java/
  Rake::JavaExtensionTask.new("ruby_http_parser")
else
  Rake::ExtensionTask.new("ruby_http_parser")
end

Spec::Rake::SpecTask.new do |t|
  t.spec_opts = %w(-fs -c)
  t.spec_files = FileList["spec/**/*_spec.rb"]
end

task :default => [:compile, :spec]
