PY2_PROGRAM(image_deploy_tester)

OWNER(
    frolstas
)

PEERDIR(
    infra/nanny/sepelib/core
    infra/qyp/image_deploy_tester/src
)

PY_SRCS(
    __main__.py
)

NO_CHECK_IMPORTS()

END()
