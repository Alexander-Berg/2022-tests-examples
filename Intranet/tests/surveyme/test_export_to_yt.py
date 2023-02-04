# -*- coding: utf-8 -*-
import itertools
import os
import pytz

from datetime import date, datetime, timedelta
from django.conf import settings
from django.test import TestCase, override_settings
from django.utils import timezone
from freezegun import freeze_time
from unittest.mock import patch, call
from yt.wrapper import TablePath

from events.accounts.helpers import YandexClient
from events.common_app.utils import chunks
from events.surveyme.export_to_yt import (
    get_table_path,
    datetime_to_string,
    get_answer,
    change_status,
    convert_answer_data,
    deploy_answers,
    export_answers,
)
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    AnswerExportYtStatusFactory,
    ProfileSurveyAnswerFactory,
)
from events.surveyme.models import (
    ProfileSurveyAnswer,
    AnswerExportYtStatus,
    AnswerType,
    Survey,
)
from events.surveyme.tasks import (
    prepare_answers_to_export,
    remove_answers_from_export,
    remove_table_from_yt,
)
from events.surveyme.utils import (
    PrepareAnswersToExportCommand,
    RemoveAnswersFromExportCommand,
    RemoveTableFromYtCommand,
    SurveyCommand,
)


class MockYtClient:
    def __init__(self, exist_folders=None):
        self.exist_folders = exist_folders or []
        self.commands = []

    def exists(self, *args, **kwargs):
        self.commands.append(['exists', args, kwargs])
        folder_path, *rest = args
        return folder_path in self.exist_folders

    def mkdir(self, *args, **kwargs):
        self.commands.append(['mkdir', args, kwargs])

    def create(self, *args, **kwargs):
        self.commands.append(['create', args, kwargs])

    def write_table(self, *args, **kwargs):
        self.commands.append(['write_table', args, kwargs])


