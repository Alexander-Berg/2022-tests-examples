# -*- coding: utf-8 -*-
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase
from unittest.mock import patch, ANY

from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import Survey
from events.tanker import tasks, utils
from events.tanker.factories import TankerKeysetFactory


class TestTankerViews(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def test_run_translation_process(self):
        survey = SurveyFactory()
        languages = ['ru', 'en', 'de']
        data = {'languages': languages}
        with patch('events.tanker.utils.run_translation_process') as mock_process:
            response = self.client.post(
                f'/admin/api/v2/surveys/{survey.pk}/run-translation-process/',
                data=data, format='json',
            )

        self.assertEqual(response.status_code, 202)
        self.assertIsNotNone(response.data['task_id'])

        mock_process.assert_called_once_with(ANY, survey.pk, languages)

    def test_check_translations(self):
        ct = ContentType.objects.get_for_model(Survey)
        survey = SurveyFactory()
        keyset = f'surveyme.survey.{survey.pk}'
        tk = TankerKeysetFactory(name=keyset, content_type=ct, object_id=survey.pk)
        with patch('events.tanker.utils.update_translations') as mock_update:
            response = self.client.post(f'/admin/api/v2/surveys/{survey.pk}/check-translations/')

        self.assertEqual(response.status_code, 202)
        self.assertIsNotNone(response.data['task_id'])

        mock_update.assert_called_once_with(ANY, ANY)
        _, tk = mock_update.call_args_list[0][0]
        self.assertEqual(tk.name, keyset)
        self.assertEqual(tk.content_type, ct)
        self.assertEqual(tk.object_id, survey.pk)

    def test_translations(self):
        survey = SurveyFactory()
        status = {
            'ru': utils.STATUSES['APPROVED'],
            'en': utils.STATUSES['EXPIRED'],
            'de': utils.STATUSES['REQUIRES_TRANSLATION'],
        }
        with patch('events.tanker.utils.get_translation_status') as mock_status:
            mock_status.return_value = status
            response = self.client.get(f'/admin/api/v2/surveys/{survey.pk}/translations/')

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data, status)


class TestTankerTasks(TestCase):
    def test_update_all_translations(self):
        ct = ContentType.objects.get_for_model(Survey)
        surveys = [
            SurveyFactory(),
            SurveyFactory(),
        ]
        tks = [
            TankerKeysetFactory(name=f'surveyme.survey.{survey.pk}', content_type=ct, object_id=survey.pk)
            for survey in surveys
        ]
        with (
            patch('events.tanker.utils.get_tankerkeysets_for_update') as mock_keysets,
            patch('events.tanker.utils.update_translations') as mock_update,
        ):
            mock_keysets.return_value = tks
            tasks.update_all_translations.delay()

        mock_keysets.assert_called_once_with(ANY)
        self.assertEqual(mock_update.call_count, len(tks))
        for i, cal in enumerate(mock_update.call_args_list):
            _, tk = cal[0]
            self.assertEqual(tk, tks[i])
