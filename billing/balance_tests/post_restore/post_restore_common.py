# coding: utf-8
__author__ = 'blubimov'

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import environments as env
from btestlib import reporter
from btestlib import utils
from btestlib.constants import ClientCategories

POST_RESTORE_CONTRACT_MEMO = 'Contract was created in post restore tests'

BASE_CONTRACT_PARAMS = {
    'MEMO': POST_RESTORE_CONTRACT_MEMO,
}


# --- Utils ---

class ContractTemplate(object):
    def __init__(self, type, person_type, params, add_params=None, collateral_template=None):
        self.type = type
        self.person_type = person_type
        self.params = params
        self.add_params = add_params
        self.collateral_template = collateral_template

    def create(self, client_id, person_id=None):
        if person_id is None:
            person_id = steps.PersonSteps.create(client_id, self.person_type.code)

        contract_id, _ = steps.ContractSteps.create_contract_3(self.type, client_id, person_id, self.params,
                                                               self.add_params)

        if self.collateral_template is not None:
            self.collateral_template.create(contract_id)

        return contract_id


class CollateralTemplate(object):
    def __init__(self, type, params, add_params=None):
        self.type = type
        self.params = params
        self.add_params = add_params

    def create(self, contract_id):
        steps.ContractSteps.create_collateral_3(self.type, contract_id, self.params, self.add_params)


def get_client_linked_with_login_or_create(login, client_category=None):
    # type: (str, ClientCategories) -> int
    resp = steps.PassportSteps.get_passport_by_login(login)
    client_id = resp.get('ClientId')

    if client_id is None:
        if client_category is None:
            raise utils.ServiceError(
                u'Логин {} не связан ни с одним клиентом и не задана категория для создания клиента'.format(login))
        else:
            with reporter.step(u'Логин {} не связан ни с одним с клиентом - создаем и связываем сами'.format(login)):
                client_id = steps.ClientSteps.create() if client_category == ClientCategories.CLIENT else \
                    steps.ClientSteps.create_agency()
                steps.ClientSteps.link(client_id, login)

    attach_client_url(client_id)
    return client_id


def get_client_persons_with_type(client_id, person_type):
    person_id_list = [d['id'] for d in db.balance().execute(
        "SELECT id FROM t_person WHERE CLIENT_ID = :client_id AND type = :person_type AND hidden = 0",
        {'client_id': client_id, 'person_type': person_type.code})]
    return person_id_list


def check_and_hide_existing_test_contracts_and_persons(client_id):
    with reporter.step(u'Проверяем наличие старых тестовых договоров'):
        existing_client_contracts = get_client_test_contract_id_list(client_id)

        if existing_client_contracts:
            with reporter.step(u'У клиента {} найдены старые тестовые договоры - сделаем их не активными, '
                               u'а их плательщиков скрытыми'.format(client_id)):
                make_contracts_unsigned(existing_client_contracts)
                make_contracts_person_hidden(existing_client_contracts)


# все ТЕСТОВЫЕ договоры клиента, в том числе и не действующие
def get_client_test_contract_id_list(client_id):
    contracts_info = api.medium().GetClientContracts({'ClientID': client_id, 'Signed': 0})
    contract_id_list = [d['ID'] for d in contracts_info]
    contract_id_list = filter_test_contracts(contract_id_list)
    return contract_id_list


def filter_test_contracts(contract_id_list):
    if contract_id_list:
        with reporter.step(u'Отбираем договоры созданные ранее в post_restore тестах'):
            query = """SELECT CONTRACT2_ID from t_contract_collateral
                       where ATTRIBUTE_BATCH_ID in
                         (SELECT ATTRIBUTE_BATCH_ID FROM t_contract_attributes
                          where ATTRIBUTE_BATCH_ID in
                            (SELECT ATTRIBUTE_BATCH_ID FROM t_contract_collateral WHERE CONTRACT2_ID in ({contract_id_str}) and NUM is null)
                          and CODE = 'MEMO'
                          and VALUE_CLOB like '%{memo}%')""".format(
                contract_id_str=list_to_str(contract_id_list), memo=POST_RESTORE_CONTRACT_MEMO)
            resp = db.balance().execute(query)
            return [d['contract2_id'] for d in resp]
    else:
        return contract_id_list


def make_contracts_unsigned(contract_id_list):
    db.balance().execute(
        """UPDATE T_CONTRACT_COLLATERAL
           SET IS_SIGNED = NULL, IS_FAXED = NULL, IS_CANCELLED = sysdate - 365
           WHERE CONTRACT2_ID in ({contract_id_str})""".format(contract_id_str=list_to_str(contract_id_list)))
    steps.ContractSteps.refresh_contracts_cache(*contract_id_list)


def make_contracts_person_hidden(contract_id_list):
    db.balance().execute(
        """update t_person
           set HIDDEN = 1
           where id in (select person_id from T_CONTRACT2
                        where id in ({contract_id_str}))""".format(contract_id_str=list_to_str(contract_id_list)))


def list_to_str(lst):
    return ','.join(map(str, lst))


def attach_client_url(client_id):
    client_page_url = '{base_url}/passports.xml?tcl_id={client_id}'.format(
        base_url=env.balance_env().balance_ai, client_id=client_id)

    reporter.report_url(u'Ссылка на клиента', client_page_url)


def restore_person_if_not_exist(client_id, person_type, params=None):
    client_persons_with_type = get_client_persons_with_type(client_id, person_type)

    if len(client_persons_with_type) == 0:
        steps.PersonSteps.create(client_id, person_type.code, params=params)
    else:
        reporter.attach(
            u"Не создаем плательщика, т.к. у клиента уже есть плательщики данного типа ({})".format(
                person_type.code),
            client_persons_with_type)
