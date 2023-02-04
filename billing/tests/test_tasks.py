from datetime import datetime, timedelta, time, date

from bcl.banks.party_ing.common import IngRuSftpConnector
from bcl.banks.party_sber.registry_operator import (
    SberbankCardRegistry, SberbankSalaryRegistry, SberbankDismissalRegistry,
)
from bcl.banks.party_tinkoff.registry_operator import TinkoffSalaryRegistry
from bcl.banks.registry import Raiffeisen, Sber, JpMorgan, VtbDe, Payoneer, Ing, Tinkoff
from bcl.banks.tasks import (
    monitor_queues, problems_count, monitor_statement, auto_build_bundles,
    fake_statement_generation, invalidated_payments_notify, salary_registry_notify
)
from bcl.core.models import Lock, states, PaymentsBundle, NumbersRegistry, Statement, Payment, RemoteCredentials, \
    RemoteClient
from bcl.core.tasks import stale_cleanup, send_messages, cleanup_messages
from bcl.toolbox.notifiers import GraphiteNotifier


def test_monitor_queues(build_payment_bundle, monkeypatch):

    data_to_send = {}

    class MockNotifier(GraphiteNotifier):

        def send(self, data=None):
            data_to_send[self.context['metric']] = self.context['value']

    monkeypatch.setattr('bcl.banks.tasks.GraphiteNotifier', MockNotifier)

    def get_bundle(assoc, status):
        bundle = build_payment_bundle(assoc)
        bundle.status = status
        bundle.save()

    get_bundle(JpMorgan, PaymentsBundle.state_processing_ready)
    get_bundle(JpMorgan, PaymentsBundle.state_processing_ready)
    get_bundle(JpMorgan, PaymentsBundle.state_processing_now)
    get_bundle(Sber, PaymentsBundle.state_processing_now)

    monitor_queues()

    assert data_to_send == {
        'bank.jpmorgan.queue.bundles.status.in_delivery': 1,
        'bank.jpmorgan.queue.bundles.status.for_delivery': 2,
        'bank.sber.queue.bundles.status.in_delivery': 1,
    }


def test_monitor_finals(
    mailoutbox, monkeypatch, read_fixture_from_dir, get_statement, get_assoc_acc_curr, response_mock):

    def mock_today(*args, **kwargs):
        return datetime.combine(date(2018, 8, 15), time(0, 0))

    monkeypatch.setattr('bcl.toolbox.utils.DateUtils.today', mock_today)

    task_dt = datetime.now() - timedelta(days=1)
    account_number = '40702810300001423589'
    get_assoc_acc_curr(Raiffeisen, account=account_number)
    lock = Lock(name='raiff_monitor_finals', dt_acquired=task_dt, dt_released=task_dt)
    lock.save()

    def mock_holiday(*args, **kwargs):
        return False

    monkeypatch.setattr('bcl.toolbox.calendars.Calendar.check_date_is_holiday', mock_holiday)

    request = (
        'GET https://calendar.yandex.ru/export/holidays.xml?country_id=225&start_date=2018-07-31&'
        'out_mode=holidays&who_am_i=bcl&end_date=2018-08-15 -> 200:'
        '<?xml version="1.0" encoding="UTF-8"?><holidays><get-holidays country-id="225" start-date="2018-07-31" '
        'end-date="2018-08-15" out-mode="holidays"><days><day date="2018-08-04" is-holiday="1" day-type="weekend" '
        'is-transfer="0"></day><day date="2018-08-05" is-holiday="1" day-type="weekend" is-transfer="0"></day>'
        '<day date="2018-08-11" is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-08-12" '
        'is-holiday="1" day-type="weekend" is-transfer="0"></day></days></get-holidays></holidays>'
    )

    with response_mock(request):
        monitor_statement('test')
    assert len(mailoutbox) == 1

    assert '[Райффайзен]. Отсутствуют итоговые выписки' in mailoutbox[0].subject
    assert 'На данный момент не получены итоговые выписки за прошедший рабочий день' in mailoutbox[0].body
    assert '40702810300001423589 fakedorg RUB\n' in mailoutbox[0].body

    lock.dt_acquired = task_dt
    lock.dt_released = task_dt
    lock.save()
    acc = ['40702810000000013449', '40702810500000048105']
    for account in acc:
        get_assoc_acc_curr(Raiffeisen, account=account)

    body = read_fixture_from_dir('banks/party_raiff/fixtures/statement/many_acc_equal_payments.txt', root_path=False)

    statement = get_statement(body, Raiffeisen.id)
    Raiffeisen.statement_dispatcher.get_parser(statement).process()

    with response_mock(request):
        monitor_statement('test')
    assert len(mailoutbox) == 1


