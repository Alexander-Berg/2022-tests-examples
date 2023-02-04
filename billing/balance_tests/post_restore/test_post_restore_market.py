# coding: utf-8
__author__ = 'blubimov'

import pytest
import datetime

from balance import balance_db as db
import balance.balance_api as api
from balance import balance_steps as steps
from btestlib.constants import ContractCommissionType, PersonTypes, Firms, Currencies, Services, ContractPaymentType, \
    ClientCategories, User, Users
from btestlib.data.defaults import Date
from post_restore_common import ContractTemplate, get_client_linked_with_login_or_create, \
    check_and_hide_existing_test_contracts_and_persons, BASE_CONTRACT_PARAMS, restore_person_if_not_exist

"""
Восстановление данных необходимых для интеграционных тестов Market -> Balance
Заказчики: lyubchich, alexkhait, zubikova

Тикеты: TESTBALANCE-684
"""


class ContractTemplates(object):
    PR_AGENCY_PREPAY = ContractTemplate(type=ContractCommissionType.PR_AGENCY,
                                        person_type=PersonTypes.UR,
                                        params=BASE_CONTRACT_PARAMS,
                                        add_params={
                                            'FIRM': Firms.MARKET_111,
                                            'CURRENCY': Currencies.RUB,
                                            'SERVICES': [Services.MARKET],
                                            'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                            'DT': Date.TODAY,
                                            'FINISH_DT': Date.YEAR_AFTER_TODAY,
                                            'IS_SIGNED': Date.TODAY,
                                        })


LOGIN_TO_CONTRACTS_MAP = {
    'uslugi-agency1': [ContractTemplates.PR_AGENCY_PREPAY],
    'uslugi-agency2': [ContractTemplates.PR_AGENCY_PREPAY],
    'test-mbi-api-3-agency': [ContractTemplates.PR_AGENCY_PREPAY],
    'testagency': [ContractTemplates.PR_AGENCY_PREPAY],
    'market-test-agency': [ContractTemplates.PR_AGENCY_PREPAY],
}

LOGIN_FOR_AGENCY = 'tst-do-not-add-in-pi-agency-4'
BUH_LOGIN_UID = '890766796' #testovyibuhgalter
BUH_LOGIN = 'testovyibuhgalter'

# восстаноление агентства для lyubchich
def test_restore_agency_for_pi():
    get_client_linked_with_login_or_create(LOGIN_FOR_AGENCY, ClientCategories.AGENCY)


# восстаноление бухлогина для lyubchich
def test_restore_buh_login_for_pi():
    steps.PassportSteps.get_passport_by_login(BUH_LOGIN)
    role_client_count = db.balance().execute("SELECT count(*) as rows_count FROM t_role_client_user "
                                             "WHERE passport_id = :passport_id",
                                             {'passport_id': BUH_LOGIN_UID})[0]['rows_count']
    if role_client_count == 0:
        client_id = steps.ClientSteps.create()
        db.balance().execute("Insert into t_role_client_user (ID,PASSPORT_ID,CLIENT_ID,ROLE_ID,CREATE_DT,UPDATE_DT) values (s_role_client_user_id.nextval,:passport_id,:client_id,100,:dt,:dt)",
                             {
                                'passport_id': BUH_LOGIN_UID,
                                'client_id': client_id,
                                'dt': datetime.datetime.now()
                             }
                             )

# восстаноление плательщиков для lyubchich и titinina-a
def test_restore_persons_for_pi():

    # для данного логина нужны плательщики физик и юрик
    user1 = User(uid=404744950, login='autotests-market-partner-web1')
    client_id1 = get_client_linked_with_login_or_create(user1.login, client_category=ClientCategories.CLIENT)
    persons1 = api.medium().GetClientPersons(client_id1)
    need_creation_ur = True
    need_creation_ph = True
    for person in persons1:
        if person['TYPE'] == PersonTypes.UR.code:
            need_creation_ur = False
        if person['TYPE'] == PersonTypes.PH.code:
            need_creation_ph = False
    if need_creation_ur:
        steps.PersonSteps.create(client_id1, PersonTypes.UR.code)
    if need_creation_ph:
        steps.PersonSteps.create(client_id1, PersonTypes.PH.code)

    # для данного логина нужен плательщик физик и овердрафт
    user2 = User(uid=1144870299, login='paymarketwithoverdraft')
    client_id2 = get_client_linked_with_login_or_create(user2.login, client_category=ClientCategories.CLIENT)
    persons2 = api.medium().GetClientPersons(client_id2)
    need_creation_ph = True
    for person in persons2:
        if person['TYPE'] == PersonTypes.PH.code:
            need_creation_ph = False
    if need_creation_ph:
        steps.PersonSteps.create(client_id2, PersonTypes.PH.code)

    limit_info = steps.OverdraftSteps.get_limit(client_id2, Services.MARKET.id)
    if not limit_info or limit_info[0]['overdraft_limit'] <= 0:
        steps.OverdraftSteps.set_force_overdraft(client_id2, Services.MARKET.id, 1000000, Firms.MARKET_111.id)
