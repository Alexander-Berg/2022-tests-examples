from builtins import object
from copy import deepcopy

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import DragImagesMarker, Marker


class TestDragImagesMarker(object):
    """
    Тесты маркера перетаскивания на картинке
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'dragimage',
        'options': {
            'main_image': {
                'resource_id': 10053,
                'x': 0.2,
                'y': 0.5,
                'width': 0.5
            },
            'fields': [
                {'id': 1, 'y': 0.5, 'x': 0.2},
                {'id': 2, 'y': 0.5, 'x': 0.4},
                {'id': 3, 'y': 0.5, 'x': 0.5},
                {'id': 4, 'y': 0.5, 'x': 0.6},
                {'id': 5, 'y': 0.5, 'x': 0.8},
                {'id': 6, 'y': 0.5, 'x': 0.9, 'disabled': True, 'choice': 3},
            ],
            'gravity_radius': 0.07,
            'choices': [
                {
                    'id': 1,
                    'resource_id': 9794,
                    'value': 1,
                    'y': 0.9,
                    'x': 0.1,
                    'width': 0.1
                },
                {
                    'id': 2,
                    'resource_id': 9795,
                    'value': 2,
                    'y': 0.9,
                    'x': 0.2,
                    'width': 0.1
                },
                {
                    'id': 3,
                    'resource_id': 9796,
                    'value': 3,
                    'y': 0.9,
                    'x': 0.3,
                    'width': 0.1
                },
                {
                    'id': 4,
                    'resource_id': 9797,
                    'value': 4,
                    'y': 0.9,
                    'x': 0.4,
                    'width': 0.1
                },
                {
                    'id': 5,
                    'resource_id': 9798,
                    'value': 5,
                    'y': 0.9,
                    'x': 0.5,
                    'width': 0.1
                },
                {
                    'id': 6,
                    'resource_id': 9799,
                    'value': 6,
                    'y': 0.9,
                    'x': 0.6,
                    'width': 0.1
                },
            ],
            'multiple_choices': True
        }
    }
    DEFAULT_TEST_ANSWER = {'1': 1, '2': 4, '4': 4, '5': 2}
    CHECKS = {
        'type': 'OR',
        'sources': [
            {
                'type': 'EQUAL',
                'sources': [
                    {
                        'type': 'NUMBER',
                        'source': 40,
                    },
                    {
                        'type': 'MULT',
                        'sources': [
                            {
                                'type': 'SUM',
                                'sources': [
                                    {'type': 'FIELD', 'source': 1},
                                    {'type': 'FIELD', 'source': 2},
                                ],
                            },
                            {
                                'type': 'MULT',
                                'sources': [
                                    {'type': 'FIELD', 'source': 4},
                                    {'type': 'FIELD', 'source': 5},
                                ],
                            },
                        ],
                    },
                ],
            },
            {
                'type': 'EQUAL',
                'sources': [
                    {
                        'type': 'NUMBER',
                        'source': 5,
                    },
                    {
                        'type': 'SUM',
                        'sources': [
                            {'type': 'FIELD', 'source': 1},
                            {'type': 'FIELD', 'source': 2},
                        ]
                    }
                ]
            }
        ]
    }
    CHECK_DATA = (
        (
            None,
            (Marker.SKIPPED, 1),
        ),
        (
            {'1': 1, '2': 4, '4': 4, '5': 2},
            (Marker.CORRECT, 0),
        ),
        (
            {'1': 1, '2': 4, '4': 4},
            (Marker.CORRECT, 0),
        ),
        (
            {'1': 1, '2': 5, '4': 4, '5': 2},
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
            [DEFAULT_TEST_ANSWER],
            '{} is not of type \'object\''
            .format([DEFAULT_TEST_ANSWER]),
        ),
    )

    def test_max_mistakes(self):
        """Тест максимального числа ошибок"""
        assert DragImagesMarker(self.DEFAULT_TEST_MARKUP,
                                self.DEFAULT_TEST_ANSWER,
                                self.CHECKS).max_mistakes == 1

    @pytest.mark.parametrize('answer,result', CHECK_DATA)
    def test_check(self, answer, result):
        """Проверка подсчета ошибок"""
        assert DragImagesMarker(self.DEFAULT_TEST_MARKUP,
                                self.DEFAULT_TEST_ANSWER,
                                self.CHECKS).check(answer) == result

    @pytest.mark.skip
    def test_validate_positive(self):
        """
        Проверка правильности разметки
        """
        try:
            DragImagesMarker(self.DEFAULT_TEST_MARKUP,
                             self.DEFAULT_TEST_ANSWER,
                             self.CHECKS).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    def test_validate_negative(self):
        """
        Проверка неправильной проверки в маркере
        """
        checks = deepcopy(self.CHECKS)
        checks['sources'] = {}
        with pytest.raises(ValidationError) as excinfo:
            DragImagesMarker(self.DEFAULT_TEST_MARKUP,
                             self.DEFAULT_TEST_ANSWER,
                             checks).validate()
        assert excinfo.value.message == "{'type': 'OR', 'sources': {}} is not valid under any of the given schemas"

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
            DragImagesMarker(
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
            DragImagesMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    def test_get_embedded_objects(self):
        """
        Тест нахождения ресурсов в маркере
        """
        assert DragImagesMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER,
            self.CHECKS
        ).get_embedded_objects() == [
            ('resource', 10053),
            ('resource', 9794),
            ('resource', 9795),
            ('resource', 9796),
            ('resource', 9797),
            ('resource', 9798),
            ('resource', 9799),
        ]
