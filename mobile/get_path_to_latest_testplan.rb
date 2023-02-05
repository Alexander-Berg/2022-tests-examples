module Fastlane
    module Actions
        class GetPathToLatestTestplanAction < Action
            @@default_platform = "ios"
            @@default_project_name = "tarsfarm-sample-app"
            @@default_branch = "dev"
            @@default_build = "unknown"

            def self.run(params)
                require_relative '../libs/tarsfarm/tarsfarm.rb'
                access_key = params[:access_key]
                secret_key = params[:secret_key]
                s3_client = Tarsfarm::Client.new(params)
                platform = params[:platform] || @@default_platform
                project_name = params[:project] || @@default_project_name
                branch = params[:branch] || @@default_branch
                build = params[:build] || @@default_build
                last_uploading = s3_client.getLastUploading(platform, project_name, branch, build)
                prefix = "http://tarsfarm.s3.mds.yandex.net/tarsfarm/v1/artifacts/"

                return prefix + "#{platform}/projects/#{project_name}/branches/#{branch}/#{build}/#{last_uploading}/files/testplan.zip"	
            end

            def self.authors
                ["andrewsboev"]
            end
        end
    end
end
