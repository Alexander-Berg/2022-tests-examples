PROTO_LIBRARY()

OWNER(g:tasklet)

SRCS(
    example.proto
)

PEERDIR(
    tasklet/api/v2
    ci/tasklet/common/proto
)

END()
