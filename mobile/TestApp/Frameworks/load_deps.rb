require_relative "lib/module_map_writer.rb"
require_relative "lib/helpers.rb"

# Runtime
# 41.5.0
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/139404/YandexRuntime_41.5.0.zip
# 42.11.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexRuntime_42.11.0.zip?at=refs%2Fheads%2Fmaster
# 44.7.5
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/124853/YandexRuntime_44.7.5.zip
# 48.10.17
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/117776/YandexRuntime_48.10.17.zip
# 52.13.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexRuntime_52.13.0.framework.zip?at=refs%2Fheads%2Fmaster
# 52.34.5
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/139404/YandexRuntime_52.34.5.zip

download_framework("YandexRuntime", "http://storage-int.mds.yandex.net:80/get-maps-ios-pods/139404/YandexRuntime_52.34.5.zip",
  :include => [/^.*\.h/],
  :exclude => [/^.*_Private.*/, /^.*Internal\/.*/, /^.*YRTView.h/]
)

# DataSync
# 9.4.0
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/176005/YandexDataSync_9.4.0.zip
# 9.9.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexDataSync_9.9.0.zip?at=refs%2Fheads%2Fmaster
# 10.1.1
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/139404/YandexDataSync_10.1.1.zip
# 10.9.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexDataSync_10.9.0.framework.zip?at=refs%2Fheads%2Fmaster
# 10.13.4
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/124853/YandexDataSync_10.13.4.zip

download_framework("YandexDataSync", "http://storage-int.mds.yandex.net:80/get-maps-ios-pods/124853/YandexDataSync_10.13.4.zip",
  :include => [/^.*\.h/],
  :exclude => [/^.*_Private.*/, /^.*Internal\/.*/]
)

# MapKit
# 14.27.19
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/139404/YandexMapKit_14.27.19.zip
# 15.27.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexMapKit_15.27.0.zip?at=refs%2Fheads%2Fmaster
# 15.90.36
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/149253/YandexMapKit_15.90.36.zip
# 15.90.42
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/117776/YandexMapKit_15.90.42.zip
# 18.91.20
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/149253/YandexMapKit_18.91.20.zip
# 22.33.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexMapKit_22.33.0.framework.zip?at=refs%2Fheads%2Fmaster
# 22.81.33
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/149253/YandexMapKit_22.81.33.zip

download_framework("YandexMapKit", "http://storage-int.mds.yandex.net:80/get-maps-ios-pods/149253/YandexMapKit_22.81.33.zip",
  :include => [/^.*\.h/],
  :exclude => [/^.*_Private.*/, /^.*Internal\/.*/]
)


# Bookmarks
# 5.4.0
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/176005/YandexMapsBookmarks_5.4.0.zip
# 5.8.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexMapsBookmarks_5.8.0.zip?at=refs%2Fheads%2Fmaster
# 5.20.0
# https://bitbucket.browser.yandex-team.ru/users/knkiselyov/repos/storage/raw/YandexMapsBookmarks_5.20.0.framework.zip?at=refs%2Fheads%2Fmaster
# 5.25.3
# http://storage-int.mds.yandex.net:80/get-maps-ios-pods/176005/YandexMapsBookmarks_5.25.3.zip

download_framework("YandexMapsBookmarks", "http://storage-int.mds.yandex.net:80/get-maps-ios-pods/176005/YandexMapsBookmarks_5.25.3.zip",
  :include => [/^.*\.h/],
  :exclude => [/^.*_Private.*/, /^.*Internal\/.*/, /^.*YRTView.h/]
)
