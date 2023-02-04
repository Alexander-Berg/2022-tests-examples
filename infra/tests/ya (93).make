PY3TEST()

OWNER(g:infractl)

TEST_SRCS(
    test.py
)

DEPENDS(
    infra/infractl/ci_tasklets/get_changelog
)
DATA(
    arcadia/infra/infractl/ci_tasklets/get_changelog/input.example.json
)

END()
