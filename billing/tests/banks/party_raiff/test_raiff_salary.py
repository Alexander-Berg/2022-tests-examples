import json
import random
from datetime import datetime
from decimal import Decimal
from io import BytesIO

import pytest

from bcl.banks.party_raiff.registry_operator import (
    RaiffCardRegistry,
    RaiffDismissalRegistry,
    RaiffRegistryOperator,
    RaiffSalaryRegistry
)
from bcl.banks.registry import Ing, Raiffeisen, RaiffeisenSpb
from bcl.core.models import states
from bcl.toolbox.utils import get_subitem, CipherUtils


class TestRaiffCardRegistry:

    @pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
    def test_compile(self, associate, get_salary_contract, get_salary_registry):
        registry = get_salary_registry(
            associate, RaiffCardRegistry, employees=[
                {
                    'record_id': '',
                    'first_name': 'Иван',
                    'last_name': 'Иванов',
                    'patronymic': '',
                    'resident': True,
                    'embossed_text': {'field1': 'IVANOV', 'field2': 'IVAN'},
                    'sex': 'M',
                    'place_of_birthday': {'city': {'name': 'Москва'}},
                    'birthday_date': datetime(2018, 10, 24),
                    'identify_card': {
                        'number': '123', 'card_type_code': 21, 'issue_date': datetime(2018, 10, 23),
                        'issued_by': 'РОВД "123" России'
                    },
                    'address_of_residence': {
                        'region': {'name': 'раз два'}
                    },
                }
            ]
        )

        data = registry.outgoing_compile()[1]

        data = data.decode('cp1251')
        assert (
            f'{registry.registry_id};Резидент;Иванов;Иван;;24.10.2018;Москва;IVANOV;IVAN;МУЖСКОЙ;;;;;;Да;;;;;;;;;;;;;;'
            'раз два;;;;;;;;;21;;123;23.10.2018;РОВД "123" России;;;;;;;;;;;;;;7813'
        ) in data


class TestRaiffSalaryRegistry:

    @pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
    def test_compile(self, associate, get_salary_contract, get_salary_registry):
        registry = get_salary_registry(
            associate, RaiffSalaryRegistry, employees=[
                {
                    'record_id': '4',
                    'first_name': 'Иван',
                    'last_name': 'Антонов',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'amount': Decimal(300),
                    'personal_account': '12345678901234567890',
                    'target_bik': '5678',
                    'card_number': '6677788',
                },
                {
                    'record_id': '3',
                    'first_name': 'Иван',
                    'last_name': 'Иванов',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'amount': Decimal(300),
                    'personal_account': '12345678901234567890',
                    'target_bik': '5678',
                    'card_number': '6677788',
                },
                {
                    'record_id': '1',
                    'first_name': 'Иван',
                    'last_name': 'Петров',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'amount': Decimal(300),
                    'personal_account': '12345678901234567890',
                    'target_bik': '5678',
                    'card_number': '6677788',
                },
                {
                    'record_id': '2',
                    'first_name': 'Иван',
                    'last_name': 'Федоров',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'amount': Decimal(300),
                    'personal_account': '12345678901234567890',
                    'target_bik': '5678',
                    'card_number': '6677788',
                    'income_type': '1',
                    'amount_deduct': Decimal(11.7)
                }
            ]
        )
        random.shuffle(registry.employees)
        registry.save()

        data = registry.outgoing_compile()[1]

        data = data.decode('cp1251')
        assert data
        assert '5678' in data
        assert '6677788' in data
        assert 'Таб. №' in data

        lines = data.split('\n')
        assert 'Петров' in lines[1]
        assert 'Федоров' in lines[2]
        assert '11.70\r' == lines[2].split(';')[13]
        assert 'Иванов' in lines[3]
        assert 'Антонов' in lines[4]
        assert all('\r' == lines[i].split(';')[13] for i in (1, 3, 4))


