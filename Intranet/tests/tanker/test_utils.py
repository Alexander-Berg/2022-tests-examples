# -*- coding: utf-8 -*-
from datetime import datetime
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase
from django.utils import timezone
from unittest.mock import patch, ANY

from events.surveyme.models import AnswerType, Survey
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
)
from events.tanker import utils
from events.tanker.factories import TankerKeysetFactory
from events.tanker.models import TankerKeyset
from events.tanker.client import TankerClient


class TestTankerUtils(TestCase):
    fixtures = ['initial_data.json']

    def test__get_actual_keys(self):
        survey = SurveyFactory(name='survey1')
        questions = [
            SurveyQuestionFactory(
                survey=survey, label='question1', param_help_text='help1',
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
            ),
            SurveyQuestionFactory(
                survey=survey, label='question2', param_help_text='help2',
                answer_type=AnswerType.objects.get(slug='answer_choices'),
            ),
            SurveyQuestionFactory(
                survey=survey, label='question3', param_help_text='help3',
                answer_type=AnswerType.objects.get(slug='answer_choices'),
            ),
        ]
        choices = [
            SurveyQuestionChoiceFactory(survey_question=questions[1], label='choice1'),
            SurveyQuestionChoiceFactory(survey_question=questions[1], label='choice2'),
        ]
        titles = [
            SurveyQuestionMatrixTitleFactory(survey_question=questions[2], label='row1', type='row'),
            SurveyQuestionMatrixTitleFactory(survey_question=questions[2], label='col1', type='column'),
            SurveyQuestionMatrixTitleFactory(survey_question=questions[2], label='col2', type='column'),
        ]
        # 8 surveytext rows
        actual_keys = utils._get_actual_keys(survey.pk)
        self.assertEqual(len(actual_keys), 21)
        expected = {
            f'surveyme.survey.{survey.pk}.name': survey.name,

            f'surveyme.surveyquestion.{questions[0].pk}.label': questions[0].label,
            f'surveyme.surveyquestion.{questions[0].pk}.param_help_text': questions[0].param_help_text,
            f'surveyme.surveyquestion.{questions[1].pk}.label': questions[1].label,
            f'surveyme.surveyquestion.{questions[1].pk}.param_help_text': questions[1].param_help_text,
            f'surveyme.surveyquestion.{questions[2].pk}.label': questions[2].label,
            f'surveyme.surveyquestion.{questions[2].pk}.param_help_text': questions[2].param_help_text,

            f'surveyme.surveyquestionchoice.{choices[0].pk}.label': choices[0].label,
            f'surveyme.surveyquestionchoice.{choices[1].pk}.label': choices[1].label,

            f'surveyme.surveyquestionmatrixtitle.{titles[0].pk}.label': titles[0].label,
            f'surveyme.surveyquestionmatrixtitle.{titles[1].pk}.label': titles[1].label,
            f'surveyme.surveyquestionmatrixtitle.{titles[2].pk}.label': titles[2].label,
        }
        expected.update({
            f'surveyme.surveytext.{text.pk}.value': text.value
            for text in survey.texts.exclude(value='')
        })
        self.assertEqual(actual_keys, expected)

    def test_create_or_modify_keyset__create_keyset(self):
        client = TankerClient('test')
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        with (
            patch.object(TankerClient, 'get_keyset') as mock_get,
            patch.object(TankerClient, 'create_keyset') as mock_create,
            patch.object(TankerClient, 'change_keyset') as mock_change,
        ):
            mock_get.return_value = None
            utils.create_or_modify_keyset(client, keyset, languages=languages)

        mock_get.assert_called_once_with(keyset)
        mock_create.assert_called_once_with(keyset, languages=languages)
        mock_change.assert_not_called()

    def test_create_or_modify_keyset__add_new_language(self):
        client = TankerClient('test')
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        with (
            patch.object(TankerClient, 'get_keyset') as mock_get,
            patch.object(TankerClient, 'create_keyset') as mock_create,
            patch.object(TankerClient, 'change_keyset') as mock_change,
        ):
            mock_get.return_value = {
                'status': 'ACTIVE',
                'meta': {
                    'languages': ['ru', 'en'],
                },
            }
            utils.create_or_modify_keyset(client, keyset, languages=languages)

        mock_get.assert_called_once_with(keyset)
        mock_create.assert_not_called()
        mock_change.assert_called_once_with(keyset, languages=languages)

    def test_create_or_modify_keyset__make_active(self):
        client = TankerClient('test')
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        with (
            patch.object(TankerClient, 'get_keyset') as mock_get,
            patch.object(TankerClient, 'create_keyset') as mock_create,
            patch.object(TankerClient, 'change_keyset') as mock_change,
        ):
            mock_get.return_value = {
                'status': 'ARCHIVED',
                'meta': {
                    'languages': ['ru', 'en', 'de'],
                },
            }
            utils.create_or_modify_keyset(client, keyset, languages=languages)

        mock_get.assert_called_once_with(keyset)
        mock_create.assert_not_called()
        mock_change.assert_called_once_with(keyset, languages=languages)

    def test_create_or_modify_keyset__do_nothing(self):
        client = TankerClient('test')
        keyset = 'testit'
        languages = ['ru', 'en']
        with (
            patch.object(TankerClient, 'get_keyset') as mock_get,
            patch.object(TankerClient, 'create_keyset') as mock_create,
            patch.object(TankerClient, 'change_keyset') as mock_change,
        ):
            mock_get.return_value = {
                'status': 'ACTIVE',
                'meta': {
                    'languages': ['ru', 'en'],
                },
            }
            utils.create_or_modify_keyset(client, keyset, languages=languages)

        mock_get.assert_called_once_with(keyset)
        mock_create.assert_not_called()
        mock_change.assert_not_called()

    def test__get_localized_value(self):
        key = {
            'translations': {
                'ru': {
                    'payload': {
                        'singular_form': 'text ru',
                    },
                },
                'pt-BR': {
                    'payload': {
                        'singular_form': 'text pt-br',
                    },
                },
            },
        }
        self.assertEqual(utils._get_localized_value(key, 'ru'), 'text ru')
        self.assertEqual(utils._get_localized_value(key, 'pt-br'), 'text pt-br')
        self.assertEqual(utils._get_localized_value(key, 'en'), '')

    def test_create_or_modify_keys__create_keys(self):
        client = TankerClient('test')
        keyset = 'testit'
        actual_keys = {
            'key1': 'value1',
            'key2': 'value2',
        }
        with (
            patch.object(TankerClient, 'get_keys') as mock_get,
            patch.object(TankerClient, 'change_keys') as mock_change,
        ):
            mock_get.return_value = []
            utils.create_or_modify_keys(client, keyset, actual_keys)

        mock_change.assert_called_once_with(
            keyset, create_keys=actual_keys,
            update_keys={}, delete_keys=[], original_language='ru',
        )

    def test_create_or_modify_keys__update_keys(self):
        client = TankerClient('test')
        keyset = 'testit'
        create_keys = {'key1': 'value1'}
        update_keys = {'key2': 'value2'}
        actual_keys = {}
        actual_keys.update(create_keys)
        actual_keys.update(update_keys)
        with (
            patch.object(TankerClient, 'get_keys') as mock_get,
            patch.object(TankerClient, 'change_keys') as mock_change,
        ):
            mock_get.return_value = [{
                'name': 'key2',
                'translations': {
                    'ru': {
                        'payload': {'singular_form': 'oldvalue'},
                    },
                },
            }]
            utils.create_or_modify_keys(client, keyset, actual_keys)

        mock_change.assert_called_once_with(
            keyset, create_keys=create_keys, update_keys=update_keys,
            delete_keys=[], original_language='ru',
        )

    def test_create_or_modify_keys__delete_keys(self):
        client = TankerClient('test')
        keyset = 'testit'
        create_keys = {'key1': 'value1'}
        update_keys = {'key2': 'value2'}
        delete_keys = ['key3']
        actual_keys = {}
        actual_keys.update(create_keys)
        actual_keys.update(update_keys)
        with (
            patch.object(TankerClient, 'get_keys') as mock_get,
            patch.object(TankerClient, 'change_keys') as mock_change,
        ):
            mock_get.return_value = [{
                'name': 'key2',
                'translations': {
                    'ru': {
                        'payload': {'singular_form': 'value2'},
                    },
                },
            }, {
                'name': 'key3',
                'translations': {
                    'ru': {
                        'payload': {'singular_form': 'value3'},
                    },
                },
            }]
            utils.create_or_modify_keys(client, keyset, actual_keys)

        mock_change.assert_called_once_with(
            keyset, create_keys=create_keys, update_keys={},
            delete_keys=delete_keys, original_language='ru',
        )

    def test_create_or_modify_keys__do_nothing(self):
        client = TankerClient('test')
        keyset = 'testit'
        update_keys = {'key2': 'value2'}
        with (
            patch.object(TankerClient, 'get_keys') as mock_get,
            patch.object(TankerClient, 'change_keys') as mock_change,
        ):
            mock_get.return_value = [{
                'name': 'key2',
                'translations': {
                    'ru': {
                        'payload': {'singular_form': 'value2'},
                    },
                },
            }]
            utils.create_or_modify_keys(client, keyset, update_keys)

        mock_change.assert_not_called()

    def test__get_worse_status(self):
        self.assertEqual(utils._get_worse_status('', utils.APPROVED), utils.APPROVED)
        self.assertEqual(utils._get_worse_status('', utils.TRANSLATED), utils.TRANSLATED)
        self.assertEqual(utils._get_worse_status('', utils.EXPIRED), utils.EXPIRED)
        self.assertEqual(utils._get_worse_status('', utils.REQUIRES_TRANSLATION),
                         utils.REQUIRES_TRANSLATION)

        self.assertEqual(utils._get_worse_status(utils.APPROVED, utils.TRANSLATED),
                         utils.TRANSLATED)
        self.assertEqual(utils._get_worse_status(utils.TRANSLATED, utils.EXPIRED),
                         utils.EXPIRED)
        self.assertEqual(utils._get_worse_status(utils.EXPIRED, utils.REQUIRES_TRANSLATION),
                         utils.REQUIRES_TRANSLATION)
        self.assertEqual(utils._get_worse_status(utils.REQUIRES_TRANSLATION, '???'),
                         utils.REQUIRES_TRANSLATION)

        self.assertEqual(utils._get_worse_status(utils.TRANSLATED, utils.APPROVED),
                         utils.TRANSLATED)
        self.assertEqual(utils._get_worse_status(utils.EXPIRED, utils.TRANSLATED),
                         utils.EXPIRED)
        self.assertEqual(utils._get_worse_status(utils.REQUIRES_TRANSLATION, utils.EXPIRED),
                         utils.REQUIRES_TRANSLATION)

        self.assertEqual(utils._get_worse_status('???', utils.APPROVED), utils.APPROVED)
        self.assertEqual(utils._get_worse_status('???', utils.TRANSLATED), utils.TRANSLATED)
        self.assertEqual(utils._get_worse_status('???', utils.EXPIRED), utils.EXPIRED)
        self.assertEqual(utils._get_worse_status('???', utils.REQUIRES_TRANSLATION),
                         utils.REQUIRES_TRANSLATION)

    def test_get_keyset_status__success(self):
        client = TankerClient('test')
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        with patch.object(TankerClient, 'get_keys') as mock_get:
            mock_get.return_value = [{
                'name': 'key1',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value1'}},
                    'en': {'status': 'TRANSLATED', 'payload': {'singular_form': 'value1'}},
                    'de': {'status': 'APPROVED', 'payload': {'singular_form': 'value1'}},
                },
            }, {
                'name': 'key2',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value2'}},
                    'en': {'status': 'EXPIRED', 'payload': {'singular_form': 'value2'}},
                },
            }, {
                'name': 'key3',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value3'}},
                    'en': {'status': 'APPROVED', 'payload': {'singular_form': 'value3'}},
                },
            }]
            result = utils.get_keyset_status(client, keyset, languages=languages, keys_data=None)
        expected = {'ru': 'APPROVED', 'en': 'EXPIRED', 'de': 'REQUIRES_TRANSLATION'}
        self.assertEqual(result, expected)

    def test_get_keyset_status__empty(self):
        client = TankerClient('test')
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        with patch.object(TankerClient, 'get_keys') as mock_get:
            mock_get.return_value = []
            result = utils.get_keyset_status(client, keyset, languages=languages, keys_data=None)
        expected = {}
        self.assertEqual(result, expected)

    def test_get_translations__success(self):
        client = TankerClient('test')
        keyset = 'testit'
        with patch.object(TankerClient, 'get_keys') as mock_get:
            mock_get.return_value = [{
                'name': 'surveyme.survey.123.key1',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value1 ru'}},
                    'en': {'status': 'TRANSLATED', 'payload': {'singular_form': 'value1 en'}},
                    'de': {'status': 'APPROVED', 'payload': {'singular_form': 'value1 de'}},
                },
            }, {
                'name': 'surveyme.survey.123.key2',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value2 ru'}},
                    'en': {'status': 'EXPIRED', 'payload': {'singular_form': 'value2 en'}},
                },
            }, {
                'name': 'key3',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value3 ru'}},
                    'en': {'status': 'APPROVED', 'payload': {'singular_form': 'value3 en'}},
                },
            }]
            result = utils.get_translations(client, keyset, status=None, keys_data=None)
        expected = {
            ('surveyme', 'survey', '123'): {
                'key1': {'ru': 'value1 ru', 'en': 'value1 en', 'de': 'value1 de'},
                'key2': {'ru': 'value2 ru', 'en': 'value2 en'},
            }
        }
        self.assertEqual(result, expected)

    def test_get_translations__approved_only(self):
        client = TankerClient('test')
        keyset = 'testit'
        with patch.object(TankerClient, 'get_keys') as mock_get:
            mock_get.return_value = [{
                'name': 'surveyme.survey.123.key1',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value1 ru'}},
                    'en': {'status': 'TRANSLATED', 'payload': {'singular_form': 'value1 en'}},
                    'De': {'status': 'APPROVED', 'payload': {'singular_form': 'value1 de'}},
                },
            }, {
                'name': 'surveyme.survey.123.key2',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value2 ru'}},
                    'en': {'status': 'EXPIRED', 'payload': {'singular_form': 'value2 en'}},
                },
            }, {
                'name': 'key3',
                'translations': {
                    'ru': {'status': 'APPROVED', 'payload': {'singular_form': 'value3 ru'}},
                    'en': {'status': 'APPROVED', 'payload': {'singular_form': 'value3 en'}},
                },
            }]
            result = utils.get_translations(client, keyset, status=utils.APPROVED, keys_data=None)
        expected = {
            ('surveyme', 'survey', '123'): {
                'key1': {'ru': 'value1 ru', 'de': 'value1 de'},
                'key2': {'ru': 'value2 ru'},
            }
        }
        self.assertEqual(result, expected)

    def test__get_datetime(self):
        result = utils._get_datetime('2021-11-12T08:53:33.201254')
        expected = timezone.make_aware(datetime(2021, 11, 12, 8, 53, 33, 201254), timezone.utc)
        self.assertEqual(result, expected)

    def test_run_translation_process(self):
        client = TankerClient('test')
        survey = SurveyFactory(name='name1')
        survey.texts.all().delete()
        keyset = f'surveyme.survey.{survey.pk}'
        languages = ['ru', 'en', 'de']
        status = {
            'ru': 'APPROVED', 'en': 'REQUIRES_TRANSLATION', 'de': 'REQUIRES_TRANSLATION',
        }
        with (
            patch('events.tanker.utils.create_or_modify_keyset') as mock_keyset,
            patch('events.tanker.utils.create_or_modify_keys') as mock_keys,
            patch('events.tanker.utils.get_keyset_status') as mock_status,
        ):
            mock_status.return_value = status
            utils.run_translation_process(client, survey.pk, languages=languages)

        mock_keyset.assert_called_once_with(client, keyset, languages)
        actual_keys = {
            f'surveyme.survey.{survey.pk}.name': survey.name,
        }
        mock_keys.assert_called_once_with(client, keyset, actual_keys)
        mock_status.assert_called_once_with(client, keyset, languages=languages)

        tk = TankerKeyset.objects.get(name=keyset)
        self.assertEqual(tk.object_id, survey.pk)
        self.assertEqual(tk.keys, list(actual_keys))
        self.assertEqual(tk.languages, status)
        self.assertIsNone(tk.last_updated_at)

    def test_get_tankerkeysets_for_update(self):
        client = TankerClient('test')
        ct = ContentType.objects.get_for_model(Survey)
        tks = [
            TankerKeysetFactory(name='key1', content_type=ct),
            TankerKeysetFactory(name='key2', content_type=ct, last_updated_at='2021-11-01T11:33:00Z'),
            TankerKeysetFactory(name='key3', content_type=ct, last_updated_at='2021-11-15T11:33:00Z'),
        ]

        with patch.object(TankerClient, 'get_keysets') as mock_keysets:
            mock_keysets.return_value = [
                {'name': 'key1', 'last_commit_ts': '2021-11-10T10:33:00.123456'},
                {'name': 'key2', 'last_commit_ts': '2021-11-10T10:33:00.123456'},
                {'name': 'key3', 'last_commit_ts': '2021-11-10T10:33:00.123456'},
            ]
            result = set(utils.get_tankerkeysets_for_update(client))
        expected = {tks[0], tks[1]}
        self.assertEqual(result, expected)

    def test_update_translations(self):
        client = TankerClient('test')
        ct = ContentType.objects.get_for_model(Survey)
        survey = SurveyFactory(name='name1')
        keyset = f'surveyme.survey.{survey.pk}'
        tk = TankerKeysetFactory(
            name=keyset, content_type=ct, object_id=survey.pk,
            languages={'ru': '', 'en': '', 'de': ''},
        )
        status = {
            'ru': 'APPROVED', 'en': 'REQUIRES_TRANSLATION', 'de': 'REQUIRES_TRANSLATION',
        }

        with (
            patch.object(TankerClient, 'get_keys') as mock_keys,
            patch('events.tanker.utils.get_keyset_status') as mock_status,
        ):
            mock_keys.return_value = [{
                'name': f'surveyme.survey.{survey.pk}.name',
                'translations': {
                    'ru': {'payload': {'singular_form': 'name1 ru'}},
                    'en': {'payload': {'singular_form': 'name1 en'}},
                    'de': {'payload': {'singular_form': 'name1 de'}},
                },
            }]
            mock_status.return_value = status
            utils.update_translations(client, tk)

        mock_keys.assert_called_once_with(keyset)
        mock_status.assert_called_once_with(client, keyset, languages=list(status), keys_data=ANY)

        survey.refresh_from_db()
        expected = {'name': {'ru': 'name1 ru', 'en': 'name1 en', 'de': 'name1 de'}}
        self.assertEqual(survey.translations, expected)

        tk.refresh_from_db()
        self.assertIsNotNone(tk.last_updated_at)
        self.assertEqual(tk.languages, status)

    def test_update_status(self):
        client = TankerClient('test')
        ct = ContentType.objects.get_for_model(Survey)
        survey = SurveyFactory(name='name1')
        keyset = f'surveyme.survey.{survey.pk}'
        tk = TankerKeysetFactory(
            name=keyset, content_type=ct, object_id=survey.pk,
            languages={'ru': '', 'en': '', 'de': ''},
        )
        status = {
            'ru': 'APPROVED', 'en': 'EXPIRED', 'de': 'REQUIRES_TRANSLATION',
        }

        with patch('events.tanker.utils.get_keyset_status') as mock_status:
            mock_status.return_value = status
            result = utils.update_status(client, tk)

        mock_status.assert_called_once_with(client, keyset, languages=list(status))
        self.assertEqual(result.languages, status)

    def test_get_translation_status(self):
        ct = ContentType.objects.get_for_model(Survey)
        survey = SurveyFactory(name='name1')
        keyset = f'surveyme.survey.{survey.pk}'
        TankerKeysetFactory(
            name=keyset, content_type=ct, object_id=survey.pk,
            languages={'ru': '', 'en': '', 'de': ''},
        )
        status = {
            'ru': 'APPROVED', 'en': 'EXPIRED', 'de': 'REQUIRES_TRANSLATION',
        }
        with patch('events.tanker.utils.get_keyset_status') as mock_status:
            mock_status.return_value = status
            result = utils.get_translation_status(survey.pk)

        mock_status.assert_called_once_with(ANY, keyset, languages=list(status))
        expected = {
            'ru': utils.STATUSES['APPROVED'],
            'en': utils.STATUSES['EXPIRED'],
            'de': utils.STATUSES['REQUIRES_TRANSLATION'],
        }
        self.assertEqual(result, expected)
