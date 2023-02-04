Pod::Spec.new do |s|
  s.name                 = 'TestamentObjC'
  s.version              = '0.0.1'
  s.summary              = 'Assertion framework'
  s.homepage             = 'https://wiki.yandex-team.ru/realty/mobile/ios/'
  s.license              = { type: 'Proprietary', text: '2021 © Yandex. All rights reserved.' }
  s.author               = { 'Yandex LLC' => 'realty-ios-dev@yandex-team.ru' }
  s.platform             = :ios, '14.0'
  s.source               = { git: '' }
  s.frameworks           = 'Foundation'
  s.requires_arc         = true

  s.swift_version = '5.0'

  s.default_subspecs = ['ObjC']

  s.subspec 'ObjC' do |ss|
    ss.source_files = 'Sources/ObjC/*.{h,m,swift}'

    ss.dependency 'Testament/Core'
  end
end
