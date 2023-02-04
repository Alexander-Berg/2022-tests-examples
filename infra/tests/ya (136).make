PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_complex_opts.py
    test_declarative.py
    test_handlers.py
    test_interface.py
    test_opt_factory.py
    test_pytest_helpers.py
    test_resolvers.py
    test_shared.py
    test_validation.py
)

PEERDIR(
    infra/reconf
    infra/reconf/pytest
)

END()
