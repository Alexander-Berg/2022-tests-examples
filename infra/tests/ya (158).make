UNITTEST(resource_cache_controller_ut)

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/resource_cache_controller/libs/resource_cache_controller
)

ADDINCL(
    infra/resource_cache_controller/libs/resource_cache_controller
)

SRCS(
    resource_cache_controller_ut.cpp
)

END()
