# coding=utf-8
__author__ = 'borograam'

import datetime

import pytest
from hamcrest import not_none, none, anything

import btestlib.data.partner_contexts
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType, NdsNew
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS, BLUE_MARKET_SUBSIDY, BLUE_MARKET_SUBSIDY_ISRAEL, \
    BLUE_MARKET_612_ISRAEL, SORT_CENTER_CONTEXT_SPENDABLE, DELIVERY_SERVICES_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_with_entries


def _populate_partner_integration_params(**params):
    return {
        'link_integration_to_client': 1,
        'link_integration_to_client_args': params,
        'set_integration_to_contract': 1,
        'set_integration_to_contract_params': params,
    }


class CurrencyToContextMapperMaker(dict):
    def __init__(self, *args, **kwargs):
        if 'partner_integration_params' in kwargs:
            kwargs['partner_integration_params'] = _populate_partner_integration_params(
                **kwargs['partner_integration_params'])
        super(CurrencyToContextMapperMaker, self).__init__(*args, **kwargs)

    def make_map_from_trust_section(self, trust_section):
        return {
            currency: orig_ctx.new(**self)
            for currency, orig_ctx in trust_section.items()
        }


class Context(object):
    _mem = {  # type: # Dict[int, Dict[str, Dict[str, btestlib.data.partner_contexts.Context]]]
        610: {
            'trust': {
                'RUB': BLUE_MARKET_PAYMENTS,
                'ILS': BLUE_MARKET_612_ISRAEL,
            },
            '_tlog_makers': {
                'acc': CurrencyToContextMapperMaker(
                    tpt_payment_type='partner_payment',
                    tpt_paysys_type_cc='acc_sberbank'
                ),
                'pay': CurrencyToContextMapperMaker(
                    tpt_payment_type='partner_payment',
                    tpt_paysys_type_cc='partner_payment'
                )
            }
        },
        609: {
            'trust': {
                'RUB': BLUE_MARKET_SUBSIDY,
                'ILS': BLUE_MARKET_SUBSIDY_ISRAEL,
            },
            '_tlog_makers': {
                'acc': CurrencyToContextMapperMaker(
                    tpt_payment_type='acc_subsidy',
                    tpt_paysys_type_cc='yamarket',
                    pps_kwargs={
                        'extra_num_0': 1
                    },
                ),
                'pay': CurrencyToContextMapperMaker(
                    tpt_payment_type='pay_subsidy',
                    tpt_paysys_type_cc='yamarket'
                )
            }
        },
        1060: {
            'trust': {
                'RUB': SORT_CENTER_CONTEXT_SPENDABLE.new(
                    partner_integration_params=_populate_partner_integration_params(
                        integration_cc='market_sort_centers',
                        configuration_cc='market_sort_centers_default'
                    )
                ),
            },
            '_tlog_makers': {
                'acc': CurrencyToContextMapperMaker(
                    tpt_payment_type='acc_sorting_reward',
                    tpt_paysys_type_cc='sort_center',
                    pps_kwargs={
                        'extra_num_0': 1
                    },
                ),
                'pay': CurrencyToContextMapperMaker(
                    tpt_payment_type='partner_payment',
                    tpt_paysys_type_cc='sort_center'
                )
            },
        },
        1100: {
            'trust': {
                'RUB': DELIVERY_SERVICES_CONTEXT_SPENDABLE.new(
                    partner_integration_params=_populate_partner_integration_params(
                        integration_cc='market_delivery_services',
                        configuration_cc='market_delivery_services_default'
                    )
                ),
            },
            '_tlog_makers': {
                'acc': CurrencyToContextMapperMaker(
                    tpt_payment_type='acc_car_delivery',
                    tpt_paysys_type_cc='market-delivery',
                    pps_kwargs={
                        'extra_num_0': 1
                    },
                ),
                'pay': CurrencyToContextMapperMaker(
                    tpt_payment_type='partner_payment',
                    tpt_paysys_type_cc='market-delivery'
                )
            },
        },
    }

    for d in _mem.values():
        if '_tlog_makers' in d:
            d['tlog'] = {}
            for tlog_type, maker in d['_tlog_makers'].items():
                d['tlog'][tlog_type] = maker.make_map_from_trust_section(d['trust'])

    @classmethod
    def get(cls, service_id, source='tlog', currency='RUB', tlog_type='pay'):
        # type: (int, str, str, str) -> btestlib.data.partner_contexts.Context
        # todo: сделать бы это ленивым с кешированием
        assert not source.startswith('_')

        result = cls._mem[service_id][source]
        if source == 'tlog':
            result = result[tlog_type]
        return result[currency]

    def __new__(cls, service_id, source='tlog', currency='RUB', tlog_type='pay'):
        return cls.get(service_id, source, currency, tlog_type)


