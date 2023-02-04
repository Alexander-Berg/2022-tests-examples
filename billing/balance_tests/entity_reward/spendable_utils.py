
from balance import mapper

from tests import object_builder as ob
from medium.medium_logic import Logic as MediumLogic
from entity_utils import create_completion


def create_contract(session, contract_params):

    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, is_partner=True).build(session).obj

    default_params = {
        'client_id': client.id,
        'currency': 'RUR',
        'firm_id': 1,
        'manager_bo_code': 20431,
        'manager_uid': '244916211',
        'person_id': person.id,
        'signed': 1,
        'services': [207],
        'nds': 0,
    }

    params = dict(default_params, **contract_params)

    contract_id = MediumLogic().CreateCommonContract(ob.create_passport(session).passport_id, params)['ID']

    contract = session.query(mapper.Contract).get(contract_id)
    return contract


def create_primitive_completion(session, dt, product_id, client_id):

    rub_currency_code = 643

    key_tuple = (client_id, rub_currency_code, -1, -1, -1, -1)
    fact_tuple = (5000, 5000, 5000, 5000)

    create_completion(session, dt, product_id, key_tuple, fact_tuple)
