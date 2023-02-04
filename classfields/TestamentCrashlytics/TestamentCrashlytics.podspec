Pod::Spec.new do |s|
  s.name                 = 'TestamentCrashlytics'
  s.version              = '0.0.1'
  s.summary              = 'Remote logging using Crashlytics'
  s.homepage             = 'https://wiki.yandex-team.ru/realty/mobile/ios/'
  s.license              = { type: 'Proprietary', text: '2021 © Yandex. All rights reserved.' }
  s.author               = { 'Yandex LLC' => 'realty-ios-dev@yandex-team.ru' }
  s.platform             = :ios, '14.0'
  s.source               = { git: '' }
  s.frameworks           = 'Foundation'
  s.requires_arc         = true

  s.swift_version = '5.0'

  s.default_subspecs = ['Error', 'Exception']

  s.dependency 'Testament'
  s.dependency 'FirebaseCrashlytics'

  s.subspec 'Error' do |ss|
    ss.source_files = 'Sources/Error/**/*.{swift}'
  end

  s.subspec 'Exception' do |ss|
    ss.source_files = 'Sources/Exception/**/*.{swift}'
  end
end
