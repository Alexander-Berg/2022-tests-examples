# coding: utf-8
__author__ = 'chihiro'

from datetime import timedelta

import pytest
from hamcrest import equal_to, assert_that, contains_string
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils as b_utils
import btestlib.reporter as reporter
from btestlib.data import person_defaults
from btestlib.constants import  Services, Products
import check.db
import balance.balance_db as db

from check import steps as check_steps
from check import shared_steps
from check import db as check_db
from check.utils import relative_date, LAST_DAY_OF_MONTH
from check import utils
from check.shared import CheckSharedBefore
from check.defaults import LAST_DAY_OF_PREVIOUS_MONTH

FIRST_MONTH = b_utils.Date.first_day_of_month() - relativedelta(months=1)
END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)

CHECK_DEFAULTS = {
    'aob_market':
        {'service_id': Services.MARKET.id,
         'product_id': Products.MARKET.id,
         'paysys_id': 1003,
         'person_category': 'ur',
         'person_additional_params': None,
         'firm_id': 111},
    'aob_tr':
        {'service_id': Services.DIRECT.id,
         'product_id': Products.DIRECT_FISH.id,
         'paysys_id': 1050,
         'person_category': 'tru',
         'person_additional_params': None,
         'firm_id': 8},
    'aob_sw':
        {'service_id': Services.DIRECT.id,
         'product_id': Products.DIRECT_FISH.id,
         'paysys_id': 1045,
         'person_category': 'sw_ur',
         'person_additional_params': None,
         'firm_id': 7},
    'aob':
        {'service_id': Services.DIRECT.id,
         'product_id': Products.DIRECT_FISH.id,
         'paysys_id': 1003,
         'person_category': 'ur',
         'firm_id': 1,
         'person_additional_params': None},
    'aob_taxi':
        {'person_additional_params': {'kpp': '234567890'},
         'person_category': 'ur',
         'firm_id': 13},
    'aob_us':
        {'service_id': Services.DIRECT.id,
         'product_id': Products.DIRECT_FISH.id,
         'paysys_id': 1028,
         'person_category': 'usu',
         'person_additional_params': None,
         'firm_id': 4},
    'aob_vertical':
        {'service_id': Services.REALTY.id,
         'product_id': Products.REALTY2.id,
         'paysys_id': 1201003,
         'person_category': 'ur',
         'person_additional_params': None,
         'firm_id': 12},
    'aob_services':
        {'service_id': Services.TOLOKA.id,
         'product_id': Products.TOLOKA.id,
         'paysys_id': 1601047,
         'person_category': 'sw_yt',
         'person_additional_params': None,
         'firm_id': 16},
    'aob_health':
        {'person_category': 'ur',
         'firm_id': 114},
    'aob_uber_ml_bv':
        {'person_category': 'ur',
         'firm_id': 115},
    'aob_kinopoisk':
        {'person_category': 'ur',
         'firm_id': 9},
    'aob_uber_kz':
        {'person_category': 'ur',
         'firm_id': 31},
    'aob_israel_go':
        {'person_category': 'ur',
         'firm_id': 35},
    'aob_taxi_am':
        {'person_category': 'ur',
         'firm_id': 26},
    'aob_taxi_bv':
        {'person_category': 'ur',
         'firm_id': 22},
    'aob_taxi_kz':
        {'person_category': 'ur',
         'firm_id': 24},
    'aob_hk_ecommerce':
        {'person_category': 'ur',
         'firm_id': 33},
    'aob_ya_cloud':
        {'person_category': 'ur',
         'firm_id': 123},
    'aob_gas':
        {'person_category': 'ur',
         'firm_id': 124},
    'aob_drive':
        {'person_category': 'ur',
         'firm_id': 30},
    'aob_uber_az':
        {'person_category': 'ur',
         'firm_id': 116},
    'aob_mlu_europe_bv':
        {'person_category': 'ur',
         'firm_id': 125},
    'aob_mlu_africa_bv':
        {'person_category': 'ur',
         'firm_id': 126},
}


def create_client_and_person(check_code):
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(
        client_id, person_category=CHECK_DEFAULTS[check_code]['person_category'],
        additional_params=CHECK_DEFAULTS[check_code].get('person_additional_params', None)
    )
    steps.ExportSteps.export_oebs(client_id=client_id)
    return client_id, person_id


