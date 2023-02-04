from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import ConnectareasMarker, Marker


class TestConnectareasMarker(object):
    """
    Тесты маркера соединения областей
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'connectareas',
        'options': {
            'color_map_file': 3605,
            'line_width': '8.0',
            'order_important': False,
            'hit_areas': [
                ['#0000FF', 'b1'],
                ['#00FF00', 'b2'],
                ['#00FFFF', 'b3'],
                ['#FF0000', 'm1'],
                ['#FF00FF', 'm2'],
            ],
            'image_file': 3604,
            'swap_enabled': True,
        },
    }
    DEFAULT_TEST_ANSWER = [
        [
            [
                {'areaId': 'b1'},
                {'areaId': 'm1'},
            ],
            [
                {'areaId': 'b2'},
                {'areaId': 'm1'},
            ],
            [
                {'areaId': 'b3'},
                {'areaId': 'm2'},
            ],
        ],
        [
            [
                {'areaId': 'b1'},
                {'areaId': 'b2'},
            ],
            [
                {'areaId': 'b1'},
                {'areaId': 'b3'},
            ],
            [
                {'areaId': 'b2'},
                {'areaId': 'b3'},
            ],
        ],
    ]
    MARKUP_WO_COLOR_MAP = {
        'type': 'connectareas',
        'options': {
            'line_width': '8.0',
            'order_important': False,
            'hit_areas': [
                ['#0000FF', 'b1'],
                ['#00FF00', 'b2'],
                ['#00FFFF', 'b3'],
                ['#FF0000', 'm1'],
                ['#FF00FF', 'm2'],
            ],
            'image_file': 3604,
            'swap_enabled': True,
        },
    }
    ANSWER_WO_COLOR_MAP = [
        [
            [
                {'areaId': 'b1'},
                {'areaId': 'm1'},
            ],
            [
                {'areaId': 'b2'},
                {'areaId': 'm1'},
            ],
            [
                {'areaId': 'b3'},
                {'areaId': 'm2'},
            ],
        ],
        [
            [
                {'areaId': 'b1'},
                {'areaId': 'b2'},
            ],
            [
                {'areaId': 'b1'},
                {'areaId': 'b3'},
            ],
            [
                {'areaId': 'b2'},
                {'areaId': 'b3'},
            ],
        ],
    ]
    MARKUP_ORDER_IMPORTANT = {
        'type': 'connectareas',
        'options': {
            'color_map_file': 3605,
            'line_width': '8.0',
            'order_important': True,
            'hit_areas': [
                ['#0000FF', 'b1'],
                ['#00FF00', 'b2'],
                ['#00FFFF', 'b3'],
                ['#FF0000', 'm1'],
                ['#FF00FF', 'm2'],
            ],
            'image_file': 3604,
            'swap_enabled': True,
        },
    }
    ANSWER_ORDER_IMPORTANT = [
        [
            [
                {'areaId': 'b1'},
                {'areaId': 'm1'},
            ],
            [
                {'areaId': 'b2'},
                {'areaId': 'm1'},
            ],
            [
                {'areaId': 'b3'},
                {'areaId': 'm2'},
            ],
        ],
        [
            [
                {'areaId': 'b1'},
                {'areaId': 'b2'},
            ],
            [
                {'areaId': 'b1'},
                {'areaId': 'b3'},
            ],
            [
                {'areaId': 'b2'},
                {'areaId': 'b3'},
            ],
        ],
    ]
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
            '{} is not valid under any of the given schemas'
            .format(DEFAULT_TEST_ANSWER),
        ),
    )
    CHECK_DATA = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            None,
            (
                {
                    'compared_with': 0,
                    'status': Marker.SKIPPED,
                },
                1,
            ),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                [
                    {'areaId': 'm1'},
                    {'areaId': 'b2'},
                ],
                [
                    {'areaId': 'b3'},
                    {'areaId': 'm2'},
                ],
                [
                    {'areaId': 'b1'},
                    {'areaId': 'm1'},
                ],
            ],
            (
                {
                    'compared_with': 0,
                    'status': [
                        Marker.CORRECT,
                        Marker.CORRECT,
                        Marker.CORRECT
                    ],
                },
                0,
            ),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [
                [
                    {'areaId': 'b1'},
                    {'areaId': 'b3'},
                ],
                [
                    {'areaId': 'm1'},
                    {'areaId': 'b3'},
                ],
            ],
            (
                {
                    'compared_with': 1,
                    'status': [Marker.CORRECT, Marker.INCORRECT],
                },
                1,
            ),
        ),
        (
            MARKUP_ORDER_IMPORTANT,
            ANSWER_ORDER_IMPORTANT,
            [
                [
                    {'areaId': 'b1'},
                    {'areaId': 'm1'},
                ],
                [
                    {'areaId': 'm1'},
                    {'areaId': 'b2'},
                ],
                [
                    {'areaId': 'b3'},
                    {'areaId': 'm2'},
                ],
            ],
            (
                {
                    'compared_with': 0,
                    'status': [
                        Marker.CORRECT,
                        Marker.INCORRECT,
                        Marker.CORRECT
                    ],
                },
                1,
            ),
        ),
    )

    def test_max_mistakes(self):
        """Тест максимального числа ошибок"""
        assert ConnectareasMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).max_mistakes == 1

    def test_get_embedded_objects(self):
        """
        Тест нахождения ресурсов в маркере
        """
        assert (ConnectareasMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).get_embedded_objects() == [
            ('resource', 3604),
            ('resource', 3605)
        ])

        assert (ConnectareasMarker(
            self.MARKUP_WO_COLOR_MAP,
            self.ANSWER_WO_COLOR_MAP
        ).get_embedded_objects() == [('resource', 3604)])

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
            ConnectareasMarker(
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
            ConnectareasMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('marker,answer,user_answer,result', CHECK_DATA)
    def test_check(self, marker, answer, user_answer, result):
        """
        Проверка подсчета ошибок

        Здесь же проверка `_compare_answers`
        """
        assert ConnectareasMarker(marker, answer).check(user_answer) == result
