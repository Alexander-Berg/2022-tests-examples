load("//tools:defaults.bzl", "scala_test")

scala_test(
    name = "test",
    size = "large",
    srcs = glob(["src/test/scala/**/*.scala"]),
    jvm_flags = [
        # тесты зависят от московской таймзоны.
        "-Duser.timezone=\"Europe/Moscow\"",
        # workaround for kryo https://github.com/EsotericSoftware/kryo/issues/859
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        # workaround for kryo https://github.com/EsotericSoftware/kryo/issues/859
    ],
    resource_strip_prefix = package_name() + "/src/test/resources",
    resources = glob(["src/test/resources/**/*"]),
    runtime_deps = [
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:com_typesafe_akka_akka_testkit_2_13",
        "@maven//:org_codehaus_janino_janino",
    ],
    deps = [
        "//billing/billing/billing-dao:testkit",
        "//billing/billing/billing-indexer/logic",
        "//billing/billing/billing-test",
        "//billing/billing/billing-tms/logic",
    ],
)