class TestExportUtils(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def test_should_return_currect_table_path_for_intranet(self):
        expected = '//home/forms/answers/%s/development/1/data' % settings.APP_TYPE
        self.assertEqual(get_table_path('1', timezone.now()), expected)

        expected = '//home/forms/answers/%s/development/123456789/data' % settings.APP_TYPE
        self.assertEqual(get_table_path('123456789', timezone.now()), expected)

    @freeze_time('2018-07-05 11:56:58Z')
    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_correct_table_path_for_business(self):
        expected = '//home/forms/answers/%s/development/2018-07-05' % settings.APP_TYPE
        self.assertEqual(get_table_path('1', timezone.now()), expected)
        self.assertEqual(get_table_path('12345', timezone.now()), expected)
        self.assertEqual(get_table_path('123456789', timezone.now()), expected)
        self.assertEqual(get_table_path('deafbeafcafe', timezone.now()), expected)

    def test_should_return_datetime_string_in_utc(self):
        dt = timezone.make_aware(datetime(2020, 5, 29, 18, 11), pytz.timezone('Europe/Moscow'))
        self.assertEqual(datetime_to_string(dt), '2020-05-29T15:11:00Z')

        dt = timezone.make_aware(datetime(2020, 5, 29, 18, 11), pytz.UTC)
        self.assertEqual(datetime_to_string(dt), '2020-05-29T18:11:00Z')

    @freeze_time('2018-07-05 11:56:58Z')
    def test_should_return_answer_as_dict(self):
        source_request = {
            'lang': 'ru',
            'ip': '127.0.0.1',
            'cookies': {
                'yandexuid': '123456',
            },
        }
        data = {
            'uid': '321456',
            'ip': '127.0.0.1',
            'yandexuid': '123456',
            'data': [
                {
                    'question': {
                        'slug': 'text',
                        'answer_type': {
                            'slug': 'answer_short_text',
                        },
                    },
                    'value': 'testme',
                },
                {
                    'question': {
                        'slug': 'group',
                        'answer_type': {
                            'slug': 'answer_group',
                        },
                    },
                    'value': [
                        [
                            {
                                'question': {
                                    'slug': 'text1',
                                },
                                'value': 'first',
                            },
                            {
                                'question': {
                                    'slug': 'text2',
                                },
                                'value': 'second',
                            },
                        ],
                        [
                            {
                                'question': {
                                    'slug': 'text1',
                                },
                                'value': 'third',
                            },
                            {
                                'question': {
                                    'slug': 'text2',
                                },
                                'value': 'fourth',
                            },
                        ],
                    ],
                },
            ],
        }
        row = 100500, 1001, timezone.now(), source_request, data
        answer = get_answer(row)
        self.assertEqual(answer.answer_id, 100500)
        self.assertEqual(answer.survey_id, '1001')
        self.assertEqual(answer.created, timezone.now())
        self.assertEqual(answer.uid, '321456')
        self.assertEqual(answer.yandexuid, '123456')
        self.assertEqual(answer.ip, '127.0.0.1')
        self.assertFalse('uid' in answer.answer_data)
        self.assertFalse('yandexuid' in answer.answer_data)
        self.assertFalse('ip' in answer.answer_data)
        question_data = answer.answer_data['data']
        self.assertTrue(isinstance(question_data, dict))
        self.assertEqual(len(question_data), 2)
        self.assertSetEqual(set(question_data.keys()), set(['text', 'group']))
        self.assertEqual(question_data['text']['value'], 'testme')
        self.assertTrue(isinstance(question_data['group']['value'], list))
        self.assertEqual(len(question_data['group']['value']), 2)
        self.assertTrue(isinstance(question_data['group']['value'][0], dict))
        self.assertEqual(len(question_data['group']['value'][0]), 2)
        self.assertSetEqual(set(question_data['group']['value'][0].keys()), set(['text1', 'text2']))
        self.assertEqual(question_data['group']['value'][0]['text1']['value'], 'first')
        self.assertEqual(question_data['group']['value'][0]['text2']['value'], 'second')
        self.assertTrue(isinstance(question_data['group']['value'][1], dict))
        self.assertEqual(len(question_data['group']['value'][1]), 2)
        self.assertSetEqual(set(question_data['group']['value'][1].keys()), set(['text1', 'text2']))
        self.assertEqual(question_data['group']['value'][1]['text1']['value'], 'third')
        self.assertEqual(question_data['group']['value'][1]['text2']['value'], 'fourth')

    def test_should_return_answer_without_uid(self):
        source_request = {
            'lang': 'ru',
        }
        data = {
            'data': [],
        }
        row = 100500, 1001, timezone.now(), source_request, data
        answer = get_answer(row)
        self.assertEqual(answer.answer_id, 100500)
        self.assertEqual(answer.survey_id, '1001')
        self.assertIsNone(answer.uid)
        self.assertEqual(answer.lang, 'ru')

    def test_should_change_answer_export_status(self):
        survey = SurveyFactory()
        data = {'data': []}
        answers = [
            get_answer(ProfileSurveyAnswerFactory(survey=survey, data=data))
            for i in range(9)
        ]
        for answer in answers:
            AnswerExportYtStatusFactory(pk=answer.answer_id, exported=False)

        with patch('events.surveyme.export_to_yt.DB_UPDATE_LIMIT', 2):
            with self.assertNumQueries(5):
                change_status(answers, True)

        answers_qs = ProfileSurveyAnswer.objects.filter(survey=survey, export_yt_status__exported=True)
        self.assertEqual(answers_qs.count(), 9)

    def test_should_invoke_yt_methods(self):
        survey = SurveyFactory()
        data = {'data': []}
        answers = [
            get_answer(ProfileSurveyAnswerFactory(survey=survey, data=data))
            for i in range(9)
        ]
        for answer in answers:
            AnswerExportYtStatusFactory(pk=answer.answer_id, exported=False)
        table_path = '//home/forms/newfolder/data'
        folder_path = os.path.dirname(table_path)

        with patch('events.surveyme.export_to_yt.DeployAnswers._get_client', return_value=MockYtClient()) as mock_get_client:
            deploy_answers(answers, table_path, 0)

        mock_get_client.assert_called_once_with()
        called_commands = mock_get_client.return_value.commands
        self.assertEqual(len(called_commands), 4)
        self.assertEqual(called_commands[0][0], 'exists')
        self.assertEqual(called_commands[0][1], (folder_path,))
        self.assertEqual(called_commands[1][0], 'mkdir')
        self.assertEqual(called_commands[1][1], (folder_path,))
        self.assertEqual(called_commands[2][0], 'create')
        self.assertEqual(called_commands[2][1], ('table', table_path))
        self.assertEqual(called_commands[3][0], 'write_table')
        self.assertEqual(called_commands[3][1][0], TablePath(table_path, append=True))
        self.assertEqual(len(called_commands[3][1][1]), 9)

    def test_should_invoke_deploy_answers_intranet(self):
        data = {'data': []}

        survey1 = SurveyFactory()
        answers1 = [
            ProfileSurveyAnswerFactory(survey=survey1, data=data)
            for i in range(9)
        ]
        for answer in answers1:
            AnswerExportYtStatusFactory(answer=answer, exported=False)

        survey2 = SurveyFactory()
        answers2 = [
            ProfileSurveyAnswerFactory(survey=survey2, data=data)
            for i in range(6)
        ]
        for answer in answers2:
            AnswerExportYtStatusFactory(answer=answer, exported=False)
        answers3 = [
            ProfileSurveyAnswerFactory(survey=survey2, data=data)
            for i in range(2)
        ]
        for i, answer in enumerate(answers3, 2):
            created = timezone.now() + timedelta(minutes=i)
            answer.date_created = created
            answer.save()

        with patch('events.surveyme.export_to_yt.DeployAnswers.write_data') as mock_write_data:
            with patch('events.surveyme.export_to_yt.YT_UPLOAD_LIMIT', 5):
                started_at = timezone.now() + timedelta(minutes=1)
                self.assertEqual(export_answers(False, True, 10, started_at), 10)
                self.assertEqual(mock_write_data.call_count, 3)
                args_list = mock_write_data.call_args_list

                answers, table_path = args_list[0][0]
                self.assertEqual(len(answers), 5)
                self.assertTrue(table_path.endswith('/%s/data' % survey1.pk))

                answers, table_path = args_list[1][0]
                self.assertEqual(len(answers), 4)
                self.assertTrue(table_path.endswith('/%s/data' % survey1.pk))

                answers, table_path = args_list[2][0]
                self.assertEqual(len(answers), 1)
                self.assertTrue(table_path.endswith('/%s/data' % survey2.pk))

                mock_write_data.reset_mock()
                self.assertEqual(export_answers(False, True, 10, started_at), 5)
                self.assertEqual(mock_write_data.call_count, 1)
                args_list = mock_write_data.call_args_list

                answers, table_path = args_list[0][0]
                self.assertEqual(len(answers), 5)
                self.assertTrue(table_path.endswith('/%s/data' % survey2.pk))

                mock_write_data.reset_mock()
                self.assertEqual(export_answers(False, True, 10, started_at), 0)
                self.assertEqual(mock_write_data.call_count, 0)

    @freeze_time('2018-07-05 11:56:58Z')
    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_invoke_deploy_answers_business(self):
        data = {'data': []}

        survey1 = SurveyFactory()
        answers1 = [
            ProfileSurveyAnswerFactory(survey=survey1, data=data)
            for i in range(9)
        ]
        for answer in answers1:
            AnswerExportYtStatusFactory(answer=answer, exported=False)

        survey2 = SurveyFactory()
        answers2 = [
            ProfileSurveyAnswerFactory(survey=survey2, data=data)
            for i in range(6)
        ]
        for answer in answers2:
            AnswerExportYtStatusFactory(answer=answer, exported=False)
        answers3 = [
            ProfileSurveyAnswerFactory(survey=survey2, data=data)
            for i in range(2)
        ]
        for answer in answers3:
            AnswerExportYtStatusFactory(answer=answer, exported=False)
        for i, chunk in enumerate(chunks(itertools.chain(answers1, answers2), 4)):
            created = timezone.now() + timedelta(days=i)
            for answer in chunk:
                answer.date_created = created
                answer.save()
        for i, answer in enumerate(answers3, 5):
            created = timezone.now() + timedelta(days=i)
            answer.date_created = created
            answer.save()

        with patch('events.surveyme.export_to_yt.DeployAnswers.write_data') as mock_write_data:
            with patch('events.surveyme.export_to_yt.YT_UPLOAD_LIMIT', 5):
                started_at = timezone.now() + timedelta(days=5)
                self.assertEqual(export_answers(False, True, 10, started_at), 10)
                args_list = mock_write_data.call_args_list
                self.assertEqual(mock_write_data.call_count, 3)

                answers, table_path = args_list[0][0]
                self.assertEqual(len(answers), 4)
                self.assertTrue(table_path.endswith('/2018-07-05'))

                answers, table_path = args_list[1][0]
                self.assertEqual(len(answers), 4)
                self.assertTrue(table_path.endswith('/2018-07-06'))

                answers, table_path = args_list[2][0]
                self.assertEqual(len(answers), 2)
                self.assertTrue(table_path.endswith('/2018-07-07'))

                mock_write_data.reset_mock()
                self.assertEqual(export_answers(False, True, 10, started_at), 5)
                self.assertEqual(mock_write_data.call_count, 2)
                args_list = mock_write_data.call_args_list

                answers, table_path = args_list[0][0]
                self.assertEqual(len(answers), 2)
                self.assertTrue(table_path.endswith('/2018-07-07'))

                answers, table_path = args_list[1][0]
                self.assertEqual(len(answers), 3)
                self.assertTrue(table_path.endswith('/2018-07-08'))
                mock_write_data.reset_mock()

                self.assertEqual(export_answers(False, True, 10, started_at), 0)
                self.assertEqual(mock_write_data.call_count, 0)

    def test_should_correct_set_export_flag_for_edited_answer(self):
        self.client.login_yandex()

        # создаем форму с ответами доступными для редактировани
        survey = SurveyFactory(
            is_published_external=True,
            is_allow_answer_editing=True,
            save_logs_for_statbox=True,
        )
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

        # первый ответ, базовое поведение, создается новая запись, статус эспорта False
        data = {
            question.param_slug: 'test 1',
        }
        response = self.client.post(f'/v1/surveys/{survey.pk}/form/', data=data)
        self.assertEqual(response.status_code, 200)
        answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])

        self.assertFalse(answer.export_yt_status.exported)
        # переводим статус экспорта в True
        answer.export_yt_status.exported = True
        answer.export_yt_status.save()

        # второй раз редактируем ответ, статус эскспорта должен вернуться в False
        data = {
            question.param_slug: 'test 2',
        }
        response = self.client.post(f'/v1/surveys/{survey.pk}/form/', data=data)
        self.assertEqual(response.status_code, 200)
        new_answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])

        self.assertEqual(answer.pk, new_answer.pk)
        self.assertFalse(new_answer.export_yt_status.exported)
        # опять переводим статус экспорта в True
        new_answer.export_yt_status.exported = True
        new_answer.export_yt_status.save()

        # третий раз редактируем ответ, но в этот раз бросаем исключение на сохранении статуса экспорта
        data = {
            question.param_slug: 'test 3',
        }
        with patch('events.surveyme.forms.AnswerExportYtStatus.save') as mock_save:
            mock_save.side_effect = ValueError
            response = self.client.post(f'/v1/surveys/{survey.pk}/form/', data=data)
        mock_save.assert_called_once_with(update_fields=['exported'])
        self.assertEqual(response.status_code, 200)
        newest_answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])

        self.assertEqual(answer.pk, newest_answer.pk)
        self.assertTrue(newest_answer.export_yt_status.exported)


