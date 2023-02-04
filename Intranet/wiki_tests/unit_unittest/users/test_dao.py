from unittest import skipIf

from django.conf import settings

from wiki.users.dao import find_staff_models, find_wiki_group_models
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class UsersDaoTest(BaseTestCase):
    def test_find_staff_models(self):
        self.setUsers()

        def assert_users(actual_users, expected_users):
            self.assertEqual(expected_users, [u['login'] for u in actual_users])

        # находится много, сортировка по фамилии
        users = find_staff_models(q='a', limit=100)
        assert_users(users, ['asm', 'thasonic', 'chapson'])

        # limit=1, сортировка по фамилии
        users = find_staff_models(q='a', limit=1)
        assert_users(users, ['asm'])

        # находится по фамилии
        users = find_staff_models(q='КАТИЛ', limit=100)
        assert_users(users, ['thasonic'])

        # находится по имени
        users = find_staff_models(q='НТО', limit=100)
        assert_users(users, ['chapson'])

        # находится по логину
        users = find_staff_models(q='hason', limit=100)
        assert_users(users, ['thasonic'])

        # пустой результат
        users = find_staff_models(q='нет таких', limit=100)
        self.assertEqual(len(users), 0)

        # разные слова, есть в разных пользователях 1
        users = find_staff_models(q='hason НТО', limit=100)
        self.assertEqual(len(users), 0)

        # разные слова, есть в разных пользователях 2
        users = find_staff_models(q='hason chap', limit=100)
        self.assertEqual(len(users), 0)

        # разные слова, одно в одном пользователе, другое нигде
        users = find_staff_models(q=' ненене ПОК', limit=100)
        assert_users(users, [])

        # разные слова, оба в одном пользователе
        users = find_staff_models(q='HASON ПОК ', limit=100)
        assert_users(users, ['thasonic'])

    if not settings.IS_INTRANET:

        @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
        def test_find_wiki_group_models(self):
            if settings.IS_BUSINESS:
                self.set_groups_business()
            else:
                self.set_groups_extranet()

            def assert_groups(actual_groups, expected_groups):
                self.assertEqual(expected_groups, [u['name'] for u in actual_groups])

            # находится много, сортировка по названию
            groups = find_wiki_group_models(q='l', limit=100)
            assert_groups(groups, ['employees', 'school', 'school РУС'])

            # limit=1, сортировка по названию
            groups = find_wiki_group_models(q='l', limit=1)
            assert_groups(groups, ['employees'])

            # пустой результат
            groups = find_wiki_group_models(q='нет таких', limit=100)
            self.assertEqual(len(groups), 0)

            # разные слова, есть в разных группах
            groups = find_wiki_group_models(q='a l', limit=100)
            self.assertEqual(len(groups), 0)

            # разные слова, одно в одной группе, другое нигде
            groups = find_wiki_group_models(q=' abc ploy', limit=100)
            assert_groups(groups, [])

            # разные слова, оба в одной группе
            groups = find_wiki_group_models(q='mp oy', limit=100)
            assert_groups(groups, ['employees'])
