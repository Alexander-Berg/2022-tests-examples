import os
from datetime import datetime, tzinfo, timedelta
from unittest import TestCase, mock

from intranet.magiclinks.src.links.dto import (
    String,
    Number,
    User,
    Datetime,
    Image,
    List,
    MatchOptions,
    UrlObject,
    BinaryResource
)


class StringTestCase(TestCase):
    def test_string_without_color(self):
        string = String(value='привет')
        self.assertEqual(string.to_dict(), {'type': 'string', 'value': 'привет'})

    def test_string_with_color(self):
        string = String(value='мир', color='#123456')
        self.assertEqual(string.to_dict(), {'type': 'string', 'value': 'мир', 'color': '#123456'})


class NumberTestCase(TestCase):
    def test_number(self):
        number = Number(value=1)
        self.assertEqual(number.to_dict(), {'type': 'number', 'value': 1, 'format': '%d'})

    def test_floating_number(self):
        number = Number(value=1.1, format='%f')
        self.assertEqual(number.to_dict(), {'type': 'number', 'value': 1.1, 'format': '%f'})


class UserTestCase(TestCase):
    def test_user(self):
        user = User(login='justlive')
        self.assertEqual(user.to_dict(), {'type': 'user', 'login': 'justlive'})


class DatetimeTestCase(TestCase):
    def test_datetime(self):
        dt = Datetime(value='2016-09-08T12:46:00')
        self.assertEqual(dt.to_dict(), {'type': 'datetime', 'value': '2016-09-08T12:46:00'})

    def test_datetime_from_datetime(self):
        dt = Datetime.from_datetime(datetime(2016, 9, 8, 12, 46))
        self.assertEqual(dt.to_dict(), {'type': 'datetime', 'value': '2016-09-08T12:46:00'})

    def test_datetime_with_microseconds_and_tzinfo(self):
        class MyTz(tzinfo):
            def utcoffset(self, dt):
                return timedelta(minutes=123)

        dt = Datetime.from_datetime(datetime(2016, 9, 8, 12, 46, 14, 221133, tzinfo=MyTz()))
        self.assertEqual(dt.to_dict(), {'type': 'datetime',
                                        'value': '2016-09-08T12:46:14.221133+02:03'})


class ImageTestCase(TestCase):
    def test_image(self):
        image = Image(src='http://ссылка-с-кирилицей.рф/path', width=50, height=50, text='Текст')
        self.assertEqual(image.to_dict(), {'type': 'image',
                                           'src': 'http://ссылка-с-кирилицей.рф/path',
                                           'width': 50, 'height': 50, 'text': 'Текст'})

    def test_src_wrong_value(self):
        self.assertRaises(TypeError, Image, src=123)


class ListTestCase(TestCase):
    def test_list(self):
        lst = List(value=[
            String(value='привет')
        ])
        self.assertEqual(lst.to_dict(), {'type': 'list', 'separator': chr(160), 'ttl': 10800, 'value': [
            {'type': 'string', 'value': 'привет'}
        ], 'completed': True})

    def test_list_with_inner_list(self):
        lst = List(value=[
            String(value='привет'),
            List(separator='', value=[
                String(value='мир')
            ])
        ])
        self.assertEqual(lst.to_dict(), {'type': 'list', 'separator': chr(160), 'ttl': 10800, 'value': [
            {'type': 'string', 'value': 'привет'},
            {'type': 'list', 'separator': '', 'ttl': 10800, 'value': [
                {'type': 'string', 'value': 'мир'}
            ], 'completed': True}
        ], 'completed': True})


class MatchOptionsTestCase(TestCase):
    def test_regexes_are_compiled_at_creation(self):
        hostname_regex = r'this is hostname regex'
        path_regex = r'this is path regex'
        fragment_regex = r'this is fragment regex'
        query_regex = r'this is query regex'
        match_options = MatchOptions(hostname_regex=hostname_regex, path_regex=path_regex,
                                     fragment_regex=fragment_regex, query_regex=query_regex,
                                     )
        self.assertEqual(match_options.hostname_regex.pattern, hostname_regex)
        self.assertEqual(match_options.path_regex.pattern, path_regex)
        self.assertEqual(match_options.fragment_regex.pattern, fragment_regex)
        self.assertEqual(match_options.query_regex.pattern, query_regex)


class UrlObjectTestCase(TestCase):
    def test_url_objects_with_same_url_are_equal_and_have_same_hash(self):
        url_object1 = UrlObject(url='http://example.com/path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        url_object2 = UrlObject(url='http://example.com/path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        self.assertIsNot(url_object1, url_object2)
        self.assertEqual(url_object1, url_object2)
        self.assertEqual(hash(url_object1), hash(url_object2))

    def test_url_objects_as_set_items(self):
        s = set()

        url_object1 = UrlObject(url='http://example.com/path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        url_object2 = UrlObject(url='http://example.com/path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        url_object3 = UrlObject(url='http://example.com/another-path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        s.add(url_object1)
        self.assertIn(url_object1, s)
        self.assertIn(url_object2, s)
        self.assertNotIn(url_object3, s)

    def test_url_objects_as_dict_keys(self):
        d = dict()

        url_object1 = UrlObject(url='http://example.com/path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        url_object2 = UrlObject(url='http://example.com/path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        url_object3 = UrlObject(url='http://example.com/another-path', split_result=None,
                                hostname_match=None, path_match=None, worker_class_file=None,
                                fragment_match=None, query_match=None,
                                )
        d[url_object1] = 123
        self.assertEqual(d[url_object1], 123)
        self.assertEqual(d[url_object2], 123)
        self.assertNotIn(url_object3, d)


class TestBinaryResource(TestCase):
    path = os.path.abspath(__file__)

    def test_resource_exists(self):
        path_to_icon = 'intranet/magiclinks/tests/resources/test_serialization/test_favicon.ico'
        with mock.patch.object(BinaryResource, 'path_to_icon', return_value=path_to_icon):
            resource = BinaryResource('test_favicon.ico', self.path)
        self.assertEqual(resource.serialize(), 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAA\n')

    def test_resource_not_exists(self):
        self.assertRaises(ValueError, BinaryResource, 'not_exists.ico', self.path)
