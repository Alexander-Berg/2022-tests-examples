from builtins import object
from copy import deepcopy

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import InlineMarker, Marker


class TestInlineMarker(object):
    """
    Тест маркера сложного ввода
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'inline',
        'options': {
            'text': u'Какой{input:4}то текст с{input:3} разными инпутами.'
                    u'Вставь пропуще{input:1}ые буквы. Ра{input:2}тавь буквы.'
                    u'Посчитай выражение 1/4 + 4/5 * 1/2 = {input:6}\n'
                    u'И еще до кучи {resource:1}{formula:1}{resource:2}',
            'inputs': {
                '1': {
                    'type': 'field',
                    'group': 1,
                    'options': {
                        'width': 2,
                        'type_content': 'text',
                    },
                },
                '2': {
                    'type': 'choice',
                    'group': 1,
                    'options': {
                        'choices': [
                            u'cc',
                            u'зз',
                            u'c',
                            u'зс',
                        ],
                    },
                },
                '3': {
                    'type': 'comma',
                    'group': 1,
                    'options': {},
                },
                '4': {
                    'type': 'separator',
                    'group': 2,
                    'options': {
                        'choices': [
                            'hyphen',
                            'together',
                        ],
                    },
                },
                '5': {
                    'type': 'field',
                    'group': 3,
                    'options': {
                        'width': 2,
                        'type_content': 'number',
                    },
                },
                '6': {
                    'type': 'rational',
                    'group': 4,
                    'options': {
                        'width': 2,
                    },
                },
            },
        },
    }
    DEFAULT_TEST_ANSWER = {
        '1': u'нн',
        '2': 0,
        '3': False,
        '4': 0,
        '5': '7',
        '6': '13/20'
    }
    CHECK = {
        '1': {
            'type': 'AND',
            'sources': [
                {
                    'type': 'EQUAL',
                    'sources': [
                        {
                            'source': 1,
                            'type': 'INPUT',
                        },
                        {
                            'source': u'нн',
                            'type': 'STRING',
                        },
                    ],
                },
                {
                    'type': 'EQUAL',
                    'sources': [
                        {
                            'source': 2,
                            'type': 'INPUT',
                        },
                        {
                            'source': 3,
                            'type': 'NUMBER',
                        },
                    ],
                },
                {
                    'type': 'EQUAL',
                    'sources': [
                        {
                            'source': 3,
                            'type': 'INPUT',
                        },
                        {
                            'source': False,
                            'type': 'BOOLEAN',
                        },
                    ],
                },
            ],
        },
        '2': {
            'type': 'EQUAL',
            'sources': [
                {
                    'source': 4,
                    'type': 'INPUT',
                },
                {
                    'source': 0,
                    'type': 'NUMBER',
                },
            ],
        },
        '3': {
            'type': 'EQUAL',
            'sources': [
                {
                    'source': 5,
                    'type': 'INPUT',
                },
                {
                    'source': 7,
                    'type': 'NUMBER',
                },
            ],
        },
        '4': {
            'type': 'EQUAL',
            'sources': [
                {
                    'source': 6,
                    'type': 'INPUT',
                },
                {
                    'source': '13/20',
                    'type': 'RATIONAL',
                },
            ],
        },
    }
    CHECKS_DATA = (
        (
            None,
            (Marker.SKIPPED, 1),
        ),
        (
            {'1': u'нн', '2': 3, '3': False, '4': 0, '5': '7', '6': '13/20'},
            ({'1': True, '2': True, '3': True, '4': True}, 0),
        ),
        (
            {'1': u'н', '2': 3, '3': False, '4': 0, '5': '7', '6': '13/20'},
            ({'1': False, '2': True, '3': True, '4': True}, 1),
        ),
        (
            {'1': u'нн', '2': 3, '3': False, '4': 1, '5': '7', '6': '13/20'},
            ({'1': True, '2': False, '3': True, '4': True}, 1),
        ),
        (
            {'1': u'нн', '2': 3, '3': False, '4': 0, '5': '9', '6': '13/20'},
            ({'1': True, '2': True, '3': False, '4': True}, 1),
        ),
        (
            {'1': u'нн', '2': 3, '3': False, '4': 0, '5': '7', '6': '13/23'},
            ({'1': True, '2': True, '3': True, '4': False}, 1),
        ),
        (
            {'1': u'н', '2': 4, '3': False, '4': 1, '5': '7,2', '6': '1/23'},
            ({'1': False, '2': False, '3': False, '4': False}, 1),
        ),
    )
    VALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER,
        ),
    )
    INVALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [DEFAULT_TEST_ANSWER],
            '{} is not of type \'object\''
            .format([DEFAULT_TEST_ANSWER]),
        ),
    )

    def test_validate_positive(self):
        """
        Проверка правильности разметки
        """
        try:
            InlineMarker(
                self.DEFAULT_TEST_MARKUP,
                self.DEFAULT_TEST_ANSWER,
                self.CHECK
            ).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    def test_validate_negative(self):
        """
        Проверка неправильной проверки в маркере
        """
        check = deepcopy(self.CHECK)
        check['1']['sources'] = {}
        with pytest.raises(ValidationError) as excinfo:
            InlineMarker(self.DEFAULT_TEST_MARKUP,
                         self.DEFAULT_TEST_ANSWER,
                         check).validate()

        assert excinfo.value.message == "{'type': 'AND', 'sources': {}} is not valid under any of the given schemas"

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer',
        VALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_positive(
            self, markup, marker_answer, user_answer):
        """
        Примеры правильного пользовательского ответа
        маркера выделений
        """
        try:
            InlineMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    def test_validate_array_not_allowed(
            self,
            markup={
                'type': 'inline',
                'options': {
                    'text': u'Какой{input:4}то текст с{input:3} разными инпутами.'
                    u'Вставь пропуще{input:1}ые буквы. Ра{input:2}тавь буквы.'
                    u'Посчитай выражение 1/4 + 4/5 * 1/2 = {input:6}\n'
                    u'И еще до кучи {resource:1}{formula:1}{resource:2}',
                    'inputs': {
                        '1': {
                            'type': 'field',
                            'group': 1,
                            'options': {
                                'width': 2,
                                'type_content': 'text',
                            },
                        },
                    },
                },
            },
            marker_answer={'1': u'5'},
            user_answer={'1': [u'5']},
    ):
        """
        Проверяем, что теперь нельзя присылать массив в инлайн-маркере ( INTLMS-902 )
        """
        with pytest.raises(ValidationError) as excinfo:
            InlineMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == u'[\'5\'] is not valid under any of the given schemas', (
            u"Неправильное сообщение об ошибке")

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer,error',
        INVALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_negative(
            self, markup, marker_answer, user_answer, error):
        """
        Примеры неправильного пользовательского ответа
        маркера выделений
        """
        with pytest.raises(ValidationError) as excinfo:
            InlineMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    def test_get_embedded_objects(self):
        """
        Тест нахождения ресурсов и формул в маркере
        """
        assert InlineMarker(
            self.DEFAULT_TEST_MARKUP, self.DEFAULT_TEST_ANSWER, self.CHECK
        ).get_embedded_objects() == [
            ('resource', '1'),
            ('formula', '1'),
            ('resource', '2'),
        ], u'Неправильно найдены ресурсы и формулы'

    def test_max_mistakes(self):
        """Проверка максимального количества ошибок"""
        assert InlineMarker(
            self.DEFAULT_TEST_MARKUP, self.DEFAULT_TEST_ANSWER, self.CHECK
        ).max_mistakes == 1, (
            u'Неправильное максимальное количество ошибок')

    @pytest.mark.parametrize('answer,result', CHECKS_DATA)
    def test_check(self, answer, result):
        """Проверка подсчета ошибок"""
        assert InlineMarker(
            self.DEFAULT_TEST_MARKUP, self.DEFAULT_TEST_ANSWER, self.CHECK
        ).check(answer) == result
