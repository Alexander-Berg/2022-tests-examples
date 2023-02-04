from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import Marker, TextAndFileMarker


class TestTextAndFileMarker(object):
    """
    Тесты маркера ввода произвольного текста или файла
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'textandfile',
        'options': {
            'no_file': False,
            'no_textarea': False,
        },
    }
    DEFAULT_TEST_ANSWER = {
        'files': [
            'https://edu-prod.s3.amazonaws.com'
            '/resources/3_class_3_1449052368.png',
            'https://edu-prod.s3.amazonaws.com'
            '/resources/3_class_1_1449052368.png',
        ],
        'text': 'Something',
    }
    CHECK_DATA = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            None,
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            {
                'files': [
                    'https://edu-prod.s3.amazonaws.com'
                    '/resources/3_class_3_1449052368.png',
                    'https://edu-prod.s3.amazonaws.com'
                    '/resources/3_class_1_1449052368.png',
                ],
                'text': 'Something',
            },
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            {
                'files': [],
                'text': '',
            },
        ),
    )
    VALID_FORMAT = (
        (
            {
                'type': 'textandfile',
                'options': {},
            },
            {}
        ),
        (
            {
                'type': 'textandfile',
                'options': {
                    'hide_file': True,
                    'hide_textarea': True,
                },
            },
            {
                'files': [
                    'https://edu-prod.s3.amazonaws.com'
                    '/resources/3_class_3_1449052368.png',
                    'https://edu-prod.s3.amazonaws.com/resources'
                    '/3_class_1_1449052368.png',
                ],
                'text': 'Something',
            }
        ),
        (
            {
                'type': 'textandfile',
                'options': {
                    'hide_file': True,
                },
            },
            {}
        ),
        (
            {
                'type': 'textandfile',
                'options': {
                    'hide_textarea': True,
                },
            },
            {}
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
        assert TextAndFileMarker(
            self.DEFAULT_TEST_MARKUP,
            self.DEFAULT_TEST_ANSWER
        ).max_mistakes == 1

    @pytest.mark.parametrize('markup,answer,user_answer', CHECK_DATA)
    def test_check(self, markup, answer, user_answer):
        """
        Проверка подсчета ошибок
        """
        assert TextAndFileMarker(markup, answer).check(user_answer) == (
            Marker.UNCHECKED, None)

    @pytest.mark.parametrize('marker_data,marker_answer', VALID_FORMAT)
    def test_validate_positive(self, marker_data, marker_answer):
        """
        Примеры правильной разметки маркера ввода
        """
        try:
            TextAndFileMarker(marker_data, marker_answer).validate()
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
        маркера ввода
        """
        try:
            TextAndFileMarker(
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
        маркера ввода
        """
        with pytest.raises(ValidationError) as excinfo:
            TextAndFileMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')
