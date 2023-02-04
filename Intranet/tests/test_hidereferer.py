# -*- coding: utf-8 -*-


import unittest
from datetime import timedelta, datetime as dt
from urllib.parse import quote
from urllib.parse import urlparse

import mock
import vcr
import yatest

from intranet.hidereferer.src import cache
from intranet.hidereferer.src.app import is_trusted_host, get_external_resource_redirect, response_strategy, \
    prepare_url, get_destination_url
from intranet.hidereferer.src.application import ye_olde_cheshire_cheese
from intranet.hidereferer.src.cache import WasInCacheException
from intranet.hidereferer.src.ext_urls import get_external_url
from intranet.hidereferer.src.language import detect_language
from intranet.hidereferer.src.responses import (
    MetaRedirectResponse,
    ImageResponse,
    Redirect302Response,
)

# обычный юзер-кейс

good_urls = [
    '',
    'http://ya.ru/future/?type=peace&speed=normal#bonuses',
    'http://images.yandex1.ru/yandsearch?text=%D0%BA%D0%B0%D1%80%D1%82%D0%B0%20%D1%81%D0%B5%D1%82%D0%B8&rpt=simage&img_url=forums.drom.ru%2Fattachment.php%3Fattachmentid%3D1112644%26stc%3D1%26d%3D1279048865&p=5',
    'http://wiki.yandex-team.ru/Market/Projects/Checkout/Address/API/checkoverwrite?checkOverWrite=1&previous=2010-12-23+15%3A07%3A50&isOldRevision=',
    'http://wiki.yandex-team.ru/%D0%90%D0%BB%D0%B5%D0%BA%D1%81%D0%B0%D0%BD%D0%B4%D1%80%D0%91%D0%BE%D0%BB%D1%85%D0%BE%D0%B2%D0%B8%D1%82%D1%8F%D0%BD%D0%BE%D0%B2',
    'http://wiki.yandex-team.ru/VadimMakishvili/sieve/files?et=inbox.gif',
    'http://wiki.yandex-team.ru/jandeikspoisk/klassifikacijasajjtovistranic/tematicheskaja/linki/opisanieskriptov/.explainlinks.json',
    'http%3A%2F%2Fxn-----clcksaplxf6byd3cyb.xn--p1ai%2F',  # пиши-код-блять.рф quoted 1 times
    'https%3A%2F%2Ftwitter.com%2Fpostgoodism%2Fstatus%2F354249200852140032',
    'http%3A%2F%2Fwww.npfraiffeisen.ru%2Fabout%2Factivities%2F',
    'www.advisormsk.ru',
    'golem.yandex-team.ru/fwparser2.sbml?query_mode=hardcore&group_mode=AND&sort_by_relevance=0&from=*&to=*&q=base.music.yandex-team.ru#@srv_94_muzyika@',
    '%252525252F%252525252Fwww.google.ru%252525252Fsearch%252525253Fie%252525253DUTF-8%2525252526hl%252525253Dru%2525252526q%252525253Dsfsdfsdf=&q=%D0%BC%D0%BE%D0%B6%D0%BD%D0%BE%20%D0%BB%D0%B8'
    # запрос заэнкожен 5 раз
]

# ссылки с элементами юникода
good_unicode_urls = [
    'http://президент.рф/putin/crab?params=' + quote('мясо - хорошо'.encode('utf-8')),
    'http://президент.рф/Путин/crab?params=' + quote('мясо - хорошо'.encode('utf-8')),
    'http://www.liveinternet.ru/stat/ru/searches.html?period=month&slice=kz&id=4&id=5&id=13&id=22&id=8&show=перестроить+график&per_page=10&report=searches.html%3Fslice%3Dkz%3Bperiod%3Dmonth'
    'ru.wikipedia.org/wiki/Союз_Советских_Социалистических_Республик'
]

