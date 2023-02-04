from builtins import object, str
from datetime import datetime, timedelta

import jsonschema
import pytest
from mock.mock import call

from django.core.exceptions import NON_FIELD_ERRORS, ValidationError

from rest_framework import serializers
from rest_framework.settings import api_settings

from kelvin.lessons.models import LessonProblemLink, LessonScenario, TextTemplate
from kelvin.lessons.validators import (
    COMMON_OPTIONS_SCHEMA, THEORY_OPTIONS_SCHEMA, AbstractLessonScenarioValidator, LessonProblemLinkValidator,
)


class TestLessonProblemLinkValidators(object):
    """
    Тесты валидаторов
    """

    def test_validate_options_json(self, mocker):
        """
        Тесты валидации поля `options`
        """
        mocked_jsonschema_validate = mocker.patch('jsonschema.validate')
        options = {}

        # обычный вопрос
        mocked_jsonschema_validate.reset_mock()
        assert LessonProblemLinkValidator.validate_options_json(
            options, LessonProblemLink.TYPE_COMMON) is None
        assert mocked_jsonschema_validate.mock_calls == [call(
            options, COMMON_OPTIONS_SCHEMA)]

        mocked_jsonschema_validate.reset_mock()
        assert LessonProblemLinkValidator.validate_options_json(
            None, LessonProblemLink.TYPE_COMMON) is None
        assert mocked_jsonschema_validate.mock_calls == []

        # теория
        mocked_jsonschema_validate.reset_mock()
        assert LessonProblemLinkValidator.validate_options_json(
            options, LessonProblemLink.TYPE_THEORY) is None
        assert mocked_jsonschema_validate.mock_calls == [call(
            options, THEORY_OPTIONS_SCHEMA)]

        mocked_jsonschema_validate.reset_mock()
        assert LessonProblemLinkValidator.validate_options_json(
            None, LessonProblemLink.TYPE_COMMON) is None
        assert mocked_jsonschema_validate.mock_calls == []

        # ошибка при валидации json
        mocked_jsonschema_validate.reset_mock()
        mocked_jsonschema_validate.side_effect = jsonschema.ValidationError(
            'text')

        with pytest.raises(ValidationError) as excinfo:
            LessonProblemLinkValidator.validate_options_json(options)
        assert excinfo.value.message == 'text', u'Неправильный текст ошибки'
        assert mocked_jsonschema_validate.mock_calls == [call(
            options, COMMON_OPTIONS_SCHEMA)]


@pytest.fixture
def fields_data(request):
    """
    Генерирует набор основных полей для тестирования валидатора.
    Настраивается через параметризацию теста.
    """
    fields_factories = {
        'finish_date': datetime.now,
        'evaluation_date': lambda: datetime.now() + timedelta(seconds=10),
        'duration': lambda: 100,
        'diagnostics_text': lambda: TextTemplate(name='text'),
    }

    fields_dict = {field: None for field in fields_factories}

    if hasattr(request, 'param'):
        fields_dict['mode'] = request.param[0]

        for field in request.param[1:]:
            fields_dict[field] = fields_factories[field]()
    else:
        fields_dict['mode'] = LessonScenario.WEBINAR_MODE

    return fields_dict


@pytest.fixture(params=(True, False))
def allow_time_limited_problems(request):
    return {'allow_time_limited_problems': request.param}


@pytest.fixture(
    params=(
        LessonScenario.VisualModes.SEPARATE,
        LessonScenario.VisualModes.BLOCKS,
    )
)
def visual_mode(request):
    return {'visual_mode': request.param}


