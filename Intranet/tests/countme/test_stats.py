# -*- coding: utf-8 -*-
from datetime import timedelta
from django.test import TestCase, override_settings
from django.utils import timezone
from django.utils.translation import ugettext as _
from unittest.mock import patch

from events.abc.factories import AbcServiceFactory
from events.accounts.factories import (
    OrganizationFactory,
    UserFactory,
)
from events.accounts.utils import GENDER_MALE, GENDER_FEMALE
from events.common_app.directory import DirectoryClient
from events.countme.factories import QuestionCountFactory
from events.countme.stats import (
    get_stats_info,
    DataSourceWikiTableSource,
    DataSourceStaffOffice,
    DataSourceStaffGroup,
    DataSourceStaffOrganization,
)
from events.countme.utils import (
    check_if_need_rebuild_counters,
    DELTA_STATISTICS_UPDATE,
    MAX_STATISTICS_REBUILD_COUNT,
)
from events.data_sources.factories import (
    UniversityFactory,
    TableRowFactory,
)
from events.geobase_contrib.factories import (
    CityFactory,
    CountryFactory,
)
from events.music.factories import MusicGenreFactory
from events.staff.factories import (
    StaffGroupFactory,
    StaffOfficeFactory,
    StaffOrganizationFactory,
    StaffPersonFactory,
)
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
    ProfileSurveyAnswerFactory,
)
from events.surveyme.models import AnswerType


