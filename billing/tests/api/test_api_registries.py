from uuid import uuid4

import pytest
from bcl.banks.party_sber.registry_operator import SberbankCardRegistry, SberbankSalaryRegistry
from bcl.banks.party_tinkoff.registry_operator import TinkoffDismissalRegistry
from bcl.banks.registry import Sber, Tinkoff
from bcl.core.models import SalaryRegistry, states


@pytest.fixture
def basic_probe(api_client, get_salary_contract):

    def basic_probe_(*, registry_data, suburl, reg_cls, associate):

        # Запрошены данные по неизвестному договору.
        response = api_client.get(f"/api/registries/{suburl}/?contract=XXXX&guid={registry_data['guid']}")
        assert response.status_code == 404
        assert response.json['errors'] == [{'msg': 'salary contract XXXX not found'}]

        # Негатив. Указанный зарплатный договор не найден.
        response = api_client.post(f'/api/registries/{suburl}/', data=registry_data)
        assert response.status_code == 404
        assert response.json['errors'] == [{'msg': 'salary contract 38140753 not found'}]

        # Позитив.
        contract = get_salary_contract(associate, number='38140753')
        response = api_client.post(f'/api/registries/{suburl}/', data=registry_data)
        assert response.ok

        assert SalaryRegistry.objects.count() == 1
        registry = reg_cls.objects.get()
        registry.status = states.NEW
        registry.save()

        response = api_client.get(
            f"/api/registries/{suburl}/?contract={contract.number}&guid={registry_data['guid']}")
        assert response.ok
        response = response.json

        true_response = {
            'number': '002',
            'records': [{
                'record_id': '1', 'personal_account': 'accnum', 'response': 'testresult',
                "processing_notes": "testnote", 'remote_id': '1234',  'comment': ''
            }],
            'status': 0, 'guid': '8a3a0201-3fd3-4535-8096-168f6620cc65'
        }
        # для реестра на увольнение processing_notes должно быть пустым
        if suburl == 'dismissals':
            true_response['records'][0]['processing_notes'] = ''

        assert response['data'] == true_response

        # Негатив. Реестр уже сущестует.
        response_err = api_client.post(f'/api/registries/{suburl}/', data=registry_data)
        assert response_err.status_code == 400
        assert response_err.json['errors'] == [{'msg': 'registry 002 already exists'}]

        return response, registry

    return basic_probe_


@pytest.mark.parametrize('income_type', ['', '1'])
@pytest.mark.parametrize('transfer_code', ['', '01'])
def test_salary(basic_probe, income_type, transfer_code):

    registry_data = {
        'contract': '38140753',
        'date': '2018-03-22',
        'number': '002',
        'guid': '8a3a0201-3fd3-4535-8096-168f6620cc65',
        'records': [{
            'record_id': '1',
            'first_name': 'first',
            'last_name': 'last',
            'patronymic': 'patro',
            'personal_account': 'accnum',
            'currency_code': 'RUB',
            'amount': '10.20',
            'office_number': '10',
            'branch_office_number': '20',
            'target_bik': '102030',
            'card_number': '12345678901',

            # Сразу эмулируем получение результата обработки реестра банком.
            'result': 'testresult',
            'processing_notes': 'testnote',
            'remote_id': '1234',
        }],
    }
    if income_type:
        registry_data['income_type'] = income_type
        registry_data['records'][0]['amount_deduct'] = '10'
    if transfer_code:
        registry_data['transfer_code'] = transfer_code

    basic_probe(
        registry_data=registry_data,
        suburl='salary',
        reg_cls=SberbankSalaryRegistry,
        associate=Sber,
    )


def test_dismissal(basic_probe):

    registry_data = {
        'contract': '38140753',
        'date': '2018-03-22',
        'number': '002',
        'guid': '8a3a0201-3fd3-4535-8096-168f6620cc65',
        'records': [{
            'record_id': '1',
            'first_name': 'first',
            'last_name': 'last',
            'patronymic': 'patro',
            'personal_account': 'accnum',
            'dismissal_date': '2019-12-12',

            # Сразу эмулируем получение результата обработки реестра банком.
            'result': 'testresult',
            'remote_id': '1234',
        }],
    }

    basic_probe(
        registry_data=registry_data,
        suburl='dismissals',
        reg_cls=TinkoffDismissalRegistry,
        associate=Tinkoff,
    )


def test_cards(basic_probe, api_client):

    addr = {
        'apartment_number': '1513',
        'city': {'name': 'test', 'short_name': 'tm'},
        'country': {'code': 'RU', 'name': 'Russia', 'short_name': 'ru'},
        'district': {'name': 'test', 'short_name': 'tm'},
        'house_block': '1',
        'house_number': '13',
        'index': '125080',
        'region': {'name': 'test', 'short_name': 'tm'},
        'settlement': {'name': 'test', 'short_name': 'tm'},
        'street': {'name': 'test', 'short_name': 'tm'}
    }

    registry_data = {
        'contract': '38140753',
        'date': '2018-03-22',
        'number': '002',
        'guid': '8a3a0201-3fd3-4535-8096-168f6620cc65',
        'records': [
            {
                'address': addr,
                'address_of_residence': addr,
                'address_of_work': addr,
                'place_of_birthday': addr,
                'birthday_date': '2018-03-22 10:30:00',
                'citizenship': 'Russia',
                'embossed_text': {'field1': 'test', 'field2': 'test'},
                'first_name': 'test',
                'identify_card': {
                    'card_type': 'ZVExBnSOlJ',
                    'card_type_code': 712,
                    'issue_date': '2018-03-22',
                    'issued_by': 'test',
                    'number': 'xxx',
                    'series': 'xxx',
                    'subdivision_code': '666'
                },
                'last_name': 'test',
                'mobile_phone': '915123456711111',
                'patronymic': 'test',
                'position': 'test',
                'resident': False,
                'sex': 'M',
                'product': 'MC_CORPORATE',

                # Sberbank related:
                'record_id': '1',
                'office_number': '6666',
                'branch_office_number': '6666',
                'bonus_program': 'sd',
                'bonus_member_number': 30706,
                # Намеренно не включаем следующий узел (см. проверку ниже).
                # 'card_type': {
                #     'card_type_code': '1',
                #     'card_subtype_code': '2',
                # },

                # Сразу эмулируем получение результата обработки реестра банком.
                'result': 'testresult',
                'processing_notes': 'testnote',
                'personal_account': 'accnum',
                'remote_id': '1234',
             }
        ],
    }

    response, registry = basic_probe(
        registry_data=registry_data,
        suburl='cards',
        reg_cls=SberbankCardRegistry,
        associate=Sber,
    )

    # Проверим, что при сериализации в словарь попадут данные по умолчанию,
    # даже если узел с данными отсутствует в исходных данных.
    # В нашем случае не был передан узел card_type в корне документа.
    reg_contents = registry.outgoing_compile()[1].decode('cp1251')
    assert '<ВидВклада КодВидаВклада="51" КодПодвидаВклада="20" КодВалюты="810"/>' in reg_contents

    reg_id = str(uuid4())
    registry_data.update({'guid': reg_id})
    registry_data['records'][0].update({'card_type': {'card_subtype_code': '6', 'card_type_code': '31'}})

    response = api_client.post('/api/registries/cards/', data=registry_data)
    assert response.ok
    assert SalaryRegistry.objects.count() == 2

    reg_contents = SberbankCardRegistry.objects.get(registry_id=reg_id).outgoing_compile()[1].decode('cp1251')
    assert '<ВидВклада КодВидаВклада="31" КодПодвидаВклада="6" КодВалюты="810"/>' in reg_contents