def test_check_problems(sitemessages, monkeypatch, get_source_payment, init_user, response_mock):
    task_dt = datetime.now() - timedelta(days=1)
    lock = Lock(name='problems_count', dt_acquired=task_dt, dt_released=task_dt)
    lock.save()

    def check_task(on_date, paydate=None, mailout=0):

        if paydate:
            get_source_payment({
                'payroll_num': '279131',
                'summ': '15.00',
                'status': states.EXPORTED_ONLINE,
                'date': paydate

            }, associate=Sber).save()

        def mock_today(*args, **kwargs):
            return datetime.combine(on_date, time(0, 0))

        monkeypatch.setattr('bcl.toolbox.utils.DateUtils.today', mock_today)

        problems_count()
        messages = sitemessages()
        assert len(messages) == mailout
        return messages

    with response_mock(
        'GET https://calendar.yandex.ru/export/holidays.xml?country_id=225&start_date=2018-11-24&'
        'out_mode=holidays&who_am_i=bcl&end_date=2018-12-09 -> 200:'
        '<?xml version="1.0" encoding="UTF-8"?>\n<holidays><get-holidays country-id="225" start-date="2018-11-24" '
        'end-date="2018-12-09" out-mode="holidays"><days><day date="2018-11-24" is-holiday="1" day-type="weekend" '
        'is-transfer="0"></day><day date="2018-11-25" is-holiday="1" day-type="weekend" is-transfer="0"></day>'
        '<day date="2018-12-01" is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-12-02" '
        'is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-12-08" is-holiday="1" '
        'day-type="weekend" is-transfer="0"></day><day date="2018-12-09" is-holiday="1" day-type="weekend" '
        'is-transfer="0"></day></days></get-holidays></holidays>'
    ):
        check_task(date(2018, 12, 9), datetime(2018, 12, 7))

    with response_mock(
        'GET https://calendar.yandex.ru/export/holidays.xml?country_id=225&start_date=2018-11-25&'
        'out_mode=holidays&who_am_i=bcl&end_date=2018-12-10 -> 200:'
        '<?xml version="1.0" encoding="UTF-8"?>\n<holidays><get-holidays country-id="225" start-date="2018-11-25" '
        'end-date="2018-12-10" out-mode="holidays"><days><day date="2018-11-25" is-holiday="1" day-type="weekend" '
        'is-transfer="0"></day><day date="2018-12-01" is-holiday="1" day-type="weekend" is-transfer="0"></day>'
        '<day date="2018-12-02" is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-12-08" '
        'is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-12-09" is-holiday="1" '
        'day-type="weekend" is-transfer="0"></day></days></get-holidays></holidays>'
    ):
        message = check_task(date(2018, 12, 10), mailout=1)[-1]
    assert f"[BCL] Проблемы на {datetime.now().strftime('%Y-%m-%d')}: 1 шт" in message.context['subject']
    assert '07-12-18' in message.context['stext_']

    with response_mock(
        'GET https://calendar.yandex.ru/export/holidays.xml?country_id=225&start_date=2018-10-21&'
        'out_mode=holidays&who_am_i=bcl&end_date=2018-11-05 -> 200:'
        '<?xml version="1.0" encoding="UTF-8"?><holidays><get-holidays country-id="225" start-date="2018-10-21" '
        'end-date="2018-11-05" out-mode="holidays"><days><day date="2018-10-21" is-holiday="1" day-type="weekend" '
        'is-transfer="0"></day><day date="2018-10-27" is-holiday="1" day-type="weekend" is-transfer="0"></day>'
        '<day date="2018-10-28" is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-11-03" '
        'is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-11-04" is-holiday="1" '
        'day-type="holiday" is-transfer="0" holiday-name="День народного единства">День народного единства</day>'
        '<day date="2018-11-05" is-holiday="1" day-type="weekend" is-transfer="0" '
        'holiday-name="Перенос выходного с 4 ноября">Перенос выходного с 4 ноября</day></days>'
        '</get-holidays></holidays>'
    ):
        check_task(date(2018, 11, 5), datetime(2018, 11, 2), 1)

    with response_mock(
        'GET https://calendar.yandex.ru/export/holidays.xml?country_id=225&start_date=2018-10-22&'
        'out_mode=holidays&who_am_i=bcl&end_date=2018-11-06 -> 200:'
        '<?xml version="1.0" encoding="UTF-8"?><holidays><get-holidays country-id="225" start-date="2018-10-22" '
        'end-date="2018-11-06" out-mode="holidays"><days><day date="2018-10-27" is-holiday="1" day-type="weekend" '
        'is-transfer="0"></day><day date="2018-10-28" is-holiday="1" day-type="weekend" is-transfer="0"></day>'
        '<day date="2018-11-03" is-holiday="1" day-type="weekend" is-transfer="0"></day><day date="2018-11-04" '
        'is-holiday="1" day-type="holiday" is-transfer="0" holiday-name="День народного единства">'
        'День народного единства</day><day date="2018-11-05" is-holiday="1" day-type="weekend" is-transfer="0" '
        'holiday-name="Перенос выходного с 4 ноября">Перенос выходного с 4 ноября</day></days>'
        '</get-holidays></holidays>'
    ):
        message = check_task(date(2018, 11, 6), mailout=2)[-1]

    # в дате названия всегда фигурирует datetime.datetime.now
    assert f"[BCL] Проблемы на {datetime.now().strftime('%Y-%m-%d')}: 2 шт" in message.context['subject']
    assert '02-11-18' in message.context['stext_']


