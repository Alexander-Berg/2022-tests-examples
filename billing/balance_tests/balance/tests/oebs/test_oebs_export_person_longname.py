# coding=utf-8
from datetime import datetime

from hamcrest import equal_to

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import Firms, Services, Currencies, Regions, PersonTypes, NdsNew

SERVICE = Services.DIRECT
CURRENCY = Currencies.EUR
REGION = Regions.SW
PERSON_TYPE = PersonTypes.SW_YT
NAME = 'Эрик Рыжий'


def get_oebs_person_longname(person_id):
    with reporter.step(u'Получим значение поля long_name из OeBS для плательщика {0}'.format(person_id)):
        object_id = 'P{0}'.format(person_id)
        query = 'SELECT attribute4 from apps.hz_cust_accounts where orig_system_reference = :object_id'
        result = db.oebs().execute_oebs(Firms.EUROPE_AG_7.id, query, {'object_id': object_id}, single_row=True)
        reporter.attach(u'Значение long_name в OeBS: ', result['attribute4'])
        return result


# проверяем, что long_name верно передается, если записывать его явно
def test_explicit_long_name():
    client_id = steps.ClientSteps.create({'REGION_ID': REGION.id, 'CURRENCY': 'EUR', 'SERVICE_ID': SERVICE.id,
                                          'MIGRATE_TO_CURRENCY': datetime(2000, 1, 1), 'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE.code, params={'is_partner': '1', 'name': NAME,
                                                                              'longname': 'test long name'})
    actual = db.get_person_by_id(person_id)[0]['longname']
    utils.check_that(actual, equal_to('test long name'),
                     step='Сравним LongName из БД c тем, что туда передали')


# проверим, что в если явно не указывать long_name, то туда копируется Name
def test_no_long_name():
    client_id = steps.ClientSteps.create({'REGION_ID': REGION.id, 'CURRENCY': 'EUR', 'SERVICE_ID': SERVICE.id,
                                          'MIGRATE_TO_CURRENCY': datetime(2000, 1, 1), 'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE.code,
                                         params={'is_partner': '1', 'name': NAME})
    actual = db.get_person_by_id(person_id)[0]['longname']
    utils.check_that(actual, equal_to(NAME.decode('utf-8')),
                     step='Проверим, что если явно не указывать LongName, то туда передастся Name')