# нас пытаются нагнуть
bad_urls = [
    'http%3A%2F%2Fremote.host.ru/<script>alert(/1/)</script>',
    "http%3A%2F%2Fremote.host.ru/<script>window.location='http://ya.ru'</script>",
    'h<script>alert(1);</script>ttp://yandex.ru</',
    'http%3A%2F%2Frem\'ote.host.ru/<script>window.location=\'http://ya.ru\'</script>',
    '%253Cscript%253Ealert("1");</script>',
    '<script>alert(/1/)</script>',
    'https://ya.ru:13%22%3E%3Cimg%20src=x%20onerror=prompt(1)%3E%0A%0Afff%3Csvg/onload=alert(1)',
    'http%3A%2F%2F%25D0%25BF%25D0%25B8%25D1%2588%25D0%25B8-%25D0%25BA%25D0%25BE%25D0%25B4-%25D0%25B1%25D0%25BB%25D1%258F%25D1%2582%25D1%258C.%25D1%2580%25D1%2584%2F',
    # пиши-код-блять.рф quoted 2 times
]


def mock_resp_callback(*args, **kwargs):
    # print args, kwargs
    pass


class TestHidereferer(unittest.TestCase):
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

    def mock_200_or_302_callback(self, *args, **kwargs):
        self.assertTrue(args[0] in (self.code_200, self.code_302))

    def setUp(self):
        self.mock_request = {
            'HTTP_ACCEPT_LANGUAGE': 'ru,en;q=0.8',
            'HTTP_REFERER': 'http://wiki.yandex-team.ru/yandex.eda/re/menu',
            'QUERY_STRING': '',
            'HTTP_HOST': 'h.yandex-team.ru',
            'REQUEST_URI': '',
        }
        self.mock_cache_dict = {}
        cache.mc = mock.Mock()
        cache.mc.set = lambda key, val, timeout: self.mock_cache_dict.update({key: val})
        cache.mc.get = lambda key: self.mock_cache_dict.get(key)

    def test_service_durability(self):
        """
        test if server is able to digest all kinds of links
        """
        for link in good_urls + [link.encode('utf-8') for link in good_unicode_urls] + bad_urls:
            self.mock_request['QUERY_STRING'] = quote(link)

            ye_olde_cheshire_cheese(self.mock_request, mock_resp_callback)

    # def test_bad_urls(self):
    #     code_500 = InternalErrorResponse._code
    #
    #     def mock_500_callback(*args, **kwargs):
    #         self.assertEqual(code_500, args[0])
    #
    #     for link in bad_urls:
    #
    #         self.mock_request['QUERY_STRING'] = quote(link)
    #         print ye_olde_cheshire_cheese(self.mock_request, mock_500_callback)

    def test_good_urls(self):

        for link in good_urls + [link.encode('utf-8') for link in good_unicode_urls]:
            self.mock_request['QUERY_STRING'] = quote(link)

            ye_olde_cheshire_cheese(self.mock_request, self.mock_200_or_302_callback)

    def test_cache(self):
        with self.vcr.use_cassette('test_cache.yaml'):
            # send to cache
            get_external_resource_redirect('http://ya.ru', self.mock_request)
            self.assertEqual('hr_http://ya.ru', list(self.mock_cache_dict.keys())[0])
            self.assertTrue(list(self.mock_cache_dict.values())[0][1])

            # check that next response if from cache
            self.assertRaises(WasInCacheException, lambda: get_external_url('http://ya.ru', self.mock_request))

            # make cache expired
            self.mock_cache_dict['hr_http://ya.ru'] = (dt.now() - timedelta(days=365), True, 'html')
            self.assertEqual(200, get_external_url('http://ya.ru', self.mock_request).code)

            # make image content type in cache
            self.mock_cache_dict['hr_http://ya.ru'] = (dt.now(), True, 'image/jpg')
            self.assertEqual(200, get_external_url('http://ya.ru', self.mock_request).code)

    def test_no_referer(self):
        link = 'http://lurkmore.to'
        with self.vcr.use_cassette('test_no_referer.yaml'):
            result = response_strategy(link, self.mock_request)

            self.assertTrue(isinstance(result, MetaRedirectResponse))

            if self.mock_request.get('HTTP_REFERER', None):
                del self.mock_request['HTTP_REFERER']

            result = response_strategy(link, self.mock_request)

            self.assertEqual(type(result), Redirect302Response)

    def test_trusted_host(self):
        # ok for trusted subdomains
        self.assertTrue(is_trusted_host(urlparse('http://trust.yandex.ru')))
        self.assertTrue(is_trusted_host(urlparse('https://www.trust.yandex.ru')))
        # not trusted domain itself
        self.assertFalse(is_trusted_host(urlparse('http://yandex.ru')))
        # nor leak domain
        self.assertFalse(is_trusted_host(urlparse('http://yabs.yandex.ru')))

        # not a domain at all
        self.assertRaises(Exception, is_trusted_host(urlparse('http://yasdsaodfs')))

    def test_ticket_WIKI_4667(self):
        """
        too long urls may crash uwsgi cause of request block size limit 4096 bytes by default
        """
        link = 'http%3A%2F%2Frasp.morelia.yandex.ru%2Fapi%2Fwizard%2Fsearch%2F%3F%26%26ag0%3Drasp_search%26brand%3D%26express%3D1%26from%3D%25D0%25BC%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26gta%3DAuraDocLogAuthor%26gta%3DDocLen%26gta%3DIsFake%26gta%3D_Erf_Language%26gta%3DEngLang%26gta%3DLongQuery%26gta%3DQueryURLClicksPCTR%26gta%3DQueryDOwnerClicksPCTR%26gta%3DQueryURLClicksFRC%26gta%3DQueryDOwnerClicksFRC%26gta%3DTRLRQuorumSyn%26gta%3DTRLRQuorumLemma%26number%3D%26rearr%3DRabota_off%3Bad%253D1%3Ball_regions%253D213%252C1%252C3%252C225%252C10001%252C10000%3Bdc%253D1%3Bdup%253D1%3Bfav_lang%253D0%3Bgeooriginal%253D213%3Bnoperson%253D1%3Bpersonalizeproduction%3Bscheme_blender%2Fenabled%253D1%26relev%3DFEfe%253D1.12112e%252B06%3BFEfsm%253D0%3BFEfsml%253D3%3Bavgl%253D0.847368%3Bcl%253D1%3Bcm2%253D0.00205273%3Bcm%253D0.130435%3Bct%253D324-97%3Bdi2%253D5.33258%3Bdi%253D43.4837%3Bdnorm%253D%25D0%25B0%25D1%258D%25D1%2580%25D0%25BE%25D1%258D%25D0%25BA%25D1%2581%25D0%25BF%25D1%2580%25D0%25B5%25D1%2581%25D1%2581%2B%25D0%25B2%25D0%25BD%25D1%2583%25D0%25BA%25D0%25BE%25D0%25B2%25D0%25BE%2B%25D0%25BC%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%3Bfe%253D12.365%3Bforum%253D-1.87283%3Bfsm%253D2%3Bfsml%253D3%3Bgaddr%253D0.849928%3Bgc%253D213%3Bgeov%253D0.6156%3Bho%253D6%3Bhodi%253D0.057285%3Biad%253D1%3Bil%253D0%3Bipe%253D-0.001%3Bisgeo%253D1.000%3Bisorg%253D0.951%3Bmum%253D256%3Bmut%253D0%3Bnavmx%253D0.582639%3Bnorm%253D%25D0%25BC%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%2B%25D0%25B2%25D0%25BD%25D1%2583%25D0%25BA%25D0%25BE%25D0%25B2%25D0%25BE%2B%25D0%25B0%25D1%258D%25D1%2580%25D0%25BE%25D1%258D%25D0%25BA%25D1%2581%25D0%25BF%25D1%2580%25D0%25B5%25D1%2581%25D1%2581%3Bpo%253D92%3Bprel%253D1%3Bqc%253D16%3Bqlu%253D1%3Bqmpl%253Dru%3Bqr2r%253D0.1%3Bqr2u%253D0.9%3Bqr2v%253D0%3Bqrr%253D0.1%3Bqru%253D0.9%3Bqrv%253D0%3Bqsegments%253D2%257C1%3Bqt%253Dru_nloc%3Bre%253D20054%3Bslow%253D0.00%3Bsyq%253D0.9991069727%3Bth3561%253D0.0865%3Bth3973%253D0.0559%3Btvm%253D0.0296511%3Buil%253Dru%3Butq%253D%25D0%25BC%25D0%25BE%25D1%2581%25D0%25BA%25D0%25BE%25D0%25B2%25D1%2581%25D0%25BA%25D0%25B8%25D0%25B9%2Bmoscow%2Bmoskva%2Bmoskwa%2Bvnukovo%2Baeroexpress%2B%25D0%25B0%25D1%258D%25D1%2580%25D0%25BE%2B%25D1%258D%25D0%25BA%25D1%2581%25D0%25BF%25D1%2580%25D0%25B5%25D1%2581%25D1%2581%3Bwmaxone%253D0.063%3Bwminone%253D1.000%3Bydngi%253D0.4847638607%3Bzsset%253DeNoz1DNQSM5ITc5WKM4sSVUoT00C0xf2XNgIIvZd2HRhg8KFfRebLjZc2Hphy4UNF3ZdbLqwA8gDSwHFexQudlzYcWHvhQ0A2bIs3g%253D%253D%26relev%3D0.5%3Brelevgeo%253D225%3Bgeochanged%253D1%26reqid%3D1354638592-80923911793-leon32%26snip%3Dreps%253Dall_on%3Buil%253Dru%26t_type%3Daeroex%26text%3D%26to%3DVKO%26user_request%3D%25D0%25BC%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%2B%25D0%25B2%25D0%25BD%25D1%2583%25D0%25BA%25D0%25BE%25D0%25B2%25D0%25BE%2B%25D0%25B0%25D1%258D%25D1%2580%25D0%25BE%25D1%258D%25D0%25BA%25D1%2581%25D0%25BF%25D1%2580%25D0%25B5%25D1%2581%25D1%2581%26user_request%3D%25D0%25BC%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%2B%25D0%25B2%25D0%25BD%25D1%2583%25D0%25BA%25D0%25BE%25D0%25B2%25D0%25BE%2B%25D0%25B0%25D1%258D%25D1%2580%25D0%25BE%25D1%258D%25D0%25BA%25D1%2581%25D0%25BF%25D1%2580%25D0%25B5%25D1%2581%25D1%2581%26ms%3Dproto%26p%3D0%26ag%3Drasp_search%26g%3D1.rasp_search.10.1.-1.0.0.-1.rlv.0..0.0%26mslevel%3D1%252001354638592957771%26json%3D1'
        code_302 = Redirect302Response._code

        def mock_200_callback(*args, **kwargs):
            self.assertEqual(args[0], code_302)

        self.mock_request['QUERY_STRING'] = link

        ye_olde_cheshire_cheese(self.mock_request, mock_200_callback)

    def test_ticket_WIKI_4659(self):
        """
        urls with unicode in params must be threated nicely
        """
        link = 'http%3A%2F%2Fwww.liveinternet.ru%2Fstat%2Fru%2Fbrowsers.gif%3Fslice%3Dru%3Bid%3D47%3Bid%3D42%3Bperiod%3Dweek%3Brelgraph%3Dyes%3Bgraph%3Dyes'

        self.mock_request['QUERY_STRING'] = link

        ye_olde_cheshire_cheese(self.mock_request, self.mock_200_or_302_callback)

    def test_external_resource(self):
        link = 'http://lurkmore.to'
        self.mock_request['QUERY_STRING'] = quote(link)

        result = get_external_resource_redirect(link, self.mock_request)

        self.assertTrue(isinstance(result, MetaRedirectResponse))

    def test_external_pic(self):
        link = 'http://lurkmore.so/images/e/e1/Nohate.jpg'

        self.mock_request['QUERY_STRING'] = quote(link)
        with self.vcr.use_cassette('test_external_pic.yaml'):
            result = get_external_resource_redirect(link, self.mock_request)
            self.assertEqual(type(result), ImageResponse)

    def test_no_cache_for_images(self):
        link = 'http://lurkmore.so/images/e/e1/Nohate.jpg'

        self.mock_request['QUERY_STRING'] = quote(link)

        for i in range(3):
            with self.vcr.use_cassette('test_no_cache_for_images.yaml'):
                result = get_external_resource_redirect(link, self.mock_request)
                self.assertEqual(type(result), ImageResponse)

    def test_bad_lang(self):
        self.mock_request['HTTP_ACCEPT_LANGUAGE'] = 'en-US, en-US; q=0.8, en; q=0.6'
        self.assertEqual('en', detect_language(self.mock_request['HTTP_ACCEPT_LANGUAGE']))

    def test_slow_responces_http(self):
        timeouted_url = 'http://httpbin.org/delay/10'
        result = get_external_resource_redirect(timeouted_url, self.mock_request)
        self.assertTrue(isinstance(result, MetaRedirectResponse))

    def test_slow_responces_https(self):
        timeouted_url = 'https://httpbin.org/delay/10'
        result = get_external_resource_redirect(timeouted_url, self.mock_request)
        self.assertTrue(isinstance(result, MetaRedirectResponse))

    def test_replace_sheme(self):
        # will replace for jing
        self.assertEqual('https://jing.yandex-team.ru/storage/mchekalov/824340/2014-03-12_1159.png',
                         prepare_url('http://jing.yandex-team.ru/storage/mchekalov/824340/2014-03-12_1159.png'))

        # will not replace for img-fotki
        self.assertEqual('http://img-fotki.yandex-team.ru/get/26/1120000000011275.1/0_41_38f2c63c_M',
                         prepare_url('http://img-fotki.yandex-team.ru/get/26/1120000000011275.1/0_41_38f2c63c_M'))

    def test_cyrilic_urls(self):
        """
        В FF 30.0 наблюдается баг со ссылкой на http://президент.рф - редирект веден на www.президент.рф и
        фыфа говорит, что сайт ненайден. В location для ссылок с символами, не влезающими в ascii должен стоять idna
        """
        links = [
            ('%D0%BF%D1%80%D0%B5%D0%B7%D0%B8%D0%B4%D0%B5%D0%BD%D1%82.%D1%80%D1%84', 'http://xn--d1abbgf6aiiy.xn--p1ai'),
            ('http://президент.рф', 'http://xn--d1abbgf6aiiy.xn--p1ai'),
            ('http://президент.рф/info?путин', 'http://xn--d1abbgf6aiiy.xn--p1ai/info?%D0%BF%D1%83%D1%82%D0%B8%D0%BD'),
            ('http://пиши-код-блядь.рф/пишу', 'http://xn-----clckctarm0ag3itcyb.xn--p1ai/%D0%BF%D0%B8%D1%88%D1%83')
        ]
        del self.mock_request['HTTP_REFERER']

        for link, idna_encoded_result in links:
            self.mock_request['QUERY_STRING'] = link
            dest_url, lang = get_destination_url(self.mock_request)
            self.assertEqual(idna_encoded_result, dest_url)

    def test_correct_unquoting(self):
        """
        по мотивам https://st.yandex-team.ru/DIRECTMOD-3114#1423758906000
        """

        in_link = 'http%3A%2F%2Fad.doubleclick.net%2Fclk%3B274918428%3B101525894%3Bp%3Fhttps%3A%2F%2Fplay.google.com%2Fstore%3Futm_source%3DHA_Desktop_RU%26utm_medium%3DSEM'
        out_link = 'http://ad.doubleclick.net/clk;274918428;101525894;p?https://play.google.com/store?utm_source=HA_Desktop_RU&utm_medium=SEM'

        self.mock_request['QUERY_STRING'] = in_link

        self.assertEqual(get_destination_url(self.mock_request)[0], out_link)