class TestRaiffDismissRegistry(object):

    @pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
    def test_compile(self, associate, get_salary_contract, get_salary_registry):
        registry = get_salary_registry(
            associate, RaiffDismissalRegistry, employees=[
                {
                    'record_id': '',
                    'first_name': 'Иван',
                    'last_name': 'Иванов',
                    'patronymic': '',
                    'dismissal_date': '2019-12-16',
                    'personal_account': '12345678901234567890',
                }
            ]
        )

        data = registry.outgoing_compile()[1]

        data = data.decode('cp1251')
        assert data


@pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
def test_parse_card(associate, get_salary_contract, get_salary_registry, ui_has_message, read_fixture):

    reg_cards_more_emp = get_salary_registry(
        associate, RaiffCardRegistry,
        status=states.NEW,
        created_dt=datetime(2017, 9, 21),
        registry_number='105',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'identify_card': {'number': '222888', 'series': '6006'},
            },
            {
                'record_id': '2',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'identify_card': {'number': '222889', 'series': '6007'},
            }
        ],
    )

    reg_cards_other_date = get_salary_registry(
        associate, RaiffCardRegistry,
        status=states.NEW,
        created_dt=datetime(2017, 9, 20),
        registry_number='105',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'identify_card': {'number': '222988', 'series': '6006'},
            }
        ],
    )

    op = associate.registry_operator
    op.process_incoming_file(BytesIO(read_fixture('raiff_card.zip')))
    assert ui_has_message('Не удалось найти однозначное соответствие')

    reg_cards = get_salary_registry(
        associate, RaiffCardRegistry,
        reg_id='1e280956-91f0-4974-abfa-8aa0e6a7918e',
        status=states.NEW,
        created_dt=datetime(2017, 9, 21),
        registry_number='106',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'identify_card': {'number': '222888', 'series': '6006'},
            }
        ],
    )

    # Для повышения надёжности поиска по зарплатному договору, используя ИНН организации.
    org = reg_cards.contract.org
    org.inn = '7777123456'
    org.save()

    reg_other_contract = get_salary_registry(
        associate, RaiffCardRegistry,
        status=states.NEW,
        created_dt=datetime(2017, 9, 22),
        reg_id='2e280956-91f0-4974-abfa-8aa0e6a7918e',
        registry_number='105',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'identify_card': {'number': '222888', 'series': '6006'},
            }
        ],
        contract_account='9999999999'
    )

    op.process_incoming_file(BytesIO(read_fixture('raiff_card_incorrect_passport.zip')))
    assert ui_has_message('найти однозначное соответствие')
    assert ui_has_message('не найден')

    op.process_incoming_file(BytesIO(read_fixture('raiff_card_empty_acc.zip')))
    assert ui_has_message('найти однозначное соответствие')

    reg_cards.refresh_from_db()
    assert reg_cards.is_loaded
    assert get_subitem(reg_cards.employees_with_answer[0], 'personal_account', '') == ''

    reg_cards.refresh_from_db()
    reg_cards_more_emp.refresh_from_db()
    reg_cards_other_date.refresh_from_db()

    assert reg_cards.is_loaded
    assert not reg_cards_more_emp.is_loaded
    assert not reg_cards_other_date.is_loaded
    assert not reg_other_contract.is_loaded


@pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
def test_parse_salary(associate, get_salary_contract, get_salary_registry, ui_has_message, read_fixture):

    reg_salary_many_emp = get_salary_registry(
        associate, RaiffSalaryRegistry,
        contract_account='40702810500001999999',
        created_dt=datetime(2017, 9, 14),
        status=states.NEW,
        registry_number='21105',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810201001234567',
            },
            {
                'record_id': '66',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654321',
            },
            {
                'record_id': '3',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654323',
            }
        ]
    )

    reg_salary_other_date = get_salary_registry(
        associate, RaiffSalaryRegistry,
        contract_account='40702810500001999999',
        created_dt=datetime(2017, 9, 16),
        status=states.NEW,
        registry_number='21105',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810201001234567',
            },
            {
                'record_id': '66',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654321',
            },
            {
                'record_id': '66',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654322',
            },
        ]
    )

    op = associate.registry_operator
    op.process_incoming_file(BytesIO(read_fixture('raiff_salary.zip')))
    assert ui_has_message('Не удалось найти однозначное соответствие')

    reg_salary = get_salary_registry(
        associate, RaiffSalaryRegistry,
        contract_account='40702810500001999999',
        created_dt=datetime(2017, 9, 14),
        status=states.NEW,
        registry_number='21105',
        reg_id='9adb4c79-7c70-4aff-afd7-d147ee2031a3',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810201001234567',
            },
            {
                'record_id': '66',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654321',
            },
        ]
    )

    op.process_incoming_file(BytesIO(read_fixture('raiff_salary_incorrect_acc.zip')))
    assert ui_has_message('Не совпадают номера счетов')

    reg_salary = get_salary_registry(
        associate, RaiffSalaryRegistry,
        contract_account='40702810500001999999',
        created_dt=datetime(2017, 9, 14),
        status=states.NEW,
        registry_number='21105',
        reg_id='8adb4c79-7c70-4aff-afd7-d147ee2031a3',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810201001234567',
            },
            {
                'record_id': '66',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654321',
            },
            {
                'record_id': '6',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('500'),
                'personal_account': '40817810001007654322',
            },
        ]
    )

    op.process_incoming_file(BytesIO(read_fixture('raiff_salary.zip')))

    reg_salary.refresh_from_db()
    reg_salary_many_emp.refresh_from_db()
    reg_salary_other_date.refresh_from_db()

    assert reg_salary.is_loaded
    assert reg_salary.employees_responded == 2
    assert reg_salary.employees_rejected == 1

    assert not reg_salary_many_emp.is_loaded
    assert not reg_salary_other_date.is_loaded

    reg_salary = get_salary_registry(
        associate, RaiffSalaryRegistry,
        contract_account='40702810500001999999',
        reg_id='d193729e-15f3-457a-a3d2-c41a7a70a17a',
        created_dt=datetime(2019, 1, 25),
        status=states.NEW,
        registry_number='106',
        employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Антонов',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'amount': Decimal('10'),
                'personal_account': '40817810401001867036',
            },
        ]
    )

    op.process_incoming_file(BytesIO(read_fixture('raiif_salary_deleted.zip')))
    reg_salary.refresh_from_db()
    assert reg_salary.is_loaded


@pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
@pytest.mark.parametrize('transfer_code', ['', '01', '03'])
@pytest.mark.parametrize('income_type', ['', '2'])
@pytest.mark.parametrize('registry_type', [RaiffCardRegistry, RaiffSalaryRegistry,  RaiffDismissalRegistry])
def test_filename(transfer_code, income_type, associate, registry_type, get_salary_registry):
    registry = get_salary_registry(
        associate, registry_type,
        status=states.NEW,
        created_dt=datetime(2017, 9, 21),
        registry_number='105',
        transfer_code=transfer_code,
        income_type=income_type,
        employees=[
            {
                'record_id': '1',
                'resident': True,
                'sex': 'M',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'currency_code': 'RUB',
                'amount': 10,
                'dismissal_date': '2019-12-16',
                'identify_card': {'number': '222888', 'series': '6006'},
            }
        ],
    )
    transfer_code = transfer_code or ('06' if income_type == '2' else '01')
    assert registry.outgoing_compile()[0] == f'{registry.registry_number.zfill(6)}_INN{registry.contract.org.inn}_ACC{registry.contract.account}_PT{transfer_code}_KVD{income_type}.csv'


