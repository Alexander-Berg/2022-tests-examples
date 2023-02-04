# -*- coding: utf-8 -*-
import os.path

from collections import OrderedDict, defaultdict
from datetime import timedelta
from django.conf import settings
from django.test import TestCase, override_settings
from django.utils import timezone
from django.utils.translation import ugettext as _
from freezegun import freeze_time
from json import loads as json_loads
from unittest.mock import patch, ANY, call

from events.accounts.factories import OrganizationFactory
from events.common_app.directory import DirectoryClient
from events.common_app.disk.client import DiskClient, DiskUploadError
from events.countme.factories import QuestionGroupDepthFactory, AnswerCountByDateFactory
from events.surveyme.api_admin.v2.serializers import (
    ExportAnswersSerializer,
    MIN_LIMIT_SIZE,
    MAX_LIMIT_SIZE,
)
from events.surveyme.models import AnswerType
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
    ProfileSurveyAnswerFactory,
)
from events.surveyme.export_answers_v2 import (
    export_answers,
    get_exported_answers_stream,
    get_answers,
    make_label,
    Answer,
    AnswerMetadata,
    FilesGetter,
    ChoiceGetter,
    MultipleChoiceGetter,
    TitleGetter,
    BooleanGetter,
    PassportPersonalData,
    DirPersonalData,
    XlsxFormatter,
    CsvFormatter,
    get_uploader,
    MdsUploader,
    DiskUploader,
    MdsDownloader,
    ExportError,
)


