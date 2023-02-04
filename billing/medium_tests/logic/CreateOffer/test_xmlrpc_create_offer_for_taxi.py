# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
from decimal import Decimal as D
from collections import namedtuple, Iterable
from sqlalchemy import and_

from tests import object_builder as ob

from balance import mapper
from balance.constants import *
from balance.constants import OebsPayReceiveType
from billing.contract_iface.contract_meta import ContractTypes


def nullify_time_of_date(date):
    return date.replace(hour=0, minute=0, second=0, microsecond=0) if date else None


NDS_DEFAULT = 18
NDS_NOT_RESIDENT = 0
WO_NDS_RECEIPT = -1
NDS_ZERO = 0

CURRENT_DT = nullify_time_of_date(datetime.today())
START_DT_PAST = CURRENT_DT.replace(day=1) + timedelta(days=-200)


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    Taxi = namedtuple('Taxi', 'country, region, currency, currency_code, person_type')
    FIRMTAXI = {
        FirmId.TAXI: Taxi(country=RegionId.RUSSIA, region='02000001000',
                          currency='RUR', currency_code=810, person_type='ur'),
        FirmId.TAXI_UA: Taxi(country=RegionId.UKRAINE, region=None,
                             currency='UAH', currency_code=980, person_type='ua'),
        FirmId.TAXI_AM: Taxi(country=RegionId.ARMENIA, region=None,
                             currency='AMD', currency_code=51, person_type='am_jp'),
        FirmId.TAXI_KZ: Taxi(country=RegionId.KAZAKHSTAN, region=None,
                             currency='KZT', currency_code=398, person_type='kzu'),
        FirmId.TAXI_EU_BV: Taxi(country=RegionId.KAZAKHSTAN, region=None,
                                currency='USD', currency_code=840, person_type='eu_yt')
    }

    def offer_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      manager_uid=manager.domain_passport_id,
                      personal_account=1,
                      offer_confirmation_type='min-payment',
                      deactivation_term=5,
                      deactivation_amount=100)
        return params

    def new_offer(self, manager, additional_params=None):
        params = self.offer_params(manager)

        if additional_params is not None:
            params.update(additional_params)
            if 'country' not in additional_params:
                country = self.FIRMTAXI[additional_params['firm_id']].country
                params.update(country=country)
            elif additional_params['country'] is None:
                params.pop('country')
            if 'region' not in additional_params:
                region = self.FIRMTAXI[additional_params['firm_id']].region
                params.update(region=region)
            elif additional_params['region'] is None:
                params.pop('region')
            currency = self.FIRMTAXI[additional_params['firm_id']].currency
            params.update(currency=currency)

        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def expected_params(self, manager, params):
        if 'start_dt' not in params:
            start_dt = CURRENT_DT
        else:
            start_dt = params['start_dt']

        expected_data = {
            'IS_DEACTIVATED': 0,
            'MANAGER_CODE': manager.manager_code,
            'COUNTRY': self.FIRMTAXI[params['firm_id']].country,
            'CURRENCY': self.FIRMTAXI[params['firm_id']].currency_code,
            'DT': start_dt,
            'PAYMENT_TYPE': params['payment_type'],
            'SERVICES': params['services'],
            'NETTING': params['netting'] if 'netting' in params else None
        }

        if self.FIRMTAXI[params['firm_id']].country != RegionId.RUSSIA:
            # дописываем пустое поле "GEOBASE_REGION" для не-России
            expected_data["GEOBASE_REGION"] = None

        if 'nds_for_receipt' in params:
            expected_data.update({'NDS_FOR_RECEIPT': params['nds_for_receipt']})
        if 'partner_commission_pct2' in params:
            expected_data.update({'PARTNER_COMMISSION_PCT2': params['partner_commission_pct2']})
        if 'netting_pct' in params:
            expected_data.update({'NETTING_PCT': params['netting_pct']})

        expected_data.update({
            'ctype': ContractTypes('GENERAL'),
            'active': True,
            'signed': True,
            'suspended': False,
            'person_id': params['person_id'],
            'is_cancelled': None,
            'is_faxed': None
        })

        if 111 in params['services']:
            expected_data.update({'offer_accepted': None})
        return expected_data

    @staticmethod
    def get_contract_attr(obj, attr):
        for o in (obj.current_state(), obj, obj.col0):
            try:
                return getattr(o, attr.lower())
            except AttributeError:
                continue
        return None

    def contract_contains(self, contract, params=None):
        if params is None:
            return True
        for param_key in params:
            try:
                val = self.get_contract_attr(contract, param_key)
            except AttributeError:
                return False
            if val != params[param_key]:
                if isinstance(val, Iterable) and set(val) == set(params[param_key]):
                    continue
                if isinstance(val, ContractTypes) and val.type == params[param_key].type:
                    continue
                return False
        return True

    def check_create_contract_res_params(self, res, manager, params):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert self.contract_contains(contract_obj, self.expected_params(manager, params))

    def check_create_contract_res_only_one(self, res, client_id, is_spendable):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        contract = contract_obj.current_state()
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert contract.GEOBASE_REGION == 163

        contract_type = 'SPENDABLE' if is_spendable == '1' else 'GENERAL'
        client_contracts = self.session.query(mapper.Contract). \
            filter(and_(mapper.Contract.client_id == client_id,
                        mapper.Contract.type == contract_type)).all()
        assert len(client_contracts) == 1


