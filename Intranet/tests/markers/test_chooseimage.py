from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import ChooseImageMarker, Marker


class TestChooseimageMarker(object):
    """
    Тесты маркера соединения областей
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'chooseimage',
        'options': {
            'hit_areas': [
                {
                    'x': 0.3,
                    'y': 0.5,
                    'width': 0.2,
                    'height': 0.1,
                    'id': 'e1',
                },
                {
                    'x': 0.2,
                    'y': 0.1,
                    'width': 0.2,
                    'height': 0.1,
                    'id': 'e2',
                },
                {
                    'x': 0.3,
                    'y': 0.2,
                    'width': 0.2,
                    'height': 0.1,
                    'id': 'e2',
                },
            ],
            'image_file': 10831,
        },
    }
    DEFAULT_TEST_ANSWER = [
        ['e0', 'e1'],
        ['e0', 'e2'],
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
    COMPARE_ANSWERS_DATA = (
        (
            ['e1', 'e2'],
            ['e1', 'e2'],
            ([Marker.CORRECT, Marker.CORRECT], 0),
        ),
        (
            ['e2'],
            ['e1', 'e2'],
            ([Marker.CORRECT], 1),
        ),
        (
            ['e2', 'e0'],
            ['e1', 'e2'],
            ([Marker.CORRECT, Marker.INCORRECT], 2),
        ),
        (
            ['e0', 'e2', 'e1'],
            ['e1', 'e2'],
            ([Marker.INCORRECT, Marker.CORRECT, Marker.CORRECT], 1),
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
            ['e1', 'e0'],
            (
                {
                    'compared_with': 0,
                    'status': [Marker.CORRECT, Marker.CORRECT],
                },
                0,
            ),
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            ['e2', 'e3'],
            (
                {
                    'compared_with': 1,
                    'status': [Marker.CORRECT, Marker.INCORRECT],
                },
                1,
            ),
        ),
    )

    def test_validate_positive(self):
        """
        Пример правильной разметки маркера выбора области
        """
        try:
            ChooseImageMarker(
                self.DEFAULT_TEST_MARKUP,
                self.DEFAULT_TEST_ANSWER
            ).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer',
        VALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_positive(
            self, markup, marker_answer, user_answer):
        """
        Примеры правильного пользовательского ответа
        маркера выбора области
        """
        try:
            ChooseImageMarker(
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
        маркера выбора области
        """
        with pytest.raises(ValidationError) as excinfo:
            ChooseImageMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    def test_max_mistakes(self):
        """Тест максимального числа ошибок"""
        assert ChooseImageMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).max_mistakes == 1

    def test_get_embedded_objects(self):
        """
        Тест нахождения ресурсов в маркере
        """
        assert (ChooseImageMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).get_embedded_objects() == [('resource', 10831)])

    @pytest.mark.parametrize('answer,right_answer,result',
                             COMPARE_ANSWERS_DATA)
    def test_compare_answers(self, answer, right_answer, result):
        """Проверка сравнения ответов"""
        assert ChooseImageMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        )._compare_answers(
            answer,
            right_answer
        ) == result

    @pytest.mark.parametrize('marker,answer,user_answer,result', CHECK_DATA)
    def test_check(self, marker, answer, user_answer, result):
        """
        Проверка подсчета ошибок

        Здесь же проверка `_compare_answers`
        """
        assert ChooseImageMarker(marker, answer).check(user_answer) == result
