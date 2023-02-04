UNITTEST(ServiceControllerDaemon)

OWNER(
    g:yp-controller-lib
    g:yp-sc
)

SIZE(MEDIUM)

DEPENDS(
    infra/service_controller/daemons/service_controller
)

SRCS(
    service_controller_test_binary_size.cpp
)

END()
