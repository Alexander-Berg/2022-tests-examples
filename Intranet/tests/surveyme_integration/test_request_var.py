# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings

from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables import (
    RequestIpVariable,
    RequestHostVariable,
    RequestURLVariable,
    RequestHeadersVariable,
    RequestHeaderVariable,
    RequestQueryParamsVariable,
    RequestQueryParamVariable,
    RequestCookiesVariable,
    RequestCookieVariable,
)


class TestRequestIpVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_ip_data_should_return_none(self):
        var = RequestIpVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_without_ip_data_should_return_ip(self):
        self.answer.source_request = {'ip': '127.0.0.1'}
        var = RequestIpVariable(answer=self.answer)
        self.assertEqual(var.get_value(), '127.0.0.1')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_none_for_business(self):
        self.answer.source_request = {'ip': '127.0.0.1'}
        var = RequestIpVariable(answer=self.answer)
        self.assertIsNone(var.get_value())


class TestRequestHostVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_headers_data_should_return_none(self):
        var = RequestHostVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_headers_but_without_host_data_should_return_none(self):
        self.answer.source_request = {'headers': {}}
        var = RequestHostVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_headers_and_host_data_should_return_host(self):
        self.answer.source_request = {'headers': {'host': 'tech.yandex.ru'}}
        var = RequestHostVariable(answer=self.answer)
        self.assertEqual(var.get_value(), 'tech.yandex.ru')


class TestRequestURLVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_url_data_should_return_none(self):
        var = RequestURLVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_url_data_should_return_url(self):
        self.answer.source_request = {'url': 'http://tech.yandex.ru/'}
        var = RequestURLVariable(answer=self.answer)
        self.assertEqual(var.get_value(), 'http://tech.yandex.ru/')


class TestRequestHeadersVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_headers_data_should_return_none(self):
        var = RequestHeadersVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_headers_data_should_return_headers(self):
        self.answer.source_request = {'headers': {'some': 'header'}}
        var = RequestHeadersVariable(answer=self.answer)
        self.assertEqual(var.get_value(), {'some': 'header'})


class TestRequestHeaderVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_headers_data_should_return_none(self):
        var = RequestHeaderVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_headers_data_but_without_exact_key(self):
        self.answer.source_request = {'headers': {'another_key': 'header'}}
        var = RequestHeaderVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_headers_data_and_with_exact_key(self):
        self.answer.source_request = {'headers': {'another_key': 'header', 'some_key': 'here it is'}}
        var = RequestHeaderVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), 'here it is')

    def test_for_answer_with_headers_data_and_with_exact_key__but_in_other_case(self):
        self.answer.source_request = {'headers': {'another_key': 'header', 'some_key': 'here it is'}}
        var = RequestHeaderVariable(answer=self.answer, name='SOME_key')
        self.assertEqual(var.get_value(), 'here it is')


class TestRequestQueryParamsVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_query_params_data_should_return_none(self):
        var = RequestQueryParamsVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_query_params_data_should_return_query_params(self):
        self.answer.source_request = {'query_params': {'some': 'param'}}
        var = RequestQueryParamsVariable(answer=self.answer)
        self.assertEqual(var.get_value(), {'some': 'param'})


class TestRequestQueryParamVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_query_params_data_should_return_none(self):
        var = RequestQueryParamVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_query_params_data_but_without_exact_key(self):
        self.answer.source_request = {'query_params': {'another_key': 'vallue'}}
        var = RequestQueryParamVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_query_params_data_and_with_exact_key(self):
        self.answer.source_request = {'query_params': {'another_key': 'vallue', 'some_key': 'here it is'}}
        var = RequestQueryParamVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), 'here it is')

    def test_for_answer_with_query_params_data_and_with_exact_key__but_in_other_case(self):
        self.answer.source_request = {'query_params': {'another_key': 'vallue', 'some_key': 'here it is'}}
        var = RequestQueryParamVariable(answer=self.answer, name='SOME_key')
        self.assertEqual(var.get_value(), 'here it is')


class TestRequestCookiesVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_query_params_data_should_return_none(self):
        var = RequestCookiesVariable(answer=self.answer)
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_query_params_data_should_return_query_params(self):
        self.answer.source_request = {'cookies': {'some': 'cookie'}}
        var = RequestCookiesVariable(answer=self.answer)
        self.assertEqual(var.get_value(), {'some': 'cookie'})


class TestRequestCookieVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_for_answer_without_cookies_data_should_return_none(self):
        var = RequestCookieVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_cookies_data_but_without_exact_key(self):
        self.answer.source_request = {'cookies': {'another_key': 'vallue'}}
        var = RequestCookieVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), None)

    def test_for_answer_with_cookies_data_and_with_exact_key(self):
        self.answer.source_request = {'cookies': {'another_key': 'vallue', 'some_key': 'here it is'}}
        var = RequestCookieVariable(answer=self.answer, name='some_key')
        self.assertEqual(var.get_value(), 'here it is')

    def test_for_answer_with_cookies_data_and_with_exact_key__but_in_other_case(self):
        self.answer.source_request = {'cookies': {'another_key': 'vallue', 'some_key': 'here it is'}}
        var = RequestCookieVariable(answer=self.answer, name='SOME_key')
        self.assertEqual(var.get_value(), 'here it is')
