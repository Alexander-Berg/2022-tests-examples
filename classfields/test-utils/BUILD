load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "test-utils",
    srcs = glob(["src/main/scala/**/*.scala"]),
    resource_strip_prefix = package_name() + "/src/main/resources",
    resources = glob(["src/main/resources/**/*"]),
    visibility = ["//baker/shark:__subpackages__"],
    deps = [
        "//baker/common/zio-baker-common",
        "//common/zio/clients/s3:testkit",
        "@maven//:com_softwaremill_common_tagging_2_13",
        "@maven//:dev_zio_zio_2_13",
        "@maven//:dev_zio_zio_test_2_13",
        "@maven//:io_github_embeddedkafka_embedded_kafka_2_13",
    ],
)