class TestAnswerExportYtStatusAfterSubmit(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(
            is_published_external=True,
            is_public=True,
            save_logs_for_statbox=False,
        )

    def test_shouldnt_add_export_status_for_intranet(self):
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)

        answer_id = response.data['answer_id']
        with self.assertRaises(AnswerExportYtStatus.DoesNotExist):
            AnswerExportYtStatus.objects.get(answer_id=answer_id)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_add_export_status_for_business(self):
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)

        answer_id = response.data['answer_id']
        export_status = AnswerExportYtStatus.objects.get(answer_id=answer_id)
        self.assertFalse(export_status.exported)

    def test_should_add_export_status_for_intranet(self):
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)

        answer_id = response.data['answer_id']
        export_status = AnswerExportYtStatus.objects.get(answer_id=answer_id)
        self.assertFalse(export_status.exported)


class TestSurveyCommand(TestCase):
    def setUp(self):
        self.survey = SurveyFactory()

    def test_should_invoke_doit_once_1(self):
        now = timezone.now()
        self.survey.date_created = now - timedelta(days=10)
        self.survey.save()

        self.survey.answercount.count = 100
        self.survey.answercount.save()

        command = SurveyCommand(self.survey, days=30, max_count=1000, max_created=90)
        with patch.object(SurveyCommand, 'doit') as mock_doit:
            command.execute()

        mock_doit.assert_called_once_with(self.survey.date_created.date(), now.date() + timedelta(days=1))

    def test_should_invoke_doit_once_2(self):
        now = timezone.now()
        self.survey.date_created = now - timedelta(days=10)
        self.survey.save()

        self.survey.answercount.count = 1100
        self.survey.answercount.save()

        command = SurveyCommand(self.survey, days=30, max_count=1000, max_created=90)
        with patch.object(SurveyCommand, 'doit') as mock_doit:
            command.execute()

        mock_doit.assert_called_once_with(self.survey.date_created.date(), now.date() + timedelta(days=1))

    def test_should_invoke_doit_once_3(self):
        now = timezone.now()
        self.survey.date_created = now - timedelta(days=100)
        self.survey.save()

        self.survey.answercount.count = 100
        self.survey.answercount.save()

        command = SurveyCommand(self.survey, days=30, max_count=1000, max_created=90)
        with patch.object(SurveyCommand, 'doit') as mock_doit:
            command.execute()

        mock_doit.assert_called_once_with(self.survey.date_created.date(), now.date() + timedelta(days=1))

    def test_should_invoke_doit_four_times(self):
        now = timezone.now()
        self.survey.date_created = now - timedelta(days=100)
        self.survey.save()

        self.survey.answercount.count = 1100
        self.survey.answercount.save()

        command = SurveyCommand(self.survey, days=30, max_count=1000, max_created=90)
        with patch.object(SurveyCommand, 'doit') as mock_doit:
            command.execute()

        self.assertEqual(mock_doit.call_count, 4)
        first_date = self.survey.date_created.date()
        expected = [
            call(first_date, first_date + timedelta(days=30)),
            call(first_date + timedelta(days=30), first_date + timedelta(days=60)),
            call(first_date + timedelta(days=60), first_date + timedelta(days=90)),
            call(first_date + timedelta(days=90), now.date() + timedelta(days=1)),
        ]
        self.assertListEqual(mock_doit.call_args_list, expected)

    def make_datetime(self, dt):
        if isinstance(dt, date) and not isinstance(dt, datetime):
            dt = datetime(dt.year, dt.month, dt.day)
        if isinstance(dt, datetime):
            return timezone.make_aware(dt, timezone.pytz.UTC)

    def test_should_invoke_doit_many_times(self):
        start_epoch = self.make_datetime(SurveyCommand.start_epoch)
        self.survey.date_created = start_epoch - timedelta(days=100)
        self.survey.save()

        self.survey.answercount.count = 1100
        self.survey.answercount.save()

        command = SurveyCommand(self.survey, days=30, max_count=1000, max_created=90)
        with patch.object(SurveyCommand, 'doit') as mock_doit:
            command.execute()

        self.assertTrue(mock_doit.call_count > 1)
        first_date = start_epoch.date()
        self.assertEqual(mock_doit.call_args_list[0], call(first_date, first_date + timedelta(days=30)))


