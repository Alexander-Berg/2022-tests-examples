# -*- coding: utf-8 -*-
import pytest
import mock
import xlrd
import freezegun

from dateutil.relativedelta import relativedelta

from django.core.management import call_command
from django.utils import timezone

from core.models import Protocol, Reporter, NewPaymentInfo, Reward, Responsible
from core.tests.factory import ReporterFactory, RewardFactory
from app.bblib.reports import ProtocolReport, FinancialReport

pytestmark = pytest.mark.django_db

@freezegun.freeze_time('2019-03-03 21:19:00')
def test_generate_protocol():
    command = 'bugbounty_generate_protocol'
    def create_reward(status=None, date=None):
        try:
            reporter_uid = Reporter.objects.latest('uid').uid + 1
        except Reporter.DoesNotExist:
            reporter_uid = 0
        reporter = ReporterFactory(uid=reporter_uid, balance_client_id=reporter_uid)
        reward = RewardFactory(reporter=reporter, payment_currency=1,
                               status=status if status else Reward.ST_NEW,
                               balance_contract_created=date if date else timezone.now())
        return reward

    assert Reward.objects.count() == 0
    assert Protocol.objects.count() == 0
    call_command(command)
    assert Protocol.objects.count() == 0  # выплат нет, протокол не создан

    previous_month = timezone.now() - relativedelta(months=1)
    too_old = timezone.now() - relativedelta(months=2)

    new_rewards = {create_reward(Reward.ST_NEW, previous_month).pk
                   for _ in range(2)}
    uploaded_rewards = {create_reward(Reward.ST_UPLOADED_TO_YT, previous_month).pk
                        for _ in range(2)}
    old_rewards = {create_reward(Reward.ST_UPLOADED_TO_YT, too_old).pk
                   for _ in range(2)}

    # Новые награды за текущий месяц не должны попадать в протокол
    too_new_rewards = {create_reward(Reward.ST_UPLOADED_TO_YT).pk
                   for _ in range(2)}

    call_command(command)
    protocol = Protocol.objects.latest('pk')
    assert protocol.start_date.month == 2
    assert Protocol.objects.count() == 1  # создан 1 протокол с 2 выплатами

    assert set(Reward.objects.filter(protocol=protocol).values_list('pk', flat=True)) == uploaded_rewards

    new_uploaded_rewards = {create_reward(Reward.ST_UPLOADED_TO_YT, previous_month).pk
                            for _ in range(2)}

    call_command(command)
    protocol = Protocol.objects.latest('pk')
    assert Protocol.objects.count() == 2  # создан 1 протокол с новыми выплатами
    assert set(Reward.objects.filter(protocol=protocol).values_list('pk', flat=True)) == new_uploaded_rewards


def test_pdf_protocol_generation():
    reporter = ReporterFactory(uid=0)
    with mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
        pi = NewPaymentInfo.objects.create(reporter=reporter)
    protocol = Protocol.objects.create(start_date=timezone.datetime(2020, 2, 1))
    reward = RewardFactory(
        reporter=reporter,
        status=Reward.ST_UPLOADED_TO_YT,
        payment_currency=1,
        payment_amount_rur=1,
        payment_amount_usd=1,
        protocol=protocol,
    )
    with mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_get_ds:
        # Резиденты не попадают в протокол, выплат в протоколе не будет
        mocked_get_ds.return_value = {'type': 'ph', 'fname': 'vasya', 'lname': 'pupkin'}
        content_ph = ProtocolReport.create_report(protocol)

        # В протоколе появляются выплаты
        mocked_get_ds.return_value['type'] = 'ytph'
        content_ytph = ProtocolReport.create_report(protocol)

        # Добавим объектов в responsibles, в протоколе появятся места под подписи с фамилиями
        Responsible.objects.create(
            staff_username='chairman', role=Responsible.ROLE_INFOSEC_CHAIRMAN, name='Чейрмэн')
        Responsible.objects.create(staff_username='member', role=Responsible.ROLE_INFOSEC_MEMBER, name='Мембер')
        content_responsibles = ProtocolReport.create_report(protocol)

    assert 0 < len(content_ph) < len(content_ytph) < len(content_responsibles)


