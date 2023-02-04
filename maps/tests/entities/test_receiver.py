import pytest

from maps_adv.common.aiosup import InvalidReceiver, Receiver


def test_raises_for_empty_receiver():
    with pytest.raises(InvalidReceiver) as exc:
        Receiver()

    assert exc.value.args == (
        "At least one receiver parameter should be passed but no more than one.",
    )


def test_raises_if_several_params_in_receiver():
    with pytest.raises(InvalidReceiver) as exc:
        Receiver(uid="any_uid", uuid="any_uuid")

    assert exc.value.args == (
        "At least one receiver parameter should be passed but no more than one.",
    )


@pytest.mark.parametrize(
    "param, value",
    [
        ("uuid", "some_uuid"),
        ("did", "some_did"),
        ("uid", "some_uid"),
        ("tag", "some_tag"),
        ("yt", "some_yt"),
    ],
)
def test_returns_formatted_string_for_str(param, value):
    receiver = Receiver(**{param: value})

    assert str(receiver) == f"{param}: {value}"