class TestStats(TestCase):
    fixtures = ['initial_data.json']

    def test_answer_short_text(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short Text',
            param_is_hidden=True,
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='one', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='', count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertDictEqual(stats['questions'][0]['stats'], {})
        self.assertDictEqual(stats['questions'][0]['question'], {
            'id': question.pk,
            'group_id': None,
            'is_hidden': True,
        })

    def test_answer_short_text_grouped(self):
        survey = SurveyFactory()
        group_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            label='Group',
        )
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short Text',
            group=group_question,
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='', count=5)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertDictEqual(stats['questions'][0]['stats'], {})
        self.assertDictEqual(stats['questions'][0]['question'], {
            'id': question.pk,
            'group_id': None,
            'is_hidden': False,
        })

    def test_answer_boolean(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_boolean'),
            label='Yes No',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='1', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='0', count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertDictEqual(stats['questions'][0]['stats'], {
            _('Да'): 3,
            _('Нет'): 2,
        })

    def test_answer_choices_survey_question_choice(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
            label='Choice',
        )
        choices = [
            SurveyQuestionChoiceFactory(survey_question=question, label='One'),
            SurveyQuestionChoiceFactory(survey_question=question, label='Two'),
            SurveyQuestionChoiceFactory(survey_question=question, label='Three'),
        ]
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(choices[0].pk), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(choices[1].pk), count=4)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 7)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (choices[1].label, 4),
            (choices[0].label, 3),
            (choices[2].label, 0),
        ])

    def test_answer_choices_survey_question_matrix_choice(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_matrix_choice',
            label='Matrix',
        )
        rows = [
            SurveyQuestionMatrixTitleFactory(survey_question=question, type='row', label='Row1'),
            SurveyQuestionMatrixTitleFactory(survey_question=question, type='row', label='Row2'),
        ]
        cols = [
            SurveyQuestionMatrixTitleFactory(survey_question=question, type='column', label='1'),
            SurveyQuestionMatrixTitleFactory(survey_question=question, type='column', label='2'),
            SurveyQuestionMatrixTitleFactory(survey_question=question, type='column', label='3'),
        ]
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='%s:%s' % (rows[0].pk, cols[0].pk), count=1)
        QuestionCountFactory(survey=survey, question=question, composite_key='%s:%s' % (rows[1].pk, cols[1].pk), count=2)
        QuestionCountFactory(survey=survey, question=question, composite_key='%s:%s' % (rows[1].pk, cols[2].pk), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertDictEqual(stats['questions'][0]['stats'], {
            rows[0].label: [
                {'label': cols[0].label, 'count': 1},
                {'label': cols[1].label, 'count': 0},
                {'label': cols[2].label, 'count': 0},
            ],
            rows[1].label: [
                {'label': cols[1].label, 'count': 2},
                {'label': cols[2].label, 'count': 2},
                {'label': cols[0].label, 'count': 0},
            ],
        })

    def test_answer_choices_gender(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='gender',
            label='Gender',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=GENDER_MALE, count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=GENDER_FEMALE, count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertDictEqual(stats['questions'][0]['stats'], {
            _('Мужской'): 3,
            _('Женский'): 2,
        })

    def test_answer_choices_staff_office(self):
        offices = [
            StaffOfficeFactory(name='Office1'),
            StaffOfficeFactory(name='Office2'),
            StaffOfficeFactory(name='Office3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='staff_office',
            label='Office',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(offices[0].staff_id), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(offices[1].staff_id), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (offices[0].name, 3),
            (offices[1].name, 2),
        ])

    def test_answer_choices_staff_organization(self):
        organizations = [
            StaffOrganizationFactory(name='Organization1'),
            StaffOrganizationFactory(name='Organization2'),
            StaffOrganizationFactory(name='Organization3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='staff_organization',
            label='Organization',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(organizations[0].staff_id), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(organizations[1].staff_id), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (organizations[0].name, 3),
            (organizations[1].name, 2),
        ])

    def test_answer_choices_staff_group(self):
        groups = [
            StaffGroupFactory(name='Group1'),
            StaffGroupFactory(name='Group2'),
            StaffGroupFactory(name='Group3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='staff_group',
            label='Group',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(groups[0].staff_id), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(groups[1].staff_id), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (groups[0].name, 3),
            (groups[1].name, 2),
        ])

    def test_answer_choices_staff_person(self):
        people = [
            StaffPersonFactory(first_name='Name1', last_name='Surname1'),
            StaffPersonFactory(first_name='Name2', last_name='Surname2'),
            StaffPersonFactory(first_name='Name3', last_name='Surname3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='staff_login',
            label='Person',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(people[0].login), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(people[1].login), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            ('%s %s' % (people[0].first_name, people[0].last_name), 3),
            ('%s %s' % (people[1].first_name, people[1].last_name), 2),
        ])

    def test_answer_choices_abc_service(self):
        services = [
            AbcServiceFactory(name='Service1'),
            AbcServiceFactory(name='Service2'),
            AbcServiceFactory(name='Service3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='abc_service',
            label='Service',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=services[0].slug, count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=services[1].slug, count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (services[0].name, 3),
            (services[1].name, 2),
        ])

    def test_answer_choices_music_genre(self):
        genres = [
            MusicGenreFactory(title='Genre1'),
            MusicGenreFactory(title='Genre2'),
            MusicGenreFactory(title='Genre3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='music_genre',
            label='Genre',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=genres[0].music_id, count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=genres[1].music_id, count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (genres[0].title, 3),
            (genres[1].title, 2),
        ])

    def test_answer_choices_city(self):
        cities = [
            CityFactory(name='City1'),
            CityFactory(name='City2'),
            CityFactory(name='City3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='city',
            label='City',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(cities[0].pk), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(cities[1].pk), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (cities[0].name, 3),
            (cities[1].name, 2),
        ])

    def test_answer_choices_country(self):
        countries = [
            CountryFactory(name='Country1'),
            CountryFactory(name='Country2'),
            CountryFactory(name='Country3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='country',
            label='Country',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(countries[0].pk), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(countries[1].pk), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (countries[0].name, 3),
            (countries[1].name, 2),
        ])

    def test_answer_choices_university(self):
        universities = [
            UniversityFactory(name='University1'),
            UniversityFactory(name='University2'),
            UniversityFactory(name='University3'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='university',
            label='Universities',
        )
        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=str(universities[0].pk), count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=str(universities[1].pk), count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (universities[0].name, 3),
            (universities[1].name, 2),
        ])

    def test_answer_choices_yt_table_source(self):
        rows = [
            TableRowFactory(text='Row1', table_identifier='//home/forms/test_hahn'),
            TableRowFactory(text='Row2', table_identifier='//home/forms/test_hahn'),
            TableRowFactory(text='Row3', table_identifier='//home/forms/test_hahn'),
        ]
        user = UserFactory()
        survey = SurveyFactory(user=user)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='yt_table_source',
            param_data_source_params={
                'filters': [{
                    'value': 'https://yt.yandex-team.ru/hahn/navigation?path=//home/forms/test',
                }],
            },
            label='YT',
        )

        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key=rows[0].source_id, count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key=rows[1].source_id, count=2)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            (rows[0].text, 3),
            (rows[1].text, 2),
        ])

    def test_answer_choices_wiki_table_source(self):
        rows = {
            'structure': {
                'fields': [{
                    'title': 'name',
                }],
            },
            'rows': [
                [{'raw': 'Wiki1'}],
                [{'raw': 'Wiki2'}],
                [{'raw': 'Wiki3'}],
            ],
        }
        user = UserFactory()
        survey = SurveyFactory(user=user)
        params = {
            'filters': [{
                'value': '/users/kdunaev/testtable/',
            }],
        }
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='wiki_table_source',
            param_data_source_params=params,
            label='Wiki',
        )

        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='1', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='2', count=2)

        with patch.object(DataSourceWikiTableSource, 'get_supertag') as mock_get_supertag:
            with patch.object(DataSourceWikiTableSource, 'get_table_data') as mock_get_table_data:
                mock_get_supertag.return_value = 'users/kdunaev/testtable'
                mock_get_table_data.return_value = rows
                stats = get_stats_info(survey)
                mock_get_supertag.assert_called_once_with(params, None)
                mock_get_table_data.assert_called_once_with('users/kdunaev/testtable', user.uid, None)

        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            ('Wiki1', 3),
            ('Wiki2', 2),
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_answer_choices_wiki_table_source_biz(self):
        rows = {
            'structure': {
                'fields': [{
                    'title': 'name',
                }],
            },
            'rows': [
                [{'raw': 'Wiki1'}],
                [{'raw': 'Wiki2'}],
                [{'raw': 'Wiki3'}],
            ],
        }
        user = UserFactory()
        org = OrganizationFactory()
        survey = SurveyFactory(user=user, org=org)
        params = {
            'filters': [{
                'value': '/users/kdunaev/testtable/',
            }],
        }
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='wiki_table_source',
            param_data_source_params=params,
            label='Wiki',
        )

        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='1', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='2', count=2)

        with patch.object(DataSourceWikiTableSource, 'get_supertag') as mock_get_supertag:
            with patch.object(DataSourceWikiTableSource, 'get_table_data') as mock_get_table_data:
                mock_get_supertag.return_value = 'users/kdunaev/testtable'
                mock_get_table_data.return_value = rows
                stats = get_stats_info(survey)
                mock_get_supertag.assert_called_once_with(params, org.dir_id)
                mock_get_table_data.assert_called_once_with('users/kdunaev/testtable', user.uid, org.dir_id)

        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            ('Wiki1', 3),
            ('Wiki2', 2),
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_answer_choices_dir_user(self):
        users = [
            {
                'id': 101,
                'name': {'first': 'Name1', 'last': 'Surname1'},
                'is_robot': False,
            },
            {
                'id': 102,
                'name': {'first': 'Robot1', 'last': ''},
                'is_robot': True,
            },
            {
                'id': 103,
                'name': {'first': 'Name2', 'last': 'Surname2'},
                'is_robot': False,
            },
            {
                'id': 104,
                'name': {'first': 'Name3', 'last': 'Surname3'},
                'is_robot': False,
            },
        ]
        user = UserFactory()
        org = OrganizationFactory()
        survey = SurveyFactory(user=user, org=org)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='dir_user',
            label='User',
        )

        survey.answercount.count = 6
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='101', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='102', count=2)
        QuestionCountFactory(survey=survey, question=question, composite_key='103', count=1)

        with patch.object(DirectoryClient, 'get_users') as mock_get_users:
            mock_get_users.return_value = users
            stats = get_stats_info(survey)
            mock_get_users.assert_called_once_with(org.dir_id, fields='id,name,is_robot')

        self.assertEqual(stats['answers']['count'], 6)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 6)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            ('Name1 Surname1', 3),
            ('102', 2),
            ('Name2 Surname2', 1),
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_answer_choices_dir_group(self):
        groups = [
            {
                'id': 11,
                'name': 'Group1',
            },
            {
                'id': 12,
                'name': 'Group2',
            },
            {
                'id': 13,
                'name': 'Group3',
            },
        ]
        user = UserFactory()
        org = OrganizationFactory()
        survey = SurveyFactory(user=user, org=org)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='dir_group',
            label='Group',
        )

        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='11', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='12', count=2)

        with patch.object(DirectoryClient, 'get_groups') as mock_get_groups:
            mock_get_groups.return_value = groups
            stats = get_stats_info(survey)
            mock_get_groups.assert_called_once_with(org.dir_id, fields='id,name')

        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            ('Group1', 3),
            ('Group2', 2),
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_answer_choices_dir_department(self):
        departments = [
            {
                'id': 110,
                'name': 'Department1',
            },
            {
                'id': 120,
                'name': 'Department2',
            },
            {
                'id': 130,
                'name': 'Department3',
            },
        ]
        user = UserFactory()
        org = OrganizationFactory()
        survey = SurveyFactory(user=user, org=org)
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='dir_department',
            label='Department',
        )

        survey.answercount.count = 5
        survey.answercount.save()
        QuestionCountFactory(survey=survey, question=question, composite_key='110', count=3)
        QuestionCountFactory(survey=survey, question=question, composite_key='120', count=2)

        with patch.object(DirectoryClient, 'get_departments') as mock_get_departments:
            mock_get_departments.return_value = departments
            stats = get_stats_info(survey)
            mock_get_departments.assert_called_once_with(org.dir_id, fields='id,name')

        self.assertEqual(stats['answers']['count'], 5)
        self.assertEqual(len(stats['questions']), 1)
        self.assertEqual(stats['questions'][0]['answers'], 5)
        self.assertEqual(stats['questions'][0]['label'], question.label)
        self.assertListEqual(list(stats['questions'][0]['stats'].items()), [
            ('Department1', 3),
            ('Department2', 2),
        ])

    def test_should_rebuild_counters(self):
        profile_survey_answer = ProfileSurveyAnswerFactory()
        self.assertTrue(check_if_need_rebuild_counters(profile_survey_answer.survey))

    def test_shouldnt_rebuild_counters_for_archived_survey(self):
        profile_survey_answer = ProfileSurveyAnswerFactory()
        profile_survey_answer.survey.date_archived = timezone.now()
        profile_survey_answer.survey.save()
        self.assertFalse(check_if_need_rebuild_counters(profile_survey_answer.survey))

    def test_shouldnt_rebuild_counters_without_answers(self):
        survey = SurveyFactory()
        self.assertFalse(check_if_need_rebuild_counters(survey))

    def test_shouldnt_rebuild_counters_for_old_answers(self):
        date_created = timezone.now() - timedelta(minutes=DELTA_STATISTICS_UPDATE)
        profile_survey_answer = ProfileSurveyAnswerFactory(date_created=date_created)
        self.assertFalse(check_if_need_rebuild_counters(profile_survey_answer.survey))

    def test_shouldt_rebuild_counters_for_large_amount_of_answers(self):
        profile_survey_answer = ProfileSurveyAnswerFactory()
        profile_survey_answer.survey.answercount.count = MAX_STATISTICS_REBUILD_COUNT + 1
        profile_survey_answer.survey.answercount.save()
        self.assertFalse(check_if_need_rebuild_counters(profile_survey_answer.survey))

    def test_should_rebuild_counters_if_answer_count_not_exist(self):
        profile_survey_answer = ProfileSurveyAnswerFactory()
        profile_survey_answer.survey.answercount.delete()
        self.assertTrue(check_if_need_rebuild_counters(profile_survey_answer.survey))

    def test_should_return_paginated_response(self):
        survey = SurveyFactory()
        survey.answercount.count = 1
        survey.answercount.save()
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        for i in range(6):
            question = SurveyQuestionFactory(survey=survey, answer_type=answer_short_text)
            QuestionCountFactory(survey=survey, question=question, composite_key=f'question{i}', count=1)

        stats = get_stats_info(survey, page=None, page_size=5)
        self.assertEqual(stats['answers']['count'], 1)
        self.assertEqual(len(stats['questions']), 5)

        stats = get_stats_info(survey, page=1, page_size=5)
        self.assertEqual(stats['answers']['count'], 1)
        self.assertEqual(len(stats['questions']), 5)

        stats = get_stats_info(survey, page=2, page_size=5)
        self.assertEqual(stats['answers']['count'], 1)
        self.assertEqual(len(stats['questions']), 1)

        stats = get_stats_info(survey, page=3, page_size=5)
        self.assertEqual(stats['answers']['count'], 1)
        self.assertEqual(len(stats['questions']), 0)

        stats = get_stats_info(survey, page=2)
        self.assertEqual(stats['answers']['count'], 1)
        self.assertEqual(len(stats['questions']), 6)

        stats = get_stats_info(survey)
        self.assertEqual(stats['answers']['count'], 1)
        self.assertEqual(len(stats['questions']), 6)


