import json
import os
from http import HTTPStatus
from typing import Optional

import pytest
import requests
import yatest.common

from conftest import PipedriveGate
from maps.b2bgeo.pipedrive_gate.lib.crm_infra.store import CrmType

PIPEDRIVE_GATE_PATH = "maps/b2bgeo/pipedrive_gate"
CRM_PARAMS = {
    crm_type: {'crm': crm_type.value}
    for crm_type in CrmType
}


def get_pipedrive_custom_fields(url, endpoint):
    params = {'api_token': 'xxx'}
    resp = requests.get(f"{url}{endpoint}", params=params)
    resp.raise_for_status()
    j = resp.json()
    assert j.get('success')
    return {x['name']: x['key'] for x in j['data']}


def find_obj(objects: list[dict], obj_id: int) -> Optional[dict]:
    for obj in objects:
        if obj.get('id') == obj_id or obj.get('ID') == obj_id:
            return obj
    return None


def get_pipedrive_obj(url, endpoint, obj_id):
    params = {'api_token': 'xxx'}
    resp = requests.get(f"{url}{endpoint}", params=params)
    resp.raise_for_status()
    j = resp.json()
    assert j.get('success')
    return find_obj(j['data'], obj_id)


def get_bitrix_obj(url: str, obj_type: str, obj_id: int) -> Optional[dict[str, str]]:
    resp = requests.get(f"{url}/crm.{obj_type}.list")
    resp.raise_for_status()
    j = resp.json()
    return find_obj(j['result'], obj_id)


def dummy_survey_with_phone(phone):
    return {
        'field_0': f'{{"question": {{"label": {{"ru": "Phone"}}}}, "value": "{phone}"}}',
        'field_1': '{"question": {"label": {"ru": "Company name"}}, "value": "dummy_company_name"}',        # 0 point
        'field_2': '{"question": {"label": {"ru": "utm_term"}}, "value": "something"}',                     # 0 point
        'field_3': '{"question": {"label": {"ru": "Name"}}, "value": "dummy_person_name"}',                 # 0 point
        'field_5': '{"question": {"label": {"ru": "utm_term"}}, "value": "yandex"}',                        # 2 point
    }


def dummy_dadata_with_status(status):
    return {
        'value': 'Dummy company name',
        'data': {
            "state": {
                "status": status,
            },
            "type": "INDIVIDUAL"  # score increment = 1 for this field
        }
    }


def _load_json_from_file(path_to_file):
    path_survey = yatest.common.source_path(os.path.join(PIPEDRIVE_GATE_PATH, path_to_file))
    with open(path_survey) as f:
        return json.load(f)


def test_pipedrive_without_dadata(pipedrive_gate: PipedriveGate):
    survey_data = _load_json_from_file('tests/data/form_request_01.json')

    headers = {'X-B2BGEO-PRODUCT': '[Routing]'}
    response = requests.post(f'{pipedrive_gate.gate}/form',
                             headers=headers,
                             data=survey_data,
                             params=CRM_PARAMS[CrmType.pipedrive_ww])
    response.raise_for_status()
    j = response.json()
    assert j.get('organization_id') is None
    assert 'deal_id' in j
    assert 'person_id' in j
    assert 'note_id' in j

    custom_fields = get_pipedrive_custom_fields(pipedrive_gate.pipedrive, '/dealFields')
    deal = get_pipedrive_obj(pipedrive_gate.pipedrive, '/deals', j['deal_id'])
    assert deal == {
        'id': j['deal_id'],
        'person_id': j['person_id'],
        'org_id': None,
        'title': 'ООО ДИАКАР [Routing]',
        custom_fields['company_id']: None,
        custom_fields['utm_source']: 'value_utm_source',
        custom_fields['utm_medium']: 'value_utm_medium',
        custom_fields['utm_content']: 'value_utm_content',
        custom_fields['utm_campaign']: 'value_utm_campaign',
        custom_fields['utm_term']: 'undefined',
        custom_fields['Key']: 'test-apikey',
    }


