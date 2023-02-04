from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import Marker, WordPartsMarker


class TestWordPartsMarker(object):
    """
    Тесты маркера разбора слов по составу
    """

    DEFAULT_TEST_MARKUP = {
        'type': 'wordparts',
        'options': {
            'text': u'Мама мыла раму',
            'min_word_length': 3,
        },
    }
    DEFAULT_TEST_ANSWER = [
        {
            'word_id': u'мама:1',
            'parts': {
                'roots': [[0, 2]],
                'endings': [[3, 4]],
            },
        },
    ]
    FULL_TEST_MARKUP = {
        'type': 'wordparts',
        'options': {
            'text': u'Идет текст, за ним еще текст и {formula:1} {resource:2}',
            "min_word_length": 3,
        },
    }
    VALID_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            [
                {
                    'word_id': u'мама:1',
                    'parts': {},
                },
                {
                    'word_id': u'раму:1',
                    'parts': {
                        'roots': [],
                        'stems': [[1, 3], [4, 5]],
                        'endings': [[0, 4], [4, 2]],
                    },
                },
            ],
        ),
        (
            {
                'type': 'wordparts',
                'options': {
                    'text': 'A Man, A Plan, A Canal: Panama!',
                    'min_word_length': 4,
                },
                'id': 5,
            },
            [
                {
                    'word_id': 'panama:1',
                    'parts': {},
                },
                {
                    'word_id': 'plan:1',
                    'parts': {
                        'suffixes': [],
                        'stems': [[1, 2], [1, 5]],
                        'endings': [[0, 4], [4, 2]],
                        'prefixes': [[1, 2], [0, 1]],
                    },
                },
            ],
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'идет:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                }
            ],
        ),
        (
            # множественный ответ
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'текст:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                },
                {
                    'word_id': u'текст:1',
                    'parts': {
                        'roots': [[0, 1], [2, 3]],
                    },
                },
            ],
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'текст:2',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                }
            ],
        )
    )
    INVALID_FORMAT = (
        (
            {
                'type': 'wordparts',
                'options': {
                    'text': u'Мама мыла раму',
                    'min_word_length': -1,
                },
            },
            [
                {
                    'word_id': u'мама:1',
                    'parts': {},
                },
                {
                    'word_id': u'рама:134',
                    'parts': {
                        'suffixes': [],
                        'stems': [[1, 2], [1, 5]],
                        'endings': [[0, 4], [4, 2]],
                        'prefixes': [[1, 2], [0, 1]],
                    },
                },
            ],
            '-1 is less than the minimum of 1',
        ),
        (
            {
                'type': 'wordparts',
                'options': {
                    'text': u'Мама мыла раму',
                    'min_word_length': 2.5,
                },
            },
            [
                {
                    'word_id': u'мама:1',
                    'parts': {},
                },
                {
                    'word_id': u'рама:134',
                    'parts': {
                        'suffixes': [],
                        'stems': [[1, 2], [1, 5]],
                        'endings': [[0, 4], [4, 2]],
                        'prefixes': [[1, 2], [0, 1]],
                    },
                },
            ],
            '2.5 is not of type \'integer\'',
        ),
        (
            {
                'type': 'wordparts',
                'options': {
                    'min_word_length': 3,
                },
            },
            [
                {
                    'word_id': u'мама:1',
                    'parts': {},
                },
                {
                    'word_id': u'рама:134',
                    'parts': {
                        'suffixes': [],
                        'stems': [[1, 2], [1, 5]],
                        'endings': [[0, 4], [4, 2]],
                        'prefixes': [[1, 2], [0, 1]],
                    },
                },
            ],
            '\'text\' is a required property',
        ),
        (
            {
                'type': 'wordparts',
                'options': {
                    'text': 'test',
                    'min_word_length': 3,
                },
            },
            [
                {
                    'word_ids': u'мама:1',
                    'parts': {},
                },
                {
                    'word_id': u'рама:134',
                    'parts': {
                        'suffixes': [],
                        'stems': [[1, 2], [1, 5]],
                        'endings': [[0, 4], [4, 2]],
                        'prefixes': [[1, 2], [0, 1]],
                    },
                },
            ],
            'Additional properties are not allowed '
            '(\'word_ids\' was unexpected)',
        ),
        (
            {
                'type': 'wordparts',
                'options': {
                    'text': 'test',
                    'min_word_length': 3,
                },
            },
            [
                {
                    'word_id': u'мама:1',
                    'parts': {},
                },
                {
                    'word_id': u'рама:134',
                    'parts': {
                        'suffixes': {},
                        'stems': [[1, 2], [1, 5]],
                        'endings': [[0, 4], [4, 2]],
                        'prefixes': [[1, 2], [0, 1]],
                    },
                },
            ],
            '{} is not of type \'array\'',
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'слово:1',  # слова нет в тексте
                    'parts': {
                        'roots': [[0, 4]],
                    },
                },
            ],
            u"Incorrect word_id: слово:1",
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'текст:0',  # индекс меньше 1
                    'parts': {
                        'roots': [[0, 4]],
                    },
                },
            ],
            u"Incorrect word_id: текст:0",
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'текст:3',  # индекс слишком большой
                    'parts': {
                        'roots': [[0, 4]],
                    },
                },
            ],
            u"Incorrect word_id: текст:3",
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'текст:qwe',  # индекс не число
                    'parts': {
                        'roots': [[0, 4]],
                    },
                },
            ],
            u"Incorrect word_id: текст:qwe",
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'за:1',  # слово слишком короткое
                    'parts': {
                        'roots': [[0, 4]],
                    },
                },
            ],
            u"Incorrect word_id: за:1",
        ),
        (
            FULL_TEST_MARKUP,
            [
                {
                    'word_id': u'formula:1',  # слово внутри ресурса
                    'parts': {
                        'roots': [[0, 6]],
                    },
                },
            ],
            u"Incorrect word_id: formula:1",
        )
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
            .format(DEFAULT_TEST_ANSWER),
        ),
    )
    CHECK_DATA = (
        (
            # пропустил вопрос
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 2]],
                        'endings': [[3]],
                    },
                },
            ],
            Marker.SKIPPED_ANSWER,
            (Marker.SKIPPED, 3),
        ),
        (
            # верный ответ (первый вариант)
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 2]],
                        'endings': [[3]],
                    },
                },
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                },
            ],
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 2]],
                        'endings': [[3]],
                    },
                },
            ],
            (
                {
                    'status': 1,
                    'results': [
                        {
                            'word_id': u'мама:1',
                            'result': 'correct',
                        },
                    ],
                },
                0
            )
        ),
        (
            # верный ответ (второй вариант)
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 2]],
                        'endings': [[3]],
                    },
                },
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                },
            ],
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                },
            ],
            (
                {
                    'status': 1,
                    'results': [
                        {
                            'word_id': u'мама:1',
                            'result': 'correct',
                        },
                    ],
                },
                0
            )
        ),
        (
            # верный ответ в немного другом формате
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 3]],
                        'endings': [],  # тут пустой массив
                    },
                },
                {
                    'word_id': u'мыла:1',
                    'parts': {
                        'roots': [[0, 1], [2, 3]],
                    },
                }
            ],
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 3]],
                        # тут нет ключа
                    },
                },
                {
                    'word_id': u'мыла:1',
                    'parts': {
                        'roots': [[2, 3], [0, 1]],  # массивы в другом порядке
                    },
                },
            ],
            (
                {
                    'status': 1,
                    'results': [
                        {
                            'word_id': u'мама:1',
                            'result': 'correct',
                        },
                        {
                            'word_id': u'мыла:1',
                            'result': 'correct',
                        },
                    ],
                },
                0
            )
        ),
        (
            # сборная солянка ошибок
            [
                {
                    'word_id': u'мама:1',
                    'parts': {
                        'roots': [[0, 2]],
                        'endings': [[3]],
                    },
                },
                {
                    'word_id': u'мыла:1',
                    'parts': {
                        'roots': [[0, 2]],
                        'endings': [[3]],
                    },
                },
            ],
            [
                # слово 'мама' не разобрал
                {
                    # разобрал слово неправильно
                    'word_id': u'мыла:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                },
                {
                    # разобрал лишнее слово
                    'word_id': u'раму:1',
                    'parts': {
                        'roots': [[0, 3]],
                    },
                },
            ],
            (
                {
                    'status': 0,
                    'results': [
                        {
                            'word_id': u'мама:1',
                            'result': 'skipped',
                        },
                        {
                            'word_id': u'раму:1',
                            'result': 'wrong_word',
                        },
                        {
                            'word_id': u'мыла:1',
                            'result': 'incorrect',
                        },
                    ]
                },
                3
            )
        ),
    )

    @pytest.mark.parametrize('markup, marker_answer', VALID_FORMAT)
    def test_validate_positive(self, markup, marker_answer):
        """
        Примеры правильного маркера разбора
        """
        try:
            WordPartsMarker(markup, marker_answer).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize('markup, marker_answer,error', INVALID_FORMAT)
    def test_validate_negative(self, markup, marker_answer, error):
        """
        Примеры неправильного маркера разбора
        """
        with pytest.raises(ValidationError) as excinfo:
            WordPartsMarker(markup, marker_answer).validate()
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
        маркера разбора
        """
        try:
            WordPartsMarker(
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
        маркера разбора
        """
        with pytest.raises(ValidationError) as excinfo:
            WordPartsMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('answer,user_answer,expected_result', CHECK_DATA)
    def test_check(self, answer, user_answer, expected_result):
        """
        Проверяет рассчет результатов
        """
        result, errors = (WordPartsMarker(self.DEFAULT_TEST_MARKUP, answer).check(user_answer))
        assert errors == expected_result[1]
        if user_answer:
            sorted_results = sorted(result['results'], key=lambda x: x['word_id'])
            sorted_expected = sorted(expected_result[0]['results'], key=lambda x: x['word_id'])
            assert sorted_results == sorted_expected
