import dateutil.tz

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str, get_unix_timestamp


@skip_if_remote
def test_get_isoformat_str_utc():
    assert get_isoformat_str(1536579834, dateutil.tz.tzutc()) == "2018-09-10T11:43:54+00:00"


@skip_if_remote
def test_get_isoformat_str_berlin():
    assert get_isoformat_str(1536579834, dateutil.tz.gettz("Europe/Berlin")) == "2018-09-10T13:43:54+02:00"


@skip_if_remote
def test_get_unix_timestamp():
    assert get_unix_timestamp("2018-09-10T11:43:54+00:00") == 1536579834
