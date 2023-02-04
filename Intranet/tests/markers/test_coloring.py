from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import ColoringMarker, Marker


class TestColoringMarker(object):
    """
    Тесты маркера раскрашивания областей
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'coloring',
        'options': {
            'is_ordered': True,
            'init_coloring_image_views': {
                'check_points_group': [
                    {
                        'is_ordered': False,
                        'check_points': [
                            {
                                'y': 0.5,
                                'x': 0.25,
                            },
                            {
                                'y': 0.25,
                                'x': 0.5,
                            },
                        ],
                    },
                    {
                        'is_ordered': True,
                        'check_points': [
                            {
                                'y': 0.75,
                                'x': 0.25,
                            },
                            {
                                'y': 0.25,
                                'x': 0.75,
                            },
                        ],
                    },
                    {
                        'is_ordered': True,
                        'check_points': [
                            {
                                'y': 0.75,
                                'x': 0.5,
                            },
                            {
                                'y': 0.5,
                                'x': 0.75,
                            },
                        ],
                    },
                ],
                'height': 0.9,
                'width': 0.9,
                'background': 143,
                'y': 0.05,
                'x': 0.05,
            },
            'pallete_views': [
                {
                    'color': '#ff0000',
                    'color_symbol': 'o',
                    'height': 0.1,
                    'width': 0.1,
                    'background': 149,
                    'y': 1,
                    'x': 0.35,
                    'active_background': 148,
                },
                {
                    'color': '#00ff00',
                    'color_symbol': 'g',
                    'height': 0.1,
                    'width': 0.1,
                    'background': 145,
                    'y': 1,
                    'x': 0.45,
                    'active_background': 146,
                },
                {
                    'color': '#0000ff',
                    'color_symbol': 'b',
                    'height': 0.1,
                    'width': 0.1,
                    'background': 144,
                    'y': 1,
                    'x': 0.55,
                    'active_background': 147,
                },
            ],
        },
    }
    MARKUP_NOT_ORDERED = {
        'type': 'coloring',
        'options': {
            'is_ordered': False,
            'init_coloring_image_views': {
                'check_points_group': [
                    {
                        'is_ordered': False,
                        'check_points': [
                            {
                                'y': 0.5,
                                'x': 0.25,
                            },
                            {
                                'y': 0.25,
                                'x': 0.5,
                            },
                        ],
                    },
                    {
                        'is_ordered': True,
                        'check_points': [
                            {
                                'y': 0.75,
                                'x': 0.25,
                            },
                            {
                                'y': 0.25,
                                'x': 0.75,
                            },
                        ],
                    },
                    {
                        'is_ordered': True,
                        'check_points': [
                            {
                                'y': 0.75,
                                'x': 0.5,
                            },
                            {
                                'y': 0.5,
                                'x': 0.75,
                            },
                        ],
                    },
                ],
                'height': 0.9,
                'width': 0.9,
                'background': 143,
                'y': 0.05,
                'x': 0.05,
            },
            'pallete_views': [
                {
                    'color': '#ff0000',
                    'color_symbol': 'o',
                    'height': 0.1,
                    'width': 0.1,
                    'background': 149,
                    'y': 1,
                    'x': 0.35,
                    'active_background': 148,
                },
                {
                    'color': '#00ff00',
                    'color_symbol': 'g',
                    'height': 0.1,
                    'width': 0.1,
                    'background': 145,
                    'y': 1,
                    'x': 0.45,
                    'active_background': 146,
                },
                {
                    'color': '#0000ff',
                    'color_symbol': 'b',
                    'height': 0.1,
                    'width': 0.1,
                    'background': 144,
                    'y': 1,
                    'x': 0.55,
                    'active_background': 147,
                },
            ],
        },
    }
    DEFAULT_TEST_ANSWER = [
        ['o', 'g'],
        ['b', 'o'],
        ['g', 'g'],
    ]
    CHECK_DATA = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            None,
            (Marker.SKIPPED, 1),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                ['o', 'g'],
                ['b', 'o'],
                ['g', 'g'],
            ],
            (Marker.CORRECT, 0),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'o'],
                ['b', 'o'],
                ['g', 'g'],
            ],
            (Marker.CORRECT, 0),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'o'],
                ['b', 'o'],
            ],
            (Marker.INCORRECT, 1),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'o'],
                ['o', 'b'],
                ['g', 'g'],
            ],
            (Marker.INCORRECT, 1),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'g'],
                ['b', 'o'],
                ['g', 'o'],
            ],
            (Marker.INCORRECT, 1),
        ),
        (
            MARKUP_NOT_ORDERED,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'g'],
                ['b', 'o'],
                ['g', 'o'],
            ],
            (Marker.CORRECT, 0),
        ),
        (
            MARKUP_NOT_ORDERED,
            DEFAULT_TEST_ANSWER,
            [
                ['o', 'g'],
                ['b', 'o'],
                ['g', 'g'],
            ],
            (Marker.CORRECT, 0),
        ),
        (
            MARKUP_NOT_ORDERED,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'o'],
                ['b', 'o'],
                ['g', 'g'],
            ],
            (Marker.CORRECT, 0),
        ),
        (
            MARKUP_NOT_ORDERED,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'g'],
                ['b', 'b'],
                ['g', 'o'],
            ],
            (Marker.INCORRECT, 1),
        ),
        (
            MARKUP_NOT_ORDERED,
            DEFAULT_TEST_ANSWER,
            [
                ['g', 'o'],
                ['b', 'o'],
            ],
            (Marker.INCORRECT, 1),
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
            DEFAULT_TEST_ANSWER[0],
            '{} is not valid under any of '
            'the given schemas'.format(DEFAULT_TEST_ANSWER[0]),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER[1],
            '{} is not valid under any of '
            'the given schemas'.format(DEFAULT_TEST_ANSWER[1]),
        ),
    )

    def test_max_mistakes(self):
        """Тест максимального числа ошибок"""
        assert ColoringMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).max_mistakes == 1

    def test_get_embedded_objects(self):
        """
        Тест нахождения ресурсов в маркере
        """
        assert ColoringMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).get_embedded_objects() == [
            ('resource', 143),
            ('resource', 149),
            ('resource', 148),
            ('resource', 145),
            ('resource', 146),
            ('resource', 144),
            ('resource', 147),
        ]

    # TODO Следует уточнить формат пользовательского ответа

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer',
        VALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_positive(
            self, markup, marker_answer, user_answer):
        """
        Примеры правильного пользовательского ответа
        маркера раскраски
        """
        try:
            ColoringMarker(
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
        маркера раскаски
        """
        with pytest.raises(ValidationError) as excinfo:
            ColoringMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('marker,answer,user_answer,result', CHECK_DATA)
    def test_check(self, marker, answer, user_answer, result):
        """
        Проверка подсчета ошибок
        """
        assert ColoringMarker(marker, answer).check(user_answer) == result