def test_xls_report_generation():
    def mocked_datasync(uid, entry_dict):
        if uid == 0:
            return {'type': 'ytph', 'fname': 'vasya', 'lname': 'pupkin', 'country': 'Uganda'}
        else:
            return {'type': 'ph', 'fname': 'petya', 'lname': 'lastochkin'}

    ytph_reporter = ReporterFactory(uid=0, username='vasyan', balance_contract_id='ABA-cabadaba')
    ph_reporter = ReporterFactory(uid=1, username='petyan', balance_contract_id='QWQ-eqwqtqwq')
    with mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
        NewPaymentInfo.objects.create(reporter=ytph_reporter)
        NewPaymentInfo.objects.create(reporter=ph_reporter)
    start_date = timezone.datetime(2020, 1, 3, 0, 0, 0, tzinfo=timezone.utc)
    protocol = Protocol.objects.create(start_date=start_date)
    from decimal import Decimal
    RewardFactory(
        reporter=ytph_reporter,
        status=Reward.ST_UPLOADED_TO_YT,
        payment_currency=2,
        payment_amount_usd=Decimal('100.00'),
        protocol=protocol,
    )
    RewardFactory(
        reporter=ytph_reporter,
        status=Reward.ST_UPLOADED_TO_YT,
        payment_currency=2,
        payment_amount_usd=Decimal('1030.00'),
        protocol=protocol,
    )
    RewardFactory(
        reporter=ph_reporter,
        status=Reward.ST_UPLOADED_TO_YT,
        payment_currency=1,
        payment_amount_rur=Decimal('327.68'),
        protocol=protocol,
    )

    with mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_get_ds:
        mocked_get_ds.side_effect = mocked_datasync
        content = FinancialReport.create_report(protocol)
    book = xlrd.open_workbook(file_contents=content)
    sheet = book.sheet_by_index(0)
    data = sorted([sheet.row_values(1), sheet.row_values(2)])
    assert u' '.join(map(unicode, data[0])) == u'lastochkin 327.68 RUR Да QWQ-eqwqtqwq'
    assert u' '.join(map(unicode, data[1])) == u'pupkin 1130.0 USD Нет ABA-cabadaba'


def test_create_kaznaoperations_tickets():
    protocol = Protocol.objects.create(start_date=timezone.datetime(2020, 2, 5), protocol_file='abacaba')
    Responsible.objects.create(staff_username='chairman', role=Responsible.ROLE_INFOSEC_CHAIRMAN, name='Чермэн')
    Responsible.objects.create(staff_username='member', role=Responsible.ROLE_INFOSEC_MEMBER, name='Мембер')
    with mock.patch('app.tasks.protocol.create_ticket') as mocked_create_issue:
        issue_mock = mock.Mock()
        issue_mock.key = 'KAZNAOPERATIONS-777'
        mocked_create_issue.return_value = issue_mock
        call_command('bugbounty_create_protocol_tickets', queue='kaznaoperations')
    issue_args = mocked_create_issue.call_args[1]
    assert issue_args['queue'] == 'KAZNAOPERATIONS'
    assert issue_args['summary'] == 'Охота за ошибками: выплаты нерезидентам за февраль'
    assert 'требуется подписать протокол' in issue_args['description']
    assert set(issue_args['summonees']) == {'chairman', 'member'}
    attachment = issue_args['attachment']
    assert attachment.name == 'report.pdf'
    assert attachment.read() == 'abacaba'
    protocol.refresh_from_db()
    assert protocol.ticket_kaznaops == 'KAZNAOPERATIONS-777'


def test_create_document_tickets():
    protocol = Protocol.objects.create(start_date=timezone.datetime(2020, 3, 5))
    Responsible.objects.create(staff_username='supervisor', role=Responsible.ROLE_SUPERVISOR, name='Супервизор')
    with mock.patch('app.tasks.protocol.create_ticket') as mocked_create_issue:
        issue_mock = mock.Mock()
        issue_mock.key = 'DOCUMENT-777'
        mocked_create_issue.return_value = issue_mock
        call_command('bugbounty_create_protocol_tickets', queue='document')
    issue_args = mocked_create_issue.call_args[1]
    assert issue_args['queue'] == 'DOCUMENT'
    assert issue_args['summary'] == 'Проверка - Март 2020 - Охота за ошибками'
    issue_description = issue_args['description']
    for line in [
        'Сверка состава выплат за Март 2020',
        'создайте в этом тикете комментарий',
        'В течении нескольких минут',
        'robot-bugbounty',
        'tools@',
    ]:
        assert line in issue_description
    assert issue_args['summonees'] == ['supervisor']
    assert issue_args['tags'] == ['bugbounty']
    protocol.refresh_from_db()
    assert protocol.ticket_document == 'DOCUMENT-777'
