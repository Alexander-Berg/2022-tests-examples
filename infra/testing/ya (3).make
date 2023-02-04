LIBRARY()

OWNER(
    g:yp-sd
    g:yp-dns
)

PEERDIR(
    infra/libs/yp_replica

    library/cpp/string_utils/base64
)

SRCS(
    decode.cpp
    encode.cpp
    testing_storage.cpp
)

END()
