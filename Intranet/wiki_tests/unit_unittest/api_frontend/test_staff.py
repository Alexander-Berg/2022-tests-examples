from django.conf import settings
from mock import patch

from wiki.api_core.errors.bad_request import InvalidDataSentError
from wiki.api_frontend.serializers.staff import StaffSerializer
from wiki.intranet.models import Staff
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class StaffViewTest(BaseApiTestCase):
    def _get(self, login=None):
        self.setUsers()
        self.client.login('chapson')

        request_url = '{api_url}/.staff?{params}'.format(
            api_url=self.api_url, params='login=%s' % login if login else ''
        )
        return self.client.get(request_url)

    def test_no_login(self):
        response = self._get()
        self.assertEqual(409, response.status_code)

    def test_no_staff(self):
        response = self._get(login='chubaka')
        self.assertEqual(404, response.status_code)

    @patch(
        'wiki.api_frontend.views.staff.staff_model_by_login',
        lambda login: Staff(login='vpupkin', first_name='Вася', last_name='Пупкин'),
    )
    def test_ok(self):
        response = self._get(login='vpupkin')
        self.assertEqual(200, response.status_code)
        self.assertEqual({'first_name': 'Вася', 'last_name': 'Пупкин', 'is_dismissed': False}, response.data['data'])


class StaffSerializerTest(BaseApiTestCase):
    def _serialize(self, staff, lang=None):
        context = {'lang': lang} if lang else None
        return StaffSerializer(staff, context=context).data

    def test_invalid_lang(self):
        staff = Staff(login='vpupkin', first_name='Вася', last_name='Пупкин')
        self.assertRaisesRegex(
            InvalidDataSentError, r'Language must be one of \(\'ru\', \'en\'\)', lambda: self._serialize(staff, 'de')
        )

    def test_no_lang(self):
        staff = Staff(login='vpupkin', first_name='Вася', last_name='Пупкин')
        data = self._serialize(staff)
        self.assertEqual({'first_name': 'Вася', 'last_name': 'Пупкин', 'is_dismissed': False}, data)

    def test_ru_lang(self):
        staff = Staff(login='vpupkin', first_name='Вася', last_name='Пупкин')
        data = self._serialize(staff, 'ru')
        self.assertEqual({'first_name': 'Вася', 'last_name': 'Пупкин', 'is_dismissed': False}, data)

    def test_en_lang(self):
        staff = Staff(login='vpupkin', first_name_en='Vasya', last_name_en='Pupkin')
        data = self._serialize(staff, 'en')
        self.assertEqual({'first_name': 'Vasya', 'last_name': 'Pupkin', 'is_dismissed': False}, data)

    if settings.IS_INTRANET:

        def test_dismissed(self):
            staff = Staff(login='vpupkin', first_name='Вася', last_name='Пупкин', is_dismissed=True)
            data = self._serialize(staff)
            self.assertEqual({'first_name': 'Вася', 'last_name': 'Пупкин', 'is_dismissed': True}, data)
