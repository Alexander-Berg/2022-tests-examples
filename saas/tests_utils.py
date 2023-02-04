# -*- coding: utf-8 -*-

import unittest
import sys
import os

path = os.path.abspath(__file__)
current = '/'.join(path.split('/')[:-2])
sys.path = [current] + sys.path

import utils
import suggest_message as smessage
import errors


BAD_STRS = [
    '95.108.170.168 - - [24/Dec/2010:18:43:42 +0300] "GET /index.php?=PHPE9568F34-D428-11d2-A769-00AA001ACF42 HTTP/1.1" 200 2524 "http://msearch-proxy1.mail.yandex.net/?user=1120000000013747&text=msearch1&db=mdb301" "Mozilla/5.0 (X11; Linux i686; rv:2.0b8) Gecko/20100101 Firefox/4.0b8"',
    '77.88.20.103 - - [24/Dec/2010:18:44:50 +0300] "GET / HTTP/1.1" 400 39 "-" "check_http/v1861 (nagios-plugins 1.4.11)"',
    '::1 - - [19/May/2011:00:02:38 +0400] "OPTIONS * HTTP/1.1" 200 - 139 "-" "Apache/2.2.3 (Red Hat) (internal dummy connection)"',
    '77.88.46.54 - - [10/May/2011:12:34:59 +0400] "GET /?text=%D0%BE+%D0%B2%D1%80%D0%B5%D0%BC%D1%8F+%D0%B1%D0%B5%D1%80%D0%B5%D0%BC%D0%B5%D0%BD%D0%BD%D0%BE%D1%81%D1%82%D0%B8+%D0%BF%D0%BE%D1%87%D0%B5%D0%BC%D1%83-%D1%82%D0%BE+%D0%BD%D0%B0%D1%87%D0%B0%D0%BB%D0%B0+%D0%BF%D0%B5%D1%80%D0%B8%D0%BE%D0%B4%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B8+%D0%B3%D0%BE%D0%B2%D0%BE%D1%80%D0%B8%D1%82%D1%8C+%D0%B2%D0%BE+%D1%81%D0%BD%D0%B5.+%D0%9E%D0%B1+%D1%8D%D1%82%D0%BE%D0%BC+%D1%8F+%D1%83%D0%B7%D0%BD%D0%B0%D0%BB%D0%B0%2C+%D0%BA%D0%BE%D0%BD%D0%B5%D1%87%D0%BD%D0%BE%2C+%D0%BE%D1%82+%D0%BC%D1%83%D0%B6%D0%B0.++-+%D0%90+%D0%BE%D0%BD+%D0%B8%D1%85+%D0%BD%D0%B0+%D0%BE%D1%89%D1%83%D0%BF%D1%8C+%D1%83%D0%B7%D0%BD%D0%B0%D0%BB%2C+-+%D0%B2%D0%B4%D1%80%D1%83%D0%B3+%D1%81%D0%BE%D0%BE%D0%B1%D1%89%D0%B8%D0%BB%D0%B0+%D0%B5%D0%BC%D1%83+%D1%8F+%D0%B2+%D0%BF%D0%B5%D1%80%D0%B2%D1%8B%D0%B9+%D1%80%D0%B0%D0%B7%2C+%D0%BE%D0%BA%D0%BE%D0%BB%D0%BE+%D1%87%D0%B0%D1%81%D0%B0+%D0%BD%D0%BE%D1%87%D0%B8.++-+%D0%9A%D0%BE%D0%B3%D0%BE%3F+-+%D1%81%D0%BF%D1%80%D0%BE%D1%81%D0%B8%D0%BB+%D0%BE%D1%85%D1%80%D0%B5%D0%BD%D0%B5%D0%B2%D1%88%D0%B8%D0%B9+%D0%BC%D1%83%D0%B6.++-+%D0%94%D0%B5%D0%BF%D1%83%D1%82%D0%B0%D1%82%D0%BE%D0%B2%2C+-+%D0%B8%2C+%D1%81%D0%BA%D0%B0%D0%B7%D0%B0%D0%B2+%D1%8D%D1%82%D0%BE%2C+%D1%81+%D1%87%D1%83%D0%B2%D1%81%D1%82%D0%B2%D0%BE%D0%BC+%D0%B2%D1%8B%D0%BF%D0%BE%D0%BB%D0%BD%D0%B5%D0%BD%D0%BD%D0%BE%D0%B3%D0%BE+%D0%B4%D0%BE%D0%BB%D0%B3%D0%B0+%D0%B7%D0%B0%D1%85%D1%80%D0%B0%D0%BF%D0%B5%D0%BB%D0%B0%2C+%D0%BD%D0%B5+%D0%B7%D0%BD%D0%B0%D1%8F%2C+%D1%87%D1%82%D0%BE+%D1%87%D0%B5%D1%80%D0%B5%D0%B7+%D0%BF%D1%8F%D1%82%D1%8C+%D1%87%D0%B0%D1%81%D0%BE%D0%B2+%D1%8F+%D0%BF%D1%80%D0%BE%D1%81%D0%BD%D1%83%D1%81%D1%8C+%D0%B8+%D0%B1%D1%83%D0%B4%D1%83+%D0%BE%D0%BE%D0%BE%D0%BE%D1%87%D0%B5%D0%BD%D1%8C+%D1%83%D0%B4%D0%B8%D0%B2%D0%BB%D0%B5%D0%BD%D0%B0.++%D0%92%D0%BE+%D0%B2%D1%82%D0%BE%D1%80%D0%BE%D0%B9+%D1%80%D0%B0%D0%B7+%D1%8F+%D0%BF%D0%BE%D1%80%D0%B0%D0%B4%D0%BE%D0%B2%D0%B0%D0%BB%D0%B0+%D0%B5%D0%B3%D0%BE+%D0%BF%D0%B5%D1%80%D0%BB%D0%BE%D0%BC+%22%D0%9A%D1%83%D0%B4%D0%B0+%D0%B2+%D0%BB%D1%83%D0%BD%D0%B5+%D0%B2%D1%81%D1%82%D0%B0%D0%B2%D0%BB%D1%8F%D1%8E%D1%82%D1%81%D1%8F+%D1%87%D0%B0%D1%81%D1%8B%3F+%D0%9E%D1%88%D0%B8%D0%B7%D0%B5%D1%82%D1%8C%21%22++-+%D0%AF+%D0%BD%D0%B5+%D1%85%D0%BE%D1%87%D1%83+%D0%B1%D1%8B%D1%82%D1%8C+%D0%BF%D0%B5%D0%BB%D0%B8%D0%BA%D0%B0%D0%BD%D0%BE%D0%BC%2C+%D0%B7%D0%B0%D0%B1%D0%B5%D1%80%D0%B8+%D0%B3%D0%B5%D1%80%D0%B0%D0%BD%D1%8C%21..+-+%D0%B7%D0%B0%D0%BE%D1%80%D0%B0%D0%BB%D0%B0+%D1%8F+%D1%81%D1%80%D0%B5%D0%B4%D0%B8+%D0%BD%D0%BE%D1%87%D0%B8+%D0%B2+%D1%82%D1%80%D0%B5%D1%82%D0%B8%D0%B9+%D1%80%D0%B0%D0%B7+%D0%B8+%D1%81%D0%B0%D0%BC%D0%B0+%D0%BE%D1%82+%D1%8D%D1%82%D0%BE%D0%B3%D0%BE+%D0%BF%D1%80%D0%BE%D1%81%D0%BD%D1%83%D0%BB%D0%B0%D1%81%D1%8C.+%D0%94%D0%BE+%D1%8D%D1%82%D0%BE%D0%B3%D0%BE+%D0%B4%D0%BD%D1%8F+%D0%BF%D0%BE%D0%B4%D0%BE%D0%B7%D1%80%D0%B5%D0%B2%D0%B0%D0%BB%D0%B0%2C+%D1%87%D1%82%D0%BE+%D0%BD%D0%B0%D0%B4%D0%BE+%D0%BC%D0%BD%D0%BE%D0%B9+%D1%81%D1%82%D0%B5%D0%B1%D1%83%D1%82%D1%81%D1%8F...+%D0%9F%D0%BE%D0%B2%D0%B5%D1%80%D0%B8%D0%BB%D0%B0.++-+%D0%A1%D0%B5%D1%80%D0%BE%D0%B5%2C+%D1%81%D0%B5%D1%80%D0%BE%D0%B5%2C+%D1%81%D0%B5%D1%80%D0%BE%D0%B5...+%D0%94%D0%B0%D0%B9+%D0%BC%D0%BD%D0%B5+%D1%86%D0%B2%D0%B5%D1%82%D0%BD%D0%BE%D0%B5+%D0%B7%D0%B5%D1%80%D0%BA%D0%B0%D0%BB%D0%BE%21..+-+%D1%82%D1%80%D0%B5%D0%B1%D0%BE%D0%B2%D0%B0%D0%BB%D0%B0+%D0%B2+%D1%87%D0%B5%D1%82%D0%B2%D0%B5%D1%80%D1%82%D1%8B%D0%B9%2C+%D0%B1%D1%83%D0%B4%D1%83%D1%87%D0%B8+%D1%83%D0%B6%D0%B5+%D0%BC%D0%B5%D1%81%D1%8F%D1%86%D0%B5+%D0%BD%D0%B0+%D1%82%D1%80%D0%B5%D1%82%D1%8C%D0%B5%D0%BC.++-+%D0%9A%D0%B0%D0%BA%D0%BE%D0%B3%D0%BE+%D1%87%D0%B5%D1%80%D1%82%D0%B0+%D1%82%D1%8B+%D0%BF%D0%BE%D0%BA%D1%80%D0%B0%D1%81%D0%B8%D0%BB+%D1%82%D0%B0%D1%80%D0%B0%D0%BA%D0%B0%D0%BD%D0%BE%D0%B2+%D0%BC%D0%BE%D0%B8%D0%BC+%D0%BA%D1%80%D0%B0%D1%81%D0%BD%D1%8B%D0%BC+%D0%BB%D0%B0%D0%BA%D0%BE%D0%BC%3F+%D0%9C%D0%BD%D0%B5+%D0%BA%D0%B0%D0%B6%D0%B5%D1%82%D1%81%D1%8F%2C+%D0%BC%D0%BE%D0%B8+%D0%BD%D0%BE%D0%B3%D1%82%D0%B8+%D0%BE%D1%82+%D0%BC%D0%B5%D0%BD%D1%8F+%D1%83%D0%B1%D0%B5%D0%B3%D0%B0%D1%8E%D1%82%21+-+%D0%B8%D1%81%D1%82%D0%B5%D1%80%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B8+%D0%B7%D0%B0%D1%8F%D0%B2%D0%B8%D0%BB%D0%B0+%D1%81%D0%BF%D1%83%D1%81%D1%82%D1%8F+%D0%B4%D0%B5%D0%B2%D1%8F%D1%82%D1%8C+%D0%B4%D0%BD%D0%B5%D0%B9.++-+%D0%9A%D1%83%D0%B4%D0%B0+%D1%82%D1%8B+%D0%B4%D0%B5%D0" 414 345 100 "-" "-"',
    '77.88.47.164 - - [24/Dec/2010:00:01:31 +0300] "GET /?text=&user=68139664&db=mdb200&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 500 419 "-" "-"',
    '77.88.47.160 - - [24/Dec/2010:00:00:14 +0300] "GET /?text=slazarev%40mconsult.ru&user=22481014&db=mdb330&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 2447 "-" "-"',
    '77.88.46.26 - - [24/Dec/2010:23:55:08 +0300] "GET /?text=sss981%40yande+&user=1130000001169669&db=mdb370&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 469 "-" "-"',
    '95.108.198.13 - - [21/Jul/2011:19:06:11 +0400] "GET /?text=body_text%3A%D0%B0%D1%80%D0%BB&db=OTRS&user=351739991&length=1000&imap=1 HTTP/1.1" 200 9 22153608 "-" "OTRS"',
    '77.88.47.177 - - [03/May/2011:19:33:13 +0400] "GET /?imap=1&user=1120000000042256&db=mdb301&offset=0&length=1000&text=body_text%3a%22POST%22 HTTP/1.1" 200 5806 462878 "-" "imap.yandex-team.ru"',
    '77.88.46.51 - - [02/Sep/2011:13:24:34 +0400] "GET /?text=vernik&user=31414801&db=mdb310&search_scope=search_folders_default&wizard=on&spcctx=doc&how=tm&np=1&utf8=1&length=200 HTTP/1.1" 500 - 8506635 "-" "-"',
    '77.88.46.252 - - [27/Apr/2011:20:19:21 +0400] "GET /?text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&length=1&user=82742315&db=mdb330 HTTP/1.1" 200 54 24188 "-" "Wget/1.11.4 Red Hat modified"',
    '77.88.47.177 - - [02/Oct/2011:22:40:24 +0400] "GET /?imap=1&user=1120000000103520&db=mdb301&offset=0&length=1000&text=%28hdr_subject%3a%22Ilj%22+OR+body_text%3a%22Ilj%22%29 HTTP/1.1" 200 9 11747 "-" "-"',
    '127.0.0.1 - - [25/May/2011:14:56:20 +0400] "GET /?text=sphinx.conf&db=OTRS&user=351739991&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 22 522895 "-" "-"',
]


