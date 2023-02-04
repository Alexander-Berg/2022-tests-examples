import pytest  # noqa: F401


@pytest.fixture(autouse=True)
def duty_role(duty_role):
    return duty_role
