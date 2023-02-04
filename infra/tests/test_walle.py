from infra.ya_salt.lib import walle

TEST_RESPONSE_NON_MSK = {
    "location": {
        "city": "SAS",
        "country": "RU",
        "datacenter": "SASTA",
        "network_source": "lldp",
        "network_timestamp": 1573658835,
        "physical_timestamp": 1498068423,
        "port": "ge1/0/2",
        "queue": "SAS-1.2.1",
        "rack": "6",
        "short_datacenter_name": "sas",
        "short_queue_name": "sas1.2.1",
        "switch": "sas1-s135"
    },
    "project": "rtc-all-dynamic",
    "tags": [
        "rtc",
        "rtc.automation-enabled",
        "rtc.scheduler-gencfg",
        "rtc.stage-production",
        "rtc_network",
        "runtime",
        "search",
        "skynet_installed",
        "yasm_monitored"
    ]
}
TEST_RESPONSE_MSK = {
    "location": {
        "city": "IVA",
        "country": "RU",
        "datacenter": "IVNIT",
        "network_source": "lldp",
        "network_timestamp": 1573659322,
        "physical_timestamp": 1498574961,
        "port": "ge1/0/28",
        "queue": "IVA-4",
        "rack": "72",
        "short_datacenter_name": "iva",
        "short_queue_name": "iva4",
        "switch": "iva4-s72"
    },
    "project": "rtc-all-dynamic",
    "tags": [
        "rtc",
        "rtc.automation-enabled",
        "rtc.scheduler-gencfg",
        "rtc.stage-production",
        "rtc_network",
        "runtime",
        "search",
        "skynet_installed",
        "yasm_monitored"
    ]
}


def test_get_info_from_resp_non_msk():
    info = walle.info_from_http(TEST_RESPONSE_NON_MSK)

    assert info['raw_country'] == 'RU'
    assert info['raw_queue'] == 'SAS-1.2.1'
    assert info['raw_city'] == 'SAS'
    assert info['raw_datacenter'] == 'SASTA'
    assert info['raw_rack'] == '6'

    assert info['dc'] == 'sas'
    assert info['project'] == 'rtc-all-dynamic'
    assert info['country'] == 'ru'
    assert info['tags'] == TEST_RESPONSE_NON_MSK['tags']
    assert info['queue'] == 'sas1.2.1'
    assert info['rack'] == '6'
    assert info['switch'] == 'sas1-s135'
    assert info['location'] == 'sas'


def test_get_info_from_resp_msk():
    info = walle.info_from_http(TEST_RESPONSE_MSK)

    assert info['raw_country'] == 'RU'
    assert info['raw_queue'] == 'IVA-4'
    assert info['raw_city'] == 'IVA'
    assert info['raw_datacenter'] == 'IVNIT'
    assert info['raw_rack'] == '72'

    assert info['dc'] == 'iva'
    assert info['project'] == 'rtc-all-dynamic'
    assert info['country'] == 'ru'
    assert info['tags'] == TEST_RESPONSE_MSK['tags']
    assert info['queue'] == 'iva4'
    assert info['rack'] == '72'
    assert info['switch'] == 'iva4-s72'
    assert info['location'] == 'msk'


def test_walle_grains_non_msk():
    w, err = walle.grains('test-non-msk-host',
                          load_fun=lambda: (walle.info_from_http(TEST_RESPONSE_NON_MSK), None))
    assert err is None
    assert w['walle_project'] == 'rtc-all-dynamic'
    assert w['walle_location'] == 'sas'
    assert w['walle_dc'] == 'sas'
    assert w['walle_country'] == 'ru'
    assert w['walle_queue'] == 'sas1.2.1'
    assert w['walle_rack'] == '6'
    assert w['walle_switch'] == 'sas1-s135'
    assert w['location'] == 'sas'
    assert w['walle_tags'] == TEST_RESPONSE_NON_MSK['tags']


def test_walle_grains_msk():
    w, err = walle.grains('test-non-msk-host',
                          load_fun=lambda: (walle.info_from_http(TEST_RESPONSE_MSK), None))
    assert w['walle_project'] == 'rtc-all-dynamic'
    assert w['walle_location'] == 'msk'
    assert w['walle_dc'] == 'iva'
    assert w['walle_country'] == 'ru'
    assert w['walle_queue'] == 'iva4'
    assert w['walle_rack'] == '72'
    assert w['walle_switch'] == 'iva4-s72'
    assert w['location'] == 'msk'
    assert w['walle_tags'] == TEST_RESPONSE_MSK['tags']


# A table of info patches (because duplicating all fields can be exhausting).
INFO_TABLE = [
    ({}, 'no or empty project'),
    ({'project': 0}, "project '0' is not a string"),
    ({'project': 'rtc', 'tags': []}, 'no or empty tags'),
    ({'tags': 1}, 'tags is not a list'),
    ({'tags': ['rtc', 1]}, "tag '1' at pos 1 is not a string"),
    ({'tags': ['']}, 'empty tag at pos 0'),
    ({'tags': ['rtc'], 'location': ''}, 'empty location'),
    ({'location': 2}, "location '2' is not a string"),
    ({'location': 'sas'}, "no switch"),
    ({'switch': ['sas1-628s']}, "switch '['sas1-628s']' is not a string"),
    ({'switch': 'sas1-628s'}, None),
]


def test_validate_info():
    fun = walle.validate_info
    info = {}
    for upd, err in INFO_TABLE:
        info.update(upd)
        got = fun(info)
        if err is None:
            assert got is None, info
        else:
            assert got == err, info
