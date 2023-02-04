import base64
import json
import re
from datetime import datetime
from functools import partial
from io import BytesIO
from uuid import uuid4

import pytest
from django.conf import settings
from django.http import HttpRequest
from django.test import Client
from django.db.models import F

from bcl.banks.common.callback_handler import CallbackHandler
from bcl.banks.party_sber.registry_operator import SberbankSalaryRegistry, SberbankCardRegistry
from bcl.banks.registry import (
    Sber, SberSpb, Ing, IngUa, Otkritie, Unicredit, VtbDe, Acba, VtbAm, Raiffeisen, AlfaKz, Tinkoff, Respublika
)
from bcl.core.models import (
    states, Account, Currency, Role, Organization, Statement, Payment,
    Intent, SalaryContract, SigningRight, PaymentsBundle, OrganizationGroup, Attachment, StatementRegister,
)
from bcl.core.models.limits import REVISERS
from bcl.core.tasks import process_statements
from bcl.core.views.front import get_file, AuditEntry, dss_sign_by_intent_code, paginate
from bcl.exceptions import (
    PermissionDenied, AccountCreateError, UserHandledException, NotEnoughArguments, DigitalSignError, LogicError
)
from bcl.toolbox.signatures import DetachedMultipleSignature, XmlSignature
from bcl.toolbox.utils import DateUtils


def test_paginate():
    request = HttpRequest()
    objects = [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12]
    page = paginate(request=request, objects=objects)

    assert page.object_list == objects
    assert page.per_page_choices == [500, 1000, 2000, 3000]
    assert page.paginator.per_page == 1000

    request.GET['plimit'] = 3
    page = paginate(request=request, objects=objects)
    assert page.object_list == [1, 2, 3]
    assert page.paginator.per_page == 3

    request.GET['plimit'] = 20
    page = paginate(
        request=request, objects=objects,
        per_page_choices=[2, 5, 10],
    )
    # поднимаем ограничение вывода до max(choices)
    assert page.per_page_choices == [2, 5, 10]
    assert len(page.object_list) == 10  # 11 элементов, 12й не позволяем.
    assert page.paginator.per_page == 10

    request.GET['plimit'] = 12
    page = paginate(request=request, objects=objects)
    # отображаем текущее ограничение в вариантах выбора
    assert page.per_page_choices == [12, 500, 1000, 2000, 3000]
    assert page.paginator.per_page == 12

    request.GET['plimit'] = 10000
    page = paginate(request=request, objects=objects, per_page_choices=[3000, 5000, 10000, 20000])
    assert page.paginator.per_page == 10000


def test_get_file(
    get_assoc_acc_curr, get_payment_bundle, get_source_payment, get_statement, mock_mds, read_fixture, init_user,
    init_uploaded,
):
    user = init_user()

    def do_assert(associate, encoding, assert_str='Яндекс', acc_num=None, payment_attrs=None, currency=None):
        if not acc_num:
            acc_num = f'{associate.id}_0'

        associate, acc, _ = get_assoc_acc_curr(
            associate.id, account={'number': acc_num, 'currency_code': currency or 'RUB'}
        )

        payment_attrs = payment_attrs or {}
        payment_attrs['f_acc'] = acc_num
        bundle = get_payment_bundle([
            get_source_payment(payment_attrs, associate=associate)], account=acc)

        fname = f'pbundle_{bundle.number}'

        _, new = get_file({'n': fname})
        assert isinstance(new, bytes)

        decoded = new.decode(encoding)

        assert assert_str in decoded

        try:
            decoded_wrong = new.decode('utf-16' if encoding == 'utf-8' else 'utf-8')

            assert assert_str not in decoded_wrong

        except UnicodeDecodeError:
            pass

        return decoded

    do_assert(Ing, 'utf-16', acc_num='00000810')
    do_assert(Ing, 'cp1251', assert_str='Кинопортал', payment_attrs={'ground': 'nocyrillics'})
    do_assert(IngUa, 'utf-16', assert_str='Кинопортал')
    do_assert(IngUa, 'utf-16', assert_str='Кинопортал', currency='UAH')
    do_assert(Otkritie, 'cp1251')
    do_assert(Raiffeisen, 'cp1251')
    do_assert(Sber, 'cp1251')
    do_assert(Unicredit, 'utf-8', payment_attrs={'n_kbk': ''})
    do_assert(Acba, 'utf-8', acc_num='123', payment_attrs={'t_acc': '123', 'f_acc': '321'})
    do_assert(VtbAm, 'utf-8', acc_num='123', payment_attrs={'t_acc': '123', 'f_acc': '321'})
    do_assert(VtbDe, 'utf-8', assert_str='Yandex', payment_attrs={
        'f_name': 'Yandex',
        't_name': 'Kinoportal',
        't_address': 'address',
        'ground': 'nocyrillics',
    })

    # Проверим, что в случае необходимости похватываются данные из MDS.
    statement = get_statement(read_fixture('sber_intraday_cleanup.txt'), associate=Sber)
    statement.mds_move()
    statement.refresh_from_db()
    assert statement.mds_path
    assert not statement.raw

    data = get_file({'n': f'st_{statement.id}'})
    assert data[1].startswith(b'1CClientBankExchange')

    # Проверим раздачу вложений.
    attachment = Attachment.from_uploaded(
        uploaded=init_uploaded(),
        linked=statement,
        user=user,
    )
    data = get_file({'n': f'att_{attachment.id}'})
    assert data == ('some.dat', b'abcd')


class ViewsTestSuite:

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user, mocker):
        init_user(roles=[Role.SUPPORT, Role.SALARY_GROUP])
        mocker.patch('bcl.banks.party_raiff.Raiffeisen.automate_payments', lambda *_: True)

    def setup_method(self, method):
        self.client = Client()

    @pytest.fixture
    def get_payment(self, get_source_payment, get_assoc_acc_curr):
        def wrapper(eng=None, associate=None, acc_num=None):

            associate = associate or Raiffeisen
            cur = 'RUB'
            org, _ = Organization.objects.get_or_create(name='Российское юрлицо')
            number = acc_num or '40702810800000007671'

            if eng:
                org = None
                cur = 'USD'
                number = '40702810800000007672'

            _, account, _ = get_assoc_acc_curr(associate.id, account={'number': number, 'currency_code': cur}, org=org)

            return get_source_payment(associate=associate, attrs={'f_inn': '37250679', 'f_acc': account.number})

        return wrapper

    def send_json(self, url, data):
        return self.client.post(url, json.dumps(data), content_type='application/json')

    def restricted(self, url, params=None, files=None, method='get', file_key='statement_file'):
        """Отправляет запрос и проверяет, что появилось исключение PermissionDenied"""
        with pytest.raises(PermissionDenied) as e:
            params = params or {}
            if files:
                params.update({
                    file_key: files
                })
            getattr(self.client, method)(url, params)
        return 'Недостаточно прав' in e.value.msg

    def create_and_edit_salary_contract(self, org, contract, check_client_response):
        salary_data = {'associate_id': Sber.id, 'org': org.id, 'number': '123456789', 'save': '1', 'id': ''}
        assert check_client_response('/settings/salary/', salary_data, method='post')
        assert SalaryContract.getone(number='123456789')
        assert check_client_response(
            '/settings/salary/',
            {
                'id': contract.id, 'associate_id': AlfaKz.id, 'save': '1', 'org': contract.org.id,
                'number': contract.number
            }, method='post')
        contract.refresh_from_db()
        assert contract.associate_id == AlfaKz.id


class TestViewsStaleApi(ViewsTestSuite):

    def bundle_get_contents(self, *, bundle: PaymentsBundle, dss: bool = False, sign_method: str = ''):

        response = self.send_json('/api/bundle_get_contents', {
            'associate': bundle.associate_id,
            'bundles': [str(bundle.id)],
            'dss': dss,
            'sign_method': sign_method,
        })
        return response

    def test_bundle_get_contents(self, mock_login, build_payment_bundle, dss_signing_right):

        associate = Unicredit

        with pytest.raises(NotEnoughArguments):
            self.send_json('/api/bundle_get_contents', {})

        with pytest.raises(DigitalSignError):
            self.send_json('/api/bundle_get_contents', {
                'associate': associate.id, 'bundles': [], 'dss': False, 'sign_method': ''})

        bundle1 = build_payment_bundle(associate, h2h=True)

        dss_signing_right(associate=associate)
        # Намерение подписать на DSS
        response = self.bundle_get_contents(bundle=bundle1, dss=True, sign_method=DetachedMultipleSignature.alias)

        assert b'dss_code' in response.content
        assert response.status_code == 200

        # Собираем дайджест.
        response = self.bundle_get_contents(bundle=bundle1).json()
        assert len(response) == 1
        assert str(bundle1.id) == response[0]['id']
        assert response[0]['contents'].startswith('<?')

    def test_bundle_signed_save_remove(self, mock_login, build_payment_bundle, xml_signature, dss_signing_right):

        associate = Unicredit

        with pytest.raises(NotEnoughArguments):
            self.send_json('/api/bundle_signed_save', {})

        with pytest.raises(DigitalSignError):
            self.send_json('/api/bundle_signed_save', {'signed': [], 'associate': Sber.id})

        with pytest.raises(UserHandledException):
            self.send_json('/api/bundle_signed_save', {'signed': [], 'associate': associate.id})

        bundle1 = build_payment_bundle(associate, h2h=True)
        assert bundle1.digital_signs.count() == 0

        dss_signing_right(associate=associate, username='testuser', serial='12000d3ece1cbf936c80c766fb0000000d3ece')

        response = self.send_json('/api/bundle_signed_save', {
            'signed': [{
                'id': bundle1.id,
                'contents': xml_signature,
            }],
            'associate': associate.id,
        })
        assert response.status_code == 200
        assert bundle1.digital_signs.count() == 1

        # Далее удаление подписи.
        with pytest.raises(NotEnoughArguments):
            self.send_json('/api/bundle_sign_remove', {})

        response = self.send_json('/api/bundle_sign_remove', {
            'bundles': [str(bundle1.id)],
        })
        assert bundle1.digital_signs.count() == 0
        assert response.status_code == 200


