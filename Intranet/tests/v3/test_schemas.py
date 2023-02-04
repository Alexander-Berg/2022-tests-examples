# -*- coding: utf-8 -*-
from bson import ObjectId
from django.test import TestCase, override_settings
from pydantic import ValidationError

from events.v3.schemas import EmailIn, VariableIn, TrackerIn, HttpIn


class TestEmailIn(TestCase):
    def test_should_validate_email_to_address(self):
        variants = [
            'user@yandex.ru',
            f'{{{ObjectId()}}}',
            f'user@yandex.ru; Yandex Admin <admin@yandex.ru>,{{{ObjectId()}}}',
        ]
        for variant in variants:
            email_in = EmailIn.parse_obj({'type': 'email', 'email_to_address': variant})
            self.assertEqual(email_in.email_to_address, variant)

    def test_shouldnt_validate_email_to_address(self):
        variants = [
            'user_yandex.ru',
            '{12345678901234567890}',
            '{12345678901234567890123q}',
            'user@yandex.ru, {12345678901234567890}',
            ' ; ',
        ]
        for variant in variants:
            with self.assertRaises(ValidationError):
                EmailIn.parse_obj({'type': 'email', 'email_to_address': variant})

    def test_should_validate_headers_intranet(self):
        headers = [
            {'name': 'Reply-To', 'value': 'admin@yandex.ru'},
            {'name': 'Otrs', 'value': '12345'},
            {'name': 'X-Form-Id', 'value': '1'},
        ]
        email_to = EmailIn.parse_obj({'type': 'email', 'headers': headers})
        self.assertEqual(len(email_to.headers), 3)
        self.assertEqual(email_to.headers[0].name, 'Reply-To')
        self.assertEqual(email_to.headers[1].name, 'Otrs')
        self.assertEqual(email_to.headers[2].name, 'X-Form-Id')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_validate_headers_business(self):
        headers = [
            {'name': 'Reply-To', 'value': 'admin@yandex.ru'},
            {'name': 'X-Otrs', 'value': '12345'},
            {'name': 'X-Form-Id', 'value': '1'},
        ]
        email_to = EmailIn.parse_obj({'type': 'email', 'headers': headers})
        self.assertEqual(len(email_to.headers), 3)
        self.assertEqual(email_to.headers[0].name, 'Reply-To')
        self.assertEqual(email_to.headers[1].name, 'X-Otrs')
        self.assertEqual(email_to.headers[2].name, 'X-Form-Id')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_validate_headers_business(self):
        headers = [
            {'name': 'Reply-To', 'value': 'admin@yandex.ru'},
            {'name': 'Otrs', 'value': '12345'},
            {'name': 'X-Form-Id', 'value': '1'},
        ]
        with self.assertRaises(ValidationError):
            EmailIn.parse_obj({'type': 'email', 'headers': headers})


class TestVariableIn(TestCase):
    def test_should_validate_question_answer(self):
        variable_in = VariableIn.parse_obj({
            'id': str(ObjectId()),
            'type': 'form.question_answer',
            'question': 'answer_text_1',
        })
        self.assertEqual(variable_in.type, 'form.question_answer')
        self.assertEqual(variable_in.question, 'answer_text_1')

    def test_shouldnt_validate_question_answer(self):
        with self.assertRaises(ValidationError):
            VariableIn.parse_obj({
                'id': str(ObjectId()),
                'type': 'form.question_answer',
                'questions': {'items': ['answer_text_1']},
            })

    def test_should_validate_questions_answers(self):
        variable_in = VariableIn.parse_obj({
            'id': str(ObjectId()),
            'type': 'form.questions_answers',
            'questions': {'items': ['answer_text_1']},
        })
        self.assertEqual(variable_in.type, 'form.questions_answers')
        self.assertEqual(variable_in.questions.items, ['answer_text_1'])

    def test_shouldnt_validate_questions_answers(self):
        with self.assertRaises(ValidationError):
            VariableIn.parse_obj({
                'id': str(ObjectId()),
                'type': 'form.questions_answers',
                'question': 'answer_text_1',
            })


class TestTrackerIn(TestCase):
    def test_should_validate_issue_type(self):
        tracker_in = TrackerIn.parse_obj({
            'type': 'tracker',
            'issue_type': 2,
        })
        self.assertEqual(tracker_in.type, 'tracker')
        self.assertEqual(tracker_in.issue_type, 2)

    def test_shouldnt_validate_issue_type(self):
        with self.assertRaises(ValidationError):
            TrackerIn.parse_obj({
                'type': 'tracker',
                'issue_type': -1,
            })

    def test_should_validate_priority(self):
        tracker_in = TrackerIn.parse_obj({
            'type': 'tracker',
            'priority': 2,
        })
        self.assertEqual(tracker_in.type, 'tracker')
        self.assertEqual(tracker_in.priority, 2)

    def test_shouldnt_validate_priority(self):
        with self.assertRaises(ValidationError):
            TrackerIn.parse_obj({
                'type': 'tracker',
                'priority': -1,
            })

    def test_should_validate_queue(self):
        tracker_in = TrackerIn.parse_obj({
            'type': 'tracker',
            'queue': 'forms',
        })
        self.assertEqual(tracker_in.type, 'tracker')
        self.assertEqual(tracker_in.queue, 'FORMS')

    def test_shouldnt_validate_queue(self):
        with self.assertRaises(ValidationError):
            TrackerIn.parse_obj({
                'type': 'tracker',
                'queue': '1forms',
            })

    def test_should_validate_parent(self):
        tracker_in = TrackerIn.parse_obj({
            'type': 'tracker',
            'parent': 'forms-12',
        })
        self.assertEqual(tracker_in.type, 'tracker')
        self.assertEqual(tracker_in.parent, 'FORMS-12')

    def test_shouldnt_validate_parent(self):
        with self.assertRaises(ValidationError):
            TrackerIn.parse_obj({
                'type': 'tracker',
                'parent': '1forms+12',
            })


class TestHttpIn(TestCase):
    def test_should_validate_url(self):
        urls = [
            'http://yandex.ru',
            'https://yandex.ru',
        ]
        for url in urls:
            http_in = HttpIn.parse_obj({
                'type': 'http',
                'method': 'post',
                'url': url,
            })
            self.assertEqual(http_in.url, url)

    def test_shouldnt_validate_url(self):
        urls = [
            'file:///readme.txt',
            'postgresql://localhost',
            'redis://localhost/1',
        ]
        for url in urls:
            with self.assertRaises(ValidationError):
                HttpIn.parse_obj({
                    'type': 'http',
                    'method': 'post',
                    'url': url,
                })

    def test_shouldnt_change_tvm_client_for_intranet(self):
        tvm_client = '200'
        http_in = HttpIn.parse_obj({
            'type': 'http',
            'method': 'post',
            'url': 'http://yandex.ru',
            'tvm_client': tvm_client,
        })
        self.assertEqual(http_in.tvm_client, tvm_client)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_change_tvm_client_for_business(self):
        tvm_client = '200'
        http_in = HttpIn.parse_obj({
            'type': 'http',
            'method': 'post',
            'url': 'http://yandex.ru',
            'tvm_client': tvm_client,
        })
        self.assertEqual(http_in.tvm_client, None)
