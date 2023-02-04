load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "promocoder-test",
    srcs = glob(["src/main/scala/**/*.scala"]),
    visibility = ["//billing/promocoder:__subpackages__"],
    deps = [
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:com_zaxxer_HikariCP",
        "@maven//:org_scalactic_scalactic_2_13",
        "@maven//:org_scalatest_scalatest_core_2_13",
        "@maven//:org_scalatest_scalatest_featurespec_2_13",
        "@maven//:org_scalatest_scalatest_matchers_core_2_13",
        "@maven//:org_scalatest_scalatest_shouldmatchers_2_13",
        "@maven//:org_scalatest_scalatest_wordspec_2_13",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_testcontainers_mysql",
    ],
)
