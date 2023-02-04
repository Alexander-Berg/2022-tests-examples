import uuid
from datetime import date
from decimal import Decimal

import pytest

from bcl.banks.party_yoomoney.common import StatusBase as YooMoneyStatusBase
from bcl.banks.registry import PayPal, YooMoney, Payoneer, Sber, Raiffeisen
from bcl.core.models import Service, states, Payment
from bcl.exceptions import PaymentSystemException


@pytest.mark.parametrize('income_type', [None, '', '3'])
def test_registration(income_type, api_client, django_assert_num_queries, get_assoc_acc_curr, monkeypatch):

    _, acc1, _ = get_assoc_acc_curr(PayPal, account='01')
    _, acc2, _ = get_assoc_acc_curr(Sber, account='02')

    name = 'Name'
    surname = 'SurName'
    midname = 'MidName'

    pay_data = {
            'user': 'automat',

            'id': '1020',
            'amount': '100.05',
            'f_acc': acc2.number,
            'currency': 'USD',

            'params': {
                'one': 1,
            },

            'metadata': {
                'meta_key': 'meta_val',
            },

            'contract_dt': '2019-12-18',

            't_first_name': name,
            't_last_name': surname,
            't_middle_name': midname,

            'f_cacc': '09876543210987654321',
            'f_bic': '7777777',
            'f_bankname': 'some',
            'f_inn': '12121212121',
            'f_kpp': '2222222',
            'f_name': 'from',

            't_acc': '012345678912345678900',
            't_cacc': '98765432109876543210',
            't_bic': '8888888',
            't_bankname': 'other',
            't_inn': '33333333333',
            't_kpp': '5555555',
            't_name': 'to',

            'purpose': 'theground',
            'priority': '5',
            'opertype': '1',

        }
    if income_type is not None:
        pay_data['income_type'] = income_type

    api_client.mock_service(Service.OEBS)

    response = api_client.post('/api/payments/', data={'payments': [
        {
            'id': '1010101',
            'amount': '0.15',
            'f_acc': 'unknownacc',
            't_acc': '101',
        },
        {
            'id': '12345',
            'amount': '10.15',
            'f_acc': acc1.number,
            't_acc': 'some@one.else',
        },
        pay_data
    ]})

    assert response.status_code == 400  # Один из счетов не существует.
    assert response.json['errors'] == [{'id': '1010101', 'msg': 'Account "unknownacc" is not registered.'}]

    pays = list(Payment.objects.order_by('id').all())
    assert len(pays) == 2
    assert pays[0].status == states.BUNDLED

    pay = pays[1]
    assert pay.number_src == '1020'
    assert pay.currency_is_dollar
    assert pay.contract_dt == date(2019, 12, 18)
    assert pay.priority == 5
    assert pay.status == states.NEW
    assert pay.summ == Decimal('100.05')
    assert all(pay.t_fio_split)
    assert pay.ground == 'theground'
    assert pay.income_type == (income_type or '')

    def raiseit(*args, **kwargs):
        raise ValueError('raiseit')

    monkeypatch.setattr('bcl.banks.registry.Sber.register_payment', raiseit)

    response = api_client.post('/api/payments/', data={'payments': [{
        'id': '666',
        'amount': '0.15',
        'f_acc': acc2.number,
        't_acc': '101',
    }]})
    assert response.status_code == 500
    err = response.json['errors'][0]
    assert err['id'] == '666'
    assert err['msg'] == 'Unhandled exception'
    assert err['description'] == 'raiseit'
    assert err['event_id']