class TestViewsAvailable(ViewsTestSuite):

    expected_editable_base = ['urgent', 'ground']

    expected_non_editable_base = [
        'number_oebs', 'number', 'summ', 'date',
        'f_iban', 'f_acc', 'f_cacc', 'f_bic', 'f_swiftcode', 'f_bankname', 'f_inn', 'f_kpp', 't_iban',
        't_acc', 't_cacc', 't_bic', 't_swiftcode', 't_bankname',
        't_inn', 't_kpp', 't_name', 't_bank_city', 'i_swiftcode', 'income_type'
    ]

    currency_fields = [
        'oper_code', 'trans_pass', 'contract_sum', 'expected_dt', 'advance_return_dt'
    ]

    expected_editable_usd = expected_editable_base + [
        't_address', 'official_name', 'official_phone', 'f_name', 'write_off_account'
    ] + currency_fields

    expected_editable_kzt = expected_editable_base + [
        'knp', 'head_of_company', 't_kbe', 'f_kbe', 'official_name', 'official_phone', 'f_name', 'write_off_account',
        't_address'
    ]

    expected_editable_azn = expected_editable_base + [
        'kbu', 't_bank_code', 'official_name', 'official_phone', 'f_name', 'write_off_account', 't_address'
    ]

    expected_editable_inter = expected_editable_usd + ['i_swiftcode']

    expected_non_editable_rub = expected_non_editable_base + [
        't_address', 'f_name', 't_address',
        'n_kod', 'n_status', 'n_kbk', 'n_okato', 'n_ground', 'n_period', 'n_doc_num', 'n_doc_date', 'n_type',
    ]
    expected_non_editable_usd = expected_non_editable_base + [
        'head_of_company', 'contract_num', 'contract_dt'
    ]
    expected_non_editable_kzt = expected_non_editable_base + ['n_kbk']

    expected_non_editable_selfemp = expected_non_editable_rub + [
        't_last_name', 't_middle_name', 't_first_name', 't_contract_type', 'oper_code'
    ]

    expected_non_editable_azn = expected_non_editable_base + ['head_of_company']

    expected_non_editable_inter = expected_non_editable_usd.copy()
    expected_non_editable_inter.remove('i_swiftcode')

    def test_getfile(self, get_payment_bundle, get_statement, check_client_response):
        associate = Ing
        bundle = get_payment_bundle([{}], associate=associate)
        statement = get_statement(b'<', associate)

        assert check_client_response(f'/get_file?n=pbundle_{bundle.id}', check_content=lambda data: data.startswith(b'\xff'))
        assert check_client_response(f'/get_file?n=stprint_{statement.id}', check_content=lambda data: data == b'')
        assert check_client_response(f'/get_file?n=st_{statement.id}', check_content=lambda data: data == b'<')

    def test_associate_summary(self):

        response = self.client.get(f'/associates/{Sber.id}/')
        assert response.status_code == 302
        assert response.url == f'/associates/{Sber.id}/payments/'

    def test_charts(self):
        response = self.client.get('/reports/charts/')
        assert response.status_code == 200
        assert 'Общее количество платежей' in response.content.decode()

    def test_tasks(self):
        response = self.client.get('/reports/tasks/')
        assert response.status_code == 200
        assert 'unicredit_intraday_statements, unicredit_statements' in response.content.decode()

    def test_rerun(self):

        success_response = {'status': 'success', 'status_desc': ''}

        # запуск фонового
        response = self.client.get('/rerun', data={'assoc': Sber.id, 'proc': 'force_task', 'name': 'reset_payment_numbers'})
        assert response.json() == success_response

        # запуск автоматизации. зависящая от внешней системы.
        response = self.client.get('/rerun', data={'assoc': Tinkoff.id, 'proc': 'automate_registries'})
        assert response.json() == success_response

        # запуск автоматизации. общая для внешних систем.
        response = self.client.get('/rerun', data={'proc': 'fake_statements'})
        assert response.json() == success_response

    def test_audit(self, get_payment, check_client_response):
        url = f'/associates/{Raiffeisen.id}/payments/'
        payment = get_payment()
        assert check_client_response(f'{url}{payment.id}/edit/', params={'action': 'cancel'}, method='post')

        url = '/reports/audit/'
        assert check_client_response(url)
        entries = AuditEntry.objects.all()
        assert entries[0].action == AuditEntry.ACTION_PAYMENT_CANCEL

    def test_printform(self, get_payment, check_client_response):
        payment = get_payment()
        url = f'/associates/{payment.associate_id}/payments/'

        assert check_client_response(
            url + f'?number={payment.number}',
            {'action': 'print_list', 'items': f'["{payment.id}"]'}, method='post',
            check_content='>Сто пятьдесят два рубля 00 копеек<'
        )

        payment = get_payment(eng=True)
        url = f'/associates/{payment.associate_id}/payments/'

        assert check_client_response(
            url + f'?number={payment.number}',
            {'action': 'print_list', 'items': f'["{payment.id}"]'}, method='post',
            check_content='152,00 RUB'
        )

    def test_filter_userinput(self, check_client_response):
        # проверяем обработку невалидного пользовательского ввода

        url = f'/associates/{Raiffeisen.id}/payments/'

        def check_input(arg, val, *, text=''):

            if not text:
                text = f'Указанное вами значение «{val}» не может быть использовано для фильтрации'

            def check(content):
                return text in content.decode()

            assert check_client_response(f'{url}?{arg}={val}', check_content=check)

        check_input('dt_from', '08-07-21')
        check_input('number', 'B265520784')
        check_input('sum_to', '25,936.21')
        check_input('bundle_number', 'PHILIP MORRIS LTD')

        url = f'/associates/{Raiffeisen.id}/statements/'
        check_input('status', '????')
        check_input('status', 'Дата загрузки выписки')

    def test_payments_views(
        self, get_payment, get_assoc_acc_curr, build_payment_bundle, django_assert_num_queries, time_shift,
        check_client_response
    ):
        url = f'/associates/{Raiffeisen.id}/payments/'

        with django_assert_num_queries(6) as _:
            assert check_client_response(url)

        payment = get_payment()

        with django_assert_num_queries(7) as _:
            assert check_client_response(url)

        edit_url = f'{url}{payment.id}/edit/'
        assert check_client_response(edit_url)
        assert check_client_response(
            edit_url,
            {'ground': 'ground', 'contract_sum': '20,15', 'contract_currency_id': '643', 'payedit': '1'},
            method='post')

        payment.refresh_from_db()
        assert payment.ground == 'ground'
        assert payment.contract_id  # создался контракт по УНК

        shift = 60 * 60 * 24 * 2

        # аннулирование
        payment = get_payment()
        with time_shift(shift, backwards=True):
            assert check_client_response(f'{url}{payment.id}/edit/', params={'action': 'cancel'}, method='post')
        assert AuditEntry.getone(description=f'№{payment.number}').action == AuditEntry.ACTION_PAYMENT_CANCEL

        with django_assert_num_queries(7) as _:
            assert check_client_response(url)

        # отзыв
        bundle = build_payment_bundle(Sber, payment_dicts=[{'status': states.EXPORTED_ONLINE}])
        payment = bundle.payments[0]
        with time_shift(shift, backwards=True):
            assert check_client_response(f'{url}{payment.id}/edit/', params={'action': 'revoke'}, method='post')
        assert AuditEntry.getone(description=f'№{payment.number}').action == AuditEntry.ACTION_PAYMENT_REJECT

        # отказ казначеем через платёж
        payment = get_payment()
        # 1. причина не указана, не обрабатываем
        assert check_client_response(
            f'{url}{payment.id}/edit/', params={'action': 'invalidate_manually', 'note': ''}, method='post')
        payment.refresh_from_db()
        assert payment.status != states.USER_INVALIDATED
        # 2. причина указана, отказываем
        assert check_client_response(
            f'{url}{payment.id}/edit/', params={'action': 'invalidate_manually', 'note': 'bogus'}, method='post')
        payment.refresh_from_db()
        assert payment.status == states.USER_INVALIDATED
        assert (
            AuditEntry.getone(description=f'№{payment.number}').action ==
            AuditEntry.ACTION_PAYMENT_INVALIDATED_BY_USER)

        # отказ вручную массово
        bundle = build_payment_bundle(Raiffeisen, payment_dicts=[{'status': states.EXPORTED_ONLINE}])
        payment = bundle.payments[0]
        assert check_client_response(
            url, {'action': 'set_reason', 'items': f'[{payment.id}]', 'reason': 'test'}, method='post'
        )
        audit_action = AuditEntry.getone(description=f'ID {payment.id}')
        assert audit_action.action == AuditEntry.ACTION_PAYMENT_MANUALLY_DECLINED
        assert audit_action.action_title == AuditEntry.ACTIONS[AuditEntry.ACTION_PAYMENT_MANUALLY_DECLINED]

        payment.refresh_from_db()
        assert payment.is_declined_by_bank
        assert payment.processing_notes == 'test'

        _, account, _ = get_assoc_acc_curr(Raiffeisen.id)

        # Проверяем фильтр по счёту.
        assert check_client_response(url, {'associate_id':  Raiffeisen.id, 'org': account.org.id, 'acc': account.id})

        params = {'associate_id':  Raiffeisen.id, 'org': account.org.id, 'account': account.id}
        assert check_client_response('/show_accounts_balance', params, method='post')

    def test_save_bcl_invalidated(self, get_assoc_acc_curr,get_source_payment ):
        _, account, _ = get_assoc_acc_curr(
            Sber.id, account={'number': '40702810800000007672', 'currency_code': Currency.by_num[Currency.RUB]}
        )
        payment = get_source_payment(
            associate=Sber,
            attrs={
                'f_inn': '37250679', 'f_acc': account.number, 'currency_id': Currency.RUB
            }
        )
        payment.set_status(states.BCL_INVALIDATED, do_save=True)
        edit_url = f'/associates/{Sber.id}/payments/{payment.id}/edit/'
        self.client.post(edit_url, {'ground': 'test', 'payedit': '1'})
        payment.refresh_from_db()
        assert payment.status == states.NEW

    @pytest.mark.parametrize(
        'associate, currency, editable_expected, non_editable_expected, is_selfemployed, kz_fields',
        [
            (Sber, Currency.RUB, 'expected_editable_base', 'expected_non_editable_rub', False, False),
            (Sber, Currency.USD, 'expected_editable_usd', 'expected_non_editable_usd', False, False),
            (Sber, Currency.RUB, 'expected_editable_base', 'expected_non_editable_selfemp', True, False),
            (AlfaKz, Currency.KZT, 'expected_editable_kzt', 'expected_non_editable_kzt', False, True),

        ])
    def test_payment_edit_all_fields(
            self, associate, currency, editable_expected, non_editable_expected, is_selfemployed, get_assoc_acc_curr,
            get_source_payment, get_editable_fields_value, kz_fields,
    ):

        _, account, _ = get_assoc_acc_curr(
            associate.id, account={'number': '40702810800000007672', 'currency_code': Currency.by_num[currency]}
        )
        payment = get_source_payment(
            associate=associate,
            attrs={
                'f_inn': '37250679', 'f_acc': account.number, 'currency_id': currency,
                'payout_type': 1 if is_selfemployed else None
            }
        )
        edit_url = f'/associates/{associate.id}/payments/{payment.id}/edit/'
        result = self.client.get(edit_url)
        editable_fields, uneditable_fields, _ = get_editable_fields_value(result.content)
        edit_rules = {'urgent': '1', 'contract_currency_id': '840', 'payedit': '1'}
        expected_editable_set = set(getattr(self, editable_expected))

        if 'trans_pass' in expected_editable_set:
            expected_editable_set.remove('trans_pass')

        for field in expected_editable_set:
            if field not in ('urgent', 'contract_sum'):
                if 'swiftcode' in field:
                    edit_rules[field] = 'BBRUCHGT'
                else:
                    edit_rules[field] = '22-06-2021'

        if 'contract_sum' in expected_editable_set:
            edit_rules['contract_sum'] = '300'
            edit_rules['oper_code'] = '12345'

        self.client.post(edit_url, edit_rules)
        editable_fields_after, _, _ = get_editable_fields_value(self.client.get(edit_url).content)

        for field_name in expected_editable_set:
            assert editable_fields[field_name] != editable_fields_after[field_name]

        if kz_fields:
            assert 'n_kbk' in uneditable_fields
            assert 'n_kod' not in uneditable_fields

        assert editable_fields_after['urgent'] == '1'

    @pytest.mark.parametrize(
        'associate, currency, editable_expected, non_editable_expected, is_selfemployed',
        [
            (Sber, Currency.RUB, 'expected_editable_base', 'expected_non_editable_rub', False),
            (SberSpb, Currency.USD, 'expected_editable_usd', 'expected_non_editable_usd', False),
            (Sber, Currency.USD, 'expected_editable_inter', 'expected_non_editable_inter', False),
            (Sber, Currency.RUB, 'expected_editable_base', 'expected_non_editable_selfemp', True),
            (AlfaKz, Currency.KZT, 'expected_editable_kzt', 'expected_non_editable_kzt', False),
            (Respublika, Currency.AZN, 'expected_editable_azn', 'expected_non_editable_azn', False),
        ])
    def test_check_editable_fields(
            self, associate, currency, editable_expected, non_editable_expected, is_selfemployed,
            get_assoc_acc_curr, get_source_payment, get_editable_fields_value,
    ):

        _, account, _ = get_assoc_acc_curr(
            associate.id, account={'number': '40702810800000007672', 'currency_code': Currency.by_num[currency]}
        )
        payment = get_source_payment(
            associate=associate,
            attrs={
                'f_inn': '37250679', 'f_acc': account.number, 'currency_id': currency,
                'payout_type': 1 if is_selfemployed else None
            }
        )
        edit_url = f'/associates/{associate.id}/payments/{payment.id}/edit/'

        result = self.client.get(edit_url)
        editable_fields, non_editable, buttons = get_editable_fields_value(result.content)

        assert len(buttons) == 3

        for btn in buttons:
            if 'Аннулировать' in btn:
                assert 'disabled' not in btn
            elif 'Отвергнуть' in btn:
                assert 'disabled' in btn

        assert set(editable_fields.keys()) == set(getattr(self, editable_expected))
        assert set(non_editable) == set(getattr(self, non_editable_expected))

    @pytest.mark.parametrize('associate', [Raiffeisen, Ing, VtbDe, AlfaKz, Unicredit])
    def test_payments_cancel_many(self, associate, get_source_payment, table_request):
        url = f'/associates/{associate.id}/payments/'
        payments = [get_source_payment(associate=associate) for _ in range(2)]
        assert table_request(
            url=url,
            realm='table-payments',
            action='cancel',
            items=[str(p.id) for p in payments],
            associate=associate
        )

    def test_payments_set_urgent(self, get_source_payment, table_request):
        """Пакетное проставление платежам признака срочности."""

        associate = Sber

        payment1 = get_source_payment(
            associate=associate)

        payment2 = get_source_payment(
            attrs=dict(status=states.BUNDLED),
            associate=Sber)

        assert not payment1.urgent
        assert not payment2.urgent

        url = f'/associates/{associate.id}/payments/'

        assert table_request(
            url=url,
            realm='table-payments',
            action='set_urgent',
            items=[payment1.id, payment2.id],
            associate=associate
        )

        payment1.refresh_from_db()
        payment2.refresh_from_db()

        assert payment1.urgent
        assert not payment2.urgent

    def test_payments_revise(self, get_source_payment, table_request):
        associate = Sber

        payment1 = get_source_payment(attrs={'status': states.NEW}, associate=associate)
        payment2 = get_source_payment(attrs={'status': states.OVER_LIMITS}, associate=associate)
        payment3 = get_source_payment(attrs={'status': states.OVER_LIMITS}, associate=associate)

        url = f'/associates/{associate.id}/payments/'

        def do_approve():
            return table_request(
                url=url,
                realm='table-payments',
                action='reviser_approve',
                items=[payment1.id, payment2.id],
                associate=associate,
            )

        def refresh ():
            for payment in (payment1, payment2, payment3):
                payment.refresh_from_db()

        # недостаток прав
        assert do_approve()
        payment2.refresh_from_db()
        assert payment2.status == states.OVER_LIMITS

        REVISERS.add('testuser')

        # утверждение платежа
        assert do_approve()
        refresh()
        assert payment1.is_new
        assert payment2.is_revised_ok
        assert 'по итогам ревизии' in payment2.processing_notes

        # отклонение платежа
        assert table_request(
            url=url,
            realm='table-payments',
            action='reviser_deny',
            items=[payment1.id, payment2.id, payment3.id],
            associate=associate,
        )

        refresh()
        assert payment1.is_new
        assert payment2.is_revised_ok
        assert payment3.status == states.USER_INVALIDATED
        assert 'по итогам ревизии' in payment3.processing_notes

        audit = list(AuditEntry.objects.all().order_by('id'))
        assert len(audit) == 2
        assert f'{payment2.id}' in audit[0].description
        assert audit[0].action == AuditEntry.ACTION_PAYMENT_REVISED
        assert f'{payment3.id}' in audit[1].description

    def test_payments_user_invalidate(self, get_source_payment, table_request):
        """Пакетное проставление платежам статуса Отказан казначейством."""

        associate = Sber

        payment = get_source_payment(
            attrs=dict(status=states.NEW),
            associate=associate)

        assert payment.status == states.NEW

        url = f'/associates/{associate.id}/payments/'

        assert table_request(
            url=url,
            realm='table-payments',
            action='invalidate',
            items=[payment.id],
            associate=associate,
            reason='test_reason',
        )

        payment.refresh_from_db()

        assert payment.status == states.USER_INVALIDATED
        assert payment.processing_notes == 'test_reason'

    def test_payments_operations(self, get_payment, check_client_response):

        url = f'/associates/{Raiffeisen.id}/payments/'

        payment = get_payment()

        assert check_client_response(url, {'action': 'create', 'items': f'["{payment.id}"]'}, method='post')

        assert check_client_response(url, {'action': 'download', 'items': f'["{get_payment().id}"]'}, method='post')
        assert check_client_response(url, {'action': 'download', 'items': f'["{get_payment().id};{get_payment().id}"]'}, method='post')
        assert check_client_response(url, {'action': 'cancel', 'items': f'["{get_payment().id}"]'}, method='post')
        assert check_client_response(url, {'action': 'cancel', 'items': f'["{get_payment().id};{get_payment().id}"]'}, method='post')
        assert check_client_response(url, {'action': 'set_reason', 'items': f'["{get_payment().id}"]'}, method='post')

    def test_create_bundle_tinkoff(self, get_payment, check_client_response):

        url = f'/associates/{Tinkoff.id}/payments/'

        payment = get_payment(associate=Tinkoff, acc_num='TECH40702810800000007671')
        payment.payout_type = 1
        payment.save()

        assert check_client_response(url, {'action': 'create', 'items': f'["{payment.id}"]'}, method='post')

        payment = get_payment(associate=Tinkoff)

        with pytest.raises(LogicError):

            check_client_response(url, {'action': 'create', 'items': f'["{payment.id}"]'}, method='post')

        payment.refresh_from_db()
        assert payment.is_new

    def test_account(self, get_assoc_acc_curr, check_client_response):
        _, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='40702810800000007671')

        assert check_client_response('/settings/accounts/')

        # создание счета
        assert check_client_response('/settings/accounts/', method='post')

        # редактирование счета
        assert check_client_response('/settings/accounts/', {'id': account.id}, method='post')

        # Неверно заполнен внешний ид.
        assert check_client_response(
            '/settings/accounts/',
            {'id': account.id, 'remote_id': 'inva lid id', 'save': '1'},
            method='post', check_content='Внешний ID указан неверно')

        assert account.currency_id is Currency.RUB
        assert check_client_response(
            '/settings/accounts/',
            {'id': account.id, 'number': account.number, 'save': 1, 'notify_balance_min': '', 'balance_hint': '',
             'comment': '',
             'notify_balance_to': '',
             'notify_payhints_to': '',
             'currency_id': Currency.USD, 'org': account.org.id,
             'associate_id': Raiffeisen.id, 'transit': 0, 'remote_id': ''}, method='post')

        account.refresh_from_db()

        assert AuditEntry.getone(
            description=f'№{account.number}'
        ).action == AuditEntry.ACTION_ACCOUNT_SAVE

        assert check_client_response(
            '/settings/accounts/',
            {'number': account.number, 'id': account.id, 'save': '1', 'notify_balance_min': '', 'balance_hint': '',
             'comment': '',
             'notify_balance_to': '',
             'notify_payhints_to': '',
             'currency_id': Currency.USD, 'org': account.org.id,
             'associate_id': Raiffeisen.id, 'transit': 0, 'remote_id': '', 'hidden': 1}, method='post')

        assert AuditEntry.getone(
            description=f'№{account.number}', action=AuditEntry.ACTION_ACCOUNT_HIDE
        ) is not None

        assert account.currency_id == Currency.USD

    def test_create_existing_account(self, check_client_response):
        org = Organization(id=1)
        org.save()
        account_edit_params = {
            'number': '100000000000', 'save': 1, 'notify_balance_min': 100, 'balance_hint': '',
            'comment': '',
            'notify_balance_to': 10000,
            'notify_payhints_to': 'a@a.com, b@b.com',
            'currency_id': 643, 'org': org.id,
            'associate_id': Raiffeisen.id, 'transit': 0, 'remote_id': ''
        }

        assert check_client_response('/settings/accounts/', account_edit_params, method='post')
        assert Account.getone(number=account_edit_params['number'])

        with pytest.raises(AccountCreateError) as e:
            check_client_response('/settings/accounts/', account_edit_params, method='post')

        assert 'Счёт уже существует' in e.value.msg

    def test_org_groups(self, mock_login, check_client_response):
        url = '/settings/groups/'

        assert check_client_response(url)
        assert not AuditEntry.objects.filter(action=AuditEntry.ACTION_ORGANIZATION_GROUP_SAVE)
        assert check_client_response(url, {'name': 'groupname'}, method='post')
        assert check_client_response(url, {'name': 'groupname', 'save': 1}, method='post')

        group = OrganizationGroup.objects.get(name='groupname')

        assert check_client_response(url)
        assert AuditEntry.objects.get(action=AuditEntry.ACTION_ORGANIZATION_GROUP_SAVE)
        assert check_client_response(url, {'name': 'grpedited', 'save': 1, 'id': group.id}, method='post')

        group.refresh_from_db()
        assert group.name == 'grpedited'

    def test_orgs(self, check_client_response):

        url = '/settings/orgs/'
        assert check_client_response(url)
        assert check_client_response(url, {'name': 'new_name'}, method='post')
        assert check_client_response(url, {'name': 'new_name', 'inn': '7704340327', 'save': 1}, method='post')

        org = Organization.getone(name='new_name')
        assert org

        params = {'name': 'Тестовая', 'inn': '7704340327', 'save': 1, 'id': org.id}

        assert check_client_response(url, params, method='post')
        org.refresh_from_db()
        assert org.name == 'Тестовая'

        params['hidden'] = ['on']

        assert check_client_response(url, params, method='post')

        # проверяем выставление скрытия через интерфейс
        org.refresh_from_db()
        assert org.hidden == True

        # проверяем скрытие организации в интерфейсе
        assert check_client_response(url, check_content='!Тестовая')

    def test_signing(self, init_user, make_org, time_freeze, check_client_response):

        org = make_org(name='myorg')
        url = '/settings/signing/'

        assert check_client_response(url)
        user = init_user('user')
        serial_number = '132344A'
        assert check_client_response(
            url,
            {
                'associate_id': Unicredit.id, 'executive': 'test',  'user': user.id, 'serial_number': serial_number,
                'level': '1', 'dss_login': '', 'dss_secret': '', 'id': '', 'save': '1', 'org': org.id,
            },
            method='post'
        )
        right = SigningRight.objects.get(serial_number=serial_number.lower())

        assert check_client_response(url, check_content='<td>myorg</td>')

        # проверим вывод дат и подсветку истечения
        right.dt_begins = datetime(2021, 7, 24, 0, 0, 0)
        right.dt_expires = datetime(2021, 7, 25, 0, 0, 0)
        right.save()

        # не подсвечиваем истечение, если до него более 15 дней.
        with time_freeze('2021-07-10'):
            assert check_client_response(url, check_content=[
                '<td nowrap>24-07-2021 00:00</td>',
                '<td nowrap >25-07-2021 00:00</td>',
            ])

        # подсвечиваем истечение.
        with time_freeze('2021-07-11'):
            assert check_client_response(url, check_content=[
                '<td nowrap class="bg-red">25-07-2021 00:00</td>',
            ])

    def test_reports(self, get_assoc_acc_curr, check_client_response, init_user):
        associate = Sber
        user = init_user()

        assert check_client_response('/reports/summary/')

        # Отчёт по остаткам.
        _, account, _ = get_assoc_acc_curr(associate)

        register = StatementRegister(
            statement_date='2021-11-07',
            associate_id=associate.id,
            account=account,
            statement=Statement.objects.create(
                associate_id=associate.id,
                user=user,
            ),
        )
        register.save()

        account.turnover = 'null'
        account.register_final = register

        account.save()

        assert check_client_response('/reports/balance/')

    def test_refs(self, check_client_response, response_mock):
        assert check_client_response('/refs/swift/')

        with response_mock([
            'POST https://refs-test.paysys.yandex.net/api/swift -> 200:'
            '{"data":{"__type":{"description":"Event","fields":[{"name":"id","description":"Event key"},'
            '{"name":"bic8","description":"BIC 8"},{"name":"bicBranch","description":"BIC Branch"},'
            '{"name":"status","description":"Status"},{"name":"active","description":"Active"}'
            '],"enumValues":null}}}',

            'POST https://refs-test.paysys.yandex.net/api/swift -> 200:'
            '{"data": {"bics": ['
            '{"bic8": "AGCAAM22", "bicBranch": "XXX", '
            '"addrOpRegion": "aoreg", "addrOpStreet": "aostr", "addrOpStreetNumber": "aostrn", '
            '"addrOpBuilding": "aobld", "addrOpCity": "aocit", "instName": "inst", "countryCode": "DE"'
            '}'
            ']}}'
        ]):
            assert check_client_response(
                '/refs/swift/', {'bic': 'AGCAAM22'}, method='post',
                extra_headers={'HTTP_X_REQUESTED_WITH': 'XMLHttpRequest'},
                check_content='BIC Branch',
            )

        assert check_client_response('/refs/banks/')
        with response_mock(
            'POST https://refs-test.paysys.yandex.net/api/cbrf -> 200:'
            '{"data":{"banks":[{"bic":"045004641","swift":"SABRRUMMNH1","nameFull":"AAA",'
            '"corr":"30101810500000000641","zip":"630007","place":"here","address":"there, 20",'
            '"regnum":"1481/982","type":"aa"}]}}'
        ):
            assert check_client_response(
                '/refs/banks/', {'bic': '045004641'}, method='post',
                extra_headers={'HTTP_X_REQUESTED_WITH': 'XMLHttpRequest'},
                check_content='Кор. счёт',
            )

    def test_statement_filtering(self, get_proved, django_assert_num_queries):
        associate = Sber
        proved1 = get_proved(associate=associate, acc_num='1234')[0]
        get_proved(associate=associate, acc_num='1234')
        get_proved(associate=associate, acc_num='6789')

        url = f'/associates/{associate.id}/statements/'

        with django_assert_num_queries(7) as _:
            content = self.client.get(url).content.decode()
            assert '1234</td>' in content
            assert '6789</td>' in content

        with django_assert_num_queries(7) as _:
            content = self.client.get(f'{url}?plimit=1&p=2&acc={proved1.register.account_id}').content.decode()
            assert '1234</td>' in content
            assert '6789</td>' not in content

    def test_statements(self, get_statement, django_assert_num_queries, check_client_response):
        associate = Raiffeisen

        url = f'/associates/{associate.id}/statements/'

        statement = get_statement('', associate)
        statement.set_status(states.ERROR)
        statement.save()
        statement2 = get_statement('', associate)  # вторая страница
        statement3 = get_statement('', associate)  # первая страница

        with django_assert_num_queries(7) as _:
            assert check_client_response(url, check_content=[
                '!pagination',
                f'tr id="{statement.id}"',
            ])

        with django_assert_num_queries(7) as _:
            assert check_client_response(f'{url}?plimit=1&p=2', check_content=[
                'pagination',
                f'tr id="{statement2.id}"',
            ])

        assert check_client_response(url, {'action': 'reprocess', 'items': f'["{statement.id}"]'}, method='post')

        assert AuditEntry.getone(
            description=f'№{statement.number}'
        ).action == AuditEntry.ACTION_STATEMENT_REPROCESS

        assert check_client_response(url, {'action': 'delete', 'items': f'["{statement.id}"]'}, method='post')

        assert AuditEntry.getone(
            description=f'№{statement.number}', action=AuditEntry.ACTION_STATEMENT_REMOVE
        ) is not None

    def test_fake_statements(self, fake_statements, get_assoc_acc_curr, init_user, check_client_response):
        init_user(robot=True)
        associate = Raiffeisen

        url = f'/associates/{associate.id}/statements/'

        _, acc, _ = get_assoc_acc_curr(associate.id, account={'number': '12345555', 'fake_statement': True})

        check_client_response(url, {'fake_statement': True, 'on_date': '20-09-2020', 'fake_acc': acc.id}, method='post')

    def test_batch_upload_statement(self, get_assoc_acc_curr, read_fixture_from_dir, check_client_response):
        """Подробнее в BCL-730"""
        get_assoc_acc_curr(Ing.id, account='40702810300001005386')
        get_assoc_acc_curr(Ing.id, account='40702810300001003838')

        url = f'/associates/{Ing.id}/statements/'

        check_client_response(
            url, files=[
                BytesIO(read_fixture_from_dir('banks/fixtures/ing_final.txt', root_path=False)),
                BytesIO(read_fixture_from_dir('banks/fixtures/ing_charge_payment.txt', root_path=False)),
                BytesIO(read_fixture_from_dir('banks/fixtures/ing_ru_many.txt', root_path=False))
            ], method='post')

        all_statements = Statement.objects.all().order_by('id')
        assert len(all_statements) == 7

        process_statements(None)
        statement_ids = []

        for idx, statement in enumerate(all_statements, 1):
            statement.refresh_from_db()
            statement_ids.append(str(statement.id))

            if idx < 3:
                assert statement.status == states.STATEMENT_PROCESSED

            else:
                # Не заведены счёта, но до начала обработки мы об этом не знаем.
                assert statement.status == states.STATEMENT_DELAYED

        assert check_client_response(
            url, {'action': 'delete', 'items': json.dumps(statement_ids)},
            method='post')

        all_statements = Statement.objects.all().order_by('id')
        assert len(all_statements) == 0

    def test_bundles(
        self, get_payment_bundle, get_payment, dss_signing_right, make_org,
        django_assert_num_queries, time_shift, check_client_response):

        associate = Raiffeisen

        url = f'/associates/{associate.id}/bundles/'

        with time_shift(60 * 60 * 24 * 2):  # стабилизация теста
            bundle = get_payment_bundle([get_payment()])
            bundle2 = get_payment_bundle([get_payment()])
            bundle3 = get_payment_bundle([get_payment()])

        dss_signing_right(associate=associate, autosigning=True)
        dss_signing_right(associate=associate, org=make_org(name='dummy'))

        with django_assert_num_queries(11) as _:
            assert check_client_response(url, check_content=[
                '!pagination',
                f'tr id="{bundle.id}"',
                'Доступные права подписи на DSS',
                'да</li>',
                'нет, орг.: dummy</li>',
            ])

        with django_assert_num_queries(11) as _:
            assert check_client_response(f'{url}?plimit=1&p=2', check_content=[
                'pagination',
                f'tr id="{bundle2.id}"',
            ])

        assert check_client_response(url, {'action': 'cancel', 'items': f'["{bundle.id}"]'}, method='post')
        assert AuditEntry.getone(description=f'№{bundle.number}').action == AuditEntry.ACTION_BUNDLE_REMOVE

        bundle = get_payment_bundle([get_payment()])
        assert check_client_response(f'{url}automate/', {'bundles': f'["{bundle.id}"]'}, method='post')

    def test_salary(self, check_client_response):
        assert check_client_response(f'/associates/{Sber.id}/salary/')

    def test_salary_listing(self, get_salary_registry, django_assert_num_queries):

        registry_object = get_salary_registry(
            Sber, SberbankSalaryRegistry,
            reg_id='6c568c1f-9b07-0793-e055-000000000075',
            registry_number='18351',
            employees=[]
        )

        registry_object.save()

        with django_assert_num_queries(7) as _:
            self.client.get(f'/associates/{Sber.id}/salary/', {})

    def test_salary_restriced_org(self, check_client_response, get_salary_registry, make_org, init_user):
        make_org(name='test')
        org = make_org(name='test_restricted', under_restrictions=True)
        registry_object = get_salary_registry(
            Sber, SberbankSalaryRegistry,
            reg_id='6c568c1f-9b07-0793-e055-000000000075',
            registry_number='18351',
            org=org,
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'currency_code': 'RUB',
                    'personal_account': '66666666666666666666',
                    'amount': '1012.00'
                }]
        )

        registry_object.save()
        result = self.client.get(f'/associates/{Sber.id}/salary/', {})
        assert str(registry_object.registry_number) not in result.content.decode('utf-8')

        user = init_user()
        user.restrictions[Role.SALARY_GROUP] = {'org': [org.id]}
        user.save()

        result = self.client.get(f'/associates/{Sber.id}/salary/', {})
        assert str(registry_object.registry_number) in result.content.decode('utf-8')

        user.restrictions[Role.SALARY_GROUP] = {'org': []}
        user.save()

    def test_sber_registry(self, get_salary_registry, check_client_response):
        registry = get_salary_registry(Sber, SberbankCardRegistry, reg_id='485b7af9-3832-4a6f-a02b-264c5056a4b2')

        assert check_client_response(
            f'/associates/{Sber.id}/salary/',
            {'action': 'download', 'associate_id': Sber.id, 'items': json.dumps([str(registry.id)])},
            method='post'
        )

        assert check_client_response(f'/associates/{Sber.id}/salary/{registry.id}/reject/')

        registry.refresh_from_db()
        assert registry.status_to_oebs() == 2

    def test_salary_xml(self, read_fixture_from_dir, get_salary_registry, check_client_response):

        registry_objects = [
            get_salary_registry(
                Sber, SberbankSalaryRegistry, reg_id='81b5e002-8db8-4f09-e053-036fa8c0b934', registry_number='101',
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
            get_salary_registry(
                Sber, SberbankSalaryRegistry, reg_id='81c2cafa-2254-04db-e053-036fa8c05c35', registry_number='146',
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
            get_salary_registry(
                Sber, SberbankSalaryRegistry, reg_id='81c2cafa-2255-04db-e053-036fa8c05c35', registry_number='159',
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
        ]

        for registry in registry_objects:
            registry.save()

        body = BytesIO(read_fixture_from_dir('banks/party_sber/fixtures/sber_incoming_xml.xml', root_path=False))

        assert check_client_response(
            f'/associates/{Sber.id}/salary/', {'upload': ''}, files=body, file_key='registry_answer', method='post')

        for registry in registry_objects:
            registry.refresh_from_db()
            assert registry.status == states.REGISTER_ANSWER_LOADED

    def test_salary_zip(self, read_fixture_from_dir, get_salary_registry, check_client_response):
        registry_object = get_salary_registry(
            Sber, SberbankSalaryRegistry,
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
                    'amount': '1012.00'
                }]
        )

        registry_object.save()

        body = BytesIO(read_fixture_from_dir('banks/party_sber/fixtures/ao38140753_160518_195303.zip', root_path=False))

        assert check_client_response(
            f'/associates/{Sber.id}/salary/', {'upload': ''}, files=body, file_key='registry_answer', method='post')

        registry_object.refresh_from_db()
        assert registry_object.status == states.REGISTER_ANSWER_LOADED

    def test_salary_contract(self, get_salary_contract, check_client_response):
        contract = get_salary_contract(Sber)
        self.create_and_edit_salary_contract(contract.org, contract, check_client_response)


class TestAccountantRole(ViewsTestSuite):

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user, mocker):
        init_user(roles=[Role.ACCOUNTANT])
        mocker.patch('bcl.banks.party_raiff.Raiffeisen.automate_payments', lambda *_: True)

    def test_payments_views(self, get_payment, get_assoc_acc_curr, check_client_response, table_request):
        url = f'/associates/{Raiffeisen.id}/payments/'
        assert check_client_response(url)

        payment = get_payment()

        assert table_request(
            url=url,
            realm='table-payments',
            action='invalidate',
            items=[payment.id],
            associate=payment.associate,
            reason='test_reason',
        )

        payment.refresh_from_db()
        assert payment.status != states.USER_INVALIDATED

        assert self.restricted(
            url, {'action': 'download', 'associate_id': Raiffeisen.id, 'items': json.dumps([str(payment.id)])},
            method='post')

        edit_url = f'{url}{payment.id}/edit/'
        assert check_client_response(edit_url)
        assert self.restricted(edit_url, {'ground': 'ground'}, method='post')

        payment_db = Payment.getone(pk__in=[payment.id])
        assert payment_db.ground != 'ground'

        assert self.restricted(f'{url}{get_payment().id}/edit/', params={'action': 'cancel'}, method='post')
        assert self.restricted(f'{url}{get_payment().id}/edit/', params={'action': 'revoke'}, method='post')

        _, account, _ = get_assoc_acc_curr(Raiffeisen.id)

        params = {'associate_id':  Raiffeisen.id, 'org': account.org.id, 'account': account.id}
        assert check_client_response('/show_accounts_balance', params, method='post')

    def test_statements(self, read_fixture_from_dir, get_assoc_acc_curr, check_client_response):
        associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001005386')
        body = BytesIO(read_fixture_from_dir('banks/fixtures/ing_final.txt', root_path=False))
        url = f'/associates/{Ing.id}/statements/'

        check_client_response(url, files=body, method='post')
        statement = Statement.getone()
        process_statements(None)
        statement.refresh_from_db()

        assert statement.is_processing_done

        assert check_client_response(url)
        assert check_client_response(url, {'action': 'delete', 'items': f'["{statement.id}"]'}, method='post')

        associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001003838')
        body = BytesIO(read_fixture_from_dir('banks/fixtures/ing_ru_mt942-intraday.txt', root_path=False))

        url = f'/associates/{Ing.id}/statements/'

        check_client_response(url, files=body, method='post')
        statement = Statement.getone(status=Statement.state_processing_ready)
        process_statements(None)
        statement.refresh_from_db()

        assert statement.is_processing_failed
        assert 'Недостаточно прав' in statement.processing_notes

        assert check_client_response(url)
        assert check_client_response(url, {'action': 'delete', 'items': f'["{statement.id}"]'}, method='post')

    def test_orgs(self):
        result = self.client.get('/settings/orgs/', {})
        assert 'Добавить организацию' not in result.content.decode('utf-8')

    def test_salary_contract(self):
        result = self.client.get('/settings/salary/', {})
        assert 'Добавить договор' not in result.content.decode('utf-8')


