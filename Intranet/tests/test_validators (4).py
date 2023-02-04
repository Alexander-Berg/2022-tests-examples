from builtins import object, str
from datetime import datetime, timedelta

import pytest

from django.core.exceptions import NON_FIELD_ERRORS, ValidationError

from rest_framework import serializers
from rest_framework.settings import api_settings

from kelvin.courses.validators import CourseLessonLinkValidator
from kelvin.lessons.models import LessonScenario
from kelvin.lessons.tests.test_validators import fields_data


class TestCourseLessonLinkValidator(object):
    """
    Тесты валидатора курсозанятия
    """

    @pytest.mark.parametrize(
        'fields_data',
        [
            (LessonScenario.CONTROL_WORK_MODE, 'evaluation_date', 'finish_date'),  # noqa
            (LessonScenario.DIAGNOSTICS_MODE, 'diagnostics_text', 'finish_date'),  # noqa
        ],
        ids=lambda p: '-'.join(str(_) for _ in p),
        indirect=True
    )
    def test_duration_positive(self, fields_data):
        """
        Проверяем, что курсозанятие с валидной длительностью проходит валидацию
        """
        now = datetime.now()

        fields_data.update({
            'duration': 10,
            'date_assignment': now - timedelta(minutes=20),
        })

        CourseLessonLinkValidator.validate(fields_data)
        CourseLessonLinkValidator.validate(
            fields_data,
            exception_class=serializers.ValidationError,
        )

    @pytest.mark.parametrize(
        'fields_data',
        [
            (LessonScenario.CONTROL_WORK_MODE, 'evaluation_date', 'finish_date'),  # noqa
            (LessonScenario.DIAGNOSTICS_MODE, 'diagnostics_text', 'finish_date'),  # noqa
        ],
        ids=lambda p: '-'.join(str(_) for _ in p),
        indirect=True
    )
    def test_duration_negative(self, fields_data):
        """
        Проверяем что duration не может быть больше интервала между
        `finish_date` и `date_assignment`
        """
        now = datetime.now()

        fields_data.update({
            'duration': 20,
            'date_assignment': now - timedelta(minutes=10),
        })

        try:
            CourseLessonLinkValidator.validate(fields_data)
        except ValidationError as ex:
            assert (
                ex.message_dict[NON_FIELD_ERRORS] ==
                [CourseLessonLinkValidator.invalid_duration_message]
            )

        try:
            CourseLessonLinkValidator.validate(
                fields_data,
                exception_class=serializers.ValidationError,
            )
        except serializers.ValidationError as ex:
            assert (
                ex.detail[api_settings.NON_FIELD_ERRORS_KEY] ==
                [CourseLessonLinkValidator.invalid_duration_message]
            )