def test_viewing(api_client, get_source_payment, django_assert_num_queries, get_assoc_acc_curr):

    acc = 'someacc'
    _, account, _ = get_assoc_acc_curr(PayPal, account=acc)

    def call(url):

        with django_assert_num_queries(2) as _:
            response = api_client.get(url)

        assert response.ok
        return sorted(response.json['data']['items'], key=lambda item: item['number'])

    pay1 = get_source_payment({'f_acc': acc, 'number_src': '01', 'metadata': {'client_id': 1111}}, service=Service.TOLOKA)
    get_source_payment({'f_acc': acc, 'number_src': '02', 'status': states.BUNDLED}, service=Service.TOLOKA)
    get_source_payment({'f_acc': acc, 'number_src': '03', 'status': states.ERROR}, service=Service.TOLOKA)
    get_source_payment(
        {'f_acc': acc, 'number_src': '04', 'status': states.ERROR, 'processing_notes': '[10] Damn'},
        service=Service.TOLOKA)
    get_source_payment({'f_acc': acc, 'number_src': '02'}, service=Service.MARKET)

    get_source_payment({'f_acc': acc, 'number_src': '01', 'status': states.ERROR}, service=Service.OEBS)
    get_source_payment(
        {'f_acc': acc, 'number_src': '02', 'status': states.ERROR}, associate=YooMoney, service=Service.OEBS)
    get_source_payment({'f_acc': acc, 'number_src': '03', 'status': states.OTHER}, service=Service.OEBS)

    api_client.mock_service(Service.TOLOKA)
    items = call(
        '/api/payments/?'
        'ids=["01","02","03","04"]&'
        'upd_since=2018-03-20&'
        'upd_till=2030-03-20&'
        f'orgs=[{account.org.id}]&'
        f'statuses=[{states.ERROR},{states.NEW},{states.BUNDLED}]')

    assert len(items) == 3
    item = items[0]
    assert item['id'] == '01'
    assert item['status'] == 'new'
    assert item['metadata'] == {'client_id': 1111}

    item = items[1]
    assert item['id'] == '03'
    assert item['status'] == 'error'
    assert not item['metadata']
    assert 'error_info' not in item

    item = items[2]
    assert item['id'] == '04'
    assert item['status'] == 'error'
    assert item['error_info'] == {'code': '10', 'msg': 'Damn'}

    # Биллинг приходит за информацией о платежах других сервисов.
    api_client.mock_service(Service.BILLING_PARTNERS)

    toloka_billing_id = {
        sid: bid for bid, sid in Service.billing_ids.items()}[Service.TOLOKA]

    items = call(
        '/api/payments/?'
        f'nums=["{pay1.number}","55555"]&'
        f'service={toloka_billing_id}'
    )
    assert len(items) == 1
    assert items[0]['id'] == '01'

    # Далее проверка преобразований статусов для ОЕБС.
    api_client.mock_service(Service.OEBS)

    items = call('/api/payments/?ids=["01","02","03","04"]')
    assert len(items) == 3

    item = items[0]
    assert item['id'] == '02'
    assert item['status'] == 'denied_by_the_bank'
    assert item['processing_notes'] == ', clientOrderId 1'

    item = items[1]
    assert item['id'] == '01'
    assert item['status'] == 'processing'

    item = items[2]
    assert item['id'] == '03'
    assert item['status'] == 'denied_by_the_bank'


def test_service_statuses(api_client, get_source_payment, django_assert_num_queries, get_assoc_acc_curr):

    acc = 'someacc'
    _, account, _ = get_assoc_acc_curr(Sber, account=acc)

    api_client.mock_service(Service.TOLOKA)

    def get_payment_and_check_status(payment_status, expected_status):

        payment = get_source_payment(
            {'f_acc': acc, 'number_src': str(uuid.uuid4()), 'status': payment_status}, service=Service.TOLOKA
        )

        response = api_client.get(f'/api/payments/?ids=["{payment.number_src}"]')
        result = list(filter(lambda item: item['id'] == payment.number_src, response.json['data']['items']))

        assert len(result) == 1
        assert result[0]['status'] == expected_status

    get_payment_and_check_status(states.BUNDLED, 'processing')
    get_payment_and_check_status(states.EXPORTED_H2H, 'processing')
    get_payment_and_check_status(states.PROCESSING, 'processing')
    get_payment_and_check_status(states.DECLINED_BY_BANK, 'error')
    get_payment_and_check_status(states.COMPLETE, 'exported_to_h2h')


