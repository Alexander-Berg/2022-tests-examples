PY3TEST()

OWNER(
    staroverovad
    g:deploy
)

TEST_SRCS(
    test_roles_behaviour.py
)

SIZE(LARGE)

PEERDIR(
    infra/dctl/src
    library/python/resource
)

RESOURCE(
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/approval_policy_spec.yaml approval_policy_spec.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/deploy_ticket_spec.yaml   deploy_ticket_spec.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/project_spec.yaml         project_spec.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/release_rule_spec.yaml    release_rule_spec.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/release_spec.yaml         release_spec.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/stage_spec_man_pre.yaml   stage_spec_man_pre.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/stage_spec_sas_test.yaml  stage_spec_sas_test.yaml
    infra/auth_controller/it/scripts/roles_behaviour_tests/data/stage_spec_xdc.yaml       stage_spec_xdc.yaml
)

TAG(
    ya:external
    ya:fat
    ya:force_sandbox
    ya:huge_logs
    ya:not_autocheck
    ya:norestart
    ya:noretries
)

END()

