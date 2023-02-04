import pytest

from maps_adv.common.aiosup import Data, InvalidData, PushAction


@pytest.mark.parametrize("push_id", [None, ""])
def test_raises_for_invalid_push_id(push_id):
    with pytest.raises(InvalidData) as exc:
        Data(push_id=push_id)

    assert exc.value.args == ("Push_id is required.",)


@pytest.mark.parametrize("push_uri", [None, ""])
def test_raises_for_invalid_uri_if_push_action_is_uri(push_uri):
    with pytest.raises(InvalidData) as exc:
        Data(push_id="any_push_id", push_action=PushAction.URI, push_uri=push_uri)

    assert exc.value.args == ("Uri is not specified.",)


@pytest.mark.parametrize("push_uri", [None, ""])
def test_does_not_raise_for_empty_uri_if_push_action_is_morda(push_uri):
    data = Data(push_id="any_push_id", push_action=PushAction.MORDA, push_uri=push_uri)

    assert data.push_uri == push_uri


@pytest.mark.parametrize(
    "values", [dict(), dict(transit_id="any_transit_id", stat_id="any_stat_id")]
)
def test_returns_only_passed_params_as_dict(values):
    data = Data(push_id="any_push_id", **values)

    assert data.to_dict() == dict(push_id="any_push_id", **values)


@pytest.mark.parametrize(
    "push_action, push_action_value",
    [(PushAction.URI, "uri"), (PushAction.MORDA, "morda")],
)
def test_composes_dict_correctly(push_action, push_action_value):
    data = Data(
        push_id="any_push_id",
        transit_id="any_transit_id",
        content_id="any_content_id",
        stat_id="any_stat_id",
        push_uri="any_push_uri",
        push_action=push_action,
        any_other="extra_params",
        may_be="passed_too",
    )

    assert data.to_dict() == dict(
        push_id="any_push_id",
        transit_id="any_transit_id",
        content_id="any_content_id",
        stat_id="any_stat_id",
        push_uri="any_push_uri",
        push_action=push_action_value,
        any_other="extra_params",
        may_be="passed_too",
    )