class TestProbation:

    def test_invalid_schema(self, api_client, monkeypatch):

        response = api_client.post('/api/payments/probation/', data={'payments': [{
            'id': '12345',
            'amount': '10.15',
            't_acc': 30,
        }]})

        assert response.status_code == 400
        errors = response.json['errors']
        assert len(errors) == 1
        failed = errors[0]['failure'][0]

        if failed['field'] == '/t_acc':
            assert failed == {'field': '/t_acc', 'value': 30, 'msg': 'Not a valid string.'}

        else:
            assert failed == {'field': '/f_acc', 'value': None, 'msg': 'Missing data for required field.'}

    def test_exceptions(self, api_client, get_assoc_acc_curr, monkeypatch):

        def raiseit(*args, **kwargs):
            raise PaymentSystemException('one', 'damn')

        monkeypatch.setattr('bcl.banks.party_yoomoney.payment_sender.YooMoneyPaymentSender.probe', raiseit)
        _, acc, _ = get_assoc_acc_curr(YooMoney, account='1234')

        # Негатив. Необработанная ошибка.
        response = api_client.post('/api/payments/probation/', data={'payments': [
            {
                'id': '12345',
                'amount': '10.15',
                'f_acc': acc.number,
                't_acc': 'some@one.else',
            },
            {
                'id': '333444',
                'amount': '10.15',
                'f_acc': 'unknownacc',
                't_acc': 'some@one.else',
            },
        ]})

        assert response.status_code == 400
        assert response.json['errors'] == [
            {'id': '12345', 'msg': 'one: damn'},
            {'id': '333444', 'msg': 'Account "unknownacc" is not registered.'}
        ]

    def test_yoomoney(self, api_client, get_assoc_acc_curr, mock_yoomoney_request_processor, response_mock):

        api_client.mock_service(Service.TOLOKA)
        _, acc, _ = get_assoc_acc_curr(YooMoney, account='1234')

        def call(status):

            error = ''

            if status == YooMoneyStatusBase.yoomoney_status_error:
                error = 'error="30"'

            with response_mock(
                'POST https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/testDeposition -> 200 :'
                '<?xml version="1.0" encoding="UTF-8"?>'
                f'<testDepositionResponse clientOrderId="12345" status="{status}" '
                f'processedDT="2011-07-01T20:38:01.000Z" {error}/>'
            ):
                response = api_client.post('/api/payments/probation/', data={'payments': [{
                    'id': '12345',
                    'amount': '10.15',
                    'f_acc': acc.number,
                    't_acc': 'some@one.else',
                }]})

            return response

        response = call(status=YooMoneyStatusBase.yoomoney_status_success)
        assert response.ok
        assert response.json['data'] == {
            'items': [{
                'id': '12345',
                'status_bcl': 7,
                'status_remote': '0',
                'status_remote_hint': ''
            }]
        }

        response = call(status=YooMoneyStatusBase.yoomoney_status_error)
        assert response.ok
        assert response.json['data'] == {
            'items': [{
                'id': '12345',
                'status_bcl': 2,
                'status_remote': '30',
                'status_remote_hint': 'Технические проблемы на стороне Яндекс.Денег.'
            }]
        }

    def test_payoneer(self, api_client, get_assoc_acc_curr, response_mock):

        api_client.mock_service(Service.TOLOKA)

        _, acc, _ = get_assoc_acc_curr(Payoneer, account={'number': '111222333', 'currency_code': 'USD'})
        acc.remote_id = 'toloka-100101370'
        acc.save()

        def call(payee_id, body, status_code=200):

            with response_mock(
                f'GET https://api.sandbox.payoneer.com/v2/programs/100101370/payees/{payee_id}/status '
                f'-> {status_code} :{body}'
            ):
                response = api_client.post('/api/payments/probation/', data={'payments': [{
                    'id': '12345',
                    'amount': '23.15',
                    'f_acc': acc.number,
                    't_acc': payee_id,
                }]})

            return response

        response = call('123321123', '{"audit_id":56026044,"code":0,"description":"Success","status":"ACTIVE"}')
        assert response.ok
        assert response.json['data'] == {
            'items': [{
                'id': '12345',
                'status_bcl': 7,
                'status_remote': 0,
                'status_remote_hint': 'Success. Audit ID: 56026044'
            }]
        }

        response = call(
            '12332198903',
            '{"audit_id": 56026052, "code": 10005, "description": "Payee was not found", '
            '"hint": "Please ensure that the payee has registered with Payoneer"}', 404
        )
        assert response.ok
        assert response.json['data'] == {
            'items': [{
                'id': '12345',
                'status_bcl': 2,
                'status_remote': 10005,
                'status_remote_hint': 'Payee was not found. Audit ID: 56026052'
            }]
        }

    def test_raiff(self, api_client, get_assoc_acc_curr, response_mock):

        api_client.mock_service(Service.TOLOKA)

        _, account, _ = get_assoc_acc_curr(Raiffeisen, account={'number': '11111111', 'sbp_payments': True})
        org = account.org
        org.connection_id = 'probki'

        org.save()

        use_sandbox = False

        # monkeypatch.setattr(RequestLog, 'add', mock_log)
        raiff_answer_sucess = (
            '{"id": "1404fhr7i272a2", "account": "40700000007710026574", "amount": 1110.01, "currency": "RUB", '
            '"payoutMethod": "SBP", "payoutParams": { "phone": "79191234567", "bankAlias": "RAIFFEISEN", '
            '"firstName": "Петр", "middleName": "Петрович", "lastName": "Петров", "inn": 123456789101}, '
            '"incomeTypeCode": "1", "extra": {"contract": "1234567/89012"}, "status": {"value": "PENDING", '
            '"date": "2019-07-11T14:45:13.000Z"}, "createDate": "2019-07-11T14:45:13.000Z"}'
        )

        def call(body, status_code=200):

            with response_mock(
                f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payouts/draft '
                f'-> {status_code} :{body}',
                bypass=use_sandbox
            ):
                response = api_client.post('/api/payments/probation/', data={'payments': [{
                        'id': '12345',
                        'f_acc': account.number,
                        'currency': 'RUB',
                        'amount': 101,
                        't_acc': '79191234567',
                        'ground': 'test',
                        'params': {
                            'bank_alias': 'RAIFFEISEN', 't_first_name': 'Петр', 't_last_name': 'Петров',
                            't_middle_name': 'Петрович', 't_inn': 123456789101, 'income_type': '1'
                        }
                    }]})

            return response

        result = call(raiff_answer_sucess).json

        assert not result['errors']
        assert result['data']['items'][0]['status_remote'] == 'PENDING'
        assert result['data']['items'][0]['status_bcl'] == 7

        result = call(
            '{"code":"ERROR.ACCOUNT_IS_NOT_REGISTERED","message":"Указан неверный счет. '
            'Проверьте его или удалите: 40702810300000181136"}',
            status_code=403,
        ).json

        assert 'ACCOUNT_IS_NOT_REGISTERED' in result['errors'][0]['msg']