class TestCreateOfferForTaxi(ContractUtils):
    # Набор тестов, которые не должны вызывать исключения
    @pytest.mark.parametrize(
        'additional_params',
        [
            dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'),
                 services=[128, 124, 125, 666, 667, 111, 626], nds_for_receipt=NDS_DEFAULT),
            # временно работает до миграции по добавлению во все договоры такси РФ 666 и 667 сервиса.
            # после нужно перенести в ошибки
            dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'),
                 services=[128, 124, 125, 111, 626], nds_for_receipt=NDS_DEFAULT),
            dict(firm_id=FirmId.TAXI_EU_BV, payment_type=2, advance_payment_sum=D('1000'),
                 services=[128, 124, 111], partner_commission_pct2=D('0')),
            dict(firm_id=FirmId.TAXI_KZ, payment_type=2, advance_payment_sum=D('1000'),
                 services=[128, 124, 125], partner_commission_pct2=D('0.01')),
            dict(firm_id=FirmId.TAXI_AM, payment_type=2, advance_payment_sum=D('1000'),
                 services=[128, 124], partner_commission_pct2=D('0.01')),
            dict(firm_id=FirmId.TAXI_UA, payment_type=2, advance_payment_sum=D('1000'),
                 services=[128, 124], partner_commission_pct2=D('20')),
            dict(firm_id=FirmId.TAXI, payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 124, 125, 111, 626], nds_for_receipt=NDS_DEFAULT),
            dict(firm_id=FirmId.TAXI, start_dt=START_DT_PAST,
                 payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 124, 125, 111, 626], nds_for_receipt=NDS_DEFAULT),
            dict(firm_id=FirmId.TAXI, payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 124, 125, 111, 626], netting=1, netting_pct=D('0.01'),
                 nds_for_receipt=NDS_DEFAULT),
            dict(firm_id=FirmId.TAXI, payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 124, 125, 111, 626], netting=1, netting_pct=D('300'), nds_for_receipt=NDS_DEFAULT),
            dict(firm_id=FirmId.TAXI, payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 124, 125, 111, 626], nds_for_receipt=NDS_NOT_RESIDENT),
            dict(firm_id=FirmId.TAXI, payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 124, 125, 111, 626], nds_for_receipt=WO_NDS_RECEIPT),
            dict(firm_id=FirmId.TAXI_EU_BV, payment_type=3, payment_term=10, service_min_cost=D('1000'),
                 services=[128, 111]),
            dict(firm_id=FirmId.TAXI, payment_type=3, payment_term=10, advance_payment_sum=D('1000'),
                 services=[128, 124, 125, 111, 626], nds_for_receipt=NDS_DEFAULT, service_min_cost=D('0'))
        ],
        ids=[
            'CreateOffer with Yandex Taxi LLC prepay wo netting nds_for_receipt = 18',
            'CreateOffer with Yandex Taxi LLC prepay wo netting nds_for_receipt = 18 WO RU CABCOMPANIES TEMPORARY NO ERROR',
            'CreateOffer with Yandex Taxi BV prepay and partner_commission_pct2 = 0',
            'CreateOffer with Yandex Taxi KZT prepay and partner_commission_pct2 = 0,01',
            'CreateOffer with Yandex Taxi ARM prepay and partner_commission_pct2 = 0,01',
            'CreateOffer with Yandex Taxi UKR prepay and partner_commission_pct2 = 20',
            'CreateOffer with Yandex Taxi LLC postpay wo netting nds_for_receipt = 18',
            'CreateOffer with Yandex Taxi LLC postpay wo netting with specified start dt nds_for_receipt = 18',
            'CreateOffer with Yandex Taxi LLC postpay with netting pct = 0,01 nds_for_receipt = 18',
            'CreateOffer with Yandex Taxi LLC postpay with netting pct = 300 nds_for_receipt = 18',
            'CreateOffer with Yandex Taxi LLC postpay wo netting nds_for_receipt = 0',
            'CreateOffer with Yandex Taxi LLC postpay wo netting nds_for_receipt = -1 (wo nds)',
            'CreateOffer with Yandex Taxi BV postpay wo 124',
            'CreateOffer with Yandex Taxi LLC postpay with service_min_cost = 0'
        ]
    )
    def test_check_taxi_create_offer(self, some_manager, additional_params):
        person = ob.PersonBuilder.construct(self.session, type=self.FIRMTAXI[additional_params['firm_id']].person_type)

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Набор тестов, которые должны вызвать исключение
    @pytest.mark.parametrize(
        'additional_params, failure_msg',
        [
            (dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'),
                  services=[128, 124, 111, 125]), u"Rule violation: 'Не заполнена ставка для НДС'"),
            (dict(firm_id=FirmId.TAXI_EU_BV, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124],
                  nds_for_receipt=NDS_ZERO), u"Rule violation: 'Не заполнен процент комиссии с карт'"),
            (dict(firm_id=FirmId.TAXI_AM, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124],
                  nds_for_receipt=NDS_ZERO), u"Rule violation: 'Не заполнен процент комиссии с карт'"),
            (dict(firm_id=FirmId.TAXI_KZ, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 125],
                  nds_for_receipt=NDS_ZERO), u"Rule violation: 'Не заполнен процент комиссии с карт'"),
            (dict(firm_id=FirmId.TAXI_UA, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124],
                  nds_for_receipt=NDS_ZERO), u"Rule violation: 'Не заполнен процент комиссии с карт'"),
            (dict(firm_id=FirmId.TAXI_EU_BV, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124],
                  nds_for_receipt=NDS_ZERO, partner_commission_pct2=D('20.01')),
             u"Rule violation: 'Значение процента комиссии (20.01%) выходит за пределы допустимого интервала [0, 20].'"),
            (
                    dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'),
                         services=[128, 124, 111, 125],
                         netting=1, netting_pct=D('0'), nds_for_receipt=NDS_DEFAULT),
                    u"Rule violation: 'Значение процента взаимозачёта (0.0%) выходит за пределы допустимого интервала (0, 300].'"),
            (
                    dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'),
                         services=[128, 124, 111, 125],
                         netting=1, netting_pct=D('300.01'), nds_for_receipt=NDS_DEFAULT),
                    u"Rule violation: 'Значение процента взаимозачёта (300.01%) выходит за пределы допустимого интервала (0, 300].'"),
            (dict(firm_id=FirmId.TAXI_EU_BV, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 111],
                  nds_for_receipt=NDS_ZERO, partner_commission_pct2=D('1'), country=None),
             u"Rule violation: 'Не выбрана страна'"),
            (dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 111],
                  netting=1, netting_pct=None, nds_for_receipt=NDS_DEFAULT),
             u"procedure '_CreateCommonContract': parameter[1]: hash item 'netting_pct': invalid decimal value: None"),
            (dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'), services=[124, 125],
                  netting=1, netting_pct=D('1'), nds_for_receipt=NDS_DEFAULT),
             u"Rule violation: 'Должны быть подключены оба сервиса на Яндекс.Такси платежи картой'"),
            (dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'), services=[128], netting=1,
                  netting_pct=D('1'), nds_for_receipt=NDS_DEFAULT),
             u"Rule violation: 'Должны быть подключены оба сервиса на Яндекс.Такси платежи картой'"),
            (dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 111],
                  nds_for_receipt=NDS_ZERO),
             u"Rule violation: 'Выбраны не все платежные сервисы (Яндекс.Такси: Платежи, Яндекс.Убер: Платежи, Такси.Везёт, Такси.РуТакси)'"),
            (dict(firm_id=FirmId.TAXI_KZ, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124],
                  nds_for_receipt=NDS_ZERO, partner_commission_pct2=D('0')),
             u"Rule violation: 'Выбраны не все платежные сервисы (Яндекс.Такси: Платежи, Яндекс.Убер: Платежи)'"),
            (dict(firm_id=FirmId.TAXI_EU_BV, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 125],
                  nds_for_receipt=NDS_ZERO, partner_commission_pct2=D('0')),
             u"Rule violation: 'Нельзя выбрать сервис Яндекс.Убер: Платежи в Yandex.Taxi B.V.'"),
            (dict(firm_id=FirmId.TAXI_AM, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 125],
                  nds_for_receipt=NDS_ZERO, partner_commission_pct2=D('0')),
             u"Rule violation: 'В фирме Yandex.Taxi AM LLP нельзя выбрать сервис Яндекс.Убер: Платежи'"),
            (
                    dict(firm_id=FirmId.TAXI, payment_type=2, advance_payment_sum=D('1000'),
                         services=[128, 124, 125, 626],
                         nds_for_receipt=NDS_DEFAULT),
                    u"Rule violation: 'Яндекс.Такси: Шереметьево может быть выбрано только вместе с Яндекс.Такси'"),
            (dict(firm_id=FirmId.TAXI_KZ, payment_type=2, advance_payment_sum=D('1000'), services=[128, 124, 125, 666, 667],
                  nds_for_receipt=NDS_ZERO, partner_commission_pct2=D('0')),
             u"Rule violation: 'Нельзя выбрать сервисы Такси.Везёт, Такси.РуТакси не в России'"),
        ],
        ids=[
            'CreateOffer with Yandex Taxi LLC wo nds_for_receipt',
            'CreateOffer with Yandex Taxi BV wo commission pct',
            'CreateOffer with Yandex Taxi ARM wo commission pct',
            'CreateOffer with Yandex Taxi KZT wo commission pct',
            'CreateOffer with Yandex Taxi UKR wo commission pct',
            'CreateOffer with Yandex Taxi BV with commission pct > 20',
            'CreateOffer with Yandex Taxi LLC with netting pct = 0',
            'CreateOffer with Yandex Taxi LLC with netting pct > 300',
            'CreateOffer with Yandex Taxi BV wo country',
            'CreateOffer with Yandex Taxi LLC with netting wo netting pct',
            'CreateOffer with Yandex Taxi LLC with 124, 125 wo 128',
            'CreateOffer with Yandex Taxi LLC with 128 wo 124',
            'CreateOffer with Yandex Taxi LLC with 124 wo 125',
            'CreateOffer with Yandex Taxi KZT with 124 wo 125',
            'CreateOffer with Yandex Taxi BV with 125',
            'CreateOffer with Yandex Taxi ARM with 125',
            'CreateOffer with Yandex Taxi SVO wo 111',
            'CreateOffer with Yandex Taxi KZT with 666, 667',
        ])
    def test_check_errors_taxi_create_offer(self, some_manager, additional_params, failure_msg):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=self.FIRMTAXI[additional_params['firm_id']].person_type)
        person = obj_builder.build(self.session).obj

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id})

        with pytest.raises(Exception) as exc:
            self.new_offer(some_manager, additional_params=additional_params)
        assert failure_msg in exc.value.faultString

    # тест правильности создания обычных и расходных договоров
    @pytest.mark.parametrize(
        'additional_params',
        [
            (dict(firm_id=FirmId.TAXI_UA, services=[128, 125], is_spendable=0)),
            (dict(firm_id=FirmId.TAXI_EU_BV, services=[135, 651], is_spendable=1))
        ],
        ids=['GENERAL', 'SPENDABLE'])
    def test_check_geo_region_contract_field_correct_storage(self, some_manager, additional_params):
        if additional_params['is_spendable']:
            is_partner = '1'
        else:
            is_partner = '0'
            additional_params.update({'payment_type': 2,
                                      'advance_payment_sum': D('1000'),
                                      'partner_commission_pct2': 0})
        additional_params.pop('is_spendable')

        additional_params.update({'geobase_region': 163,
                                  'payment_sum': 8000,
                                  'pay_to': OebsPayReceiveType.TINKOFF,
                                  'nds': NDS_DEFAULT,
                                  'nds_for_receipt': NDS_DEFAULT})

        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=self.FIRMTAXI[additional_params['firm_id']].person_type, is_partner=is_partner)
        person = obj_builder.build(self.session).obj

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_only_one(res, person.client.id, is_partner)
