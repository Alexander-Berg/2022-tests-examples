import json
from datetime import timedelta

from hamcrest import assert_that, equal_to, less_than

from yb_darkspirit import scheme
from yb_darkspirit.api.schemas import WhitespiritSchema


NEW_VERSION = '1.0.666'


def _request_post_whitespirit(test_client, url, **data):
    response = test_client.post(
        "/v1/whitespirits",
        json=dict(url=url, **data),
    )
    return json.loads(response.get_data())


def test_post_whitespirit_updates_provided_field_value(test_client, session, ws_url):
    resp_data = _request_post_whitespirit(test_client, url=ws_url, version=NEW_VERSION)
    assert_that(resp_data['version'], equal_to(NEW_VERSION))


def test_post_whitespirit_preserves_unprovided_field_value(test_client, session, ws_url):
    before_request_ws = session.query(scheme.WhiteSpirit).get(ws_url)
    before_version = before_request_ws.version
    session.flush([before_request_ws])
    resp_data = _request_post_whitespirit(test_client, url=ws_url)

    assert_that(resp_data['version'], equal_to(before_version))


def test_post_whitespirit_internally_updates_last_active_dt(test_client, session, ws_url):
    ws = session.query(scheme.WhiteSpirit).get(ws_url)
    ws.last_active_dt -= timedelta(minutes=1)
    session.flush([ws])
    # Have to copy it since `session` object is shared with application code and after request
    # `before_request_ws` state will be updated via linked shared session object
    before_request_dt = ws.last_active_dt

    _request_post_whitespirit(test_client, url=ws_url, version=NEW_VERSION)

    assert_that(before_request_dt, less_than(ws.last_active_dt))


def test_post_whitespirit_commits_in_db(test_client, session, ws_url):
    resp_data = _request_post_whitespirit(test_client, url=ws_url, version=NEW_VERSION)

    database_data = WhitespiritSchema().dump(
        session.query(scheme.WhiteSpirit).get(ws_url)
    ).data
    assert_that(resp_data, equal_to(database_data))