class TestAbstractLessonScenarioValidator(object):

    # только валидные случаи
    LESSON_SCENARIO_SETUP_VALID = (
        # mode, passed fields
        (LessonScenario.TRAINING_MODE, 'finish_date', 'evaluation_date'),                  # noqa
        (LessonScenario.WEBINAR_MODE, 'duration'),
        (LessonScenario.CONTROL_WORK_MODE, 'duration', 'finish_date', 'evaluation_date'),  # noqa
        (LessonScenario.DIAGNOSTICS_MODE, 'duration', 'finish_date', 'diagnostics_text'),  # noqa
    )

    # только невалидные случаи
    LESSON_SCENARIO_SETUP_NOT_VALID = (
        (LessonScenario.CONTROL_WORK_MODE, 'duration'),
        (LessonScenario.CONTROL_WORK_MODE, 'duration'),
        (LessonScenario.CONTROL_WORK_MODE, 'duration', 'evaluation_date'),                # noqa
        (LessonScenario.DIAGNOSTICS_MODE, 'duration', 'finish_date', 'evaluation_date'),  # noqa
        (LessonScenario.DIAGNOSTICS_MODE, 'finish_date', 'evaluation_date'),              # noqa
    )

    @pytest.mark.parametrize(
        'fields_data',
        LESSON_SCENARIO_SETUP_NOT_VALID,
        ids=lambda p: '-'.join(str(_) for _ in p),
        indirect=True
    )
    def test_invalid_required_fields(self, fields_data):
        """
        Проверяем поведение валидации на невалидных данных.

        :type fields_data: dict
        """
        lesson_scenario = LessonScenario(**fields_data)

        with pytest.raises(ValidationError):
            lesson_scenario.clean()

        with pytest.raises(ValidationError):
            AbstractLessonScenarioValidator.validate(fields_data)

        with pytest.raises(serializers.ValidationError):
            AbstractLessonScenarioValidator.validate(
                fields_data,
                exception_class=serializers.ValidationError
            )

    @pytest.mark.parametrize(
        'fields_data',
        LESSON_SCENARIO_SETUP_VALID,
        ids=lambda p: '-'.join(str(_) for _ in p),
        indirect=True
    )
    def test_valid_required_fields(self, fields_data):
        """
        Проверяем поведение валидации на валидных данных.

        :type fields_data: dict
        """
        # если исключений не случилось – значит все хорошо
        lesson_scenario = LessonScenario(**fields_data)
        lesson_scenario.clean()

        AbstractLessonScenarioValidator.validate(fields_data)
        AbstractLessonScenarioValidator.validate(
            fields_data,
            exception_class=serializers.ValidationError
        )

    @pytest.mark.parametrize(
        'fields_data',
        LESSON_SCENARIO_SETUP_NOT_VALID,
        ids=lambda p: '-'.join(str(_) for _ in p),
        indirect=True
    )
    def test_errors_format(self, fields_data):
        """
        Тест формата ошибок, которые попадает в исключение, если
        валидация не пройдена.

        :type fields_data: dict
        """
        mode = fields_data['mode']

        error_messages = {
            LessonScenario.CONTROL_WORK_MODE: (
                u'Это обязательное поле для режима занятия '
                u'"Контрольная работа"'
            ),
            LessonScenario.DIAGNOSTICS_MODE: (
                u'Это обязательное поле для режима занятия '
                u'"Диагностика"'
            ),
        }

        expected_errors = {
            field: [error_messages[mode]] for field in fields_data
            if not fields_data[field] and field in
            AbstractLessonScenarioValidator.required_fields[mode]
        }

        try:
            AbstractLessonScenarioValidator.validate(fields_data)
        except ValidationError as ex:
            assert ex.message_dict == expected_errors

    @pytest.mark.parametrize(
        'fields_data',
        [
            (LessonScenario.CONTROL_WORK_MODE, ),
            (LessonScenario.DIAGNOSTICS_MODE, ),
            (LessonScenario.TRAINING_MODE, ),
            (LessonScenario.WEBINAR_MODE, ),
        ],
        ids=lambda p: str(p[0]),
        indirect=True
    )
    def test_dates(self, fields_data):
        """
        Проверяем что finish_date не может быть больше evaluation_date
        """
        now = datetime.now()

        fields_data.update({
            'finish_date': now,
            'evaluation_date': now - timedelta(seconds=10),
        })

        try:
            AbstractLessonScenarioValidator.validate(fields_data)
        except ValidationError as ex:
            assert (
                ex.message_dict[NON_FIELD_ERRORS] ==
                [AbstractLessonScenarioValidator.default_messages['dates']]
            )

        try:
            AbstractLessonScenarioValidator.validate(
                fields_data,
                exception_class=serializers.ValidationError,
            )
        except serializers.ValidationError as ex:
            assert (
                ex.detail[api_settings.NON_FIELD_ERRORS_KEY] ==
                AbstractLessonScenarioValidator.default_messages['dates']
            )

    @pytest.mark.parametrize(
        'fields_data',
        [
            (LessonScenario.CONTROL_WORK_MODE, ),
            (LessonScenario.DIAGNOSTICS_MODE, ),
            (LessonScenario.TRAINING_MODE, ),
            (LessonScenario.WEBINAR_MODE, ),
        ],
        ids=lambda p: str(p[0]),
        indirect=True
    )
    def test_allow_time_limited_problems(
        self,
        fields_data,
        allow_time_limited_problems,
        visual_mode
    ):
        """
        Проверяем что:
            allow_time_limited_problems разрешено только в TRAINING_MODE
            allow_time_limited_problems несовместимо с VisualModes.BLOCKS

        """
        fields_data.update(allow_time_limited_problems)
        fields_data.update(visual_mode)

        msg_no_blocks = (
            AbstractLessonScenarioValidator.default_messages.get(
                'allow_time_limited_problems_no_blocks')
        )
        msg_training_only = (
            AbstractLessonScenarioValidator.default_messages.get(
                'allow_time_limited_problems_training_only')
        )

        visual_mode_val = list(visual_mode.values())[0]
        allow_time_limited_problems_val = (
            list(allow_time_limited_problems.values())[0])
        mode = fields_data['mode']

        # allow_time_limited_problems разрешено только в TRAINING_MODE
        if (
            allow_time_limited_problems_val and
            mode != LessonScenario.TRAINING_MODE
        ):
            with pytest.raises(ValidationError) as ex:
                AbstractLessonScenarioValidator.validate(fields_data)
            assert msg_training_only in ex.value.messages
        else:
            try:
                AbstractLessonScenarioValidator.validate(fields_data)
            except ValidationError as ex:
                assert msg_training_only not in ex.messages

        # allow_time_limited_problems несовместимо с VisualModes.BLOCKS
        if (
            allow_time_limited_problems_val and
            visual_mode_val == LessonScenario.VisualModes.BLOCKS
        ):
            with pytest.raises(ValidationError) as ex:
                AbstractLessonScenarioValidator.validate(fields_data)
            assert msg_no_blocks in ex.value.messages
        else:
            try:
                AbstractLessonScenarioValidator.validate(fields_data)
            except ValidationError as ex:
                assert msg_no_blocks not in ex.messages