class TestRemoveTableFromYtCommand(TestCase):
    def setUp(self):
        self.survey = SurveyFactory()

    def test_should_invoke_delete_path(self):
        from events.common_app.yt.utils import get_yt_path

        command = RemoveTableFromYtCommand(self.survey)
        with patch('yt.wrapper.YtClient.remove') as mock_remove:
            command.execute()

        path = get_yt_path(self.survey.pk)
        mock_remove.assert_called_once_with(path, recursive=True, force=True)


class TestPrepareAnswersToExportTask(TestCase):
    def setUp(self):
        self.survey = SurveyFactory(save_logs_for_statbox=True)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_execute_for_business(self):
        with patch.object(PrepareAnswersToExportCommand, 'execute') as mock_execute:
            prepare_answers_to_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_does_not_exist(self):
        self.survey.delete()

        with patch.object(PrepareAnswersToExportCommand, 'execute') as mock_execute:
            prepare_answers_to_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_was_deleted(self):
        self.survey.is_deleted = True
        self.survey.save()

        with patch.object(PrepareAnswersToExportCommand, 'execute') as mock_execute:
            prepare_answers_to_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_wasnt_selected_for_export_answers(self):
        self.survey.save_logs_for_statbox = False
        self.survey.save()

        with patch.object(PrepareAnswersToExportCommand, 'execute') as mock_execute:
            prepare_answers_to_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_should_execute_command(self):
        with patch.object(PrepareAnswersToExportCommand, 'execute') as mock_execute:
            prepare_answers_to_export(self.survey.pk)

        mock_execute.assert_called_once()