class StepsAob(object):
    CHECK_CODE = 'aob'

    def create_acted_orders_(self, check_code=None):
        client_id, person_id = create_client_and_person(self.CHECK_CODE)
        check_defaults = CHECK_DEFAULTS[self.CHECK_CODE]
        act_map = check_steps.create_act_map(
            {1: {'paysys_id': check_defaults.get('paysys_id'),
                 'service_id': check_defaults.get('service_id'),
                 'product_id': check_defaults.get('product_id'),
                 'shipment_info': {'Bucks': 30}}
             }, client_id, person_id, firm_id=CHECK_DEFAULTS[check_code]['firm_id'],
            act_needed=True)
        steps.ExportSteps.export_oebs(person_id=person_id)
        steps.ExportSteps.export_oebs(invoice_id=act_map['invoice']['id'])
        steps.ExportSteps.export_oebs(act_id=act_map['id'])
        return act_map


class TestAob(StepsAob):
    CHECK_CODE = 'aob'
    DIFFS_COUNT = 5

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_sum_not_converge(self, shared_data):
        """
        https://st.yandex-team.ru/CHECK-2750
        Проверяем, что комментарий в тикете о расхождении содержит текст:
        "... не превышает сумму в ..."
        """
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 113}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

        min_threshold = '10000'

        comment_text = u' не превышает сумму в {}'.format(min_threshold)

        ticket = utils.get_check_ticket(StepsAob.CHECK_CODE, cmp_id)
        comments = list(ticket.comments.get_all())
        for comment in comments:
            if min_threshold in comment.text:
                b_utils.check_that(comment.text, contains_string(comment_text),
                                   u'Проверяем, что в комментарии содержится требуемый текст')
                break
            else:
                assert False, u'Требуемый комментарий авторазбора не найден'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            # Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально

        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))



# Шаблон для тестов
def aob_fabric(service):
    class aob_fabric(object):
        CHECK_CODE = service.check_name
        DIFFS_COUNT = 5
        SHARED_NAME = service.shared_name
        SERVICE_NAME = service.service_name

        @classmethod
        def create_acted_orders_(cls):
            act_id, external_id = check_steps.create_act_for_aob(cls.SERVICE_NAME, FIRST_MONTH,
                                                                 CHECK_DEFAULTS[cls.CHECK_CODE]['firm_id'])
            act_map = {'id': act_id,
                       'eid': external_id}
            invoice_id, _ = check.db.get_invoice_by_act(act_id)
            act_map['invoice'] = {'id': invoice_id}

            steps.ExportSteps.export_oebs(invoice_id=act_map['invoice']['id'], act_id=act_map['id'])

            return act_map

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_without_diff(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['act_map']) as before:
                before.validate()
                act_map = self.create_acted_orders_()

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert act_map['eid'] not in [row['eid'] for row in cmp_data]

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_not_found_in_billing(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['act_map']) as before:
                before.validate()
                act_map = self.create_acted_orders_()

                check_steps.change_external_id(act_map['id'], object_='act')

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_not_found_in_oebs(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['act_map']) as before:
                before.validate()
                act_map = self.create_acted_orders_()

                check_steps.change_external_id(act_map['id'], object_='act')
                act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                      {'act_id': act_map['id']})[0]['external_id']

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_sum_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['act_map']) as before:
                before.validate()
                act_map = self.create_acted_orders_()

                db.balance().execute(
                    'update t_act set amount = :changed_amount where id = :id',
                    {'id': act_map['id'], 'changed_amount': 113}
                )

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_date_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['act_map']) as before:
                before.validate()
                act_map = self.create_acted_orders_()

                db.balance().execute(
                    'update t_act set dt =:changed_date where id = :id',
                    {
                        'id': act_map['id'],
                        'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                    }
                )

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_person_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['act_map']) as before:
                before.validate()
                act_map = self.create_acted_orders_()

                #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
                try:
                    check.db.wait_export('Act', 'OEBS_API', act_map['id'])
                except IndexError:
                    check.db.wait_export('Act', 'OEBS', act_map['id'])

                client_id, person_id = create_client_and_person(self.CHECK_CODE)
                db.balance().execute(
                    'update t_invoice set person_id = :person_id where id = :id',
                    {'id': act_map['invoice']['id'],
                     'person_id': steps.PersonSteps.create(
                         client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                         inn_type=person_defaults.InnType.UNIQUE)}
                )

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_check_diffs_count(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['cache_var']) as before:
                before.validate()
                cache_var = 'test'

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert len(cmp_data) == self.DIFFS_COUNT

        @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
        @pytest.mark.shared(block=SHARED_NAME)
        def test_aob_use_yt(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['cache_var']) as before:
                before.validate()
                cache_var = 'test'

            cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            # будет падать локально - но вроде этот тест никому не нужен локально

            cmp_id = shared_data.cache.get('cmp_id_yt')

            diff_acts_yt = check_db.get_cmp_diff(
                cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                        'aob_us'] else 'aob')
            assert_that(diff_acts_yt, equal_to(cmp_data))

    return aob_fabric


