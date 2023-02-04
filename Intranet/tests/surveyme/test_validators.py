# -*- coding: utf-8 -*-
from django.core.exceptions import ValidationError
from django.test import TestCase
from django.test.utils import override_settings
from django.utils.encoding import force_str
from django.utils.translation import override

from events.accounts.helpers import YandexClient
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import (
    AnswerType,
    ValidatorType,
)
from events.surveyme.validators import (
    DecimalValidator,
    INNValidator,
    RussianSymbolsValidator,
    RegexpValidator,
)


class TestDecimalValidator(TestCase):
    def setUp(self):
        self.decimal_re = DecimalValidator.regex
        self.valid_source_data = '''
            123 123.0 12.3 1.23 0.123 123,0 12,3 1,23 0,123
            -123 -123.0 -12.3 -1.23 -0.123 -0,123 -123,0 -12,3 -1,23 -0,123 -0,123
            +123 +123.0 +12.3 +1.23 +0.123 +0.123 +123,0 +12,3 +1,23 +0,123 +0,123
        '''.split()
        self.invalid_source_data = [
            '  123  ', '  12.34', '0.123  ', '  12,34', '0,123  ',
            '  -123  ', '  -12.34', '-0.123  ', '  -12,34', '-0,123  ',
            '  +123  ', '  +12.34', '+0.123  ', '  +12,34', '+0,123  ',
            '', ' \n\t', 'not a number', 'не число',
        ] + '''
            123. .123 123, ,123
            -123. -.123 -123, -,123
            +123. +.123 +123, +,123
            0123 0123. 0123.0 0123.123
            -0123 -0123. -0123.0 -0123.123
            +0123 +0123. +0123.0 +0123.123
        '''.split()

    def test_regular_expression_should_match(self):
        for value in self.valid_source_data:
            self.assertTrue(self.decimal_re.match(value), 'Regular expression not matched value "%s"' % value)

    def test_validator_should_pass(self):
        validator = DecimalValidator()
        for value in self.valid_source_data:
            try:
                validator(value)
            except ValidationError as e:
                self.fail(str(e))

    def test_regular_expression_shouldnt_match(self):
        for value in self.invalid_source_data:
            self.assertFalse(self.decimal_re.match(value), 'Regular expression matched value "%s"' % value)

    def test_validator_shouldnt_pass(self):
        validator = DecimalValidator()
        for value in self.invalid_source_data:
            with self.assertRaises(ValidationError):
                validator(value)

    @override(language='en')
    def test_error_message_translation_en(self):
        validator = DecimalValidator()
        with self.assertRaises(ValidationError) as e:
            validator('not a number')
        self.assertIn('Enter a valid decimal number', force_str(e.exception.message))

    @override(language='ru')
    def test_error_message_translation_ru(self):
        validator = DecimalValidator()
        with self.assertRaises(ValidationError) as e:
            validator('не число')
        self.assertIn('Введите корректное десятичное число', force_str(e.exception.message))