class TestRemoveAnswersFromExportTask(TestCase):
    def setUp(self):
        self.survey = SurveyFactory(is_deleted=True, save_logs_for_statbox=False)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_execute_for_business(self):
        with patch.object(RemoveAnswersFromExportCommand, 'execute') as mock_execute:
            remove_answers_from_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_does_not_exist(self):
        self.survey.delete()

        with patch.object(RemoveAnswersFromExportCommand, 'execute') as mock_execute:
            remove_answers_from_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_wasnt_selected_for_export_answers_and_wasnt_deleted(self):
        self.survey.is_deleted = False
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        with patch.object(RemoveAnswersFromExportCommand, 'execute') as mock_execute:
            remove_answers_from_export(self.survey.pk)

        mock_execute.assert_not_called()

    def test_should_execute_if_survey_was_selected_for_export_answers(self):
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        with patch.object(RemoveAnswersFromExportCommand, 'execute') as mock_execute:
            remove_answers_from_export(self.survey.pk)

        mock_execute.assert_called_once()

    def test_should_execute_if_survey_wasnt_deleted(self):
        self.survey.is_deleted = False
        self.survey.save()

        with patch.object(RemoveAnswersFromExportCommand, 'execute') as mock_execute:
            remove_answers_from_export(self.survey.pk)

        mock_execute.assert_called_once()

    def test_should_execute_command(self):
        with patch.object(RemoveAnswersFromExportCommand, 'execute') as mock_execute:
            remove_answers_from_export(self.survey.pk)

        mock_execute.assert_called_once()