def test_check_invalidation(sitemessages, monkeypatch, get_source_payment, init_user, time_freeze):
    now_date = datetime.now()
    task_dt = datetime.combine(now_date, time(6, 0))
    lock = Lock(
        name='invalidated_payments_notify', dt_acquired=task_dt, dt_released=task_dt, result=task_dt.isoformat()
    )
    lock.save()

    with time_freeze(str(now_date - timedelta(days=1))):

        payment1 = get_source_payment(
            {'status': states.USER_INVALIDATED, 'number': '12345'}, associate=Sber
        )
    with time_freeze(str(datetime.combine(now_date, time(9, 0)))):

        payment2 = get_source_payment(
            {'status': states.USER_INVALIDATED, 'number': '12346'}, associate=Sber
        )

    lock.refresh_from_db()
    mailoutbox_len = len(sitemessages())
    assert mailoutbox_len == 0

    with time_freeze(str(datetime.combine(now_date, time(10, 0)))):
        invalidated_payments_notify(lock.result)

    messages = sitemessages()
    assert len(messages) == mailoutbox_len + 1

    assert payment2.number in messages[-1].context['stext_']
    assert payment1.number not in messages[-1].context['stext_']

    lock.refresh_from_db()
    with time_freeze(str(datetime.combine(now_date, time(11, 0)))):
        payment3 = get_source_payment(
            {'status': states.USER_INVALIDATED, 'number': '12347'}, associate=Sber
        )

    lock.refresh_from_db()
    mailoutbox_len = len(sitemessages())

    with time_freeze(str(datetime.combine(now_date, time(16, 0)))):
        invalidated_payments_notify(lock.result)

    messages = sitemessages()
    assert len(messages) == mailoutbox_len + 1

    last_message = messages[-1].context['stext_']
    assert payment3.number in last_message
    assert payment2.number not in last_message
    assert payment1.number not in last_message


def test_reset_numbers():
    associate_sber = Sber
    NumbersRegistry.get_number(associate_sber)
    entry_sber = NumbersRegistry.objects.get(associate_id=associate_sber.id)
    entry_sber.number = 999
    entry_sber.save()

    associate_vtbde = VtbDe
    NumbersRegistry.get_number(associate_vtbde)
    entry_vtbde = NumbersRegistry.objects.get(associate_id=associate_vtbde.id)
    entry_vtbde.number = 999
    entry_vtbde.save()

    NumbersRegistry.reset_numbers()

    entry_sber.refresh_from_db()
    assert entry_sber.number == 0

    entry_vtbde.refresh_from_db()
    assert entry_vtbde.number == 999


