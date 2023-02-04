load("//tools:defaults.bzl", "scala_library", "scala_test")

scala_library(
    name = "techsupport-test-util",
    srcs = glob([
        "src/main/**/*.scala",
        "src/main/**/*.java",
    ]),
    visibility = ["//vs-quality/techsupport:__subpackages__"],
    deps = [
        "//vs-quality/utils/cats_utils:cats-utils",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_scalacheck_scalacheck_2_13",
        "@maven//:org_scalatestplus_scalacheck_1_15_2_13",
        "@maven//:org_typelevel_cats_effect_2_13",
        "@maven//:ru_yandex_vertis_mockito_2x_2_13",
        "@maven//:ru_yandex_vertis_vertis_testkit_2_13",
    ],
)

scala_test(
    name = "tests",
    srcs = glob(["src/test/**/*.scala"]),
    visibility = ["//visibility:private"],
    deps = [
        ":techsupport-test-util",
    ],
)
