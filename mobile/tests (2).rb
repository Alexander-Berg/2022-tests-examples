private_lane :tests do |options|
    teamcity_activity(activity: "Build and run tests")

    scan(
      configuration: options[:configuration],
      app_name: options[:target],
      workspace: options[:workspace],
      scheme: options[:scheme],
      derived_data_path: options[:derived_data_path],
      deployment_target_version: options[:test_os_version],
      device: options[:test_device],
      output_directory: options[:build_dir_path],
      buildlog_path: options[:logs_dir_path],
      code_coverage: options[:code_coverage],
      skip_detect_devices: false,
      reset_simulator: true
    )

    log_file_name = "#{options[:target]}-#{options[:scheme]}.log"
    log_file_path = File.join(options[:logs_dir_path], log_file_name)
    rename_file(
      file_at_path:log_file_path,
      new_filename: "xcodebuild-output.txt"
    )
  end