def test_filename_transit_account(get_salary_registry, get_salary_contract):
    contract = get_salary_contract(associate=Raiffeisen, transit_acc='6666666666')

    def create_registry(registry_type, target_bik=None):
        employees = []
        if registry_type is RaiffSalaryRegistry:
            employees = [
                {
                    'record_id': '1',
                    'resident': True,
                    'sex': 'M',
                    'first_name': 'Иван',
                    'last_name': 'Иванов',
                    'patronymic': '',
                    'currency_code': 'RUB',
                    'amount': 10,
                    'dismissal_date': '2019-12-16',
                    'identify_card': {'number': '222888', 'series': '6006'},
                }
            ]
            if target_bik:
                employees[0].update({'target_bik': target_bik})
        registry = get_salary_registry(
            Raiffeisen, registry_type,
            status=states.EXPORTED_H2H,
            created_dt=datetime(2017, 9, 21),
            contract=contract,
            employees=employees,
        )
        return registry

    registry = create_registry(RaiffCardRegistry)
    assert f'ACC{contract.account}' in registry.outgoing_compile()[0]

    registry = create_registry(RaiffSalaryRegistry)
    assert f'ACC{contract.account}' in registry.outgoing_compile()[0]

    registry = create_registry(RaiffSalaryRegistry, target_bik=Ing.bid)
    assert f'ACC{contract.transit_account}' in registry.outgoing_compile()[0]


@pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb])
def test_salary_registry_to_file(associate, get_salary_registry):
    registry1 = get_salary_registry(
        associate, RaiffSalaryRegistry,
        status=states.NEW,
        created_dt=datetime(2017, 9, 21),
        registry_number='105',
        transfer_code='',
        income_type='',
        employees=[
            {
                'record_id': '1',
                'resident': True,
                'sex': 'M',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'currency_code': 'RUB',
                'amount': 10,
                'dismissal_date': '2019-12-16',
                'identify_card': {'number': '222888', 'series': '6006'},
            }
        ],
    )
    filename, _data, content_type = RaiffRegistryOperator.registers_to_file([registry1.id], {})
    assert filename.endswith('.zip')
    assert content_type is None

    registry2 = get_salary_registry(
        associate, RaiffSalaryRegistry,
        status=states.NEW,
        created_dt=datetime(2017, 10, 21),
        registry_number='106',
        transfer_code='',
        income_type='',
        employees=[
            {
                'record_id': '1',
                'resident': True,
                'sex': 'M',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'currency_code': 'RUB',
                'amount': 10,
                'dismissal_date': '2019-12-16',
                'identify_card': {'number': '222888', 'series': '6006'},
            }
        ],
    )
    filename, _data, content_type = RaiffRegistryOperator \
        .registers_to_file([registry1.id, registry2.id], {})
    assert filename.endswith('.zip')
    assert content_type is None


