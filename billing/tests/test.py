import datetime
import logging as log
import os
import unittest
from unittest import mock

import pytest
from billing.contrib.py3solomon.py3solomon import (
    Sensor,
    API,
)

from billing.yb_reports_monitorings.yb_reports_monitorings import db_structure as db_struct
from billing.yb_reports_monitorings.yb_reports_monitorings import view_monitoring as vm
from billing.yb_reports_monitorings.yb_reports_monitorings.view_monitoring import measure_rows_and_recency, \
    get_prev_month, parse_date

namespace = "billing.yb_reports_monitorings.yb_reports_monitorings"


def make_sensor() -> Sensor:
    s = Sensor('hot', pepper="hell chili")
    s.value = 666
    return s


SOLOMON_CONF = {
    "project_id": "balance-reports",
    "project_name": "BalanceReports",
    "cluster_id": "push",
    "cluster_name": "Push",
    "ABC": "ybar",
    "subprojects":
        {
            "reports":
                {
                    "service_id": "yb-reports",
                    "service_name": "YbReports",
                    "shard": "push-yb-reports"
                },
            "ar":
                {
                    "service_id": "balance-reports_yb-ar",
                    "service_name": "YbAr",
                    "shard": "push-yb-ar"
                }
        }
}


def stub_subproject(requests_mock, subproject: str, conf: dict, headers: dict, host: str):
    """
        Мокает нужные ссылки в requests
    """
    project = conf['project_id']
    service = conf["subprojects"][subproject]["service_id"]
    cluster = conf["cluster_id"]
    shard = conf['subprojects'][subproject]['shard']
    raw_shard = {
        'createdAt': "dobyri vecher",
        'createdBy': "hitler",
        'state': 'nice',
        'clusterId': cluster,
        'serviceId': service,
    }

    raw_cluster = {
        "name": conf["cluster_id"],
        'createdAt': "dobyri vecher",
        'createdBy': "hitler",
    }

    raw_service = {
        "name": service,
        'createdAt': "dobyri vecher",
        'createdBy': "hitler",
    }

    project_url = host + API + f"projects/{project}"
    shard_url = project_url + f"/shards/{shard}"
    cluster_url = project_url + f"/clusters/{cluster}"
    service_url = project_url + f"/services/{service}"

    requests_mock.get(project_url, request_headers=headers, text='ok')

    log.info(f"Twice Stub {shard_url} with {raw_shard}")
    requests_mock.get(shard_url, request_headers=headers, json=raw_shard)
    log.info(f"Stub {cluster_url} with {raw_cluster}")
    requests_mock.get(cluster_url, request_headers=headers, json=raw_cluster)
    log.info(f"Stub {service_url} with {raw_service}")
    requests_mock.get(service_url, request_headers=headers, json=raw_service)

    push_url = host + API + f'push/?service={service}&project={project}&cluster={cluster}'
    requests_mock.post(push_url, request_headers=headers, text='ok')


@mock.patch(f"{namespace}.view_monitoring.get_ar_sensors", return_value=[make_sensor()])
@mock.patch(f"{namespace}.view_monitoring.get_solomon_conf", return_value=SOLOMON_CONF)
@mock.patch(f"{namespace}.view_monitoring.get_reports_sensors", return_value=[make_sensor()])
@mock.patch(f"{namespace}.view_monitoring.get_target_views", return_value=None)
def test_main(get_target_views_m, get_all_sensors_m, solomon_conf_mock, ar_sensors, requests_mock):
    os.environ['YT_TOKEN'] = 'UPYACHKA'
    os.environ['YQL_TOKEN'] = 'UPYACHKA'
    os.environ['SOLOMON_TOKEN'] = 'UPYACHKA'
    os.environ['YBRM_ERRORBOOSTER_ENABLED'] = '0'
    sol_mock = solomon_conf_mock.return_value

    headers = {'Authorization': 'OAuth UPYACHKA'}
    host = 'http://monitoring/'

    stub_subproject(requests_mock, 'reports', sol_mock, headers, host)
    stub_subproject(requests_mock, 'ar', sol_mock, headers, host)

    with mock.patch(f"{namespace}.view_monitoring.get_host", return_value=host):
        vm.main()

    assert len(requests_mock.request_history) == 11

    for index, _ in enumerate(["Project Exists", "Shard Exists", "Shard Read", "Cluster Read", "Service Read"]):
        request = requests_mock.request_history[index]
        assert request.method == 'GET'

    request = requests_mock.last_request
    assert request.method == 'POST'


