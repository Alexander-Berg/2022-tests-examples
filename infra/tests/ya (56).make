PY3TEST()

OWNER(torkve reddi alonger)

INCLUDE(../../../yp/python/ya_programs.make.inc)

PEERDIR(
    infra/libs/local_yp
    contrib/python/pytest
    contrib/python/mock
    contrib/python/pytest-asyncio

    infra/deploy_notifications_controller/lib
)

TEST_SRCS(
    conftest.py
    test_dict_wrapper.py
    test_looper.py
    test_stage_history_create.py
    test_stage_history_remove.py
    test_stage_history_update.py
    test_stage_history_update_inner.py
    test_stage_history_update_with_deploy.py
    test_tvm_ticket_renewer.py
    test_tvm_ticket_renewer_cached.py
)

REQUIREMENTS(
    cpu:4
    ram_disk:4
)
TIMEOUT(600)
SIZE(MEDIUM)

END()