class TestRemoveTableFromYtTask(TestCase):
    def setUp(self):
        self.survey = SurveyFactory(is_deleted=True, save_logs_for_statbox=False)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_execute_for_business(self):
        with patch.object(RemoveTableFromYtCommand, 'execute') as mock_execute:
            remove_table_from_yt(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_does_not_exist(self):
        self.survey.delete()

        with patch.object(RemoveTableFromYtCommand, 'execute') as mock_execute:
            remove_table_from_yt(self.survey.pk)

        mock_execute.assert_not_called()

    def test_shouldnt_execute_if_survey_wasnt_selected_for_export_answers_and_wasnt_deleted(self):
        self.survey.is_deleted = False
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        with patch.object(RemoveTableFromYtCommand, 'execute') as mock_execute:
            remove_table_from_yt(self.survey.pk)

        mock_execute.assert_not_called()

    def test_should_execute_if_survey_was_selected_for_export_answers(self):
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        with patch.object(RemoveTableFromYtCommand, 'execute') as mock_execute:
            remove_table_from_yt(self.survey.pk)

        mock_execute.assert_called_once()

    def test_should_execute_if_survey_wasnt_deleted(self):
        self.survey.is_deleted = False
        self.survey.save()

        with patch.object(RemoveTableFromYtCommand, 'execute') as mock_execute:
            remove_table_from_yt(self.survey.pk)

        mock_execute.assert_called_once()

    def test_should_execute_command(self):
        with patch.object(RemoveTableFromYtCommand, 'execute') as mock_execute:
            remove_table_from_yt(self.survey.pk)

        mock_execute.assert_called_once()


class TestChangeSurveyExportStatus(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(user=user, save_logs_for_statbox=False)

    def test_should_invoke_prepare_answers_to_export(self):
        data = {
            'export_answers_to_yt': True,
        }
        with patch.object(Survey, 'prepare_answers_to_export') as mock_prepare:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        mock_prepare.assert_called_once()

    def test_shouldnt_invoke_prepare_answers_to_export(self):
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        data = {
            'export_answers_to_yt': True,
        }
        with patch.object(Survey, 'prepare_answers_to_export') as mock_prepare:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        mock_prepare.assert_not_called()

    def test_should_invoke_remove_answers_from_export(self):
        self.survey.save_logs_for_statbox = True
        self.survey.save()

        data = {
            'export_answers_to_yt': False,
        }
        with patch.object(Survey, 'remove_answers_from_export') as mock_remove:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        mock_remove.assert_called_once()

    def test_shouldnt_invoke_remove_answers_from_export(self):
        data = {
            'export_answers_to_yt': False,
        }
        with patch.object(Survey, 'remove_answers_from_export') as mock_remove:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        mock_remove.assert_not_called()


class TestSurveyExportMethods(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()

    def test_should_invoke_prepare_task(self):
        with patch('events.surveyme.tasks.prepare_answers_to_export.delay') as mock_prepare:
            self.survey.prepare_answers_to_export()

        mock_prepare.assert_called_once_with(self.survey.pk)

    def test_should_invoke_remove_tasks(self):
        with patch('events.surveyme.tasks.remove_answers_from_export.apply_async') as mock_remove_answers:
            with patch('events.surveyme.tasks.remove_table_from_yt.apply_async') as mock_remove_table:
                self.survey.remove_answers_from_export()

        mock_remove_answers.assert_called_once_with(
            args=(self.survey.pk,),
            countdown=settings.YT_ANSWERS_REMOVE_COUNTDOWN,
        )
        mock_remove_table.assert_called_once_with(
            args=(self.survey.pk,),
            countdown=settings.YT_ANSWERS_REMOVE_COUNTDOWN,
        )


class TestConvertAnswerDataWithNullableGroup(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            param_is_required=False,
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question,
            param_is_required=False,
        )

    def test_should_correct_convert_answer_data_1(self):
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.group_question.get_answer_info(),
                    'value': [
                        None,
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '1',
                        }],
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '2',
                        }],
                    ],
                }],
            }
        )
        result = convert_answer_data(answer.data)
        value = result['data'][self.group_question.param_slug]['value']
        self.assertEqual(len(value), 2)
        self.assertIn(self.question.param_slug, value[0])
        self.assertIn(self.question.param_slug, value[1])

    def test_should_correct_convert_answer_data_2(self):
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '1',
                        }],
                        None,
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '2',
                        }],
                    ],
                }],
            }
        )
        result = convert_answer_data(answer.data)
        value = result['data'][self.group_question.param_slug]['value']
        self.assertEqual(len(value), 2)
        self.assertIn(self.question.param_slug, value[0])
        self.assertIn(self.question.param_slug, value[1])

    def test_should_correct_convert_answer_data_3(self):
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '1',
                        }],
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '2',
                        }],
                        None,
                    ],
                }],
            }
        )
        result = convert_answer_data(answer.data)
        value = result['data'][self.group_question.param_slug]['value']
        self.assertEqual(len(value), 2)
        self.assertIn(self.question.param_slug, value[0])
        self.assertIn(self.question.param_slug, value[1])
