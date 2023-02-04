PY2TEST()

OWNER(
    frolstas
    i-dyachkov
)

TEST_SRCS(
    conftest.py
    test_process.py
    test_qemu_ctl.py
    test_qemu_launcher.py
    test_resource_manager.py
    test_volume_manager.py
    test_vmagent_context.py
    test_vmworker.py
)

PEERDIR(
    contrib/python/mock
    contrib/python/Flask
    contrib/python/freezegun
    contrib/python/requests-unixsocket
    contrib/python/waitress

    yt/yt/python/yt_yson_bindings
    yp/python/client

    infra/qyp/vmagent/src

)
DATA(
    arcadia/infra/qyp/vmagent/tests/qemu_launcher_expected
)

NO_CHECK_IMPORTS()

END()
