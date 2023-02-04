PY2TEST()

OWNER(g:hostman)

TEST_SRCS(
    test_context.py
    test_fileutil.py
    test_grains_gencfg.py
    test_grub.py
    test_hashutil.py
    test_httpsalt.py
    test_init_manager.py
    test_initial_setup.py
    test_jugglerutil.py
    test_kernel_man.py
    test_modutil.py
    test_package_manager.py
    test_packages.py
    test_pbutil.py
    test_persist.py
    test_policy.py
    test_power_man.py
    test_reboots.py
    test_runutil.py
    test_rusage.py
    test_saltutil.py
    test_statusutil.py
    test_subprocutil.py
    test_walle.py
    test_yamlutil.py
    test_reporter_monitoring.py
    test_hostctl.py
    test_envutil.py
)

DATA(
    arcadia/infra/ya_salt/lib/tests/test_grub_precise.cfg
    arcadia/infra/ya_salt/lib/tests/test_grub_precise2.cfg
    arcadia/infra/ya_salt/lib/tests/test_grub_xenial.cfg
    arcadia/infra/ya_salt/lib/tests/gencfg_response.json
    arcadia/infra/ya_salt/lib/tests/minion_config.json
)

PEERDIR(
    contrib/python/mock
    infra/ya_salt/lib
)

NO_CHECK_IMPORTS(

)

END()
