module Fastlane
    module Actions
        class TestAction < Action
            def self.run(params)

                #require_relative '../libs/tarsfarm/tarsfarm.rb'
                #
                #tf = Tarsfarm::Client.new
                #uploading = tf.makeUploading('ios', 'testapp', 'master', '2237', {
                #    'made-by' => 'dmt021' # destination
                #})
                #uploading.upload('DirectTests.zip', {
                #    type: 'file',
                #    path: '/Users/dmt021/Desktop/tmp/mobile-direct-client-ios/YandexDirect/build5/Artifacts/DirectTests.zip'
                #})
            


                # Actions::UploadTarsfarmAction.run({
                #     :platform => "ios",
                #     :project_name => "testapp",
                #     :branch => "master",
                #     :build => "2238",
                #     :files => {
                #         '/Users/dmt021/Desktop/tmp/mobile-direct-client-ios/YandexDirect/build4/Artifacts/1.zip' => "1.zip"
                #     }
                # })

                Actions::RunTarsfarmTestAction.run({
                    :artifact_url => '/Users/dmt021/Desktop/work/tars/tarsfarm/sample-app/Tarsfarm Test App/build/Build/testplan.zip', #'/Users/dmt021/Desktop/tmp/mobile-direct-client-ios/YandexDirect/build4/Artifacts/2.zip', #'http://tarsfarm.s3.mds.yandex.net/tarsfarm/v1/artifacts/ios/projects/testapp/branches/master/2238/1fd69c5d060baddf332a7100cd5f57478f3b799a/files/1.zip',
                    #:xctestrun => 'App_iphoneos11.2-arm64.xctestrun' #'Direct_iphonesimulator11.1-x86_64.xctestrun'
                })
    
            end
    
            #####################################################
            # @!group Documentation
            #####################################################
    
            def self.description
            "Test action"
            end
    
            def self.available_options
            [
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
