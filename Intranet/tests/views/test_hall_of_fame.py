# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

import pytest

from django.core.urlresolvers import reverse

from app.tests.views import MessagesMixin
from app.tests.views import YauthAdminTestCase
from core.models import Item
from core.models import HallOfFameGroup
from mock import patch
from core.utils import blackbox


class HallOfFameListTest(YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_hall_of_fame']

    def _get_request(self, year=None, month=None):
        return self.client.get(
            reverse('hall_of_fame:month-list',
                    kwargs={'year': year, 'month': month}))

    def _get_request_context(self, year=None, month=None):
        return getattr(self._get_request(year, month), 'context')

    def test_status_code(self):
        response = self.client.get(reverse('hall_of_fame:list'))
        assert response.status_code == 200

    def test_context(self):
        with patch('core.models.user.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            context = self._get_request_context(2015, 8)
        assert context['year'] == '2015'
        assert context['month'] == '8'
        assert context['hall_of_fame'].is_published
        assert list(context['items']) == list(Item.objects.filter(list=1))


@pytest.mark.usefixtures('patch_external_userinfo_by_login')
@pytest.mark.usefixtures('patch_external_userinfo_by_uid')
class AddHallOfFameMemberTest(MessagesMixin, YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_hall_of_fame']

    def _post_request(self, year, month, username):
        url = reverse('hall_of_fame:add-member',
                      kwargs={'year': year, 'month': month})
        return self.client.post(url, {'username': username}, follow=True)

    def test_user_added(self):
        response = self._post_request(2015, 9, 'user')
        self.assert_success_message(
            response, 'user was added to hall of fame')
        assert HallOfFameGroup.objects.get(year=2015, month=9).members.exists()

    def test_user_not_registered(self):
        response = self._post_request(2015, 9, 'nouser')
        self.assert_error_message(
            response, 'User uid 2 is not registered in BugBounty')

    def test_user_not_found(self):
        response = self._post_request(2015, 9, 'nologin')
        self.assert_error_message(
            response, 'nologin does not exist')


class PublishHallOfFameTest(MessagesMixin, YauthAdminTestCase):

    def _post_request(self, data=None):
        url = reverse('hall_of_fame:publish',
                      kwargs={'year': 2015, 'month': 9})
        return self.client.post(url, data, follow=True)

    def test_publish_success(self):
        response = self._post_request({'is_published': True})
        self.assert_success_message(response, 'Successfully published')
        assert HallOfFameGroup.published.filter(year=2015, month=9).exists()

    def test_publish_error(self):
        response = self._post_request()
        self.assert_error_message(response, 'Failed to publish')
        assert not HallOfFameGroup.published.filter(year=2015, month=9).exists()


@pytest.mark.usefixtures('patch_external_userinfo_by_uid')
class RemoveHallOfFameMemberTest(MessagesMixin, YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_hall_of_fame']

    def _post_request(self, data=None):
        url = reverse('hall_of_fame:edit',
                      kwargs={'year': 2015, 'month': 8})
        return self.client.post(url, data, follow=True)

    def test_remove_success(self):
        response = self._post_request({'reporter_id': 1})
        self.assert_success_message(
            response, 'User was removed from the hall of fame')
        assert not Item.objects.filter(
            list__year=2015, list__month=8, user=1).exists()

    def test_remove_error(self):
        response = self._post_request()
        self.assert_error_message(response, 'Failed to remove')
        assert Item.objects.filter(
            list__year=2015, list__month=8, user=1).exists()
