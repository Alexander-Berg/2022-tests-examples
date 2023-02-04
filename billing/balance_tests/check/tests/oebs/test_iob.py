# coding=utf-8

import json
import os

import pytest
import datetime
from hamcrest import equal_to, contains_string, is_in, empty, has_length
from decimal import Decimal as D
from startrek_client import Startrek

from balance import balance_api as api
from balance import balance_steps as steps
from balance.balance_db import balance
from btestlib import utils as butils
import btestlib.reporter as reporter
from btestlib.constants import TaxiOrderType, PaymentType, OEBSOperationType, Export
from check import db
from check import retrying, shared_steps
from check import steps as check_steps
from check import db as check_db
from check import utils
from check.db import get_person_type_and_cliend_id_by_invoice
from check.defaults import DATA_DIR
from check.defaults import Services, Products, STARTREK_PARAMS
from check.shared import CheckSharedBefore
from check.utils import need_data_regeneration
from dateutil.relativedelta import relativedelta
from temp.igogor.balance_objects import Contexts


"""
Осуществляет сравнение суммы счета в рамках одного заказа в Биллинге и OEBS
Типы расхождений:
    * 1 - Отсутствует в OEBS
    * 2 - Отсутствует в Биллинге
    * 3 - Расходятся суммы акта
    * 4 - Расходятся даты акта
    * 5 - Расходятся плательщики
    * 6 - Расходятся даты счета
  

**Забираем данные из Биллинга (в таблицу cmp.iob_bill)**
 
Свойства данных для забора:
    - счет, по которому выставлен акт, сформирован во внешней системе
    - счет - не сертификатный
    - счет не является счетом на покупку через приложение
    - у счета имеется плательщик
    - тип счета - не 'fictive_personal_account', 'fictive', 'charge_note', 'bonus_account'
    - у счета статус !=5 (готов к отправке), т е либо оплачен, либо закрыт, либо частично оплачен, либо ожидает оплаты, либо состояние не известно
    - клиент - не внутренний (разработчики, менеджеры, поддержка)
    - для 116 сервиса валюта не 'USD'
    - для 119, 118 сервисов только предоплатные счета
    - выбираются не удаленные не внутренние акты за предыдущий месяц
    - происходит сортировка счета по firm_id для каждого типа сверки (Россия - 1, 12, 13)
    - осуществляется переход на бизнес-юниты
    
**Забираем данные из OEBS (в таблицу cmp.iob_oebs)**
Здесь забираем счета, импортированные из биллинга за предыдущий месяц
"""


CHECK_DEFAULTS = {    # 12 всего
    'iob_services':
        {'service_id': Services.service_ag,
         'product_id': Products.service_ag,
         'paysys_id': 1601047,
         'person_category': 'sw_yt',
         'person_additional_params': None,
         'firm_id': 16},
    'iob_market':
        {'service_id': Services.market,
         'product_id': Products.market,
         'paysys_id': 1003,
         'person_category': 'ur',
         'person_additional_params': None,
         'firm_id': 111},
    'iob_tr':
        {'service_id': Services.direct,
         'product_id': Products.direct_pcs,
         'paysys_id': 1050,
         'person_category': 'tru',
         'person_additional_params': None},
    'iob_sw':
        {'service_id': Services.direct,
         'product_id': Products.direct_pcs,
         'paysys_id': 1045,
         'person_category': 'sw_ur',
         'person_additional_params': None},
    'iob':
        {'service_id': Services.direct,
         'product_id': Products.direct_pcs,
         'paysys_id': 1003,
         'person_category': 'ur',
         'person_additional_params': None,
         'firm_id': 1},
    'iob_us':
        {'service_id': Services.direct,
         'product_id': Products.direct_pcs,
         'paysys_id': 1028,
         'person_category': 'usu',
         'person_additional_params': None},
    'iob_vertical':
        {'service_id': Services.vertical,
         'product_id': Products.vertical,
         'paysys_id': 1201003,
         'person_category': 'ur',
         'person_additional_params': None,
         'firm_id': 12}
}

check_list = CHECK_DEFAULTS.keys()


