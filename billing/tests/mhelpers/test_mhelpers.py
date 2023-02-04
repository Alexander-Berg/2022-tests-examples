from butils.mhelpers import mhelpers

import datetime

import pytest

FAKE_TIME = datetime.datetime.utcnow()


@pytest.fixture
def patch_datetime_now(monkeypatch):

    class mydatetime(datetime.datetime):
        @classmethod
        def utcnow(cls):
            return FAKE_TIME

    monkeypatch.setattr(datetime, 'datetime', mydatetime)


class TestBaseDocument(object):
    def test_to_mongo_default(self, patch_datetime_now):
        # arrange
        doc = mhelpers.BaseDocument()

        # act
        res = doc.to_mongo(use_db_field=True, fields=None)

        # assert
        assert res['dt'] == FAKE_TIME
        assert res['update_dt'] == FAKE_TIME
