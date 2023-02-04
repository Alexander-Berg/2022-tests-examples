PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    dmc/conftest.py
    dmc/test_automation_settings.py
    dmc/test_decision_making.py
    dmc/test_event_handling.py
    dmc/test_limits.py
    dmc/test_manual_checks_disable.py
    rules/conftest.py
    rules/test_configurable_rules.py
    rules/test_escalation.py
    rules/test_hw_watcher_rules.py
    rules/test_limits.py
    rules/test_rules.py
    rules/test_utils.py
    conftest.py
    test_checks.py
    test_failure_log.py
    test_failed_checks_priority.py
    test_failure_reasons.py
    test_netmon.py
    test_rack_topology.py
    test_screening.py
    test_triage.py
    test_triage_decision_selection.py
)

PY_SRCS(
    dmc/util.py
    rules/util.py
    util.py
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

IF (NOT NO_FORK_TESTS)
    FORK_SUBTESTS(MODULO)
    SPLIT_FACTOR(4)
ENDIF()

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()
