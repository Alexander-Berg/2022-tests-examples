desc "Yandex tests"
lane :tests do |options|
    scan(scheme: "App", # !!! Setup proper project scheme
         clean: false,
         slack_channel: "#builds", # !!! Setup proper slack channel or disable slack reporting
         slack_message: "iOS: протестирован билд #{options[:betaBranch]} (#{options[:buildNumber]})",
         skip_slack: false)
end
