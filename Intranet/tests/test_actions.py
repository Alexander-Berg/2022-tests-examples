import pytest

from django.core.exceptions import ValidationError

from kelvin.courses.models.criterion.action import validate_actions


class TestActionValidator:
    def test_invalid_my_action(self):
        with pytest.raises(ValidationError):
            validate_actions([{"type": "MYACT1"}])

    def test_valid_my_action(self):
        assert validate_actions([{"type": "MYACT"}]) is None

    def test_course_completion_valid(self):
        assert validate_actions([{"type": "COURSE_COMPLETION"}]) is None
        assert validate_actions([{"type": "COURSE_COMPLETION", "extra_courses": []}]) is None
        assert validate_actions([{"type": "COURSE_COMPLETION", "extra_courses": [1,2,3,4]}]) is None

    def test_course_completion_invalid(self):
        with pytest.raises(ValidationError):
            validate_actions([{"type": "COURSE_COMPLETION", "extra_courses": ""}])
        with pytest.raises(ValidationError):
            validate_actions([{"type": "COURSE_COMPLETION", "extra_courses": ["1", "2"]}])

    def test_clesson_completion_invalid(self):
        with pytest.raises(ValidationError):
            validate_actions([{"type": "CLESSON_COMPLETION1"}])

    def test_clesson_completion_valid(self):
        assert validate_actions([{"type": "CLESSON_COMPLETION"}]) is None

    ACTION_REQUEST_DATA_INVALID = (
        (
            {
                "achievement_id": -1,
                "level": -2,
                "comment": "qwerty",
            }
        ),
        (
            {
                "achievement_id": 1,
                "level": -2,
                "comment": "qwerty",
            }
        ),
        (
            {
                "achievement_id": -1,
                "level": -1,
                "comment": "qwerty",
            }
        ),
        (
            {
                "achievement_id": 1,
                "level": -1,
            }
        ),
    )

    @pytest.mark.parametrize('request_data', ACTION_REQUEST_DATA_INVALID)
    def test_request_achievement_action_invalid(self, request_data):
        request_dict = {
            **{"type": "REQUEST_ACHIEVEMENT_ACTION"}, **request_data,
        }

        with pytest.raises(ValidationError):
            validate_actions(
                [
                    request_dict,
                ]
            )

    ACTION_REQUEST_DATA_VALID = (
        (
            {
                "achievement_id": 1,
                "level": "-1",
                "comment": "qwerty",
            }
        ),
        (
            {
                "achievement_id": 1,
                "level": "1",
                "comment": "qwerty",
            }
        ),
    )

    @pytest.mark.parametrize('request_data', ACTION_REQUEST_DATA_VALID)
    def test_request_achievement_action_valid(self, request_data):
        request_dict = {
            **{"type": "REQUEST_ACHIEVEMENT_ACTION"}, **request_data,
        }

        assert validate_actions([request_dict]) is None
