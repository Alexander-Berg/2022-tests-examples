from builtins import object
from copy import deepcopy

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import DragTablesMarker


class TestDragTablesMarker(object):
    """
    Тесты маркера перетаскивания в таблице
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'dragtable',
        'options': {
            'style': 'AMAZING_TABLE_WITH_PINK_HEADERS',
            'rows': [
                {
                    'header': True,
                    'cells': [
                        {'header': True},
                        {'header': True, 'text': 'Животные'},
                        {'header': True, 'text': 'Мурлыкают'},
                    ],
                },
                {
                    'cells': [
                        {'header': True, 'image': 1231},
                        {'id': 1, 'choice': 2},
                        {'id': 2},
                    ]
                },
                {
                    'cells': [
                        {'header': True, 'text': 'Собаки'},
                        {'id': 3},
                        {'id': 4},
                    ],
                },
            ],
            'choices': [
                {'id': 1, 'image': 9794, 'value': False},
                {'id': 2, 'image': 9795, 'value': True},
            ],
            'multiple_choices': True,
        },
    }
    DEFAULT_TEST_ANSWER = {'1': 2, '2': 2, '3': 2, '4': 1}
    CHECK = {
        'type': 'AND',
        'sources': [
            {'type': 'FIELD', 'source': 1},
            {'type': 'FIELD', 'source': 2},
            {'type': 'FIELD', 'source': 3},
            {
                'type': 'NOT',
                'source': {'type': 'FIELD', 'source': 4},
            },
        ],
    }
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
            DragTablesMarker(self.DEFAULT_TEST_MARKUP,
                             self.DEFAULT_TEST_ANSWER,
                             self.CHECK).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    def test_validate_negative(self):
        """
        Проверка неправильной проверки в маркере
        """
        checks = deepcopy(self.CHECK)
        checks['sources'] = {}
        with pytest.raises(ValidationError) as excinfo:
            DragTablesMarker(self.DEFAULT_TEST_MARKUP,
                             self.DEFAULT_TEST_ANSWER,
                             checks).validate()
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
            DragTablesMarker(
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
            DragTablesMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    def test_get_embedded_objects(self):
        """
        Тест нахождения ресурсов в маркере
        """
        assert DragTablesMarker(self.DEFAULT_TEST_MARKUP,
                                self.DEFAULT_TEST_ANSWER,
                                self.CHECK).get_embedded_objects() == [
            ('resource', 9794),
            ('resource', 9795),
            ('resource', 1231),
        ]
