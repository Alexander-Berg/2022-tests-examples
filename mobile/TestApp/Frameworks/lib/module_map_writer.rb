# framework module YandexMapKit {
#   umbrella header "YandexMapKit.h"
#
#   export *
#   module * { export * }
# }

def find_headers(framework_path, options)
  files = Dir["#{framework_path}/Headers/**/*.*"]

  exclude_regexps = options[:exclude] || []
  include_regexps = options[:include] || []

  included = files.select { |f|
    include_regexps.each { |ir|
      ret = false
      if (ir =~ f)
        ret = true
      end
      ret
    }
  }
  headers = included.select { |f|
    ret = true
    exclude_regexps.each { |er|
      if (er =~ f)
        ret = false
      end
    }
    name = f.gsub("#{framework_path}/Headers/", "")
    puts "❌  #{name}" unless ret
    ret
  }
  headers = headers.map { |f| f.gsub("#{framework_path}/Headers/", "") }
  puts "\nAccepted headers:"
  headers.each { |h| puts "✅  #{h}" }

  puts "\n"
  headers
end

def write_umbrella(module_name, headers, framework_path = "#{module_name}.framework")
  File.open("#{framework_path}/Headers/#{module_name}.h", 'w') { |file|
    headers.each { |h|
      file.write("#import \"#{h}\"\n")
    }
  }
end

def write_module_map(module_name, headers, framework_path = "#{module_name}.framework")
  dir = "#{framework_path}/Modules"
  path = "#{dir}/module.modulemap"
  mkdir_cmd = "mkdir -p #{dir}"

  %x( #{mkdir_cmd} )

  File.open(path, 'w') { |file|
    file.write("framework module #{module_name} {\n")

    headers.each { |h|
      file.write("\theader \"#{h}\"\n")
    }

    file.write("\n")
    file.write("\texport *\n")
    # file.write("\tmodule * { export * }\n")
    file.write("}\n")
  }
end
