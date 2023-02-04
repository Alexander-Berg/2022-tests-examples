from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory
from django.core.urlresolvers import resolve

from staff.lib.testing import GroupFactory, StaffFactory, GroupMembershipFactory, UserFactory

from .factories.model import (
    AchievementFactory,
    GivenAchievementFactory,
    IconFactory,
)
from .factories.notifications import RouteFactory


class AchieverySmokeTestCase(TestCase):
    def setUp(self):
        self.user = StaffFactory(
            login='tester', lang_ui='ru', native_lang='ru',
            first_name='first-name', last_name='last-name',
            first_name_en='first_name-en', last_name_en='last-name-en'
        )

        self.adm = GroupFactory(url='achieveryadmin')
        self.membership = GroupMembershipFactory(staff=self.user,
                                                 group=self.adm)
        self.achievement = AchievementFactory(native_lang='ru',
                                              title='russian title',
                                              title_en='english title',
                                              owner_group=self.adm)
        self.holder = StaffFactory(
            login='jester', lang_ui='ru',
            first_name='first-name', last_name='last-name',
            first_name_en='first_name-en', last_name_en='last-name-en'
        )
        self.given = GivenAchievementFactory(
            achievement=self.achievement,
            person=self.holder,
            level=1,
        )

        self.external_user = UserFactory()
        self.external_user_st = StaffFactory(login='external', user=self.external_user)
        self.external_user_st.is_internal = lambda: False
        self.ext = GroupFactory(url='outstaff')
        self.achievement_for_external = AchievementFactory(native_lang='ru', owner_group=self.ext)
        self.given_for_external = GivenAchievementFactory(
            achievement=self.achievement_for_external,
            person=self.external_user_st,
            level=1,
        )

        self.icon_default = IconFactory(achievement=self.achievement, level=-1)
        self.icon = IconFactory(achievement=self.achievement, level=1)
        self._route = RouteFactory(transport_id='email')
        self.rf = RequestFactory()


class AchievementSmokeTestCase(AchieverySmokeTestCase):
    def one_url(self, pk):
        return reverse('achievery:achievement_details', kwargs={'pk': pk})

    @property
    def list_url(self):
        return reverse('achievery:achievements_list')

    def test_get_list(self):
        data = {
            'owner_group.members.uid': self.user.uid,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_filter(self):
        data = {
            'title.en': self.achievement.title_en,
            'owner_group.members.uid': self.user.uid,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_paging(self):
        data = {
            '_page': 10,
            'owner_group.members.uid': self.user.uid,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_sorting(self):
        data = {
            '_sort': '-id',
            'owner_group.members.uid': self.user.uid,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_fields(self):
        data = {
            '_fields': 'id,title',
            'owner_group.members.uid': self.user.uid,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_search(self):
        response = self.client.get(self.list_url,
                                   {'_search': 'blah'})
        self.assertEqual(response.status_code, 200)

    def test_get_one(self):
        response = self.client.get(self.one_url(self.achievement.id))
        self.assertEqual(response.status_code, 200)

    def test_get_one_fields(self):
        response = self.client.get(self.one_url(self.achievement.id),
                                   {'_fields': 'id,description'})
        self.assertEqual(response.status_code, 200)

    def test_post_list(self):
        response = self.client.post(self.list_url)
        self.assertEqual(response.status_code, 405)

    def test_post_list_fields(self):
        response = self.client.post(self.list_url + '?_fields=id,description')
        self.assertEqual(response.status_code, 405)

    def test_post_one(self):
        response = self.client.post(
            self.one_url(self.achievement.id),
            {'title.en': 'new title'},
        )
        self.assertEqual(response.status_code, 200)

    def test_post_one_fields(self):
        response = self.client.post(
            self.one_url(self.achievement.id) + '?_fields=description_short',
            {'title.en': 'new title'},
        )
        self.assertEqual(response.status_code, 200)


class GivenSmokeTestCase(AchieverySmokeTestCase):
    def one_url(self, pk):
        return reverse('achievery:given_details', kwargs={'pk': pk})

    @property
    def list_url(self):
        return reverse('achievery:given_list')

    def test_get_list(self):
        response = self.client.get(self.list_url)
        self.assertEqual(response.status_code, 200)

    def test_get_list_filter(self):
        data = {
            'comment': self.given.comment,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_paging(self):
        data = {
            '_page': 10,
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_sorting(self):
        data = {
            '_sort': '-id',
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_fields(self):
        data = {
            '_fields': 'id,title',
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_list_search(self):
        data = {
            '_search': 'blah',
        }
        response = self.client.get(self.list_url, data)
        self.assertEqual(response.status_code, 200)

    def test_get_one(self):
        response = self.client.get(self.one_url(self.given.id))
        self.assertEqual(response.status_code, 200)

    def test_get_one_fields(self):
        data = {
            '_fields': 'id,description',
        }
        response = self.client.get(self.one_url(self.given.id), data)
        self.assertEqual(response.status_code, 200)

    def test_post_list(self):
        response = self.client.post(
            self.list_url,
            {
                'level': -1,
                'person.id': self.user.id,
                'achievement.id': self.achievement.id,
            }
        )
        self.assertEqual(response.status_code, 201, response.content)

    def test_post_list_fields(self):
        response = self.client.post(
            self.list_url + '?_fields=id,description,level,achievement,person',
            {
                'level': -1,
                'person.id': self.user.id,
                'achievement.id': self.achievement.id,
            }
        )
        self.assertEqual(response.status_code, 201, response.content)

    def test_post_one(self):
        response = self.client.post(
            self.one_url(self.given.id),
            {'title.en': 'new title', 'revision': 0}
        )
        self.assertEqual(response.status_code, 200, response.content)

    def test_post_one_fields(self):
        response = self.client.post(
            self.one_url(self.given.id) + '?_fields=title.en,revision',
            {'title.en': 'new title', 'revision': 0}
        )
        self.assertEqual(response.status_code, 200, response.content)

    def test_external_get_list_not_self(self):
        kwargs = {
            'person.login': self.user.login,
        }
        request = self.rf.get(self.list_url, kwargs)
        request.user = self.external_user
        response = resolve(self.list_url).func(request)
        self.assertEqual(response.status_code, 403)

    def test_external_get_list_self(self):
        kwargs = {
            'person.login': self.external_user_st.login,
        }
        request = self.rf.get(self.list_url, kwargs)
        request.user = self.external_user
        response = resolve(self.list_url).func(request)
        self.assertEqual(response.status_code, 200)

    def test_external_get_hall_of_fame(self):
        kwargs = {
            'achievement.id': self.achievement_for_external.id,
        }
        request = self.rf.get(self.list_url, kwargs)
        request.user = self.external_user
        response = resolve(self.list_url).func(request)
        self.assertEqual(response.status_code, 403)


class IconSmokeTestCase(AchieverySmokeTestCase):
    def one_big_url(self, pk):
        return reverse('achievery:icon_raw', kwargs={'pk': pk, 'size': 'big'})

    def one_small_url(self, pk):
        return reverse('achievery:icon_raw', kwargs={'pk': pk, 'size': 'small'})

    def test_get_one_big(self):
        self.icon = IconFactory()
        response = self.client.get(self.one_big_url(self.icon.id))
        self.assertEqual(response.status_code, 200)

    def test_get_one_small(self):
        self.icon = IconFactory()
        response = self.client.get(self.one_small_url(self.icon.id))
        self.assertEqual(response.status_code, 200)