########################################################################################################
# Указывем реквизиты для тестов для каждого сервиса

from collections import namedtuple

services = namedtuple('service', 'shared_name check_name service_name')

KINOPOISK =    services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_KINOPOISK,
                        check_name   = 'aob_kinopoisk',
                        service_name = 'kinopoisk')
HEALTH =       services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_HEALTH,
                        check_name   = 'aob_health',
                        service_name = 'health')
UBER_ML_BV =   services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_UBER_ML_BV,
                        check_name   = 'aob_uber_ml_bv',
                        service_name = 'uber_ml_bv')
ISRAEL_GO =    services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_ISRAEL_GO,
                        check_name   = 'aob_israel_go',
                        service_name = 'israel_go')
TAXI_AM =      services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_TAXI_AM,
                        check_name   = 'aob_taxi_am',
                        service_name = 'taxi_am')
TAXI_BV =      services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_TAXI_BV,
                        check_name   = 'aob_taxi_bv',
                        service_name = 'taxi_bv')
TAXI_KZ =      services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_TAXI_KZ,
                        check_name   = 'aob_taxi_kz',
                        service_name = 'taxi_kz')
DRIVE =        services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_DRIVE,
                        check_name   = 'aob_drive',
                        service_name = 'drive')
TAXI =         services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_TAXI,
                        check_name   = 'aob_taxi',
                        service_name = 'taxi')
HK_ECOMMERCE = services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_HK_ECOMMERCE,
                        check_name   = 'aob_hk_ecommerce',
                        service_name = 'hk_ecommerce')
YA_CLOUD =     services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_YA_CLOUD,
                        check_name   = 'aob_ya_cloud',
                        service_name =  'ya_cloud')
GAS =          services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_GAS,
                        check_name   = 'aob_gas',
                        service_name =  'gas')
UBER_AZ =      services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_UBER_AZ,
                        check_name   = 'aob_uber_az',
                        service_name = 'uber_az')
UBER_KZ =      services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_UBER_KZ,
                        check_name   = 'aob_uber_kz',
                        service_name = 'uber_kz')
MLU_EUROPE_BV =services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_MLU_EUROPE_BV,
                        check_name   = 'aob_mlu_europe_bv',
                        service_name = 'mlu_europe_bv')

MLU_AFRICA_BV =services(shared_name  = shared_steps.SharedBlocks.RUN_AOB_MLU_AFRICA_BV,
                        check_name   = 'aob_mlu_africa_bv',
                        service_name = 'mlu_africa_bv')

########################################################################################################
#Тут непосредственно вызываем тесты для каждого сервиса

class TestAobKinopoisk(aob_fabric(KINOPOISK)):
    """
    Тесты для Kinopoisk полностью совпадают с описанными в aob_fabric
    """


class TestAobHealth(aob_fabric(HEALTH)):
    """
        Тесты для Health полностью совпадают с описанными в aob_fabric
    """


class TestAobUberMlBv(aob_fabric(UBER_ML_BV)):
    """
    Тесты для UberMlBv полностью совпадают с описанными в aob_fabric
    """


class TestAobIsraelGo(aob_fabric(ISRAEL_GO)):
    """
    Тесты для IsraelGo полностью совпадают с описанными в aob_fabric
    """


class TestAobTaxiAm(aob_fabric(TAXI_AM)):
    """
    Тесты для TaxiAm полностью совпадают с описанными в aob_fabric
    """


class TestAobTaxiBv(aob_fabric(TAXI_BV)):
    """
    Тесты для TaxiBv полностью совпадают с описанными в aob_fabric
    """


