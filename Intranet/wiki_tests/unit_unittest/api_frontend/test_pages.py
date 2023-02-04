from datetime import datetime
from io import BytesIO
from ujson import loads
from unittest import skipIf

import yenv
from django.conf import settings
from django.http import HttpResponse
from django.test import override_settings
from django.test.client import MULTIPART_CONTENT
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase, now_for_tests
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import create_grid
from mock import patch
from pretend import raiser, stub
from rest_framework import serializers
from wiki import access as wiki_access
from wiki.api_core.errors.permissions import AlreadyRequestedAccess, UserHasNoAccess
from wiki.api_frontend.serializers.pages import PageEditionSerializer
from wiki.api_frontend.views.pages import PageView, RawPageParamsValidator
from wiki.favorites_v2.models import Folder
from wiki.files.models import File
from wiki.notifications.models import PageEvent
from wiki.pages.api import save_page
from wiki.pages.exceptions import PageTypeMismatch
from wiki.pages.models import AbsentPage, Page, PageLink, PageWatch, Revision
from wiki.users import DEFAULT_AVATAR_ID
from wiki.users.logic import set_user_setting
from wiki.utils.supertag import translit
from wiki.utils.timezone import make_aware_utc

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name",
      "type": "string",
      "required": true
    },
    {
      "name" : "number",
      "type": "number",
      "title" : "Number of films"
    }
  ]
}
"""

# структура, описывающая поля, которые должны быть в ответе про страницу.
page_json_struct = [
    'tag',
    'supertag',
    'url',
    'page_type',
    'is_redirect',
    'lang',
    'last_author',
    'title',
    'breadcrumbs',
    'created_at',
    'modified_at',
    'owner',
    'access',
    'version',
    'current_user_subscription',
    'user_css',
    'bookmark',
    'actuality_status',
    'qr_url',
    'comments_count',
    'is_official',
    'org',
    'comments_status',
    'authors',
    'with_new_wf',
    'is_readonly',
    'notifier',
]

env = str(yenv.type.lower())


class APIPagesHandlerTest(BaseApiTestCase):
    """
    Tests for pages api handlers
    """

    def setUp(self):
        super(APIPagesHandlerTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_error404(self):
        """
        404
        """
        assert_queries = 45 if not settings.WIKI_CODE == 'wiki' else 3
        with self.assertNumQueries(assert_queries):
            response = self.client.get('{api_url}/NonExistentPage'.format(api_url=self.api_url))

        json = loads(response.content)
        self.assertTrue('error' in json)
        self.assertEqual(response.status_code, 404)

    def test_access_error(self):
        """
        403
        """
        page = self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson)

        assert_queries = 50 if not settings.WIKI_CODE == 'wiki' else 8
        with self.assertNumQueries(assert_queries):
            response = self.client.get(
                '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
            )
        json = loads(response.content)
        error = json['error']
        self.assertEqual(response.status_code, 403)
        self.assertEqual(error['message'], ['You have no access to requested resource'])
        self.assertEqual(error['error_code'], UserHasNoAccess.error_code)

        if settings.IS_INTRANET:
            reason = 'хочу всё\nзнать'
            response = self.client.put(
                '{api_url}/{page_supertag}/.requestaccess'.format(api_url=self.api_url, page_supertag=page.supertag),
                {'reason': reason},
            )
            self.assertEqual(response.status_code, 200)

            with self.assertNumQueries(8):
                response = self.client.get(
                    '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
                )
            error = loads(response.content)['error']
            self.assertEqual(response.status_code, 403)
            self.assertEqual(error['error_code'], AlreadyRequestedAccess.error_code)

    def test_simple_show(self):
        """
        Получение страницы
        """
        page = self.create_page(tag='Страница', body='**Супертекст в суперкедах**')
        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        assert_queries = 56 if not settings.WIKI_CODE == 'wiki' else 14
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        page_data = loads(response.content)['data']

        for example_key in page_json_struct:
            self.assertTrue(
                example_key in page_data, '"{key}" not in {result}'.format(key=example_key, result=page_data)
            )
        # Проверить, что нет никаких новых полей в ответе.
        for key in page_data:
            self.assertTrue(key in page_json_struct, '"{0}" is a new field in response'.format(key))

        self.assertEqual(page_data['actuality_status'], 'actual')

        author = page_data['authors'][0]
        self.assertEqual(author['uid'], int(self.user_thasonic.staff.uid))
        self.assertEqual(author['login'], 'thasonic')
        self.assertEqual(author['display'], 'Александр Покатилов')
        self.assertEqual(author['first_name'], 'Александр')
        self.assertEqual(author['last_name'], 'Покатилов')
        self.assertEqual(author['avatar_id'], DEFAULT_AVATAR_ID)
        if settings.IS_INTRANET:
            self.assertEqual(author['is_dismissed'], False)

        authors = page_data['authors']
        self.assertEqual(len(authors), 1)
        self.assertEqual(author['uid'], authors[0]['uid'])
        self.assertEqual(author['login'], authors[0]['login'])

        self.user_thasonic.staff.native_lang = 'ru'
        self.user_thasonic.staff.save()

        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)
        page_data = loads(response.content)['data']

        self.assertTrue(page_data['with_new_wf'])
        author = page_data['authors'][0]
        self.assertEqual(author['display'], 'Alexander Pokatilov')
        self.assertEqual(author['first_name'], 'Alexander')
        self.assertEqual(author['last_name'], 'Pokatilov')
        self.assertFalse(page_data['is_readonly'])

    def test_show_with_user_css(self):
        parent_page = self.create_page(tag='Стр', body='')
        page = self.create_page(tag='Стр/Тест', body='')
        self.create_file(parent_page, url='user.css')

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)
        page_data = loads(response.content)['data']

        if settings.WIKI_CODE == 'wiki':
            self.assertEqual(page_data['user_css'], '/str/.files/user.css')
        else:
            self.assertEqual(page_data['user_css'], None)

    def test_main_page_alias(self):
        """
        '/' - алиас '/homepage'
        """
        self.create_page(tag=settings.MAIN_PAGE, body=""" - Викарий придет нескоро, Зельда! """)
        assert_queries = 56 if not settings.WIKI_CODE == 'wiki' else 14
        with self.assertNumQueries(assert_queries):
            response = self.client.get(self.api_url + '/')
        self.assertEqual(200, response.status_code)
        self.assertEqual('homepage', loads(response.content)['data']['supertag'])

    def test_files(self):
        page, _, _ = save_page('newpage', '**page**', title='Ok', user=self.user)

        PageWatch(user=self.user.username, page=page).save()
        get(File, page=page, name='killa.txt')

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        self.client.get(request_url)

    def test_code_theme(self):
        page = self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )

        def get_code_theme():
            request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
            response = self.client.get(request_url)
            user_data = loads(response.content)['user']
            return user_data['settings']['code_theme']

        self.assertEqual(get_code_theme(), 'github')

        self.user.profile['code_theme'] = 'idea'
        self.user.save()

        self.assertEqual(get_code_theme(), 'idea')

    def test_create_simple_page(self):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag='testpage'))

        tag = 'ТестПаге'
        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        assert_queries = 60 if not settings.WIKI_CODE == 'wiki' else 19
        with self.assertNumQueries(assert_queries):
            response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)

        data = loads(response.content)['data']
        self.assertEqual(data['supertag'], 'testpage')
        self.assertEqual(data['tag'], tag)
        self.assertEqual(data['url'], '/testpage')

        page = Page.active.filter(supertag='testpage').get()
        self.assertEqual(page.tag, tag)
        self.assertEqual(page.body, 'Йа %%тельце%%, и я хочу быть в базе')
        self.assertEqual(page.title, 'ЙаЗаголовок')

        rev = Revision.objects.get(page=page)
        self.assertEqual(rev.body, 'Йа %%тельце%%, и я хочу быть в базе')
        self.assertEqual(rev.created_at, page.modified_at)

    def test_strict_page_create(self):
        page_data = {'title': 'Header', 'body': 'body'}
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag='testpage'))
        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag='tag/dontoverride')
        response = self.client.put(request_url, data=page_data)
        self.assertEqual(200, response.status_code)

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag='tag/dontoverride')
        response = self.client.put(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

        # Из-за того что то что в урле считается за "тег" который после нормализации станет "супертегом"
        # создание страницы путом  или постом на tag/donto_verri_de эквивалентно tag/dontoverride
        # это адовый адок и это надо решить через создание страницы по супертегу в новой апишке
        # без включения его в [POST] api/frontend/{...}/ а в body реквеста
        # пока я просто расширяю старую апиху

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag='tag/donto_verri_de')
        response = self.client.put(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

    def test_create_reserved_supertag_page(self):
        page_data = {'title': 'Ковологаз', 'body': 'Просто текст без %%смысла%%'}
        tag = supertag = 'users/testuser'

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)
        self.assertEqual(response.json()['error']['message'][0], 'Bad tag given: "{}"'.format(supertag))

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        request_url = '{api_url}/{page_tag}/.grid/create'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)
        self.assertEqual(response.json()['error']['message'][0], 'Bad tag given: "{}"'.format(supertag))

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

    @skipIf(settings.WIKI_CODE != 'biz', 'Only for business version of wiki')
    def test_create_setupservice_page(self):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        tag = supertag = 'yandexsetupservice/coolservice'
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

        request_url = '{api_url}/{page_tag}/.grid/create'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

    @skipIf(settings.WIKI_CODE != 'biz', 'Only for business version of wiki')
    def test_create_shad1_page_fail(self):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        tag = supertag = 'shad/shadpage'
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

        request_url = '{api_url}/{page_tag}/.grid/create'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

    @skipIf(settings.WIKI_CODE != 'biz', 'Only for business version of wiki')
    def test_create_shad2_page_fail(self):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        tag = supertag = 'shad'
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

        request_url = '{api_url}/{page_tag}/.grid/create'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(409, response.status_code)

    @skipIf(settings.WIKI_CODE != 'biz', 'Only for business version of wiki')
    def test_create_shad_page_success(self):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        tag = supertag = 'shad1'
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)

    def test_create_page_from_txt(self):
        title = 'ЙаЗаголовок'
        filedata = 'Йа %%тельце%%, и я хочу быть в базе'
        supertag = 'testpagefromtxt'
        tag = 'TestPageFromTxt'

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        testfile = BytesIO(filedata.encode('utf-8'))
        testfile.name = 'test.txt'
        data = {
            'title': title,
            'body': '',
            'file': testfile,
        }

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=data, content_type=MULTIPART_CONTENT)

        self.assertEqual(200, response.status_code)
        response_data = response.json()['data']

        self.assertEqual(response_data['supertag'], supertag)
        self.assertEqual(response_data['tag'], tag)
        self.assertEqual(response_data['url'], '/{}'.format(tag.lower()))

        page = Page.active.filter(supertag=supertag).get()
        self.assertEqual(page.tag, tag)
        self.assertEqual(page.body, filedata)
        self.assertEqual(page.title, title)

    def test_create_page_from_not_utf8_txt(self):
        title = 'ЙаЗаголовок'
        tag = supertag = 'testpage'
        filedata = 'русский текст'

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))

        testfile = BytesIO(filedata.encode('windows-1251'))
        testfile.name = 'test.txt'

        data = {
            'title': title,
            'body': '',
            'file': testfile,
        }

        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)

        response = self.client.post(request_url, data=data, content_type=MULTIPART_CONTENT)
        self.assertEqual(200, response.status_code)

        page = Page.active.filter(supertag=supertag).get()
        self.assertEqual(page.tag, tag)
        self.assertEqual(page.body, filedata)
        self.assertEqual(page.title, title)

    def test_clone_page(self):
        tag = 'cloneme'
        new_tag = 'p/t/a'
        body = 'pagebody'

        self.create_page(tag=tag, body=body)
        data = {'destination': new_tag}

        request_url = '{api_url}/{page_tag}/.clone'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=data)

        self.assertEqual(200, response.status_code)
        new_page = Page.active.filter(tag=new_tag).get()
        self.assertEqual(new_page.body, body)

    def test_clone_page_invalid_destination(self):
        tag = 'cloneme'
        new_tag = 'p/t/a'
        body = 'pagebody'

        self.create_page(tag=tag, body=body)
        self.create_page(tag=new_tag, body=body)
        data = {'destination': new_tag}

        request_url = '{api_url}/{page_tag}/.clone'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data=data)

        self.assertEqual(409, response.status_code)

    def test_event_after_page_creation_and_edition(self):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}
        tag = 'ТестПаге'
        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        self.client.post(request_url, data=page_data)

        kw = {'page__tag': tag}
        event = PageEvent.objects.get(**kw)
        self.assertEqual(event.event_type, PageEvent.EVENT_TYPES.create)
        self.assertTrue(event.timeout > event.created_at)  # должно быть на 20 минут

        page_data = {
            'body': 'changed text',
            'notify_immediately': '1',
        }
        self.client.post(request_url, data=page_data)

        event = PageEvent.objects.filter(**kw).order_by('-id')[0]
        self.assertEqual(event.event_type, PageEvent.EVENT_TYPES.edit)

    def test_create_page_with_trailing_dots(self):
        page_data = {'title': 'Title', 'body': 'Body'}
        supertag = 'aaa./bbb.'
        proper_supertag = 'aaa/bbb'

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=supertag)
        r = self.client.post(request_url, data=page_data)
        self.assertEqual(200, r.status_code)
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))
        self.assertTrue(Page.active.filter(supertag=proper_supertag).exists())

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=supertag)
        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertEqual('aaa/bbb', data['redirect_to_tag'])

    def test_create_page_wo_direct_parent(self):
        """
        User must be able to create page without existing parents, i.e.
        create page with supertag 'parent/child' even if page w. supertag 'parent' does not exist.
        """
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag='testpage'))

        request_url = '{api_url}/{page_supertag}'.format(
            api_url=self.api_url, page_supertag='somerandomparent/testpage'
        )
        assert_queries = 64 if not settings.WIKI_CODE == 'wiki' else 23
        with self.assertNumQueries(assert_queries):
            response = self.client.post(request_url, data=page_data)

        self.assertEqual(200, response.status_code)
        self.assertFalse(Page.active.filter(supertag='somerandomparent').exists())
        self.assertTrue(Page.active.filter(supertag='somerandomparent/testpage').exists())

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_watchers(self):
        set_user_setting(self.user, 'new_subscriptions', False)

        def _post_page(tag):
            page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}
            request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
            self.client.post(request_url, data=page_data)

        def _test(page_or_tag, user, is_cluster, counter=1):
            kw = {
                'user': user.username,
            }
            if counter:
                kw['is_cluster'] = is_cluster
            if isinstance(page_or_tag, str):
                kw['page__tag'] = page_or_tag
            else:
                kw['page'] = page_or_tag
            self.assertEqual(PageWatch.objects.filter(**kw).count(), counter)

        # Создаем страницу, автор должен быть подписан

        root_page_tag = 'КорневойПадж'

        _post_page(root_page_tag)
        page = Page.active.get(tag=root_page_tag)
        _test(page, self.user, False)

        # Subscribe cluster's watchers without author
        chapson = self.get_or_create_user('chapson')
        set_user_setting(chapson, 'new_subscriptions', False)
        PageWatch(page=page, user=chapson.username, is_cluster=True).save()

        subpage = root_page_tag + '/NewSubPage1'
        _post_page(subpage)
        _test(subpage, self.user, False)
        _test(subpage, chapson, True)

        # Subscribe cluster's watchers including author
        pw = PageWatch.objects.get(page=page, user=self.user.username)
        pw.is_cluster = True
        pw.save()

        # Пересохраняем существующую страницу.
        # Пользователь не был подписан на страницу,
        # и после изменения остается неподписанным
        subpage = root_page_tag + '/NewSubPage3'
        save_page(subpage, 'hello', user=self.user)

        _post_page(subpage)
        _test(subpage, self.user, False, 0)

    def _test_send_wrong_post_eror(self, page_data, status_code, error, supertag='testpage'):
        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=supertag)
        response = self.client.post(request_url, data=page_data)

        error_data = loads(response.content)['error']
        self.assertEqual(status_code, response.status_code)
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag=supertag))
        if status_code == 409:
            self.assertEqual(error_data['error_code'], 'CLIENT_SENT_INVALID_DATA')
            self.assertTrue(len(error_data['errors'][error]) > 0)

    def test_send_wrong_post(self):
        """
        Разные варианты
        """
        # no body in request
        page_data = {
            'title': 'ЙаЗаголовок',
        }
        self._test_send_wrong_post_eror(page_data, 409, 'body')

        # no title in request
        page_data = {'body': 'Йа %%тельце%%, и я хочу быть в базе'}
        self._test_send_wrong_post_eror(page_data, 409, 'title')

        # edit section of nonexistent page
        page_data = {'section_id': 1, 'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}
        self._test_send_wrong_post_eror(page_data, 409, 'section_id')

        # invalid section of existing page
        page_data = {'section_id': 0, 'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}
        self._test_send_wrong_post_eror(page_data, 409, 'section_id')

        # create page with invalid tag
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}
        self._test_send_wrong_post_eror(page_data, 404, 'tag', 'a/b&c')

    def test_post_to_grid(self):
        """
        Пост на существующий грид возвращает ошибку.
        """
        page = self.create_page(
            page_type=Page.TYPES.GRID,
        )

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        response = self.client.post(request_url, data={})
        self.assertEqual(409, response.status_code)

    def test_head(self):
        """
        Yandex.Server can view any page
        """
        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag='СтраницаАнтона')
        assert_queries = 45 if not settings.WIKI_CODE == 'wiki' else 3
        with self.assertNumQueries(assert_queries):
            response = self.client.head(request_url)
        self.assertEqual(404, response.status_code)

        self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )
        response = self.client.head(request_url)
        self.assertEqual(200, response.status_code)

    def test_no_post_for_yandex_server(self):
        """
        Yandex.Server can't create/edit pages
        """
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag='testpage')
        response = self.client.post(request_url, data=page_data, HTTP_USER_AGENT='yandex.server')

        loads(response.content)['error']
        self.assertEqual(403, response.status_code)
        self.assertRaises(Page.DoesNotExist, lambda: Page.active.get(supertag='testpage'))

    def test_access_to_remove(self):
        page = self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )
        response = self.client.delete(
            '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        )

        json = loads(response.content)
        self.assertTrue('error' in json)
        status_code = response.status_code
        self.assertEqual(status_code, 403)

    def test_edit_page_readonly(self):
        page = self.create_page(tag='testpage', body='page text')
        request_url = f'{self.api_url}/{page.supertag}'

        params = {'is_readonly': True, 'for_cluster': False}
        response = self.client.post(f'{request_url}/.readonly', data=params)
        self.assertEqual(200, response.status_code)

        self.client.login('chapson')
        page_data = {'title': 'some title', 'body': 'some text'}
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(403, response.status_code)

    def test_remove_page(self):
        page = self.create_page(tag='СтраницаАнтона', body='page test')
        assert_queries = 57 if not settings.WIKI_CODE == 'wiki' else 15
        with self.assertNumQueries(assert_queries):
            response = self.client.delete(
                '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
            )

        self.assertEqual(200, response.status_code)
        self.assertFalse(Page.active.filter(supertag='testpage').exists())

    def test_remove_grid(self):
        """
        Успешное удаление грида.
        """
        page = self.create_page(
            page_type=Page.TYPES.GRID,
        )

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        assert_queries = 57 if not settings.WIKI_CODE == 'wiki' else 15
        with self.assertNumQueries(assert_queries):
            response = self.client.delete(request_url)
        self.assertEqual(200, response.status_code)
        self.assertFalse(Page.active.filter(supertag='testpage').exists())

    def test_breadcrumbs_with_deleted_pages(self):
        self.create_page(tag='Тест1', body='вапвап вапвапв вапвапвап')
        page2 = self.create_page(tag='Тест1/Тест2', body='asdfasdfsa')
        page3 = self.create_page(tag='Тест1/Тест2/Тест3', body='werwrwerwer')

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page3.supertag)
        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        page_data = loads(r.content)['data']
        breadcrumbs = page_data['breadcrumbs']
        self.assertEqual(3, len(breadcrumbs))
        self.assertEqual(
            breadcrumbs[1], {'url': '/test1/test2', 'tag': 'Тест1/Тест2', 'is_active': True, 'title': 'Page'}
        )

        self.client.delete('{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page2.supertag))

        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        page_data = loads(r.content)['data']
        breadcrumbs = page_data['breadcrumbs']
        self.assertEqual(3, len(breadcrumbs))
        self.assertEqual(
            breadcrumbs[1], {'url': '/test1/test2', 'tag': 'test1/test2', 'is_active': False, 'title': 'test2'}
        )

    def test_cant_create_page_over_grid(self):
        supertag = 'super/tag'
        improper_supertag = 'super./tag'

        page_data = {'title': 'Title', 'body': 'Body'}

        create_grid(self, supertag, GRID_STRUCTURE, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                name='Boston Legal',
                                number=10,
                            ),
                        )
                    )
                ],
            ),
        )

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=improper_supertag)
        response = self.client.post(request_url, data=page_data)
        self.assertTrue(response.status_code >= 400)

        # Грид не изменился
        request_url = '{api_url}/{page_supertag}/.grid'.format(api_url=self.api_url, page_supertag=supertag)
        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        rows = content['data']['rows']
        self.assertEqual(1, len(rows))
        self.assertEqual('Boston Legal', rows[0][0]['raw'])
        self.assertEqual('10', rows[0][1]['raw'])

    def test_page_in_bookmarks(self):
        set_user_setting(self.user, 'new_favorites', False)

        page = self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )

        response = self.client.get(
            '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        )
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertFalse(data['bookmark'])

        # добавим страницу в закладки
        response = self.client.put(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
            {'folder_name': Folder.FAVORITES_FOLDER_NAME, 'title': 'вторая закладка', 'url': page.absolute_url},
        )
        self.assertEqual(200, response.status_code)

        response = self.client.get(
            '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        )
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertTrue(data['bookmark'])
        self.assertEqual(data['bookmark']['title'], 'вторая закладка')

    def test_not_create_page_without_title(self):
        page_data = {'body': 'body'}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag='testpage')
        r = self.client.post(request_url, data=page_data)

        self.assertEqual(409, r.status_code)

    def test_edit_page_without_title(self):
        page = self.create_page(
            tag='testpage',
            title='old title',
            body='old body',
        )

        page_data = {'body': 'new body'}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        r = self.client.post(request_url, data=page_data)

        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertEqual(data['title'], 'old title')

        page = Page.objects.get(id=page.id)
        self.assertEqual(page.body, 'new body')

    def test_edit_page_with_nice_showpage(self):
        page = self.create_page(
            tag='testpage',
            title='testtitle',
            body='testbody',
        )
        self.create_page(
            tag='another',
            title='another title',
            body='another body',
        )

        page_data = {
            'body': 'new body',
            'showpage': 'another',
        }

        request_url = '{api_url}/{page_supertag}'.format(
            api_url=self.api_url,
            page_supertag=page.supertag,
        )
        r = self.client.post(request_url, data=page_data)

        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']
        self.assertEqual(data['title'], 'another title')

        page = Page.objects.get(id=page.id)
        self.assertEqual(page.body, 'new body')

    def test_edit_page_with_bad_showpage(self):
        page = self.create_page(
            tag='testpage',
            title='testtitle',
            body='testbody',
        )

        page_data = {
            'body': 'new body',
            'showpage': 'nonexistent',
        }

        request_url = '{api_url}/{page_supertag}'.format(
            api_url=self.api_url,
            page_supertag=page.supertag,
        )
        r = self.client.post(request_url, data=page_data)

        self.assertEqual(409, r.status_code)
        errors = loads(r.content)['error']['errors']
        self.assertIn('showpage', errors)

    def test_edit_page_section(self):
        body = '''
Жила была Килла
=== Предисловие

Она была Горилла
=== Глава один

Патисоник любит имбирь!!

=== Конец

Ну вот и все, сказке конец

'''
        page_data = {'title': 'ЙаЗаголовок', 'body': body}
        tag = 'ТестПаге'
        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
        self.client.post(request_url, data=page_data)

        new_second_section = '''== Новая глава Один
Брют жует патисон
Патисон жует Гориллу


'''
        page_data = {
            'title': 'ЙаЗаголовок',
            'body': new_second_section,
            'section_id': '2',
        }
        self.client.post(request_url, data=page_data)

        expected_new_body = '''
Жила была Килла
=== Предисловие

Она была Горилла
== Новая глава Один
Брют жует патисон
Патисон жует Гориллу
=== Конец

Ну вот и все, сказке конец

'''

        page = Page.active.get(tag=tag)
        self.assertEqual(expected_new_body, page.body)

    @celery_eager
    def test_track_links(self):
        def _post_page(tag=None, body=None):
            page_data = {'title': 'ЙаЗаголовок', 'body': body}
            request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
            self.client.post(request_url, data=page_data)

        wiki_host = list(settings.FRONTEND_HOSTS)[0]
        homepage = self.create_page(tag='HomePage')
        str_stag = translit('Страница')
        _post_page(
            body=f'''
((http://{wiki_host}/HomePage oi))
((http://yandex.ru/ Hooray!))
((/Страница которая в тёмном чулане хранится))
/HomePage
HomePage
'''
        )
        self.assertEqual(PageLink.objects.count(), 1)
        self.assertEqual(PageLink.objects.filter(to_page=homepage).count(), 1)
        self.assertEqual(AbsentPage.objects.count(), 1)
        self.assertEqual(AbsentPage.objects.filter(to_supertag=str_stag).count(), 1)

        _post_page(str_stag, body='test')
        self.assertEqual(PageLink.objects.count(), 2)
        self.assertEqual(AbsentPage.objects.count(), 0)

    def test_no_revision_created_if_page_was_not_changed(self):
        tag = 'test-no-revision-created-if-page-was-not-changed'

        def _post_page():
            page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}
            request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)
            self.client.post(request_url, data=page_data)

        _post_page()

        response = self.client.get('/_api/frontend/{0}/.revisions'.format(tag))
        revisions = loads(response.content)['data']['data']
        self.assertEqual(len(revisions), 1)

        _post_page()

        response = self.client.get('/_api/frontend/{0}/.revisions'.format(tag))
        revisions = loads(response.content)['data']['data']
        self.assertEqual(len(revisions), 1)

    def test_redirect_page(self):
        redirect1 = self.create_page(tag='редирект 1')
        redirect2 = self.create_page(tag='редирект 2')
        target = self.create_page(tag='цель')

        redirect1.redirects_to = redirect2
        redirect1.save()

        redirect2.redirects_to = target
        redirect2.save()

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=redirect1.supertag)

        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        redirect_json = {
            'redirect_to_tag': target.supertag,
            'page_type': 'article',
        }

        self.assertEqual(redirect_json, r.data['data'])

    def test_noredirect(self):
        # Если в запросе присутствует GET параметр noredirect,
        # то редирект игнорируется и отдаются данные страницы-редиректа.

        redirect = self.create_page(tag='редирект', body='тело редиректа')
        target = self.create_page(tag='цель', body='тело цели')

        redirect.redirects_to = target
        redirect.save()

        request_url = '{api_url}/{page_supertag}?noredirect=true'.format(
            api_url=self.api_url,
            page_supertag=redirect.supertag,
        )

        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        self.assertEqual(redirect.supertag, r.data['data']['supertag'])

    @override_settings(LIMIT__WIKI_TEXT_FOR_PAGE__BYTES=5)
    def test_max_body_size(self):
        self.create_page(tag='page')

        page_data = {'title': 'Заголовок', 'body': 'Килла'}
        request_url = '{api_url}/page'.format(api_url=self.api_url)

        r = self.client.post(request_url, data=page_data)
        self.assertEqual(409, r.status_code)
        self.assertEqual('Maximum page size 0 Kb exceeded', r.data['error']['errors']['body'][0])

        page_data = {'title': 'Заголовок', 'body': 'Killa'}
        request_url = '{api_url}/page'.format(api_url=self.api_url)

        r = self.client.post(request_url, data=page_data)
        self.assertEqual(200, r.status_code)

    def test_edit_new_wf_flag(self):
        page = self.create_page(
            tag='testpage',
            title='title',
            body='body',
        )

        page_data = {
            'body': 'new body',
        }

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=page.supertag)
        r = self.client.post(request_url, data=page_data)

        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertTrue(data['with_new_wf'])


class APIPagesHandlerUnitTest(BaseApiTestCase):
    def _get_request_stub(self, page_is_redirect, get_params=None):
        return stub(
            method='GET',
            page=stub(id=0, has_redirect=lambda *args: page_is_redirect),
            user=stub(id=0, is_anonymous=lambda *args: False),
            GET=get_params or {},
        )

    def test_access_granted_on_redirect(self):
        view = PageView()
        view.request = self._get_request_stub(page_is_redirect=False)
        # для нередиректов
        with patch('wiki.api_core.framework.PageAPIView.check_page_access', raiser(UserHasNoAccess)):
            self.assertRaises(UserHasNoAccess, lambda: view.check_page_access())

        # для редиректов
        view = PageView()
        view.request = self._get_request_stub(page_is_redirect=True)
        with patch('wiki.api_core.framework.PageAPIView.check_page_access', raiser(UserHasNoAccess)):
            self.assertEqual(None, view.check_page_access())

        # для редиректов с GET параметром noredirect
        view = PageView()
        view.request = self._get_request_stub(page_is_redirect=True, get_params={'noredirect': ''})
        with patch('wiki.api_core.framework.PageAPIView.check_page_access', raiser(UserHasNoAccess)):
            self.assertRaises(UserHasNoAccess, lambda: view.check_page_access())


class WasModifiedTestCase(BaseApiTestCase):
    def test_it_says_false_on_old_page(self):
        from wiki.api_core.not_modified import was_modified

        page = stub(modified_at=make_aware_utc(datetime(2000, 8, 8, 0, 0, 0)))
        request = stub(META={'HTTP_IF_MODIFIED_SINCE': 'Fri, 01 Aug 2013 07:00:12 GMT'})
        self.assertFalse(was_modified(request, page))

    def test_is_says_true_on_new_page(self):
        from datetime import datetime

        from wiki.api_core.not_modified import was_modified

        page = stub(modified_at=make_aware_utc(datetime(2015, 1, 1, 0, 0, 0)))
        request = stub(META={'HTTP_IF_MODIFIED_SINCE': 'Fri, 08 Aug 2014 07:00:12 GMT'})
        self.assertTrue(was_modified(request, page))

    def test_it_says_true_when_no_header(self):
        from datetime import datetime

        from wiki.api_core.not_modified import was_modified

        page = stub(modified_at=datetime(2015, 1, 1, 0, 0, 0))
        request = stub(META={})
        self.assertTrue(was_modified(request, page))


class RawPageTestCase(BaseApiTestCase):
    def setUp(self):
        super(RawPageTestCase, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_simple(self):
        page = self.create_page(tag='page', title='Засоник', body='Килла **Годжилла** 1. Патисоник')
        response = self.client.get('/_api/frontend/page/.raw')
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual('Килла **Годжилла** 1. Патисоник', data['body'])
        self.assertEqual('Засоник', data['title'])
        self.assertEqual(page.get_page_version(), data['version'])

    def test_follow_redirects(self):
        page0 = self.create_page(supertag='page', tag='page', title='page0', body='1')
        page1 = self.create_page(supertag='page2', tag='page2', title='page0', body='2')
        page2 = self.create_page(supertag='page3', tag='page3', title='page0', body='3')

        page0.redirects_to = page1
        page0.save()
        page1.redirects_to = page2
        page1.save()

        response = self.client.get('/_api/frontend/page/.raw?follow_redirects=1')
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual('3', data['body'])

    def test_raw_on_grid(self):
        page0 = self.create_page(supertag='page', tag='page', title='page0', body='1')
        page1 = self.create_page(supertag='page2', tag='page2', title='page0', body='2')

        page0.redirects_to = page1
        page0.save()
        page1.page_type = Page.TYPES.GRID
        page1.save()

        response = self.client.get('/_api/frontend/page/.raw?follow_redirects=1')
        self.assertEqual(409, response.status_code)
        data = loads(response.content)['error']
        self.assertEqual(data['error_code'], PageTypeMismatch.error_code)

    def test_revision(self):
        page = self.create_page(tag='page', title='Засоник', body='Невероятный Жираф')
        first_version = page.get_page_version()
        save_page(page, 'Измененное тело Жирафа', 'Измененная голова Жирафа')
        response = self.client.get('/_api/frontend/page/.raw?revision=%s' % first_version)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual('Невероятный Жираф', data['body'])
        self.assertEqual('Измененная голова Жирафа', data['title'])  # А заголовок берется из последней ревизии
        self.assertEqual(page.get_page_version(), data['version'])

    def test_section(self):
        body = '''
Жила была Килла
=== Предисловие

Она была Горилла
=== Глава один

Патисоник любит имбирь!!

=== Конец

Ну вот и все, сказке конец

'''
        self.create_page(tag='page', title='Засоник', body=body)
        response = self.client.get('/_api/frontend/page/.raw?section_id=2')

        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        second_section = '''=== Глава один

Патисоник любит имбирь!!

'''
        self.assertEqual(second_section, data['body'])

    def test_not_modified(self):
        self.create_page(tag='page', modified_at=now_for_tests())
        assert_queries = 48 if not settings.WIKI_CODE == 'wiki' else 6
        with self.assertNumQueries(assert_queries):
            response = self.client.get(
                '/_api/frontend/page/.raw', HTTP_IF_MODIFIED_SINCE='Fri, 08 Aug 9014 11:09:43 GMT'
            )
        self.assertEqual(304, response.status_code)
        self.assertEqual(b'', response.content)

    def test_as_is_from_storage(self):
        stub_response = HttpResponse('', 'application/json')
        stub_response['X-Accel-Redirect'] = 'my storage id'
        stub_response['X-Accel-Buffering'] = 'no'
        with patch('wiki.utils.xaccel_header.accel_redirect_to_download', lambda *args: stub_response):
            self.create_page(tag='page', body='джимми-джимми! адьо-адьо')
        response = self.client.get('/_api/frontend/page/.raw?as_is=yes')
        self.assertEqual(200, response.status_code)
        self.assertEqual(b'', response.content)
        self.assertEqual(response['Content-Type'], 'application/json')
        self.assertIn('X-Accel-Redirect', stub_response)
        self.assertEqual(stub_response['X-Accel-Redirect'], 'my storage id')
        self.assertEqual(stub_response['X-Accel-Buffering'], 'no')

    def test_get_grid(self):
        page = self.create_page(
            page_type=Page.TYPES.GRID,
        )

        response = self.client.get(
            '{api_url}/{page_supertag}/.raw'.format(api_url=self.api_url, page_supertag=page.supertag)
        )

        self.assertEqual(409, response.status_code)


class RawPageParamsValidatorUnitTest(BaseApiTestCase):
    validator_cls = RawPageParamsValidator

    def test_no_required_params(self):
        validator = self.validator_cls({})
        self.assertTrue(validator.is_valid())

    def test_defaults(self):
        validator = self.validator_cls({})
        self.assertEqual(
            validator.clean_or_raise(),
            {'as_is': False, 'section_id': None, 'revision': None, 'follow_redirects': False},
        )

    def test_as_is_yes(self):
        validator = self.validator_cls({'as_is': 'yes'})
        cleaned = validator.clean_or_raise()
        self.assertTrue(cleaned['as_is'])

    def test_as_is_invalid(self):
        validator = self.validator_cls({'as_is': 'WHATEVER'})
        self.assertRaises(validator.validation_error_cls, validator.clean_or_raise)

    def test_section_id_ok(self):
        validator = self.validator_cls({'section_id': '666'})
        cleaned = validator.clean_or_raise()
        self.assertEqual(cleaned['section_id'], 666)

    def test_section_invalid_integer(self):
        validator = self.validator_cls({'section_id': 'NOT_INTEGER'})
        self.assertRaises(validator.validation_error_cls, validator.clean_or_raise)

    def test_section_id_invalid(self):
        validator = self.validator_cls({'revision': 100500})
        self.assertRaises(validator.validation_error_cls, validator.clean_or_raise)

    def test_multiple_params_given(self):
        validator = self.validator_cls(
            {
                'as_is': 'yes',
                'section_id': '2',
            }
        )
        self.assertRaises(validator.validation_error_cls, validator.clean_or_raise)


class RawPageParamsValidatorIntegrationTest(BaseApiTestCase):
    validator_cls = RawPageParamsValidator

    def test_revision_ok(self):
        page = self.create_page()
        revision = Revision.objects.create_from_page(page=page)
        validator = self.validator_cls({'revision': revision.id})
        cleaned = validator.clean_or_raise()
        self.assertEqual(cleaned['revision'], revision)


class PageEditionSerializerTest(BaseApiTestCase):
    @patch('wiki.api_frontend.serializers.pages.who_edited_the_page', lambda *args: '')
    def test_validate_version(self):
        serializer = PageEditionSerializer()
        page = stub(get_page_version=lambda *args: '1')
        serializer.root._context = {'request': stub(page=page)}
        self.assertEqual('1', serializer.validate_version('1'))

        page = stub(get_page_version=lambda *args: '2')
        serializer.root._context = {'request': stub(page=page)}
        self.assertRaises(serializers.ValidationError, lambda: serializer.validate_version('1'))