def create_client_and_person(check_code):
    if check_code == 'iob_vertical':
        client_id = steps.ClientSteps.create()
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
        person_id = steps.PersonSteps.create(agency_id, 'ur')
        steps.ExportSteps.export_oebs(client_id=agency_id)
    else:
        client_id = check_steps.create_client()
        person_id = check_steps.create_person(
                            client_id,
                            person_category=CHECK_DEFAULTS[check_code]['person_category'],
                            additional_params=CHECK_DEFAULTS[check_code].get('person_additional_params', None)
        )
        agency_id = None
    steps.ExportSteps.export_oebs(client_id=client_id)

    return client_id, person_id, agency_id


@retrying.retry(stop_max_attempt_number=5, wait_exponential_multiplier=1 * 1000)
def export(inv_id):
    steps.ExportSteps.export_oebs(invoice_id=inv_id)


class StepsIob(object):
    CHECK_CODE = 'iob'

    def create_invoice(self, check_code):
        client_id, person_id, agency_id = create_client_and_person(self.CHECK_CODE)
        check_defaults = CHECK_DEFAULTS[self.CHECK_CODE]

        orders_map = {1: {
            'paysys_id':     check_defaults.get('paysys_id'),
            'service_id':    check_defaults.get('service_id'),
            'product_id':    check_defaults.get('product_id'),
            'shipment_info': {'Bucks': 30}
        }}

        if check_code == 'iob_vertical':
            _, inv_id, inv_eid = check_steps.create_vertical_invoice(client_id, agency_id, person_id)
            invoice_map = {'id': inv_id,
                           'eid': inv_eid}

        else:
            invoice_map = check_steps.create_invoice_map(orders_map, client_id, person_id)

        steps.ExportSteps.export_oebs(person_id=person_id)
        export(invoice_map['id'])
        return invoice_map


