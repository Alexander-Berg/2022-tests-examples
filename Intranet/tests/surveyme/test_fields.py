# -*- coding: utf-8 -*-
import datetime

from dateutil import parser

from django import forms
from django.utils import timezone
from django.test import TestCase
from django.test.utils import override_settings
from django.core.exceptions import ValidationError
from django.core import validators

from events.surveyme.fields.base.fields import (
    PhoneField,
    SimpleDateField,
    DateRangeField,
)
from events.surveyme.fields.base.widgets import DateRangeFieldInput
from events.surveyme.fields.base.validators import validate_phone


@override_settings(SURVEYME_DEFAULT_PHONE_COUNTRY_CODE='RU')
class TestPhoneField__get_formatted_phone_number(TestCase):
    def setUp(self):
        self.field = PhoneField()

    def test_must_format_phone(self):
        experiments = [
            {'raw_phone': '+7 916 1234590', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '+7 916 123-45-90', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '+7 916 123 45 90', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '+79161234590', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '9161234590', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '4991234567', 'exp_formatted_phone': '+7 499 123-45-67'},
            {'raw_phone': '4951234567', 'exp_formatted_phone': '+7 495 123-45-67'},
            {'raw_phone': '84951234567', 'exp_formatted_phone': '+7 495 123-45-67'},
            {'raw_phone': '+74951234567', 'exp_formatted_phone': '+7 495 123-45-67'},
            {'raw_phone': '00 3445678', 'exp_formatted_phone': '003445678'},
            {'raw_phone': '00 910 344-56-78', 'exp_formatted_phone': '009103445678'},
        ]
        for experiment in experiments:
            formatted_phone = self.field._get_formatted_phone_number(experiment['raw_phone'])

            msg = (
                'Отформатированное значение телефона не соответствует ожидаемому "%s" != "%s".'
                % (str(formatted_phone), experiment['exp_formatted_phone'])
            )
            self.assertEqual(formatted_phone, experiment['exp_formatted_phone'], msg=msg)


@override_settings(SURVEYME_DEFAULT_PHONE_COUNTRY_CODE='RU')
class TestPhoneField_clean(TestCase):
    def setUp(self):
        self.field = PhoneField()

    def test_default_country_code(self):
        msg = 'Неверный дефолтный код страны'
        self.assertEqual(self.field.default_country_code, 'RU', msg=msg)

    def test_default_validators(self):
        msg = 'Неправильный список default_validators'
        self.assertEqual(self.field.default_validators, [validate_phone], msg=msg)

    def test_must_clean_with_valid_phones(self):
        experiments = [
            {'raw_phone': '+7 916 1234590', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '+7 916 123-45-90', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '+7 916 123 45 90', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '+79161234590', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '9161234590', 'exp_formatted_phone': '+7 916 123-45-90'},
            {'raw_phone': '4991234567', 'exp_formatted_phone': '+7 499 123-45-67'},
            {'raw_phone': '4951234567', 'exp_formatted_phone': '+7 495 123-45-67'},
            {'raw_phone': '84951234567', 'exp_formatted_phone': '+7 495 123-45-67'},
            {'raw_phone': '+74951234567', 'exp_formatted_phone': '+7 495 123-45-67'},
        ]
        for experiment in experiments:
            formatted_phone = self.field.clean(experiment['raw_phone'])

            msg = (
                'Отформатированное значение телефона не соответствует ожидаемому "%s" != "%s".'
                % (str(formatted_phone), experiment['exp_formatted_phone'])
            )
            self.assertEqual(formatted_phone, experiment['exp_formatted_phone'], msg=msg)

    def test_must_raise_error_with_invalid_phones(self):
        invalid_phones = [
            '+7',
            '+',
            '',
            'test',
        ]
        for invalid_phone in invalid_phones:
            for is_required in (True, False):
                self.field.required = is_required
                try:
                    self.field.clean(invalid_phone)
                    raised = False
                except ValidationError:
                    raised = True

                msg = (
                    'Телефон не валиден. Должно быть вызвано исключение '
                    'ValidationError. (required: %s)'
                ) % is_required
                if invalid_phone in validators.EMPTY_VALUES and not is_required:
                    self.assertFalse(raised, msg=msg)
                else:
                    self.assertTrue(raised, msg=msg)


