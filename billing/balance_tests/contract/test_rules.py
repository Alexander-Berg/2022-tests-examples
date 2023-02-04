# coding=utf-8
import xmlrpclib
import datetime
import pytest
import sqlalchemy as sa
from mock import patch
import json

from billing.contract_iface.cmeta import partners, distribution, spendable
from billing.contract_iface.constants import ContractTypeId, CollateralType

from balance import constants as cst, mapper, muzzle_util as ut
from balance.scheme import contract_awards_scale_rules, enums_tree
from tests import object_builder as ob

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)
TOMORROW = NOW + datetime.timedelta(days=1)


def test_prohibited_services_for_firm(session, xmlrpcserver):  # Еще одна неведомая хрень
    """
    Проверяем запрет на создание договора с ООО Яндекс после
    выделения сервиса в бизнес юнит.
    """
    # Находим бизнес-юнитизированный сервис
    service_id = session.query(
        mapper.PayPolicyService.service_id
    ).select_from(mapper.PayPolicyService).\
        join(mapper.PayPolicyRegion).\
        filter(sa.and_(
            mapper.PayPolicyService.legal_entity == 1,
            mapper.PayPolicyService.is_atypical == 0,
            mapper.PayPolicyRegion.is_contract == 0,
            mapper.PayPolicyRegion.region_id == cst.RegionId.RUSSIA,
            mapper.PayPolicyService.firm_id.in_(session.config.BUSINESS_UNITS),
            ~sa.exists().where(
                mapper.PayPolicyPaymentMethod.pay_policy_service_id == mapper.PayPolicyService.id
            )
        )).limit(1).scalar()

    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(type='ur', client=client).build(session).obj
    dt = datetime.datetime.now().replace(microsecond=0).isoformat()
    params = {
        # Пробуем создать договор с ООО Яндекс
        'firm': cst.FirmId.YANDEX_OOO,
        'client-id': client.id,
        'person-id': person.id,
        'services': u'1',
        ('services-%s' % service_id): service_id,
        'dt': dt,
        'is-signed-dt': dt
    }
    expected_message = (
        u'Создание договоров для выбранных сервисов '
        u'с фирмой ООО Яндекс на дату после создания Бизнес-Юнита запрещено'
    )
    try:
        xmlrpcserver.CreateContract(session.oper_id, params)
    except xmlrpclib.Fault as e:
        assert expected_message in e.faultString


@pytest.mark.parametrize(
    'client_limit, payment_type',
    ([100, cst.PREPAY_PAYMENT_TYPE],
     [0, cst.PREPAY_PAYMENT_TYPE],
     [100, cst.POSTPAY_PAYMENT_TYPE])
)
@pytest.mark.auto_overdraft
def test_prohibited_for_auto_overdraft_person(session, xmlrpcserver, client_limit, payment_type):
    """
    Запрет на создание договора, если контракт не постоплатный и у плательщика подключен автоовердрафт
    """
    service_1 = ob.Getter(mapper.Service, cst.ServiceId.DIRECT).build(session).obj
    service_2 = ob.Getter(mapper.Service, cst.ServiceId.KINOPOISK_PLUS).build(session).obj
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)
    _ov_params = ob.OverdraftParamsBuilder(client=client, service=service_1, person=person,
                                           client_limit=client_limit
                                           ).build(session)
    session.flush()

    params = {
        'firm': cst.FirmId.YANDEX_OOO,
        'client-id': client.id,
        'manager-code': manager.manager_code,
        'person-id': person.id,
        'services': u'1',
        ('services-%s' % service_1.id): service_1.id,
        ('services-%s' % service_2.id): service_2.id,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'payment-type': payment_type,
        'payment_term': 10,
    }

    if client_limit and payment_type == cst.PREPAY_PAYMENT_TYPE:
        with pytest.raises(xmlrpclib.Fault) as e:
            xmlrpcserver.CreateContract(session.oper_id, params)

        part_of_exception_error = u'Плательщик не может быть использован в договоре,' \
                                  u' так как для него настроен порог отключения.'
        assert part_of_exception_error in e.value.faultString
    else:
        xmlrpcserver.CreateContract(session.oper_id, params)


