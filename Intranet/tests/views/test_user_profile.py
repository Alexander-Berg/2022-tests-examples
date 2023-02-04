# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from app.tests.views import MessagesMixin
from app.tests.views import YauthAdminTestCase
from core.models import Badge
from core.models import Reporter
from core.models import UserBadge
from mock import patch


@pytest.mark.usefixtures('patch_external_userinfo_by_login')
class AddUserBadgeTest(MessagesMixin, YauthAdminTestCase):
    form_data = {'username': 'user'}
    url_name = 'user_profile:add_badge'

    def setUp(self):
        super(AddUserBadgeTest, self).setUp()
        self.reporter = Reporter.objects.create(uid=1)
        self.badge = Badge.objects.create()

    def test_assigned_badge(self):
        response = self._post_request_follow_redirect(self.badge.pk)
        assert UserBadge.objects.filter(user=self.reporter, badge=self.badge).exists()
        self.assert_success_message(
            response, 'Badge was successfully assigned')

    def test_user_not_found(self):
        self.form_data = {'username': 'nouser'}
        response = self._post_request_follow_redirect(self.badge.pk)
        self.assert_error_message(
            response, 'User does not exist')

    def test_invalid_badge(self):
        response = self._post_request_follow_redirect(self.badge.pk + 1)
        assert response.status_code == 404


@pytest.mark.usefixtures('patch_external_userinfo_by_login')
@pytest.mark.usefixtures('patch_external_userinfo_by_uid')
class UserProfileListTest(MessagesMixin, YauthAdminTestCase):
    url_name = 'user_profile:list'
    form_data = {'username': 'user'}

    def setUp(self):
        super(UserProfileListTest, self).setUp()
        Reporter.objects.create(uid=1, username='user1', balance_contract_id='ABC-123457')
        Reporter.objects.create(uid=2, username='user2', balance_contract_id='ABC-123456')
        Reporter.objects.create(uid=3, username='user3')
        Reporter.objects.create(uid=4, username='user4')

    def test_find_user(self):
        """Неудавшийся поиск возвращает всех пользователей, удавшийся возвращает одного.
        Валидируем наличие пользователей по ссылкам на страницы редактирования платежной информации на странице"""
        url = self._url()
        with patch('app.views.user_profile.UserProfileList.render_to_response') as mocked_render:
            self.client.get(url, {'username': 'supermegauser'})
            self.client.get(url, {'username': 'ABC-123456'})
            self.client.get(url, {'username': 'user'})
            self.client.get(url)
        # В первый раз никого не нашли, отдали всех
        len(mocked_render.call_args_list[0][0][0]['users']) == 4

        # Нашли по номеру договора, отдали одного
        len(mocked_render.call_args_list[1][0][0]['users']) == 1
        mocked_render.call_args_list[1][0][0]['users'][0].balance_contract_id == 'ABC-123456'

        # Замоканный blackbox отдает по username=user uid=1, тоже нашли
        len(mocked_render.call_args_list[2][0][0]['users']) == 1
        mocked_render.call_args_list[2][0][0]['users'][0].uid == 1

        # Запрос без username возвращает всех
        len(mocked_render.call_args_list[0][0][0]['users']) == 4