class TestAobTaxiKz(aob_fabric(TAXI_KZ)):
    """
    Тесты для TaxiKz полностью совпадают с описанными в aob_fabric
    """


class TestAobDrive(aob_fabric(DRIVE)):
    """
    Тесты для Drive полностью совпадают с описанными в aob_fabric
    """


class TestAobTaxi(aob_fabric(TAXI)):
    """
    Тесты для Taxi полностью совпадают с описанными в aob_fabric
    """


class TestAobHkEcommerce(aob_fabric(HK_ECOMMERCE)):
    """
    Тесты для HkEcommerce полностью совпадают с описанными в aob_fabric
    """


class TestAobYaCloud(aob_fabric(YA_CLOUD)):
    """
    Тесты для YaCloud полностью совпадают с описанными в aob_fabric
    """


class TestAobGas(aob_fabric(GAS)):
    """
    Тесты для Gas полностью совпадают с описанными в aob_fabric
    """


class TestAobUberAZ(aob_fabric(UBER_AZ)):
    """
    Тесты для UberAZ полностью совпадают с описанными в aob_fabric
    """


class TestAobUberKZ(aob_fabric(UBER_KZ)):
    """
    Тесты для UberKZ полностью совпадают с описанными в aob_fabric
    """


class TestAobMluEuropeBv(aob_fabric(MLU_EUROPE_BV)):
    """
    Тесты для MLU_EUROPE_BV полностью совпадают с описанными в aob_fabric
    """


class TestAobMluAfricaBv(aob_fabric(MLU_AFRICA_BV)):
    """
    Тесты для MLU_AFRICA_BV полностью совпадают с описанными в aob_fabric
    """

########################################################################################################

class TestAobGasSales(object):
    CHECK_CODE = 'aob_gas'
    SERVICE_NAME = 'gas_sales'

    @classmethod
    def create_acted_orders_(cls):
        act_id, external_id = check_steps.create_act_for_aob(cls.SERVICE_NAME, FIRST_MONTH,
                                                             CHECK_DEFAULTS[cls.CHECK_CODE]['firm_id'])
        act_map = {'id': act_id,
                   'eid': external_id}
        invoice_id, _ = check.db.get_invoice_by_act(act_id)
        act_map['invoice'] = {'id': invoice_id}

        steps.ExportSteps.export_oebs(invoice_id=act_map['invoice']['id'], act_id=act_map['id'])

        return act_map

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_GAS)
    def test_aob_check_2765_diff_without_diff(self, shared_data):
        """
        https://st.yandex-team.ru/CHECK-2765
        Проверяем, что сервис 636 не участвует в проверке
        """
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 113}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]


class TestAobMarket(StepsAob):
    CHECK_CODE = 'aob_market'
    DIFFS_COUNT = 5

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]


    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_sum_not_converge(self, shared_data):
        """
           https://st.yandex-team.ru/CHECK-2750
           Проверяем, что комментарий в тикете о расхождении содержит текст:
           "... превышает сумму ..."
        """
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 400000}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')
        reporter.log("CMP_DATA = {}".format(cmp_data))
        cmp_id = cmp_data[0]['cmp_id']

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

        max_threshold = '300000'

        comment_text = u'превышает сумму {}'.format(max_threshold)

        ticket = utils.get_check_ticket(StepsAob.CHECK_CODE, cmp_id)
        comments = list(ticket.comments.get_all())
        for comment in comments:
            if max_threshold in comment.text:
                b_utils.check_that(comment.text, contains_string(comment_text),
                                   u'Проверяем, что в комментарии содержится требуемый текст')
                break
            else:
                assert False, u'Требуемый комментарий авторазбора не найден'



    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_MARKET)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально
        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))