def test_card_registry(response_mock, get_salary_registry):

    address_of_residence = {
        'country': {'code': '643'},
        'city': {'name': 'Москва'},
        'region': {'name': 'Москва'},
        'street': {'name': 'Октябрьская'},
        'house_block': '3',
        'house_number': '84',
    }

    address_of_delivery = {
        'country': {'code': '643'},
        'city': {'name': 'Москва'},
        'region': {'name': 'Москва'},
        'street': {'name': 'Октябрьская'},
        'house_block': '3',
        'house_number': '84',
        'index': '127521',
    }

    registry = get_salary_registry(
        Raiffeisen, RaiffCardRegistry, employees=[
            {
                'record_id': '1',
                'first_name': 'Тест',
                'last_name': 'Тестов',
                'patronymic': 'Тестович',
                'sex': 'M',
                'mobile_phone': '79123456789',
                'personal_account': '40702810603000060801',
                'birthday_date': datetime(1993, 10, 22),
                'birth_country_iso': '643',
                'branch_office_number': '203',
                'identify_card': {
                    'number': '123456', 'series': '1234', 'issue_date': datetime(2010, 1, 30),
                    'issued_by': 'ОУФМС по МО в городском округе Серпухов',
                },
                'citizenship_country_iso': '643',
                'address_of_residence': address_of_residence,
                'address_of_delivery': address_of_delivery,
                'product': 'MC_CORPORATE',
                'secret_word': CipherUtils.cipher_text('itsmysecret'),
            },
        ]
        , automated_contract=True
    )

    registry.set_status(states.FOR_DELIVERY)
    org = registry.contract.org
    org.connection_id = 'ya'
    org.save()

    # ошибка валидации
    response = {
        'code': 'ERROR.INVALID_DATA',
        'message': 'Некорректные данные',
        'errors': [
            {
                'id': '1',
                'invalidFields': [
                    'product'
                ]
            }
        ]
    }

    with response_mock([
        f'POST https://pay-test.raif.ru/api/cards/v1/corporate-cards/batch -> 400 :{json.dumps(response)}',
    ]):
        RaiffRegistryOperator.send(registry=registry)

    registry.refresh_from_db()
    assert registry.is_error
    assert registry.status_to_oebs() == 0

    registry.sent = False
    registry.set_status(states.FOR_DELIVERY)

    with response_mock([
        f'POST https://pay-test.raif.ru/api/cards/v1/corporate-cards/batch -> 200 :',
    ]):
        RaiffRegistryOperator.send(registry=registry)

    registry.refresh_from_db()

    assert registry.is_exported_h2h

    # получаем статус
    response = [
        {
            'id': '1',
            'cardId': '12345678',
            'account': '40702810603000060801',
            'product': 'MC_CORPORATE',
            'status': {
                'value': 'IN_PROGRESS',
                'description': 'В обработке',
                'dateTime': '2021-01-01T12:00:27.87+00:20'
            },
            'person': {
                'companyId': 'HBA23K',
                'maskedName': 'Тестов Т.Т.',
                'birthday': '1980.01.30',
                'gender': 'MALE',
                'maskedCellPhone': '7916***4567',
                'branchId': '603',
                'passport': {
                    'maskedSeries': '1*3*',
                    'maskedNumber': '1***56',
                    'issuedDate': '2012-10-22'
                }
            }
        },
    ]

    with response_mock([
        f'GET https://pay-test.raif.ru/api/cards/v1/corporate-cards/batch/{registry.registry_id} -> 200 : '
        f'{json.dumps(response)}'
    ]):
        RaiffRegistryOperator.status_get(Raiffeisen)

    registry.refresh_from_db()

    assert registry.status == states.PROCESSING
    assert registry.employees[0].result == 'IN_PROGRESS'
    assert registry.employees[0].processing_notes == 'В обработке'

    # ошибка сотрудника
    response[0].update(status={
                'value': 'ERROR.PERSON_VALIDATION',
                'description': 'Ошибка при создании физ лица',
                'dateTime': '2021-01-01T12:00:27.87+00:20'
            })

    with response_mock([
        f'GET https://pay-test.raif.ru/api/cards/v1/corporate-cards/batch/{registry.registry_id} -> 200 : '
        f'{json.dumps(response)}'
    ]):
        RaiffRegistryOperator.status_get(Raiffeisen)

    registry.refresh_from_db()

    assert registry.status == states.REGISTER_ANSWER_LOADED
    assert registry.employees_rejected == 1
    assert registry.employees[0].result == 'ERROR.PERSON_VALIDATION'

    # успешный выпуск карты
    response[0].update(status={
        'value': 'SUCCESS',
        'description': 'Карта успешно выпущена',
        'dateTime': '2021-01-01T12:00:27.87+00:20'
    })

    registry.set_status(states.PROCESSING)

    with response_mock([
        f'GET https://pay-test.raif.ru/api/cards/v1/corporate-cards/batch/{registry.registry_id} -> 200 : '
        f'{json.dumps(response)}'
    ]):
        RaiffRegistryOperator.status_get(Raiffeisen)

    registry.refresh_from_db()

    assert registry.status == states.REGISTER_ANSWER_LOADED
    assert registry.employees_rejected == 0
    assert registry.employees[0].result == 'SUCCESS'
