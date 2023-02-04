# -*- coding: utf-8 -*-
from django.conf import settings
from django.test import TestCase
from unittest.mock import patch

from events.media.factories import ImageFactory
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyGroupFactory,
    SurveyQuestionFactory,
    SurveyStyleTemplateFactory,
)
from events.surveyme.models import AnswerType
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    SurveyHookFactory,
    ServiceSurveyHookSubscriptionFactory,
)


class TestSurveySuccessView(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(
            name='New Form',
            metrika_counter_code='123456',
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'quiz': {
                    'title': 'Title',
                    'description': 'Description',
                    'scores': 10,
                    'total_scores': 100,
                    'image_path': None,
                },
                'data': [{
                    'question': self.question.get_answer_info(),
                    'value': '42',
                }],
            }
        )

    def test_should_return_not_found_for_deleted_survey(self):
        self.survey.is_deleted = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_not_found_for_not_published_survey(self):
        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_not_found_for_banned_survey(self):
        self.survey.is_ban_detected = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_minimal_valueable_response(self):
        self.survey.is_published_external = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['id'], self.survey.pk)
        self.assertEqual(response.data['name'], self.survey.name)
        self.assertEqual(response.data['metrika_counter_code'], self.survey.metrika_counter_code)
        self.assertIsNone(response.data['group'])
        self.assertIsNone(response.data['redirect'])
        self.assertIsNone(response.data['footer'])
        self.assertIsNone(response.data['styles_template'])
        self.assertIsNone(response.data['stats'])
        self.assertIsNone(response.data['answer'])
        self.assertIsNone(response.data['integrations'])

    def test_should_return_survey_group(self):
        self.survey.is_published_external = True
        self.survey.save()

        group = SurveyGroupFactory(metrika_counter_code='234567')
        self.survey.group = group
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertIsNotNone(response.data['group'])
        self.assertDictEqual(response.data['group'], {
            'id': group.pk,
            'metrika_counter_code': group.metrika_counter_code,
        })

    def test_should_return_texts(self):
        self.survey.is_published_external = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        texts = response.data['texts']
        self.assertTrue(len(texts) > 0)
        self.assertIn('submit_button', texts)
        self.assertIn('redirect_button', texts)
        self.assertIn('back_button', texts)
        self.assertIn('next_button', texts)
        self.assertIn('successful_submission_title', texts)
        self.assertIn('successful_submission', texts)

    def test_should_return_redirect(self):
        self.survey.is_published_external = True
        self.survey.extra = {
            'redirect': {
                'enabled': True,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)
        self.assertDictEqual(response.data['redirect'], self.survey.extra['redirect'])

    def test_should_return_footer(self):
        self.survey.is_published_external = True
        self.survey.extra = {
            'footer': {
                'enabled': True,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertDictEqual(response.data['footer'], self.survey.extra['footer'])

    def test_should_return_styles_template(self):
        self.survey.is_published_external = True
        styles_template = SurveyStyleTemplateFactory(
            styles={
                'bg_color': '#fff',
            },
        )
        self.survey.styles_template = styles_template
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        styles = response.data['styles_template']
        self.assertIsNotNone(styles)
        self.assertEqual(styles['id'], styles_template.pk)
        self.assertEqual(styles['name'], styles_template.name)
        self.assertEqual(styles['type'], styles_template.type)
        self.assertDictEqual(styles['styles'], styles_template.styles)

    def test_should_return_stats(self):
        self.survey.is_published_external = True
        self.survey.extra = {
            'stats': {
                'enabled': True,
            },
        }
        self.survey.save()

        with patch('events.countme.stats.get_stats_info') as mock_get_stats_info:
            mock_get_stats_info.return_value = {
                'answers': {
                    'count': 3,
                },
                'questions': [],
            }
            response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        stats = response.data['stats']
        self.assertIsNotNone(stats)
        self.assertDictEqual(stats, {
            'answers': {
                'count': 3,
            },
            'questions': [],
        })
        mock_get_stats_info.assert_called_once_with(self.survey)

    def test_shouldnt_return_stats(self):
        self.survey.is_published_external = True
        self.survey.extra = {
            'stats': {
                'enabled': False,
            },
        }
        self.survey.save()

        with patch('events.countme.stats.get_stats_info') as mock_get_stats_info:
            mock_get_stats_info.return_value = {}
            response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['stats'])
        mock_get_stats_info.assert_not_called()

    def test_should_return_scores(self):
        self.survey.extra = {
            'quiz': {
                'show_results': True,
            },
        }
        self.survey.save()

        image = ImageFactory()
        image_path = str(image.image)
        self.answer.data = {
            'quiz': {
                'title': 'Title',
                'description': 'Description',
                'scores': 10,
                'total_scores': 100,
                'image_path': image_path,
            },
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '42',
            }],
        }
        self.answer.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        scores = response.data['scores']
        self.assertIsNotNone(scores)
        self.assertDictEqual(scores, {
            'title': 'Title',
            'description': 'Description',
            'scores': 10,
            'total_scores': 100,
            'image_path': image_path,
            'image': {
                'links': {
                    size: f'{settings.AVATARS_HOST}get-{settings.IMAGE_NAMESPACE}/{image_path}/{size}'
                    for size in settings.IMAGE_SIZES_AS_STR
                },
            },
        })

    def test_should_return_scores_without_image(self):
        self.survey.extra = {
            'quiz': {
                'show_results': True,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        scores = response.data['scores']
        self.assertIsNotNone(scores)
        self.assertDictEqual(scores, {
            'title': 'Title',
            'description': 'Description',
            'scores': 10,
            'total_scores': 100,
            'image_path': None,
        })

    def test_shouldnt_return_scores(self):
        self.survey.extra = {
            'quiz': {
                'show_results': False,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['scores'])

    def test_shouldnt_return_scores_without_answer_key(self):
        self.survey.is_published_external = True
        self.survey.extra = {
            'quiz': {
                'show_results': True,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['scores'])

    def test_should_return_answer(self):
        self.survey.extra = {
            'quiz': {
                'show_correct': True,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        answer = response.data['answer']
        self.assertIsNotNone(answer)
        self.assertEqual(len(answer['data']), 1)
        question_info = self.question.get_answer_info()
        question_info.update({
            'is_deleted': False,
            'is_hidden': False,
            'label': self.question.label,
        })
        self.assertDictEqual(answer['data'][0], {
            'question': question_info,
            'value': '42',
        })

    def test_shouldnt_return_answer(self):
        self.survey.extra = {
            'quiz': {
                'show_correct': False,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['answer'])

    def test_shouldnt_return_answer_without_answer_key(self):
        self.survey.is_published_external = True
        self.survey.extra = {
            'quiz': {
                'show_correct': True,
            },
        }
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['answer'])

    def test_should_return_integrations(self):
        hook = SurveyHookFactory(survey=self.survey)
        subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=hook,
            service_type_action_id=3,  # email
            follow_result=True,
        )
        HookSubscriptionNotificationFactory(
            survey=self.survey,
            subscription=subscription,
            answer=self.answer,
            status='success',
            context={
                'to_address': 'user@company.com',
            },
        )

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        integrations = response.data['integrations']
        self.assertIsNotNone(integrations)
        self.assertIn(str(subscription.pk), integrations)
        self.assertDictEqual(integrations[str(subscription.pk)], {
            'status': 'success',
            'id': str(subscription.pk),
            'integration_type': 'email',
            'resources': {
                'to_address': 'user@company.com',
            },
        })

    def test_shouldnt_return_integrations(self):
        hook = SurveyHookFactory(survey=self.survey)
        subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=hook,
            service_type_action_id=3,  # email
            follow_result=False,
        )
        HookSubscriptionNotificationFactory(
            survey=self.survey,
            subscription=subscription,
            answer=self.answer,
            status='success',
            context={
                'to_address': 'user@company.com',
            },
        )

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/?answer_key={self.answer.secret_code}')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['integrations'])

    def test_shouldnt_return_integrations_without_answer_key(self):
        self.survey.is_published_external = True
        self.survey.save()

        hook = SurveyHookFactory(survey=self.survey)
        subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=hook,
            service_type_action_id=3,  # email
            follow_result=True,
        )
        HookSubscriptionNotificationFactory(
            survey=self.survey,
            subscription=subscription,
            answer=self.answer,
            status='success',
            context={
                'to_address': 'user@company.com',
            },
        )

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/success/')
        self.assertEqual(response.status_code, 200)

        self.assertIsNone(response.data['integrations'])