GOOD_STRS = [
    '77.88.46.51 - - [24/Dec/2010:00:01:50 +0300] "GET /?text=Subscribe.Ru&user=15524912&db=mdb360&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 19100 "-" "-"',
    '77.88.47.158 - - [24/Dec/2010:00:02:44 +0300] "GET /?text=mamba&user=102996613&db=mdb230&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 8 "-" "-"',
]


EMPTY_STRS = [
    '77.88.46.51 - - [24/Dec/2010:00:01:50 +0300] "GET /?text=&user=15524912&db=mdb360&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 19100 "-" "-"',
    '77.88.47.158 - - [24/Dec/2010:00:02:44 +0300] "GET /?user=102996613&db=mdb230&wizard=on&spcctx=doc&how=tm&np=1&utf8=1 HTTP/1.1" 200 8 "-" "-"',
]


GOOD_STRS_WITH_JSON_FORMAT = [
    '77.88.46.3 - - [06/Oct/2011:00:00:02 +0400] "GET /?user=11903018&db=mdb200&wizard=on&spcctx=doc&how=tm&np=1&utf8=1&format=json&getfields=mid,url,&text=expedia&length=200 HTTP/1.1" 200 2801 65066 "-" "-"',
    '77.88.47.199 - - [07/Oct/2011:00:00:00 +0400] "GET /?user=309514110&db=mdb330&wizard=on&spcctx=doc&how=tm&np=1&utf8=1&format=json&getfields=mid,url,&text=webmoney.ru&length=200 HTTP/1.1" 200 31 111220 "-" "-"',
    '77.88.47.199 - - [07/Oct/2011:00:00:00 -0400] "GET /?user=456553021&db=mdb333&wizard=on&spcctx=doc&how=tm&np=1&utf8=1&format=json&getfields=mid,url,&text=dhhdhdhd&length=1200 HTTP/1.1" 200 3555 111220 "16" "-"',
]