class TestTreasurerRole(ViewsTestSuite):

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user, mocker):
        init_user(roles=[Role.TREASURER])
        mocker.patch('bcl.banks.party_raiff.Raiffeisen.automate_payments', lambda *_: True)

    def test_payments_views(self, get_payment, get_assoc_acc_curr, check_client_response, table_request):
        url = f'/associates/{Raiffeisen.id}/payments/'
        assert check_client_response(url)

        payment = get_payment()

        assert table_request(
            url=url,
            realm='table-payments',
            action='invalidate',
            items=[payment.id],
            associate=payment.associate,
            reason='test_reason',
        )

        payment.refresh_from_db()
        assert payment.status == states.USER_INVALIDATED

        payment = get_payment()
        edit_url = f'{url}{payment.id}/edit/'
        assert check_client_response(edit_url)
        assert check_client_response(edit_url, {'ground': 'ground', 'payedit': '1'}, method='post')

        payment_db = Payment.getone(pk__in=[payment.id])
        assert payment_db.ground == 'ground'

        assert check_client_response(f'{url}{get_payment().id}/edit/', params={'action': 'cancel'}, method='post')
        assert check_client_response(f'{url}{get_payment().id}/edit/', params={'action': 'revoke'}, method='post')

        _, account, _ = get_assoc_acc_curr(Raiffeisen.id)

        params = {'associate_id':  Raiffeisen.id, 'org': account.org.id, 'account': account.id}
        assert check_client_response('/show_accounts_balance', params, method='post')

    def test_statements(self, read_fixture_from_dir, get_assoc_acc_curr, check_client_response):
        associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001005386')
        body = BytesIO(read_fixture_from_dir('banks/fixtures/ing_final.txt', root_path=False))

        url = f'/associates/{Ing.id}/statements/'

        check_client_response(url, files=body, method='post')
        statement = Statement.getone()
        process_statements(None)
        statement.refresh_from_db()

        assert statement.is_processing_failed
        assert 'Недостаточно прав' in statement.processing_notes

        assert check_client_response(url)
        assert self.restricted(url, {'action': 'reprocess', 'items': f'["{statement.id}"]'}, method='post')
        assert self.restricted(url, {'action': 'delete', 'items': f'["{statement.id}"]'}, method='post')

        associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001003838')
        body = BytesIO(read_fixture_from_dir('banks/fixtures/ing_ru_mt942-intraday.txt', root_path=False))

        check_client_response(url, files=body, method='post')
        statement = Statement.getone(status=Statement.state_processing_ready)
        process_statements(None)
        statement.refresh_from_db()

        assert statement.status == Statement.state_processing_done

        assert check_client_response(url)
        assert self.restricted(url, {'action': 'delete', 'items': f'["{statement.id}"]'}, method='post')

    def test_orgs(self):
        result = self.client.get('/settings/orgs/', {})
        assert 'Добавить организацию' in result.content.decode('utf-8')

    def test_salary_contract(self, get_salary_contract, check_client_response):
        contract = get_salary_contract(Sber)
        self.create_and_edit_salary_contract(contract.org, contract, check_client_response)

    def test_sber_registry(self, get_salary_registry):
        registry = get_salary_registry(Sber, SberbankCardRegistry, reg_id='485b7af9-3832-4a6f-a02b-264c5056a4b2')

        assert self.restricted(
            f'/associates/{Sber.id}/salary/',
            {'action': 'download', 'associate_id': Sber.id, 'items': json.dumps([str(registry.id)])},
            method='post'
        )