class TestMeasureRowsAndRecencyYT(unittest.TestCase):

    @mock.patch(f"{namespace}.view_monitoring.dt", wraps=datetime.datetime)
    @mock.patch(f"{namespace}.view_monitoring.get_yt_attr")
    @mock.patch(f"{namespace}.view_monitoring.yt.get")
    def test_measure_rows_and_recency_mapnode(self, yt_get_mock, get_yt_attr_mock, datetime_mock):
        """
            Проверяем корректность работы measure_rows_and_recency на мапноде
        """
        datetime_mock.now.return_value = datetime.datetime(2021, 11, 5, 0, 0, 45, tzinfo=datetime.timezone.utc)
        # мокаем атрибуты мапноды в порядке вызова: type, map_node_row_count,
        # update_id первой партиции, update_id второй партиции
        get_yt_attr_mock.side_effect = ["map_node", {"2021-09": 100, "2021-10": 200, "2021-11": 150},
                                        "2021-11-5T00:00:00.000000Z",
                                        "2021-01-01T00:00:00.000000Z"]
        mapnode_path = "//path/to/mapnode"
        yt_get_mock.return_value = {"2021-09": None, "2021-10": None}
        actual_sensors = measure_rows_and_recency(mapnode_path)
        expected_sensors = [
            Sensor("rows-now", mview="mapnode", source="yt"),
            Sensor("rows-prev-month", mview="mapnode", source="yt"),
            Sensor("rows-prev-prev-month", mview="mapnode", source="yt"),
            Sensor("recency", mview="mapnode", source="yt")
        ]
        # Ожидаемые значения метрик
        expected_sensors[0].value = 150
        expected_sensors[1].value = 200
        expected_sensors[2].value = 100
        # кол-во секунд с обновления = 45 (starting_point - max(update_id))
        expected_sensors[3].value = 45

        self.assertEqual(expected_sensors, actual_sensors)
        for expected, actual in zip(expected_sensors, actual_sensors):
            self.assertEqual(expected.value, actual.value)

    @mock.patch(f"{namespace}.view_monitoring.get_yt_attr")
    @mock.patch(f"{namespace}.view_monitoring.yt.get")
    def test_measure_rows_and_recency_table(self, yt_get_mock, get_yt_attr_mock):
        """
            Проверяем корректность работы measure_rows_and_recency на обычной таблице
            Здесь же проверяем, что получилось отправить метрику с переопределенным названием mview
        """
        # мокаем атрибуты таблицы в порядке вызова: type, row_count, update_id
        get_yt_attr_mock.side_effect = ["table", 100_000, "2022-01-11T00:00:00.000000Z"]
        table_path = "//path/to/table"
        actual_sensors = measure_rows_and_recency(table_path, starting_point=datetime.datetime(2022, 1, 11, 0, 0, 45),
                                                  also={"rename": "new_name"})
        expected_sensors = [
            Sensor("rows", mview="new_name", source="yt"),
            Sensor("recency", mview="new_name", source="yt")
        ]
        # Ожидаемые значения метрик
        # кол-во строк = 100_000
        expected_sensors[0].value = 100_000
        # кол-во секунд с обновления = 45 (starting_point - update_id)
        expected_sensors[1].value = 45

        self.assertEqual(expected_sensors, actual_sensors)

    @mock.patch(f"{namespace}.view_monitoring.get_yt_attr")
    @mock.patch(f"{namespace}.view_monitoring.yt.get")
    def test_measure_rows_and_recency_unknown(self, yt_get_mock, get_yt_attr_mock):
        """
            Проверяем, что measure_rows_and_recency падает, если у ноды в YT неизвестный тип
        """
        get_yt_attr_mock.side_effect = ["something"]
        table_path = "//path/to/table"
        with self.assertRaises(Exception) as context:
            measure_rows_and_recency(table_path)
        self.assertIn("Unknown content of node //path/to/table", str(context.exception))

    @mock.patch(f"{namespace}.view_monitoring.get_yt_attr")
    @mock.patch(f"{namespace}.view_monitoring.yt.get")
    def test_measure_rows_and_recency_none(self, yt_get_mock, get_yt_attr_mock):
        """
            Если нет никаких данных, ничего не отправляем и не падаем.
        """
        get_yt_attr_mock.side_effect = ["map_node", {"2021-09": 100, "2021-10": 200}, "2022-01-11T00:00:00.000000Z",
                                        "None"]
        mapnode_path = "//path/to/mapnode"
        yt_get_mock.return_value = {"2021-09": None, "2021-10": None}
        actual_sensors = measure_rows_and_recency(mapnode_path, starting_point=datetime.datetime(2022, 1, 11, 0, 0, 45))
        expected_sensors = []
        self.assertEqual(actual_sensors, expected_sensors)

    def test_get_prev_month(self):
        """
            Проверяем работу функции get_prev_month,
            которая возвращает 1 число предыдущего месяца.
        """
        test_data = {
            datetime.datetime(2023, 2, 1): datetime.datetime(2023, 1, 1),
            datetime.datetime(2023, 1, 1): datetime.datetime(2022, 12, 1),
            datetime.datetime(2022, 12, 1): datetime.datetime(2022, 11, 1),
        }
        for input, result in test_data.items():
            self.assertEqual(result, get_prev_month(input))

    def test_parse_date(self):
        """
            Проверяем работу функции parse_date,
            которая по строковой дате возвращает формат.
        """
        test_data = {
            "2022-02": "%Y-%m",
            "2022-02-01": "%Y-%m-%d",
            "202202": "%Y%m",
        }
        for input, result in test_data.items():
            self.assertEqual(result, parse_date(input))