@freeze_time('2020-04-22 09:18')
class TestExportAnswersSerializer(TestCase):  # {{{
    serializer_class = ExportAnswersSerializer

    def test_should_validate_empty_data(self):  # {{{
        data = {}
        serializer = self.serializer_class(data=data)
        self.assertTrue(serializer.is_valid())

        validated_data = serializer.validated_data
        self.assertEqual(validated_data['export_format'], 'xlsx')
        self.assertEqual(validated_data['upload'], 'mds')
    # }}}

    def test_should_validate_data(self):  # {{{
        data = {
            'pks': '101, 103, 105',
            'export_format': 'csv',
            'upload': 'disk',
            'export_columns': {
                'questions': '1001, 1002, 1003',
                'user_fields': 'param_name, param_surname',
                'answer_fields': 'date_updated, uid',
            },
            'date_started': timezone.now() - timedelta(days=7),
            'date_finished': timezone.now(),
            'limit': 100,
        }
        serializer = self.serializer_class(data=data)
        self.assertTrue(serializer.is_valid())

        validated_data = serializer.validated_data
        self.assertListEqual(validated_data['pks'], [101, 103, 105])
        self.assertEqual(validated_data['export_format'], 'csv')
        self.assertEqual(validated_data['upload'], 'disk')
        self.assertEqual(validated_data['limit'], 100)
        self.assertEqual(validated_data['date_started'], timezone.now() - timedelta(days=7))
        self.assertEqual(validated_data['date_finished'], timezone.now())
        self.assertListEqual(validated_data['export_columns']['questions'], [1001, 1002, 1003])
        self.assertListEqual(validated_data['export_columns']['user_fields'], ['param_name', 'param_surname'])
        self.assertListEqual(validated_data['export_columns']['answer_fields'], ['date_updated', 'uid'])
    # }}}

    def test_shouldnt_validate_data_if_limit_less_then_min(self):  # {{{
        data = {
            'limit': MIN_LIMIT_SIZE - 1,
        }
        serializer = self.serializer_class(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('limit', serializer.errors)
    # }}}

    def test_shouldnt_validate_data_if_limit_greater_then_max(self):  # {{{
        data = {
            'limit': MAX_LIMIT_SIZE + 1,
        }
        serializer = self.serializer_class(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('limit', serializer.errors)
    # }}}
# }}}


@freeze_time('2020-04-22 09:18')
class TestExportAnswers(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):  # {{{
        # {{{ metadata definition
        self.survey = SurveyFactory()
        self.questions = OrderedDict([
            (question.label, question)
            for question in [
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_short_text'),
                    label='text',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_boolean'),
                    label='boolean',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_files'),
                    label='files',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_date'),
                    param_date_field_type='date',
                    label='date',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_date'),
                    param_date_field_type='daterange',
                    label='daterange',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_payment'),
                    label='payment'
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_choices'),
                    param_data_source='survey_question_choice',
                    param_is_allow_multiple_choice=True,
                    label='choices'
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_choices'),
                    param_data_source='survey_question_matrix_choice',
                    label='titles'
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_choices'),
                    param_data_source='users',
                    label='users'
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_group'),
                    label='group',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_short_text'),
                    position=1,
                    label='grouped_text',
                ),
                SurveyQuestionFactory(
                    survey=self.survey,
                    answer_type=AnswerType.objects.get(slug='answer_choices'),
                    param_data_source='survey_question_choice',
                    position=2,
                    label='grouped_choices'
                ),
            ]
        ])

        QuestionGroupDepthFactory(
            survey=self.survey,
            question=self.questions['group'],
            depth=2,
        )

        question = self.questions['grouped_text']
        question.group_id = self.questions['group'].pk
        question.save()

        question = self.questions['grouped_choices']
        question.group_id = self.questions['group'].pk
        question.save()

        self.choices = {}

        question = self.questions['choices']
        self.choices[question.label] = OrderedDict([
            (choice.label, choice)
            for choice in [
                SurveyQuestionChoiceFactory(
                    survey_question=question,
                    label='one',
                ),
                SurveyQuestionChoiceFactory(
                    survey_question=question,
                    label='two',
                ),
                SurveyQuestionChoiceFactory(
                    survey_question=question,
                    label='three',
                ),
            ]
        ])

        question = self.questions['grouped_choices']
        self.choices[question.label] = OrderedDict([
            (choice.label, choice)
            for choice in [
                SurveyQuestionChoiceFactory(
                    survey_question=question,
                    label='first',
                ),
                SurveyQuestionChoiceFactory(
                    survey_question=question,
                    label='second',
                ),
                SurveyQuestionChoiceFactory(
                    survey_question=question,
                    label='third',
                ),
            ]
        ])

        self.titles = {}

        question = self.questions['titles']
        self.titles[question.label] = {
            'row': OrderedDict([
                (choice.label, choice)
                for choice in [
                    SurveyQuestionMatrixTitleFactory(
                        survey_question=question,
                        type='row',
                        label='row1',
                    ),
                ]
            ]),
            'col': OrderedDict([
                (choice.label, choice)
                for choice in [
                    SurveyQuestionMatrixTitleFactory(
                        survey_question=question,
                        type='column',
                        label='1',
                    ),
                    SurveyQuestionMatrixTitleFactory(
                        survey_question=question,
                        type='column',
                        label='2',
                    ),
                    SurveyQuestionMatrixTitleFactory(
                        survey_question=question,
                        type='column',
                        label='3',
                    ),
                ]
            ])
        }
        # }}}
        # {{{ answers definition
        self.answers = [
            ProfileSurveyAnswerFactory(  # {{{ first answer
                survey=self.survey,
                source_request={
                    'ip': '127.0.0.1',
                    'cookies': {
                        'yandexuid': '123456',
                    },
                },
                data={
                    'uid': '123',
                    'data': [
                        {
                            'value': 'simple text\ufffe',
                            'question': {
                                'id': self.questions['text'].pk,
                                'answer_type': {
                                    'slug': self.questions['text'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': True,
                            'question': {
                                'id': self.questions['boolean'].pk,
                                'answer_type': {
                                    'slug': self.questions['boolean'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'path': '/100/123456',
                                    'name': 'myfile1.txt',
                                },
                                {
                                    'path': '/101/234567',
                                    'name': 'myfile2.txt',
                                },
                            ],
                            'question': {
                                'id': self.questions['files'].pk,
                                'answer_type': {
                                    'slug': self.questions['files'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': '2020-04-15',
                            'question': {
                                'id': self.questions['date'].pk,
                                'answer_type': {
                                    'slug': self.questions['date'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': {
                                'begin': '2020-04-15',
                                'end': '2020-04-20',
                            },
                            'question': {
                                'id': self.questions['daterange'].pk,
                                'options': {
                                    'date_range': True,
                                },
                                'answer_type': {
                                    'slug': self.questions['daterange'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': {
                                'amount': 42,
                            },
                            'question': {
                                'id': self.questions['payment'].pk,
                                'answer_type': {
                                    'slug': self.questions['payment'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'key': str(self.choices['choices']['two'].pk),
                                },
                                {
                                    'key': str(self.choices['choices']['three'].pk),
                                },
                            ],
                            'question': {
                                'id': self.questions['choices'].pk,
                                'options': {
                                    'data_source': self.questions['choices'].param_data_source,
                                },
                                'answer_type': {
                                    'slug': self.questions['choices'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'row': {
                                        'key': str(self.titles['titles']['row']['row1'].pk),
                                    },
                                    'col': {
                                        'key': str(self.titles['titles']['col']['3'].pk),
                                    },
                                },
                            ],
                            'question': {
                                'id': self.questions['titles'].pk,
                                'options': {
                                    'data_source': self.questions['titles'].param_data_source,
                                },
                                'answer_type': {
                                    'slug': self.questions['titles'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'key': 'user1',
                                    'text': 'first user',
                                },
                                {
                                    'key': 'user2',
                                    'text': 'second user',
                                },
                            ],
                            'question': {
                                'id': self.questions['users'].pk,
                                'options': {
                                    'data_source': self.questions['users'].param_data_source,
                                },
                                'answer_type': {
                                    'slug': self.questions['users'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                [
                                    {
                                        'value': 'grouped text 1',
                                        'question': {
                                            'id': self.questions['grouped_text'].pk,
                                            'answer_type': {
                                                'slug': self.questions['grouped_text'].answer_type.slug,
                                            },
                                        },
                                    },
                                    {
                                        'value': [
                                            {
                                                'key': str(self.choices['grouped_choices']['first'].pk),
                                            },
                                        ],
                                        'question': {
                                            'id': self.questions['grouped_choices'].pk,
                                            'options': {
                                                'data_source': self.questions['grouped_choices'].param_data_source,
                                            },
                                            'answer_type': {
                                                'slug': self.questions['grouped_choices'].answer_type.slug,
                                            },
                                        },
                                    },
                                ],
                                [
                                    {
                                        'value': 'grouped text 2',
                                        'question': {
                                            'id': self.questions['grouped_text'].pk,
                                            'answer_type': {
                                                'slug': self.questions['grouped_text'].answer_type.slug,
                                            },
                                        },
                                    },
                                    {
                                        'value': [
                                            {
                                                'key': str(self.choices['grouped_choices']['second'].pk),
                                            },
                                        ],
                                        'question': {
                                            'id': self.questions['grouped_choices'].pk,
                                            'options': {
                                                'data_source': self.questions['grouped_choices'].param_data_source,
                                            },
                                            'answer_type': {
                                                'slug': self.questions['grouped_choices'].answer_type.slug,
                                            },
                                        },
                                    },
                                ],
                            ],
                            'question': {
                                'id': self.questions['group'].pk,
                                'answer_type': {
                                    'slug': self.questions['group'].answer_type.slug,
                                },
                            },
                        },
                    ],
                },
            ),  # }}}
            ProfileSurveyAnswerFactory(  # {{{ second answer
                survey=self.survey,
                source_request={
                    'ip': '::1',
                    'cookies': {
                        'yandexuid': '234567',
                    },
                },
                data={
                    'uid': '124',
                    'data': [
                        {
                            'value': 'another \x0csimple \x0ctext',
                            'question': {
                                'id': self.questions['text'].pk,
                                'answer_type': {
                                    'slug': self.questions['text'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': False,
                            'question': {
                                'id': self.questions['boolean'].pk,
                                'answer_type': {
                                    'slug': self.questions['boolean'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'path': '/102/345678',
                                    'name': 'myfile3.txt',
                                },
                            ],
                            'question': {
                                'id': self.questions['files'].pk,
                                'answer_type': {
                                    'slug': self.questions['files'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': '2020-04-16',
                            'question': {
                                'id': self.questions['date'].pk,
                                'answer_type': {
                                    'slug': self.questions['date'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': {
                                'begin': '2020-04-16',
                                'end': '2020-04-21',
                            },
                            'question': {
                                'id': self.questions['daterange'].pk,
                                'options': {
                                    'date_range': True,
                                },
                                'answer_type': {
                                    'slug': self.questions['daterange'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': {
                                'amount': 12,
                            },
                            'question': {
                                'id': self.questions['payment'].pk,
                                'answer_type': {
                                    'slug': self.questions['payment'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'key': str(self.choices['choices']['one'].pk),
                                },
                            ],
                            'question': {
                                'id': self.questions['choices'].pk,
                                'options': {
                                    'data_source': self.questions['choices'].param_data_source,
                                },
                                'answer_type': {
                                    'slug': self.questions['choices'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'row': {
                                        'key': str(self.titles['titles']['row']['row1'].pk),
                                    },
                                    'col': {
                                        'key': str(self.titles['titles']['col']['1'].pk),
                                    },
                                },
                            ],
                            'question': {
                                'id': self.questions['titles'].pk,
                                'options': {
                                    'data_source': self.questions['titles'].param_data_source,
                                },
                                'answer_type': {
                                    'slug': self.questions['titles'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                {
                                    'key': 'user3',
                                    'text': 'third user',
                                },
                            ],
                            'question': {
                                'id': self.questions['users'].pk,
                                'options': {
                                    'data_source': self.questions['users'].param_data_source,
                                },
                                'answer_type': {
                                    'slug': self.questions['users'].answer_type.slug,
                                },
                            },
                        },
                        {
                            'value': [
                                [
                                    {
                                        'value': 'another grouped text 1',
                                        'question': {
                                            'id': self.questions['grouped_text'].pk,
                                            'answer_type': {
                                                'slug': self.questions['grouped_text'].answer_type.slug,
                                            },
                                        },
                                    },
                                    {
                                        'value': [
                                            {
                                                'key': str(self.choices['grouped_choices']['second'].pk),
                                            },
                                        ],
                                        'question': {
                                            'id': self.questions['grouped_choices'].pk,
                                            'options': {
                                                'data_source': self.questions['grouped_choices'].param_data_source,
                                            },
                                            'answer_type': {
                                                'slug': self.questions['grouped_choices'].answer_type.slug,
                                            },
                                        },
                                    },
                                ],
                                [
                                    {
                                        'value': 'another grouped text 2',
                                        'question': {
                                            'id': self.questions['grouped_text'].pk,
                                            'answer_type': {
                                                'slug': self.questions['grouped_text'].answer_type.slug,
                                            },
                                        },
                                    },
                                    {
                                        'value': [
                                            {
                                                'key': str(self.choices['grouped_choices']['third'].pk),
                                            },
                                        ],
                                        'question': {
                                            'id': self.questions['grouped_choices'].pk,
                                            'options': {
                                                'data_source': self.questions['grouped_choices'].param_data_source,
                                            },
                                            'answer_type': {
                                                'slug': self.questions['grouped_choices'].answer_type.slug,
                                            },
                                        },
                                    },
                                ],
                            ],
                            'question': {
                                'id': self.questions['group'].pk,
                                'answer_type': {
                                    'slug': self.questions['group'].answer_type.slug,
                                },
                            },
                        },
                    ],
                },
            ),  # }}}
            ProfileSurveyAnswerFactory(  # {{{ third answer
                survey=self.survey,
                source_request={
                    'ip': '::1',
                    'cookies': {
                        'yandexuid': '234567',
                    },
                },
                data={
                    'uid': '124',
                    'data': [
                        {
                            'value': False,
                            'question': {
                                'id': self.questions['boolean'].pk,
                                'answer_type': {
                                    'slug': self.questions['boolean'].answer_type.slug,
                                },
                            },
                        },
                    ],
                },
            ),  # }}}
        ]
        for answer in self.answers:
            data = answer.data
            data['answer_id'] = answer.pk
            answer.data = data
            answer.save()

        self.survey.date_created = timezone.now() - timedelta(days=30)
        self.survey.save()

        self.answers[0].date_created = timezone.now() - timedelta(days=7)
        self.answers[0].save()

        self.answers[1].date_created = timezone.now() - timedelta(days=5)
        self.answers[1].save()

        self.answers[2].date_created = timezone.now() - timedelta(days=3)
        self.answers[2].save()
        # }}}
    # }}}

    def test_csv_row_number(self):  # {{{
        response = get_exported_answers_stream(
            survey_id=self.survey.pk,
            format='csv',
        )
        self.assertEqual(response.content_type, 'text/csv')
        buff = response.stream.getvalue()
        self.assertEqual(len(buff.decode().strip('\n').split('\n')), 4)
    # }}}

    def test_xlsx_export_content(self):  # {{{
        response = get_exported_answers_stream(
            survey_id=self.survey.pk,
            format='xlsx',
        )
        self.assertEqual(response.content_type, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
        buff = response.stream.getvalue()
        self.assertTrue(len(buff) > 0)
    # }}}

    def test_csv_export_content(self):  # {{{
        response = get_exported_answers_stream(
            survey_id=self.survey.pk,
            format='csv',
        )
        self.assertEqual(response.content_type, 'text/csv')
        buff = response.stream.getvalue()
        self.assertTrue(len(buff) > 0)
    # }}}

    def test_json_export_content(self):  # {{{
        response = get_exported_answers_stream(
            survey_id=self.survey.pk,
            format='json',
        )
        self.assertEqual(response.content_type, 'application/json')
        buff = response.stream.getvalue()
        data = json_loads(buff.decode())
        self.assertEqual(len(data), 3)
        headers = [
            _('ID'),
            _('Время создания'),
            'text',
            'boolean',
            'files',
            'date',
            make_label('daterange', _('Начало')), make_label('daterange', _('Конец')),
            'payment',
            make_label('choices', 'one'), make_label('choices', 'two'), make_label('choices', 'three'),
            make_label('titles', 'row1'),
            'users',
            'grouped_text [1]', 'grouped_choices [1]',
            'grouped_text [2]', 'grouped_choices [2]',
            'grouped_text [3]', 'grouped_choices [3]',
        ]
        make_answer = lambda lines: list(zip(headers, lines))
        expected = [
            make_answer([
                str(self.answers[0].pk),
                '2020-04-15 12:18:00',
                'simple text\ufffe',
                _('Yes'),
                'https://forms.yandex-team.ru/files?path=%2F100%2F123456, https://forms.yandex-team.ru/files?path=%2F101%2F234567',
                '2020-04-15',
                '2020-04-15', '2020-04-20',
                _('Оплата заказа %(answer_id)s на сумму %(amount)s руб') % {'answer_id': self.answers[0].pk, 'amount': 42},
                '', 'two', 'three',
                '3',
                'first user, second user',
                'grouped text 1', 'first',
                'grouped text 2', 'second',
                '', '',
            ]),
            make_answer([
                str(self.answers[1].pk),
                '2020-04-17 12:18:00',
                'another \x0csimple \x0ctext',
                _('No'),
                'https://forms.yandex-team.ru/files?path=%2F102%2F345678',
                '2020-04-16',
                '2020-04-16', '2020-04-21',
                _('Оплата заказа %(answer_id)s на сумму %(amount)s руб') % {'answer_id': self.answers[1].pk, 'amount': 12},
                'one', '', '',
                '1',
                'third user',
                'another grouped text 1', 'second',
                'another grouped text 2', 'third',
                '', '',
            ]),
            make_answer([
                str(self.answers[2].pk),
                '2020-04-19 12:18:00',
                '',
                _('No'),
                '',
                '',
                '', '',
                '',
                '', '', '',
                '',
                '',
                '', '',
                '', '',
                '', '',
            ]),
        ]
        for i in range(3):
            for (data_item, expected_item) in zip(data[i], expected[i]):
                self.assertListEqual(list(data_item), list(expected_item))
    # }}}

    def test_json_export_content_with_tld(self):  # {{{
        response = get_exported_answers_stream(
            survey_id=self.survey.pk,
            format='json',
            tld='.com',
        )
        self.assertEqual(response.content_type, 'application/json')
        buff = response.stream.getvalue()
        data = json_loads(buff.decode())
        self.assertEqual(len(data), 3)
        headers = [
            _('ID'),
            _('Время создания'),
            'text',
            'boolean',
            'files',
            'date',
            make_label('daterange', _('Начало')), make_label('daterange', _('Конец')),
            'payment',
            make_label('choices', 'one'), make_label('choices', 'two'), make_label('choices', 'three'),
            make_label('titles', 'row1'),
            'users',
            'grouped_text [1]', 'grouped_choices [1]',
            'grouped_text [2]', 'grouped_choices [2]',
            'grouped_text [3]', 'grouped_choices [3]',
        ]
        make_answer = lambda lines: list(zip(headers, lines))
        expected = [
            make_answer([
                str(self.answers[0].pk),
                '2020-04-15 12:18:00',
                'simple text\ufffe',
                _('Yes'),
                'https://forms.yandex-team.com/files?path=%2F100%2F123456, https://forms.yandex-team.com/files?path=%2F101%2F234567',
                '2020-04-15',
                '2020-04-15', '2020-04-20',
                _('Оплата заказа %(answer_id)s на сумму %(amount)s руб') % {'answer_id': self.answers[0].pk, 'amount': 42},
                '', 'two', 'three',
                '3',
                'first user, second user',
                'grouped text 1', 'first',
                'grouped text 2', 'second',
                '', '',
            ]),
            make_answer([
                str(self.answers[1].pk),
                '2020-04-17 12:18:00',
                'another \x0csimple \x0ctext',
                _('No'),
                'https://forms.yandex-team.com/files?path=%2F102%2F345678',
                '2020-04-16',
                '2020-04-16', '2020-04-21',
                _('Оплата заказа %(answer_id)s на сумму %(amount)s руб') % {'answer_id': self.answers[1].pk, 'amount': 12},
                'one', '', '',
                '1',
                'third user',
                'another grouped text 1', 'second',
                'another grouped text 2', 'third',
                '', '',
            ]),
            make_answer([
                str(self.answers[2].pk),
                '2020-04-19 12:18:00',
                '',
                _('No'),
                '',
                '',
                '', '',
                '',
                '', '', '',
                '',
                '',
                '', '',
                '', '',
                '', '',
            ]),
        ]
        for i in range(3):
            for (data_item, expected_item) in zip(data[i], expected[i]):
                self.assertListEqual(list(data_item), list(expected_item))
    # }}}

    def test_internal_getters(self):  # {{{
        questions_pks = None
        columns = [
            'param_name', 'param_surname', 'param_patronymic',
            'param_gender', 'param_phone', 'param_subscribed_email',
            'param_birthdate', 'param_position', 'param_job_place',
            'ip', 'uid', 'yandexuid', 'date_updated',
        ]
        answer_metadata = AnswerMetadata(self.survey.pk)
        answer_metadata.init_data()
        getters = list(answer_metadata.filter_getters(questions_pks, columns))

        grouped_getters = defaultdict(list)
        for getter in getters:
            grouped_getters[getter.group_name].append(getter)

        self.assertEqual(len(grouped_getters['base']), 2)
        self.assertEqual(len(grouped_getters['question']), 16)
        self.assertEqual(len(grouped_getters['personal']), 5)
        self.assertEqual(len(grouped_getters['extra']), 4)
    # }}}

    @override_settings(IS_BUSINESS_SITE=True)
    def test_business_getters(self):  # {{{
        self.survey.org = OrganizationFactory()
        self.survey.save()

        questions_pks = None
        columns = [
            'param_name', 'param_surname', 'param_patronymic',
            'param_gender', 'param_phone', 'param_subscribed_email',
            'param_birthdate', 'param_position', 'param_job_place',
            'ip', 'uid', 'yandexuid', 'date_updated',
        ]
        answer_metadata = AnswerMetadata(self.survey.pk)
        answer_metadata.init_data()
        getters = list(answer_metadata.filter_getters(questions_pks, columns))

        grouped_getters = defaultdict(list)
        for getter in getters:
            grouped_getters[getter.group_name].append(getter)

        self.assertEqual(len(grouped_getters['base']), 2)
        self.assertEqual(len(grouped_getters['question']), 16)
        self.assertEqual(len(grouped_getters['personal']), 9)
        self.assertEqual(len(grouped_getters['extra']), 1)
    # }}}

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_getters(self):  # {{{
        questions_pks = None
        columns = [
            'param_name', 'param_surname', 'param_patronymic',
            'param_gender', 'param_phone', 'param_subscribed_email',
            'param_birthdate', 'param_position', 'param_job_place',
            'ip', 'uid', 'yandexuid', 'date_updated',
        ]
        answer_metadata = AnswerMetadata(self.survey.pk)
        answer_metadata.init_data()
        getters = list(answer_metadata.filter_getters(questions_pks, columns))

        grouped_getters = defaultdict(list)
        for getter in getters:
            grouped_getters[getter.group_name].append(getter)

        self.assertEqual(len(grouped_getters['base']), 2)
        self.assertEqual(len(grouped_getters['question']), 16)
        self.assertEqual(len(grouped_getters['personal']), 0)
        self.assertEqual(len(grouped_getters['extra']), 1)
    # }}}

    def test_internal_getters_with_selected_questions(self):  # {{{
        questions_pks = [
            self.questions['text'].pk,
            self.questions['choices'].pk,
        ]
        columns = None
        answer_metadata = AnswerMetadata(self.survey.pk)
        answer_metadata.init_data()
        getters = list(answer_metadata.filter_getters(questions_pks, columns))

        grouped_getters = defaultdict(list)
        for getter in getters:
            grouped_getters[getter.group_name].append(getter)

        self.assertEqual(len(grouped_getters['base']), 2)
        self.assertEqual(len(grouped_getters['question']), 4)
        self.assertEqual(len(grouped_getters['personal']), 0)
        self.assertEqual(len(grouped_getters['extra']), 0)
    # }}}

    @override_settings(IS_BUSINESS_SITE=True)
    def test_business_getters_with_selected_questions(self):  # {{{
        self.survey.org = OrganizationFactory()
        self.survey.save()

        questions_pks = [
            self.questions['text'].pk,
            self.questions['choices'].pk,
        ]
        columns = None
        answer_metadata = AnswerMetadata(self.survey.pk)
        answer_metadata.init_data()
        getters = list(answer_metadata.filter_getters(questions_pks, columns))

        grouped_getters = defaultdict(list)
        for getter in getters:
            grouped_getters[getter.group_name].append(getter)

        self.assertEqual(len(grouped_getters['base']), 2)
        self.assertEqual(len(grouped_getters['question']), 4)
        self.assertEqual(len(grouped_getters['personal']), 0)
        self.assertEqual(len(grouped_getters['extra']), 0)
    # }}}

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_getters_with_selected_questions(self):  # {{{
        questions_pks = [
            self.questions['text'].pk,
            self.questions['choices'].pk,
        ]
        columns = None
        answer_metadata = AnswerMetadata(self.survey.pk)
        answer_metadata.init_data()
        getters = list(answer_metadata.filter_getters(questions_pks, columns))

        grouped_getters = defaultdict(list)
        for getter in getters:
            grouped_getters[getter.group_name].append(getter)

        self.assertEqual(len(grouped_getters['base']), 2)
        self.assertEqual(len(grouped_getters['question']), 4)
        self.assertEqual(len(grouped_getters['personal']), 0)
        self.assertEqual(len(grouped_getters['extra']), 0)
    # }}}

    def test_filter_answers_by_pks(self):  # {{{
        answers_pks = sorted([self.answers[1].pk, self.answers[2].pk])
        answers = list(get_answers(survey=self.survey, answers_pks=answers_pks))
        self.assertEqual(len(answers), 2)
        result_pks = sorted(answer.answer_id for answer in answers)
        self.assertListEqual(result_pks, answers_pks)
    # }}}

    def test_filter_answers_by_under_limit(self):  # {{{
        answers_pks = sorted((answer.pk for answer in self.answers), reverse=True)
        answers = list(get_answers(survey=self.survey, limit=2))
        result_pks = sorted((answer.answer_id for answer in answers), reverse=True)
        self.assertEqual(len(result_pks), 2)
        self.assertListEqual(result_pks, answers_pks[:2])
    # }}}

    def test_filter_answers_by_over_limit(self):  # {{{
        answers_pks = sorted((answer.pk for answer in self.answers), reverse=True)
        answers = list(get_answers(survey=self.survey, limit=100))
        result_pks = sorted((answer.answer_id for answer in answers), reverse=True)
        self.assertEqual(len(result_pks), len(answers_pks))
        self.assertListEqual(result_pks, answers_pks)
    # }}}

    def test_filter_answers_by_zero_limit(self):  # {{{
        answers_pks = sorted((answer.pk for answer in self.answers), reverse=True)
        answers = list(get_answers(survey=self.survey, limit=0))
        result_pks = sorted((answer.answer_id for answer in answers), reverse=True)
        self.assertEqual(len(result_pks), len(answers_pks))
        self.assertListEqual(result_pks, answers_pks)
    # }}}

    def test_filter_answers_by_created_at(self):  # {{{
        started_at = timezone.now() - timedelta(days=6)

        answers = list(get_answers(survey=self.survey, started_at=started_at))
        self.assertEqual(len(answers), 2)
        result_pks = sorted(answer.answer_id for answer in answers)
        self.assertListEqual(result_pks, sorted([self.answers[1].pk, self.answers[2].pk]))
    # }}}

    def test_filter_answers_by_finished_at(self):  # {{{
        finished_at = timezone.now() - timedelta(days=4)

        answers = list(get_answers(survey=self.survey, finished_at=finished_at))
        self.assertEqual(len(answers), 2)
        result_pks = sorted(answer.answer_id for answer in answers)
        self.assertListEqual(result_pks, sorted([self.answers[0].pk, self.answers[1].pk]))
    # }}}

    def test_mixed_filter_answers(self):  # {{{
        started_at = timezone.now() - timedelta(days=6)
        finished_at = timezone.now() - timedelta(days=4)

        answers = list(get_answers(survey=self.survey, started_at=started_at, finished_at=finished_at))
        self.assertEqual(len(answers), 1)
        self.assertEqual(answers[0].answer_id, self.answers[1].pk)
    # }}}

    def test_should_return_answers_not_at_once(self):  # {{{
        some_date = self.answers[0].date_created.date()
        AnswerCountByDateFactory(survey=self.survey, created=some_date, count=12000)
        AnswerCountByDateFactory(survey=self.survey, created=some_date + timedelta(days=1), count=11000)
        answers = list(get_answers(survey=self.survey))
        self.assertEqual(len(answers), 3)
        self.assertEqual(answers[0].answer_id, self.answers[0].pk)
        self.assertEqual(answers[1].answer_id, self.answers[1].pk)
        self.assertEqual(answers[2].answer_id, self.answers[2].pk)
    # }}}

    def test_should_return_archived_answers_with_default_args(self):  # {{{
        self.survey.date_archived = self.survey.date_created + timedelta(days=15)
        self.survey.save()

        with patch('events.surveyme.export_answers_v2.get_answers_data') as mock_answers_data:
            with patch('events.surveyme.export_answers_v2.get_archived_data') as mock_archived_data:
                mock_answers_data.return_value = []
                mock_archived_data.return_value = []
                results = list(get_answers(self.survey))

        self.assertEqual(len(results), 0)
        mock_answers_data.assert_called_once_with(self.survey.pk, self.survey.date_archived, timezone.now(), None, None)
        mock_archived_data.assert_called_once_with(self.survey.pk, self.survey.date_created, self.survey.date_archived, None, None)
    # }}}

    def test_should_return_archived_answers_with_date_range(self):  # {{{
        self.survey.date_archived = self.survey.date_created + timedelta(days=15)
        self.survey.save()

        date_started = self.survey.date_created + timedelta(days=5)
        date_finished = self.survey.date_created + timedelta(days=25)

        with patch('events.surveyme.export_answers_v2.get_answers_data') as mock_answers_data:
            with patch('events.surveyme.export_answers_v2.get_archived_data') as mock_archived_data:
                mock_answers_data.return_value = []
                mock_archived_data.return_value = []
                results = list(get_answers(self.survey, date_started, date_finished))

        self.assertEqual(len(results), 0)
        mock_answers_data.assert_called_once_with(self.survey.pk, self.survey.date_archived, date_finished, None, None)
        mock_archived_data.assert_called_once_with(self.survey.pk, date_started, self.survey.date_archived, None, None)
    # }}}

    def test_should_return_archived_answers_with_limit(self):  # {{{
        self.survey.date_archived = self.survey.date_created + timedelta(days=15)
        self.survey.save()

        with patch('events.surveyme.export_answers_v2.get_answers_data') as mock_answers_data:
            with patch('events.surveyme.export_answers_v2.get_archived_data') as mock_archived_data:
                mock_answers_data.return_value = [
                    (3, '2020-04-15T09:18:00Z', '2020-04-15T09:18:00Z', {}, None),
                    (5, '2020-04-19T09:18:00Z', '2020-04-19T09:18:00Z', {}, None),
                    (4, '2020-04-18T09:18:00Z', '2020-04-18T09:18:00Z', {}, None),
                ]
                mock_archived_data.return_value = [
                    (1, '2020-03-27T09:18:00Z', '2020-03-27T09:18:00Z', {}, None),
                    (2, '2020-03-28T09:18:00Z', '2020-03-28T09:18:00Z', {}, None),
                ]
                results = list(get_answers(self.survey, limit=6))

        self.assertEqual(len(results), 5)
        mock_answers_data.assert_called_once_with(self.survey.pk, self.survey.date_archived, timezone.now(), None, 6)
        mock_archived_data.assert_called_once_with(self.survey.pk, self.survey.date_created, self.survey.date_archived, None, 3)
    # }}}

    def test_shouldnt_return_archived_answers_with_limit(self):  # {{{
        self.survey.date_archived = self.survey.date_created + timedelta(days=15)
        self.survey.save()

        with patch('events.surveyme.export_answers_v2.get_answers_data') as mock_answers_data:
            with patch('events.surveyme.export_answers_v2.get_archived_data') as mock_archived_data:
                mock_answers_data.return_value = [
                    (3, '2020-04-15T09:18:00Z', '2020-04-15T09:18:00Z', {}, None),
                    (5, '2020-04-19T09:18:00Z', '2020-04-19T09:18:00Z', {}, None),
                    (4, '2020-04-18T09:18:00Z', '2020-04-18T09:18:00Z', {}, None),
                    (1, '2020-03-27T09:18:00Z', '2020-03-27T09:18:00Z', {}, None),
                    (2, '2020-03-28T09:18:00Z', '2020-03-28T09:18:00Z', {}, None),
                ]
                mock_archived_data.return_value = []
                results = list(get_answers(self.survey, limit=5))

        self.assertEqual(len(results), 5)
        mock_answers_data.assert_called_once_with(self.survey.pk, self.survey.date_archived, timezone.now(), None, 5)
        mock_archived_data.assert_not_called()
    # }}}
# }}}


class TestPassportPersonalData(TestCase):  # {{{
    def test_should_return_correct_personal_data(self):
        userinfo_data = {  # {{{
            'users': [
                {
                    'id': '1101',
                    'login': 'user1101',
                    'dbfields': {
                        'account_info.fio.uid': 'Doe John',
                        'account_info.birth_date.uid': '2020-05-06',
                        'account_info.sex.uid': '1',
                    },
                    'address-list': [
                        {
                            'address': 'user1101@yandex.ru',
                            'default': True,
                        },
                    ],
                },
                {
                    'id': '1102',
                    'login': 'user1102',
                    'dbfields': {
                        'account_info.fio.uid': 'Jane',
                        'account_info.birth_date.uid': '',
                        'account_info.sex.uid': '2',
                    },
                    'address-list': [
                        {
                            'address': 'user1102@yandex.ru',
                            'default': True,
                        },
                    ],
                },
                {
                    'id': '1103',
                    'login': 'user1103',
                    'dbfields': {
                        'account_info.fio.uid': '',
                        'account_info.birth_date.uid': '',
                        'account_info.sex.uid': '',
                    },
                    'address-list': [
                        {
                            'address': 'user1103@yandex.ru',
                            'default': True,
                        },
                    ],
                },
            ],
        }  # }}}
        answers = [
            Answer(answer_id=1, created_at=None, updated_at=None, data={'uid': '1101'}, source_request=None),
            Answer(answer_id=2, created_at=None, updated_at=None, data={'uid': '1102'}, source_request=None),
            Answer(answer_id=3, created_at=None, updated_at=None, data={'uid': '1103'}, source_request=None),
        ]
        personal_data = PassportPersonalData()
        with patch('events.common_app.blackbox_requests.JsonBlackbox.userinfo', return_value=userinfo_data) as mock_userinfo:
            personal_data.prepare(answers)
            personal_data.prepare(answers)  # второй вызов не должен приводить к походу в ББ
        uids = set(it.data['uid'] for it in answers)
        mock_userinfo.assert_called_once_with(
            uid=','.join(uids),
            dbfields=[
                'accounts.login.uid',
                'account_info.fio.uid',
                'account_info.nickname.uid',
                'account_info.sex.uid',
                'account_info.email.uid',
                'account_info.birth_date.uid',
                'account_info.city.uid',
                'userinfo.lang.uid'
            ],
            emails='getdefault',
            userip='5.255.219.135',
        )

        pd = personal_data.get_value('1101')
        self.assertEqual(pd.get('param_name'), 'John')
        self.assertEqual(pd.get('param_surname'), 'Doe')
        self.assertEqual(pd.get('param_gender'), '1')
        self.assertEqual(pd.get('param_subscribed_email'), 'user1101@yandex.ru')
        self.assertEqual(pd.get('param_birthdate'), '2020-05-06')
        self.assertEqual(pd.get('yandex_username'), 'user1101')

        pd = personal_data.get_value('1102')
        self.assertEqual(pd.get('param_name'), 'Jane')
        self.assertEqual(pd.get('param_surname'), 'Jane')
        self.assertEqual(pd.get('param_gender'), '2')
        self.assertEqual(pd.get('param_subscribed_email'), 'user1102@yandex.ru')
        self.assertEqual(pd.get('param_birthdate'), '')
        self.assertEqual(pd.get('yandex_username'), 'user1102')

        pd = personal_data.get_value('1103')
        self.assertEqual(pd.get('param_name'), '')
        self.assertEqual(pd.get('param_surname'), '')
        self.assertEqual(pd.get('param_gender'), '')
        self.assertEqual(pd.get('param_subscribed_email'), 'user1103@yandex.ru')
        self.assertEqual(pd.get('param_birthdate'), '')
        self.assertEqual(pd.get('yandex_username'), 'user1103')
# }}}


class TestFilesGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        self.getter = FilesGetter(question=self.question, group_index=None)

    def test_should_return_valid_intranet_file_link(self):
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, 'https://forms.yandex-team.ru/files?path=%2F100%2F12345')

    @override_settings(
        IS_BUSINESS_SITE=True,
        FRONTEND_DOMAIN='forms.yandex{tld}',
        FILE_PATH_PREFIX='/u',
    )
    def test_should_return_valid_business_file_link(self):
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, 'https://forms.yandex.ru/u/files?path=%2F100%2F12345')

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        self.assertEqual(header, self.question.label)

    def test_should_upload_file_content_to_ydisk(self):
        uploader = DiskUploader('321', '1')
        self.getter = FilesGetter(question=self.question, group_index=None,
                                  uploader=uploader, upload_files=True)
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }
        content = b'some binary content'
        file_url = 'http://somefileurl'

        with patch('events.surveyme.export_answers_v2.MdsDownloader.download_file') as mock_download_file:
            with patch.object(uploader, 'upload_file') as mock_upload_file:
                mock_download_file.return_value = content
                mock_upload_file.return_value = file_url
                value = self.getter.get_value(value_data)

        self.assertEqual(value, file_url)
        mock_download_file.assert_called_once_with('/100/12345')
        mock_upload_file.assert_called_once_with('12345', content)

    def test_shouldnt_upload_empty_file_to_ydisk(self):
        uploader = DiskUploader('321', '1')
        self.getter = FilesGetter(question=self.question, group_index=None,
                                  uploader=uploader, upload_files=True)
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }
        content = b''
        file_url = 'http://somefileurl'

        with patch('events.surveyme.export_answers_v2.MdsDownloader.download_file') as mock_download_file:
            with patch.object(uploader, 'upload_file') as mock_upload_file:
                mock_download_file.return_value = content
                mock_upload_file.return_value = file_url
                value = self.getter.get_value(value_data)

        self.assertEqual(value, 'https://forms.yandex-team.ru/files?path=%2F100%2F12345')
        mock_download_file.assert_called_once_with('/100/12345')
        mock_upload_file.assert_not_called()

    def test_shouldnt_fail_if_file_does_not_exist(self):
        from django_mds.client import APIError
        uploader = DiskUploader('321', '1')
        self.getter = FilesGetter(question=self.question, group_index=None,
                                  uploader=uploader, upload_files=True)
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }

        with patch('events.surveyme.export_answers_v2.MdsClient.get') as mock_get:
            mock_get.side_effect = APIError()
            with patch.object(uploader, 'upload_file') as mock_upload_file:
                value = self.getter.get_value(value_data)

        self.assertEqual(value, 'https://forms.yandex-team.ru/files?path=%2F100%2F12345')
        mock_get.assert_called_once_with('/100/12345')
        mock_upload_file.assert_not_called()

    def test_shouldnt_upload_file_to_ydisk(self):
        uploader = DiskUploader('321', '1')
        self.getter = FilesGetter(question=self.question, group_index=None,
                                  uploader=uploader, upload_files=False)
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }

        with patch('events.surveyme.export_answers_v2.MdsDownloader.download_file') as mock_download_file:
            with patch.object(uploader, 'upload_file') as mock_upload_file:
                value = self.getter.get_value(value_data)

        self.assertEqual(value, 'https://forms.yandex-team.ru/files?path=%2F100%2F12345')
        mock_download_file.assert_not_called()
        mock_upload_file.assert_not_called()

    def test_shouldnt_do_something_wierd_with_mds(self):
        uploader = MdsUploader()
        self.getter = FilesGetter(question=self.question, group_index=None,
                                  uploader=uploader, upload_files=True)
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }

        with patch('events.surveyme.export_answers_v2.MdsDownloader.download_file') as mock_download_file:
            value = self.getter.get_value(value_data)

        self.assertEqual(value, 'https://forms.yandex-team.ru/files?path=%2F100%2F12345')
        mock_download_file.assert_not_called()

    def test_shouldnt_do_something_wierd_with_mds_and_tld(self):
        uploader = MdsUploader()
        self.getter = FilesGetter(question=self.question, group_index=None,
                                  uploader=uploader, upload_files=True, tld='.com')
        value_data = {
            self.question.pk: [
                '/100/12345',
            ]
        }

        with patch('events.surveyme.export_answers_v2.MdsDownloader.download_file') as mock_download_file:
            value = self.getter.get_value(value_data)

        self.assertEqual(value, 'https://forms.yandex-team.com/files?path=%2F100%2F12345')
        mock_download_file.assert_not_called()
# }}}


class TestChoiceGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
        )
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='three'),
        ]
        choices = {
            str(choice.pk): choice.label
            for choice in self.choices
        }
        self.getter = ChoiceGetter(question=self.question, group_index=None, choices=choices)

    def test_should_return_field_label(self):
        value_data = {
            self.question.pk: {
                str(self.choices[0].pk),
            }
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, self.choices[0].label)

    def test_should_return_any_field_label(self):
        value_data = {
            self.question.pk: {
                str(self.choices[1].pk),
                str(self.choices[0].pk),
            }
        }
        value = self.getter.get_value(value_data)
        self.assertIn(value, [self.choices[0].label, self.choices[1].label])

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        self.assertEqual(header, self.question.label)
# }}}


class TestMultipleChoiceGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
            param_is_allow_multiple_choice=True,
        )
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='three'),
        ]
        choice = self.choices[1]
        self.getter = MultipleChoiceGetter(question=self.question, group_index=None, pk=str(choice.pk), label=choice.label)

    def test_should_return_none_for_wrong_pk(self):
        value_data = {
            self.question.pk: {
                str(self.choices[0].pk),
            }
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_correct_field_label(self):
        value_data = {
            self.question.pk: {
                str(self.choices[1].pk),
                str(self.choices[0].pk),
            }
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, self.choices[1].label)

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        expected = make_label(self.question.label, self.choices[1].label)
        self.assertEqual(header, expected)
# }}}


class TestTitleGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_title_choice',
        )
        self.rows = [
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='row1', type='row'),
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='row2', type='row'),
        ]
        self.cols = [
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='1', type='column'),
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='2', type='column'),
        ]
        cols = {
            str(col.pk): col.label
            for col in self.cols
        }
        row = self.rows[1]
        self.getter = TitleGetter(question=self.question, group_index=None, row_pk=str(row.pk), row_label=row.label, cols=cols)

    def test_should_return_for_wrong_row_pk(self):
        value_data = {
            self.question.pk: {
                str(self.rows[0].pk): str(self.cols[0].pk),
            }
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_correct_field_label(self):
        value_data = {
            self.question.pk: {
                str(self.rows[0].pk): str(self.cols[1].pk),
                str(self.rows[1].pk): str(self.cols[0].pk),
            }
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, self.cols[0].label)

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        expected = make_label(self.question.label, self.rows[1].label)
        self.assertEqual(header, expected)
# }}}


class TestBooleanGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_boolean'),
        )
        self.getter = BooleanGetter(question=self.question, group_index=None)

    def test_should_return_yes(self):
        value_data = {
            self.question.pk: True,
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, _('Yes'))

    def test_should_return_no(self):
        value_data = {
            self.question.pk: False,
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, _('No'))

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        self.assertEqual(header, self.question.label)
# }}}


class TestGroupedBooleanGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.group = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_boolean'),
            group_id=self.group.pk,
            position=1,
        )
        self.group_index = 1
        self.getter = BooleanGetter(question=self.question, group_index=self.group_index)

    def test_should_return_yes(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: True,
                },
            ],
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, _('Yes'))

    def test_should_return_no(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: False,
                },
            ],
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, _('No'))

    def test_should_return_none_for_empty_group_item(self):
        value_data = {
            self.group.pk: [
                {
                    self.question.pk: False,
                },
                {},
            ],
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        expected = '%s [%s]' % (self.question.label, self.group_index + 1)
        self.assertEqual(header, expected)
# }}}


class TestGroupedChoiceGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.group = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
            group_id=self.group.pk,
            position=1,
        )
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='three'),
        ]
        choices = {
            str(choice.pk): choice.label
            for choice in self.choices
        }
        self.group_index = 1
        self.getter = ChoiceGetter(question=self.question, group_index=self.group_index, choices=choices)

    def test_should_return_correct_field_label(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: {
                        str(self.choices[0].pk),
                    }
                },
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, self.choices[0].label)

    def test_should_return_any_field_label(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: {
                        str(self.choices[1].pk),
                        str(self.choices[0].pk),
                    }
                },
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertIn(value, [self.choices[0].label, self.choices[1].label])

    def test_should_return_none_for_empty_group_item(self):
        value_data = {
            self.group.pk: [
                {
                    self.question.pk: {
                        str(self.choices[0].pk),
                    }
                },
                {},
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        expected = '%s [%s]' % (self.question.label, self.group_index + 1)
        self.assertEqual(header, expected)
# }}}


class TestGroupedMultipleChoiceGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.group = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
            param_is_allow_multiple_choice=True,
            group_id=self.group.pk,
            position=1,
        )
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='three'),
        ]
        self.group_index = 1
        choice = self.choices[1]
        self.getter = MultipleChoiceGetter(question=self.question, group_index=self.group_index, pk=str(choice.pk), label=choice.label)

    def test_should_return_none_for_wrong_pk(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: {
                        str(self.choices[0].pk),
                    }
                },
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_correct_field_label(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: {
                        str(self.choices[1].pk),
                        str(self.choices[0].pk),
                    }
                },
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, self.choices[1].label)

    def test_should_return_for_empty_group_item(self):
        value_data = {
            self.group.pk: [
                {
                    self.question.pk: {
                        str(self.choices[1].pk),
                        str(self.choices[0].pk),
                    }
                },
                {},
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        expected = '%s [%s]' % (make_label(self.question.label, self.choices[1].label), self.group_index + 1)
        self.assertEqual(header, expected)
# }}}


class TestGroupedTitleGetter(TestCase):  # {{{
    fixtures = ['initial_data.json']

    def setUp(self):
        self.group = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_title_choice',
            group_id=self.group.pk,
            position=1,
        )
        self.rows = [
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='row1', type='row'),
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='row2', type='row'),
        ]
        self.cols = [
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='1', type='column'),
            SurveyQuestionMatrixTitleFactory(survey_question=self.question, label='2', type='column'),
        ]
        cols = {
            str(col.pk): col.label
            for col in self.cols
        }
        row = self.rows[1]
        self.group_index = 1
        self.getter = TitleGetter(question=self.question, group_index=self.group_index, row_pk=str(row.pk), row_label=row.label, cols=cols)

    def test_should_return_none_for_wrong_pk(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: {
                        str(self.rows[0].pk): str(self.cols[0].pk),
                    }
                },
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_correct_field_label(self):
        value_data = {
            self.group.pk: [
                {},
                {
                    self.question.pk: {
                        str(self.rows[0].pk): str(self.cols[1].pk),
                        str(self.rows[1].pk): str(self.cols[0].pk),
                    }
                },
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertEqual(value, self.cols[0].label)

    def test_should_return_none_for_empty_group_item(self):
        value_data = {
            self.group.pk: [
                {
                    self.question.pk: {
                        str(self.rows[0].pk): str(self.cols[1].pk),
                        str(self.rows[1].pk): str(self.cols[0].pk),
                    }
                },
                {},
            ]
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_should_return_none_for_empty_answer(self):
        value_data = {
        }
        value = self.getter.get_value(value_data)
        self.assertIsNone(value)

    def test_header(self):
        header = self.getter.get_header()
        expected = '%s [%s]' % (make_label(self.question.label, self.rows[1].label), self.group_index + 1)
        self.assertEqual(header, expected)
# }}}


@freeze_time('2020-05-15')
class TestFileName(TestCase):  # {{{
    def test_should_return_letters_and_digits(self):
        survey = SurveyFactory(
            name='hello.привет-123'
        )
        answer_metadata = AnswerMetadata(survey.pk)
        answer_metadata.init_data()
        self.assertEqual(answer_metadata.get_file_name('csv'), '2020-05-15 hello.привет-123.csv')

    def test_shouldn_return_invalid_symbols(self):
        survey = SurveyFactory(
            name='''ё!"№;%:?*()!@#$%^&*()_+{}[]\\|:;"'?/.><,123'''
        )
        answer_metadata = AnswerMetadata(survey.pk)
        answer_metadata.init_data()
        self.assertEqual(answer_metadata.get_file_name('csv'), '2020-05-15 ё_.123.csv')
# }}}


class TestUploadFactory(TestCase):
    def test_should_return_mds_uploader(self):
        uploader = get_uploader('mds', '123', '1')
        self.assertTrue(isinstance(uploader, MdsUploader))

    def test_should_return_disk_uploader_for_intranet(self):
        with patch('events.surveyme.export_answers_v2.get_external_uid') as mock_external_uid:
            with patch.object(DiskClient, 'isdir') as mock_isdir:
                mock_isdir.return_value = True
                mock_external_uid.return_value = '321'
                uploader = get_uploader('disk', '123', '1')

        self.assertTrue(isinstance(uploader, DiskUploader))
        self.assertEqual(uploader.client.user_uid, '321')
        mock_external_uid.assert_called_once_with('123')
        mock_isdir.assert_called_once_with(settings.DISK_NDA_FOLDER)

    def test_should_return_disk_uploader_without_linked_external_uid(self):
        with patch('events.surveyme.export_answers_v2.get_external_uid') as mock_external_uid:
            mock_external_uid.return_value = None
            uploader = get_uploader('disk', '123', '1')

        self.assertTrue(isinstance(uploader, MdsUploader))
        mock_external_uid.assert_called_once_with('123')

    def test_shouldnt_return_disk_uploader_without_nda_folder(self):
        with patch('events.surveyme.export_answers_v2.get_external_uid') as mock_external_uid:
            with patch.object(DiskClient, 'isdir') as mock_isdir:
                mock_isdir.return_value = False
                mock_external_uid.return_value = '321'
                uploader = get_uploader('disk', '123', '1')

        self.assertTrue(isinstance(uploader, MdsUploader))
        mock_external_uid.assert_called_once_with('123')
        mock_isdir.assert_called_once_with(settings.DISK_NDA_FOLDER)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_disk_uploader_for_biz(self):
        with patch('events.surveyme.export_answers_v2.get_external_uid') as mock_external_uid:
            uploader = get_uploader('disk', '123', '1')

        self.assertTrue(isinstance(uploader, DiskUploader))
        self.assertEqual(uploader.client.user_uid, '123')
        mock_external_uid.assert_not_called()

    def test_shouldnt_return_any_uploader(self):
        uploader = get_uploader('notexist', None, '1')
        self.assertIsNone(uploader)


class TestMdsUploader(TestCase):
    def test_should_upload_content(self):
        uploader = MdsUploader()
        with patch.object(uploader.client, 'upload') as mock_upload:
            mock_upload.return_value = '/123/4321'
            content = b'some binary data'
            response = uploader.upload_report('newform.xlsx', XlsxFormatter.content_type, content)

        self.assertIsNotNone(response)
        self.assertEqual(response['file_name'], 'newform.xlsx')
        self.assertEqual(response['path'], '/123/4321')
        self.assertEqual(response['content_type'], XlsxFormatter.content_type)
        self.assertEqual(response['status_code'], 200)

        mock_upload.assert_called_once_with(content, ANY, expire=MdsUploader.expire)


class TestDiskUploader(TestCase):
    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_upload_xlsx_report_content(self):
        uploader = DiskUploader('321', '1')

        folder_path = os.path.join(settings.DISK_FOLDER_PATH, '1')
        file_name = 'newform.xlsx'
        file_path = os.path.join(folder_path, file_name)
        content = b'some binary data'

        with patch.object(uploader.client, 'mkdir') as mock_mkdir:
            with patch.object(uploader.client, 'check_file_name') as mock_check_file_name:
                with patch.object(uploader.client, 'upload') as mock_upload:
                    with patch.object(uploader.client, 'get_edit_url') as mock_edit_url:
                        mock_check_file_name.return_value = (file_path, False)
                        mock_upload.return_value = True
                        mock_edit_url.return_value = 'http:///somesecreturl'
                        response = uploader.upload_report(file_name, XlsxFormatter.content_type, content)

        self.assertIsNotNone(response)
        self.assertEqual(response['file_name'], file_name)
        self.assertEqual(response['path'], 'http:///somesecreturl')
        self.assertEqual(response['content_type'], XlsxFormatter.content_type)
        self.assertEqual(response['status_code'], 302)

        mock_mkdir.assert_called_once_with(folder_path)
        mock_check_file_name.assert_called_once_with(file_path)
        mock_edit_url.assert_called_once_with(file_path)
        mock_upload.assert_called_once_with(file_path, content)

    def test_should_upload_csv_report_content(self):
        uploader = DiskUploader('321', '1')
        folder_path = os.path.join(settings.DISK_FOLDER_PATH, '1')
        file_name = 'newform.csv'
        file_path = os.path.join(folder_path, file_name)
        content = b'some binary data'

        with patch.object(uploader.client, 'mkdir') as mock_mkdir:
            with patch.object(uploader.client, 'check_file_name') as mock_check_file_name:
                with patch.object(uploader.client, 'upload') as mock_upload:
                    with patch.object(uploader.client, 'get_edit_url') as mock_edit_url:
                        mock_check_file_name.return_value = (file_path, False)
                        mock_upload.return_value = True
                        response = uploader.upload_report(file_name, CsvFormatter.content_type, content)

        self.assertIsNotNone(response)
        self.assertEqual(response['file_name'], file_name)
        self.assertEqual(response['path'], 'https://%s/client/disk%s' % (settings.DISK_FRONTEND_HOST, file_path))
        self.assertEqual(response['content_type'], CsvFormatter.content_type)
        self.assertEqual(response['status_code'], 302)

        mock_mkdir.assert_called_once_with(folder_path)
        mock_check_file_name.assert_called_once_with(file_path)
        mock_edit_url.assert_not_called()
        mock_upload.assert_called_once_with(file_path, content)

    def test_should_upload_new_file_content(self):
        uploader = DiskUploader('321', '1')
        folder_path = os.path.join(settings.DISK_FOLDER_PATH, '1', 'Files')
        file_name = 'textfile.txt'
        file_path = os.path.join(folder_path, file_name)
        content = 'some text data'

        with patch.object(uploader.client, 'mkdir') as mock_mkdir:
            with patch.object(uploader.client, 'listdir') as mock_listdir:
                with patch.object(uploader.client, 'upload') as mock_upload:
                    mock_listdir.return_value = []
                    mock_upload.return_value = True
                    response = uploader.upload_file(file_name, content)

        self.assertEqual(response, 'https://%s/client/disk%s' % (settings.DISK_FRONTEND_HOST, file_path))

        mock_mkdir.assert_called_once_with(folder_path)
        mock_listdir.assert_called_once_with(folder_path)
        mock_upload.assert_called_once_with(file_path, content)

    def test_shouldnt_upload_existing_file_content(self):
        uploader = DiskUploader('321', '1')
        folder_path = os.path.join(settings.DISK_FOLDER_PATH, '1', 'Files')
        file_name = 'textfile.txt'
        file_path = os.path.join(folder_path, file_name)
        content = 'some text data'

        with patch.object(uploader.client, 'mkdir') as mock_mkdir:
            with patch.object(uploader.client, 'listdir') as mock_listdir:
                with patch.object(uploader.client, 'upload') as mock_upload:
                    mock_listdir.return_value = ['textfile.txt']
                    mock_upload.return_value = True
                    response = uploader.upload_file(file_name, content)

        self.assertEqual(response, 'https://%s/client/disk%s' % (settings.DISK_FRONTEND_HOST, file_path))

        mock_mkdir.assert_called_once_with(folder_path)
        mock_listdir.assert_called_once_with(folder_path)
        mock_upload.assert_not_called()


class TestExportWithFiles(TestCase):
    fixtures = ['initial_data.json']

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_throw_upload_error(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        ProfileSurveyAnswerFactory(
            survey=survey,
            data={
                'data': [{
                    'value': [{
                        'path': '/files/textfile.txt',
                    }],
                    'question': question.get_answer_info(),
                }],
            },
        )

        folder_path = os.path.join(settings.DISK_FOLDER_PATH, '1', 'Files')
        file_name = 'textfile.txt'
        file_path = os.path.join(folder_path, file_name)
        content = 'some text data'

        with patch.object(MdsDownloader, 'download_file') as mock_download_file:
            with patch.object(DiskClient, 'mkdir') as mock_mkdir:
                with patch.object(DiskClient, 'listdir') as mock_listdir:
                    with patch.object(DiskClient, 'upload') as mock_upload:
                        mock_download_file.return_value = content
                        mock_listdir.return_value = []
                        mock_upload.side_effect = DiskUploadError(507, 'Not enough space')
                        try:
                            export_answers(survey_id=survey.pk, format='csv', upload='disk', upload_files=True, user_uid='321')
                            self.fail(1)
                        except ExportError as e:
                            self.assertEqual(str(e), 'Not enough space')
                            self.assertTrue(isinstance(e.__cause__, DiskUploadError))

        mock_mkdir.assert_called_once_with(folder_path)
        mock_listdir.assert_called_once_with(folder_path)
        mock_upload.assert_called_once_with(file_path, content)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_export_answers(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        ProfileSurveyAnswerFactory(
            survey=survey,
            data={
                'data': [{
                    'value': [{
                        'path': '/files/textfile.txt',
                    }],
                    'question': question.get_answer_info(),
                }],
            },
        )

        folder_path = os.path.join(settings.DISK_FOLDER_PATH, '1')
        files_path = os.path.join(folder_path, 'Files')
        file_name = 'textfile.txt'
        file_path = os.path.join(files_path, file_name)
        content = 'some text data'

        with patch.object(MdsDownloader, 'download_file') as mock_download_file:
            with patch.object(DiskClient, 'mkdir') as mock_mkdir:
                with patch.object(DiskClient, 'listdir') as mock_listdir:
                    with patch.object(DiskClient, 'upload') as mock_upload:
                        with patch.object(DiskClient, 'isfile') as mock_isfile:
                            mock_download_file.return_value = content
                            mock_listdir.return_value = []
                            mock_upload.return_value = True
                            mock_isfile.return_value = False
                            export_answers(survey_id=survey.pk, format='csv', upload='disk', upload_files=True, user_uid='321')

        mock_mkdir.assert_has_calls([call(files_path), call(folder_path)])
        mock_listdir.assert_has_calls([call(files_path)])
        mock_upload.assert_has_calls([call(file_path, content), call(ANY, ANY)])


class TestExportAnswerDataWithNullableGroup(TestCase):
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
        QuestionGroupDepthFactory(
            survey=self.survey,
            question=self.group_question,
            depth=2,
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

        with patch.object(MdsUploader, 'upload_report') as mock_upload_report:
            mock_upload_report.return_value = {
                'file_name': 'report.csv',
                'path': '/report.csv',
                'content_type': 'text/csv',
                'status_code': 200,
            }
            export_answers(survey_id=self.survey.pk, format='csv', upload='mds')

        mock_upload_report.assert_called_once()
        file_name, mime_type, data = mock_upload_report.call_args[0]
        result = data.decode().strip('\r\n').split('\r\n')
        self.assertEqual(len(result), 2)
        data = result[1].split(',')
        self.assertEqual(len(data), 4)
        self.assertEqual(data[0], str(answer.pk))
        self.assertTrue(data[1] != '')
        self.assertEqual(data[2], '1')
        self.assertEqual(data[3], '2')

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

        with patch.object(MdsUploader, 'upload_report') as mock_upload_report:
            mock_upload_report.return_value = {
                'file_name': 'report.csv',
                'path': '/report.csv',
                'content_type': 'text/csv',
                'status_code': 200,
            }
            export_answers(survey_id=self.survey.pk, format='csv', upload='mds')

        mock_upload_report.assert_called_once()
        file_name, mime_type, data = mock_upload_report.call_args[0]
        result = data.decode().strip('\r\n').split('\r\n')
        self.assertEqual(len(result), 2)
        data = result[1].split(',')
        self.assertEqual(len(data), 4)
        self.assertEqual(data[0], str(answer.pk))
        self.assertTrue(data[1] != '')
        self.assertEqual(data[2], '1')
        self.assertEqual(data[3], '2')

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

        with patch.object(MdsUploader, 'upload_report') as mock_upload_report:
            mock_upload_report.return_value = {
                'file_name': 'report.csv',
                'path': '/report.csv',
                'content_type': 'text/csv',
                'status_code': 200,
            }
            export_answers(survey_id=self.survey.pk, format='csv', upload='mds')

        mock_upload_report.assert_called_once()
        file_name, mime_type, data = mock_upload_report.call_args[0]
        result = data.decode().strip('\r\n').split('\r\n')
        self.assertEqual(len(result), 2)
        data = result[1].split(',')
        self.assertEqual(len(data), 4)
        self.assertEqual(data[0], str(answer.pk))
        self.assertTrue(data[1] != '')
        self.assertEqual(data[2], '1')
        self.assertEqual(data[3], '2')


class TestDirPersonalData(TestCase):  # {{{
    def test_should_return_correct_personal_data(self):
        userinfo_data = [  # {{{
            {
                'id': 1101,
                'cloud_uid': 'abcc',
                'nickname': 'user1101',
                'name': {
                    'first': 'John',
                    'last': 'Doe',
                },
                'birthday': '2020-05-06',
                'gender': 'male',
                'contacts': [
                    {
                        'type': 'email',
                        'value': 'user1101@yandex.ru',
                        'main': True,
                    },
                ],
            },
            {
                'id': 1102,
                'cloud_uid': 'abcd',
                'nickname': 'user1102',
                'name': {
                    'first': 'Jane',
                    'last': '',
                },
                'birthday': None,
                'gender': 'female',
                'contacts': [
                    {
                        'type': 'email',
                        'value': 'user1102@yandex.ru',
                        'main': True,
                    },
                ],
            },
            {
                'id': 1103,
                'cloud_uid': 'abce',
                'nickname': 'user1103',
                'name': {
                    'first': '',
                    'last': '',
                },
                'birthday': None,
                'gender': None,
                'contacts': [
                    {
                        'type': 'email',
                        'value': 'user1103@yandex.ru',
                        'main': True,
                    },
                ],
            },
        ]  # }}}
        organization = {'id': 123, 'organization_type': 'general'}
        with (
            patch.object(DirectoryClient, 'get_organization', return_value=organization) as mock_organization,
            patch.object(DirectoryClient, 'get_users', return_value=userinfo_data) as mock_userinfo,
        ):
            personal_data = DirPersonalData('123')
        mock_userinfo.assert_called_once()
        mock_organization.assert_called_once_with('123', fields='organization_type')

        pd = personal_data.get_value('1101')
        self.assertEqual(pd.get('param_name'), 'John')
        self.assertEqual(pd.get('param_surname'), 'Doe')
        self.assertEqual(pd.get('param_gender'), '1')
        self.assertEqual(pd.get('param_subscribed_email'), 'user1101@yandex.ru')
        self.assertEqual(pd.get('param_birthdate'), '2020-05-06')
        self.assertEqual(pd.get('yandex_username'), 'user1101')

        pd = personal_data.get_value('abcd')
        self.assertEqual(pd.get('param_name'), 'Jane')
        self.assertEqual(pd.get('param_surname'), '')
        self.assertEqual(pd.get('param_gender'), '2')
        self.assertEqual(pd.get('param_subscribed_email'), 'user1102@yandex.ru')
        self.assertEqual(pd.get('param_birthdate'), None)
        self.assertEqual(pd.get('yandex_username'), 'user1102')

        pd = personal_data.get_value('1103')
        self.assertEqual(pd.get('param_name'), '')
        self.assertEqual(pd.get('param_surname'), '')
        self.assertEqual(pd.get('param_gender'), None)
        self.assertEqual(pd.get('param_subscribed_email'), 'user1103@yandex.ru')
        self.assertEqual(pd.get('param_birthdate'), None)
        self.assertEqual(pd.get('yandex_username'), 'user1103')
# }}}
