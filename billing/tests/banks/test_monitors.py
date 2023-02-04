from collections import namedtuple
from datetime import datetime, timedelta

from bcl.banks.monitors import AutoBatchCreateBundleMonitor, CertificateMonitor
from bcl.banks.registry import IngNl, IngCh, IngDe, IngTr, Unicredit, JpMorgan, PayPal, Payoneer
from bcl.banks.tasks import auto_create_bundles
from bcl.core.models import PaymentsBundle, SigningRight
from bcl.toolbox.signatures import Dss


def test_associate_request_monitor(monkeypatch):
    params = []
    monkeypatch.setattr('bcl.toolbox.notifiers.GraphiteNotifier.send_data', lambda *args, **kwargs: params.append(args[3]))
    PayPal.log_request(action='dummy')
    assert '.bank.paypal.request.dummy 1.000000' in params[0]


def test_autobatch_bundles(get_source_payment, get_assoc_acc_curr, read_fixture):
    """Тест автоматического формирования пакетов для платежей CPA."""

    associates_ids = tuple(
        assoc.id for assoc in (
            IngNl, IngCh, IngDe, IngTr, Unicredit, JpMorgan
        )
    )

    for index, associate_id in enumerate(associates_ids, 1):
        # создаем счета с настройками лимитов
        associate, account, _ = get_assoc_acc_curr(associate_id, account=f'407028103000010_{associate_id}_050'[:20])
        # создаем платежи для счетов
        for count in range(index):
            get_source_payment(dict(associate_id=associate_id, autobuild=1, f_acc=account.number, summ=152+count))

    # пытаемся автоформировать пакеты
    unbundled = auto_create_bundles()

    # должены создаться 2 пакета, остальные не должны пролезть в лимиты
    assert PaymentsBundle.objects.count() == 2

    message = AutoBatchCreateBundleMonitor({'unbundled': unbundled}).compose(True)

    example_message = read_fixture('autobatch_message.html', decode='utf-8')

    assert message == example_message  # проверяем отправляемое сообщение


def test_monitor_cert_expiration(dss_signing_right, monkeypatch, sitemessages, init_user):

    class DummyCert:

        def __init__(self, serial, expires=None, issued=None):
            now = datetime.now()
            self.serial = serial
            self.parsed = namedtuple('DummyParsed', ['date_expires', 'date_issued'])(
                expires or now,
                issued or now,
            )

    class DummyDss(Dss):

        def get_certificate(self, serial: str) -> DummyCert:
            return cert

    monkeypatch.setattr('bcl.banks.monitors.Dss', DummyDss)

    right_1 = dss_signing_right(associate=Unicredit, username='one', serial='123')
    dss_signing_right(associate=Unicredit, username='two')
    dss_signing_right(associate=Unicredit, username='three', serial='')

    # со сроком. не dss
    right = SigningRight(
        user=init_user(), serial_number='3344', executive='', associate_id=Unicredit.id,
        dt_expires=datetime(2022, 1, 2),
    )
    right.save()

    # без срока. не dss
    right = SigningRight(user=init_user(), serial_number='5566', executive='', associate_id=Unicredit.id)
    right.save()

    def get_notification():
        CertificateMonitor({}).run()
        messages = sitemessages()
        return messages[-1].context['stext_']

    assert not right_1.dt_expires

    cert = DummyCert('000000')
    html = get_notification()
    assert 'Срок действия сертификата истёк' in html
    assert '@one' in html
    assert '@two' in html
    assert '@three' in html
    assert 'В BCL не указан серийный номер' in html
    assert '3344' in html
    assert '5566' not in html

    expires = datetime.now() + timedelta(days=5)
    cert = DummyCert('000000', expires=expires)
    html = get_notification()
    assert 'До истечения срока действия сертификата 4 дня' in html

    # проверим, что в БД прописались новые данные истечения
    right_1.refresh_from_db()
    assert right_1.dt_expires == expires

    cert = None
    html = get_notification()
    assert 'Нет сертификата с указанным серийным номером' in html

    def exception_raiser(self, serial):
        raise ValueError(b'{"error":"invalid_grant","error_description":"invalid_username_or_password"}')

    DummyDss.get_certificate = exception_raiser
    html = get_notification()
    assert 'Неверный логин или пароль' in html


def test_statement_pays_duplicates(
    get_source_payment, get_assoc_acc_curr, init_user, parse_statement_fixture, monitor_tester):

    init_user(robot=True)

    associate = Payoneer
    _, acc, _ = get_assoc_acc_curr(associate)

    payment1 = get_source_payment({'f_acc': acc.number, 'date': '2022-05-16'}, associate=associate, service=True)
    payment2 = get_source_payment({'f_acc': acc.number, 'date': '2022-05-16'}, associate=associate, service=True)

    statement = '''{"doctype": "statement_final", "items": [
            {
            "meta": {
                "account": "fakeacc", "statement_date": "2022-05-16", "created": "2022-05-17 10:41",
                "opening_sum": "0", "opening_dc": "C", "closing_sum": "0", "closing_dc": "D"
            },
            "payments": [
                {{common}, "number": {xxx1}, "amount": "152.000000", "purpose": "doubled"},
                {{common}, "number": {xxx1}, "amount": "152.000000", "purpose": "doubled"},
                {{common}, "number": {xxx1}, "amount": "1.000000", "purpose": "unlinked"},
                {{common}, "number": {xxx2}, "amount": "152.000000", "purpose": "linked"}
            ]}
        ]}
    '''
    statement = statement.replace(
        '{common}',
        '"payee": {"bank": {"bik": "044525593"}, "inn": "7725713770", "name": "two"}, "status": 1, "dc": "D"'
    )
    statement = statement.replace('{xxx1}', f'{payment1.number}')
    statement = statement.replace('{xxx2}', f'{payment2.number}')

    monitor_messages = monitor_tester('bcl.banks.monitors.PayDuplicateMonitor')

    assert parse_statement_fixture(statement, associate, acc.number, from_file=False)
    assert '[Payoneer]' in monitor_messages[0]
    assert '/statements/pays/' in monitor_messages[0]
