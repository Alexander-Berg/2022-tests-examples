require 'open-uri'
require 'securerandom'
require 'digest'
require 'fileutils'
require 'json'

module Fastlane
    module Actions
        class RunTarsfarmTestAction < Action
            def self.run(params)
                workingId = Digest::SHA1.hexdigest SecureRandom.uuid
                workingDir = "./work/#{workingId}/"
                
                FileUtils::mkdir_p(workingDir)

                zipPath = workingDir + 'testplan.zip'
                unzipPath = workingDir + 'testplan/'
                open(zipPath, 'wb') do |file| 
                    file << open(params[:artifact_url]).read
                end
                system("unzip", "-q", "-d", unzipPath, zipPath)

                xctestrunFileName = params[:xctestrun]
                if xctestrunFileName.nil? || xctestrunFileName.empty?
                    Dir.chdir(unzipPath) do
                        xctestrunFileName = Dir.glob("*.xctestrun").first
                    end
                end
                xctestrunPath = "#{unzipPath}#{xctestrunFileName}"
                # TODO check xctestrunPath exists

                buildFilePath = "#{unzipPath}build.json"

                architectures = []
                if(File.exist?(buildFilePath)) 
                    buildFile = File.read(buildFilePath)
                    buildInfo = JSON.parse(buildFile)
                    architectures = buildInfo['architectures'] || []
                end

                #puts architectures

                voltaPid = -1
                voltaCfgPath = params[:volta_config]
                voltaDefaultsPath = params[:defaults] || ""
                if (!voltaCfgPath.nil?) && (!voltaCfgPath.empty?) && File.exist?(voltaCfgPath) 
                    command = "volta -c #{voltaCfgPath}"
                    if (voltaDefaultsPath.length > 0) 
                        command += " --defaults #{voltaDefaultsPath}" 
                    end
                    voltaPid = Process.spawn(command)
                else
                    raise "volta_config is required"
                end

                begin
                    UI.message("Flashing...")
                    Actions::XcodebuildAction.run({
                        :derivedDataPath => workingDir + 'derivedData',
                        :destination => self.getDestination(architectures),
                        :xcargs => "test-without-building -xctestrun ~/VoltaFlashApp/VoltaFlashApp_iphoneos11.4-arm64.xctestrun"
                    })

                    UI.message("Running Tests...")
                    Actions::XcodebuildAction.run({
                        :derivedDataPath => workingDir + 'derivedData',
                        :destination => self.getDestination(architectures),
                        :xcargs => "test-without-building -xctestrun #{xctestrunPath}"
                    })
                rescue => e
                    UI.message("Run TARSfarm Tests failed@")
                    UI.message("Exception Class: #{e.class.name}")
                    UI.message("Exception Message: #{e.message}")
                    UI.message("Exception Backtrace: #{e.backtrace}")
                ensure
                    if voltaPid != -1
                        Process.kill("SIGINT", voltaPid)
                    end
                    FileUtils.rm(zipPath, :force => true)
                    FileUtils.remove_dir(unzipPath, :force => true)
                end
            end

            def self.getDestination(architectures)
                devs = FastlaneCore::DeviceManager.connected_devices('iOS')
                deviceSpecifier = 'platform=iOS,name=iphone-volta'
                devs.each do |dev|
                    UI.message("Found: #{dev.name} -> #{dev.udid}")
                    if dev.name == 'iphone-volta'
                        UI.message("Found needed device, it is #{dev.udid}, chaning specifier to 'id=#{dev.udid}'")
                        deviceSpecifier = "id=#{dev.udid}"
                    end
                end

                if architectures.include?("arm64")
                    return deviceSpecifier
                elsif architectures.include?("armv7") || architectures.include?("armv7s")
                    return deviceSpecifier
                elsif architectures.include?("x86_64")
                    return 'platform=iOS Simulator,name=iPhone 5s'
                elsif architectures.include?("x86")
                    return 'platform=iOS Simulator,name=iPhone 5'
                else
                    return 'platform=iOS Simulator,name=iPhone 5s'
                end
            end
    
            #####################################################
            # @!group Documentation
            #####################################################
    
            def self.description
                "Run tests on tarsfarm"
            end
    
            def self.available_options
                [
                    FastlaneCore::ConfigItem.new(
                        key: :artifact_url,
                        env_name: "",
                        description: "",
                        is_string: true, # true: verifies the input is a string, false: every kind of value
                        optional: false
                        ),
                    FastlaneCore::ConfigItem.new(
                        key: :xctestrun,
                        env_name: "",
                        description: "",
                        is_string: true, # true: verifies the input is a string, false: every kind of value
                        optional: true
                        ),
                    FastlaneCore::ConfigItem.new(
                        key: :volta_config,
                        env_name: "VOLTA_CONFIG",
                        description: "",
                        is_string: true,
                        optional: true
                        ),
  		    FastlaneCore::ConfigItem.new(
                        key: :defaults,
                        env_name: "",
                        description: "",
                        is_string: true,
                        optional: true
		    )
                ]
            end
    
            def self.output
                # Define the shared values you are going to provide
                # Example
                [
                    # ['S3_FILE_OUTPUT_PATH', 'URL of the uploaded file.']
                ]
            end
    
            def self.return_value
                # If you method provides a return value, you can describe here what it does
                return "Returns "
            end
    
            def self.authors
                # So no one will ever forget your contribution to fastlane :) You are awesome btw!
                ["dmt021"]
            end
    
            def self.is_supported?(platform)
                true
            end
        end
    end
end