@pytest.mark.parametrize(
    'firm_id, country, currency, ok',
    ([cst.FirmId.UBER_ML_BV, cst.RegionId.BELARUS, 'USD', False],
     [cst.FirmId.UBER_ML_BV, cst.RegionId.BELARUS, 'BYN', True],
     [cst.FirmId.UBER_ML_BV_BYN, cst.RegionId.BELARUS, 'BYN', True],
     [cst.FirmId.TAXI_EU_BV, cst.RegionId.ARMENIA, 'USD', True],
     [cst.FirmId.TAXI_EU_BV, cst.RegionId.ARMENIA, 'EUR', False])
)
def test_prohibited_currency_general(session, xmlrpcserver, firm_id, country, currency, ok):
    """
    Запрет на создание договора, если валюта не соответствует стране
    """
    services = [cst.ServiceId.TAXI_CASH, cst.ServiceId.TAXI_CARD, cst.ServiceId.TAXI_PAYMENT]
    if firm_id in (cst.FirmId.UBER_ML_BV, cst.FirmId.UBER_ML_BV_BYN):
        services += [cst.ServiceId.UBER_PAYMENT, cst.ServiceId.UBER_PAYMENT_ROAMING]

    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='eu_yt').build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)
    session.flush()

    params = {
        'firm_id': firm_id,
        'country': country,
        'client_id': client.id,
        'manager_code': manager.manager_code,
        'person_id': person.id,
        'services': services,
        'currency': currency,
        'partner_commission_pct2': 1,
        'dt': dt,
        'is_signed_dt': dt.isoformat(),
        'finish_dt': dt + datetime.timedelta(days=10),
        'payment_type': cst.PREPAY_PAYMENT_TYPE,
    }

    if ok:
        xmlrpcserver.CreateCommonContract(session.oper_id, params)
    else:
        with pytest.raises(xmlrpclib.Fault) as e:
            xmlrpcserver.CreateCommonContract(session.oper_id, params)

        part_of_exception_error = u'Неправильная валюта для выбранных условий'
        assert part_of_exception_error in e.value.faultString


def test_prohibited_currency_spendable(session, xmlrpcserver):
    """
    Запрет на создание договора, если валюта не соответствует стране
    """
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ur', is_partner=True).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)
    session.flush()

    params = {
        'ctype': 'SPENDABLE',
        'firm_id': cst.FirmId.TAXI,
        'country': cst.RegionId.RUSSIA,
        'region': 1,
        'client_id': client.id,
        'manager_code': manager.manager_code,
        'person_id': person.id,
        'services': [cst.ServiceId.TAXI_CORP, cst.ServiceId.TAXI_CORP_PARTNERS],
        'currency': 'RUR',
        'partner_commission_pct2': 1,
        'dt': dt,
        'is_signed_dt': dt.isoformat(),
        'nds': 18,
        'finish_dt': dt + datetime.timedelta(days=10),
        'payment_type': cst.PREPAY_PAYMENT_TYPE,
    }

    xmlrpcserver.CreateCommonContract(session.oper_id, params)


@pytest.mark.parametrize(
    'client_limit',
    [0, 100]
)
@pytest.mark.auto_overdraft
def test_do_prepay_collateral_prohibited_for_auto_overdraft_person(session, xmlrpcserver, client_limit):
    """
    Запрет на создание ДС о переводе на предоплату, если у плательщика подключен автоовердрафт
    """
    service_1 = ob.Getter(mapper.Service, cst.ServiceId.DIRECT).build(session).obj
    service_2 = ob.Getter(mapper.Service, cst.ServiceId.MKB).build(session).obj
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)
    _ov_params = ob.OverdraftParamsBuilder(client=client, service=service_1, person=person,
                                           client_limit=client_limit
                                           ).build(session)
    contract_params = {
        'firm': cst.FirmId.YANDEX_OOO,
        'client': client,
        'manager-code': manager.manager_code,
        'person': person,
        'services': [service_1.id, service_2.id],
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'payment_type': cst.POSTPAY_PAYMENT_TYPE,
        'payment_term': 10
    }
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    session.flush()

    collateral_params = {
        'CONTRACT2_ID': contract.id,
        'DT': dt,
        'IS_SIGNED': dt
    }

    if client_limit:
        with pytest.raises(xmlrpclib.Fault) as e:
            xmlrpcserver.CreateCollateral(session.oper_id, contract.id, CollateralType.DO_PREPAY,
                                          collateral_params)

        part_of_exception_error = u'Плательщик не может быть использован в договоре,' \
                                  u' так как для него настроен порог отключения.'
        assert part_of_exception_error in e.value.faultString
    else:
        xmlrpcserver.CreateCollateral(session.oper_id, contract.id, CollateralType.DO_PREPAY, collateral_params)


