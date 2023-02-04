import pytest

from maps_adv.common.aiosup import InvalidNotification, Notification


def test_raises_for_invalid_notification():
    with pytest.raises(InvalidNotification) as exc:
        Notification()

    assert exc.value.args == ("At least one notification parameter should be passed",)


@pytest.mark.parametrize(
    "values",
    [
        dict(title="some_title"),
        dict(body="some_body", link="some_link"),
        dict(
            title="some_title",
            body="some_body",
            link="some_link",
            icon="some_icon",
            iconId="some_iconId",
        ),
    ],
)
def test_returns_only_passed_params_as_dict(values):
    notification = Notification(**values)

    assert notification.to_dict() == values
