load("//tools:defaults.bzl", "scala_binary", "scala_library", "zio_test")

scala_library(
    name = "utils",
    srcs = [
        "src/FeedTestUtils.scala",
        "src/TestCases.scala",
    ],
    deps = [
        "//common/zio/grpc:client",
        "//schema-registry/proto/general/feed:transformer_scala_proto",
        "@maven//:dev_zio_izumi_reflect_2_13",
        "@maven//:dev_zio_zio_2_13",
        "@maven//:dev_zio_zio_streams_2_13",
    ],
)

scala_binary(
    name = "regenerate_tests",
    srcs = ["src/FeedResultsGenerator.scala"],
    main_class = "ru.yandex.vertis.general.feed.transformer.integration_test.FeedResultsGenerator",
    resource_strip_prefix = package_name() + "/resources",
    resources = glob(["resources/**/*"]),
    deps = [
        ":utils",
        "//common/zio/app/base",
        "//common/zio/app/info",
        "//common/zio/clients/s3:live",
        "//common/zio/clients/s3:s3Client",
        "//common/zio/config",
        "//common/zio/files",
        "//common/zio/grpc:client",
        "//common/zio/logging",
        "//common/zio/ops/prometheus",
        "//common/zio/ops/tracing",
        "//common/zio/pureconfig",
        "//schema-registry/proto/general/feed:transformer_scala_proto",
        "@maven//:com_github_pureconfig_pureconfig_core_2_13",
        "@maven//:com_typesafe_config",
        "@maven//:commons_codec_commons_codec",
        "@maven//:dev_zio_izumi_reflect_2_13",
        "@maven//:dev_zio_zio_2_13",
        "@maven//:dev_zio_zio_streams_2_13",
    ],
)

zio_test(
    name = "integration_test",
    srcs = ["src/FeedTransformationTest.scala"],
    resource_strip_prefix = package_name() + "/resources",
    resources = glob(["resources/**/*"]),
    tags = ["manual"],
    deps = [
        ":utils",
        "//common/zio/clients/s3:live",
        "//common/zio/pureconfig",
    ],
)