class TestIob(StepsIob):
    CHECK_CODE = 'iob'
    DIFFS_COUNT = 10
    TEXT_DATA_NOT_IN_PERIOD = u"""Дата счета не попала в период сверки. По счету расходится дата. Будем разбирать в"""

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_no_diffs(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в обеих системах
            -количество открученного сходится
        Ожидаемый результат:
            заказ отсутствует в списке расхождений
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id':      invoice_map['id'],
                'eid':     invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_not_found_in_billing(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS
            -заказ отсутствует в Биллинге
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Отсутствует в биллинге"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id':      invoice_map['id'],
                'eid':     old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_not_found_in_oebs(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в Биллинге
            -заказ отсутствует в OEBS
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Отсутствует в OEBS"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice') # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает
            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id':      invoice_map['id'],
                'eid':     changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_sum_not_converge(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, сумма открученного на дату сверки = 100 ед.
            -заказ присутствует в биллинге, сумма открученного на дату сверки = 150 ед.
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Расходятся суммы по счету"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_1C_sum_not_converge(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, сумма по приходу на дату сверки = 120 ед.
            -заказ присутствует в биллинге, сумма по приходу на дату сверки = 150 ед.
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Расходятся суммы по приходу"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_person_not_converge(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, у счета плательщик №1
            -заказ присутствует в биллинге, у счета плательщик №2
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Расходятся конечные покупатели"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_invoice_dt_not_converge(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, у счета дата = 30 число предыдущего месяца
            -заказ присутствует в биллинге, у счета дата = 29 число предыдущего месяца
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Расходятся даты счета"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_check_2666_not_found_1(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, у счета дата = текущее число
            -заказ присутствует в биллинге, у счета дата = 29 число предыдущего месяца
            -запускаем сверку(по умолчанию запускается за предыдущий месяц)
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Отсутствует в OEBS"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()
            
            client_id, person_id, _ = create_client_and_person(self.CHECK_CODE)
            check_defaults = CHECK_DEFAULTS[self.CHECK_CODE]
            invoice_map = check_steps.create_invoice_map({1: {'paysys_id':     check_defaults.get('paysys_id'),
                                                              'service_id':    check_defaults.get('service_id'),
                                                              'product_id':    check_defaults.get('product_id'),
                                                              'shipment_info': {'Bucks': 30}}}, client_id, person_id,
                                                         on_dt=datetime.datetime.now())
            steps.ExportSteps.export_oebs(person_id=person_id)
            export(invoice_map['id'])

            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        expected_result = [(invoices['eid'], 1)]
        butils.check_that(set(result), equal_to(set(expected_result)))

        ticket = utils.get_check_ticket('iob', cmp_id)

        for comment in ticket.comments.get_all():
            if invoices['eid'] in comment.text:
                reporter.log(u">>>>>>> Invoice[EID]:{}".format(invoices['eid']))
                reporter.log(u">>>>>>> comment_text:{}".format(comment.text))
                butils.check_that(comment.text, contains_string(self.TEXT_DATA_NOT_IN_PERIOD),
                                   u'Проверяем, что в комментарии содержится требуемый текст, '
                                   u'В комментарии указан ID заказа')
                break
            else:
                assert False, u'Комментарий авторазбора не найден'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_check_2666_not_found_2(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, у счета дата = 30 число предыдущего месяца
            -заказ присутствует в биллинге, у счета дата = текущее число
            -запускаем сверку(по умолчанию запускается за предыдущий месяц)
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Отсутствует в Биллинге"
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            client_id, person_id, _ = create_client_and_person(self.CHECK_CODE)
            check_defaults = CHECK_DEFAULTS[self.CHECK_CODE]

            invoice_map = check_steps.create_invoice_map({1: {'paysys_id':     check_defaults.get('paysys_id'),
                                                              'service_id':    check_defaults.get('service_id'),
                                                              'product_id':    check_defaults.get('product_id'),
                                                              'shipment_info': {'Bucks': 30}}},
                                                         client_id, person_id)
            steps.ExportSteps.export_oebs(person_id=person_id)
            export(invoice_map['id'])

            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': datetime.datetime.now()})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        expected_result = [(invoices['eid'], 2)]
        butils.check_that(set(result), equal_to(set(expected_result)))

        ticket = utils.get_check_ticket('iob', cmp_id)

        for comment in ticket.comments.get_all():
            if invoices['eid'] in comment.text:
                reporter.log(u">>>>>>> Invoice[EID]:{}".format(invoices['eid']))
                reporter.log(u">>>>>>> comment_text:{}".format(comment.text))
                butils.check_that(comment.text, contains_string(self.TEXT_DATA_NOT_IN_PERIOD),
                                  u'Проверяем, что в комментарии содержится требуемый текст, '
                                  u'В комментарии указан ID заказа')
                break
            else:
                assert False, u'Комментарий авторазбора не найден'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_check_3059_not_check_for_billing(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в OEBS, у счета дата = 30 число предыдущего месяца
            -заказ присутствует в биллинге, у счета дата = текущее число
            -запускаем сверку(по умолчанию запускается за предыдущий месяц)
            -проставляем клиенту признак 'internal' = 1
        Ожидаемый результат:
            заказ попадает в список с расхождений,
            состояние = "Отсутствует в Биллинге"
            Авторазбор не происходит. Проверяем, что случай не попадает в авторазбор "Дата счета не попала в период сверки",
            т.к. 'internal' = 1. Хотя по остальным условиям подходит.
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            client_id, person_id, _ = create_client_and_person(self.CHECK_CODE)
            check_defaults = CHECK_DEFAULTS[self.CHECK_CODE]

            invoice_map = check_steps.create_invoice_map({1: {'paysys_id': check_defaults.get('paysys_id'),
                                                              'service_id': check_defaults.get('service_id'),
                                                              'product_id': check_defaults.get('product_id'),
                                                              'shipment_info': {'Bucks': 30}}},
                                                         client_id, person_id)
            steps.ExportSteps.export_oebs(person_id=person_id)
            export(invoice_map['id'])

            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': datetime.datetime.now()})

            balance().execute('update t_client set internal = 1 '
                              'where  id = (select  client_id from t_invoice where  id = :invoice_id)',
                              {'invoice_id': invoice_map['id'],})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        expected_result = [(invoices['eid'], 2)]
        butils.check_that(set(result), equal_to(set(expected_result)))

        ticket = utils.get_check_ticket('iob', cmp_id)
        text = u'По счету клиент был признан внутренним'

        reporter.log(u">>>>>>> COMMENTS[EID_3059]:{}".format(ticket.comments.get_all()))  #TODO удалить
        for comment in ticket.comments.get_all():
            reporter.log(u">>>>>>> Invoice[EID_3059_INT]:{}".format(invoices['eid']))  # TODO удалить
            reporter.log(u">>>>>>> comment_text:{}".format(comment.text))
            if invoices['eid'] in comment.text:
                # не считается расхождением, т.к. клиент внутренний и всё равно, что счёт не в периоде
                butils.check_that(comment.text, contains_string(text),
                                  u'Проверяем, что в комментарии содержится требуемый текст, '
                                  u'В комментарии указан ID заказа')
                break
        else:
            assert False, u'Комментарий авторазбора не найден'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_check_3060_internal_client(self, shared_data):
        """
        Начальные условия:
            -заказ присутствует в обеих системах
            -количество открученного сходится
            -у клиента выставляем признак internal=1
        Ожидаемый результат:
            заказ отсутствует в списке расхождений
        """
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

            balance().execute('update t_client set internal = 1 '
                              'where  id = (select  client_id from t_invoice where  id = :invoice_id)',
                              {'invoice_id': invoice_map['id'], })

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        expected_result = [(invoices['eid'], 2)]
        butils.check_that(set(result), equal_to(set(expected_result)))

        ticket = utils.get_check_ticket('iob', cmp_id)
        text = u'По счету клиент был признан внутренним'

        for comment in ticket.comments.get_all():
            if invoices['eid'] in comment.text:
                reporter.log(u">>>>>>> Invoice[EID_3060_INT]:{}".format(invoices['eid']))
                reporter.log(u">>>>>>> comment_text:{}".format(comment.text))
                butils.check_that(comment.text, contains_string(text),
                                  u'Проверяем, что в комментарии содержится требуемый текст, '
                                  u'В комментарии указан ID заказа')
                break
        else:
            assert False, u'Комментарий авторазбора не найден'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB)
    def test_iob_check_diffs_count(self, shared_data):
        """
        Проверка выполняется только вместе с другими и проверяет, что количество расхождений соответствует ожидаемому колчичеству.
        Проверяет, что нет лишних расхождений
        """
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT



FIRST_MONTH = butils.Date.first_day_of_month() - relativedelta(months=1)
#====================================================================================
# Шаблон для тестов
def iob_fabric(service):
    class iob_fabric(object):
        CHECK_CODE = service.check_name
        DIFFS_COUNT = 6
        SHARED_NAME = service.shared_name
        SERVICE_NAME = service.service_name

        @classmethod
        def create_invoice_for_orders_(cls):
            invoice_id, eid = check_steps.create_invoice_for_iob(cls.SERVICE_NAME, FIRST_MONTH)

            invoice_map = {
                'id': invoice_id,
                'eid': eid
            }

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

            export(invoice_map['id'])

            return invoices

        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_without_diff(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()
                invoices = self.create_invoice_for_orders_()

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            # assert invoices['eid'] not in [row['eid'] for row in cmp_data]
            # Проверяем, что нет расхождений
            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that(result, empty())

        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_not_found_in_billing(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()

                invoices = self.create_invoice_for_orders_()

                old_eid = check_steps.change_external_id(invoices['id'], object_='Invoice')

                invoices['eid'] = old_eid

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            # assert (invoices['eid'], 2) in [(row['eid'], row['state']) for row in cmp_data]

            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that((invoices['eid'], 2), is_in(result))


        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_not_found_in_oebs(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()
                invoices = self.create_invoice_for_orders_()

                # check_steps.change_external_id(invoices['id'], object_='act')
                check_steps.change_external_id(invoices['id'], object_='invoice')
                changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                                      {'inv_id': invoices['id']})[0]['external_id']

                invoices['eid'] = changed_eid

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that((invoices['eid'], 1), is_in(result))


        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_sum_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()
                invoices = self.create_invoice_for_orders_()

                balance().execute(
                                  'update (select  * from t_invoice where  id = :id) '
                                  'set total_sum = :changed_amount',
                                  {'id': invoices['id'], 'changed_amount': 113}
                                  )

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that((invoices['eid'], 3), is_in(result))


        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_1C_sum_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()
                invoices = self.create_invoice_for_orders_()

                balance().execute(
                                'update (select  * from t_invoice where  id = :id) '
                                'set RECEIPT_SUM_1C = :changed_amount',
                                {'id': invoices['id'], 'changed_amount': 13}
                                 )

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that((invoices['eid'], 4), is_in(result))


        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_date_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()
                invoices = self.create_invoice_for_orders_()

                balance().execute(
                                'update (select * from t_invoice '
                                'where id = :invoice_id) set dt = :dt',
                                {'invoice_id': invoices['id'],
                                'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)}
                                )

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that((invoices['eid'], 6), is_in(result))


        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_person_not_converge(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['invoices']) as before:
                before.validate()
                invoices = self.create_invoice_for_orders_()

                result = get_person_type_and_cliend_id_by_invoice(invoices['id'])
                person_type, client_id = result[0]['type'], result[0]['id']

                balance().execute(
                                    'update (select  * from t_invoice '
                                    'where  id = :invoice_id) set person_id = :person_id',
                                    {'invoice_id': invoices['id'],
                                     'person_id': steps.PersonSteps.create(client_id, person_type)}
                                )

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            result = [(row['eid'], row['state'])
                      for row in cmp_data if row['eid'] == invoices['eid']]
            reporter.log(result)

            butils.check_that((invoices['eid'], 5), is_in(result))


        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_check_diffs_count(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['cache_var']) as before:
                before.validate()
                cache_var = 'test'

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            assert len(cmp_data) == self.DIFFS_COUNT


        @pytest.mark.xfail(reason='Will be fixed later. Unknown reason yet')
        @pytest.mark.shared(block=SHARED_NAME)
        def test_iob_use_yt(self, shared_data):
            with CheckSharedBefore(shared_data=shared_data,
                                   cache_vars=['cache_var']) as before:
                before.validate()
                cache_var = 'test'

            cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)

            cmp_data = cmp_data or shared_data.cache.get('cmp_data')

            # будет падать локально - но вроде этот тест никому не нужен локально

            cmp_id = shared_data.cache.get('cmp_id_yt')

            diff_acts_yt = check_db.get_cmp_diff(
                cmp_id, cmp_name=self.CHECK_CODE if self.CHECK_CODE in ['iob_auto', 'iob_sw', 'iob_tr', 'iob_ua',
                                                                        'iob_us'] else 'iob')
            butils.check_that(diff_acts_yt, equal_to(cmp_data))

    return iob_fabric


########################################################################################################
# Указывем реквизиты для тестов для каждого сервиса

from collections import namedtuple

services = namedtuple('service', 'shared_name check_name service_name')

KINOPOISK =    services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_KINOPOISK,
                        check_name   = 'iob_kinopoisk',
                        service_name = 'kinopoisk')
HEALTH =       services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_HEALTH,
                        check_name   = 'iob_health',
                        service_name = 'health')
UBER_ML_BV =   services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_UBER_ML_BV,
                        check_name   = 'iob_uber_ml_bv',
                        service_name = 'uber_ml_bv')
ISRAEL_GO =    services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_ISRAEL_GO,
                        check_name   = 'iob_israel_go',
                        service_name = 'israel_go')
TAXI_AM =      services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_TAXI_AM,
                        check_name   = 'iob_taxi_am',
                        service_name = 'taxi_am')
TAXI_BV =      services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_TAXI_BV,
                        check_name   = 'iob_taxi_bv',
                        service_name = 'taxi_bv')
TAXI_KZ =      services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_TAXI_KZ,
                        check_name   = 'iob_taxi_kz',
                        service_name = 'taxi_kz')
