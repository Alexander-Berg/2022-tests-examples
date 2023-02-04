import pytest
from infra.yasm.gateway.lib.handlers import meta_alert

from tornado import web


def test_raises_bad_request_for_bad_format():
    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input([])
    assert exc_info.value.status_code == 400

    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input({})
    assert exc_info.value.status_code == 400

    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input({"meta-alert": "oh, no"})
    assert exc_info.value.status_code == 400

    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input({"meta-alert": [[]]})
    assert exc_info.value.status_code == 400

    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input(
            {"meta-alert": [{"name": "alert1"}, {"itype": "itype1"}], "without_mgroups": True}
        )
    assert exc_info.value.status_code == 400


def test_raises_bad_request_for_old_format():
    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input({"meta-alert": ["alert1", "alert2"], "without_mgroups": True})
    assert exc_info.value.status_code == 400

    with pytest.raises(web.HTTPError) as exc_info:
        meta_alert.parse_meta_alert_input({"meta-alert": {"ASEARCH": ["alert1", "alert2"]}})
    assert exc_info.value.status_code == 400


def test_parses_new_format():
    alerts = meta_alert.parse_meta_alert_input(
        {"meta-alert": [{"name": "a1", "itype": "i1"}, {"name": "a2", "itype": "i2"}], "without_mgroups": True}
    )
    assert alerts == [meta_alert.AlertID(name="a1", itype="i1"), meta_alert.AlertID(name="a2", itype="i2")]

    alerts = meta_alert.parse_meta_alert_input(
        {"meta-alert": [{"name": "a1", "itype": "i1"}, {"name": "a2", "itype": "i2"}]}
    )
    assert alerts == [meta_alert.AlertID(name="a1", itype="i1"), meta_alert.AlertID(name="a2", itype="i2")]
