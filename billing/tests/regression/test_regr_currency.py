from decimal import Decimal

import pytest


@pytest.mark.regression
def test_get_fields(refs_get):
    expected_fields = [
        'id', 'dateAdded', 'dateUpdated', 'source', 'date', 'fromCode', 'fromAmount', 'toCode', 'toAmount', 'buy', 'sell',
        'rateDir', 'rateInv'
    ]
    result = refs_get('/api/currency/?query={__type(name: "Rate") {description fields {name description}}}')
    result_fields = [field['name'] for field in result['data']['__type']['fields']]
    assert set(expected_fields) == set(result_fields)

    result = refs_get(
        '/api/currency/?query={rates (fromCode:["USD"], toCode:["CNY"]) {%(fields)s}}',
        fields=result_fields)
    assert not result.get('errors', None)
    assert set(result['data']['rates'][0].keys()) == set(result_fields)


@pytest.mark.regression
@pytest.mark.parametrize('from_code', ['USD', 'EUR'])
def test_get_fixer(from_code, refs_get):
    currency_list = [
        'DZD', 'NAD', 'GHS', 'EGP', 'BGN', 'PAB', 'PHP', 'XBD', 'BOB', 'XBA', 'DKK', 'XBC', 'XBB', 'BWP',
        'LBP', 'TZS', 'VND', 'AOA', 'CLP', 'KHR', 'QAR', 'KYD', 'LYD', 'UAH', 'JOD', 'AWG', 'SAR', 'XPT',
        'HKD', 'CHE', 'CHF', 'GIP', 'MRU', 'BYR', 'CDF', 'XPD', 'BYN', 'XCP', 'XAL', 'BOV', 'HRK', 'DJF',
        'FJD', 'THB', 'XAF', 'BND', 'ISK', 'UYU', 'NIO', 'LAK', 'SYP', 'MAD', 'UYI', 'MZN', 'YER', 'ZAR',
        'NPR', 'ZWL', 'XSU', 'NGN', 'CRC', 'AED', 'GBP', 'MWK', 'LKR', 'PKR', 'HUF', 'BMD', 'LSL', 'MNT',
        'AMD', 'ETB', 'UGX', 'XDR', 'STN', 'JMD', 'GEL', 'SHP', 'AFN', 'SBD', 'KPW', 'MKD', 'TRY', 'BDT',
        'XUA', 'GGP', 'HTG', 'SLL', 'MGA', 'ANG', 'LRD', 'XCD', 'NOK', 'MXV', 'MOP', 'SSP', 'INR', 'MXN',
        'CZK', 'TJS', 'BTC', 'BTN', 'COP', 'MYR', 'TMT', 'MUR', 'IDR', 'HNL', 'XPF', 'SZL', 'VUV', 'PEN',
        'BZD', 'CHW', 'ILS', 'DOP', 'TWD', 'MDL', 'BSD', 'SEK', 'ZMK', 'MVR', 'VES', 'SRD', 'CUP', 'CLF',
        'BBD', 'KMF', 'KRW', 'GMD', 'VEF', 'GTQ', 'CUC', 'CVE', 'ZMW', 'EUR', 'ALL', 'RWF', 'KZT', 'RUB',
        'XFU', 'XAG', 'TTD', 'OMR', 'BRL', 'MMK', 'PLN', 'PYG', 'KES', 'SVC', 'USD', 'AZN', 'USN', 'TOP',
        'JEP', 'GNF', 'WST', 'IQD', 'ERN', 'BAM', 'SCR', 'AUD', 'GYD', 'KWD', 'BIF', 'PGK', 'SOS', 'CAD',
        'SGD', 'UZS', 'STD', 'IRR', 'CNY', 'XOF', 'TND', 'MRO', 'NZD', 'FKP', 'LVL', 'USS', 'KGS', 'ARS',
        'RON', 'COU', 'IMP', 'RSD', 'BHD', 'LTL', 'JPY', 'SDG', 'XAU'
    ]
    not_found_curr = [
        'VES', 'MRU', 'STN', 'SSP', 'XUA', 'XBA', 'XBB', 'XBC', 'XBD', 'XSU', 'BOV', 'COU', 'MXV', 'USN',
        'USS', 'UYI', 'CHE', 'CHW', 'XFU', 'XPD', 'XPT', 'XAL', 'XCP'
    ]
    result = refs_get(
        '/api/currency/?query={rates (fromCode:["%(from)s"], source:["FIXER"]) '
        '{fromCode fromAmount toCode toAmount source}}' % {'from': from_code}
    )
    assert len(result['data']['rates']) == 168
    assert set(currency_list).difference(set(not_found_curr)) == set(
        [curr_data['toCode'] for curr_data in result['data']['rates']])


@pytest.mark.regression
def test_get_fixer_ils(refs_get):
    result = refs_get(
        '/api/currency/?query={rates (fromCode:["ILS"], source:["FIXER"]) '
        '{fromCode fromAmount toCode toAmount source}}')
    assert len(result['data']['rates']) == 1
    assert 'RUB' in [curr_data['toCode'] for curr_data in result['data']['rates']]


@pytest.mark.regression
def test_get_china_source(refs_get):
    fields = [
        'id', 'dateAdded', 'source', 'date', 'fromCode', 'fromAmount', 'toCode', 'toAmount', 'buy', 'sell',
        'rateDir', 'rateInv'
    ]

    result = refs_get('/api/currency/?query={rates (source:"CHN") {%(fields)s}}', fields=fields)
    assert not result.get('errors', None)
    assert result['data']['rates'][0]['toCode'] == 'CNY'
    assert Decimal(result['data']['rates'][0]['fromAmount']) == Decimal(100)


@pytest.mark.regression
def test_get_uzb_source(refs_get):
    fields = [
        'id', 'dateAdded', 'source', 'date', 'fromCode', 'fromAmount', 'toCode', 'toAmount', 'buy', 'sell',
        'rateDir', 'rateInv'
    ]

    result = refs_get('/api/currency/?query={rates (source:"UZB", fromCode:"EUR") {%(fields)s}}', fields=fields)
    assert not result.get('errors', None)
    assert len(result['data']['rates']) == 1
    assert result['data']['rates'][0]['toCode'] == 'UZS'
    assert Decimal(result['data']['rates'][0]['fromAmount']) == Decimal(1)