class TestSimpleDateField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.field = SimpleDateField()

    def test_strptime_with_valid_string_date_value(self):
        value = '2015-01-01'
        exp_date = timezone.make_aware(parser.parse(value), timezone.utc).date()

        result = self.field.strptime(value, '%Y-%m-%d')
        msg = 'Неправильно распарсилась дата из строки'
        self.assertEqual(result, exp_date, msg=msg)
        self.assertEqual(result.year, 2015, msg=msg)
        self.assertEqual(result.month, 1, msg=msg)
        self.assertEqual(result.day, 1, msg=msg)

    def test_to_python(self):
        experiments = [
            {'value': None, 'required': False},
            {'value': '2015-01-01', 'required': False},
            {'value': None, 'required': True},
            {'value': '2015-01-01', 'required': True},
        ]
        for experiment in experiments:
            self.field.required = experiment['required']
            if experiment['value']:
                exp_date = timezone.make_aware(parser.parse(experiment['value']), timezone.utc).date()
            else:
                exp_date = None

            try:
                result = self.field.to_python(experiment['value'])
                raised = False
            except ValidationError:
                raised = True

            if not raised:
                self.assertEqual(result['date_start'], exp_date)
            if experiment['required'] and not experiment['value']:
                msg = 'ValidationError должен был вызваться, если поле обязательное, а значения нет, exp: %s' % experiment
                self.assertTrue(raised, msg=msg)

    def test_prepare_value(self):
        experiments = [
            {'date_start': None},
            {'date_start': '2015-01-01'},
        ]
        for experiment in experiments:
            if experiment['date_start']:
                date_start = timezone.make_aware(parser.parse(experiment['date_start']), timezone.utc)
            else:
                date_start = None
            answer_date = {
                'date_start': date_start,
            }
            msg = 'prepare_value неправильно подготовил значение даты'
            self.assertEqual(self.field.prepare_value(answer_date), date_start, msg=msg)


class TestDateRangeField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.field = DateRangeField()

    def test_default_widget(self):
        self.assertTrue(isinstance(self.field.widget, DateRangeFieldInput))

    def test_default_field(self):
        msg = 'Должно быть два вложенных поля'
        self.assertEqual(len(self.field.fields), 2, msg=msg)

        msg = 'Поле должно быть инстансом DateField'
        for field in self.field.fields:
            self.assertTrue(isinstance(field, forms.DateField), msg=msg)

    def test_validate_is_date_end_after_date_start_must_raise_exc_if_not_valid(self):
        experiments = [
            {
                'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'date_end': datetime.datetime.strptime('2015-01-02', '%Y-%m-%d'),
                'should_raise_error': False,
            },
            {
                'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'date_end': datetime.datetime.strptime('2014-01-01', '%Y-%m-%d'),
                'should_raise_error': True,
            },
        ]
        for experiment in experiments:
            answer_date = {
                'date_start': timezone.make_aware(experiment['date_start'], timezone.utc),
                'date_end': timezone.make_aware(experiment['date_end'], timezone.utc),
            }
            try:
                self.field._validate_is_date_end_after_date_start(answer_date)
                raised = False
            except ValidationError:
                raised = True

            msg = 'Exp: %s' % experiment
            self.assertEqual(raised, experiment['should_raise_error'], msg=msg)

    def test_equal_date_start_and_date_end_is_valid(self):
        experiment = {
            'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
            'date_end': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
            'should_raise_error': False,
        }
        answer_date = {
            'date_start': timezone.make_aware(experiment['date_start'], timezone.utc),
            'date_end': timezone.make_aware(experiment['date_end'], timezone.utc),
        }
        try:
            self.field._validate_is_date_end_after_date_start(answer_date)
            raised = False
        except ValidationError:
            raised = True

        msg = 'Exp: %s' % experiment
        self.assertEqual(raised, experiment['should_raise_error'], msg=msg)

    def test_validate_is_date_start_and_date_end_exists_must_raise_exc_if_one_value_is_not_exists(self):
        experiments = [
            {
                'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'date_end': datetime.datetime.strptime('2015-01-02', '%Y-%m-%d'),
                'should_raise_error': False,
            },
            {
                'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'date_end': None,
                'should_raise_error': True,
            },
            {
                'date_start': None,
                'date_end': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'should_raise_error': True,
            }
        ]
        for experiment in experiments:
            answer_date = {}
            if experiment['date_start']:
                answer_date['date_start'] = timezone.make_aware(experiment['date_start'], timezone.utc)
            if experiment['date_end']:
                answer_date['date_end'] = timezone.make_aware(experiment['date_end'], timezone.utc)
            try:
                self.field._validate_is_date_start_and_date_end_exists(answer_date)
                raised = False
            except ValidationError:
                raised = True

            msg = 'Exp: %s' % experiment
            self.assertEqual(raised, experiment['should_raise_error'], msg=msg)

    def test_compress_must_return_answer_date_model(self):
        experiments = [
            {
                'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'date_end': datetime.datetime.strptime('2015-01-02', '%Y-%m-%d'),
                'should_raise_error': False,
            },
            {
                'date_start': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'date_end': None,
                'should_raise_error': True,
            },
            {
                'date_start': None,
                'date_end': datetime.datetime.strptime('2015-01-01', '%Y-%m-%d'),
                'should_raise_error': True,
            }
        ]
        for experiment in experiments:
            if experiment['date_start']:
                date_start = timezone.make_aware(experiment['date_start'], timezone.utc)
            else:
                date_start = None
            if experiment['date_end']:
                date_end = timezone.make_aware(experiment['date_end'], timezone.utc)
            else:
                date_end = None

            data_list = [d for d in (date_start, date_end) if d]
            result = self.field.compress(data_list)

            msg = 'Exp: %s' % experiment
            if len(data_list) == 2:
                self.assertEqual(result['date_start'], date_start, msg=msg)
                self.assertEqual(result['date_end'], date_end, msg=msg)
            else:
                self.assertEqual(result, None, msg=msg)