class TestBenefitRole(ViewsTestSuite):

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user, mocker):
        init_user(roles=[Role.BENEFITS_GROUP])

    def test_sber_registry(self, get_salary_registry, check_client_response):
        registry = get_salary_registry(Sber, SberbankSalaryRegistry, reg_id='485b9af9-3832-4a6f-a02b-264c5056a4b2')

        assert check_client_response(
            f'/associates/{Sber.id}/salary/',
            {'action': 'download', 'associate_id': Sber.id, 'items': json.dumps([str(registry.id)]),
             'realm': 'table-card-registries'},
            method='post'
        )

    def test_sber_registry_restricted(self, get_salary_registry):
        registry = get_salary_registry(Sber, SberbankCardRegistry, reg_id='485b7af9-3832-4a6f-a02b-264c5056a4b2')

        assert self.restricted(
            f'/associates/{Sber.id}/salary/',
            {'action': 'download', 'associate_id': Sber.id, 'items': json.dumps([str(registry.id)]),
             'realm': 'table-salary-registries'},
            method='post'
        )


class TestSuperTreasurerRole(ViewsTestSuite):

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user, mocker):
        init_user(roles=[Role.SUPER_TREASURER])
        mocker.patch('bcl.banks.party_raiff.Raiffeisen.automate_payments', lambda *_: True)

    def test_payments_views(self, get_payment, get_assoc_acc_curr, check_client_response):
        url = f'/associates/{Raiffeisen.id}/payments/'
        assert check_client_response(url)
        payment = get_payment()

        edit_url = f'{url}{payment.id}/edit/'
        assert check_client_response(edit_url)
        assert check_client_response(edit_url, {'ground': 'ground', 'payedit': '1'}, method='post')

        payment_db = Payment.getone(pk__in=[payment.id])
        assert payment_db.ground == 'ground'

        assert check_client_response(f'{url}{get_payment().id}/edit/', params={'action': 'cancel'}, method='post')
        assert check_client_response(f'{url}{get_payment().id}/edit/', params={'action': 'revoke'}, method='post')

        _, account, _ = get_assoc_acc_curr(Raiffeisen.id)

        params = {'associate_id': Raiffeisen.id, 'org': account.org.id, 'account': account.id}
        assert check_client_response('/show_accounts_balance', params, method='post')

    def test_statements(self, get_statement, init_user, check_client_response):
        init_user(robot=True)
        url = f'/associates/{Raiffeisen.id}/statements/'

        statement = get_statement('', Raiffeisen)
        statement.status = states.ERROR
        statement.save()

        assert check_client_response(url)
        assert self.restricted(url, {'action': 'reprocess', 'items': f'["{statement.id}"]'}, method='post')

        statement = get_statement('', Ing, final=False)
        statement.status = states.ERROR
        statement.save()

        assert check_client_response(url)
        assert check_client_response(url, {'action': 'reprocess', 'items': f'["{statement.id}"]'}, method='post')

    def test_orgs(self):
        result = self.client.get('/settings/orgs/', {})
        assert 'Добавить организацию' in result.content.decode('utf-8')

    def test_salary_contract(self, get_salary_contract, check_client_response):
        contract = get_salary_contract(Sber)
        self.create_and_edit_salary_contract(contract.org, contract, check_client_response)