class TestAobServices(StepsAob):
    CHECK_CODE = 'aob_services'
    DIFFS_COUNT = 5

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_sum_not_converge(self, shared_data):
        """
               https://st.yandex-team.ru/CHECK-2750
               Проверяем, что комментарий в тикете о расхождении содержит текст:
               "... превышает сумму в {} руб. и не превышает {}..."
        """
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 1000}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]


        min_threshold = '10000'
        max_threshold = '300000'

        comment_text = u'превышает сумму в {0} руб. и не превышает {1}'.format(min_threshold, max_threshold)

        ticket = utils.get_check_ticket(StepsAob.CHECK_CODE, cmp_id)
        comments = list(ticket.comments.get_all())
        for comment in comments:
            b_utils.check_that(comment.text, contains_string(comment_text),
                               u'Проверяем, что в комментарии содержится требуемый текст')
            b_utils.check_that(comment.text, contains_string(min_threshold),
                               u'Проверяем, что в комментарии содержится минимальный порог')
            b_utils.check_that(comment.text, contains_string(max_threshold),
                               u'Проверяем, что в комментарии содержится максимальный порог')

            break
        else:
            assert False, u'Требуемый комментарий авторазбора не найден'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SERVICES)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально

        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))


class TestAobSW(StepsAob):
    CHECK_CODE = 'aob_sw'
    DIFFS_COUNT = 5

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_sum_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 113}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_SW)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально

        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))


class TestAobTR(StepsAob):
    CHECK_CODE = 'aob_tr'
    DIFFS_COUNT = 5

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_sum_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 113}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_TR)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально

        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))


class TestAobUS(StepsAob):
    CHECK_CODE = 'aob_us'
    DIFFS_COUNT = 5

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_sum_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 113}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_(self.CHECK_CODE)

            #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_US)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально

        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))


class TestAobVertical(object):
    CHECK_CODE = 'aob_vertical'
    DIFFS_COUNT = 5

    @staticmethod
    def create_acted_orders_():
        client_id = steps.ClientSteps.create()
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
        person_id = steps.PersonSteps.create(agency_id, 'ur')
        steps.ExportSteps.export_oebs(client_id=agency_id)
        steps.ExportSteps.export_oebs(client_id=client_id)
        act_id, act_eid = check_steps.create_vertical_act(client_id, agency_id, person_id,
                                                          firm_id=CHECK_DEFAULTS['aob_vertical']['firm_id'])
        act_map = {'id': act_id,
                   'eid': act_eid
                   }
        invoice_id, _ = check.db.get_invoice_by_act(act_id)
        act_map['invoice'] = {'id': invoice_id}
        steps.ExportSteps.export_oebs(person_id=person_id)
        steps.ExportSteps.export_oebs(invoice_id=act_map['invoice']['id'])
        steps.ExportSteps.export_oebs(act_id=act_map['id'])
        return act_map

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert act_map['eid'] not in [row['eid'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

            check_steps.change_external_id(act_map['id'], object_='act')

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

            check_steps.change_external_id(act_map['id'], object_='act')
            act_map['eid'] = db.balance().execute('select external_id from bo.T_ACT where id= :act_id',
                                                  {'act_id': act_map['id']})[0]['external_id']

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 1) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_sum_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

            db.balance().execute(
                'update t_act set amount = :changed_amount where id = :id',
                {'id': act_map['id'], 'changed_amount': 113}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 3) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_date_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

            db.balance().execute(
                'update t_act set dt =:changed_date where id = :id',
                {
                    'id': act_map['id'],
                    'changed_date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=3)
                }
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 4) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_person_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['act_map']) as before:
            before.validate()
            act_map = self.create_acted_orders_()

            #Сейчас всегда тип OEBS_API, но могут быть тесты на стороне Баланса и тогда может стать OEBS
            try:
                check.db.wait_export('Act', 'OEBS_API', act_map['id'])
            except IndexError:
                check.db.wait_export('Act', 'OEBS', act_map['id'])

            client_id, person_id = create_client_and_person(self.CHECK_CODE)
            db.balance().execute(
                'update t_invoice set person_id = :person_id where id = :id',
                {'id': act_map['invoice']['id'],
                 'person_id': steps.PersonSteps.create(
                     client_id, CHECK_DEFAULTS[self.CHECK_CODE]['person_category'],
                     inn_type=person_defaults.InnType.UNIQUE)}
            )

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert (act_map['eid'], 5) in [(row['eid'], row['state']) for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

    @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AOB_VERTICAL)
    def test_aob_use_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_aob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        # будет падать локально - но вроде этот тест никому не нужен локально

        cmp_id = shared_data.cache.get('cmp_id_yt')

        diff_acts_yt = check_db.get_cmp_diff(
            cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                    'aob_us'] else 'aob')
        assert_that(diff_acts_yt, equal_to(cmp_data))
