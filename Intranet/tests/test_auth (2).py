# coding: utf-8

import vcr
import yatest
import blackbox
import unittest
from mock import Mock, patch

from intranet.hidereferer.src import cache
from intranet.hidereferer.src.auth import check_authentification, NotAuthentificatedError, \
    TwoCookiesRequiredError, NeedResetCookieError
from intranet.hidereferer.src.responses import MetaRedirectResponse, ImageResponse, \
    InternalErrorResponse, Redirect302Response
from intranet.hidereferer.src.application import ye_olde_cheshire_cheese


SESSION_ID = '3:1398674527.5.0.1398411719000:mc4tBQ:60.0|1120000000008011.0.2|55405.716002.GN2ElXcQicSIsrkmz-DZdGJ_Cxo'
# пример реального значения sessionid для короткоживущей куки,
# чтобы проверить правильно ли мы это парсим
SHORT_LIVED_SESSION_ID = '3:1398674527.4.0.1398411719000:mc4tBQ:60.0|' \
                         '1120000000008011.0.2|55405.716002.GN2ElXcQicSIsrkmz-DZdGJ_Cxo'
SESSION_ID2 = '3:1398674527.5.0.1398411719000:mc4tBQ:60.0|1120000000008011.0.2|55405.716002.GN2ElXcQicSIsrkmz-DZdGJ_Cxo'
OLD_SESSION_ID = 'OLD_SESSION'


class MockBlackbox(object):
    url = 'mock-blackbox'

    def __init__(self, **params):
        self.params = params

        defaults = dict(valid=True, uid=1,
                        lite_uid='', fields={'login': 'test_login',
                                             'login_rule': None},
                        redirect=False, emails='', default_email='test_login@yandex-team.ru',
                        status='Some status', error=None, new_session=None, new_sslsession=None,
                        secure=False)

        self.result = blackbox.odict(defaults, **self.params)

        self.userinfo = Mock(return_value=self.result)

    def _check_login_rule(self, *args, **kwargs):
        return False

    def sessionid(self, cookie, userip, http_host, dbfields=None, sslsessionid=None):
        if cookie.startswith('"'):  # fail quoted cookie check, as in read bb
            self.result.valid = False

        self.result.secure = bool(sslsessionid)
        if cookie == OLD_SESSION_ID:
            self.result.redirect = True

        return self.result


class TestAuth(unittest.TestCase):
    code_200 = MetaRedirectResponse._code
    code_302 = Redirect302Response._code

    @property
    def vcr(self):
        path = yatest.common.source_path('intranet/hidereferer/vcr_cassettes')
        return vcr.VCR(
            cassette_library_dir=path,
        )

    def mock_200_callback(self, *args, **kwargs):
        self.assertEqual(args[0], self.code_200)

    def mock_302_callback(self, *args, **kwargs):
        self.assertEqual(args[0], self.code_302)

    def setUp(self):
        self.mock_request = {
            'HTTP_ACCEPT_LANGUAGE': 'ru,en;q=0.8',
            'HTTP_REFERER': 'http://wiki.yandex-team.ru/yandex.eda/re/menu',
            'QUERY_STRING': '',
            'HTTP_HOST': 'h.yandex-team.ru',
            'REQUEST_URI': '/',
            'HTTP_X_FORWARDED_PROTO': 'https'
        }
        self.mock_cache_dict = {}
        cache.mc = Mock()
        cache.mc.set = lambda key, val, timeout: self.mock_cache_dict.update({key: val})
        cache.mc.get = lambda key: self.mock_cache_dict.get(key)

    def test_anonymous(self):
        # h.yandex-team.ru - login required
        self.mock_request['HTTP_HOST'] = 'h.yandex-team.ru'
        ye_olde_cheshire_cheese(self.mock_request, self.mock_302_callback)

    def test_no_cookie(self):
        self.assertRaises(NotAuthentificatedError, check_authentification, self.mock_request)

    def test_old_cookie(self):
        with patch('intranet.hidereferer.src.auth.blackbox.XmlBlackbox', MockBlackbox):
            self.mock_request['HTTP_COOKIE'] = 'Session_id=%s' % SESSION_ID
            self.assertTrue(check_authentification(self.mock_request))

            self.mock_request['HTTP_COOKIE'] = 'Session_id=%s' % OLD_SESSION_ID
            self.assertRaises(NeedResetCookieError, check_authentification, self.mock_request)

    def test_short_ttl_cookie(self):
        with patch('intranet.hidereferer.src.auth.blackbox.XmlBlackbox', MockBlackbox):
            self.mock_request['HTTP_COOKIE'] = 'Session_id=%s;sessionid2=%s' % (SHORT_LIVED_SESSION_ID, SESSION_ID2)
            self.assertTrue(check_authentification(self.mock_request))

    def test_quoted_cookie(self):
        """
        тест имени WIKI-6678
        """
        with patch('intranet.hidereferer.src.auth.blackbox.XmlBlackbox', MockBlackbox):
            self.mock_request['HTTP_COOKIE'] = 'Session_id="%s";sessionid2="%s";' % (SESSION_ID, SESSION_ID2)
            self.assertTrue(check_authentification(self.mock_request))

    def test_white_list(self):
        with self.vcr.use_cassette('test_white_list.yaml'):
            self.mock_request['QUERY_STRING'] = 'https%3A%2F%2Fvk.com%2Fyandex.academy'
            ye_olde_cheshire_cheese(self.mock_request, self.mock_200_callback)

    def test_no_url(self):
        with patch('intranet.hidereferer.src.auth.blackbox.XmlBlackbox', MockBlackbox):
            self.mock_request['HTTP_COOKIE'] = 'Session_id="%s";sessionid2="%s";' % (SESSION_ID, SESSION_ID2)
            ye_olde_cheshire_cheese(self.mock_request, self.mock_200_callback)
