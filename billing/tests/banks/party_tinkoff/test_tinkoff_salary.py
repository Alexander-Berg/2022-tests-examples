import uuid
from datetime import datetime
from io import BytesIO

import pytest
from lxml import etree

from bcl.banks.party_tinkoff.registry_operator import (
    TinkoffCardRegistry, TinkoffDismissalRegistry, TinkoffSalaryRegistry,
)
from bcl.banks.registry import Tinkoff
from bcl.core.models import states
from bcl.exceptions import SalaryUserHandledError


class TestTinkoffCardRegistry:

    def test_to_oebs(self):
        registry = TinkoffCardRegistry(registry_id=uuid.uuid4())
        assert registry.to_oebs()

    def test_to_oebs_status_exported(self):
        registry = TinkoffCardRegistry(
            registry_id=uuid.uuid4(),
            status=states.NEW
        )

        data = registry.to_oebs()

        assert data['status'] == states.NEW

    def test_tags(self, get_salary_registry, django_assert_num_queries):

        registry = get_salary_registry(Tinkoff, TinkoffCardRegistry,
            reg_id='6e825b4b-bb91-28f3-e055-000000000074',
            registry_number='935',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'Иванов',
                    'last_name': 'Иван',
                    'patronymic': 'c',
                    'foreigner_card': {
                        'issue_date': '2018-08-27',
                        'valid_date': '2018-08-28',
                    }
                },
                {
                    'record_id': '2',
                    'first_name': 'Петров',
                    'last_name': 'Пётр',
                    'patronymic': 'c',
                },
            ]
        )

        with django_assert_num_queries(0):
            _, xml, _ = registry.outgoing_compile()

        xml_decoded = xml.decode('cp1251')

        assert 'ИНН="3456781209"' in xml_decoded

        root = etree.fromstring(xml)

        assert root.xpath('//Сотрудник/ДокументИностранногоГражданина/ДатаВыдачи')[0].text == '2018-08-27'
        assert root.xpath('//Сотрудник/ДокументИностранногоГражданина/ДействительноДо')[0].text == '2018-08-28'

        with pytest.raises(IndexError):
            # Нет блока с информацией об удостоверении.
            assert root.xpath('//Сотрудник/ДокументИностранногоГражданина/ДатаВыдачи')[1]


class TestTinkoffDismissRegistry:

    def test_parse(self, fixturesdir):
        xml_to_parse = fixturesdir.read('registry_dismiss.xml')

        result = TinkoffDismissalRegistry.incoming_parse(xml_to_parse)

        for employee in result['employees']:
            assert employee['personal_account'] != ''

        assert result['employees'][0]['result']

    def test_dismissal_registry(self, path_fixture, get_salary_registry, salary_parse_from_fixture):

        registry = get_salary_registry(Tinkoff, TinkoffDismissalRegistry,
            reg_id='0be81983-0631-4d48-96f7-12fb86325c78',
            registry_number='101',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'Иванов',
                    'last_name': 'Иван',
                    'patronymic': 'Иванович',
                    'personal_account': '6666666666666666666',
                    'dismissal_date': datetime(2018, 6, 13),
                }
            ])

        _, xml, _ = registry.outgoing_compile()
        xml_decoded = xml.decode('cp1251')

        assert 'ИНН="3456781209"' in xml_decoded

        result = salary_parse_from_fixture('correct_dismissal.zip', Tinkoff.registry_operator)
        registry.incoming_save(result[TinkoffDismissalRegistry][0][0])
        assert len(registry.to_oebs()['employees']) == 1

    def test_upload_employees(self, path_fixture, get_salary_registry, salary_parse_from_fixture):

        registry = get_salary_registry(Tinkoff, TinkoffDismissalRegistry,
            reg_id='0be81983-0631-4d48-96f7-12fb96325c78',
            registry_number='103',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'Петр',
                    'last_name': 'Петров',
                    'patronymic': 'Петрович',
                    'personal_account': '6666666666666666667',
                    'dismissal_date': datetime(2018, 6, 13),
                },
                {
                    'record_id': '2',
                    'first_name': 'Павел',
                    'last_name': 'Павлов',
                    'patronymic': 'Павлович',
                    'personal_account': '6666666666666666666',
                    'dismissal_date': datetime(2018, 6, 13),
                }
            ])

        op = Tinkoff.registry_operator
        result = salary_parse_from_fixture('correct_dismissal_emp_1.zip', op)
        registry.incoming_save(result[TinkoffDismissalRegistry][0][0])
        assert len(registry.to_oebs()['employees']) == 1

        result = salary_parse_from_fixture('correct_dismissal_emp_2.zip', op)
        registry.incoming_save(result[TinkoffDismissalRegistry][0][0])
        assert len(registry.to_oebs()['employees']) == 2

    def test_incorrect_personal_account(self, path_fixture, get_salary_registry, salary_parse_from_fixture):

        registry = get_salary_registry(Tinkoff, TinkoffDismissalRegistry,
            reg_id='0be81983-0631-4d48-96f7-12fb86325c78',
            registry_number='101',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'Иванов',
                    'last_name': 'Иван',
                    'patronymic': 'Иванович',
                    'personal_account': '9666666666666666666',
                    'dismissal_date': datetime(2018, 6, 13),
                }
            ])

        result = salary_parse_from_fixture('correct_dismissal.zip', Tinkoff.registry_operator)

        with pytest.raises(SalaryUserHandledError) as e:
            registry.incoming_save(result[TinkoffDismissalRegistry][0][0])

        assert 'Сотрудник <Иван Иванов Иванович> не найден' in e.value.msg

    def test_salary_registry_not_found(self, salary_parse_from_fixture):

        with pytest.raises(TinkoffDismissalRegistry.DoesNotExist) as e:
            salary_parse_from_fixture('correct_dismissal.zip', Tinkoff.registry_operator)

        assert '0be81983-0631-4d48-96f7-12fb86325c78' in e.value.args[0]

    def test_reject_registry(self, read_fixture, get_salary_registry, ui_has_message):

        registry = get_salary_registry(Tinkoff, TinkoffDismissalRegistry,
            reg_id='0be81983-0631-4d48-96f7-12fb86325c78',
            registry_number='101',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'Иванов',
                    'last_name': 'Иван',
                    'patronymic': 'Иванович',
                    'personal_account': '6666666666666666666',
                    'dismissal_date': datetime(2018, 6, 13),
                }
            ])

        registry.reject()
        registry.save()
        registry.refresh_from_db()

        assert registry.is_rejected

        Tinkoff.registry_operator.process_incoming_file(BytesIO(read_fixture('correct_dismissal.zip')))
        msg = ui_has_message('Реестр 101')
        assert 'был удалён' in msg[0]
        assert 'Файл 101-01p.xml' in msg[0]


