load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "hobo-test-util",
    srcs = glob(["src/**/*.scala"]),
    visibility = ["//vs-quality/hobo:__subpackages__"],
    deps = [
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_specs2_specs2_mock_2_13",
        "@maven//:ru_yandex_vertis_vertis_testkit_2_13",
    ],
)