DRIVE =        services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_DRIVE,
                        check_name   = 'iob_drive',
                        service_name = 'drive')
TAXI =         services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_TAXI,
                        check_name   = 'iob_taxi',
                        service_name = 'taxi')
HK_ECOMMERCE = services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_HK_ECOMMERCE,
                        check_name   = 'iob_hk_ecommerce',
                        service_name = 'hk_ecommerce')
YA_CLOUD =     services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_YA_CLOUD,
                        check_name   = 'iob_ya_cloud',
                        service_name =  'ya_cloud')
GAS =          services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_GAS,
                        check_name   = 'iob_gas',
                        service_name =  'gas')
UBER_AZ =      services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_UBER_AZ,
                        check_name   = 'iob_uber_az',
                        service_name = 'uber_az')
UBER_KZ =      services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_UBER_KZ,
                        check_name   = 'iob_uber_kz',
                        service_name = 'uber_kz')
MLU_EUROPE_BV =services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_MLU_EUROPE_BV,
                        check_name   = 'iob_mlu_europe_bv',
                        service_name = 'mlu_europe_bv')

MLU_AFRICA_BV =services(shared_name  = shared_steps.SharedBlocks.RUN_IOB_MLU_AFRICA_BV,
                        check_name   = 'iob_mlu_africa_bv',
                        service_name = 'mlu_africa_bv')

