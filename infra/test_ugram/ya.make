FUZZ()

OWNER(
    osidorkin
    g:golovan
)

PEERDIR(
    infra/yasm/zoom/components/yasmconf
    infra/yasm/zoom/components/containers
    infra/yasm/zoom/components/serialization/deserializers
    infra/yasm/zoom/components/serialization/zoom_to_msgpack
    library/cpp/json
)

SRCS(
    main.cpp
)

END()