class TestSalaryGroup(ViewsTestSuite):

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user, mocker):
        init_user(roles=[Role.ACCOUNTANT])
        mocker.patch('bcl.banks.party_raiff.Raiffeisen.automate_payments', lambda *_: True)

    @pytest.mark.parametrize(
        'registry_type',
        [
            SberbankCardRegistry,
            SberbankSalaryRegistry
        ])
    def test_sber_registry(self, registry_type, get_salary_registry):
        registry = get_salary_registry(Sber, registry_type, reg_id='485b7af9-3832-4a6f-a02b-264c5056a4b2')

        assert self.restricted(
            f'/associates/{Sber.id}/salary/',
            {'action': 'download', 'associate_id': Sber.id, 'items': json.dumps([str(registry.id)])},
            method='post'
        )

        assert self.restricted(f'/associates/{Sber.id}/salary/{registry.id}/reject/')

    def test_salary(self, read_fixture_from_dir, get_salary_registry):

        registry_objects = [
            get_salary_registry(Sber, SberbankSalaryRegistry, reg_id='485b7af3-3832-4a6f-a02b-264c5056a4b2'),
            get_salary_registry(Sber, SberbankSalaryRegistry, reg_id='32352d01-5dd3-4a49-8cdb-d2fc46921f61'),
        ]

        for registry in registry_objects:
            registry.save()

        body = BytesIO(read_fixture_from_dir('banks/party_sber/fixtures/sber_incoming_salary.zip', root_path=False))

        assert self.restricted(
            f'/associates/{Sber.id}/salary/', {'upload': ''}, body, file_key='registry_answer', method='post')

        for registry in registry_objects:
            registry.refresh_from_db()
            assert registry.status == states.NEW

    def test_card(self, read_fixture_from_dir, get_salary_registry):
        registry = get_salary_registry(Sber, SberbankCardRegistry, reg_id='6CB6B472-77F4-6DC0-E055-000000000075')

        body = BytesIO(read_fixture_from_dir('banks/party_sber/fixtures/sber_incoming_card.zip', root_path=False))

        assert self.restricted(
            f'/associates/{Sber.id}/salary/', {'upload': ''}, body, file_key='registry_answer', method='post')

        registry.refresh_from_db()
        assert registry.status == states.NEW


