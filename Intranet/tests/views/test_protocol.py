# -*- coding: utf-8 -*-
import pytest
import io
import copy

from django.test import TestCase
from django.test import override_settings
from django.core.urlresolvers import reverse
from django.utils import timezone

from app.tests.views import MessagesMixin
from app.tests.views import YauthTestCase
from app.tests.views import YauthAdminTestCase
from app.bblib.reports import FinancialReport
from app.bblib.data import Winner
from core.models import Protocol

from mock import patch
from core.utils import blackbox
from core.models import Responsible


class UnauthenticatedProtocolTest(TestCase):
    def test_redirect_to_login_url(self):
        response = self.client.get(reverse('protocol:list'))
        self.assertEquals(302, response.status_code)
        self.assertTrue(response['Location'].startswith('https://passport'))

    def test_unauthenticated_file_response(self):
        """Test that unauthorized access is not allowed for file views."""
        response = self.client.get(reverse('protocol:pdf', kwargs={'pk': 1}))
        self.assertEquals(302, response.status_code)
        self.assertTrue(response['Location'].startswith('https://passport'))


@override_settings(YAUTH_TEST_USER={'uid': 1000, 'login': 'user'})
class UnauthorizedProtocolTest(YauthTestCase):
    def test_unauthorized_response(self):
        response = self.client.get(reverse('protocol:list'))
        self.assertEqual(403, response.status_code)


class NoProtocolTest(MessagesMixin, YauthAdminTestCase):
    """Test views without existing protocols.
    
    Also test views output when no protocols are created.
    """

    url_name = 'protocol:list'

    def test_protocol_list_context(self):
        response = self._get_request()
        assert response.status_code == 200
        assert len(response.context['object_list']) == 0


class ProtocolTest(MessagesMixin, YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_protocol']

    def test_status_code(self):
        with patch('core.models.user.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            response = self.client.get(reverse('protocol:pdf', kwargs={'pk': 1}))
        assert response.status_code == 200

    def test_content_type(self):
        with patch('core.models.user.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            response = self.client.get(reverse('protocol:pdf', kwargs={'pk': 1}))
        assert response['Content-Type'] == 'application/pdf'


class ProtocolListTest(YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_protocol']
    url_name = 'protocol:list'

    def test_protocol_list_context(self):
        response = self._get_request()
        assert len(response.context['object_list']) == 2


class ProtocolFinancialAuditTest(YauthAdminTestCase):

    url_name = 'protocol:financial-report-audit'
    form_data = {'ticket_key': 'DOCUMENT-666'}
    document_data = [
        {
            'lname': 'Gillespie',
            'reward_amount': 1025.51,
            'reward_currency': 'USD',
            'is_foreigner': True,
            'contract': 'ОФ-123456',
            'hall_name': 'dizzy',
        },
        {
            'lname': 'Coltrane',
            'reward_amount': 200,
            'reward_currency': 'USD',
            'is_foreigner': True,
            'contract': 'ОФ-123457',
            'hall_name': 'johnnie'
        },
        {
            'lname': 'Дэвис',
            'reward_amount': 30000,
            'reward_currency': 'RUR',
            'is_foreigner': False,
            'contract': 'ОФ-123458',
            'hall_name': 'miles',
        },
        {
            'lname': 'Паркер',
            'reward_amount': 17000,
            'reward_currency': 'RUR',
            'is_foreigner': False,
            'contract': 'ОФ-123459',
            'hall_name': 'bird',
        },
    ]

    def setUp(self):
        self.protocol = Protocol.objects.create(ticket_document='DOCUMENT-666')
        with open('/app/app/tests/static_files/financial_report.xlsx', 'rb') as f:
            self.external_content = f.read()

    def generate_document(self, data):
        start_date = timezone.datetime(2020, 3, 1)
        end_date = timezone.datetime(2020, 4, 1)
        winners = [Winner(**line) for line in data]
        buffer = io.BytesIO()
        content = FinancialReport(start_date, end_date).generate(winners, buffer)
        buffer.seek(0)
        return buffer.read()

    def test_protocol_audit_success(self):
        out_content = self.generate_document(self.document_data)
        self.protocol.financial_report_file = out_content
        self.protocol.save()

        with patch('app.tasks.protocol.get_latest_comment_attachment') as mocked_get_comment, \
                patch('app.tasks.protocol.comment_on_ticket_key') as mocked_comment:
            mocked_get_comment.return_value = self.external_content
            self.client.get(self._url(), self.form_data)
        assert mocked_get_comment.call_args[0][0] == mocked_comment.call_args[0][0] == 'DOCUMENT-666'
        assert mocked_comment.call_args[0][1] == u'Состав выплат в порядке.'

    def test_protocol_audit_inconsistency(self):
        Responsible.objects.create(staff_username='supervisor', role=Responsible.ROLE_SUPERVISOR, name='Супервизор')
        Responsible.objects.create(staff_username='scapegt', role=Responsible.ROLE_BUGBOUNTY_RESPONSIBLE, name='Крайний')
        data = copy.deepcopy(self.document_data)
        data[0]['reward_amount'] *= 10
        out_content = self.generate_document(data)
        self.protocol.financial_report_file = out_content
        self.protocol.save()

        with patch('app.tasks.protocol.get_latest_comment_attachment') as mocked_get_comment, \
                patch('app.tasks.protocol.comment_on_ticket_key') as mocked_comment:
            mocked_get_comment.return_value = self.external_content
            self.client.get(self._url(), self.form_data)
        assert mocked_get_comment.call_args[0][0] == mocked_comment.call_args[0][0] == 'DOCUMENT-666'
        message = mocked_comment.call_args[0][1]
        expected_content = [
            u'В приведенном документе не хватает следующих записей:\nОФ-123456 Gillespie 10255.10 USD',
            u'Следующие записи в приведенном документе лишние:\nОФ-123456 Gillespie 1025.51 USD',
        ]
        for line in expected_content:
            assert line in message
        assert set(mocked_comment.call_args[1]['summonees']) == {'supervisor', 'scapegt'}

    def test_idle_comment(self):
        # Если к комментарию не прикреплен файл - молчим
        with patch('app.tasks.protocol.get_latest_comment_attachment') as mocked_get_comment, \
                patch('app.tasks.protocol.comment_on_ticket_key') as mocked_comment:
            mocked_get_comment.return_value = None
            self.client.get(self._url(), self.form_data)
        assert mocked_get_comment.call_args[0][0] == 'DOCUMENT-666'
        assert mocked_comment.call_count == 0

    def test_incorrect_content(self):
        # Если файл прикреплен, но некорректен - ругаемся
        with patch('app.tasks.protocol.get_latest_comment_attachment') as mocked_get_comment, \
                patch('app.tasks.protocol.comment_on_ticket_key') as mocked_comment:
            mocked_get_comment.return_value = 'Некорректный контент excel документа'
            self.client.get(self._url(), self.form_data)
        assert mocked_get_comment.call_args[0][0] == mocked_comment.call_args[0][0] == 'DOCUMENT-666'
        assert mocked_comment.call_args[0][1] == u'Не получилось прочитать документ'