class TestDbStructure:
    @pytest.fixture
    def mock_sqlalchemy_execute(self):
        with mock.patch(f"{namespace}.db_structure.make_alchemy_connection") as m:
            execute = m.return_value.connect.return_value.execute
            yield execute

    @pytest.fixture
    def mock_query(self, mock_sqlalchemy_execute):
        schema = ('COLUMN_NAME', 'DATA_TYPE', 'DATA_LENGTH', 'DATA_PRECISION', 'DATA_SCALE', 'CHAR_LENGTH')

        def _make_rows(columns_list):
            for columns in columns_list:
                yield [
                    mock.MagicMock(**{
                        '__getitem__.side_effect': column.__getitem__,
                        'items.return_value': list(zip(schema, column)),
                    })
                    for column in columns
                ]

        def _mock_query(columns):
            if not isinstance(columns, list):
                columns = [columns]
            mock_sqlalchemy_execute.side_effect = _make_rows(columns)

        return _mock_query

    def test_measure_db_structure(self, mock_query):
        columns_common = (
            ('ID', 'NUMBER', '22', None, None, None),
            ('VALUE_STR', 'VARCHAR2', '1536', None, None, '512'),
            ('VALUE_DT', 'DATE', '7', None, None, None),
            ('VALUE_CLOB', 'VARCHAR2', 4000, None, None, 4000),
            ('VALUE_NUM', 'NUMBER', '22', None, None, 0),
        )

        columns1 = columns_common + (('VALUE_STR', 'VARCHAR2', '1536', None, None, '512'),)
        columns2 = columns_common + (('VALUE_STR', 'VARCHAR2', '768', None, None, '256'),)

        mock_query([columns1, columns1])
        sensor = db_struct.measure_db_structure('title')[0]
        assert not sensor.value['value']

        mock_query([columns2, columns2])
        sensor = db_struct.measure_db_structure('title')[0]
        assert not sensor.value['value']

        mock_query([columns1, columns2])
        sensor = db_struct.measure_db_structure('title')[0]
        assert sensor.value['value']

        assert sensor.name == 'db_structure'
        assert sensor.labels['mview'] == 'title'

    def test_table_load(self, mock_query):
        columns = (
            ('ID', 'NUMBER', 22, None, None, 0),
            ('NAME', 'VARCHAR2', 128, None, None, 128),
            ('OTHER', 'DATE', 7, None, None, 0),
        )
        mock_query(columns)

        source_config = db_struct.get_config(
            'some_title',
            {'source1': {'exclude': ['other']}},
        )['source1']

        struct = db_struct.TableLoader(source_config).load_structure()

        assert struct['ID']['DATA_TYPE'] == 'NUMBER'
        assert struct['NAME']['DATA_TYPE'] == 'VARCHAR2'
        assert struct['NAME']['CHAR_LENGTH'] == 128
        assert not struct['OTHER']

    def test_find_structure_diff(self):
        columns1 = db_struct.TableColumns({
            'one': {
                'oaooamm': 123,
                'amogus': 'yes',
                'foobar': None,
            },
            'two': {
                'oaooamm': 0,
                'amogus': 'no',
                'foobar': 'buzz',
            },
        })

        columns2 = db_struct.TableColumns(columns1)
        updates = {
            'one': {
                'amogus': 'no',
                'foobar': 'cat',
            },
            'two': {
                'foobar': 'buss',
            },
            'three': {
                'oaooamm': 5,
                'amogus': 'no',
                'foobar': 'dog',
            }
        }
        columns2.update(updates)

        diffs = db_struct._find_structure_diff(columns1, columns1)
        assert not (diffs['columns1'] or diffs['columns2'])

        diffs = db_struct._find_structure_diff(columns2, columns2)
        assert not (diffs['columns1'] or diffs['columns2'])

        diffs = db_struct._find_structure_diff(columns1, columns2)
        assert all(map(
            lambda col: (col in diffs['columns1']) or (col in diffs['columns2']),
            updates.keys()
        ))


