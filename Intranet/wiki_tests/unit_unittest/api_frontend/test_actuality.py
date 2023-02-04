from datetime import datetime, timedelta
from unittest import skipIf

import django.core.mail
from django.conf import settings
from django.core.management import call_command
from mock import patch
from pretend import stub
from rest_framework.response import Response
from ujson import loads

from wiki.api_core.raises import raises
from wiki.api_frontend.views import ActualityView
from wiki.intranet.models.intranet_extensions import Group
from wiki.pages.models.consts import EDITED_PAGE_ACTUALITY_TIMEOUT, ACTUALITY_STATUS
from wiki.pages.models.intranet_extensions import Officiality
from wiki.subscriptions.logic import create_subscription
from wiki.utils import timezone

from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase, now_for_tests
from intranet.wiki.tests.wiki_tests.common.utils import CallRecorder, celery_eager, unexpected_call
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class APIActualityHandlerBindingTest(BaseApiTestCase):
    def setUp(self):
        super(APIActualityHandlerBindingTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    @raises()
    def view_patch(*args, **kwargs):
        return Response({'patch_called': 1})

    def _check_response(self, response):
        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        self.assertEqual(json['data']['patch_called'], 1)

    def test(self):
        page = self.create_page()

        with patch('wiki.api_frontend.views.actuality.ActualityView.get', self.view_patch):
            response = self.client.get('/_api/frontend/{tag}/.actuality'.format(tag=page.tag))
        self._check_response(response)

        with patch('wiki.api_frontend.views.actuality.ActualityView.post', self.view_patch):
            response = self.client.post('/_api/frontend/{tag}/.actuality'.format(tag=page.tag))
        self._check_response(response)


class APIActualityHandlerTest(FixtureMixin, WikiDjangoTestCase):
    def setUp(self):
        super(APIActualityHandlerTest, self).setUp()
        self.view = ActualityView()

    def _invoke(self, method, request):
        return method(request)

    def _check_user(self, user_data, user):
        self.assertEqual(user_data['login'], user.username)
        self.assertEqual(user_data['first_name'], user.first_name)
        self.assertEqual(user_data['last_name'], user.last_name)

    def test_get_no_mark(self):
        page = stub(has_manual_actuality_mark=False)
        request = stub(page=page)

        response = self.view.get(request)
        self.assertEqual(response.status_code, 200)
        self.assertFalse(len(response.data))

    def _test_get_marked(self, status, with_comment, links, expected_links=None):
        if expected_links is None:
            expected_links = []

        moment = timezone.now().replace(microsecond=0)  # в АПИ нет микросекунд
        page = stub(id=111, has_manual_actuality_mark=True, actuality_status=status, actuality_marked_at=moment)
        user = self.get_or_create_user('theuser')
        request = stub(page=page)

        comment = stub(body='anything', page=page) if with_comment else None
        actuality_mark = stub(user=user, comment=comment)

        @patch(
            'wiki.pages.logic.actuality.get_actuality_mark',
            lambda x: actuality_mark if x == page.id else unexpected_call(),
        )
        @patch(
            'wiki.pages.logic.actuality.get_linked_actual_pages_tags',
            lambda x: links if x == page.id else unexpected_call(),
        )
        @patch('wiki.pages.logic.actuality.format_comment', lambda x: loads('{"patched_format_comment": 1}'))
        @patch('wiki.api_core.serializers.users.is_admin', lambda x: False)
        def f():
            return self._invoke(self.view.get, request)

        response = f()

        self.assertEqual(response.status_code, 200)
        data = response.data
        self.assertEqual(data.pop('status'), 'obsolete' if status == ACTUALITY_STATUS.obsolete else 'actual')
        self.assertEqual(
            timezone.make_aware_current(datetime.strptime(data.pop('marked_at'), '%Y-%m-%dT%H:%M:%S')), moment
        )
        self._check_user(data.pop('user'), user)
        self.assertEqual(data.pop('comment'), {'patched_format_comment': 1} if with_comment else None)
        self.assertEqual(data.pop('links'), expected_links)
        self.assertFalse(len(response.data))

    def test_get_marked_obsolete_max_data(self):
        self._test_get_marked(
            status=ACTUALITY_STATUS.obsolete, with_comment=True, links=['aaa', 'bbb'], expected_links=['/aaa', '/bbb']
        )

    def test_get_marked_obsolete_min_data(self):
        self._test_get_marked(status=ACTUALITY_STATUS.obsolete, with_comment=False, links=[])

    def test_get_marked_actual(self):
        self._test_get_marked(status=ACTUALITY_STATUS.actual, with_comment=False, links=[])

    def _test_set_mark_valid(self, data):
        p = CallRecorder()
        request = stub(page=stub(), user=stub(), DATA=data)

        @patch('wiki.api_frontend.views.actuality.mark_page_actuality', p.get_func())
        @patch(
            'wiki.api_frontend.views.actuality.ActualityView.validate',
            lambda *args: data,
        )
        def f():
            return self._invoke(self.view.post, request)

        response = f()
        self.assertEqual(response.status_code, 200)

        self.assertEqual(p.times, 1)
        args = p.calls[0].kwargs
        expected_comment = data.get('comment') or None
        expected_links = data.get('links') or []
        self.assertTrue(args['user'] is request.user)
        self.assertTrue(args['page'] is request.page)
        self.assertEqual(args['is_actual'], data['actual'])
        self.assertEqual(args['comment'], expected_comment)
        self.assertEqual(args['mixed_links'], expected_links)

    def test_set_mark_obsolete_max_data(self):
        self._test_set_mark_valid({'actual': False, 'comment': 'неправда', 'links': ['aaa', 'bbb']})

    def test_set_mark_obsolete_min_data(self):
        self._test_set_mark_valid({'actual': False, 'links': ['aaa', 'bbb']})
        self._test_set_mark_valid({'actual': False, 'comment': None, 'links': ['aaa', 'bbb']})
        self._test_set_mark_valid({'actual': False, 'comment': '', 'links': ['aaa', 'bbb']})

    def test_set_mark_actual(self):
        self._test_set_mark_valid({'actual': False, 'links': None})
        self._test_set_mark_valid(
            {
                'actual': False,
            }
        )

    def _test_set_mark_invalid(self, error_key, data):
        from wiki.api_frontend.serializers.actuality import ActualityMarkDeserializer

        serializer = ActualityMarkDeserializer(data=data)
        self.assertFalse(serializer.is_valid(), 'serializer should be invalid on {0}'.format(data))

        self.assertTrue(error_key in serializer.errors, 'no error "{0}" in {1}'.format(error_key, serializer.errors))

    def test_set_mark_invalid(self):
        # указаны ссылки для актульной страницы
        self._test_set_mark_invalid(error_key='non_field_errors', data={'actual': True, 'links': ['aaa']})

        # обязательность значений, типы и ограничения на значения
        self._test_set_mark_invalid(error_key='actual', data={})
        self._test_set_mark_invalid(error_key='links', data={'actual': False, 'links': ['']})
        self._test_set_mark_invalid(error_key='links', data={'actual': False, 'links': [None]})


class ActualityAPIIntegrationTest(BaseApiTestCase):
    def setUp(self):
        super(ActualityAPIIntegrationTest, self).setUp()
        self.setUsers()
        self.setGroupMembers()
        self.client.login('thasonic')

    def test_get_not_marked(self):
        page = self.create_page(tag='страница', body=' ')

        page_url = '{api_url}/{supertag}'.format(api_url=self.api_url, supertag=page.supertag)
        actuality_url = '{api_url}/{supertag}/.actuality'.format(api_url=self.api_url, supertag=page.supertag)

        # свежая страница без отметок актуальности
        response = self.client.get(actuality_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 0)

        response = self.client.get(page_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data['actuality_status'], 'actual')

        # НЕсвежая страница без отметок актуальности
        page.modified_at = timezone.now() - EDITED_PAGE_ACTUALITY_TIMEOUT - timedelta(seconds=1)
        page.save()

        response = self.client.get(actuality_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 0)

        response = self.client.get(page_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data['actuality_status'], 'possibly_obsolete')

    @patch('yenv.type', 'production')
    @celery_eager
    def test_mark_and_get_marked(self):
        page = self.create_page(tag='Страница', authors_to_add=[self.user_chapson], body='')

        page_url = '{api_url}/{supertag}'.format(api_url=self.api_url, supertag=page.supertag)
        actuality_url = '{api_url}/{supertag}/.actuality'.format(api_url=self.api_url, supertag=page.supertag)

        # помечаем устаревшей, без комента, без ссылок
        moment_before = now_for_tests()
        moment_before -= timedelta(microseconds=moment_before.microsecond)

        assert_queries = 110 if not settings.WIKI_CODE == 'wiki' else 13
        with self.assertNumQueries(assert_queries):
            response = self.client.post(actuality_url, data={'actual': False})

        moment_after = now_for_tests()
        moment_after -= timedelta(microseconds=moment_after.microsecond)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 0)

        assert_queries = 15 if not settings.WIKI_CODE == 'wiki' else 14
        with self.assertNumQueries(assert_queries):
            response = self.client.get(actuality_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data.pop('status'), 'obsolete')
        marked_at = timezone.make_aware_current(datetime.strptime(data.pop('marked_at'), '%Y-%m-%dT%H:%M:%S'))
        self.assertTrue(moment_before <= marked_at <= moment_after)
        self.assertEqual(data.pop('user')['login'], 'thasonic')
        self.assertEqual(data.pop('links'), [])
        self.assertEqual(data.pop('comment'), None)
        self.assertEqual(len(data), 0)

        response = self.client.get(page_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data['actuality_status'], 'obsolete')

        call_command('notify', verbosity=0)
        self.assertEqual(len(django.core.mail.outbox), 1)
        message = django.core.mail.outbox.pop(0)
        self.assertEqual('markactuality', message.extra_headers['X-Yandex-Wiki-Notification'])
        self.assertEqual(page.supertag, message.extra_headers['X-Yandex-Wiki-Path'])
        self.assertEqual(len(message.cc), 0)
        self.assertEqual(len(message.bcc), 0)
        self.assertEqual(1, len(message.to))
        self.assertEqual('Anton Chaporgin <chapson@yandex-team.ru>', message.to[0])
        self.assertTrue('Anton Chaporgin' in message.body)
        if settings.IS_INTRANET:
            self.assertTrue('/thasonic' in message.body)
            self.assertTrue('Aleksandr Pokatilov' in message.body)
        self.assertTrue('obsolete' in message.subject)
        self.assertTrue('saying' not in message.body)
        self.assertTrue('obsolete' in message.body)
        self.assertTrue(page.tag in message.subject)
        self.assertTrue(page.tag in message.body)
        self.assertTrue(page.supertag in message.body)

    @patch('yenv.type', 'production')
    @celery_eager
    def test_mark_and_get_marked_with_data(self):
        page = self.create_page(tag='Страница', authors_to_add=[self.user_chapson], body='')
        actuality_url = '{api_url}/{supertag}/.actuality'.format(api_url=self.api_url, supertag=page.supertag)

        # помечаем устаревшей, с коментом, с валидными ссылками, с наблюдателем
        create_subscription(self.user_kolomeetz, page)
        another_page = self.create_page(tag='Страница/subpage', authors_to_add=[self.user_thasonic])

        comment_text = 'неправда'
        assert_queries = 121 if not settings.WIKI_CODE == 'wiki' else 21
        with self.assertNumQueries(assert_queries):
            response = self.client.post(
                actuality_url,
                data={
                    'actual': False,
                    'comment': comment_text,
                    'links': [
                        page.tag,
                        '%s://%s/%s' % (settings.WIKI_PROTOCOL, settings.NGINX_HOST, another_page.tag),
                        'https://yandex.com',
                    ],
                },
            )
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 0)

        response = self.client.get(actuality_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data.pop('status'), 'obsolete')
        data.pop('marked_at')
        self.assertEqual(data.pop('user')['login'], 'thasonic')
        self.assertEqual(set(data.pop('links')), {'/' + page.tag, '/' + another_page.tag, 'https://yandex.com'})
        self.assertEqual(data.pop('comment')['content'][0]['wiki-attrs']['txt'], comment_text)
        self.assertEqual(len(data), 0)

        call_command('notify', verbosity=0)
        self.assertEqual(len(django.core.mail.outbox), 2)

        message = django.core.mail.outbox.pop(0)
        self.assertEqual(1, len(message.to))
        self.assertEqual('Konstantin Kolomeetz <kolomeetz@yandex-team.ru>', message.to[0])
        if settings.IS_INTRANET:
            self.assertTrue('/thasonic' in message.body)
        self.assertTrue('obsolete' not in message.subject)
        self.assertTrue(page.tag in message.subject)
        self.assertTrue('commented' in message.body)
        self.assertTrue('obsolete' not in message.body)
        self.assertTrue(comment_text in message.body)

        message = django.core.mail.outbox.pop(0)
        self.assertEqual(1, len(message.to))
        self.assertEqual('Anton Chaporgin <chapson@yandex-team.ru>', message.to[0])
        if settings.IS_INTRANET:
            self.assertTrue('/thasonic' in message.body)
        self.assertTrue('obsolete' in message.subject)
        self.assertTrue(page.tag in message.subject)
        self.assertTrue('saying' in message.body)
        self.assertTrue('obsolete' in message.body)
        self.assertTrue(comment_text in message.body)

    @skipIf(settings.IS_BUSINESS, 'Only for intranet')
    @patch('yenv.type', 'production')
    @celery_eager
    def test_mark_official(self):
        page = self.create_page(tag='Страница', authors_to_add=[self.user_chapson], body='', is_official=True)
        actuality_url = '{api_url}/{supertag}/.actuality'.format(api_url=self.api_url, supertag=page.supertag)

        officiality = Officiality.objects.create(page=page)
        officiality.responsible_persons.add(self.user_volozh)
        officiality.responsible_persons.add(self.user_asm)
        officiality.responsible_groups.add(Group.objects.get(url='yandex_mnt_srv'))
        officiality.responsible_groups.add(Group.objects.get(url='yandex_mnt'))
        officiality.save()

        moment_before = now_for_tests()
        moment_before -= timedelta(microseconds=moment_before.microsecond)
        with self.assertNumQueries(13):
            response = self.client.post(actuality_url, data={'actual': True})
        moment_after = now_for_tests()
        moment_after -= timedelta(microseconds=moment_after.microsecond)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 0)

        response = self.client.get(actuality_url)
        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data.pop('status'), 'actual')
        marked_at = timezone.make_aware_current(datetime.strptime(data.pop('marked_at'), '%Y-%m-%dT%H:%M:%S'))
        self.assertTrue(moment_before <= marked_at <= moment_after)
        self.assertEqual(data.pop('user')['login'], 'thasonic')
        self.assertEqual(data.pop('links'), [])
        self.assertEqual(data.pop('comment'), None)
        self.assertEqual(len(data), 0)

        call_command('notify', verbosity=0)
        self.assertEqual(len(django.core.mail.outbox), 4)

        emails = [x.to[0] for x in django.core.mail.outbox]

        for template in [
            '<asm@yandex-team.ru>',
            '<chapson@yandex-team.ru>',
            '<volozh@yandex-team.ru>',
            '<kolomeetz@yandex-team.ru>',
        ]:
            self.assertTrue(any([template in email for email in emails]))

        for message in django.core.mail.outbox:
            self.assertEqual(len(message.to), 1)
            self.assertTrue(page.tag in message.subject)
            self.assertTrue('actual' in message.subject)
            self.assertTrue('/thasonic' in message.body)
            self.assertTrue('actual' in message.body)
            self.assertTrue('saying' not in message.body)
