load("//tools:defaults.bzl", "scala_library", "scala_test")

scala_library(
    name = "billing-test",
    srcs = glob(["src/main/scala/**/*.scala"]),
    resource_strip_prefix = package_name() + "/src/main/resources",
    resources = glob(["src/main/resources/**/*"]),
    visibility = ["//billing/billing:__subpackages__"],
    runtime_deps = [
        "//common/testcontainers/docker",
        "@maven//:mysql_mysql_connector_java",
    ],
    deps = [
        "@maven//:com_typesafe_akka_akka_actor_2_13",
        "@maven//:com_typesafe_akka_akka_stream_2_13",
        "@maven//:com_typesafe_config",
        "@maven//:io_prometheus_simpleclient",
        "@maven//:org_scalatest_scalatest_core_2_13",
        "@maven//:org_scalatest_scalatest_matchers_core_2_13",
        "@maven//:org_scalatest_scalatest_shouldmatchers_2_13",
        "@maven//:org_scalatest_scalatest_wordspec_2_13",
        "@maven//:org_scalatestplus_scalacheck_1_15_2_13",
        "@maven//:ru_yandex_vertis_monitoring_2_13",
    ],
)