def create_and_process_sidepayment(context, client_id, paysys_type_cc=None, payment_type=None, add_pps_keys=None,
                                   is_refund=False):
    paysys_type_cc = paysys_type_cc or context.tpt_paysys_type_cc
    payment_type = payment_type or context.tpt_payment_type

    pps_kwargs = {
        'payload': '{}',
        'paysys_type_cc': paysys_type_cc,
        'extra_str_0': '123-item-987',
        'extra_str_1': 'bank',
        'currency': context.currency,
    }
    if add_pps_keys:
        assert isinstance(add_pps_keys, dict)
        pps_kwargs.update(add_pps_keys)
    if is_refund:
        pps_kwargs['transaction_type'] = TransactionType.REFUND

    side_payment_id, transaction_id_payment = steps.PartnerSteps.create_sidepayment_transaction(
        client_id, utils.Date.moscow_offset_dt(), simpleapi_defaults.DEFAULT_PRICE, payment_type, context.service.id,
        **pps_kwargs
    )
    steps.ExportSteps.create_export_record_and_export(
        side_payment_id, Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT,
        service_id=context.service.id)

    return side_payment_id, transaction_id_payment


def check_sidepayment(context, client_id, contract_id, person_id, transaction_id_payment, side_payment_id,
                      trust_refund_id=None, we_add_nds=False,
                      paysys_type_cc=None, payment_type=None, add_tpt_keys=None, payout_ready_dt=anything(), **kwargs):
    paysys_type_cc = paysys_type_cc or context.tpt_paysys_type_cc
    payment_type = payment_type or context.tpt_payment_type

    expected_amount = simpleapi_defaults.DEFAULT_PRICE
    if we_add_nds:
        expected_amount *= context.nds.koef_on_dt(datetime.datetime.now())

    expected_data = steps.SimpleApi.create_expected_tpt_row(
        context, client_id, contract_id, person_id, transaction_id_payment, side_payment_id, trust_refund_id,
        paysys_type_cc=paysys_type_cc, payment_type=payment_type, payout_ready_dt=payout_ready_dt,
        amount=expected_amount,
        **(add_tpt_keys or {})
    )
    actual_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        side_payment_id,
        transaction_type=TransactionType.REFUND if trust_refund_id else TransactionType.PAYMENT,
        source='sidepayment')
    utils.check_that(actual_data, contains_dicts_with_entries([expected_data]), u'Сравниваем платёж с шаблоном')


@pytest.mark.parametrize('params', (
        pytest.param({}, id='common'),
        pytest.param({'create_refund': True}, id='refund'),
        pytest.param({
            'add_pps_keys': {'extra_num_0': 1},
            'add_tpt_keys': {'internal': 1}
        }, id='ignore_in_oebs'),
))
@pytest.mark.parametrize(
    'context, payout_ready_dt_is_not_null, we_add_nds',
    (
            pytest.param(Context.get(610), True, False, id='610-Russia'),
            pytest.param(Context.get(609), True, False, id='609-Russia'),
            pytest.param(Context.get(610, currency='ILS'), True, False, id='610-Israel'),
            pytest.param(Context.get(609, currency='ILS'), True, False, id='609-Israel'),
            pytest.param(
                Context.get(610).new(
                    tpt_payment_type='partner_payment',
                    tpt_paysys_type_cc='cash_item'
                ),
                True,
                False,
                id='610-refund-from-613'
            ),
            pytest.param(Context(1060), False, True, id='1060-Russia-with-nds'),
            pytest.param(Context(1100, tlog_type='acc'), False, True, id='1100-Russia-with-nds-acc'),
            # накручиваем ндс и начислениям
            pytest.param(Context(1100), False, True, id='1100-Russia-with-nds-pay'),
            # и выплатам
            pytest.param(Context(1100).new(nds=NdsNew.ZERO), False, False, id='1100-Russia-wo-nds'),
    )
)
def test_sidepayment(params, context, payout_ready_dt_is_not_null, we_add_nds):
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(context)
    side_payment_id, transaction_id_payment = create_and_process_sidepayment(
        context, client_id, add_pps_keys=params.get('add_pps_keys'))

    check_sidepayment(
        add_tpt_keys=params.get('add_tpt_keys'),
        payout_ready_dt=not_none() if payout_ready_dt_is_not_null else none(),
        **locals()
    )

    if params.get('create_refund', False):
        side_payment_id, trust_refund_id = create_and_process_sidepayment(
            context, client_id, add_pps_keys=params.get('add_pps_keys'), is_refund=True)

        transaction_id_payment = None
        check_sidepayment(add_tpt_keys=params.get('add_tpt_keys'), is_refund=True,
                          payout_ready_dt=not_none() if payout_ready_dt_is_not_null else none(),
                          **locals())


def test_rewrite_609_tlog_payment_types():
    payment_type_map = {
        PaymentType.ACC_YA_WITHDRAW: PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        PaymentType.ACC_SUBSIDY: PaymentType.SUBSIDY,
        PaymentType.ACC_DELIVERY_SUBSIDY: PaymentType.DELIVERY_SUBSIDY,
        PaymentType.PAY_YA_WITHDRAW: PaymentType.PAY_YA_WITHDRAW,
        PaymentType.PAY_SUBSIDY: PaymentType.PAY_SUBSIDY,
        PaymentType.PAY_DELIVERY_SUBSIDY: PaymentType.PAY_DELIVERY_SUBSIDY,
    }
    context = BLUE_MARKET_SUBSIDY
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_offer=1,
    )
    paysys_type_cc = 'paysys_type_cc'

    for from_payment_type, to_payment_type in payment_type_map.items():
        side_payment_id, transaction_id_payment = create_and_process_sidepayment(
            context, client_id, paysys_type_cc, from_payment_type)
        check_sidepayment(payment_type=to_payment_type, **locals())
