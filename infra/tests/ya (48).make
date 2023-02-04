PY3TEST()

OWNER(g:cores)

TEST_SRCS(
    test_tag_parser.py
    test_strings.py
)

PEERDIR(
    infra/cores/app
)

END()
