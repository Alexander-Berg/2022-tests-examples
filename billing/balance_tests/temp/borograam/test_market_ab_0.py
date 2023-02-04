# coding=utf-8
import pytest
from btestlib import utils
from datetime import datetime
from dateutil.relativedelta import relativedelta
from balance import balance_steps as steps
from btestlib.constants import TransactionType, Services
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS
from datetime import datetime as dt
import balance.balance_db as db
from btestlib.data import person_defaults
from decimal import Decimal as D
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries


CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
ACT_DATE = utils.Date.first_day_of_month(dt.now())


def create_client_person():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id,
                                         'ur',
                                         {'is-partner': '1'},
                                         inn_type=person_defaults.InnType.RANDOM,
                                         full=False)
    return client_id, person_id


def create_contract(client_id=None, person_id=None):
    # создаем клиента-партнера
    if client_id is None:
        client_id = steps.SimpleApi.create_partner(BLUE_MARKET_PAYMENTS.service)
    product = steps.SimpleApi.create_service_product(BLUE_MARKET_PAYMENTS.service, client_id)
    product_fee = steps.SimpleApi.create_service_product(BLUE_MARKET_PAYMENTS.service, client_id, service_fee=1)

    _, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        BLUE_MARKET_PAYMENTS,
        client_id=client_id,
        person_id=person_id,
        is_offer=1,
        additional_params={
            'start_dt': CONTRACT_START_DT
        })
    return client_id, person_id, contract_id, contract_eid, product, product_fee


def test_create_only_client_person():
    client_id, person_id = create_client_person()
    print 'client: {}, person: {}'.format(client_id, person_id)


def test_create_contract():
    client_id = 10264538
    _, person_id, contract_id, contract_eid, service_product_id, service_product_fee_id = create_contract(client_id=client_id)

    print 'service_product_id: {} \nservice_product_fee_id: {}\nperson_id: {}\ncontract_id: {}\ncontract_eid: {}'.format(
        service_product_id, service_product_fee_id, person_id, contract_id, contract_eid)


@pytest.mark.parametrize(
    "purchase_token",
    (
            "0a2d5dd303186d71b59650847d8ad052",
            "da3f996363bd3c95b6fd998b03669041"
    )
)
def test_trust_payment(purchase_token):
    payment_id = db.balance().execute(
        'select id from t_payment where purchase_token=:token',
        {'token': purchase_token}
    )[0]['id']
    # payment_id = steps.SimpleApi.wait_for_payment_export_from_bs(trust_payment_id)
    steps.CommonPartnerSteps.export_payment(payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        payment_id,
        TransactionType.PAYMENT
    )
    assert all(row['yandex_reward'] == 0 for row in payment_data), 'payment_id: {}'.format(payment_id)

    for tpt in payment_data:
        steps.ExportSteps.export_oebs(
            client_id=tpt['partner_id'],
            person_id=tpt['person_id'],
            contract_id=tpt['contract_id'],
            transaction_id=tpt['id']
        )

    for tpt in payment_data:
        print 'row: `{}`, service_order_id_str:`{}`, amount: `{}`, client: `{}`, contract: `{}`'.format(
            tpt['id'],
            tpt['service_order_id_str'],
            tpt['amount'],
            tpt['partner_id'],
            tpt['contract_id']
        )


def test_create_act():
    client_id = 1349769547
    contract_id = 3223465
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, ACT_DATE)
    act_data = steps.ActsSteps.get_all_act_data(client_id)
    print act_data


def create_trust_payment(product, product_fee, commission=D('0')):
    # создаю платёж в трасте
    service_order_id_list, trust_payment_id, _, payment_id = steps.SimpleApi.create_multiple_trust_payments(
        BLUE_MARKET_PAYMENTS.service,
        [product],
        commission_category_list=[commission],
        prices_list=[D('100000')],
        paymethod=None  # вроде же карта тогда будет?
    )
    # обрабатываю платёж
    steps.CommonPartnerSteps.export_payment(payment_id)
    # достаю айдишник транзакции
    tpt_id = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        payment_id,
        TransactionType.PAYMENT
    )[0]['id']
    return tpt_id


def create_tlog_commission(client_id, dt, last=1):
    # создаэм фейковую тлог открутку
    steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
        dt,
        type_='agency_commission',
        service_id=Services.BLUE_MARKET.id,
        client_id=client_id, amount=D('1000'),
        last_transaction_id=last)


def test_create_act_from_tlog():
    client_id, person_id, contract_id, contract_eid, product, product_fee = create_contract()

    tpt_id = create_trust_payment(product, product_fee)
    create_tlog_commission(client_id, ACT_DATE)

    # генерируем акт
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, ACT_DATE)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = [steps.CommonData.create_expected_act_data(
        amount=D('1000'),
        act_date=utils.Date.last_day_of_month(ACT_DATE),
        context=BLUE_MARKET_PAYMENTS
    )]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), step=u'Сравним акт с ожидаемым')

    act_id, invoice_id = [steps.ActsSteps.get_all_act_data(client_id)[0][attr] for attr in ('id', 'invoice_id')]
    steps.ExportSteps.export_oebs(
        client_id=client_id, person_id=person_id, contract_id=contract_id,
        act_id=act_id, invoice_id=invoice_id, transaction_id=tpt_id)

    print 'client: `{}`, person: `{}`, contract: `{}`, invoice: `{}`, act: `{}`, tpt_row: `{}`'.format(
        client_id, person_id, contract_id, invoice_id, act_id, tpt_id
    )

    # tlog_notches = TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    # last_transaction_ids = max([n['last_transaction_id'] for n in tlog_notches])
    # utils.check_that(last_transaction_ids, equal_to(max_last_transaction_id),
    #                  'Сравниваем last_transaction_id с ожидаемым')


