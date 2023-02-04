OWNER(torkve)

PY2TEST()

SRCDIR(infra/netlibus/test_lib)

TEST_SRCS(test.py)

TIMEOUT(25)

DEPENDS(infra/netlibus/pymodule)

REQUIREMENTS(network:full)

END()
