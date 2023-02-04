OWNER(torkve)

PY3TEST()

SRCDIR(infra/netlibus/test_lib)

TEST_SRCS(test.py)

TIMEOUT(25)

DEPENDS(infra/netlibus/py3module)

REQUIREMENTS(network:full)

END()