def test_bitrix_without_dadata(pipedrive_gate: PipedriveGate):
    survey_data = _load_json_from_file('tests/data/form_request_01.json')
    params = CRM_PARAMS[CrmType.bitrix]
    headers = {'X-B2BGEO-PRODUCT': '[Routing]'}
    response = requests.post(f'{pipedrive_gate.gate}/form', headers=headers, data=survey_data, params=params)

    response.raise_for_status()
    j = response.json()
    assert j.get('organization_id') is None
    assert set(j) == {'organization_id', 'deal_id', 'person_id', 'note_id'}

    assert get_bitrix_obj(pipedrive_gate.bitrix, 'contact', j['person_id']) == {
        'ID': j['person_id'],
        'NAME': 'Vasya Pupkin',
        'EMAIL][0][VALUE': 'pupkin@yandex.ru',
        'EMAIL][0][VALUE_TYPE': 'WORK',
        'PHONE][0][VALUE': '+7 964 521-35-06',
        'PHONE][0][VALUE_TYPE': 'WORK',
    }
    assert get_bitrix_obj(pipedrive_gate.bitrix, 'deal', j['deal_id']) == {
        'ID': j['deal_id'],
        'TITLE': 'ООО ДИАКАР [Routing]',
        'UTM_SOURCE': 'value_utm_source',
        'UTM_MEDIUM': 'value_utm_medium',
        'UTM_CONTENT': 'value_utm_content',
        'UTM_CAMPAIGN': 'value_utm_campaign',
        'UTM_TERM': 'undefined',
        'UF_GOOGLE_ID': '123',
        'UF_YANDEX_ID': '456',
        'UF_FACEBOOK_ID': '789',
        'UF_KEY': 'test-apikey',
        'UF_SCORE': '0',
        'CONTACT_ID': j['person_id'],
    }
    assert get_bitrix_obj(pipedrive_gate.bitrix, 'timeline.comment', j['note_id']) == {
        'ID': j['note_id'],
        'ENTITY_ID': j['deal_id'],
        'ENTITY_TYPE': 'deal',
        'COMMENT': '<b>Название компании</b>\nООО ДИАКАР\n'
        '<b>ФИО</b>\nVasya Pupkin\n'
        '<b>Должность</b>\nlogistics\n'
        '<b>Электронная почта</b>\npupkin@yandex.ru\n'
        '<b>Телефон</b>\n+7 964 521-35-06\n'
        '<b>Количество транспортных средств или курьеров</b>\n10-20\n'
        '<b>Отметьте, если вы хотели бы воспользоваться нашим автопарком для доставки заказов</b>\nДа\n'
        '<b>Я принимаю условия <a href="https://yandex.ru/legal/rules/index.html">Пользовательского соглашения'
        '</a> сервисов Яндекса</b>\nДа\n'
        '<b>apikey</b>\ntest-apikey\n'
        '<b>Score</b>\n0 - Код страны: RU',
    }


