# -*- coding: utf-8 -*-
from django.test import TestCase
from events.surveyme.answer import fetch_answer_data


class TestFetchAnswerData(TestCase):
    def test_should_return_questions(self):
        answer_data = {
            'data': [
                {
                    'value': 'one',
                    'question': {
                        'id': 1,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        }
                    },
                },
                {
                    'value': 'two',
                    'question': {
                        'id': 2,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        }
                    },
                },
            ],
        }
        result = list(fetch_answer_data(answer_data))
        self.assertEqual(len(result), 2)
        ids = (it['question']['id'] for it in result)
        self.assertSetEqual(set(ids), {1, 2})

    def test_shouldnt_return_none(self):
        answer_data = {
            'data': [
                {
                    'value': 'one',
                    'question': {
                        'id': 1,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        }
                    },
                },
                None,
            ],
        }
        result = list(fetch_answer_data(answer_data))
        self.assertEqual(len(result), 1)
        ids = (it['question']['id'] for it in result)
        self.assertSetEqual(set(ids), {1})

    def test_should_return_grouped_questions(self):
        answer_data = {
            'data': [
                {
                    'value': [[
                        {
                            'value': 'one',
                            'question': {
                                'id': 1,
                                'answer_type': {
                                    'slug': 'answer_short_text',
                                }
                            },
                        },
                        {
                            'value': 'two',
                            'question': {
                                'id': 2,
                                'answer_type': {
                                    'slug': 'answer_short_text',
                                }
                            },
                        },
                    ], None],
                    'question': {
                        'id': 3,
                        'answer_type': {
                            'slug': 'answer_group',
                        }
                    },
                },
            ],
        }
        result = list(fetch_answer_data(answer_data))
        self.assertEqual(len(result), 3)
        ids = (it['question']['id'] for it in result)
        self.assertSetEqual(set(ids), {1, 2, 3})

    def test_shouldnt_return_none_grouped_questions(self):
        answer_data = {
            'data': [
                {
                    'value': [[
                        {
                            'value': 'one',
                            'question': {
                                'id': 1,
                                'answer_type': {
                                    'slug': 'answer_short_text',
                                }
                            },
                        },
                        None,
                        {
                            'value': 'two',
                            'question': {
                                'id': 2,
                                'answer_type': {
                                    'slug': 'answer_short_text',
                                }
                            },
                        },
                    ], None],
                    'question': {
                        'id': 3,
                        'answer_type': {
                            'slug': 'answer_group',
                        }
                    },
                },
            ],
        }
        result = list(fetch_answer_data(answer_data))
        self.assertEqual(len(result), 3)
        ids = (it['question']['id'] for it in result)
        self.assertSetEqual(set(ids), {1, 2, 3})
