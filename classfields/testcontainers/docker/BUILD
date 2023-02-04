load("//tools:defaults.bzl", "scala_library")

scala_library(
    name = "docker",
    srcs = [
        "DockerContextProviderStrategy.scala",
        "RemoteDockerProviderStrategy.scala",
        "SshProviderStrategy.scala",
    ],
    resource_strip_prefix = package_name(),
    resources = ["META-INF/services/org.testcontainers.dockerclient.DockerClientProviderStrategy"],
    deps = [
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_module_jackson_module_scala_2_13",
        "@maven//:org_testcontainers_testcontainers",
    ],
)
