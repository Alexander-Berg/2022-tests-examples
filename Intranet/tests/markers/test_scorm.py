from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import Marker, ScormMarker


class TestScormMarker(object):
    """
    Scorm marker test
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'scorm',
        'options': {
            'sco': {
                'id': 1,
            },
        },
    }
    DEFAULT_TEST_ANSWER = {}
    VALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            {},
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
    CHECK_DATA = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            {},
            Marker.CORRECT,
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            {'a': 'b'},
            Marker.INCORRECT,
        ),
    )

    def test_validate_positive(self):
        """
        Test validation markup in scorm marker
        """
        try:
            ScormMarker(
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
        Right user answer format in scorm marker
        """
        try:
            ScormMarker(
                markup,
                marker_answer,
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
        Wrong user answer format in scorm marker
        """
        with pytest.raises(ValidationError) as excinfo:
            ScormMarker(
                markup,
                marker_answer,
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('marker,answer,user_answer,result', CHECK_DATA)
    def test_check(self, marker, answer, user_answer, result):
        """
        Test errors count
        """
        assert ScormMarker(marker, answer).check(user_answer) == result
