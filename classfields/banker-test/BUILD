load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "banker-test",
    srcs = glob(["src/main/scala/**/*.scala"]),
    resource_strip_prefix = package_name() + "/src/main/resources",
    resources = glob(["src/main/resources/**/*"]),
    visibility = ["//billing/banker:__subpackages__"],
    runtime_deps = [
        "//common/testcontainers/docker",
        "@maven//:mysql_mysql_connector_java",
    ],
    deps = [
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:com_typesafe_config",
        "@maven//:com_typesafe_slick_slick_2_13",
        "@maven//:org_apache_commons_commons_dbcp2",
        "@maven//:org_scalatest_scalatest_core_2_13",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_testcontainers_mysql",
    ],
)