########################################################################################################
#Тут непосредственно вызываем течты для каждого сервиса

class TestIobKinopoisk(iob_fabric(KINOPOISK)):
    """
    Тесты для Kinopoisk полностью совпадают с описанными в iob_fabric
    """


class TestIobHealth(iob_fabric(HEALTH)):
    """
        Тесты для Health полностью совпадают с описанными в iob_fabric
    """


class TestIobUberMlBv(iob_fabric(UBER_ML_BV)):
    """
    Тесты для UberMlBv полностью совпадают с описанными в iob_fabric
    """


class TestIobIsraelGo(iob_fabric(ISRAEL_GO)):
    """
    Тесты для IsraelGo полностью совпадают с описанными в iob_fabric
    """


class TestIobTaxiAm(iob_fabric(TAXI_AM)):
    """
    Тесты для TaxiAm полностью совпадают с описанными в iob_fabric
    """


class TestIobTaxiBv(iob_fabric(TAXI_BV)):
    """
    Тесты для TaxiBv полностью совпадают с описанными в iob_fabric
    """


class TestIobTaxiKz(iob_fabric(TAXI_KZ)):
    """
    Тесты для TaxiKz полностью совпадают с описанными в iob_fabric
    """


class TestIobDrive(iob_fabric(DRIVE)):
    """
    Тесты для Drive полностью совпадают с описанными в iob_fabric
    """