class FakeSocketWithClosedConnection(object):
    def __init__(self, *args, **kwargs):
        pass

    def sendall(self, *args, **kwargs):
        pass

    def recv(self, *args, **kwargs):
        return ''

    def close(self):
        pass


class Test(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_parse_log_bad_strs(self):
        self.assertEqual(list(utils.parse_log(BAD_STRS)), [])

    def test_parse_log_good_strs(self):
        result = [
            smessage.SuggestMessage(
                keyprefix=15524912,
                text=u'Subscribe.Ru',
                query={'spcctx': 'doc', 'utf8': '1', 'wizard': 'on', 'db': 'mdb360', 'how': 'tm', 'np': '1'},
                send_type='modify',
                timestamp=1293138110),
            smessage.SuggestMessage(
                keyprefix=102996613,
                text=u'mamba',
                query={'spcctx': 'doc', 'utf8': '1', 'wizard': 'on', 'db': 'mdb230', 'how': 'tm', 'np': '1'},
                send_type='delete',
                timestamp=1293138164),
        ]
        self.assertEqual(list(utils.parse_log(GOOD_STRS)), result)

    def test_parse_log_empty_strs(self):
        self.assertEqual(list(utils.parse_log(EMPTY_STRS)), [])

    def test_parse_log_good_strs_with_json_format(self):
        result = [
            smessage.SuggestMessage(
                keyprefix=11903018,
                text=u'expedia',
                query={'spcctx': 'doc', 'format': 'json', 'utf8': '1', 'wizard': 'on', 'db': 'mdb200', 'how': 'tm',
                    'length': '200', 'np': '1', 'getfields': 'mid,url,'},
                send_type='modify',
                timestamp=1317844802),
            smessage.SuggestMessage(
                keyprefix=309514110,
                text=u'webmoney.ru',
                query={'spcctx': 'doc', 'format': 'json', 'utf8': '1', 'wizard': 'on', 'db': 'mdb330', 'how': 'tm',
                    'length': '200', 'np': '1', 'getfields': 'mid,url,'},
                send_type='modify',
                timestamp=1317931200),
            smessage.SuggestMessage(
                keyprefix=456553021,
                text=u'dhhdhdhd',
                query={'spcctx': 'doc', 'format': 'json', 'utf8': '1', 'wizard': 'on', 'db': 'mdb333', 'how': 'tm',
                    'length': '1200', 'np': '1', 'getfields': 'mid,url,'},
                send_type='modify',
                timestamp=1317931200)]
        self.assertEqual(list(utils.parse_log(GOOD_STRS_WITH_JSON_FORMAT)), result)

    def test_closed_connection(self):
        self.assertRaises(
            errors.ClosedConnection,
            utils.send_message, FakeSocketWithClosedConnection(),
            smessage.SuggestMessage(
                keyprefix=11903018,
                text=u'expedia',
                query={'spcctx': 'doc'},
                send_type='modify',
                timestamp=1317844802))

if __name__ == '__main__':
    unittest.main()
