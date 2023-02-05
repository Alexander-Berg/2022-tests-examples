module Fastlane
    module Actions
        class PackTestScreenshotsAction < Action

            def self.run(params)
              begin
                return fetch_screenshots(params)
              rescue => e
                UI.message("Pack Test Screenshots Failed")
                UI.message("Exception Class: #{e.class.name}")
                UI.message("Exception Message: #{e.message}")
                UI.message("Exception Backtrace: #{e.backtrace}")
                return "CANT PACK SCREENSHOTS !!!"
              end
            end

            #####################################################
            # @!group Documentation
            #####################################################

            def self.description
                "Action for parsing test output and copying screenshots"
            end

            def self.available_options
                [
                    FastlaneCore::ConfigItem.new(
                        key: :derived_data_path,
                        description: "Path to derived data",
                        is_string: true,
                        optional: false
                    )
                ]
            end

            def self.is_supported?(platform)
                [:ios].include?(platform)
            end

            def self.fetch_screenshots(params)                
                containing = File.join(params[:derived_data_path], "Logs", "Test")
                attachments_path = File.join(containing, "Attachments")

                UI.message "Attachments path: #{containing}"
                UI.message "Collectiong attachments"

                testGroups = attachments(containing)
                if testGroups.count == 0
                    return false
                end

                output_directory = (File.directory?("fastlane") ? "fastlane/screenshots" : "screenshots")

                UI.message "remove old files from output directory"

                FileUtils.mkdir_p(output_directory)

                UI.message "saving screenshots"
                testGroups.each do |test_group|
                    save_screenshots_for_test_group(test_group, output_directory, attachments_path)
                end

                return output_directory
            end

            def self.save_screenshots_for_test_group(test_group, output_directory, attachments_path)
                test_group_directory = File.join(output_directory, test_group.test_name)
                FileUtils.mkdir_p(test_group_directory)

                test_group.subtests.each do |subtest|
                    save_screenshots_for_subtests(subtest, test_group_directory, attachments_path)
                end
            end

            def self.save_screenshots_for_subtests(subtest, test_group_directory, attachments_path)
                if subtest.success
                    return
                end

                subtest_directory = File.join(test_group_directory, subtest.subtest_name)
                FileUtils.mkdir_p(subtest_directory)

                file_number_length = subtest.screenshots.count.to_s.length

                subtest.screenshots.each_with_index do |screenshot_name, index|                    
                    if screenshot_name.nil?
                        return
                    end

                    new_screenshot_name = ("%0#{file_number_length}d" % index) + "-" + screenshot_name
                    output_path = File.join(subtest_directory, new_screenshot_name)
                    from_path = File.join(attachments_path, screenshot_name)

                    if File.exist?(from_path)
                        UI.success "Copying '#{from_path}' to '#{output_path}'..."
                        FileUtils.cp(from_path, output_path)
                    end
                end
            end

            def self.attachments(containing)
                UI.message "Collecting screenshots..."
                plist_path = Dir[File.join(containing, "*.plist")].sort_by{ |f| File.mtime(f) }.last

                return attachments_in_file(plist_path)
            end

            def self.attachments_in_file(plist_path)
                UI.message "Loading up '#{plist_path}'..."
                report = Plist.parse_xml(plist_path)

                test_groups = []

                report["TestableSummaries"].each do |summary|
                    (summary["Tests"] || []).each do |test|
                        (test["Subtests"] || []).each do |subtest|
                            (subtest["Subtests"] || []).each do |subtest2|

                                subtests = []
                                (subtest2["Subtests"] || []).each do |subtest3|
                                    to_store = []
                                    (subtest3["ActivitySummaries"] || []).each do |activity|
                                        check_activity(activity, to_store)
                                    end

                                    UI.message "subtest name: #{subtest3["TestName"]}"
                                    UI.message "screenshots count: #{to_store.count}"
                                    if to_store.count > 0
                                        subtests << SubTest.new(subtest3["TestName"].gsub(/[^0-9a-z]/i, ''), to_store, subtest3["TestStatus"] == "Success")
                                    end
                                end

                                if subtests.count > 0
                                    test_groups << TestGroup.new(subtest2["TestName"], subtests)
                                end
                            end
                        end
                    end
                end

                return test_groups
            end

            def self.check_activity(activity, to_store)
                if activity["Attachments"]
                    activity["Attachments"].each do |attachment|
                        to_store << attachment["Filename"]
                    end
                else # Xcode 7.3 has stopped including 'Attachments', so we synthesize the filename manually
                    to_store << "Screenshot_#{activity['UUID']}.png"
                end

                (activity["SubActivities"] || []).each do |subactivity|
                    self.check_activity(subactivity, to_store)
                end
            end

        end
    end

    class TestGroup < Object
        attr_accessor :test_name, :subtests

        def initialize(test_name, subtests)
            @test_name = test_name
            @subtests = subtests
        end
    end

    class SubTest < Object
        attr_accessor :subtest_name, :screenshots, :success

        def initialize(subtest_name, screenshots, success)
            @subtest_name = subtest_name
            @screenshots = screenshots
            @success = success
        end
    end

end