import pytest

from maps_adv.common.aiosup import InvalidThrottlePolicies, ThrottlePolicies


def test_raises_for_invalid_init_params():
    with pytest.raises(InvalidThrottlePolicies) as exc:
        ThrottlePolicies()

    assert exc.value.args == ("At least one policy parameter must be set.",)


@pytest.mark.parametrize(
    "values",
    [
        dict(install_id="id_1"),
        dict(install_id="id_1", device_id="id_2"),
        dict(install_id="id_1", device_id="id_2", content_id="id_3"),
    ],
)
def test_returns_as_dict_only_passed_params(values):
    policies = ThrottlePolicies(**values)

    assert policies.to_dict() == values
