load("//tools:defaults.bzl", "scala_binary")

scala_binary(
    name = "multipostingPlayground",
    srcs = ["MultipostingPlayground.scala"],
    main_class = "auto.dealers.loyalty.storage.test.manualPlayground.MultipostingPlayground",
    deps = [
        "//auto/dealers/loyalty/storage:clients",
        "//common/zio/app/base",
        "//common/zio/grpc:client",
    ],
)

scala_binary(
    name = "palmaPlayground",
    srcs = ["PalmaPlayground.scala"],
    main_class = "auto.dealers.loyalty.storage.test.manualPlayground.PalmaPlayground",
    deps = [
        "//auto/dealers/loyalty/storage:clients",
        "//common/zio/app/base",
        "//common/zio/grpc:client",
    ],
)