@pytest.mark.parametrize(
    'client_limit',
    [0, 100]
)
@pytest.mark.auto_overdraft
def test_change_service_collateral_prohibited_for_auto_overdraft_person(session, xmlrpcserver, client_limit):
    """
    Запрет на создание ДС о смене сервиса, если у плательщика подключен автоовердрафт на этот сервис
    """
    service_1 = ob.Getter(mapper.Service, cst.ServiceId.DIRECT).build(session).obj
    service_2 = ob.Getter(mapper.Service, cst.ServiceId.MKB).build(session).obj
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)
    _ov_params = ob.OverdraftParamsBuilder(client=client, service=service_1, person=person,
                                           client_limit=client_limit
                                           ).build(session)
    contract_params = {
        'firm': cst.FirmId.YANDEX_OOO,
        'client': client,
        'manager-code': manager.manager_code,
        'person': person,
        'services': [service_2.id, ],
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'payment_type': cst.PREPAY_PAYMENT_TYPE
    }
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    session.flush()

    collateral_params = {
        'CONTRACT2_ID': contract.id,
        'DT': dt,
        'IS_SIGNED': dt,
        'services': [service_1.id, service_2.id]
    }

    if client_limit:
        with pytest.raises(xmlrpclib.Fault) as e:
            xmlrpcserver.CreateCollateral(session.oper_id, contract.id, CollateralType.CHANGE_SERVICES,
                                          collateral_params)

        part_of_exception_error = u'Плательщик не может быть использован в договоре,' \
                                  u' так как для него настроен порог отключения.'
        assert part_of_exception_error in e.value.faultString
    else:
        xmlrpcserver.CreateCollateral(session.oper_id, contract.id, CollateralType.CHANGE_SERVICES,
                                      collateral_params)


@pytest.mark.parametrize(
    'client_limit',
    [0, 100]
)
@pytest.mark.auto_overdraft
def test_brand_prohibited_for_auto_overdraft_client(session, xmlrpcserver, client_limit):
    """
    Запрет на создание техсвязки с клиентом, у которого есть автоовердрафт
    """
    service = ob.Getter(mapper.Service, cst.ServiceId.DIRECT).build(session).obj
    client_1 = ob.ClientBuilder().build(session).obj
    client_2 = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client_1).build(session).obj
    dt = session.now().replace(microsecond=0)
    _ov_params = ob.OverdraftParamsBuilder(client=client_1, service=service, person=person,
                                           client_limit=client_limit
                                           ).build(session)
    session.flush()

    params = {
        'client_id': client_1.id,
        'person': None,
        'commission': ContractTypeId.ADVERTISING_BRAND,
        'firm': cst.FirmId.YANDEX_OOO,
        'dt': dt + datetime.timedelta(days=2),
        'payment_type': None,
        'brand_type': 7,
        'brand_clients': json.dumps(
            [
                {"id": "1", "num": client_1.id, "client": client_1.id},
                {"id": "2", "num": client_2.id, "client": client_2.id}
            ]
        )
    }

    if client_limit:
        with pytest.raises(xmlrpclib.Fault) as e:
            xmlrpcserver.CreateContract(session.oper_id, params)

        part_of_exception_error = u'Клиент(ы) %s имеют автоовердрафт. Включение их в бренд запрещено' % client_1.id
        assert part_of_exception_error in e.value.faultString

        params['client_id'] = client_2.id

        with pytest.raises(xmlrpclib.Fault) as e:
            xmlrpcserver.CreateContract(session.oper_id, params)

        part_of_exception_error = u'Клиент(ы) %s имеют автоовердрафт. Включение их в бренд запрещено' % client_1.id
        assert part_of_exception_error in e.value.faultString
    else:
        xmlrpcserver.CreateContract(session.oper_id, params)


@pytest.mark.parametrize('trace, is_called', [
                        ('0',     False),
                        ('1',     True)
])
def test_tracer(session, xmlrpcserver, trace, is_called):
    contract = ob.ContractBuilder.construct(session=session)
    with patch('sys.settrace') as settrace:
        xmlrpcserver.CreateContract(session.oper_id, {
            'id': str(contract.id),
            'dt': session.now().replace(microsecond=0).isoformat(),
            'trace': trace
        })

        assert settrace.called == is_called