def test_autobundle(get_assoc_acc_curr, get_source_payment, sitemessages):

    associate, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='40702840700000011973')
    account.settings['auto_batch']['days'][str(datetime.now().weekday())]['active'] = False
    account.save()
    exported1 = get_source_payment(dict(associate_id=Raiffeisen.id, f_acc=account.number, summ=300, autobuild='1'))
    exported2 = get_source_payment(
        dict(associate_id=Raiffeisen.id, f_acc=account.number, summ=300, autobuild='1'))
    cancelled = get_source_payment(
        dict(associate_id=Raiffeisen.id, f_acc=account.number, summ=300, autobuild='1'))
    wo_auto = get_source_payment(
        dict(associate_id=Raiffeisen.id, f_acc=account.number, summ=300, autobuild='0'))

    task_dt = datetime.now() - timedelta(days=1)
    lock = Lock(name='auto_build_bundles', dt_acquired=task_dt, dt_released=task_dt)
    lock.save()

    def check_new():
        for pay in [exported1, exported2, cancelled, wo_auto]:
            pay.refresh_from_db()
            assert pay.status == states.NEW

    auto_build_bundles()
    check_new()

    account.settings['auto_batch']['days'][str(datetime.now().weekday())] = {
        'limit_low': 300, 'limit_up': 601, 'active': True
    }
    account.save()

    auto_build_bundles()
    message = sitemessages()[-1]
    assert f"[BCL] Не пройден контроль сумм для автоформирования" in message.context['subject']
    assert account.number in message.context['stext_']
    check_new()

    cancelled.cancel()
    cancelled.refresh_from_db()
    assert cancelled.status == states.CANCELLED

    auto_build_bundles()

    exported1.refresh_from_db()
    assert not exported1.is_new

    exported2.refresh_from_db()
    assert not exported1.is_new

    wo_auto.refresh_from_db()
    assert wo_auto.is_new


def test_stale_cleanup():
    stale_cleanup(None)


def test_monitor_certificates(run_task):
    run_task('monitor_certificates')


def test_reschedule_bundles(run_task):
    run_task('reschedule_bundles')


def test_reschedule_statements(run_task):
    run_task('reschedule_statements')


def test_reset_payment_numbers(run_task):
    run_task('reset_payment_numbers')


def test_monitor_suspicious_payments(run_task):
    run_task('monitor_suspicious_payments')


def test_process_documents(get_document, mock_gpg, monkeypatch, run_task):

    associate = Ing

    doc = get_document(associate=associate, generate='spd')
    doc.schedule()

    monkeypatch.setattr(IngRuSftpConnector, 'put', lambda *args, **kwargs: '')

    run_task('process_documents')

    doc.refresh_from_db()
    assert doc.status == states.EXPORTED_H2H

    record = get_document(associate=associate)
    record.schedule()
    run_task('process_documents')
    record.refresh_from_db()
    assert 'Не найден DocumentCreator' in record.processing_notes


def test_fake_statement_generation(get_assoc_acc_curr, fake_statements, init_user):

    associate = Payoneer

    init_user(robot=True)

    _, acc, _ = get_assoc_acc_curr(associate=associate)

    statements = fake_statements(associate=associate, account=acc, parse=False)
    assert len(statements) == 1
    statements[0].delete()

    payment: Payment = Payment.objects.first()
    fake_statement_generation(acc=acc, on_date=payment.dt.date())

    statements = list(Statement.objects.all())
    assert len(statements) == 1
    assert f'"number": {payment.number}' in statements[0].zip_raw.decode()


def test_send_messages(run_task):
    send_messages(0)


def test_cleanup_messages(run_task):
    cleanup_messages(0)


def test_check_salary(sitemessages, monkeypatch, get_salary_registry, init_user):
    task_dt = datetime.now() - timedelta(days=1)
    lock = Lock(name='salary_registry_notify', dt_acquired=task_dt, dt_released=task_dt)
    lock.save()

    def create_registry(associate, regstry_type, status, contract_num='123', reg_num='111', created_dt=datetime.now()):
        get_salary_registry(
            associate, regstry_type,
            status=status, contract_number=contract_num,
            registry_number=reg_num, created_dt=created_dt,
            employees=[
                {
                    'record_id': '1',
                    'first_name': 'a',
                    'last_name': 'b',
                    'patronymic': 'c',
                    'identify_card': {'number': 'xxx', 'series': 'xxx'},
                    'currency_code': 'RUB',
                    'amount': '11',
                    'dismissal_date': '2019-10-21'
                }]
        )

    def check_task(on_date, mailout=0):

        def mock_today(*args, **kwargs):
            return datetime.combine(on_date, time(0, 0))

        monkeypatch.setattr('bcl.toolbox.utils.DateUtils.today', mock_today)

        salary_registry_notify()
        messages = sitemessages()
        assert len(messages) == mailout
        return messages

    create_registry(Sber, SberbankCardRegistry, states.NEW)
    check_task(datetime.now() + timedelta(days=1))

    create_registry(Sber, SberbankSalaryRegistry, states.NEW, created_dt=datetime.now() + timedelta(days=1), reg_num='231')
    create_registry(Sber, SberbankSalaryRegistry, states.REGISTER_ANSWER_LOADED_PARTIALLY)
    create_registry(Sber, SberbankSalaryRegistry, states.REGISTER_ANSWER_LOADED)
    create_registry(Sber, SberbankDismissalRegistry, states.NEW)
    create_registry(Tinkoff, TinkoffSalaryRegistry, states.REGISTER_ANSWER_LOADED_PARTIALLY, contract_num='1234')

    check_task(datetime.now())

    message = check_task(datetime.now() + timedelta(days=1), mailout=1)[-1]
    message_text = message.context['stext_']

    assert f"[BCL] Не загружены ответники по зарплатным рееcтрам на {datetime.now().strftime('%Y-%m-%d')}: 2 шт" in message.context['subject']
    assert 'Сбербанк' in message_text
    assert 'Тинькофф' in message_text
    assert 'счёта' in message_text
    assert 'карт' not in message_text
    assert 'увольнений' not in message_text
    assert '231' not in message_text

    message = check_task(datetime.now() + timedelta(days=2), mailout=2)[-1]
    message_text = message.context['stext_']

    assert '231' in message_text
    assert '111' not in message_text


