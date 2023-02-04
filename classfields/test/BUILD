load("//tools:defaults.bzl", "scala_binary", "scala_image", "scala_test")

exports_files(["test.txt"])

scala_binary(
    name = "test-app",
    srcs = ["TestDockerApp.scala"],
    jvm_flags = ["-Xmx128m"],
    main_class = "tools.docker.test.TestDockerApp",
)

scala_image(
    name = "test-image-base",
    binary = ":test-app",
)

scala_image(
    name = "test-image-jvm-flags",
    binary = ":test-app",
    jvm_flags = ["-Xmx129m"],
)

scala_image(
    name = "test-image-args",
    args = [
        "ab",
        "bc",
        "cd",
    ],
    binary = ":test-app",
)

scala_image(
    name = "test-image-main-class",
    binary = ":test-app",
    main_class = "tools.docker.test.AlternativeMain",
)

scala_image(
    name = "test-image-expand-args",
    args = ["$(location :test.txt)"],
    binary = ":test-app",
    data = [":test.txt"],
)

scala_image(
    name = "test-image-heap-dumps",
    binary = ":test-app",
    jvm_flags = ["-XX:HeapDumpPath=/alloc/custom"],
)

scala_test(
    name = "test",
    srcs = ["ScalaImageTest.scala"],
    data = [
        ":test-image-args",
        ":test-image-base",
        ":test-image-expand-args",
        ":test-image-heap-dumps",
        ":test-image-jvm-flags",
        ":test-image-main-class",
    ],
    deps = [
        "@bazel_tools//tools/java/runfiles",
    ],
)
