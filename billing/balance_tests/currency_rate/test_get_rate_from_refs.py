# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
from decimal import Decimal
import mock
import pytest

from balance import exc, mapper
from cluster_tools.currency_rate_refs import get_rate_from_refs, old_currency_code, store_rate
import balance.muzzle_util as ut

from butils.decimal_unit import DecimalUnit


DUMMY_TODAY = ut.trunc_date(datetime.strptime('2021-10-20', '%Y-%m-%d'))
DUMMY_TOMORROW = DUMMY_TODAY + timedelta(days=1)

DUMMY_CONFIGS = [
    {
        'currency_pairs':            [['USD', 'RUB']],
        'is_tomorrow_rate_required': is_tomorrow_rate_required,
        'can_use_outdated_rate':     can_use_outdated_rate,
        'probing_period':            probing_period
    }
    for is_tomorrow_rate_required, can_use_outdated_rate, probing_period in (
        (0, 0, 0),  # (None, {'output': 'Skipped, because tomorrow rate is not required'})
        (1, 0, 0),  # (rate, {'real_rate_date': required_date})
        (0, 0, 0),  # raise exc.RATE_NOT_FOUND_EXCEPTION(**exception_params)
        (1, 1, 7),  # (rate, {'real_rate_date': required_date})
        (0, 1, 7)   # raise exc.RATE_NOT_FOUND_EXCEPTION(**exception_params)
    )
]

DUMMY_DATES = [
    DUMMY_TOMORROW,
    DUMMY_TOMORROW,
    DUMMY_TODAY,
    DUMMY_TOMORROW,
    DUMMY_TODAY
]

RESOURCES = [
    mapper.CurrencyRateResource(
        rate_src_id=1000,
        cc='USD',
        base_cc='RUB',
        config=config,
        dt=dt
    )
    for config, dt in zip(DUMMY_CONFIGS, DUMMY_DATES)
]

DUMMY_REFS_RATES = [
    {
        u'sell':       None,
        u'buy':        None,
        u'toAmount':   Decimal('70.967400'),
        u'source':     u'RUS',
        u'toCode':     u'RUB',
        u'date':       '2021-10-20',
        u'fromCode':   u'USD',
        u'fromAmount': Decimal('1.000000')
    },
    {
        u'sell':       None,
        u'buy':        None,
        u'toAmount':   Decimal('71.055500'),
        u'source':     u'RUS',
        u'toCode':     u'RUB',
        u'date':       '2021-10-21',
        u'fromCode':   u'USD',
        u'fromAmount': Decimal('1.000000')
    }
]

RETURN_VALUE_GENERATORS = [
    (DUMMY_REFS_RATES[1],),
    (DUMMY_REFS_RATES[1],),
    (None,),
    (None, DUMMY_REFS_RATES[0]),
    (None for _ in range(14))
]

EXCEPTION_MESSAGES = [
    u'No relevant currency rate found in refs for '
    u'source: RUS '
    u'cc: USD '
    u'base_cc: RUB '
    u'required_date: 2021-10-20 00:00:00 '
    u'can_use_outdated_rate: 0 '
    u'probing_period: 0',

    u'No relevant currency rate found in refs for '
    u'source: RUS '
    u'cc: USD '
    u'base_cc: RUB '
    u'required_date: 2021-10-20 00:00:00 '
    u'can_use_outdated_rate: 1 '
    u'probing_period: 7'
]

EXPECTED_OUTPUTS = [
    (None, {'output': 'Skipped, because tomorrow rate is not required'}),
    (DUMMY_REFS_RATES[1], {'real_rate_date': DUMMY_TOMORROW}),
    EXCEPTION_MESSAGES[0],
    (DUMMY_REFS_RATES[0], {'real_rate_date': DUMMY_TODAY}),
    EXCEPTION_MESSAGES[1]
]


@pytest.mark.parametrize(
    ['resource', 'return_value_generator', 'expected_output'],
    zip(RESOURCES, RETURN_VALUE_GENERATORS, EXPECTED_OUTPUTS)
)
def test_get_rate_from_refs(session, resource, return_value_generator, expected_output):
    if isinstance(expected_output, basestring):
        with pytest.raises(exc.RATE_NOT_FOUND_EXCEPTION) as exc_info:
            with mock.patch('balance.muzzle_util.trunc_date', return_value=DUMMY_TODAY):
                with mock.patch(
                    'cluster_tools.currency_rate_refs.request_rate',
                    side_effect=return_value_generator
                ):
                    get_rate_from_refs(resource)
            assert exc_info.value.faultString == expected_output
        return

    with mock.patch('balance.muzzle_util.trunc_date', return_value=DUMMY_TODAY):
        with mock.patch(
            'cluster_tools.currency_rate_refs.request_rate',
            side_effect=return_value_generator
        ):
            rate_data, output = get_rate_from_refs(resource)
    assert (rate_data, output) == expected_output

    if rate_data is None:
        return

    store_rate(
        session,
        rate_src_id=resource.rate_src_id,
        rate_data=rate_data,
        dry_run=0
    )
    session.flush()

    new_rows_in_t_currency_rate_v2 = session.query(mapper.CurrencyRate) \
                                            .filter_by(rate_dt=rate_data['date']) \
                                            .all()

    assert len(new_rows_in_t_currency_rate_v2) == 1

    for row in new_rows_in_t_currency_rate_v2:
        assert row.cc == old_currency_code(rate_data['fromCode'])
        assert row.rate == DecimalUnit(rate_data['toAmount'],
                                       [rate_data['toCode']],
                                       [rate_data['fromCode']])
        assert row.rate_dt == rate_data['date']
        assert row.base_cc == old_currency_code(rate_data['toCode'])
        assert row.rate_src_id == resource.rate_src_id
        assert row.selling_rate == rate_data['sell']

    new_rows_in_t_iso_currency_rate = session.query(mapper.IsoCurrencyRate) \
                                             .filter_by(dt=rate_data['date']) \
                                             .all()

    assert len(new_rows_in_t_iso_currency_rate) == 2

    iso_currency_from, iso_currency_to = rate_data['toCode'], rate_data['fromCode']
    rate_from, rate_to = rate_data['toAmount'], rate_data['fromAmount']

    for row in new_rows_in_t_iso_currency_rate:
        assert row.dt == rate_data['date']
        assert row.iso_currency_from == iso_currency_from
        assert row.iso_currency_to == iso_currency_to
        assert row.rate_from == rate_from
        assert row.rate_to == rate_to
        iso_currency_from, iso_currency_to = iso_currency_to, iso_currency_from
        rate_from, rate_to = rate_to, rate_from

