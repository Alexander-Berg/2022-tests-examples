load("//tools:defaults.bzl", "scala_library", "zio_test")

scala_library(
    name = "common",
    srcs = glob(["src/**/*.scala"]),
    visibility = ["//vertistraf:__subpackages__"],
    deps = [
        "//common/yt",
        "//common/yt:live",
        "//common/zio/app/base",
        "//common/zio/config",
        "//common/zio/logging",
        "//vertistraf/common",
        "@maven//:dev_zio_zio_test_2_13",
    ],
)

zio_test(
    name = "test",
    srcs = glob(["test/**/*.scala"]),
    resource_strip_prefix = package_name() + "/test/resources",
    resources = glob(["test/resources/**/*"]),
    deps = [
        ":common",
    ],
)
