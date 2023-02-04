
from balance import mapper
from medium.medium_logic import Logic as MediumLogic
from tests import object_builder as ob
from balance.mapper.tarification_entity import ProductsMetadataCache
from entity_utils import create_completion

PLACE_TYPE_DISTRIBUTION = 8
PLACE_INTERNAL_TYPE_DISTRIBUTION = 100


def create_distribution_tag(session, client_id):
    last_id = session.execute(''' select nvl(max(id), 0) tag_id
                                  from t_distribution_tag
                              ''').fetchone()['tag_id']
    tag_id = last_id + 777
    MediumLogic().CreateOrUpdateDistributionTag(
        ob.create_passport(session).passport_id,
        {'TagID': tag_id, 'TagName': 'CreatedByScript', 'ClientID': client_id}
    )
    return tag_id


def create_contract(session, contract_params):

    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, is_partner=True).build(session).obj

    default_params = {
        'client_id': client.id,
        'ctype': 'DISTRIBUTION',
        'currency': 'RUR',
        'currency_calculation': 1,
        'distribution_contract_type': 3,
        'distribution_tag': create_distribution_tag(session, client.id),
        'download_domains': 'test',
        'firm_id': 1,
        'manager_bo_code': 20431,
        'manager_uid': '3692781',
        'nds': '18',
        'person_id': person.id,
        'product_searchf': 'test',
        'products_currency': 'RUR',
        'reward_type': 1,
        'signed': 1,
    }

    params = dict(default_params, **contract_params)

    params['service_start_dt'] = params.get('service_start_dt') or params['start_dt']

    supplements = []
    supplements += [1] if 'products_revshare' in params else []
    supplements += [2] if 'products_download' in params else []
    params['supplements'] = list(set(supplements))

    contract_id = MediumLogic().CreateCommonContract(ob.create_passport(session).passport_id, params)['ID']

    contract = session.query(mapper.Contract).get(contract_id)
    return contract


def add_place(contract, products=(10000,)):
    session = contract.session
    last_ids = session.execute(''' select
                                          nvl(max(id), 0) as place_id,
                                          nvl(max(search_id), 0) as search_id
                                   from t_place
                               ''').fetchone()
    place_id = last_ids['place_id'] + 777
    search_id = last_ids['search_id'] + 777
    MediumLogic().CreateOrUpdatePlace(
        ob.create_passport(session).passport_id,
        {
            'ID': place_id,
            'ClientID': contract.client.id,
            'Type': PLACE_TYPE_DISTRIBUTION,
            'URL': "pytest.com",
            'InternalType': PLACE_INTERNAL_TYPE_DISTRIBUTION,
            'ProductList': [{'id': pid} for pid in products],
            'SearchID': search_id,
            'TagID': contract.col0.distribution_tag,
        })
    return place_id


def create_primitive_completion(session, dt, product_id, place_id):

    mapping = ProductsMetadataCache().fields_mapping[product_id]

    if 'place_id' in mapping:
        key_tuple = (place_id, -1, -1, -1, -1, -1)
    else:
        place = session.query(mapper.Place).get(place_id)
        key_tuple = (place.search_id, -1, -1, -1, -1, -1)

    fact_tuple = (5000, 5000, 5000, 5000)

    create_completion(session, dt, product_id, key_tuple, fact_tuple)
