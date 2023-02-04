import datetime as dt

import mongomock
import pytest
import pytz

from maps.garden.libs_server.common.param_storage import ParamStorage, ParameterName

NOW = dt.datetime(2020, 8, 25, 16, 20, 00, tzinfo=pytz.utc)


@pytest.mark.freeze_time(NOW)
def test_storage():
    db = mongomock.MongoClient(tz_aware=True).db
    storage = ParamStorage(db)

    assert not storage.get(ParameterName.LAST_OPERATION_FETCH_TIMESTAMP)
    assert storage.get(ParameterName.LAST_OPERATION_FETCH_TIMESTAMP, "") == ""
    with pytest.raises(KeyError):
        storage[ParameterName.LAST_OPERATION_FETCH_TIMESTAMP]

    storage[ParameterName.LAST_OPERATION_FETCH_TIMESTAMP] = str(int(dt.datetime.utcnow().timestamp()))
    assert storage[ParameterName.LAST_OPERATION_FETCH_TIMESTAMP] == "1598372400"
