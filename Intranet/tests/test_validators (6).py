from builtins import object, str

import jsonschema
import pytest
from mock import call

from django.core.exceptions import ValidationError

from kelvin.problems.constants import COMMON_MARKUP_SCHEMA
from kelvin.problems.tests.data.text_resource_data import (
    problem_markup_with_invalid_objects_ids, problem_markup_with_valid_objects_ids, text_resources_valid,
    text_resources_with_invalid_formula_ids, text_resources_with_invalid_resource_ids,
)
from kelvin.problems.validators import ProblemValidators, TextResourceValidators


def test_validate_markup_json(mocker):
    """
    Проверка валидации json
    """
    mocked_jsonschema_validate = mocker.patch('jsonschema.validate')
    markup = 'markup'

    # успешная проверка
    assert ProblemValidators.validate_markup_json(
        markup) is None, u'Валидатор должен возвращать `None`'
    assert mocked_jsonschema_validate.mock_calls == [
        call(markup, COMMON_MARKUP_SCHEMA)], (
        u'Валидация `json` вызвана с неправильными параметрами')

    # есть исключение
    mocked_jsonschema_validate.side_effect = jsonschema.ValidationError('text')
    with pytest.raises(ValidationError) as excinfo:
        ProblemValidators.validate_markup_json(markup)
    assert excinfo.value.message == 'text', u'Неправильный текст ошибки'


class TestTextResourceValidators(object):
    """
    Проверка валидаторов модели TextResource
    """
    @pytest.mark.parametrize('content,formulas,existing_resources',
                             text_resources_valid)
    def test_validate_content_valid(self, mocker,
                                    content, formulas, existing_resources):
        """
        Проверяем работу метода validate_content на валидных данных

        :param existing_resources: массив айдишников существующих ресурсов
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)

        assert TextResourceValidators.validate_content(
            content) == set(existing_resources)

    @pytest.mark.parametrize('content,existing_resources,error',
                             text_resources_with_invalid_resource_ids)
    def test_validate_content_invalid_resources(self, mocker, content,
                                                existing_resources, error):
        """
        Проверяем работу метода validate_content на невалидных ресурсах

        :param existing_resources: массив айдишников существующих ресурсов
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)

        with pytest.raises(ValidationError) as excinfo:
            TextResourceValidators.validate_content(content)
        assert excinfo.value.message == error

    @pytest.mark.parametrize('content,formulas,error',
                             text_resources_with_invalid_formula_ids)
    def test_validate_formula_set_negative(self, content, formulas, error):
        """
        Тесты валидации при несоответствии формул в `content` формулам в
        `formulas`
        """
        with pytest.raises(ValidationError) as excinfo:
            TextResourceValidators.validate_formula_set(content, formulas)
        assert excinfo.value.message == error


class TestProblemValidators(object):
    """
    Проверка валидаторов
    """
    @pytest.mark.parametrize('markup,existing_resources',
                             problem_markup_with_valid_objects_ids)
    def test_validate_markup_valid(self, mocker, markup, existing_resources):
        """
        Проверка валидации поля разметки задачи в случае правильной разметки
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)
        assert ProblemValidators.validate_markup(
            markup) == existing_resources, u'Неправильные значения ресурсов'

    @pytest.mark.parametrize('markup,existing_resources,errors',
                             problem_markup_with_invalid_objects_ids)
    def test_validate_markup_invalid(self, mocker, markup, existing_resources,
                                     errors):
        """
        Проверка валидации поля разметки задачи в случае неправильной разметки
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)

        with pytest.raises(ValidationError) as excinfo:
            ProblemValidators.validate_markup(markup)
        assert excinfo.value.messages == errors, u'Неправильный список ошибок'

    @pytest.mark.parametrize('markup,existing_resources',
                             problem_markup_with_valid_objects_ids)
    def test_validate_markup_newlines(self, mocker, markup,
                                      existing_resources):
        """
        Проверка удаления \r из разметки
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)
        ProblemValidators.validate_markup(markup)
        assert '\\r' not in str(markup)