def check_create_two_collaterals(session, xmlrpcserver, contract_type, contract_type_params, params):
    """Проверяем создание двух ДС"""

    client = ob.ClientBuilder.construct(session)
    contract_params = {'ctype': contract_type, 'firm': 1, 'is_signed': NOW, 'client': client}
    if contract_type == 'DISTRIBUTION':
        contract_params['distribution_tag'] = ob.DistributionTagBuilder.construct(session, client_id=client.id).id
    contract_params.update(contract_type_params)
    contract_params.update(params['contract_params'])
    contract = ob.ContractBuilder.construct(session, **contract_params)

    col_type_func = {
        'PARTNERS': partners.collateral_types,
        'DISTRIBUTION': distribution.collateral_types,
        'SPENDABLE': spendable.collateral_types,
    }[contract_type]

    col_1_params = params['col_1'].copy()
    col_1_type = col_1_params.pop('collateral_type')[contract_type]
    col_1_params = dict(
        {'contract': contract, 'num': 'rule_test_01', 'is_signed': NOW, 'collateral_type': col_type_func[col_1_type]}.items()
        + col_1_params.items()
    )
    col_1 = ob.CollateralBuilder.construct(session, **col_1_params)

    col_2_params = params['col_2'].copy()
    col2_type = col_2_params.pop('collateral_type')[contract_type]
    col_2_params = dict({'NUM': 'rule_test_02'}.items() + col_2_params.items())

    def create_new_collateral():
        return xmlrpcserver.CreateCollateral(
            session.oper_id,
            str(contract.id),
            col2_type,
            col_2_params,
        )

    if params['error_msg'] is None:
        # создание допсоглашения не должно генерировать ошибку
        create_new_collateral()
    else:
        # создание допсоглашения должно вернуть ошибку
        with pytest.raises(xmlrpclib.Fault) as e:
            create_new_collateral()
        assert params['error_msg'] in e.value.faultString


@pytest.mark.parametrize(
    'contract_type, contract_type_params',
    [
        pytest.param('PARTNERS', {'contract_type': 9, 'services': [290]}, id='partners'),
        pytest.param('DISTRIBUTION', {'contract_type': 7, 'service_start_dt': NOW, 'currency_calculation': 1}, id='distribution'),
    ],
)
@pytest.mark.parametrize(
    'params',
    [
        pytest.param({
            'contract_params': {'dt': ut.trunc_date(YESTERDAY), 'is_signed': ut.trunc_date(YESTERDAY)},
            'col_1': {'dt': ut.trunc_date(YESTERDAY), 'collateral_type': {'PARTNERS': 2050, 'DISTRIBUTION': 3060}, 'end_dt': ut.trunc_date(TOMORROW)},
            'col_2': {'DT': ut.trunc_date(NOW), 'collateral_type': {'PARTNERS': 2050, 'DISTRIBUTION': 3060}, 'END_DT': ut.trunc_date(TOMORROW)},
            'error_msg': u'Больше одного доп. соглашения на закрытие',
        }, id='closing'),
        pytest.param({
            'contract_params': {'dt': ut.trunc_date(YESTERDAY), 'is_signed': ut.trunc_date(YESTERDAY)},
            'col_1': {'dt': ut.trunc_date(TOMORROW), 'collateral_type': {'PARTNERS': 2160, 'DISTRIBUTION': 3120}, 'selfemployed': 1},
            'col_2': {'DT': ut.trunc_date(YESTERDAY), 'collateral_type': {'PARTNERS': 2050, 'DISTRIBUTION': 3060}, 'END_DT': ut.trunc_date(NOW)},
            'error_msg': u'Нельзя создать доп. соглашение на закрытие, после которого заключены другие доп. соглашения',
        }, id='complex_closing'),
    ],
)
def test_all_collaterals_data(session, xmlrpcserver, contract_type, contract_type_params, params):
    check_create_two_collaterals(session, xmlrpcserver, contract_type, contract_type_params, params)


@pytest.mark.parametrize(
    'contract_type, contract_type_params',
    [
        pytest.param('PARTNERS', {'contract_type': 9, 'services': [290]}, id='partners'),
        pytest.param('DISTRIBUTION', {'contract_type': 7, 'service_start_dt': NOW, 'currency_calculation': 1}, id='distribution'),
        pytest.param('SPENDABLE', {'service_start_dt': NOW, 'services': [42], 'is_offer': 1}, id='spendable'),
    ],
)
@pytest.mark.parametrize(
    'params',
    [
        pytest.param({
            'contract_params': {},
            'col_1': {'dt': ut.trunc_date(NOW), 'collateral_type': {'PARTNERS': 2160, 'DISTRIBUTION': 3120, 'SPENDABLE': 7090}, 'selfemployed': 1},
            'col_2': {'DT': ut.trunc_date(NOW), 'collateral_type': {'PARTNERS': 2160, 'DISTRIBUTION': 3120, 'SPENDABLE': 7090}, 'SELFEMPLOYED': 1},
            'error_msg': u'Запрещено создавать два доп. соглашения на одну дату, изменяющих одинаковые параметры договора',
        }, id='same_dt'),
        pytest.param({
            'contract_params': {},
            'col_1': {'dt': ut.trunc_date(NOW), 'collateral_type': {'PARTNERS': 2160, 'DISTRIBUTION': 3120, 'SPENDABLE': 7090}, 'selfemployed': 1},
            'col_2': {'DT': ut.trunc_date(NOW), 'collateral_type': {'PARTNERS': 2050, 'DISTRIBUTION': 3060, 'SPENDABLE': 7050}, 'END_DT': ut.trunc_date(NOW)},
            'error_msg': None,
        }, id='same_dt_but_diff_attributes'),
    ],
)
def test_two_collaterals_same_date(session, xmlrpcserver, contract_type, contract_type_params, params):
    check_create_two_collaterals(session, xmlrpcserver, contract_type, contract_type_params, params)


