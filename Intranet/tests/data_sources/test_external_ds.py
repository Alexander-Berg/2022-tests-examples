# -*- coding: utf-8 -*-
import responses

from datetime import timedelta
from django.test import TestCase, override_settings
from django.utils import timezone
from unittest.mock import patch

from events.common_app.helpers import override_cache_settings
from events.data_sources.factories import TableMetaFactory, TableRowFactory
from events.data_sources.models import TableRow, TableMeta
from events.data_sources.sources.external_table.wiki_table import WikiDataSource
from events.data_sources.sources.external_table.yt_table import YTDataSource
from events.data_sources.tasks import (
    sync_external_table_with_db,
    sync_external_tables_for_all_questions,
    sync_yt_table,
)
from events.data_sources.utils import get_table_id
from events.surveyme.factories import SurveyFactory, SurveyQuestionFactory
from events.surveyme.models import AnswerType


class TestYTDataSource(TestCase):
    def setUp(self):
        cluster_name = 'hahn'
        table_path = '//tmp/mytable'
        self.table_id = get_table_id(cluster_name, table_path)
        self.filter_data = {
            'free_url': f'https://yt.yandex-team.ru/{cluster_name}/navigation?path={table_path}',
        }
        table_names = [
            ('choices 1', ''),
            ('choice 2', ''),
            ('smth 3', 'Что-то'),
            ('test 4', 'Тест'),
        ]
        self.table_data = [
            TableRowFactory(
                table_identifier=self.table_id,
                source_id=str(i),
                text=text,
                display_text=display_text,
            )
            for i, (text, display_text) in enumerate(table_names, start=1)
        ]

    def test_queryset_should_return_data_from_table(self):
        result = YTDataSource().get_filtered_queryset(filter_data=self.filter_data)
        expected = [
            {
                'id': row.pk,
                'name': row.text,
                'display_name': row.display_text,
            }
            for row in TableRow.objects.filter(table_identifier=self.table_id).order_by('text')
        ]
        self.assertListEqual(result, expected)

    def test_queryset_should_suggest_correct(self):
        self.filter_data['suggest'] = 'Hoice'

        result = YTDataSource().get_filtered_queryset(filter_data=self.filter_data)
        expected = [
            {'id': 2, 'name': 'choice 2', 'display_name': ''},
            {'id': 1, 'name': 'choices 1', 'display_name': ''},
        ]
        self.assertListEqual(result, expected)

    def test_should_filter_by_question_correct(self):
        self.table_data[0].filter_by = '10'
        self.table_data[1].filter_by = '4'
        self.table_data[2].filter_by = '21'
        self.table_data[3].filter_by = '4'

        for it in self.table_data:
            it.save()

        parent = TableRowFactory(table_identifier='smth', source_id='4', text='smth parent')
        self.filter_data['yt_table_source'] = parent.pk

        result = YTDataSource().get_filtered_queryset(filter_data=self.filter_data)
        expected = [
            {'id': 2, 'name': 'choice 2', 'display_name': ''},
            {'id': 4, 'name': 'test 4', 'display_name': 'Тест'},
        ]
        self.assertListEqual(result, expected)

    def test_should_filter_by_id(self):
        self.filter_data['id'] = str(self.table_data[0].pk)

        result = YTDataSource().get_filtered_queryset(filter_data=self.filter_data)
        expected = [
            {'id': 1, 'name': 'choices 1', 'display_name': ''},
        ]
        self.assertListEqual(result, expected)

    def test_serializer(self):
        serializer = YTDataSource.serializer_class(
            YTDataSource().get_filtered_queryset(filter_data=self.filter_data),
            many=True
        )
        result = serializer.data
        expected = [
            {'id': str(item.pk), 'text': item.display_text or item.text}
            for item in TableRow.objects.filter(table_identifier=self.table_id).order_by('text')
        ]
        self.assertListEqual(result, expected)

    def test_master_detail_suggest(self):
        city_rows = [
            TableRowFactory(table_identifier='//city_hahn', source_id='10', text='Moscow'),
            TableRowFactory(table_identifier='//city_hahn', source_id='20', text='Sankt-Petersburg'),
            TableRowFactory(table_identifier='//city_hahn', source_id='30', text='Ekaterinburg'),
        ]
        street_rows = [
            TableRowFactory(table_identifier='//street_hahn', source_id='11', text='Arbat', filter_by='10'),
            TableRowFactory(table_identifier='//street_hahn', source_id='12', text='Tverskaya', filter_by='10'),
            TableRowFactory(table_identifier='//street_hahn', source_id='21', text='Nevskiy', filter_by='20'),
            TableRowFactory(table_identifier='//street_hahn', source_id='22', text='Dvortsovaya', filter_by='20'),
        ]

        params = {
            'free_url': 'https://yt.yandex-team.ru/hahn/navigation?path=//street',
            'yt_table_source': city_rows[0].pk,
            'suggest': '',
        }
        response = self.client.get('/v1/data-source/yt-table-source/', params)
        self.assertEqual(response.status_code, 200)
        results = {
            it['id']: it['text']
            for it in response.data['results']
        }
        self.assertEqual(len(results), 2)
        self.assertEqual(results, {
            str(street_rows[0].pk): street_rows[0].text,
            str(street_rows[1].pk): street_rows[1].text,
        })

        params = {
            'free_url': 'https://yt.yandex-team.ru/hahn/navigation?path=//street',
            'yt_table_source': city_rows[1].pk,
            'suggest': 'dvor',
        }
        response = self.client.get('/v1/data-source/yt-table-source/', params)
        self.assertEqual(response.status_code, 200)
        results = {
            it['id']: it['text']
            for it in response.data['results']
        }
        self.assertEqual(len(results), 1)
        self.assertEqual(results, {
            str(street_rows[3].pk): street_rows[3].text,
        })

        params = {
            'free_url': 'https://yt.yandex-team.ru/hahn/navigation?path=//street',
            'yt_table_source': city_rows[2].pk,
            'suggest': 'lenina',
        }
        city_rows[2].delete()
        response = self.client.get('/v1/data-source/yt-table-source/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 0)


@override_cache_settings()
class TestWikiDataSource(TestCase):

    def setUp(self):
        self.filter_data = {
            'free_url': 'https://wiki.test.yandex-team.ru/users/smosker/testgrid/',
        }
        self.supertag = 'users/smosker/testgrid'

    def get_grid_data_success(self):  # {{{
        return {
            'data': {
                'structure': {
                    'done': False,
                    'fields': [
                        {
                            'name': '101',
                            'title': 'id',
                            'required': False,
                            'type': 'string',
                        },
                        {
                            'name': '100',
                            'title': 'name',
                            'required': False,
                            'type': 'string',
                        }
                    ],
                    'title': 'testgrid',
                    'sorting': [],
                },
                'rows': [
                    [
                        {
                            'raw': '1',
                            'row_id': '1',
                            '__key__': '101',
                        },
                        {
                            'raw': 'Американская акита',
                            'row_id': '1',
                            '__key__': '100',
                        }
                    ],
                    [
                        {
                            'raw': '2',
                            'row_id': '2',
                            '__key__': '101',
                        },
                        {
                            'raw':  'Американская эскимосская собака',
                            'row_id': '2',
                            '__key__': '100',
                        }
                    ],
                    [
                        {
                            'raw': '3',
                            'row_id': '3',
                            '__key__': '101',
                        },
                        {
                            'raw':  'Хаски 2',
                            'row_id': '3',
                            '__key__': '100',
                        }
                    ],
                ],
            },
        }  # }}}

    @responses.activate
    def test_queryset_should_return_data_from_table(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/testgrid/.grid'
        responses.add(responses.GET, url, json=self.get_grid_data_success())

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        expected = [
            {'id': '1', 'name': 'Американская акита'},
            {'id': '2', 'name': 'Американская эскимосская собака'},
            {'id': '3', 'name': 'Хаски 2'},
        ]
        self.assertListEqual(result, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_queryset_should_suggest_correct(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/testgrid/.grid'
        responses.add(responses.GET, url, json=self.get_grid_data_success())
        self.filter_data['suggest'] = 'Американская'

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        expected = [
            {'id': '1', 'name': 'Американская акита'},
            {'id': '2', 'name': 'Американская эскимосская собака'},
        ]
        self.assertListEqual(result, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_queryset_should_suggest_correct_biz(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/testgrid/.grid'
        responses.add(responses.GET, url, json=self.get_grid_data_success())
        self.filter_data['suggest'] = 'собака'
        self.filter_data['dir_id'] = '789'

        with patch('events.common_app.wiki.get_robot_wiki', return_value=456) as mock_get_robot_wiki:
            result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        mock_get_robot_wiki.assert_called_once_with('789')
        expected = [
            {'id': '2', 'name': 'Американская эскимосская собака'},
        ]
        self.assertListEqual(result, expected)
        self.assertEqual(len(responses.calls), 1)

    def get_grid_data_with_parent_success(self):  # {{{
        return {
            'data': {
                'structure': {
                    'fields': [
                        {
                            'required': False,
                            'type': 'string',
                            'name': '100',
                            'title': 'name'
                        },
                        {
                            'format': '%.2f',
                            'required': False,
                            'type': 'number',
                            'name': '101',
                            'title': 'parent'
                        }
                    ],
                    'sorting': [],
                    'done': False,
                    'title': 'еще грид'
                },
                'rows': [
                    [
                        {
                            'raw': 'собака',
                            'sort': 'собака',
                            'row_id': '1',
                            '__key__': '100'
                        },
                        {
                            'sort': 2,
                            'raw': '2',
                            'row_id': '1',
                            '__key__': '101'
                        }
                    ],
                    [
                        {
                            'raw': 'не собака',
                            'sort': 'не собака',
                            'row_id': '2',
                            '__key__': '100'
                        },
                        {
                            'sort': 2,
                            'raw': '2',
                            'row_id': '2',
                            '__key__': '101'
                        }
                    ],
                    [
                        {
                            'raw': 'smth',
                            'sort': 'smth',
                            'row_id': '3',
                            '__key__': '100'
                        },
                        {
                            'sort': 3,
                            'raw': '3',
                            'row_id': '3',
                            '__key__': '101'
                        }
                    ]
                ],
            },
        }  # }}}

    @responses.activate
    def test_should_filter_by_question_correct(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/newgrid/.grid'
        responses.add(responses.GET, url, json=self.get_grid_data_with_parent_success())
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/'
        self.filter_data['wiki_table_source'] = '{}_{}_{}'.format('tag', 'name', 2)

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        expected = [
            {'id': '2', 'name': 'не собака'},
            {'id': '1', 'name': 'собака'},
        ]
        self.assertListEqual(result, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_filter_by_id(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/newgrid/.grid'
        responses.add(responses.GET, url, json=self.get_grid_data_with_parent_success())
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/'
        self.filter_data['wiki_table_source'] = '{}_{}_{}'.format('tag', 'name', 2)
        self.filter_data['id'] = '1'

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        expected = [
            {'id': '1', 'name': 'собака'},
        ]
        self.assertListEqual(result, expected)
        self.assertEqual(len(responses.calls), 1)

    def get_grid_with_wrong_structure(self):  # {{{
        return {
            'error': {
                'debug_message': 'user has no access to resource',
                'error_code': 'USER_HAS_NO_ACCESS',
                'level': 'ERROR',
                'message': [
                    'User is not in the ACL for requested page'
                ]
            },
        }  # }}}

    @responses.activate
    def test_should_fail_on_wrong_table_structure(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/newgrid/smthtest/.grid'
        responses.add(responses.GET, url, json=self.get_grid_with_wrong_structure())
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/smthtest/'

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        self.assertEqual(len(responses.calls), 1)
        self.assertListEqual(result, [])

    def get_grid_not_allowed(self):  # {{{
        return {
            'error': {
                'debug_message': 'user has no access to resource',
                'error_code': 'USER_HAS_NO_ACCESS',
                'level': 'ERROR',
                'message': [
                    'User is not in the ACL for requested page'
                ]
            },
        }  # }}}

    @responses.activate
    def test_should_fail_on_not_allowed_page(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/newgrid/smthtest/.grid'
        responses.add(responses.GET, url, json=self.get_grid_not_allowed(), status=403)
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/smthtest/'

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        self.assertEqual(len(responses.calls), 1)
        self.assertListEqual(result, [])

    @responses.activate
    def test_should_return_403_on_not_allowed_page(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/newgrid/smthtest/.grid'
        responses.add(responses.GET, url, json=self.get_grid_not_allowed(), status=403)
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/smthtest/'

        response = self.client.get('/v1/data-source/wiki-table-source/?free_url=%s' % self.filter_data['free_url'])

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)
        self.assertListEqual(response.data['results'], [])

    @responses.activate
    def test_should_return_404_on_not_existed_page(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/users/smosker/newgrid/smthtest/.grid'
        responses.add(responses.GET, url, json=self.get_grid_not_allowed(), status=404)
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/smthtest/'

        response = self.client.get('/v1/data-source/wiki-table-source/?free_url=%s' % self.filter_data['free_url'])

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)
        self.assertListEqual(response.data['results'], [])

    @responses.activate
    def test_should_fail_on_wrong_supertag(self):
        self.filter_data['free_url'] = 'https://wiki.test.yandex-team.ru/users/smosker/newgrid/smth!test.grdig/'

        result = WikiDataSource().get_filtered_queryset(filter_data=self.filter_data)

        self.assertListEqual(result, [])


class TestSyncYtTable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.cluster_name = 'hahn'
        self.table_path = '//tmp/mytable'

    def test_should_raise_if_table_does_not_exist(self):
        from events.data_sources.tasks import SyncTableDoesNotExist
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            mock_get_metadata.return_value = None
            with self.assertRaises(SyncTableDoesNotExist):
                sync_yt_table(self.cluster_name, self.table_path)
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)

    def test_should_raise_if_table_has_wrong_schema(self):
        from events.data_sources.tasks import SyncTableIncorrectSchema
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            mock_get_metadata.return_value = {
                'schema': [
                    {'name': 'pk'},
                    {'name': 'text'},
                ],
            }
            with self.assertRaises(SyncTableIncorrectSchema):
                sync_yt_table(self.cluster_name, self.table_path)
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)

    def test_should_raise_if_table_too_large(self):
        from events.data_sources.tasks import SyncTableTooLarge, MAX_TABLE_SIZE
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            mock_get_metadata.return_value = {
                'row_count': MAX_TABLE_SIZE + 1,
            }
            with self.assertRaises(SyncTableTooLarge):
                sync_yt_table(self.cluster_name, self.table_path)
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)

    def test_shouldnt_sync_anything_if_table_data_too_old(self):
        modification_time = timezone.now() - timedelta(days=1)
        TableMetaFactory(table_identifier=get_table_id(self.cluster_name, self.table_path))
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            with patch('events.data_sources.tasks._get_table_data') as mock_get_table_data:
                mock_get_metadata.return_value = {
                    'modification_time': modification_time.isoformat(),
                }
                self.assertFalse(sync_yt_table(self.cluster_name, self.table_path))
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)
        mock_get_table_data.assert_not_called()

    def test_shouldnt_sync_anything_if_table_is_empty(self):
        modification_time = timezone.now()
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            with patch('events.data_sources.tasks._get_table_data') as mock_get_table_data:
                mock_get_metadata.return_value = {
                    'modification_time': modification_time.isoformat(),
                }
                mock_get_table_data.return_value = None
                self.assertFalse(sync_yt_table(self.cluster_name, self.table_path))
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)
        mock_get_table_data.assert_called_once_with(self.cluster_name, self.table_path)

    def test_should_create_table_and_insert_data(self):
        modification_time = timezone.now()
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            with patch('events.data_sources.tasks._get_table_data') as mock_get_table_data:
                mock_get_metadata.return_value = {
                    'modification_time': modification_time.isoformat(),
                }
                mock_get_table_data.return_value = [
                    {'id': '101', 'name': 'row101'},
                    {'id': '102', 'name': 'row102'},
                    {'id': '103', 'name': 'row103', 'display_name': 'My Row'},
                    {'id': '104', 'name': 'row104', 'display_name': 'Your Row', 'parent': '11'},
                ]
                self.assertTrue(sync_yt_table(self.cluster_name, self.table_path))
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)
        mock_get_table_data.assert_called_once_with(self.cluster_name, self.table_path)

        table_id = get_table_id(self.cluster_name, self.table_path)
        table_meta = TableMeta(table_identifier=table_id)
        self.assertTrue(table_meta.modification_time > modification_time)
        table_data = {
            row.source_id: row
            for row in TableRow.objects.filter(table_identifier=table_id)
        }
        self.assertEqual(len(table_data), 4)
        self.assertEqual(table_data['101'].text, 'row101')
        self.assertEqual(table_data['101'].display_text, None)
        self.assertEqual(table_data['101'].filter_by, '')
        self.assertEqual(table_data['102'].text, 'row102')
        self.assertEqual(table_data['102'].display_text, None)
        self.assertEqual(table_data['102'].filter_by, '')
        self.assertEqual(table_data['103'].text, 'row103')
        self.assertEqual(table_data['103'].display_text, 'My Row')
        self.assertEqual(table_data['103'].filter_by, '')
        self.assertEqual(table_data['104'].text, 'row104')
        self.assertEqual(table_data['104'].display_text, 'Your Row')
        self.assertEqual(table_data['104'].filter_by, '11')

    def test_should_modify_table_and_update_data(self):
        modification_time = timezone.now()
        table_id = get_table_id(self.cluster_name, self.table_path)
        table_meta = TableMetaFactory(
            table_identifier=table_id,
            modification_time=timezone.now() - timedelta(days=1),
        )
        TableRowFactory(table_identifier=table_id, source_id='101', text='row101')
        TableRowFactory(table_identifier=table_id, source_id='102', text='row102')
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            with patch('events.data_sources.tasks._get_table_data') as mock_get_table_data:
                mock_get_metadata.return_value = {
                    'modification_time': modification_time.isoformat(),
                }
                mock_get_table_data.return_value = [
                    {'id': '102', 'name': 'Row102', 'display_name': 'Not My Row', 'parent': '12'},
                    {'id': '103', 'name': 'Row103', 'display_name': 'My Row', 'parent': '13'},
                ]
                self.assertTrue(sync_yt_table(self.cluster_name, self.table_path))
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)
        mock_get_table_data.assert_called_once_with(self.cluster_name, self.table_path)

        table_meta.refresh_from_db()
        self.assertTrue(table_meta.modification_time > modification_time)
        table_data = {
            row.source_id: row
            for row in TableRow.objects.filter(table_identifier=table_id)
        }
        self.assertEqual(len(table_data), 2)
        self.assertEqual(table_data['102'].text, 'Row102')
        self.assertEqual(table_data['102'].display_text, 'Not My Row')
        self.assertEqual(table_data['102'].filter_by, '12')
        self.assertEqual(table_data['103'].text, 'Row103')
        self.assertEqual(table_data['103'].display_text, 'My Row')
        self.assertEqual(table_data['103'].filter_by, '13')

    def test_should_delete_all_table_rows(self):
        modification_time = timezone.now()
        table_id = get_table_id(self.cluster_name, self.table_path)
        table_meta = TableMetaFactory(
            table_identifier=table_id,
            modification_time=timezone.now() - timedelta(days=1),
        )
        TableRowFactory(table_identifier=table_id, source_id='101', text='row101')
        TableRowFactory(table_identifier=table_id, source_id='102', text='row102')
        with patch('events.data_sources.tasks._get_metadata') as mock_get_metadata:
            with patch('events.data_sources.tasks._get_table_data') as mock_get_table_data:
                mock_get_metadata.return_value = {
                    'modification_time': modification_time.isoformat(),
                }
                mock_get_table_data.return_value = []
                self.assertTrue(sync_yt_table(self.cluster_name, self.table_path))
        mock_get_metadata.assert_called_once_with(self.cluster_name, self.table_path)
        mock_get_table_data.assert_called_once_with(self.cluster_name, self.table_path)

        table_meta.refresh_from_db()
        self.assertTrue(table_meta.modification_time > modification_time)
        self.assertEqual(TableRow.objects.filter(table_identifier=table_id).count(), 0)

    def test_should_invoke_sync_yt_table(self):
        url = f'https://yt.yandex-team.ru/{self.cluster_name}/navigation?path={self.table_path}'
        param_data_source_params = {
            'filters': [{'value': url}]
        }
        with patch('events.data_sources.tasks.sync_yt_table') as mock_sync_yt_table:
            sync_external_table_with_db('yt_table_source', param_data_source_params)
        mock_sync_yt_table.assert_called_once_with(self.cluster_name, self.table_path)

    def test_should_invoke_sync_yt_table_for_all_questions(self):
        survey = SurveyFactory(is_published_external=True)
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='yt_table_source',
            param_data_source_params={
                'filters': [{
                    'value': f'https://yt.yandex-team.ru/{self.cluster_name}/navigation?path={self.table_path}&sort=asc',
                }],
            },
        )
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='yt_table_source',
            param_data_source_params={
                'filters': [{
                    'value': f'https://yt.yandex-team.ru/{self.cluster_name}/navigation?path={self.table_path}&sort=desc',
                }],
            },
        )
        with patch('events.data_sources.tasks.sync_yt_table') as mock_sync_yt_table:
            sync_external_tables_for_all_questions()
        mock_sync_yt_table.assert_called_once_with(self.cluster_name, self.table_path)
