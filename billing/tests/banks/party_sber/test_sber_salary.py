import datetime
import uuid
from io import BytesIO

import pytest
from lxml.etree import XMLSyntaxError

from bcl.banks.party_sber.registry_operator import SberbankCardRegistry, SberbankSalaryRegistry
from bcl.banks.registry import Sber, SberSpb, SberKdr
from bcl.core.models import states
from bcl.exceptions import SalaryUserHandledError, ValidationError

SBER_BANKS = [Sber, SberSpb, SberKdr]


@pytest.mark.parametrize('associate', SBER_BANKS)
class TestSberbankCardRegistry:

    def test_to_oebs(self, associate):
        registry = SberbankCardRegistry(registry_id=uuid.uuid4())
        assert registry.to_oebs()

    def test_to_oebs_status_exported(self, associate):
        registry = SberbankCardRegistry(
            registry_id=uuid.uuid4(),
            status=states.NEW
        )

        data = registry.to_oebs()

        assert data['status'] == states.NEW

    def test_parse(self, associate, fixturesdir):
        xml_to_parse = fixturesdir.read('card_registry_without_account.xml')

        result = SberbankCardRegistry.incoming_parse(xml_to_parse)

        for employee in result['employees']:
            assert employee['personal_account'] == ''

        xml_to_parse = fixturesdir.read('card_registry_with_empty_result.xml')
        result = SberbankCardRegistry.incoming_parse(xml_to_parse)

        for employee in result['employees']:
            assert employee['result'] == ''

    def test_format_escaping(self, associate, get_salary_contract, validate_xml):
        salary_contract = get_salary_contract(associate, 'ООО "ЯНДЕКС"')

        registry = SberbankCardRegistry(
            contract=salary_contract,
            created_dt=datetime.datetime.utcnow(),
            employees=[
                {
                    'record_id': '1',
                    'identify_card': {'number': 'xxx', 'series': 'xxx'},
                    'address_of_work': {'region': {'name': 'раз два', 'short_name': 'три четыре'}}
                }
            ]
        )

        data = registry.outgoing_compile()[1]
        validate_xml('salary_registry_3.4.xsd', data, [])

        data = data.decode('cp1251')
        assert 'НаименованиеОрганизации="ООО &quot;ЯНДЕКС&quot;"' in data
        assert '<РегионНазвание>раз два</РегионНазвание>' in data
        assert '<РегионСокращение>три четыре</РегионСокращение>' in data
        assert '"1234567890"' in data  # account
        assert '"3456781209"' in data  # inn

    def test_incorrect_length_personal_account(self, associate, salary_parse_from_fixture, get_salary_registry):

        registry = get_salary_registry(
            associate, SberbankCardRegistry,
            reg_id='6e81b961-2095-69c6-e055-000000000075',
            registry_number='923',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'identify_card': {'number': 'xxx', 'series': 'xxx'}
                }])

        result = salary_parse_from_fixture(
            'incorrect_length_personal_account.zip', associate.registry_operator)

        with pytest.raises(ValidationError) as e:
            registry.incoming_save(result[SberbankCardRegistry][0][0])

    def test_missing_tag(self, associate, salary_parse_from_fixture):

        with pytest.raises(XMLSyntaxError) as e:
            salary_parse_from_fixture('missing_tag_in_card.zip', associate.registry_operator)

        assert 'error parsing attribute name' in e.value.msg

    def test_card_registry_not_found(self, associate, salary_parse_from_fixture):

        with pytest.raises(SberbankCardRegistry.DoesNotExist) as e:
            salary_parse_from_fixture('sber_incoming_card.zip', associate.registry_operator)

        assert '6cb6b472-77f4-6dc0-e055-000000000075' in e.value.args[0]

    def test_reject_registry(self, associate, read_fixture, get_salary_registry, ui_has_message):

        registry = get_salary_registry(
            associate, SberbankCardRegistry,
            registry_id='6e825b4b-bb91-28f3-e055-000000000074', registry_number='923',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'identify_card': {'number': 'xxx'}
                }])

        registry.reject()
        registry.save()
        registry.refresh_from_db()

        assert registry.is_rejected

        associate.registry_operator.process_incoming_file(BytesIO(read_fixture('935-01n.zip')))
        msg = ui_has_message('Реестр 923')
        assert 'Файл 935-01n.xml' in msg[0]
        assert 'был удалён' in msg[0]

    def test_upload_employees(self, associate, salary_parse_from_fixture, get_salary_registry):

        registry = get_salary_registry(
            associate, SberbankCardRegistry,
            registry_id='6e83bd4e-945d-06b5-e055-000000000074', registry_number='937',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'identify_card': {'number': 'xxx', 'series': 'xxx'}
                },
                {
                    'record_id': '2',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'identify_card': {'number': 'xxx', 'series': 'xxx'}
                }
            ])

        result = salary_parse_from_fixture('card_employee_1.zip', associate.registry_operator)
        registry.incoming_save(result[SberbankCardRegistry][0][0])
        assert len(registry.to_oebs()['employees']) == 1

        result = salary_parse_from_fixture('card_employee_2.zip', associate.registry_operator)
        registry.incoming_save(result[SberbankCardRegistry][0][0])
        assert len(registry.to_oebs()['employees']) == 2

        registry.refresh_from_db()
        assert registry.employees_rejected == 1
        assert registry.employees_responded == 2

    def test_upload_multireg(self, associate, salary_parse_from_fixture, get_salary_registry):

        reg_params = [
            ('70193D7F-2F7E-5E5D-E055-000000000075', '113', '164508'),
            ('70193D7F-2F7F-5E5D-E055-000000000075', '114', '593009'),
            ('70193D7F-2F80-5E5D-E055-000000000075', '115', '626731')
        ]

        reg_list = []

        for reg_id, reg_number, card_number in reg_params:
            registry = get_salary_registry(
                associate, SberbankCardRegistry,
                registry_id=reg_id, registry_number=reg_number,
                employees=[
                    {
                        'record_id': '1',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'identify_card': {'number': card_number, 'series': 'xxx'}
                    }
                ])
            reg_list.append(registry)

        result = salary_parse_from_fixture('multireg_card.zip', associate.registry_operator)

        assert len(result[SberbankCardRegistry]) == 3

        for i in range(2):
            reg_list[i].incoming_save(result[SberbankCardRegistry][i][0])
            assert len(reg_list[i].to_oebs()['employees']) == 1

    def test_outgoing_file_name(self, associate, get_salary_contract):
        salary_contract = get_salary_contract(associate, 'ООО "ЯНДЕКС"')
        registry = SberbankCardRegistry(
            contract=salary_contract,
            created_dt=datetime.datetime.utcnow(),
            registry_number='123'
        )
        f_name, _, _ = registry.outgoing_compile()

        assert '7813123o.xml' == f_name

    def test_batch_download(self, associate, get_salary_registry):
        registry = get_salary_registry(associate, SberbankCardRegistry, reg_id=uuid.uuid4())

        operator = associate.registry_operator

        filename, contents, content_type = operator.registers_to_file([registry.id], {})
        assert '.xml' in filename
        assert content_type
        registry2 = get_salary_registry(associate, SberbankCardRegistry, reg_id=uuid.uuid4())

        filename, contents, content_type = operator.registers_to_file([registry.id, registry2.id], {})
        assert '.zip' in filename
        assert not content_type


