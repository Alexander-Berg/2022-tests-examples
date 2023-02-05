Pod::Spec.new do |s|
    s.name             = 'TestApp'
    s.version          = '1.0.0'
    s.summary          = 'TestApp'
    s.homepage         = "local" #path to home page
    s.license          = { type: 'Proprietary', text: '2021 © Yandex. All rights reserved.' }
    s.authors          = { 'Alexey Osipenko' => 'aosipenko@yandex-team.ru' }
    s.source           = { :git => 'local', :tag => s.version.to_s }
    s.description      = 'TestApp'
    s.requires_arc     = true

    s.ios.deployment_target = '13.0'

    s.source_files = 'pods/Dummy.swift'

    s.app_spec 'Guidance' do |app|
        app.source_files = 'Guidance/*.swift'
        app.resources = 'Guidance/Base.lproj/*'

        app.ios.frameworks = 'CoreMotion'

        app.info_plist = {
            'CFBundleIdentifier' => '$(PRODUCT_BUNDLE_IDENTIFIER)',

            'NSLocationWhenInUseUsageDescription' =>
                'Give access to your location to get the right directions.',
            'UIBackgroundModes' => ['audio', 'location'],
            'UILaunchStoryboardName' => 'LaunchScreen',
            'UIMainStoryboardFile' => 'Main',

            'yandex.maps.mapkit.directions.predictedRoute' => 'enabled',
            'yandex.maps.mapkit.map.DisableYandexLogo' => 'yes',
            'yandex.maps.mapkit.map.DisabledLayers' => 'point-cloud,masstransit',
            'yandex.maps.runtime.hosts.Env' => 'production',
            'yandex.maps.runtime.logging.Level' => 'Error'
        }

        app.pod_target_xcconfig = {
            'ARCHS' => 'arm64 x86_64',
            'CODE_SIGN_IDENTITY' => 'iPhone Distribution',
            'CODE_SIGN_STYLE' => 'Manual',
            'DEVELOPMENT_TEAM' => 'EK7Z26L6D4',
            'ENABLE_BITCODE' => 'NO',
            'PRODUCT_BUNDLE_IDENTIFIER' => 'ru.yandex.mobile.navigator.guidance-lib-test-app',
            'PROVISIONING_PROFILE_SPECIFIER' => 'Generic Yandex In House Profile 2014',
            'SKIP_INSTALL' => 'NO',
        }

        app.dependency 'Libs'

        app.dependency 'YandexNaviKitAssets'
        app.dependency 'YandexNaviDayNight'
        app.dependency 'YandexNaviGuidanceUI'
        app.dependency 'YandexNaviHelpers'
        app.dependency 'YandexNaviStrings'
        app.dependency 'YandexNaviStyleKitExtended'

        app.dependency 'YandexMapsMobile'
    end

    s.app_spec 'Projected' do |app|
        app.source_files = 'Projected/**/*.swift'
        app.resources = 'Projected/{Base,en,ru}.lproj/*'

        app.ios.frameworks = 'CoreMotion'

        app.info_plist = {
            'CFBundleIdentifier' => '$(PRODUCT_BUNDLE_IDENTIFIER)',
            'CFBundleDevelopmentRegion' => '$(DEVELOPMENT_LANGUAGE)',

            'NSLocationWhenInUseUsageDescription' =>
                'Give access to your location to get the right directions.',
            'UIBackgroundModes' => ['audio', 'location'],
            'UILaunchStoryboardName' => 'LaunchScreen',
            'UIMainStoryboardFile' => 'Main',

            'UIApplicationSceneManifest' => {
                'CPSupportsDashboardNavigationScene' => true,
                'UISceneConfigurations' => {
                    'UIWindowSceneSessionRoleApplication' => [{
                        'UISceneClassName' => 'UIWindowScene',
                        'UISceneConfigurationName' => 'WindowSceneConfiguration',
                        'UISceneDelegateClassName'  => '(PRODUCT_MODULE_NAME).WindowSceneDelegate',
                    }],
                    'CPTemplateApplicationSceneSessionRoleApplication' => [{
                        'UISceneClassName' => 'CPTemplateApplicationScene',
                        'UISceneConfigurationName' => 'TemplateSceneConfiguration',
                        'UISceneDelegateClassName'  => '(PRODUCT_MODULE_NAME).TemplateApplicationSceneDelegate',
                    }],
                    'CPTemplateApplicationDashboardSceneSessionRoleApplication' => [{
                        'UISceneClassName' => 'CPTemplateApplicationDashboardScene',
                        'UISceneConfigurationName' => 'DashboardSceneConfiguration',
                        'UISceneDelegateClassName'  => '(PRODUCT_MODULE_NAME).TemplateApplicationSceneDelegate',
                    }],
                },
            },

            'yandex.maps.mapkit.directions.predictedRoute' => 'enabled',
            'yandex.maps.mapkit.map.DisableYandexLogo' => 'yes',
            'yandex.maps.mapkit.map.DisabledLayers' => 'point-cloud,masstransit',
            'yandex.maps.runtime.hosts.Env' => 'production',
            'yandex.maps.runtime.logging.Level' => 'Error',
        }

        app.pod_target_xcconfig = {
            'ARCHS' => 'arm64 x86_64',
            'CODE_SIGN_ENTITLEMENTS' => '../TestApp/Projected/Projected.entitlements',
            'CODE_SIGN_IDENTITY' => 'iPhone Developer',
            'CODE_SIGN_IDENTITY[config=AdHoc]' => 'iPhone Distribution: Yandex LLC',
            'CODE_SIGN_STYLE' => 'Manual',
            'DEVELOPMENT_TEAM' => '477EAT77S3',
            'DEVELOPMENT_TEAM[config=AdHoc]' => 'EK7Z26L6D4',
            'PRODUCT_BUNDLE_IDENTIFIER' => 'ru.yandex.mobile.projected-lib-test-app.sandbox',
            'PRODUCT_BUNDLE_IDENTIFIER[config=AdHoc]' => 'ru.yandex.mobile.projected-lib-test-app.inhouse',
            'PROVISIONING_PROFILE_SPECIFIER' => 'Projected Lib Test App Dev Provisioning Profile',
            'PROVISIONING_PROFILE_SPECIFIER[config=AdHoc]' => 'Projected Lib TestApp InHouse Provisioning Profile',
            'SKIP_INSTALL' => 'NO',
        }

        app.dependency 'Libs'

        app.dependency 'YandexNaviKitAssets'
        app.dependency 'YandexNaviDayNight'
        app.dependency 'YandexNaviGuidanceUI'
        app.dependency 'YandexNaviHelpers'
        app.dependency 'YandexNaviProjectedLibDeps'
        app.dependency 'YandexNaviProjectedUI'
        app.dependency 'YandexNaviSearch'
        app.dependency 'YandexNaviStrings'
        app.dependency 'YandexNaviStyleKit'
        app.dependency 'YandexNaviStyleKitExtended'

        app.dependency 'YandexMapsMobile'

        app.dependency 'KotlinNativeInteropHelpers'
        app.dependency 'KotlinNative/AllNaviModules'
        app.dependency 'KotlinNativeAdapters/Core'

        app.dependency 'MetroToolbox'

        app.dependency 'YandexGeoCommonComponents'

        app.dependency 'YandexMapsAccessibility'
        app.dependency 'YandexMapsAssets'
        app.dependency 'YandexMapsAssetsProvider'
        app.dependency 'YandexMapsBinders/SearchBinders'
        app.dependency 'YandexMapsCommonComponents'
        app.dependency 'YandexMapsCommonTypes'
        app.dependency 'YandexMapsControls'
        app.dependency 'YandexMapsCurbside'
        app.dependency 'YandexMapsDeps/Core'
        app.dependency 'YandexMapsDeps/SearchResults'
        app.dependency 'YandexMapsDeps/WebViewScreenCommon'
        app.dependency 'YandexMapsDirectCard'
        app.dependency 'YandexMapsEventTracker'
        app.dependency 'YandexMapsFonts'
        app.dependency 'YandexMapsKeys'
        app.dependency 'YandexMapsMapKit'
        app.dependency 'YandexMapsMasstransit'
        app.dependency 'YandexMapsMocks'
        app.dependency 'YandexMapsModularSnippet/Core'
        app.dependency 'YandexMapsNavigation'
        app.dependency 'YandexMapsOverlayScreen'
        app.dependency 'YandexMapsParkingPayment'
        app.dependency 'YandexMapsPersonalAccount'
        app.dependency 'YandexMapsPhotos'
        app.dependency 'YandexMapsPlaceSelection'
        app.dependency 'YandexMapsRubricsAndPOI/Common'
        app.dependency 'YandexMapsRx'
        app.dependency 'YandexMapsSearch'
        app.dependency 'YandexMapsSearchResultCard'
        app.dependency 'YandexMapsSearchResults'
        app.dependency 'YandexMapsShutter'
        app.dependency 'YandexMapsShutterV3'
        app.dependency 'YandexMapsStories'
        app.dependency 'YandexMapsStrings'
        app.dependency 'YandexMapsSuggest'
        app.dependency 'YandexMapsUGC'
        app.dependency 'YandexMapsUI'
        app.dependency 'YandexMapsUIComponents'
        app.dependency 'YandexMapsUtils'
    end
end