class TestSigning(TestViewsStaleApi):

    @pytest.fixture
    def try_signing(self, build_payment_bundle, dss_signing_right, check_client_response):

        def try_signing_(associate, sign_method):
            associate_id = associate.id

            bundle = build_payment_bundle(associate, payment_dicts=[{}, {}], account='12345678901234567890', h2h=True)

            def send():
                return self.bundle_get_contents(bundle=bundle, dss=True, sign_method=sign_method)

            with pytest.raises(UserHandledException):
                send()

            dss_signing_right(associate=associate, org=bundle.org)

            response = send()
            assert b'dss_code' in response.content
            assert response.status_code == 200

            intent = list(Intent.objects.all())
            assert len(intent) == 1
            intent = intent[0]

            check_client_response(f'/associates/{associate_id}/bundles/', {'dss_code': intent.code})

        return try_signing_

    @pytest.mark.skip(reason='Долгий запрос к возможно недоступному DSS.')
    def test_signing_views_cms(self, try_signing):
        try_signing(Raiffeisen, DetachedMultipleSignature.alias)

    @pytest.mark.skip(reason='Долгий запрос к возможно недоступному DSS.')
    def test_signing_views_xml(self, try_signing):
        try_signing(Unicredit, XmlSignature.alias)


def test_payments_default_filter(get_payment_bundle, get_source_payment, init_user, get_assoc_acc_curr):
    user = init_user('user')
    associate = Sber
    _, acc, _ = get_assoc_acc_curr(associate, account='40702810800000007671')

    payment = get_source_payment(associate=associate, set_account=True)
    excluded_payment = get_source_payment(
        attrs=dict(
            status=states.NEW,
            user=user,
            date=DateUtils.days_from_now(6),
            f_acc=acc.number,
            account_id=acc.id,
        ),
        associate=associate, set_account=True
    )

    bundle = get_payment_bundle([payment, excluded_payment])

    url = f'/associates/{associate.id}/payments/'
    client = Client()

    result = client.get(url).content.decode()
    assert f'id="{payment.id}"' in result
    assert f'id="{excluded_payment.id}"' not in result
    assert 'pagination' not in result

    result = client.get(url, {'bundle_number': bundle.number}).content.decode('utf-8')
    assert f'id="{payment.id}"' in result
    assert f'id="{excluded_payment.id}"' in result

    # Проверяем наличие постраничной навигации.
    payment2 = get_source_payment(associate=associate, set_account=True)  # вторую страницу
    payment3 = get_source_payment(associate=associate, set_account=True)  # на первую страницу
    result = client.get(f'{url}?plimit=1&p=2').content.decode()
    assert 'pagination' in result
    assert f'id="{payment2.id}"' in result


def _check_cancel_invalidate(response, role, cancellation_allowed, invalidation_allowed):
    context = response.context
    if cancellation_allowed and role in cancellation_allowed:
        assert context['allow_cancellation']
    else:
        assert not context['allow_cancellation']
    if invalidation_allowed and role in invalidation_allowed:
        assert context['allow_invalidation']
    else:
        assert not context['allow_invalidation']


def test_payments_cancel_invalidate_decline(
    get_source_payment, init_user, mock_yauth_user, check_client_response
):
    client = Client()
    associate = Sber
    url = f'/associates/{associate.id}/payments/'
    su_only = frozenset({Role.SUPPORT, Role.SUPER_TREASURER})
    allowed_roles = su_only | {Role.TREASURER}
    creator = init_user()

    def _check_payments(
        *payments,
        cancellation_allowed=allowed_roles,
        invalidation_allowed=allowed_roles,
        bank_decline_allowed=None
    ):
        for role in Role.available:
            user = init_user(role, roles=[role])
            mock_yauth_user(user.username)
            response = client.get(url, data={
                'user': [user.id, creator.id],
                'status': states.get_titles(states.STATES_PAYMENTS)
            })
            result = response.content.decode()
            if role in {Role.BENEFITS_GROUP}:
                assert result == ''
            else:
                for p in payments:
                    assert f'id="{p.id}"' in result
                    decline_allowed = (bank_decline_allowed and bank_decline_allowed.get(p.id)) or set()
                    if role in decline_allowed:
                        assert f'data-id="{p.id}" data-action="set_reason"' in result
                    else:
                        assert f'data-id="{p.id}" data-action="set_reason"' not in result
                _check_cancel_invalidate(response, role, cancellation_allowed, invalidation_allowed)

    # единственный НОВЫЙ платеж могут аннулировать (СУПЕР)КАЗНАЧЕЙ или САППОРТ
    # но не другие роли
    payment1 = get_source_payment(associate=associate)
    _check_payments(payment1)
    # несколько НОВЫХ платежей ведут себя так же
    payment2 = get_source_payment(associate=associate)
    _check_payments(payment1, payment2)
    # несколько платежей, среди которых есть хоть 1 новый, ведут себя так же
    payment3 = get_source_payment(associate=associate)
    payment3.status = states.ERROR
    payment3.save()
    _check_payments(payment1, payment2, payment3)
    # НЕ-новые платежи не могут быть аннулированы никем
    payment1.status = states.CANCELLED
    payment2.status = states.USER_INVALIDATED
    payment1.save()
    payment2.save()
    _check_payments(
        payment1, payment2, payment3,
        cancellation_allowed=frozenset(),
        invalidation_allowed=frozenset()
    )
    # проверяем доступность возможности отклонять-как-банк
    payment_h2h = get_source_payment(associate=associate)
    payment_h2h.status = states.EXPORTED_H2H
    payment_h2h.save()
    payment_online = get_source_payment(associate=associate)
    payment_online.status = states.EXPORTED_ONLINE
    payment_online.save()
    payment_declined = get_source_payment(associate=associate)
    payment_declined.status = states.DECLINED_BY_BANK
    payment_declined.save()
    payment_invalidated = get_source_payment(associate=associate)
    payment_invalidated.status = states.BCL_INVALIDATED
    payment_invalidated.save()
    payment_processing = get_source_payment(associate=associate)
    payment_processing.status = states.PROCESSING
    payment_processing.save()
    _check_payments(
        payment1, payment2, payment3,
        payment_h2h, payment_online, payment_invalidated,
        payment_processing,
        cancellation_allowed=frozenset(),
        invalidation_allowed=frozenset(),
        bank_decline_allowed={
            payment_h2h.id: allowed_roles,
            payment_online.id: allowed_roles,
            payment_invalidated.id: allowed_roles,
            payment_processing.id: su_only
        }
    )