@pytest.mark.parametrize('crm', [CrmType.bitrix, CrmType.pipedrive_ww])
def test_form_4xx(pipedrive_gate: PipedriveGate, crm: CrmType):
    payloads = [
        {},
        {
            'field_0': '{"question": {"label": {"ru": "Name"}}, "value": "abc"}',
            'field_1': '{"question": {"label": {"ru": "Company name"}}, "value": ""}'
        },
        {
            'field_0': '{"question": {"label": {"ru": "Name"}}, "value": ""}',
            'field_1': '{"question": {"label": {"ru": "Company name"}}, "value": "123"}'
        }
    ]
    headers = {
        'X-B2BGEO-PRODUCT': '[Routing]',
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    for index, payload in enumerate(payloads):
        resp = requests.post(f'{pipedrive_gate.gate}/form',
                             headers=headers,
                             data=payload,
                             params=CRM_PARAMS[crm])
        assert resp.status_code == HTTPStatus.BAD_REQUEST


@pytest.mark.parametrize(
    'mode',
    ['normal', 'absence_data', 'bad_type_data'],
    ids=['normal', 'absence_data', 'bad_type_data']
)
def test_form_with_dadata(pipedrive_gate: PipedriveGate, mode: str):
    expected_pipedrive_values = {
        'address': '248007, Калужская обл, г Калуга, ул Вишневского, д 17, кв 55',
        'name': 'ООО "МОТОРИКА"',
        'people_count': 0,
        'income': 77000,
    }

    survey_data = _load_json_from_file('tests/data/form_request_01.json')
    dadata = _load_json_from_file('tests/data/dadata.json')

    if mode == 'absence_data':
        del dadata['data']['finance']
        del dadata['data']['name']
        del expected_pipedrive_values['income']
    elif mode == 'bad_type_data':
        dadata['data']['state']['registration_date'] = 'bad_type'
        dadata['data']['state']['okveds'] = 'bad_type'

    full_payload = dict(**survey_data, dadata=json.dumps(dadata))

    response = requests.post(f'{pipedrive_gate.gate}/form',
                             headers={'X-B2BGEO-PRODUCT': '[Routing]'},
                             data=full_payload,
                             params=CRM_PARAMS[CrmType.pipedrive_ww])
    response.raise_for_status()
    j = response.json()

    assert set(j) == {'organization_id', 'deal_id', 'person_id', 'note_id'}

    org_id = j['organization_id']
    org_fields = get_pipedrive_custom_fields(pipedrive_gate.pipedrive, '/organizationFields')
    org_from_pipedrive = get_pipedrive_obj(pipedrive_gate.pipedrive, '/organizations', org_id)

    for key_pipedrive, value in expected_pipedrive_values.items():
        assert org_from_pipedrive.get(org_fields[key_pipedrive]) == value


def test_bitrix_with_dadata(pipedrive_gate: PipedriveGate):
    survey_data = _load_json_from_file('tests/data/form_request_01.json')
    dadata = _load_json_from_file('tests/data/dadata.json')
    full_payload = dict(**survey_data, dadata=json.dumps(dadata))
    print('matrohin', json.dumps(full_payload))

    response = requests.post(f'{pipedrive_gate.gate}/form',
                             headers={'X-B2BGEO-PRODUCT': '[Routing]'},
                             data=full_payload,
                             params=CRM_PARAMS[CrmType.bitrix])
    response.raise_for_status()
    j = response.json()
    assert set(j) == {'organization_id', 'deal_id', 'person_id', 'note_id'}

    assert get_bitrix_obj(pipedrive_gate.bitrix, 'company', j['organization_id']) == {
        'ID': j['organization_id'],
        'TITLE': 'ООО "МОТОРИКА"',
        'ADDRESS': '248007, Калужская обл, г Калуга, ул Вишневского, д 17, кв 55',
        'EMPLOYEES': '0',
        'UF_RANCH_TYPE': 'MAIN',
        'BANKING_DETAILS': '4028051108',
        'UF_MANAGEMENT_NAME': 'Алексеев Денис Сергеевич',
        'UF_MANAGEMENT_POST': 'ДИРЕКТОР',
        'UF_OKVED': '45.3 Торговля автомобильными деталями, узлами и принадлежностями',
        'UF_OKVED_TYPE': '2014',
        'UF_OKVEDS': '45.3, 45.4',
        'UF_OPF': 'ООО',
        'UF_DATE_CREATE': '2012-04-20T00:00:00',
        'UF_STATUS': 'ACTIVE',
        'UF_TYPE': 'LEGAL',
        'UF_INCOME': '77000',
    }


@pytest.mark.parametrize('crm', [CrmType.bitrix])
def test_form_with_dadata_miss_requried(pipedrive_gate: PipedriveGate, crm: CrmType):
    survey_data = _load_json_from_file('tests/data/form_request_01.json')
    dadata = _load_json_from_file('tests/data/dadata.json')

    del dadata['value']
    full_payload = dict(**survey_data, dadata=json.dumps(dadata))

    response = requests.post(f'{pipedrive_gate.gate}/form',
                             headers={'X-B2BGEO-PRODUCT': '[Routing]'},
                             data=full_payload,
                             params=CRM_PARAMS[crm])
    response.raise_for_status()
    j = response.json()

    assert j.get('organization_id') is None
    assert set(j) == {'organization_id', 'deal_id', 'person_id', 'note_id'}


def test_score(pipedrive_gate: PipedriveGate):
    headers = {
        'X-B2BGEO-PRODUCT': '[Routing]',
        'Content-Type': 'application/x-www-form-urlencoded'
    }

    for score, survey, dadata in [
        (7, dummy_survey_with_phone('+375 (17) 309-25-02'), None),  # BY
        (7, dummy_survey_with_phone('+ 7 (7172) 574714'), None),  # KZ
        (7, dummy_survey_with_phone('+99871 244 4141'), None),  # UZ
        (0, dummy_survey_with_phone('+442083661177'), None),   # GB
        (0, dummy_survey_with_phone('+74593330099'), None),    # RU
        (0, dummy_survey_with_phone('89687776655'), None),     # RU
        (0, dummy_survey_with_phone('+9not a phone'), None),   # no phone
        (0, dummy_survey_with_phone("don't care"), dummy_dadata_with_status('LIQUIDATING')),
        (0, dummy_survey_with_phone("don't care"), dummy_dadata_with_status('LIQUIDATED')),
        (0, dummy_survey_with_phone("don't care"), dummy_dadata_with_status('BANKRUPT')),
        (3, dummy_survey_with_phone("don't care"), dummy_dadata_with_status('ACTIVE')),
        (3, dummy_survey_with_phone("don't care"), dummy_dadata_with_status('REORGANIZING')),
    ]:
        full_payload = dict(
            **survey,
            dadata=json.dumps(dadata) if dadata else None
        )

        resp = requests.post(f'{pipedrive_gate.gate}/form',
                             headers=headers,
                             data=full_payload,
                             params=CRM_PARAMS[CrmType.bitrix])
        deal_from_bitrix = get_bitrix_obj(pipedrive_gate.bitrix, 'deal', resp.json()['deal_id'])
        assert deal_from_bitrix.get('UF_SCORE') == str(score)
