module Fastlane
  module Actions
    class BuildAndRunTestsAction < Action

      def self.run(params)
        options = params[:options]
      
        scan_params = {
            configuration: 'Debug',
            clean: params[:clean].nil? ? true : params[:clean],
            workspace: options.workspace,
            scheme: params[:custom_scheme] || options.scheme,
            testplan: params[:testplan],
            test_without_building: params[:test_without_building],
            skip_detect_devices: true,
            destination: Actions.lane_context[SharedValues::RESOLVED_SIMULATOR_DESTINATION],
            slack_url: options.slack_url,
            slack_message: "Tests for build: #{options.build_number}",
            slack_only_on_failure: true,
            buildlog_path: options.scan_log_path,
            xcargs: options.xcargs,
            derived_data_path: options.derived_data_path,
            output_files: 'junit.xml',
            output_types: 'junit',
            disable_concurrent_testing: params[:disable_concurrent_testing].nil? ? true : params[:disable_concurrent_testing],
        }

        custom_output_directory = params[:custom_output_directory]
        scan_params[:output_directory] = custom_output_directory unless custom_output_directory.nil?

        formatter = "#{File.dirname(__FILE__)}/../libs/teamcity_formatter.rb"
        UI.message("Formatter: #{formatter}")

        scan_params[:formatter] = formatter if options.is_running_on_teamcity

        other_action.scan(scan_params)
        return
      end

      def self.description
        "Run Tests Using SCAN"
      end

      def self.available_options
        [
            FastlaneCore::ConfigItem.new(key: :options,
                                         description: "Configuration Options",
                                         is_string: false,
                                         optional: false),
            FastlaneCore::ConfigItem.new(key: :custom_scheme,
                                         description: "Custom build scheme",
                                         is_string: true,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :custom_output_directory,
                                         description: "Custom directory for test reports",
                                         is_string: true,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :clean,
                                         description: "Clean before build/test",
                                         is_string: false,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :testplan,
                                         description: "Use specific testplan",
                                         is_string: true,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :test_without_building,
                                         description: "Use derived data from previous build",
                                         is_string: false,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :disable_concurrent_testing,
                                         description: "Do not run test bundles in parallel on the specified destinations",
                                         is_string: false,
                                         optional: true)                             
        ]
      end

      def self.is_supported?(platform)
        [:ios].include?(platform)
      end

      def self.authors
        ["sapalt"]
      end

    end
  end
end
