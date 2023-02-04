from datetime import datetime, timedelta

import pytest


@pytest.mark.regression
def test_get_fields_bics(refs_get):
    expected_fields = [
        'id', 'prev', 'next', 'eventType', 'eventDate', 'bic8', 'bicBranch', 'status', 'active', 'subtype',
        'instNameLegal', 'instName', 'instType', 'finConnected', 'fileActAvailable', 'interActAvailable',
        'entityType', 'countryName', 'countryCode', 'recordKey', 'branchInfo', 'locationCode', 'finServiceCodes',
        'addrRegStreet', 'addrRegStreetNumber', 'addrRegBuilding', 'addrRegArea', 'addrRegCity', 'addrRegRegion',
        'addrRegZip', 'addrRegPob', 'addrOpStreet', 'addrOpStreetNumber', 'addrOpBuilding', 'addrOpArea',
        'addrOpCity', 'addrOpRegion', 'addrOpZip', 'addrOpPob', 'addrBrStreet', 'addrBrStreetNumber',
        'addrBrBuilding', 'addrBrArea', 'addrBrCity', 'addrBrRegion', 'addrBrZip', 'addrBrPob'
    ]
    result = refs_get('/api/swift/?query={__type(name: "Event") {description fields {name description}}}')
    result_fields = [field['name'] for field in result['data']['__type']['fields']]
    assert set(expected_fields) == set(result_fields)

    result = refs_get('/api/swift/?query={bics(bic:["YNDMRUM1", "SECTAEA1710"]) {%(fields)s}}', fields=result_fields)
    assert not result.get('errors', None)
    assert set(result['data']['bics'][0].keys()) == set(result_fields)


@pytest.mark.regression
def test_result_bics(refs_get):
    expected_result = [
        {'id': 'BP0000004C4K', 'bic8': 'YNDMRUM1', 'active': True, 'countryName': 'Russian Federation'},
        {'id': 'BP000000I2A7', 'bic8': 'SECTAEA1', 'active': True, 'countryName': 'United Arab Emirates'}
    ]
    result = refs_get('/api/swift/?query={bics(bic:["YNDMRUM1", "SECTAEA1710"]) {id bic8 active countryName}}')
    assert result['data']['bics'] == expected_result


@pytest.mark.regression
def test_get_fields_holidays(refs_get):
    expected_fields = [
        'id', 'checksum', 'date', 'type', 'countryName', 'countryCode', 'hint', 'archived'
    ]
    result = refs_get('/api/swift/?query={__type(name: "Holiday") {description fields {name description}}}')
    result_fields = [field['name'] for field in result['data']['__type']['fields']]
    assert set(expected_fields) == set(result_fields)

    result = refs_get('/api/swift/?query={holidays(dateTo: "%s", country: "RU") {%s}}' % (
        (datetime.now() + timedelta(days=7)).strftime('%Y%m%d'), ' '.join(result_fields)
    ))
    assert not result.get('errors', None)
    assert set(result['data']['holidays'][0].keys()) == set(result_fields)


@pytest.mark.regression
def test_result_holidays(refs_get):
    result = refs_get('/api/swift/?query={holidays(dateTo: "%s", country: "RU") {id date countryName}}' % (
        (datetime.now() + timedelta(days=7)).strftime('%Y%m%d')
    ))
    assert not result.get('errors', None)
    assert len(result['data']['holidays']) >= 2
