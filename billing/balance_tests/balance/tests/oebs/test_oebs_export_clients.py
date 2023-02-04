# -*- coding: utf-8 -*-

import json
import os

import pytest
from enum import Enum
import hamcrest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import Firms, Currencies
from btestlib.matchers import equal_to_casted_dict
from export_commons import Locators, read_attr_values
from btestlib import config as balance_config

pytestmark = [reporter.feature(Features.OEBS, Features.CLIENT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

try:
    import balance_contracts
    from balance_contracts.oebs.client import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals

    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/client/'


class ClientAttrs(Enum):
    name = \
        Locators(balance=lambda b: b['t_client.name'],
                 oebs=lambda o: o['hz_parties.party_name'])

    fullname = \
        Locators(balance=lambda b: b['t_client.fullname'],
                 oebs=lambda o: o['hz_parties.attribute3'])

    type_client = \
        Locators(balance=lambda b: 'CLIENT',
                 oebs=lambda o: o['hz_parties.attribute1'])

    type_agency = \
        Locators(balance=lambda b: 'AGENCY',
                 oebs=lambda o: o['hz_parties.attribute1'])

    email = \
        Locators(balance=lambda b: b['t_client.email'],
                 oebs=lambda o: o['hz_contact_points.e.email_address'])

    email_contact_point_type = \
        Locators(balance=lambda b: 'EMAIL',
                 oebs=lambda o: o['hz_contact_points.e.contact_point_type'])

    phone = \
        Locators(balance=lambda b: b['t_client.phone'],
                 oebs=lambda o: o['hz_contact_points.p.raw_phone_number'])

    phone_contact_point_type = \
        Locators(balance=lambda b: 'PHONE',
                 oebs=lambda o: o['hz_contact_points.p.contact_point_type'])

    phone_line_type = \
        Locators(balance=lambda b: 'GEN',
                 oebs=lambda o: o['hz_contact_points.p.phone_line_type'])

    fax = \
        Locators(balance=lambda b: b['t_client.fax'],
                 oebs=lambda o: o['hz_contact_points.f.raw_phone_number'])

    fax_contact_point_type = \
        Locators(balance=lambda b: 'PHONE',
                 oebs=lambda o: o['hz_contact_points.f.contact_point_type'])

    fax_line_type = \
        Locators(balance=lambda b: 'FAX',
                 oebs=lambda o: o['hz_contact_points.f.phone_line_type'])


class ClientAttrsByType(object):
    _COMMON = {
        ClientAttrs.name,
        ClientAttrs.fullname,
        ClientAttrs.email,
        ClientAttrs.email_contact_point_type,
        ClientAttrs.phone,
        ClientAttrs.phone_contact_point_type,
        ClientAttrs.phone_line_type,
        ClientAttrs.fax,
        ClientAttrs.fax_contact_point_type,
        ClientAttrs.fax_line_type,
    }

    CLIENT = set.union(_COMMON, {ClientAttrs.type_client})
    AGENCY = set.union(_COMMON, {ClientAttrs.type_agency})


def check_json_contract(client_id, json_file):
    steps.ExportSteps.init_oebs_api_export('Client', client_id)
    actual_json_data = steps.ExportSteps.get_json_data('Client', client_id)

    steps.ExportSteps.log_json_contract_actions(json_contracts_repo_path,
                                                JSON_OEBS_PATH,
                                                json_file,
                                                balance_config.FIX_CURRENT_JSON_CONTRACT)

    contract_utils.process_json_contract(json_contracts_repo_path,
                                         JSON_OEBS_PATH,
                                         json_file,
                                         actual_json_data,
                                         replace_mask,
                                         balance_config.FIX_CURRENT_JSON_CONTRACT)


@pytest.mark.parametrize('context, attrs, json_file', [
    (utils.aDict(name='client', client_params={'IS_AGENCY': 0}),
     ClientAttrsByType.CLIENT, 'client.json'),
    (utils.aDict(name='subclient_nonres', client_params={'NON_RESIDENT_CURRENCY': Currencies.USD.char_code}),
     ClientAttrsByType.CLIENT, 'nonres_subclient.json'),
    (utils.aDict(name='agency', client_params={'IS_AGENCY': 1}),
     ClientAttrsByType.AGENCY, 'agency.json'),
], ids=lambda context, attrs, json_file: context.name)
def test_export_client(context, attrs, json_file):
    client_id = steps.ClientSteps.create(context.client_params, prevent_oebs_export=True)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(client_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id)
        check_attrs(client_id, attrs)

# ---------------------------------------------- Contracts ----------------------------------------------

# def get_json_data(client_id):
#     query = "select * from t_oebs_api_export_log where method = 'billingImport' and classname = 'Client' and object_id = :client_id"
#     result = db.balance().execute(query, {'client_id': client_id})
#
#     data = result[0]['data']
#     json_data = json.loads(json.loads(data))
#     return json_data

# ---------------------------------------------- Utils ----------------------------------------------

def get_balance_client_data(client_id):
    balance_client_data = {}

    # t_client
    query = "SELECT * FROM t_client WHERE id = :client_id"
    result = db.balance().execute(query, {'client_id': client_id}, single_row=True)
    balance_client_data.update(utils.add_key_prefix(result, 't_client.'))

    return balance_client_data


def get_oebs_client_data(balance_client_data):
    balance_client_id = balance_client_data['t_client.id']
    firm_id = Firms.YANDEX_1.id

    oebs_client_data = {}

    # hz_parties
    object_id = 'C{}'.format(balance_client_id)
    query = "SELECT * FROM apps.hz_parties WHERE orig_system_reference  = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
    oebs_client_data.update(utils.add_key_prefix(result, 'hz_parties.'))

    # hz_contact_points
    object_id_template_options = {'e': 'C{}_E',
                                  'p': 'C{}_P',
                                  'f': 'C{}_F'}
    for option, object_id_template in object_id_template_options.iteritems():
        query = "SELECT * FROM apps.hz_contact_points WHERE orig_system_reference  = :object_id"
        # blubimov execute_oebs сейчас падает с ошибкой not well formed
        # (скорее всего связано с нечитаемым символом в ответе)
        # Коля это поправил в ExecuteSQL, после переноса изменений в ExecuteOEBS можно здесь использовать execute_oebs
        # Пока используем execute т.к. для клиентов фирма не важна
        # result = db.oebs().execute_oebs(firm_id, query,
        #                                 {'object_id': object_id_template.format(balance_client_id)}, single_row=True)
        result = db.oebs().execute(query,
                                   {'object_id': object_id_template.format(balance_client_id)}, single_row=True)
        oebs_client_data.update(utils.add_key_prefix(result, 'hz_contact_points.{}.'.format(option)))

    return oebs_client_data


def check_attrs(client_id, attrs):
    with reporter.step(u'Считываем данные из баланса'):
        balance_client_data = get_balance_client_data(client_id)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_client_data = get_oebs_client_data(balance_client_data)

    balance_values, oebs_values = read_attr_values(attrs, balance_client_data, oebs_client_data)

    utils.check_that(oebs_values, equal_to_casted_dict(balance_values),
                     step=u'Проверяем корректность данных клиента в ОЕБС')