def test_payments_group_cancel_invalidate(get_source_payment, init_user, mock_yauth_user):
    client = Client()
    associate = Sber
    url = f'/associates/{associate.id}/payments/'
    allowed_roles = frozenset({Role.SUPPORT, Role.TREASURER, Role.SUPER_TREASURER})
    creator = init_user()

    def _check_payments(
        *payments,
        cancellation_allowed=allowed_roles,
        invalidation_allowed=allowed_roles,
    ):
        for role in Role.available:
            user = init_user(role, roles=[role])
            mock_yauth_user(user.username)
            response = client.get(url, data={
                'user': [user.id, creator.id],
                'status': states.get_titles(states.STATES_PAYMENTS),
                'group_mode': 1
            })
            result = response.content.decode()
            if role in {Role.BENEFITS_GROUP}:
                assert result == ''
            else:
                assert f'id="{";".join(str(p.id) for p in payments)}"' in result
                _check_cancel_invalidate(response, role, cancellation_allowed, invalidation_allowed)

    # единственный НОВЫЙ платеж могут аннулировать (СУПЕР)КАЗНАЧЕЙ или САППОРТ
    # но не другие роли
    payment1 = get_source_payment(associate=associate)
    _check_payments(payment1)
    # несколько НОВЫХ платежей ведут себя так же
    payment2 = get_source_payment(associate=associate)
    _check_payments(payment1, payment2)
    # несколько платежей, среди которых есть хоть 1 НЕ-новый,
    # нельзя аннулировать в групповом режиме
    payment3 = get_source_payment(associate=associate)
    payment3.status = states.ERROR
    payment3.save()
    _check_payments(
        payment1, payment2, payment3,
        cancellation_allowed=frozenset(),
        invalidation_allowed=frozenset()
    )


def test_payment_edit_cancel_invalidate(
    get_source_payment, get_assoc_acc_curr, init_user, mock_yauth_user
):
    client = Client()
    associate = Sber
    allowed_roles = frozenset({Role.SUPPORT, Role.TREASURER, Role.SUPER_TREASURER})

    def _check_payment(
        payment,
        cancellation_allowed=allowed_roles,
        invalidation_allowed=allowed_roles,
    ):
        url = f'/associates/{associate.id}/payments/{payment.id}/edit/'
        for role in Role.available:
            user = init_user(role, roles=[role])
            mock_yauth_user(user.username)
            response = client.get(url)
            result = response.content.decode()
            if role in {Role.BENEFITS_GROUP}:
                assert result == ''
            else:
                _check_cancel_invalidate(response, role, cancellation_allowed, invalidation_allowed)

    # НОВЫЙ платеж могут аннулировать (СУПЕР)КАЗНАЧЕЙ или САППОРТ
    # но не другие роли
    _, acc, _ = get_assoc_acc_curr(associate)
    payment1 = get_source_payment(dict(
        f_acc=acc.number,
        f_iban='0000000000000001',
        f_inn=acc.org.inn,
        f_kpp='123456789',
        f_name=acc.org.name,
        f_bic='044525700',
        f_swiftcode='OWHBDEFF',
        f_cacc='30101810200000000700',
        f_bankname='АО БАНК1',
    ), associate=associate)
    _check_payment(payment1)
    # НЕ-новый платеж нельзя аннулировать
    payment1.status = states.ERROR
    payment1.save()
    _check_payment(
        payment1,
        cancellation_allowed=frozenset(),
        invalidation_allowed=frozenset()
    )


def test_payments_group_mode(
    get_payment_bundle, get_source_payment_mass, init_user, django_assert_num_queries, mock_yauth_user
):
    associate = Sber
    get_source_payment_mass(5, associate=associate, set_account=True)

    url = f'/associates/{associate.id}/payments/?group_mode=1'
    client = Client()
    with django_assert_num_queries(8) as _:
        result = client.get(url).content.decode()
    assert '5</td>' in result
    assert 'nowrap>760,00' in result


@pytest.mark.xfail(reason='Вероятно DSS флюктуация')
@pytest.mark.parametrize(
    "associate, sign_type, serial, sign_fixture",
    [
        (Unicredit, XmlSignature, '12000d3ece1cbf936c80c766fb0000000d3ece', 'signatures/signature.xml'),
        (Raiffeisen, DetachedMultipleSignature, '216f0a8c000100001380', 'signatures/detached_signature_base64.txt')
    ]
)
def test_dss_sign_by_intent_code(
    get_payment_bundle, get_source_payment, mock_signer, read_fixture,
    get_signing_right, associate, sign_type, serial, sign_fixture, monkeypatch, init_user, make_org,
    get_assoc_acc_curr
):

    raw_signature = read_fixture(sign_fixture)

    user = init_user()
    _, account, _ = get_assoc_acc_curr(associate=associate)

    mock_signer(base64.b64encode(raw_signature) if sign_type is XmlSignature else raw_signature.decode(), serial)
    get_signing_right(associate.id, serial, org=account.org)
    data_template = '{"associate_id":"%s", "bundles": ["%s"]}'
    bundle = get_payment_bundle(
        [get_source_payment(associate=associate)], associate=associate, h2h=True, account=account)

    if sign_type is DetachedMultipleSignature:
        bundle.digests = ['']
        bundle.save()
        bundle.refresh_from_db()

        raw_signature = [raw_signature.decode()]
    else:
        raw_signature = raw_signature.decode()

    uuid = str(uuid4())

    Intent(
        realm=Intent.REALM_DSS_SIGNING,
        code=uuid,
        expire_after=datetime(9999, 12, 31),
        data_raw=data_template % (associate.id, bundle.id)
    ).save()

    def dss_sign_fail(*args, **kwargs):
        from dssclient.exceptions import DssClientException
        raise DssClientException

    with monkeypatch.context() as patch:
        patch.setattr(associate, 'dss_sign', dss_sign_fail)

        with pytest.raises(UserHandledException) as e:
            dss_sign_by_intent_code(uuid, user=user)
        assert 'повторите попытку' in e.value.msg

    result = dss_sign_by_intent_code(uuid, user=user)
    bundle.refresh_from_db()

    assert result
    assert len(bundle.digital_signs) == 1
    assert bundle.digital_signs[0].value.decode() == sign_type.from_signed(raw_signature).as_text()

def test_callback(mocker, init_user):
    init_user()

    client = Client()

    class BogusHandler(CallbackHandler):

        def run(self):
            raise Exception('damn')

    class FineHandler(CallbackHandler):

        def run(self):
            return f'{self.realm} - fine', 200

    class FineJsonHandler(CallbackHandler):

        def run(self):
            return dict(self.request.POST), 202

    response = client.post('/callback/dummy/other/', data={'a': 'b'})
    assert response.status_code == 400  # Неизвестный dummy

    response = client.post('/callback/sber/other/', data={'a': 'b'})
    assert response.status_code == 400  # Неопределён обработчик

    mocker.patch('bcl.banks.registry.Sber.callback_handler', BogusHandler)
    response = client.post('/callback/sber/other/', data={'a': 'b'})

    assert response.status_code == 500  # Необработанное исключение.

    mocker.patch('bcl.banks.registry.Sber.callback_handler', FineHandler)
    response = client.post('/callback/sber/other/', data={'a': 'b'})

    assert response.status_code == 200
    assert response.content == b'other - fine'

    mocker.patch('bcl.banks.registry.Sber.callback_handler', FineJsonHandler)
    response = client.post('/callback/sber/other/', data={'a': 'b'})

    assert response.status_code == 202
    assert response.content == b'{"a": ["b"]}'


class TestProved(ViewsTestSuite):

    def test_basic(self, get_proved, get_source_payment, django_assert_num_queries):

        associate = Sber

        proved1, proved2 = get_proved(
            associate=associate,
            proved_pay_kwargs=[
                {},
                {'payment': get_source_payment(associate=associate)},
            ]
        )

        def get(url):
            response = self.client.get(url, follow=True)
            assert response.status_code == 200
            return response.content.decode()

        # Для начала список.
        with django_assert_num_queries(6) as _:
            result = get(f'/associates/{associate.id}/statements/pays/')

        assert f'payments/{proved2.payment.id}/edit/' in result
        assert f'statements/pays/{proved2.id}/"' in result
        assert '<td>fakedorg</td>' in result

        proved2.set_info(name='one', bank='some')
        proved2.save()

        # Далее детализация.
        with django_assert_num_queries(3) as _:
            result = get(f'/associates/{associate.id}/statements/pays/{proved2.id}/')

        assert f'/?register_id={proved2.register.id}' in result
        assert f'payments/{proved2.payment.id}/edit/' in result
        assert '123.00 RUB' in result
        assert '[01] Контрагент</b><br>one<hr>' in result