class TestTinkoffSalaryRegistry:

    def test_to_oebs(self):
        registry = TinkoffSalaryRegistry(registry_id=uuid.uuid4())
        data = registry.to_oebs()

        assert isinstance(data['registry_guid'], str)
        assert data['status'] == states.NEW

    def test_to_oebs_status_exported(self):
        registry = TinkoffSalaryRegistry(
            registry_id=uuid.uuid4(),
            status=states.NEW
        )

        data = registry.to_oebs()

        assert data['status'] == states.NEW

    def test_fractional_amount(self, get_salary_registry):
        from lxml import etree

        registry = get_salary_registry(Tinkoff, TinkoffSalaryRegistry,
            reg_id='6e825b4b-bb91-28f3-e055-000000000074',
            registry_number='935',
            employees=[{
                'record_id': '2',
                'first_name': 'Иванов',
                'last_name': 'Иван',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.12'
            }]
        )

        _, xml, _ = registry.outgoing_compile()
        xml_decoded = xml.decode('cp1251')

        assert 'КодВидаДохода' not in xml_decoded
        assert 'ОбщаяСуммаУдержаний' not in xml_decoded
        assert 'ИНН="3456781209"' in xml_decoded

        root = etree.fromstring(xml)

        assert root.xpath('//Сотрудник/Сумма')[0].text == '101.12'
        assert root.xpath('//КонтрольныеСуммы/СуммаИтого')[0].text == '101.12'

    def test_income_type(self, get_salary_registry):
        registry = get_salary_registry(
            Tinkoff, TinkoffSalaryRegistry,
            reg_id='6e825b4b-bb91-28f3-e055-000000000074',
            registry_number='935',
            income_type='3',
            employees=[{
                'record_id': '2',
                'first_name': 'Иванов',
                'last_name': 'Иван',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.12',
                'amount_deduct': '11',
            }]
        )

        content = registry.outgoing_compile()[1].decode('cp1251')

        assert not registry.nonresidential
        assert '<ОбщаяСуммаУдержаний>11.00</ОбщаяСуммаУдержаний>' in content
        assert '<КодВидаДохода>3</КодВидаДохода>' in content

    def test_nonresidential(self, get_salary_registry):
        registry = get_salary_registry(
            Tinkoff, TinkoffSalaryRegistry,
            reg_id='6e825b4b-bb91-28f3-e055-000000000074',
            registry_number='935',
            employees=[{
                'record_id': '2',
                'first_name': 'Иванов',
                'last_name': 'Иван',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '40820666666666666666',
                'amount': '101.12',
            },
            {
                'record_id': '2',
                'first_name': 'Иванов',
                'last_name': 'Иван',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.12',
            }]
        )
        assert registry.nonresidential