class TestSubmitForm(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.inn_validator = ValidatorType.objects.get(slug='inn')

        decimal_validator = ValidatorType.objects.get(slug='decimal')
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            label='First',
            answer_type=answer_short_text,
            validator_type=decimal_validator,
        )
        self.url = '/v1/surveys/{survey}/form/'.format(survey=self.survey.pk)

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_submit_form_decimal_error(self):
        data = {
            self.question.param_slug: 'not a number',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(400, response.status_code)

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_submit_form_decimal_success(self):
        data = {
            self.question.param_slug: '3.145',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_submit_form_inn_success(self):
        self.question.validator_type = self.inn_validator
        self.question.save()
        data = {
            self.question.param_slug: '123456789047',
        }

        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_submit_form_inn_error_not_a_number(self):
        self.question.validator_type = self.inn_validator
        self.question.save()
        data = {
            self.question.param_slug: 'not a number',
        }

        response = self.client.post(self.url, data)
        self.assertEqual(400, response.status_code)

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_submit_form_inn_error_incorrect_number(self):
        self.question.validator_type = self.inn_validator
        self.question.save()
        data = {
            self.question.param_slug: '1234567895',
        }

        response = self.client.post(self.url, data)
        self.assertEqual(400, response.status_code)

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_submit_with_regexp_validator(self):
        self.question.validator_type = ValidatorType.objects.get(slug='regexp')
        # условие: должен начинаться со слова - 'Hello' или 'hello'
        self.question.validator_options = {
            'regexp': '^[hH]ello',
        }
        self.question.save()

        data = {
            self.question.param_slug: 'Hello World'
        }

        response = self.client.post(self.url, data)
        assert response.status_code == 200

        data[self.question.param_slug] = 'hello World'
        response = self.client.post(self.url, data)
        assert response.status_code == 200

        data[self.question.param_slug] = 'hellO World'
        response = self.client.post(self.url, data)
        assert response.status_code == 400

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_patch_with_reqexp_validator(self):
        self.client.login_yandex(is_superuser=True)

        regexp_validator = ValidatorType.objects.get(slug='regexp')
        data = {
            'validator_type_id': regexp_validator.pk,
            'validator_options': {
                'regexp': '^[hH]ello',
            },
        }

        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        assert response.status_code == 200

        self.question.refresh_from_db()
        assert self.question.validator_type.pk == regexp_validator.pk
        assert self.question.validator_options == data['validator_options']

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_patch_reqexp_validator_with_no_expression(self):
        self.client.login_yandex(is_superuser=True)

        regexp_validator = ValidatorType.objects.get(slug='regexp')
        data = {
            'validator_type_id': regexp_validator.pk,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        assert response.status_code == 400

    @override_settings(YLOG_TRACKING=0, TOOLS_LOG_CONTEXT_ENABLE_TRACKING=False)
    def test_patch_reqexp_validator_with_invalid_options(self):
        self.client.login_yandex(is_superuser=True)

        regexp_validator = ValidatorType.objects.get(slug='regexp')
        data = {
            'validator_type_id': regexp_validator.pk,
            'validator_options': {
                'wrong_option': '^[hH]ello',
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        assert response.status_code == 400

        data['validator_options']['regexp'] = '^[hH]ello'
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        assert response.status_code == 200


class TestINNValidator(TestCase):
    def setUp(self):
        self.validator = INNValidator()

    def test_should_pass(self):
        correct_values = (
            '123456789047',
            '1234567894',
            '1234567894',
            '0452507759',
            '777242472182',
        )
        for value in correct_values:
            self.assertIsNone(self.validator(value))

    def test_should_fail(self):
        incorrect_values = (
            '123456789037',
            '1234567895',
            '777242472181',
            'hello',
            ['test', 'me'],
            123124142,
        )
        for value in incorrect_values:
            with self.assertRaises(ValidationError):
                self.validator(value)


class TestRussianSymbolsValidator(TestCase):
    def setUp(self):
        self.validator = RussianSymbolsValidator()

    def test_should_pass(self):
        correct_values = (
            'СЪЕШЬЕЩЁЭТИХМЯГКИХФРАНЦУЗСКИХБУЛОКДАВЫПЕЙЧАЮ',
            'съешьещёэтихмягкихфранцузскихбулокдавыпейчаю',
            '0123456789',
            '.,()- ',
        )
        for value in correct_values:
            self.assertIsNone(self.validator(value))

    def test_shouldnt_pass(self):
        incorrect_values = (
            'THEFIVEBOXINGWIZARDSJUMPQUICKLY',
            'thefiveboxingwizardsjumpquickly',
            '\n\t[]{}',
            'ЀЂЃЄЅІЇЈЉЊЋЌЍЎЏѐђѓєѕіїјљњћќѝўџѠѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿҀҁ',
            'ҋҌҍҎҏҐґҒғҔҕҖҗҘҙҚқҜҝҞҟҠҡҢңҤҥҦҧҨҩҪҫҬҭҮүҰұҲҳҴҵҶҷҸҹҺһҼҽҾҿӀӁӂӃӄӅӆӇӈӉӊ',
            'ӋӌӍӎӏӐӑӒӓӔӕӖӗӘәӚӛӜӝӞӟӠӡӢӣӤӥӦӧӨөӪӫӬӭӮӯӰӱӲӳӴӵӶӷӸӹӺӻӼӽӾ',
        )
        for value in incorrect_values:
            with self.assertRaises(ValidationError):
                self.validator(value)


class TestRegexpValidator(TestCase):

    def setUp(self):
        pass

    def test_only_digits(self):
        options = {
            'regexp': '^[0-9]+$',
        }
        check = RegexpValidator(**options)

        bad = ['12345a', 'a123', 'qbc', 'z123b']
        for value in bad:
            with self.assertRaises(ValidationError):
                check(value)

        good = ['1', '123', '987654321']
        for value in good:
            check(value)


class TestExternalValidator(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.external_validator = ValidatorType.objects.get(slug='external')

    def test_create_question_with_external_validator(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_short_text.pk,
            'label': 'Text',
            'validator_type_id': self.external_validator.pk,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

    def test_modify_question_with_exernal_validator(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_short_text,
        )
        data = {
            'id': question.pk,
            'survey_id': self.survey.pk,
            'validator_type_id': self.external_validator.pk,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
