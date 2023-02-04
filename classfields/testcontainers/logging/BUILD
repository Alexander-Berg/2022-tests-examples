load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "logging",
    srcs = glob(["src/**/*.scala"]),
    visibility = [
        "//etc-mono:__subpackages__",
        "//etc/telepony/telepony-dao:__subpackages__",
    ],
    deps = [
        "//common/zio/logging",
        "@maven//:org_testcontainers_testcontainers",
    ],
)
