load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "testkit",
    srcs = glob(["src/**/*.scala"]),
    visibility = ["//auto/base/catalog:__subpackages__"],
    deps = [
        "//auto/base/catalog/core",
        "@maven//:com_google_guava_guava",
        "@maven//:org_scalatest_scalatest_core_2_13",
        "@maven//:org_scalatest_scalatest_shouldmatchers_2_13",
        "@maven//:org_scalatest_scalatest_wordspec_2_13",
        "@maven//:ru_yandex_vertis_mockito_2x_2_13",
    ],
)
