require 'net/http'
require 'open-uri'
require 'zip'

def download_framework(name, url, options)
  download_dir = "Content/#{name}"
  path = "#{download_dir}/#{name}.zip"

  puts "Downloading '#{name}' from '#{url}'..."
  %x( mkdir -p #{download_dir} )

  input = open(url)
  IO.copy_stream(input, path)

  puts "Unarchiving '#{path}'"
  unzip_file(path, download_dir)

  framework_path = "#{download_dir}/#{name}.framework"

  puts "Generating module map for '#{name}'..."
  headers = find_headers(framework_path, options)
  write_module_map(name, headers, framework_path)

  puts "\n"
end

def unzip_file (file, destination)
  Zip::File.open(file) { |zip_file|
    zip_file.each { |f|
      f_path=File.join(destination, f.name)
      FileUtils.mkdir_p(File.dirname(f_path))
      zip_file.extract(f, f_path) unless File.exist?(f_path)
    }
  }
end
