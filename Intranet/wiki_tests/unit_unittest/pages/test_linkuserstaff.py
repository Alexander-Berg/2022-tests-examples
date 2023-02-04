
from django.core.management import call_command

from wiki.intranet.models import Staff
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class LinkuserstaffTest(BaseTestCase):
    def test_login_changed_WIKI_4042(self):
        """Если у пользователя поменялся логин, нам прилетит обновленная модель Staff
        Команда должна обновить username в модели auth.User
        """
        self.setUsers()
        staff_model = self.user_thasonic.staff
        staff_model.login = 'new_login'
        staff_model.save()

        call_command('linkuserstaff', verbosity=0)

        renewed = Staff.objects.get(login=staff_model.login)
        self.assertEqual(renewed.django_user.username, staff_model.login)
        self.assertEqual(renewed.user.username, staff_model.login)