class TestIobTaxi(iob_fabric(TAXI)):
    """
    Тесты для Taxi полностью совпадают с описанными в iob_fabric
    """


class TestIobHkEcommerce(iob_fabric(HK_ECOMMERCE)):
    """
    Тесты для HkEcommerce полностью совпадают с описанными в iob_fabric
    """


class TestIobYaCloud(iob_fabric(YA_CLOUD)):
    """
    Тесты для YaCloud полностью совпадают с описанными в iob_fabric
    """


class TestIobGas(iob_fabric(GAS)):
    """
    Тесты для Gas полностью совпадают с описанными в iob_fabric
    """


class TestIobUberAZ(iob_fabric(UBER_AZ)):
    """
    Тесты для UberAZ полностью совпадают с описанными в iob_fabric
    """


class TestIobUberKZ(iob_fabric(UBER_KZ)):
    """
    Тесты для UberKZ полностью совпадают с описанными в iob_fabric
    """


class TestIobMluEuropeBv(iob_fabric(MLU_EUROPE_BV)):
    """
    Тесты для MLU_EUROPE_BV полностью совпадают с описанными в iob_fabric
    """


class TestIobMluAfricaBv(iob_fabric(MLU_AFRICA_BV)):
    """
    Тесты для MLU_AFRICA_BV полностью совпадают с описанными в iob_fabric
    """

##############################################################

class TestIobServices(StepsIob):
    CHECK_CODE = 'iob_services'
    DIFFS_COUNT = 6

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_no_diffs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id': invoice_map['id'],
                'eid': old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')  # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает

            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id': invoice_map['id'],
                'eid': changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_1C_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_person_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_invoice_dt_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SERVICES)
    def test_iob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

