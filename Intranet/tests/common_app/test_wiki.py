# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.test import TestCase, override_settings
from unittest.mock import patch

from events.common_app.wiki import WikiClient, check_if_supertag_exist


class TestWikiClient(TestCase):
    def register_uri(self, supertag, status=200):
        responses.add(
            responses.HEAD,
            f'https://wiki-api.test.yandex-team.ru/_api/frontend/{supertag}',
            status=status,
        )

    @responses.activate
    def test_should_succeed_on_page_exists(self):
        supertag = 'user/test'
        user_uid = '1234'
        self.register_uri(supertag)

        client = WikiClient(user_uid)
        self.assertTrue(client.is_page_exists(supertag))

        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request
        self.assertEqual(request.headers['X-UID'], user_uid)
        self.assertIn(f'uid={user_uid}', request.url)

    @responses.activate
    def test_should_fail_on_page_does_not_exist(self):
        supertag = 'user/test'
        user_uid = '1234'
        self.register_uri(supertag, 404)

        client = WikiClient(user_uid)
        self.assertFalse(client.is_page_exists(supertag))

        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request
        self.assertEqual(request.headers['X-UID'], user_uid)
        self.assertIn(f'uid={user_uid}', request.url)

    @responses.activate
    def test_should_fail_on_page_does_not_permitted(self):
        supertag = 'user/test'
        user_uid = '1234'
        self.register_uri(supertag, 403)

        client = WikiClient(user_uid)
        self.assertFalse(client.is_page_exists(supertag))

        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request
        self.assertEqual(request.headers['X-UID'], user_uid)
        self.assertIn(f'uid={user_uid}', request.url)


class TestUpdatePageContent(TestCase):
    def setUp(self):
        self.client = WikiClient(None)
        self.page_content = '''header
{{a name='first'}} tail
old string
{{a name='second'}}
footer'''

    def test_should_insert_text_after_first_anchor(self):
        new_content = self.client._update_page_content(self.page_content, 'new string', 'first')
        expected = '''header
{{a name='first'}} tail
new string
old string
{{a name='second'}}
footer'''
        self.assertEqual(new_content, expected)

    def test_should_insert_text_after_second_anchor(self):
        new_content = self.client._update_page_content(self.page_content, 'new string', 'second')
        expected = '''header
{{a name='first'}} tail
old string
{{a name='second'}}
new string
footer'''
        self.assertEqual(new_content, expected)

    def test_should_insert_text_after_page_footer(self):
        new_content = self.client._update_page_content(self.page_content, 'new string', 'third')
        expected = '''header
{{a name='first'}} tail
old string
{{a name='second'}}
footer
new string'''
        self.assertEqual(new_content, expected)

    def test_empty_anchor_should_insert_text_after_page_footer(self):
        new_content = self.client._update_page_content(self.page_content, 'new string', None)
        expected = '''header
{{a name='first'}} tail
old string
{{a name='second'}}
footer
new string'''
        self.assertEqual(new_content, expected)


class TestIsValidSupertag(TestCase):
    def setUp(self):
        self.client = WikiClient(None)

    def test_is_valid_supertag_1(self):
        self.assertEqual(self.client.is_valid_supertag('/users/newpage/'), True)

    def test_is_valid_supertag_2(self):
        self.assertEqual(self.client.is_valid_supertag('users/newpage'), True)

    def test_is_valid_supertag_3(self):
        self.assertEqual(self.client.is_valid_supertag('/users/newpage/#myanchor'), True)

    def test_is_valid_supertag_4(self):
        self.assertEqual(self.client.is_valid_supertag('users/newpage#myanchor'), True)

    def test_is_valid_supertag_5(self):
        self.assertEqual(self.client.is_valid_supertag('users/newpage#my.anchor'), True)

    def test_is_valid_supertag_6(self):
        self.assertEqual(self.client.is_valid_supertag('users/newpage$myanchor'), False)

    def test_is_valid_supertag_7(self):
        self.assertEqual(self.client.is_valid_supertag('users/newpage#my anchor'), False)


class TestWikiUtils(TestCase):
    def test_check_if_supertag_exist_intranet(self):
        supertag = 'user/testit#first'
        dir_id = '123'
        with (
            patch.object(WikiClient, '__init__') as mock_init,
            patch.object(WikiClient, 'is_page_exists') as mock_exists,
        ):
            mock_init.return_value = None
            mock_exists.return_value = True
            result = check_if_supertag_exist(supertag, dir_id)

        self.assertTrue(result)
        mock_init.assert_called_once_with(settings.FORMS_ROBOT_UID, dir_id)
        mock_exists.assert_called_once_with(supertag)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_check_if_supertag_exist_business(self):
        wiki_robot_uid = '120001'
        supertag = 'user/testit#first'
        dir_id = '123'
        with (
            patch('events.common_app.wiki.get_robot_wiki') as mock_robot,
            patch.object(WikiClient, '__init__') as mock_init,
            patch.object(WikiClient, 'is_page_exists') as mock_exists,
        ):
            mock_robot.return_value = wiki_robot_uid
            mock_init.return_value = None
            mock_exists.return_value = True
            result = check_if_supertag_exist(supertag, dir_id)

        self.assertTrue(result)
        mock_init.assert_called_once_with(wiki_robot_uid, dir_id)
        mock_exists.assert_called_once_with(supertag)
