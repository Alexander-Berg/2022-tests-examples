import json
import logging
import unittest

from .common import (
    PIPEDRIVE_GATE_URLS,
    pipedrive_gate_post,
    pipedrive_get,
)


_FORM_FIELDS = [
    {
        'name': 'Название компании',
        'manager_name': 'ФИО',
        'manager_phone': 'Телефон',
        'manager_email': 'Электронная почта',
        'manager_position': 'Должность',
        'vehicle_park_size': 'Количество курьеров',
        'apikey': 'Ключ API',
        'utm_source': 'utm_source',
        'utm_medium': 'utm_medium',
        'utm_campaign': 'utm_campaign',
        'utm_content': 'utm_content',
        'utm_term': 'utm_term',
        'company_id': 'company_id',
        'yandex_id': 'Яндекс ID',
        'google_id': 'Google ID',
        'facebook_id': 'Facebook ID',
        'problem_description': 'Описание проблемы'
    },
    {
        'name': 'Şirket adı',
        'manager_name': 'Ad-Soyad',
        'manager_phone': 'Telefon numarası',
        'manager_email': 'E-posta adresi',
        'manager_position': 'Görev',
        'vehicle_park_size': 'Şirketinize ait araç veya kurye sayısı',
        'apikey': 'Ключ API',
        'utm_source': 'utm_source',
        'utm_medium': 'utm_medium',
        'utm_campaign': 'utm_campaign',
        'utm_content': 'utm_content',
        'utm_term': 'utm_term',
        'company_id': 'company_id',
        'yandex_id': 'Yandex ID',
        'google_id': 'Google ID',
        'facebook_id': 'Facebook ID',
        'problem_description': 'Описание проблемы'
    }
]


def _build_product_name(key_value_dict):
    return f'[Routing] / Test-drive ({key_value_dict["vehicle_park_size"]})'


def _build_title(key_value_dict):
    return f'{key_value_dict["name"]} {_build_product_name(key_value_dict)}'


def _send_form(gate_url, form_fields, key_value_dict):
    assert form_fields.keys() == key_value_dict.keys()
    headers = {
        'X-B2BGEO-PRODUCT': _build_product_name(key_value_dict).encode('unicode_escape'),
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    payload = {
        f'field_{i}': json.dumps({
            'question': {
                'label': {
                    'ru': form_fields[key]
                }
            },
            'value': key_value_dict[key]
        }) for i, key in enumerate(form_fields)
    }
    return pipedrive_gate_post(f'{gate_url}/form', headers, payload)


class FormTest(unittest.TestCase):
    """
    Test that sending a form via pipedrive gate works and that
    the form data sent are available via pipedrive API.
    """
    def test_form(self):
        logging.info('Started test_form()')

        form_data = {
            'name': 'YTEST-567',
            'manager_name': 'Емельян Тестеров',
            'manager_phone': '+70000000000',
            'manager_email': 'testcourier@yandex.ru',
            'manager_position': 'Level 8 Project Manager',
            'vehicle_park_size': '33 - 55 машин',
            'apikey': 'dummy-fake-apikey-for-test',
            'utm_source': 'utm_source_value',
            'utm_medium': 'utm_medium_value',
            'utm_campaign': 'utm_campaign_value',
            'utm_content': 'utm_content_value',
            'utm_term': 'utm_term_value',
            'company_id': '7891',
            'yandex_id': '123',
            'google_id': '456',
            'facebook_id': '789',
            'problem_description': 'Какое-то описание проблемы',
        }

        for form_fields in _FORM_FIELDS:
            logging.info(f'Using labels: {list(form_fields.values())}')

            gate_url = PIPEDRIVE_GATE_URLS[0]

            logging.info(f'Sending form {form_data} to {gate_url}')
            j = _send_form(gate_url, form_fields, form_data).json()
            logging.debug(f'Form: {json.dumps(j,indent=4)}')

            deal_id = j['deal_id']
            person_id = j['person_id']
            note_id = j['note_id']

            logging.info(f'Checking deal {deal_id}')
            j = pipedrive_get(f'/deals/{deal_id}').json()
            logging.debug(f'Deal: {json.dumps(j,indent=4)}')
            d = j['data']
            self.assertEqual(deal_id, d['id'])
            self.assertEqual(form_data['manager_name'], d['person_name'])
            self.assertEqual(form_data['manager_name'], d['person_id']['name'])
            self.assertEqual(form_data['manager_phone'], d['person_id']['phone'][0]['value'])
            self.assertEqual(form_data['manager_email'], d['person_id']['email'][0]['value'])
            self.assertEqual(_build_title(form_data), d['title'])

            logging.info(f'Checking person {person_id}')
            j = pipedrive_get(f'/persons/{person_id}').json()
            logging.debug(f'Person: {json.dumps(j,indent=4)}')
            d = j['data']
            self.assertEqual(person_id, d['id'])
            self.assertEqual(form_data['manager_name'], d['name'])
            self.assertEqual(form_data['manager_phone'], d['phone'][0]['value'])
            self.assertEqual(form_data['manager_email'], d['email'][0]['value'])

            logging.info(f'Checking note {note_id}')
            j = pipedrive_get(f'/notes/{note_id}').json()
            logging.debug(f'Note: {json.dumps(j,indent=4)}')
            d = j['data']
            self.assertEqual(note_id, d['id'])
            self.assertEqual(deal_id, d['deal_id'])
            self.assertEqual(person_id, d['person_id'])
            self.assertEqual(form_data['manager_name'], d['person']['name'])
            self.assertEqual(_build_title(form_data), d['deal']['title'])
            self.assertTrue(form_data['problem_description'] in d['content'])

        logging.info('Finished test_form()')
