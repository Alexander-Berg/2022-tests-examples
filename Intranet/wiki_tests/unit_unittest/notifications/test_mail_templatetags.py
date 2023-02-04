
__author__ = 'chapson'

from django.utils import translation

from wiki.notifications.templatetags import mail_tags
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class TemplateTagsTest(BaseTestCase):
    def test_link_to_staff(self):
        self.setUsers()
        profile = self.user_chapson.staff
        profile.first_name = 'Антон'
        profile.last_name = 'Чапоргин'
        profile.save()
        translation.activate('ru')
        # должно завершиться без ошибок
        name = mail_tags.link_to_staff(self.user_chapson)
        self.assertTrue('Антон Чапоргин' in name)
