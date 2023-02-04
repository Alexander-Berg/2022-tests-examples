# -*- coding: utf-8 -*-

import pytest

from butils.dbhelper.signals import db_ping
from tests.conftest import create_session


@pytest.fixture()
def record_ping():
    recorded = []

    def record(sender, ping_duration, database_id, **kwargs):
        recorded.append(
            (ping_duration, database_id)
        )

    db_ping.connect(record)
    yield recorded
    db_ping.disconnect(record)


@pytest.fixture
def recorded_session(app, request, record_ping):
    with create_session(request, app) as s:
        yield s, record_ping


def test_db_ping(recorded_session):
    """
    Во время создания сессии происходит ping на уровне cx_oracle.Connection,
     который мы и записываем
    """
    _, recorded = recorded_session

    assert len(recorded) == 1