class TestIobMarket(StepsIob):
    CHECK_CODE = 'iob_market'
    DIFFS_COUNT = 7

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_no_diffs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id': invoice_map['id'],
                'eid': old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'],
                                                     object_='Invoice')  # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает
            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id': invoice_map['id'],
                'eid': changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_1C_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_person_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_MARKET)
    def test_invoice_dt_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))


class TestIobTR(StepsIob):
    CHECK_CODE = 'iob_tr'
    DIFFS_COUNT = 6

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_no_diffs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id': invoice_map['id'],
                'eid': old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'],
                                                     object_='Invoice')  # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает
            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id': invoice_map['id'],
                'eid': changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_1C_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_person_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_invoice_dt_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))


    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_TR)
    def test_iob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

class TestIobSW(StepsIob):
    CHECK_CODE = 'iob_sw'
    DIFFS_COUNT = 6

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_no_diffs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id': invoice_map['id'],
                'eid': old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'],
                                                     object_='Invoice')  # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает
            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id': invoice_map['id'],
                'eid': changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_1C_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_person_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_invoice_dt_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))


    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_SW)
    def test_iob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT

class TestIobUS(StepsIob):
    CHECK_CODE = 'iob_us'
    DIFFS_COUNT = 6

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_no_diffs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id': invoice_map['id'],
                'eid': old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'],
                                                     object_='Invoice')  # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает
            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id': invoice_map['id'],
                'eid': changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_1C_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_person_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_invoice_dt_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))


    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_US)
    def test_iob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT


class TestIobVertical(StepsIob):
    CHECK_CODE = 'iob_vertical'
    DIFFS_COUNT = 6

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_no_diffs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        # Проверяем, что нет расхождений
        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            old_eid = check_steps.change_external_id(invoice_map['id'], object_='Invoice')
            invoices = {
                'id': invoice_map['id'],
                'eid': old_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_not_found_in_oebs(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            # steps.ExportSteps.export_oebs(invoice_id=invoice_map['id'])
            old_eid = check_steps.change_external_id(invoice_map['id'],
                                                     object_='Invoice')  # Тут мы возвращаем тот eid, который и был создам функцией выше. При этом в таблицe t_invoice заменяем eid. Строка ниже как раз его и получает
            changed_eid = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
                                            {'inv_id': invoice_map['id']})[0]['external_id']
            invoices = {
                'id': invoice_map['id'],
                'eid': changed_eid,
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set total_sum = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 113})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_1C_sum_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :id) set RECEIPT_SUM_1C = :changed_amount',
                              {'id': invoice_map['id'], 'changed_amount': 13})
            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_person_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            result = get_person_type_and_cliend_id_by_invoice(invoice_map['id'])
            person_type, client_id = result[0]['type'], result[0]['id']
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id',
                              {'invoice_id': invoice_map['id'],
                               'person_id': steps.PersonSteps.create(client_id, person_type)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_invoice_dt_not_converge(self, shared_data):
        with CheckSharedBefore(
                shared_data=shared_data, cache_vars=['invoices']
        ) as before:
            before.validate()

            invoice_map = self.create_invoice(self.CHECK_CODE)
            balance().execute('update (select  * from t_invoice where  id = :invoice_id) set dt = :dt',
                              {'invoice_id': invoice_map['id'],
                               'dt': check_steps.END_OF_MONTH - datetime.timedelta(days=1)})

            invoices = {
                'id': invoice_map['id'],
                'eid': invoice_map['eid'],
                'invoice': invoice_map
            }

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['eid'], row['state'])
                  for row in cmp_data if row['eid'] == invoices['eid']]
        reporter.log(result)

        butils.check_that((invoices['eid'], 6), is_in(result))


    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_IOB_VERTICAL)
    def test_iob_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_iob(shared_data, before, pytest.active_tests, self.CHECK_CODE)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT












"""
Для IOB_TAXI решено не переносить в Shared тесты:
- test_CHECK_2585_netting
- test_CHECK_2585_netting_with_diff
"""