@pytest.mark.parametrize(
    'pagination_conf',
    [False, True],
)
def test_all_collaterals_data_w_config(session, xmlrpcserver, pagination_conf):
    session.config.__dict__['CONTRACTPAGE_PAGINATION'] = pagination_conf
    now = session.now()

    contract = ob.ContractBuilder.construct(
        session,
        ctype='PARTNERS',
        contract_type=9,  # Оферта
        firm=1,
        is_signed=now,
        services=[290],
    )
    col_1 = ob.CollateralBuilder.construct(
        session,
        contract=contract,
        num='rule_test_01',
        dt=ut.trunc_date(now),
        collateral_type=partners.collateral_types[2160],  # Статус самозанятого
        selfemployed=1,
        is_signed=now,
    )
    session.flush()

    with pytest.raises(xmlrpclib.Fault) as e:
        res = xmlrpcserver.CreateCollateral(
            session.oper_id,
            str(contract.id),
            2160,
            {
                'DT': ut.trunc_date(now),
                'SELFEMPLOYED': 0,
                'NUM': 'rule_test_02',
            },
        )
        print res

    part_of_exception_error = u'Запрещено создавать два доп. соглашения на одну дату, изменяющих одинаковые параметры договора'
    assert part_of_exception_error in e.value.faultString


def test_col_w_same_date_cancelled(session, xmlrpcserver):
    now = session.now()
    contract = ob.ContractBuilder.construct(
        session,
        ctype='PARTNERS',
        contract_type=9,  # Оферта
        firm=1,
        is_signed=now,
        services=[290],
    )
    col_1 = ob.CollateralBuilder.construct(
        session,
        contract=contract,
        num='rule_test_01',
        dt=ut.trunc_date(now),
        collateral_type=partners.collateral_types[2160],  # Статус самозанятого
        selfemployed=1,
        is_signed=now,
        is_cancelled=now,
    )
    session.flush()

    res = xmlrpcserver.CreateCollateral(
        session.oper_id,
        str(contract.id),
        2160,
        {
            'DT': ut.trunc_date(now),
            'SELFEMPLOYED': 0,
            'NUM': 'rule_test_02',
        },
    )
    assert res['CONTRACT_ID'] == contract.id


@pytest.mark.parametrize('default', (True, False))
def test_wholesale_agent_premium_awards_scale_type_default(session, xmlrpcserver, default):
    if default:
        scale_type = ob.get_big_number()
        expected_scale_type = 0
    else:
        expected_scale_type = scale_type = ob.get_big_number()
        session.execute(sa.insert(contract_awards_scale_rules), {
            'id': ob.get_big_number(),
            'scale': scale_type,
            'contract_type': ContractTypeId.WHOLESALE_AGENCY_AWARD,
            'firm': None,
            'currency': None,
        })
        session.execute(sa.insert(enums_tree), {
            'id': ob.get_big_number(),
            'parent_id': 3000,
            'code': str(scale_type),
            'value': 'TEST'
        })
        session.flush()

    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)
    session.flush()

    params = {
        'client-id': client.id,
        'person-id': person.id,
        'manager-code': manager.manager_code,
        'commission': ContractTypeId.WHOLESALE_AGENCY_AWARD,
        'firm': cst.FirmId.YANDEX_OOO,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'payment_type': cst.PREPAY_PAYMENT_TYPE,
        'services': [cst.ServiceId.DIRECT],
        ('services-%s' % cst.ServiceId.DIRECT): cst.ServiceId.DIRECT,
        'currency': cst.NUM_CODE_RUR,
        'wholesale_agent_premium_awards_scale_type': scale_type,
    }

    res = xmlrpcserver.CreateContract(session.oper_id, params)
    contract = session.query(mapper.Contract).get(res['ID'])
    assert contract.col0.wholesale_agent_premium_awards_scale_type == expected_scale_type
