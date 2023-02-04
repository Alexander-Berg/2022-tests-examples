import mock

from infra.ya_salt.proto import ya_salt_pb2
from infra.rtc.nodeinfo.lib.modules import overrides


def test_load_overrides_dict():
    expected = {"networkInfo": {"bandwidth": 1000}}
    m = mock.mock_open(read_data='{"networkInfo": {"bandwidth": 1000}}')
    d, err = overrides.load_overrides_dict(open_func=m, isfile=lambda x: True)
    assert err is None
    assert d == expected
    m.assert_called_once_with(overrides.OVERRIDES_FILE_PATH, 'r')


def test_load_overrides_dict_absent():
    m = mock.mock_open(read_data='{"networkInfo": {"bandwidth": 1000}}')
    d, err = overrides.load_overrides_dict(open_func=m, isfile=lambda x: False)
    assert err is None
    assert d is None
    m.assert_not_called()


def test_overrides_from_dict():
    d = {"networkInfo": {"bandwidth": 1000}}
    ni, err = overrides.overrides_from_dict(d)
    assert err is None
    assert ni.network_info.bandwidth == 1000


def test_apply_overrides():
    ni = ya_salt_pb2.NodeInfo()
    ni.network_info.bandwidth = 10000
    m = mock.mock_open(read_data='{"networkInfo": {"bandwidth": 1000}}')
    overrides.apply_overrides(ni, open_func=m, isfile=lambda x: True)
    assert ni.network_info.bandwidth == 1000