class TestDataSources(TestCase):
    def test_staff_group(self):
        groups = [
            StaffGroupFactory(),
            StaffGroupFactory(),
            StaffGroupFactory(),
        ]
        data_source = DataSourceStaffGroup()
        result = data_source.get_data([groups[0].staff_id, groups[1].staff_id, 'yandex'])

        self.assertEqual(len(result), 2)
        expected = {
            str(groups[0].staff_id): groups[0].name,
            str(groups[1].staff_id): groups[1].name,
        }
        self.assertDictEqual(result, expected)

    def test_staff_office(self):
        offices = [
            StaffOfficeFactory(),
            StaffOfficeFactory(),
            StaffOfficeFactory(),
        ]
        data_source = DataSourceStaffOffice()
        result = data_source.get_data([offices[0].staff_id, offices[1].staff_id, 'morozov'])

        self.assertEqual(len(result), 2)
        expected = {
            str(offices[0].staff_id): offices[0].name,
            str(offices[1].staff_id): offices[1].name,
        }
        self.assertDictEqual(result, expected)

    def test_staff_organization(self):
        organizations = [
            StaffOrganizationFactory(),
            StaffOrganizationFactory(),
            StaffOrganizationFactory(),
        ]
        data_source = DataSourceStaffOrganization()
        result = data_source.get_data([organizations[0].staff_id, organizations[1].staff_id, 'dc-iva'])

        self.assertEqual(len(result), 2)
        expected = {
            str(organizations[0].staff_id): organizations[0].name,
            str(organizations[1].staff_id): organizations[1].name,
        }
        self.assertDictEqual(result, expected)
