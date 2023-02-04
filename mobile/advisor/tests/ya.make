PY2TEST()

INCLUDE(${ARCADIA_ROOT}/yaphone/advisor/tests/recipe.inc)

PY_SRCS(
    fixtures.py
)

TEST_SRCS(
    views/__init__.py
    views/device.py
    views/order.py

    __init__.py
    impression_id.py
    lbs.py
    locale.py
    profile.py
)

END()
