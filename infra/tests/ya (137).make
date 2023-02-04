PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_handled_dict.py
    test_handled_list.py
    test_handlers.py
    test_marshalling.py
    test_subclassing.py
)

PEERDIR(
    infra/reconf/util/handled
)

END()
