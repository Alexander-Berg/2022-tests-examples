load("//tools:defaults.bzl", "scala_binary", "scala_image")
load("//tools/shiva:rules.bzl", "shiva_service")

scala_binary(
    name = "test-calls",
    srcs = glob(["src/**/*.scala"]),
    main_class = "ru.yandex.vertis.etc.telepony.telepony_test_calls.Main",
    resource_strip_prefix = package_name() + "/resources",
    resources = glob(["resources/**/*"]),
    visibility = ["//etc/dust:__subpackages__"],
    runtime_deps = [
        "@maven//:mysql_mysql_connector_java",
        "@maven//:ru_yandex_yql_yql_jdbc",
    ],
    deps = [
        "//common/yt:live",
        "//common/zio/akka",
        "//common/zio/app/base",
        "//common/zio/config",
        "//common/zio/doobie",
        "//common/zio/logging",
        "//common/zio/pureconfig",
        "//common/zio/vertis-scheduler",
        "//common/zio/ydb",
        "//etc/telepony/telepony-controller:controller",
        "//etc/telepony/telepony-dao:dao",
        "@maven//:dev_zio_zio_2_13",
        "@maven//:dev_zio_zio_interop_cats_2_13",
        "@maven//:io_github_kitlangton_zio_magic_2_13",
        "@maven//:org_tpolecat_doobie_core_2_13",
    ],
)

scala_image(
    name = "image",
    binary = ":test-calls",
    jvm_flags = [
        "-Xms1g",
        "-Xmx2g",
    ],
    main_class = "ru.yandex.vertis.etc.telepony.telepony_test_calls.Main",
    service = "telepony-test-calls",
    visibility = ["//etc/dust:__subpackages__"],
)

shiva_service(
    name = "service",
    binary = ":test-calls",
    service_name = "telepony-test-calls",
)
