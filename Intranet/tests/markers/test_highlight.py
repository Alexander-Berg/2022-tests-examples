from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import HighlightMarker, Marker
from kelvin.problems.markers.highlight import HIGHLIGHT_TYPES


class TestHighlightMarker(object):
    """
    Тесты маркера выделения
    """

    DEFAULT_TEST_MARKUP = {
        'type': 'highlight',
        'options': {
            'text': u'Мама мыла раму',
            'highlight_types': {
                'highlight': [
                    'highlight/word',
                    'highlight/letter',
                    'highlight/stress'
                ],
                'morpheme': [
                    'morpheme/root',
                    'morpheme/suffix',
                    'morpheme/prefix',
                    'morpheme/stem',
                    'morpheme/ending',
                ],
                'morpheme-with-bubble': [
                    'morpheme-with-bubble/root',
                    'morpheme-with-bubble/suffix',
                    'morpheme-with-bubble/prefix',
                    'morpheme-with-bubble/stem',
                    'morpheme-with-bubble/ending',
                ],
            }
        },
    }
    DEFAULT_TEST_ANSWER = [
        {
            'highlight/word': [[1, 4], [11, 15]]
        },
        {
            'highlight/stress': [[1, 2], [11, 12]],
            'highlight/letter': [[2, 3], [4, 5]],
        },
        {
            'highlight/stress': [[1, 2], [11, 12]],
            'highlight/letter': [[2, 3], [4, 5]],
            'morpheme-with-bubble/root': [[2, 3], [4, 5]],
            'morpheme/stem': [[0, 3]],
        },
    ]
    ONE_HIGHLIGHT_TYPE_TEST_MARKUP = {
        'type': 'highlight',
        'options': {
            'text': u'Мама мыла раму',
            'highlight_types': {
                'highlight': ['highlight/word'],
            }
        },
    }
    VALID_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'highlight/word': [[1, 4], [11, 15]]
                },
            ],
        ),
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'highlight/word': [[1, 4], [11, 15]],
                    'highlight/letter': [[5, 6], [8, 9]],
                },
            ],
        ),
    )
    INVALID_FORMAT = (
        (
            ONE_HIGHLIGHT_TYPE_TEST_MARKUP,
            [
                {
                    'highlight/word': [[1, 4], [11, 15]],
                    'highlight/letter': [[5, 6], [7, 8]],
                },
            ],
            'Answer types don\'t match problem\'s markup'
        ),
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'highlight/word': [[-1, 4]],
                },
            ],
            '-1 is less than the minimum of 0'
        ),
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'highlight/word': [[4, 1]],
                },
            ],
            'range [4, 1] is invalid'
        ),
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'highlight/word': [[4]],
                },
            ],
            '[4] is too short'
        ),
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'highlight/word': [[4, 5, 6]],
                },
            ],
            '[4, 5, 6] is too long'
        ),
        (
            {
                'type': 'highlight',
                'options': {
                    'text': u'Мама мыла раму',
                    'highlight_types': {
                        'nonexistingsection': [
                            'nonexisting/type',
                        ],
                    }
                },
            },
            [],
            'Additional properties are not allowed '
            '(\'nonexistingsection\' was unexpected)',
        ),
        (
            {
                'type': 'highlight',
                'options': {
                    'text': u'Мама мыла раму',
                    'highlight_types': {
                        'highlight': [
                            'nonexisting/type',
                        ],
                    }
                },
            },
            [{}],
            '\'nonexisting/type\' is not one of {}'
            .format(HIGHLIGHT_TYPES['highlight'])
        ),
        (
            DEFAULT_TEST_MARKUP,
            [],
            '[] is too short'
        ),
    )
    VALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER[0],
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER[1],
        ),
    )

    INVALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER,
            '{} is not of type \'object\''.format(DEFAULT_TEST_ANSWER),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            {
                'nonexisting/type': [[1, 2], [11, 12]],
            },
            'Additional properties are not allowed'
            ' (\'nonexisting/type\' was unexpected)',
        )
    )
    CHECK_DATA = (
        (
            # пропустил вопрос
            [
                {
                    'highlight/word': [[0, 4]],
                },
            ],
            Marker.SKIPPED_ANSWER,
            (Marker.SKIPPED, 1),
        ),
        (
            # правильно ответил #1
            [
                {
                    'highlight/word': [[0, 4]],
                    'highlight/letter': [[7, 8], [9, 10]],
                },
                {
                    'highlight/word': [[5, 9]],
                },
            ],
            {
                'highlight/word': [[0, 4]],
                'highlight/letter': [[9, 10], [7, 8]],  # другой порядок
            },
            (
                {
                    'status': 1,
                    'results': {
                        'highlight/word': {
                            'correct': [[0, 4]],
                        },
                        'highlight/letter': {
                            'correct': [[7, 8], [9, 10]],
                        },
                    },
                },
                0
            ),
        ),
        (
            # правильно ответил #2
            [
                {
                    'highlight/word': [[0, 4]],
                    'highlight/letter': [[7, 8], [9, 10]],
                },
                {
                    'highlight/word': [[5, 9]],
                },
            ],
            {
                'highlight/word': [[5, 9]],
            },
            (
                {
                    'status': 1,
                    'results': {
                        'highlight/word': {
                            'correct': [[5, 9]],
                        },
                    },
                },
                0
            ),
        ),
        (
            # неправильно ответил #1
            [
                {
                    'highlight/word': [[0, 4]],
                    'highlight/letter': [[7, 8], [9, 10]],
                },
                {
                    'highlight/word': [[5, 9]],
                },
            ],
            {
                'highlight/word': [[0, 3]],
                'highlight/letter': [[9, 10], [4, 5]],
            },
            (
                {
                    'status': 0,
                    'results': {
                        'highlight/word': {
                            'incorrect': [[0, 3]],
                            'skipped': [[0, 4]]
                        },
                        'highlight/letter': {
                            'correct': [[9, 10]],
                            'incorrect': [[4, 5]],
                            'skipped': [[7, 8]]
                        },
                    },
                },
                4
            ),
        ),
        (
            # неправильно ответил #2 (одинаковая степень совпадений)
            [
                {
                    'highlight/word': [[0, 4]],
                    'highlight/letter': [[7, 8], [9, 10]],
                },
                {
                    'highlight/word': [[5, 9], [10, 14]],
                },
            ],
            {
                'highlight/word': [[5, 9], [0, 4]],
            },
            (
                {
                    'status': 0,
                    'results': {
                        'highlight/word': {
                            'correct': [[0, 4]],
                            'incorrect': [[5, 9]],
                        },
                        'highlight/letter': {
                            'skipped': [[7, 8], [9, 10]],
                        },
                    },
                },
                3
            ),
        ),
        (
            # неправильно ответил #3
            [
                {
                    'highlight/word': [[0, 4]],
                    'highlight/letter': [[7, 8], [9, 10]],
                },
                {
                    'highlight/word': [[5, 9], [10, 14]],
                    'highlight/letter': [[8, 9], [4, 5]],
                },
            ],
            {
                'highlight/word': [[5, 9], [0, 4]],
                'highlight/letter': [[8, 9]],
            },
            (
                {
                    'status': 0,
                    'results': {
                        'highlight/word': {
                            'correct': [[5, 9]],
                            'incorrect': [[0, 4]],
                            'skipped': [[10, 14]]
                        },
                        'highlight/letter': {
                            'correct': [[8, 9]],
                            'skipped': [[4, 5]],
                        },
                    },
                },
                3
            ),
        ),
    )

    @pytest.mark.parametrize('markup,marker_answer', VALID_FORMAT)
    def test_validate_positive(self, markup, marker_answer):
        """
        Примеры правильного маркера выделений
        """
        try:
            HighlightMarker(markup, marker_answer).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize('markup,marker_answer,error', INVALID_FORMAT)
    def test_validate_negative(self, markup, marker_answer, error):
        """
        Примеры неправильного маркера выделений
        """
        with pytest.raises(ValidationError) as excinfo:
            HighlightMarker(markup, marker_answer).validate()
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

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
            HighlightMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

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
            HighlightMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('answer,user_answer,result', CHECK_DATA)
    def test_check(self, answer, user_answer, result):
        """
        Проверяет рассчет результатов
        """
        assert (HighlightMarker(self.DEFAULT_TEST_MARKUP, answer)
                .check(user_answer)) == result
