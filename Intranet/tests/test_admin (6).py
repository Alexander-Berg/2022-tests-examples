from builtins import object

import pytest
from mock import call

from django import forms
from django.core.exceptions import ValidationError as DjangoValidationError

from kelvin.problems.admin.forms import ProblemAdminForm


class TestProblemAdminForm(object):
    """
    Тесты формы задачи в админке
    """

    def test_clean(self, mocker):
        """
        Проверка метода `clean`
        """
        mocked_model_form_clean = mocker.patch.object(forms.ModelForm, 'clean')
        mocked_validate_markup = mocker.patch(
            'kelvin.problems.validators.ProblemValidators.validate_markup')
        mocked_validate_markup_json = mocker.patch(
            'kelvin.problems.validators.ProblemValidators'
            '.validate_markup_json')
        mocked_validate_markup_marker_types = mocker.patch(
            'kelvin.problems.validators.ProblemValidators'
            '.validate_markup_marker_types'
        )
        mocker.patch.object(ProblemAdminForm, '_meta')

        # нет исключений
        data = {'markup': 1}
        return_resources = ['1', '2']
        mocked_validate_markup.return_value = return_resources
        mocked_validate_markup_marker_types.return_value = None
        expected_data = {'markup': 1, 'resources': return_resources}
        form = ProblemAdminForm()
        mocked_model_form_clean.return_value = data
        assert form.clean() == expected_data, (
            u'В результате должно добавиться поле `resources`')
        assert mocked_validate_markup.mock_calls == [call(1)], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_model_form_clean.mock_calls == [call()], (
            u'Должен быть вызван родительский метод `clean`')
        assert mocked_validate_markup_marker_types.mock_calls == [call(1)], (
            u'Должна вызываться проверка типов маркеров')
        assert mocked_validate_markup_json.mock_calls == [call(1)], (
            u'Должны вызываться валидация json-схемы')

        # возникает исключение при проверке разметки
        mocked_validate_markup_json.reset_mock()
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_marker_types.reset_mock()
        mocked_validate_markup.side_effect = DjangoValidationError('message')
        data = {'markup': 1}
        form = ProblemAdminForm()
        form.cleaned_data = data
        with pytest.raises(DjangoValidationError) as excinfo:
            form.clean()
        assert 'markup' in excinfo.value.error_dict, (
            u'Неправильно сформировано сообщение об ошибке')
        assert excinfo.value.error_dict['markup'][0].messages == ['message'], (
            u'Неправильно сформировано сообщение об ошибке')
        assert mocked_validate_markup.mock_calls == [call(1)], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_validate_markup_marker_types.mock_calls == [call(1)], (
            u'Должна вызываться проверка типов маркеров')
        assert mocked_validate_markup_json.mock_calls == [call(1)], (
            u'Должны вызываться валидация json-схемы')

        # возникает исключение при проверке типов маркеров
        mocked_validate_markup_json.reset_mock()
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_marker_types.reset_mock()
        mocked_validate_markup_marker_types.side_effect = (
            DjangoValidationError('msg'))
        with pytest.raises(DjangoValidationError) as excinfo:
            form.clean()
        assert 'markup' in excinfo.value.error_dict, (
            u'Неправильно сформировано сообщение об ошибке')
        assert excinfo.value.error_dict['markup'][0].messages == ['msg'], (
            u'Неправильно сформировано сообщение об ошибке')
        assert mocked_validate_markup_marker_types.mock_calls == [call(1)], (
            u'Должна вызываться проверка типов маркеров')
        assert mocked_validate_markup.mock_calls == [], (
            u'При проваленной проверке типов маркеров не должна вызываться '
            u'валидация разметки'
        )

        # если у формы уже есть ошибки в поле, то не делаем проверку
        mocked_validate_markup_json.reset_mock()
        mocked_validate_markup.reset_mock()
        mocked_model_form_clean.reset_mock()
        form = ProblemAdminForm()
        form.errors['markup'] = ['error']
        data = {'markup': 1}
        mocked_model_form_clean.return_value = data
        assert form.clean() == data, u'Данные должны вернуться без изменений'
        assert mocked_validate_markup.mock_calls == [], (
            u'Валидация поля разметки не должна быть вызвана')
        assert mocked_model_form_clean.mock_calls == [call()], (
            u'Должен быть вызван родительский метод `clean`')
        assert mocked_validate_markup_json.mock_calls == [], (
            u'Должны вызываться валидация json-схемы')