@pytest.mark.parametrize('associate', SBER_BANKS)
class TestSberbankSalaryRegistry:

    def test_to_oebs(self, associate):
        registry = SberbankSalaryRegistry(registry_id=uuid.uuid4())
        data = registry.to_oebs()

        assert isinstance(data['registry_guid'], str)
        assert data['status'] == states.NEW

    def test_to_oebs_status_exported(self, associate):
        registry = SberbankSalaryRegistry(
            registry_id=uuid.uuid4(),
            status=states.NEW
        )

        data = registry.to_oebs()

        assert data['status'] == states.NEW

    def test_format_escaping(self, associate, get_salary_contract, validate_xml):
        salary_contract = get_salary_contract(associate, 'ООО "ЯНДЕКС"')

        registry = SberbankSalaryRegistry(
            contract=salary_contract,
            created_dt=datetime.datetime.utcnow()
        )

        data = registry.outgoing_compile()[1]
        validate_xml('salary_registry_3.4.xsd', data, [])

        data = data.decode('cp1251')
        assert f'БИК="{associate.bid}"' in data
        assert 'НаименованиеОрганизации="ООО &quot;ЯНДЕКС&quot;"' in data

    def test_parse_incoming_registries(self, associate, salary_parse_from_fixture, get_salary_registry):

        for reg_id in ['485b7af3-3832-4a6f-a02b-264c5056a4b2', '32352d01-5dd3-4a49-8cdb-d2fc46921f61']:
            get_salary_registry(associate, SberbankCardRegistry, reg_id=reg_id)

        result = salary_parse_from_fixture('sber_incoming_salary.zip', associate.registry_operator)
        assert len(result) == 1

        result = result.popitem()[1]
        assert len(result) == 2

    def test_incorrect_end_amount(self, associate, salary_parse_from_fixture, get_salary_registry):

        registry = get_salary_registry(
            associate, SberbankSalaryRegistry,
            reg_id='6c568c1f-3b07-0793-e055-000000000075',
            registry_number='735',
            employees=[
            {
                'record_id': '1',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.00'
            }]
        )

        result = salary_parse_from_fixture('ao38140753_160518_195303.zip', associate.registry_operator)

        with pytest.raises(SalaryUserHandledError) as e:
            registry.incoming_save(result[SberbankSalaryRegistry][0][0])

        assert 'Не совпадают суммы зачислений' in e.value.msg

    def test_incorrect_personal_account(self, associate, salary_parse_from_fixture, get_salary_registry):

        registry = get_salary_registry(
            associate, SberbankSalaryRegistry,
            reg_id='6c568c1f-3b07-0793-e055-000000000075',
            registry_number='735',
            employees=[
            {
                'record_id': '1',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '96666666666666666666',
                'amount': '101.00'
            }])

        result = salary_parse_from_fixture('ao38140753_160518_195303.zip', associate.registry_operator)

        with pytest.raises(SalaryUserHandledError) as e:
            registry.incoming_save(result[SberbankSalaryRegistry][0][0])

        assert 'Не совпадают номера счетов' in e.value.msg

    def test_incorrect_record_id(self, associate, salary_parse_from_fixture, get_salary_registry):

        registry = get_salary_registry(
            associate, SberbankSalaryRegistry,
            reg_id='6c568c1f-3b07-0793-e055-000000000075',
            registry_number='735',
            employees=[
            {
                'record_id': '2',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.00'
            }])

        result = salary_parse_from_fixture('ao38140753_160518_195303.zip', associate.registry_operator)

        with pytest.raises(SalaryUserHandledError) as e:
            registry.incoming_save(result[SberbankSalaryRegistry][0][0])

        assert 'Сотрудник <test test test> не найден' in e.value.msg

    def test_incorrect_file_name(self, associate, salary_parse_from_fixture):

        with pytest.raises(SalaryUserHandledError) as e:
            salary_parse_from_fixture('ao1357924680_190617_104148.zip', associate.registry_operator)

        assert 'В архиве не найдены поддерживаемые файлы реестров' in e.value.msg

    def test_missing_tag(self, associate, salary_parse_from_fixture, get_salary_registry):

        get_salary_registry(
            associate, SberbankSalaryRegistry,
            reg_id='6e825b4b-bb91-28f3-e055-000000000074',
            registry_number='935',
            employees=[{
                'record_id': '2',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.00'
            }])

        with pytest.raises(XMLSyntaxError) as e:
            salary_parse_from_fixture('missing_tag_in_salary.zip', associate.registry_operator)

        assert 'error parsing attribute name' in e.value.msg

    def test_salary_registry_not_found(self, associate, salary_parse_from_fixture):

        with pytest.raises(SberbankSalaryRegistry.DoesNotExist) as e:
            salary_parse_from_fixture('ao38140753_160518_195303.zip', associate.registry_operator)

        assert '6c568c1f-3b07-0793-e055-000000000075' in e.value.args[0]

    def test_reject_registry(self, associate, read_fixture, get_salary_registry, ui_has_message):

        registry = get_salary_registry(
            associate, SberbankSalaryRegistry,
            registry_id='6c568c1f-3b07-0793-e055-000000000075',
            registry_number='923',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'personal_account': '96666666666666666666',
                    'amount': '101.00'
                }])

        registry.reject()
        registry.save()
        registry.refresh_from_db()

        assert registry.is_rejected

        associate.registry_operator.process_incoming_file(
            BytesIO(read_fixture('ao38140753_160518_195303.zip')))

        msg = ui_has_message('Реестр 923')
        assert 'был удалён' in msg[0]
        assert 'Файл 725-01y.xml' in msg[0]

    def test_upload_employees(self, associate, salary_parse_from_fixture, get_salary_registry):

        registry = get_salary_registry(
            associate, SberbankSalaryRegistry,
            reg_id='6e840997-1bbd-3024-e055-000000000075',
            registry_number='937',
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'personal_account': '66666666666666666666',
                    'amount': '1001.00'
                },
                {
                    'record_id': '2',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'personal_account': '66666666666666666666',
                    'amount': '1001.00'
                }
            ])

        result = salary_parse_from_fixture('salary_employee_1.zip', associate.registry_operator)
        registry.incoming_save(result[SberbankSalaryRegistry][0][0])
        assert len(registry.to_oebs()['employees']) == 1

        result = salary_parse_from_fixture('salary_employee_2.zip', associate.registry_operator)
        registry.incoming_save(result[SberbankSalaryRegistry][0][0])
        employees = registry.to_oebs()['employees']
        assert len(registry.to_oebs()['employees']) == 2

    def test_fractional_amount(self, associate, get_salary_registry):
        from lxml import etree

        registry = get_salary_registry(
            associate, SberbankSalaryRegistry,
            reg_id='6e825b4b-bb91-28f3-e055-000000000074',
            registry_number='935',
            income_type='2',
            employees=[{
                'record_id': '2',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '66666666666666666666',
                'amount': '101.12',
                'amount_deduct': '9.03'
            }],
        )

        _, xml, _ = registry.outgoing_compile()
        root = etree.fromstring(xml)

        assert f'БИК="{associate.bid}"' in xml.decode('cp1251')
        assert root.xpath('//Сотрудник/Сумма')[0].text == '101.12'
        assert root.xpath('//Сотрудник/ОбщаяСуммаУдержаний')[0].text == '9.03'
        assert root.xpath('//КонтрольныеСуммы/СуммаИтого')[0].text == '101.12'
        assert root.xpath('/СчетаПК/КодВидаДохода')[0].text == '2'

        assert b'"1234567890"' in xml  # account
        assert b'"3456781209"' in xml  # inn

    def test_upload_multireg(self, associate, salary_parse_from_fixture, get_salary_registry):

        reg_params = [
            ('70193D7F-2F81-5E5D-E055-000000000075', '113'),
            ('70193D7F-2F82-5E5D-E055-000000000075', '114'),
            ('70193D7F-2F83-5E5D-E055-000000000075', '115')
        ]

        reg_list = []
        for reg_id, reg_number in reg_params:

            registry = get_salary_registry(
                associate, SberbankSalaryRegistry,
                reg_id=reg_id,
                registry_number=reg_number,
                employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'personal_account': '66666666666666666666',
                    'amount': '1001.00'
                }])

            reg_list.append(registry)

        result = salary_parse_from_fixture('multireg_salary.zip', associate.registry_operator)

        assert len(result[SberbankSalaryRegistry]) == 3

        for i in range(2):
            reg_list[i].incoming_save(result[SberbankSalaryRegistry][i][0])
            assert len(reg_list[i].to_oebs()['employees']) == 1

    def test_outgoing_file_name(self, associate, get_salary_contract):
        salary_contract = get_salary_contract(associate, 'ООО "ЯНДЕКС"')
        registry = SberbankSalaryRegistry(
            contract=salary_contract,
            created_dt=datetime.datetime.utcnow(),
            registry_number='123'
        )
        f_name, _, _ = registry.outgoing_compile()

        assert '7813123z.xml' == f_name

    def test_parse_xml(self, associate, get_salary_registry, salary_parse_from_fixture):
        registry_dict = {
            '81b5e002-8db8-4f09-e053-036fa8c0b934': get_salary_registry(
                associate, SberbankSalaryRegistry,
                reg_id='81b5e002-8db8-4f09-e053-036fa8c0b934',
                registry_number='101',
                employees=[
                    {
                        'record_id': '1',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810838110846218',
                        'amount': '30867.00'
                    },
                    {
                        'record_id': '2',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810242002386951',
                        'amount': '8683.45'
                    },
                    {
                        'record_id': '3',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810242003491913',
                        'amount': '6968.28'
                    },
                    {
                        'record_id': '4',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810538117735826',
                        'amount': '17577.00'
                    }
                ]
            ),
            '81c2cafa-2254-04db-e053-036fa8c05c35': get_salary_registry(
                associate, SberbankSalaryRegistry,
                reg_id='81c2cafa-2254-04db-e053-036fa8c05c35',
                registry_number='146',
                employees=[
                    {
                        'record_id': '1',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810542053702572',
                        'amount': '22125.35'
                    },
                    {
                        'record_id': '2',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810852090815432',
                        'amount': '7309.14'
                    }
                ]
            ),
            '81c2cafa-2255-04db-e053-036fa8c05c35': get_salary_registry(
                associate, SberbankSalaryRegistry,
                reg_id='81c2cafa-2255-04db-e053-036fa8c05c35',
                registry_number='159',
                employees=[
                    {
                        'record_id': '1',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810718351353196',
                        'amount': '1479.01'
                    },
                    {
                        'record_id': '2',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810360311219953',
                        'amount': '85.04'
                    },
                    {
                        'record_id': '3',
                        'first_name': 'a',
                        'last_name': 'b',
                        'patronymic': 'c',
                        'currency_code': 'RUB',
                        'personal_account': '40817810340013194306',
                        'amount': '11568.02'
                    }
                ]
            )
        }

        result = salary_parse_from_fixture('sber_incoming_xml.xml', associate.registry_operator)

        assert len(result[SberbankSalaryRegistry]) == 3

        for inc_registry, registry in result[SberbankSalaryRegistry]:
            reg_data = registry_dict[str(registry.registry_id)]
            reg_data.incoming_save(inc_registry)
            oebs_registry = reg_data.to_oebs()
            if len(oebs_registry['employees']) == 3:
                assert reg_data.employees[0].processing_notes
                assert reg_data.employees_rejected == 2
                assert reg_data.employees_responded == 3

                assert oebs_registry['employees'][0]['response'] == 'Зачислено'
                assert oebs_registry['employees'][1]['response'] == 'неЗачислено'
                assert oebs_registry['employees'][2]['response'] == 'НеЗачислено'
            else:
                assert reg_data.employees_responded == len(oebs_registry['employees'])
                for emp in oebs_registry['employees']:
                    assert emp['response'] == 'Зачислено'
