# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from unittest.mock import Mock

from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables import (
    BrowserDataVariable,
    BrowserOSFamilyVariable,
    BrowserOSNameVariable,
    BrowserOSVersionVariable,
    BrowserNameVariable,
    BrowserVersionVariable,
    BrowserEngineVariable
)


class BaseTestMixin(object):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.var = self.variable_class(answer=self.answer)

    def test_for_answer_without_source_request_data(self):
        self.assertEqual(self.var.get_value(), None)

    def test_for_answer_without_user_agent_data(self):
        self.answer.source_request = {'headers': {'some': 'header'}}
        self.assertEqual(self.var.get_value(), None)


class TestBrowserDataVariable(BaseTestMixin, TestCase):
    variable_class = BrowserDataVariable

    @responses.activate
    def test_for_answer_with_user_agent_data(self):
        responses.add(
            responses.POST,
            'http://uatraits.qloud.yandex.ru/v0/detect',
            json={
                'isTouch': False, 'isMobile': False, 'postMessageSupport': True, 'isBrowser': True,
                'historySupport': True, 'WebPSupport': True, 'SVGSupport': True, 'YaGUI': '2.5',
                'OSVersion': '10.13.6', 'OSName': 'Mac OS X High Sierra', 'BrowserBaseVersion': '72.0.3626.109',
                'BrowserEngine': 'WebKit', 'OSFamily': 'MacOS', 'BrowserEngineVersion': '537.36',
                'BrowserVersion': '19.3.0.2489', 'BrowserName': 'YandexBrowser', 'CSP1Support': True,
                'localStorageSupport': True, 'BrowserBase': 'Chromium', 'CSP2Support': True,
            },
        )
        user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.109 YaBrowser/19.3.0.2489 Yowser/2.5 Safari/537.36'
        self.answer.source_request = {'headers': {'user-agent': user_agent}}
        expected = {
            'isTouch': False,
            'isMobile': False,
            'postMessageSupport': True,
            'isBrowser': True,
            'historySupport': True,
            'WebPSupport': True,
            'SVGSupport': True,
            'YaGUI': '2.5',
            'OSVersion': '10.13.6',
            'OSName': 'Mac OS X High Sierra',
            'BrowserBaseVersion': '72.0.3626.109',
            'BrowserEngine': 'WebKit',
            'OSFamily': 'MacOS',
            'BrowserEngineVersion': '537.36',
            'BrowserVersion': '19.3.0.2489',
            'BrowserName': 'YandexBrowser',
            'CSP1Support': True,
            'localStorageSupport': True,
            'BrowserBase': 'Chromium',
            'CSP2Support': True,
        }
        self.assertEqual(self.var.get_value(), expected)


class SingleVariableTestMixin(BaseTestMixin):
    def test_should_return_none_if_could_not_detect_value(self):
        self.var.get_browser_data = Mock(return_value={'some': 'key'})
        self.assertEqual(self.var.get_value(), None)

    def test_should_return_value_if_could_detect_value(self):
        self.var.get_browser_data = Mock(return_value={self.data_key: 'value'})
        self.assertEqual(self.var.get_value(), 'value')


class TestBrowserOSFamilyVariable(SingleVariableTestMixin, TestCase):
    variable_class = BrowserOSFamilyVariable
    data_key = 'OSFamily'


class TestBrowserOSNameVariable(SingleVariableTestMixin, TestCase):
    variable_class = BrowserOSNameVariable
    data_key = 'OSName'


class TestBrowserOSVersionVariable(SingleVariableTestMixin, TestCase):
    variable_class = BrowserOSVersionVariable
    data_key = 'OSVersion'


class TestBrowserNameVariable(SingleVariableTestMixin, TestCase):
    variable_class = BrowserNameVariable
    data_key = 'BrowserName'


class TestBrowserVersionVariable(SingleVariableTestMixin, TestCase):
    variable_class = BrowserVersionVariable
    data_key = 'BrowserVersion'


class TestBrowserEngineVariable(SingleVariableTestMixin, TestCase):
    variable_class = BrowserEngineVariable
    data_key = 'BrowserEngine'
