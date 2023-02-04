# -*- coding: utf-8 -*-
from django.test import TestCase
from django.core.exceptions import ValidationError

from events.surveyme.models import SurveyText, Survey


class TestSurveyText_get_default_texts_for_survey_type(TestCase):
    fixtures = ['initial_data.json']

    def test_for_other_types(self):
        expected = {
            'redirect_button': {
                'value': {
                    'ru': '',
                    'en': '',
                },
                'max_length': 30
            },
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                    'en': 'Submit',
                    'tr': 'Gönder',
                    'uk': 'Надіслати',
                },
                'max_length': 30
            },
            'back_button': {
                'value': {
                    'ru': 'Назад',
                    'en': 'Back',
                },
                'max_length': 30
            },
            'next_button': {
                'value': {
                    'ru': 'Вперед',
                    'en': 'Next',
                },
                'max_length': 30
            },
            'save_changes_button': {
                'value': {
                    'ru': 'Сохранить изменения',
                    'en': 'Save changes',
                    'tr': 'Değişiklikleri kaydet',
                    'uk': 'Зберегти зміни',
                },
                'max_length': 30
            },
            'invitation_to_submit': {
                'value': {
                    'ru': 'Ответить на опрос',
                    'en': 'Answer survey',
                    'tr': 'Ankete katıl',
                    'uk': 'Відповісти на опитування',
                },
                'max_length': 30
            },
            'invitation_to_change': {
                'value': {
                    'ru': 'Изменить ответ на опрос',
                    'en': 'Change survey answer',
                    'tr': 'Yanıtı değiştir',
                    'uk': 'Змінити відповідь на опитування',
                },
                'max_length': 30
            },
            'successful_submission_title': {
                'value': {
                    'ru': 'Спасибо!',
                    'en': 'Thanks for the response to a survey',
                    'tr': 'Ankete katıldığınız için teşekkür ederiz',
                    'uk': 'Дякуємо за відповідь на опитування',
                },
                'max_length': 90
            },
            'successful_submission': {
                'value': {
                    'ru': 'Ваше сообщение отправлено.',
                },
                'max_length': 750
            },
            'successful_change_title': {
                'value': {
                    'ru': 'Ваш ответ на опрос был изменен',
                    'en': 'Your response to the survey was changed',
                    'tr': 'Yanıtınız değiştirildi',
                    'uk': 'Вашу відповідь на опитування було змінено',
                },
                'max_length': 90
            },
            'successful_change': {
                'null': True,
                'max_length': 750
            }
        }

        types = [None, 'simple_form', 'yet_another_type']
        for type in types:
            response = SurveyText.get_default_texts_for_survey_type(type)
            self.assertEqual(response, expected)


class TestSurveyText_clean_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        survey = Survey.objects.create()
        self.survey_text = SurveyText.objects.create(max_length=4, slug='s', survey=survey)

    def test_validate_for_max_length(self):
        raised = False
        error_message = ''
        try:
            self.survey_text.null = False
            self.survey_text.value = 'test value'
            self.survey_text.clean_fields()
        except ValidationError as e:
            raised = True
            error_message = e.message_dict

        msg = 'При превышении максимальной длины должно вызываться исключение ValidationError'
        self.assertTrue(raised, msg=msg)

        expected_error_message = {'value': ['Максимальная длина сообщения не должна превышать 4 символов.']}
        msg = 'Неверное сообщение об ошибке'
        self.assertEqual(error_message, expected_error_message, msg=msg)

    def test_validate_for_null(self):
        for value in ['', '   ', None]:
            raised = False
            error_message = ''

            self.survey_text.null = True
            self.survey_text.value = value
            self.survey_text.clean_fields()

            try:
                self.survey_text.null = False
                self.survey_text.value = value
                self.survey_text.clean_fields()
            except ValidationError as e:
                raised = True
                error_message = e.message_dict

            msg = (
                'Если SurveyText.null == True,'
                'то при попытке сохранения должно вызываться исключение ValidationError'
            )
            self.assertTrue(raised, msg=msg)

            expected_error_message = {'value': ['Это поле не может быть пустым.']}
            msg = 'Неверное сообщение об ошибке'
            self.assertEqual(error_message, expected_error_message, msg=msg)