class TestMeasureRowsAndRecencyOracle(unittest.TestCase):
    def test_parse_cmv_query_result(self):
        oracle_query_result = [
            [
                'SYSTEM.CMV_REFRESH',
                datetime.datetime.strptime('2022-06-16 16:13:30', '%Y-%m-%d %H:%M:%S'),
                'BO.CMV_RECEIPTS_ITG: finished. Started at 2022-06-16 15:22:20'
            ],
            [
                'SYSTEM.CMV_REFRESH',
                datetime.datetime.strptime('2022-06-19 23:14:44', '%Y-%m-%d %H:%M:%S'),
                'BO.CMV_CONTRACT_LAST_ATTR: finished. Started at 2022-06-19 22:43:02'
            ]
        ]
        expected = {
            'CMV_RECEIPTS_ITG': (
                datetime.datetime(2022, 6, 16, 15, 22, 20),
                datetime.datetime(2022, 6, 16, 16, 13, 30),
                3070.0,
            ),
            'CMV_CONTRACT_LAST_ATTR': (
                datetime.datetime(2022, 6, 19, 22, 43, 2),
                datetime.datetime(2022, 6, 19, 23, 14, 44),
                1902.0,
            )
        }
        dictionary_prepared_for_sensors = vm._parse_cmv_query_result(oracle_query_result)
        assert dictionary_prepared_for_sensors == expected