def test_update_rauth_creds(run_task, init_user, response_mock, time_freeze):

    associate = Sber
    user = init_user()

    rclient = RemoteClient.objects.create(
        ident='my',
        associate_id=associate.id,
    )

    dt_revised = datetime(2021, 9, 1)
    dt_revise = datetime(2021, 9, 3)

    def init_cred():
        credentials = RemoteCredentials(
            associate_id=associate.id,
            user=user,
            client=rclient,
            dt_revised=dt_revised,
            dt_revise=dt_revise,
            state='1',
            nonce='2',
        )
        credentials.token_access = 'x'
        credentials.token_refresh = 'y'
        credentials.save()
        return credentials

    credentials_1 = init_cred()
    credentials_2 = init_cred()

    with response_mock([
        f'POST https://edupirfintech.sberbank.ru:9443/ic/sso/api/v2/oauth/token -> 400:'
        '{"error": "aaaa", "error_description": "bbbb"}',

        f'POST https://edupirfintech.sberbank.ru:9443/ic/sso/api/v2/oauth/token -> 200:'
        '{"scope": "openid", "access_token": "z", '
        '"refresh_token": "q", "id_token": "n", "expires_in": 3600}',
    ]):
        with time_freeze('2021-09-04'):
            run_task('update_rauth_creds')

    # Ошибка при запросе обновления.
    credentials_1.refresh_from_db()
    assert credentials_1.token_access == 'x'
    assert credentials_1.token_refresh == 'y'
    assert credentials_1.dt_revise == dt_revise
    assert credentials_1.dt_revised == dt_revised

    # Запрос обновления обработан успешно.
    credentials_2.refresh_from_db()
    assert credentials_2.token_access == 'z'
    assert credentials_2.token_refresh == 'q'
    assert credentials_2.dt_revise > dt_revise
    assert credentials_2.dt_revised > dt_revised


def test_update_rauth_clients(run_task, init_user, response_mock, time_freeze):

    associate = Sber
    user = init_user()

    dt_revised = datetime(2021, 9, 1)
    dt_revise = datetime(2021, 9, 3)

    def init_client(ident):

        client = RemoteClient.objects.create(
            ident=ident,
            associate_id=associate.id,
            dt_revise=dt_revise,
        )
        client.secret = '123'
        client.save()

        cred = RemoteCredentials(
            associate_id=associate.id,
            user=user,
            client=client,
            dt_revised=dt_revised,
        )
        cred.token_access = 'zzzz'
        cred.save()

        return client

    client_1 = init_client('one')
    client_2 = init_client('two')

    with response_mock([
        'POST https://edupirfintech.sberbank.ru:9443/v1/change-client-secret -> 400:'
        '{"error": "aaaa", "error_description": "bbbb"}',

        'POST https://edupirfintech.sberbank.ru:9443/v1/change-client-secret -> 200:'
        '{"clientSecretExpiration": 40}'
    ]):
        with time_freeze('2021-09-04'):
            run_task('update_rauth_clients')

    # Ошибка при запросе обновления.
    client_1.refresh_from_db()
    assert client_1.secret == '123'
    assert client_1.dt_revise == dt_revise
    assert client_1.dt_revised is None

    # Запрос обновления обработан успешно.
    client_2.refresh_from_db()
    assert client_2.secret != '123'
    assert client_2.dt_revise > dt_revise
    assert client_2.dt_revised > dt_revised
