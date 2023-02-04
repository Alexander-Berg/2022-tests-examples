UNITTEST()

SIZE(MEDIUM)

OWNER(g:golovan)

REQUIREMENTS(
    network:full
)

TAG(
    ya:external
)

PEERDIR(
    infra/yasm/stockpile_client
    infra/yasm/stockpile_client/common
    library/cpp/logger
    mapreduce/yt/client
)

SRCS(
    resolving_ut.cpp
    reading_ut.cpp
    metabase_client_ut.cpp
    metabase_shard_provider_ut.cpp
)

REQUIREMENTS(ram:32)

END()
