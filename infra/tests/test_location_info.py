import mock

from infra.rtc.nodeinfo.lib.modules import location_info


def test_get_location_info():
    m = mock.Mock()
    m.return_value = {
        'raw_city': 'CITY_mock',
        'raw_datacenter': 'DC_mock',
        'raw_country': 'COUNTRY_mock',
        'raw_queue': 'QUEUE_mock',
        'raw_rack': 'RACK_mock',
    }, None
    info, err = location_info.get_location_info(load_fun=m)
    assert err is None
    assert info.rack == 'RACK_mock'
    assert info.line == 'QUEUE_mock'
    assert info.building == 'DC_mock'
    assert info.city == 'CITY_mock'
    assert info.country == 'COUNTRY_mock'
