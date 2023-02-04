
from django.conf import settings
from django.contrib import messages
from django.http import HttpRequest, HttpResponseRedirect

from wiki.actions import UnknownActionError, get_action_class
from wiki.actions.classes.base_action_deprecated import OldDjangoWikiBaseAction, SimpleWikiAction, WikiActionWithPOST
from intranet.wiki.tests.wiki_tests.common.request_factory import prepare_request
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import BaseOldActionTestCase, HttpOldActionTestCase


class EmulatorWfParams(object):
    def __init__(self, _dict, _list):
        self.dict = _dict
        self.list = ((None, p) for p in _list)


class SimpleActionOldDjango(OldDjangoWikiBaseAction):
    template_name = 'actions/_base_action.html'
    default_params = {'chuck': 'geck'}

    def get_context_data(self):
        self.add_error('killa!')


class BaseOldActionTest(BaseOldActionTestCase):
    def test_init_by_ctx(self):
        action = OldDjangoWikiBaseAction(request=object(), killa='gorilla')
        self.assertEqual(action.killa, 'gorilla')

    def test_default_params(self):
        action = SimpleActionOldDjango()
        self.assertEqual(action.params.get('chuck'), 'geck')

    def test_render(self):
        # look into actions/templates/actions/baseaction.html
        action = OldDjangoWikiBaseAction({'word': 'Hello', 'action_user': 'thasonic'}, object())
        action.template_name = 'actions/_base_action.html'
        self.assertEqual(action.render(), 'Hello, thasonic!')

    def test_parsing_params(self):
        action = OldDjangoWikiBaseAction({'a': 'a', 'b': 'b'}, **{'request': object(), 'killa': 'gorilla'})
        self.assertEqual(action.killa, 'gorilla')
        self.assertEqual(action.params['a'], 'a')
        self.assertEqual(action.params['b'], 'b')

    def test_parsing_wf_params(self):
        params = EmulatorWfParams({'a': 'a', 'b': 'b'}, ['0', '1'])
        action = OldDjangoWikiBaseAction(params=params, request=object())
        self.assertEqual(action.params['a'], 'a')
        self.assertEqual(action.params['b'], 'b')
        self.assertEqual(action.ordered_params[0], '0')
        self.assertEqual(action.ordered_params[1], '1')

    def test_error(self):
        action = SimpleActionOldDjango()
        res = action.render()
        self.assertEqual(action.params['errors'], ['killa!'])
        self.assertIn('<div class="error">', res)


class SimpleDynamicActionWithPOST(WikiActionWithPOST):
    template_name = 'actions/_base_action.html'

    def handle(self):
        if self.params.get('valid', True):
            self.add_message('killa')
            self.add_global_message('chukilla')
            return self.redirect()


class DynamicActionTest(HttpOldActionTestCase):
    def test(self):
        action = SimpleDynamicActionWithPOST(
            params={
                'word': 'Hello',
                '__count__': 1,
            },
            request=self.request,
            page_path=self.page.supertag,
        )
        self.assertIsInstance(action.request, HttpRequest)
        if settings.IS_INTRANET:
            self.assertEqual(action.render(), '<!--dynamic action-->Hello, thasonic!<!--/dynamic action-->')
        else:
            self.assertEqual(action.render(), '<!--dynamic action-->Hello,   (thasonic)!<!--/dynamic action-->')

    def test_success(self):
        action = SimpleDynamicActionWithPOST(
            params={
                'valid': True,
                '__count__': 1,
            },
            request=self.request,
            page_path=self.page.supertag,
        )
        self.assertIsInstance(action.handle(), HttpResponseRedirect)

    def test_error(self):
        action = SimpleDynamicActionWithPOST(
            params={
                'valid': False,
                '__count__': 1,
            },
            request=self.request,
            page_path=self.page.supertag,
        )
        self.assertEqual(action.handle(), None)

    def test_message(self):
        action = SimpleDynamicActionWithPOST(
            params={
                'valid': True,
                '__count__': 1,
            },
            request=self.request,
            page_path=self.page.supertag,
        )
        action.handle()
        self.assertEqual(action.get_messages(), ['killa'])
        m = next(messages.get_messages(self.request).__iter__())
        self.assertEqual(m.message, 'chukilla')


class HttpActionTest(HttpOldActionTestCase):
    urls = 'wiki.actions.tests.urls'
    action_name = 'test'

    def test(self):
        action = SimpleWikiAction(
            params={
                'word': 'Hello',
                '__count__': 1,
            },
            request=self.request,
            page_path=self.page.supertag,
        )

        request = prepare_request(method='GET', user=self.user_thasonic, page=self.page)
        response = action.dispatch(request, tag=self.page.supertag)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content.strip(), b'Hello!')

    def test_parsing_params(self):
        action = SimpleWikiAction(
            params={
                'a': 'a',
                'b': 'b',
                2: 2,
                1: 1,
                0: 0,
                'AB': 'ab',
                '__count__': 1,
            },
            request=self.request,
            page_path=self.page.supertag,
        )

        request = prepare_request(method='GET', page=self.page)
        # вызывает разбор переданных параметров
        action.dispatch(request, tag=self.page.supertag)

        self.assertEqual(action.params['a'], 'a')
        self.assertEqual(action.params['b'], 'b')
        self.assertEqual(action.params['AB'], 'ab')


class OldActionClassLookupTestCase(BaseOldActionTestCase):
    def test_known_not_in_wf(self):
        get_action_class('tree')

    def test_unknown(self):
        try:
            get_action_class('unknownaction')
            self.fail()
        except UnknownActionError:
            pass

    def test_with_error(self):
        # проверяем, что эксепшн, отличный от ImportError про отсутствующий модуль форматера
        # (с конкретного вида текстом ошибки) вылетит наверх
        try:
            self.fail()
        except BaseException as e:
            self.assertTrue(e is not UnknownActionError)
            pass
