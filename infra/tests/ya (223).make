PY2TEST()

OWNER(g:runtime-infra)

SIZE(MEDIUM)

TEST_SRCS(
    conftest.py
    walletest.py
    test_abc.py
    test_automation.py
    test_certificate.py
    test_check_schemas.py
    test_cms.py
    test_deploy.py
    test_dns_domain.py
    test_generic.py
    test_gpu.py
    test_limits.py
    test_owners.py
    test_profile.py
    test_reboot_segment.py
    test_reboot_via_ssh.py
    test_reports.py
    test_roles.py
    test_schedulers.py
    test_setup.py
    test_tags.py
    test_vlan_scheme.py
    test_writer.py
    test_yasm.py
    test_yp.py
    test_yt_cluster.py
    test_utils.py
)

PEERDIR(
    contrib/python/packaging
    infra/rtc/walle_validator/lib
    infra/rtc/walle_validator/dto
    infra/wall-e/sdk
)

DATA(
    arcadia/infra/rtc/walle_validator/projects/configs
    arcadia/infra/rtc/walle_validator/projects/auxiliaries
    arcadia/infra/orly/rules
    arcadia/infra/maxwell/specs
)

END()