def test_fix_payment_to_existing_contract():
    client_id, contract_id = 1350378723, 3563451
    product = steps.SimpleApi.create_service_product(BLUE_MARKET_PAYMENTS.service, client_id)
    product_fee = steps.SimpleApi.create_service_product(BLUE_MARKET_PAYMENTS.service, client_id, service_fee=1)
    tpt_id = create_trust_payment(product, product_fee)
    steps.ExportSteps.export_oebs(transaction_id=tpt_id)
    print 'row: `{}`'.format(tpt_id)


def test_skip_610_act():
    client_id, person_id, contract_id, contract_eid, product, product_fee = create_contract()
    tpt_id = create_trust_payment(product, product_fee, commission=D('100'))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, ACT_DATE)
    act_data = steps.ActsSteps.get_all_act_data(client_id)
    print act_data


def get_ids_from_contract(contract_id):
    contract_attributes = ['client_id', 'person_id']
    res = db.balance().execute(
        """select {} from T_CONTRACT2 where id=:contract_id""".format(
            ', '.join(contract_attributes)),
        dict(contract_id=contract_id))[0]
    client_id, person_id = [res[attr] for attr in contract_attributes]
    #
    # product = db.balance().execute(
    #     """select *
    #     from T_SERVICE_PRODUCT
    #     where PARTNER_ID=:client_id
    #     and SERVICE_ID=610""",
    #     dict(client_id=client_id)
    # )[0]['name']

    return client_id, person_id


# @pytest.mark.parametrize(
#     'contract_id',
#     (1583306,)
#
# contract pool:
# 1147236
# 884034
# 449424
# 948396
# 2085973
# 794259
# 751521
# 1399961
# 3029120
# 1260512
# 431926
# 823420
# 1445520
# 2247622
# 1054077
# 412739
# 1056177
# 1558498
# 2687851
# 1329938
# 1341860
# 633015
# 1068847
# 2879020
# 2663879
# 835014
# 1968267
# 926848
# 919381
# 2904437
# 1137386
# 2489043
# 1973107
# 2196483
# 2868309
# 2803382
# 1932831
# 2227705
# 637221
# 872350
# 2259265
# 2669166
# 2666522
# 966994

# def test_like_prod(contract_id):
def test_like_prod(mock_simple_api):  # to use fake trust add 'mock_simple_api' to args and move file to balance/tests
    contract_id = 948448
    client_id, person_id = get_ids_from_contract(contract_id)
    product = steps.SimpleApi.create_service_product(BLUE_MARKET_PAYMENTS.service, client_id)
    april, may = datetime(2021, 04, 01), datetime(2021, 05, 01)

    #create_tlog_commission(client_id, april)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, april)

    act_data = steps.ActsSteps.get_all_act_data(client_id, dt=utils.Date.last_day_of_month(april))
    # assert len(act_data) == 2
    # act_april_id1, invoice_id, act_april_id2, _ = [
    #     act[attr]
    #     for act in act_data
    #     for attr in ('id', 'invoice_id')
    # ]
    # steps.ExportSteps.export_oebs(
    #     client_id=client_id, person_id=person_id, contract_id=contract_id,
    #     act_id=act_april_id1, invoice_id=invoice_id
    # )
    for act in act_data:
        steps.ExportSteps.export_oebs(act_id=act['id'])

    tpt_id_ab0 = create_trust_payment(product, None)
    tpt_id_ab100 = create_trust_payment(product, None, commission=D('100'))
    create_tlog_commission(client_id, may)
    create_tlog_commission(client_id, may, 2)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, may)

    act_data = steps.ActsSteps.get_all_act_data(client_id, dt=utils.Date.last_day_of_month(may))
    #assert len(act_data) == 1
    act_may_id, invoice_id = [act_data[0][attr] for attr in ('id', 'invoice_id')]

    steps.ExportSteps.export_oebs(transaction_id=tpt_id_ab0)
    steps.ExportSteps.export_oebs(transaction_id=tpt_id_ab100)
    for act in act_data:
        steps.ExportSteps.export_oebs(act_id=act['id'])

    # steps.ExportSteps.export_oebs(
    #     client_id=client_id, person_id=person_id, contract_id=contract_id,
    #     invoice_id=invoice_id, transaction_id=tpt_id_ab0)
    # steps.ExportSteps.export_oebs(
    #     act_id=act_may_id, transaction_id=tpt_id_ab100)

    print 'client: `{}`, person: `{}`, contract: `{}`, act: `{}`'.format(client_id, person_id, contract_id, act_may_id)


def test_reexport():
    # steps.ExportSteps.export_oebs(
    #     client_id=54831239, person_id=6989290, contract_id=601060,
    #     invoice_id=89667662, transaction_id=79508622379)
    #steps.ExportSteps.export_oebs(transaction_id=79508622379)
    steps.ExportSteps.export_oebs(act_id=146470385)#, transaction_id=79508622419)
