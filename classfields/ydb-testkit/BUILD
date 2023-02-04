load("//tools:defaults.bzl", "scala_library", "scala_test")

scala_library(
    name = "ydb-testkit",
    srcs = glob(["src/main/scala/**/*.scala"]),
    deps = [
        "//common/ydb-wrapper/ydb-container",
        "//common/ydb-wrapper/ydb-wrapper-zio",
        "@maven//:com_yandex_ydb_ydb_sdk_table",
    ],
)

scala_test(
    name = "tests",
    srcs = glob(["src/test/scala/**/*.scala"]),
    resources = glob(["src/test/resources/**"]),
    runtime_deps = [
        "//common/testcontainers/docker",
    ],
    deps = [
        ":ydb-testkit",
        "//common/ydb-wrapper/ydb-container",
        "@maven//:com_dimafeng_testcontainers_scala_scalatest_2_13",
        "@maven//:org_scalatest_scalatest_core_2_13",
        "@maven//:org_scalatest_scalatest_matchers_core_2_13",
        "@maven//:org_scalatest_scalatest_shouldmatchers_2_13",
    ],
)