def test_cancellation(api_client, get_source_payment, django_assert_num_queries, build_payment_bundle, monkeypatch, init_user):

    api_client.mock_service(Service.OEBS)
    get_source_payment({'number_src': '1111'})

    with django_assert_num_queries(5) as _:
        response = api_client.post('/api/payments/cancellation/', data={'ids': ['1111']})

    assert response.ok
    assert response.json['data'] == {'items': [{'id': '1111'}]}


def test_revocation(
    api_client, get_source_payment, django_assert_num_queries, build_payment_bundle, get_assoc_acc_curr, monkeypatch,
    init_user
):
    _, account, _ = get_assoc_acc_curr(PayPal, account={'number': 'fakeacc', 'remote_id': 'sag'})

    # Негатив. Проверяем невозможность отвержения.
    api_client.mock_service(Service.TOLOKA)

    payment1 = get_source_payment({'number_src': '1'}, service=True)
    payment2 = get_source_payment({'number_src': '2'}, service=True)
    payment3 = get_source_payment({'number_src': '3'}, service=Service.QA)

    with django_assert_num_queries(4) as _:  # 4 запроса, включая два начала транзакций

        response = api_client.post('/api/payments/revocation/', data={
            'ids': [
                payment1.number_src,
                payment2.number_src,
                '33',
                payment3.number_src,
            ]
        })

    response = response.json
    assert response['data'] == {}
    assert response['errors'] == [
        {'msg': 'Restricted.', 'id': '1'},
        {'msg': 'Restricted.', 'id': '2'},
    ]

    # Далее сценарий для внешней системы с автоматизированным отвержением (см. PaymentDiscarder).
    api_client.mock_service(Service.QA)
    init_user(robot=True)

    bundle = build_payment_bundle(associate=PayPal, payment_dicts=[
        {'number_src': '4'},
    ], service=Service.QA, h2h=True, account=account)

    def mock_get(*args, **kwargs):
        return {'items': [{'payout_item_id': 'xxx'}], 'payout_item_id': 'xxx'}

    def mock_post(*args, **kwargs):
        return {'items': [{'payout_item_id': 'xxx'}]}

    prefix = 'paypalrestsdk.Api.'
    monkeypatch.setattr(prefix + 'get', mock_get)
    monkeypatch.setattr(prefix + 'post', mock_post)

    # 3 запроса от PaymentDiscarder.updates_from_response()
    with django_assert_num_queries(6) as _:
        response = api_client.post('/api/payments/revocation/', data={'ids': ['4']})

    assert response.ok
    assert response.json['data'] == {'items': [{'id': '4'}]}

    payment = bundle.payments[0]
    payment.refresh_from_db()
    assert payment.status == states.REVOKED

    monkeypatch.setattr(prefix + 'get', lambda: None)
    response = api_client.post('/api/payments/revocation/', data={'ids': ['4']})
    assert response.status_code == 500

    err = response.json['errors'][0]
    assert err['id'] == '4'
    assert err['msg'] == 'Unhandled exception'
    assert '<lambda>() got an unexpected keyword arg' in err['description']
    assert err['event_id']