class TestRestrictions(ViewsTestSuite):

    def test_report_users(self, init_user, get_assoc_acc_curr, django_assert_num_queries, make_org_grp, make_org, ):

        def get():
            response = self.client.get('/reports/users/', follow=True)

            assert response.status_code == 200
            return re.sub(r'\s+', '', response.content.decode())

        associate = Sber
        _, acc1, _ = get_assoc_acc_curr(associate, account='1234')
        _, acc2, _ = get_assoc_acc_curr(associate, account='5678')

        testuser = init_user()
        init_user('user1', roles=[Role.ACCOUNTANT], restr_accs=[acc1.id, acc2.id], restr_orgs=[acc1.org_id])

        with django_assert_num_queries(9) as _:
            out = get()

        assert (
            f'{settings.DEVELOPER_LOGIN}</span></td></tr><tr><td><ulclass="pl-0">Поддержка</ul></td><td>'
            '<ulclass="pl-0"><li>Организация<ulclass="pl-3mb-2"><li>fakedorg</li></ul></li></ul></td><td>'
            '<ulclass="pl-0"><li>Всё</li></ul></td></tr><tr><td><ulclass="pl-0">Зарплатнаягруппа</ul></td><td>'
            '<ulclass="pl-0"></ul></td><td><ulclass="pl-0"></ul></td></tr>') in out

        assert (
            'user1</span></td></tr><tr><td><ulclass="pl-0">Бухгалтер</ul></td><td><ulclass="pl-0"></ul></td><td>'
            '<ulclass="pl-0"><li>Обработкафинальныхвыписок</li><li>Удалениевыписок</li></ul></td></tr>') in out, out

        org = make_org(name='test_grp')
        org_grp = make_org_grp(name='test')
        org.groups.add(org_grp)
        org.save()

        init_user('user2', roles=[Role.ACCOUNTANT], restr_orgs=[acc1.org_id])
        out = get()
        assert (
                   'user2">user2</span></td></tr><tr><td><ulclass="pl-0">Бухгалтер</ul></td><td><ulclass="pl-0"></ul>'
                   '</td><td><ulclass="pl-0"><li>Обработкафинальныхвыписок</li><li>Удалениевыписок</li></ul></td></tr>'
               ) in out

        init_user('user3', roles=[Role.ACCOUNTANT], restr_orgs=[acc1.org_id], restr_grps=[org_grp.id])
        out = get()

        assert (
                   'user3</span></td></tr><tr><td><ulclass="pl-0">Бухгалтер</ul></td><td><ulclass="pl-0"></ul></td><td>'
                   '<ulclass="pl-0"><li>Обработкафинальныхвыписок</li><li>Удалениевыписок</li></ul></td></tr>'
               ) in out

        # Далее проверка усечения списка пользователей.
        testuser.roles = [Role.READ_ONLY]
        testuser.save()

        out = get()
        assert 'user1' not in out

    def test_mixed_restrictions(self, get_assoc_acc_curr, init_user):
        def get(page='payments'):
            response = self.client.get(f'/associates/9/{page}/', follow=True)

            assert response.status_code == 200
            return re.sub(r'\s+', '', response.content.decode())

        associate = Sber
        _, acc1, _ = get_assoc_acc_curr(associate, account='1234', org='testorg1')
        _, acc2, _ = get_assoc_acc_curr(associate, account='5678', org='testorg2')
        _, acc3, _ = get_assoc_acc_curr(associate, account='91011', org='testorg3')

        testuser = init_user(roles=[Role.ACCOUNTANT, Role.TREASURER, Role.SALARY_GROUP])
        testuser.restrictions = {
            Role.ACCOUNTANT: {testuser.RESTR_ORG: [acc1.org.id]},
            Role.TREASURER: {testuser.RESTR_ORG: [acc2.org.id]},
            Role.SALARY_GROUP: {testuser.RESTR_ORG: [acc3.org.id]},
        }
        testuser.save()

        out = get()
        assert 'testorg2' in out
        assert 'testorg1' not in out
        assert 'testorg3' not in out

        out = get(page='bundles')
        assert acc2.number in out
        assert acc2.number in out
        assert acc1.number not in out
        assert acc3.number not in out

        out = get(page='statements')
        assert 'testorg1' in out
        assert 'testorg2' in out
        assert 'testorg3' not in out

        out = get(page='salary')
        assert 'testorg1' not in out
        assert 'testorg2' not in out
        assert 'testorg3' in out

    def test_complex(
        self,
        get_assoc_acc_curr,
        init_user,
        build_payment_bundle,
        fake_statements,
        get_salary_registry,
        make_org_grp,
        get_proved,
        stabilize_payment_dt,
    ):

        user = init_user()
        user = init_user()
        init_user(robot=True)
        associate = Sber

        _, acc_org1_1, _ = get_assoc_acc_curr(associate.id, org='org1', account='acc-org1-1')
        _, acc_org1_2, _ = get_assoc_acc_curr(associate.id, org=acc_org1_1.org, account='acc-org1-2')
        _, acc_org2_1, _ = get_assoc_acc_curr(associate.id, org='org2', account='acc-org2-1')

        org_grp = make_org_grp(name='test')
        org = Organization.objects.get(id=acc_org2_1.org_id)
        org.groups.add(org_grp)
        org.save()

        bundle1 = build_payment_bundle(associate, account=acc_org1_1)
        bundle2 = build_payment_bundle(associate, account=acc_org1_2)
        bundle3 = build_payment_bundle(associate, account=acc_org2_1)

        with stabilize_payment_dt('2021-12-01', bundles=[bundle1, bundle2, bundle3]):
            # фиксируем даты, дабы обеспечить постоянство в тестах
            pass

        proved1 = get_proved(associate, acc_num=acc_org1_1)[0]
        proved2 = get_proved(associate, acc_num=acc_org1_2)[0]
        proved3 = get_proved(associate, acc_num=acc_org2_1)[0]

        Payment.objects.update(status=states.COMPLETE)

        make_statement = partial(fake_statements, associate, generate_payments=False, parse=True)

        make_statement(account=acc_org1_1)
        make_statement(account=acc_org1_2)
        make_statement(account=acc_org2_1)

        for acc in [acc_org1_1, acc_org1_2, acc_org2_1]:
            acc.fake_statement = False
            acc.save()

        make_registry = partial(get_salary_registry, associate, SberbankCardRegistry)

        registry1 = make_registry(contract_account=acc_org1_1, org=acc_org1_1.org, contract_number='1')
        registry2 = make_registry(contract_account=acc_org1_2, org=acc_org1_2.org, contract_number='2')
        registry3 = make_registry(contract_account=acc_org2_1, org=acc_org2_1.org, contract_number='3')

        def restrict_org():
            user.restrictions[Role.SUPPORT] = {user.RESTR_ORG: [acc_org1_1.org_id]}
            user.save()

        def restrict_acc():
            user.restrictions[Role.SUPPORT] = {user.RESTR_ACC: [acc_org1_2.id]}
            user.save()

        def restrict_grp():
            user.restrictions[Role.SUPPORT][user.RESTR_GRP] = [org_grp.id]
            user.save()

        def assert_items(url, params, method, items=None, not_items=None):

            method = method or 'get'
            response = getattr(self.client, method)(url, params, follow=True)

            assert response.status_code == 200
            contents = response.content.decode()

            for item in items or []:
                assert item in contents, url

            for item in not_items or []:
                assert item not in contents, url

        class CheckRule:

            def __init__(self, url, items, url_params=None, url_method='get'):
                self.items = items
                self.url = url
                self.url_params = url_params
                self.url_method = url_method

        check_rules = [
            CheckRule(
                '/settings/accounts/',
                [
                    ['<td>acc-org1-1</td>', '<td>acc-org1-2</td>', '<td>acc-org2-1</td>'],
                    ['<td>acc-org1-1</td>', '<td>acc-org1-2</td>'],
                    ['<td>acc-org1-2</td>'],
                ],
            ),
            CheckRule(
                '/settings/orgs/',
                [
                    ['<td>org1</td>', '<td>org2</td>'],
                    ['<td>org1</td>'],
                    ['<td>org1</td>', '<td>org2</td>'],
                ],
            ),
            CheckRule(
                '/reports/balance/',
                [
                    ['<td>acc-org1-1</td>', '<td>acc-org1-2</td>', '<td>acc-org2-1</td>'],
                    ['<td>acc-org1-1</td>', '<td>acc-org1-2</td>'],
                    ['<td>acc-org1-2</td>'],
                ],
            ),
            CheckRule(
                '/show_accounts_balance',
                [
                    ['<td>acc-org1-1</td>', '<td>acc-org1-2</td>', '<td>acc-org2-1</td>'],
                    ['<td>acc-org1-1</td>', '<td>acc-org1-2</td>'],
                    ['<td>acc-org1-2</td>'],
                ],
                {'associate_id': associate.id},
                'post',
            ),
            CheckRule(
                f'/associates/{associate.id}/payments/',
                [
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'acc-org2-1 RUB</option>',
                        'org1</option>',
                        'org2</option>',
                        f'?bundle_id={bundle1.id}"',
                        f'?bundle_id={bundle2.id}"',
                        f'?bundle_id={bundle3.id}"',
                    ],
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'org1</option>',
                        f'?bundle_id={bundle1.id}"',
                        f'?bundle_id={bundle2.id}"',
                    ],
                    [
                        'acc-org1-2 RUB</option>',
                        'org1</option>',
                        'org2</option>',
                        f'?bundle_id={bundle2.id}"',
                    ],
                ],
                {'status': states.COMPLETE, 'user': user.id}
            ),
            CheckRule(
                f'/associates/{associate.id}/bundles/',
                [
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'acc-org2-1 RUB</option>',
                        f'?bundle_number={bundle1.id}"',
                        f'?bundle_number={bundle2.id}"',
                        f'?bundle_number={bundle3.id}"',
                    ],
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        f'?bundle_number={bundle1.id}"',
                        f'?bundle_number={bundle2.id}"',
                    ],
                    [
                        'acc-org1-2 RUB</option>',
                        f'?bundle_number={bundle2.id}"',
                    ],
                ],
            ),
            CheckRule(
                f'/associates/{associate.id}/statements/',
                [
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'acc-org2-1 RUB</option>',
                        'org1</option>',
                        'org2</option>',
                        '<td>acc-org1-1</td>',
                        '<td>acc-org1-2</td>',
                        '<td>acc-org2-1</td>',
                    ],
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'org1</option>',
                        '<td>acc-org1-1</td>',
                        '<td>acc-org1-2</td>',
                    ],
                    [
                        'acc-org1-2 RUB</option>',
                        'org1</option>',
                        'org2</option>',
                        '<td>acc-org1-2</td>',
                    ],
                ],
            ),
            CheckRule(
                f'/associates/{associate.id}/statements/pays/',
                [
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'acc-org2-1 RUB</option>',
                        'org1</option>',
                        'org2</option>',
                        f'statements/pays/{proved1.id}',
                        f'statements/pays/{proved2.id}',
                        f'statements/pays/{proved3.id}',
                    ],
                    [
                        'acc-org1-1 RUB</option>',
                        'acc-org1-2 RUB</option>',
                        'org1</option>',
                        f'statements/pays/{proved1.id}',
                        f'statements/pays/{proved2.id}',
                    ],
                    [
                        'acc-org1-2 RUB</option>',
                        'org1</option>',
                        'org2</option>',
                        f'statements/pays/{proved2.id}',
                    ],
                ],
            ),
            CheckRule(
                f'/associates/{associate.id}/salary/',
                [
                    [
                        f'salary/{registry1.id}/',
                        f'salary/{registry2.id}/',
                        f'salary/{registry3.id}/',
                    ],
                    [
                        f'salary/{registry1.id}/',
                        f'salary/{registry2.id}/',
                    ],
                    [
                        f'salary/{registry1.id}/',
                        f'salary/{registry2.id}/',
                        f'salary/{registry3.id}/',
                    ],
                ],
            ),
            CheckRule(
                f'/associates/{associate.id}/payments/{bundle3.payments[0].id}/edit/',
                [
                    ['for="id_date">Дата платежа</label>'],
                    ['Добро пожаловать!'],
                    ['Добро пожаловать!'],
                ],
            ),
        ]

        for check_rule in check_rules:
            user.restrictions = {Role.SUPPORT: {user.RESTR_ORG: '*'}}
            user.save()

            assertit = partial(assert_items, check_rule.url, check_rule.url_params, check_rule.url_method)
            items_all = set(check_rule.items[0])

            # Всё доступно.
            assertit(items_all)

            # Только первая организация.
            restrict_org()
            items = check_rule.items[1]
            assertit(items, items_all.difference(items))

            # Расширяем права еще и на вторую организацию (через добавление прав на группу)
            restrict_grp()
            items = check_rule.items[0]
            assertit(items, items_all.difference(items))

            # Только второй счёт первой организации.
            restrict_acc()
            items = check_rule.items[2]
            assertit(items, items_all.difference(items))
