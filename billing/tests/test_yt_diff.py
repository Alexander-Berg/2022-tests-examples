import unittest
from unittest.mock import Mock

from dwh.grocery.yt_table_diff import YTTableDiff
from dwh.grocery.targets import YTTableTarget


class TestYTTableDiff(unittest.TestCase):

    def test_get_patched_schema(self):
        """
            Проверяем функцию get_patched_schema
        """
        diff = YTTableDiff(prod_table="", dev_table="", pr_id="", cluster="", diff_rows="")
        table_mock = Mock(YTTableTarget)
        table_mock.get_attr.side_effect = [
            # get_attr("schema")
            [
                {
                    "name": "act_id",
                    "required": False,
                    "type": "int64",
                    "type_v3": {
                        "type_name": "optional",
                        "item": "int64"
                    }
                },
                {
                    "name": "dt",
                    "required": False,
                    "type": "string",
                    "type_v3": {
                        "type_name": "optional",
                        "item": "string"
                    }
                },
                {
                    "name": "client_name",
                    "required": False,
                    "type": "utf8",
                    "type_v3": {
                        "type_name": "optional",
                        "item": "utf8"
                    }
                },
                {
                    "name": "amt_rur",
                    "required": False,
                    "type": "double",
                    "type_v3": {
                        "type_name": "optional",
                        "item": "double"
                    }
                },
            ],
            # get_attr("_yql_row_spec")
            {"TypePatch": [
                "StructType",
                [
                    [
                        "dt",
                        [
                            "OptionalType",
                            [
                                "DataType",
                                "Datetime"
                            ]
                        ]
                    ]
                ]
            ]}
        ]
        schema = diff.get_patched_schema(table_mock)
        expected = {
            "act_id": "int64",
            "dt": "datetime",
            "client_name": "utf8",
            "amt_rur": "double"
        }
        self.assertEqual(expected, schema)

    @unittest.mock.patch("dwh.grocery.yt_table_diff.YTTableDiff.get_patched_schema")
    def test_get_yql_query(self, get_patched_schema_mock):
        """
            Проверяет функцию get_yql_query
        """
        table = Mock(YTTableTarget)
        diff = YTTableDiff(prod_table="//path/to/prod", dev_table="//path/to/dev", pr_id="12345", cluster="hahn")
        get_patched_schema_mock.side_effect = [{
            "act_id": "int64",
            "dt": "datetime",
            "client_name": "utf8",
            "amt_rur": "double"
        }]
        prod_query, dev_query = diff.get_yql_query(table, "//dwh/prod/only", "//dwh/dev/only")
        # Все колонки должны быть проверены на NULL, float'ы скастованы к double
        prod_query_expected = """
use hahn;

$left =
SELECT
IF(act_id IS NULL, CAST(0 as int64), act_id) AS act_id, IF(dt IS NULL, CAST(0 as datetime), dt) AS dt, IF(client_name IS NULL, CAST(0 as utf8), client_name) AS client_name, IF(amt_rur IS NULL, CAST(0 as double), amt_rur) AS amt_rur
FROM `//path/to/prod`;

$right =
SELECT
IF(act_id IS NULL, CAST(0 as int64), act_id) AS act_id, IF(dt IS NULL, CAST(0 as datetime), dt) AS dt, IF(client_name IS NULL, CAST(0 as utf8), client_name) AS client_name, IF(amt_rur IS NULL, CAST(0 as double), amt_rur) AS amt_rur
FROM `//path/to/dev`;


INSERT INTO `//dwh/prod/only` WITH TRUNCATE
SELECT a.act_id as act_id, a.dt as dt, a.client_name as client_name, CAST(a.amt_rur as float) as amt_rur
FROM $left AS a LEFT JOIN $right AS b
ON a.act_id = b.act_id AND a.dt = b.dt AND a.client_name = b.client_name AND CAST(a.amt_rur as float) = CAST(b.amt_rur as float)
WHERE b.act_id is null AND b.dt is null AND b.client_name is null AND b.amt_rur is null
ORDER BY act_id, dt, client_name, amt_rur;
""" # noqa
        dev_query_expected = """
use hahn;

$left =
SELECT
IF(act_id IS NULL, CAST(0 as int64), act_id) AS act_id, IF(dt IS NULL, CAST(0 as datetime), dt) AS dt, IF(client_name IS NULL, CAST(0 as utf8), client_name) AS client_name, IF(amt_rur IS NULL, CAST(0 as double), amt_rur) AS amt_rur
FROM `//path/to/dev`;

$right =
SELECT
IF(act_id IS NULL, CAST(0 as int64), act_id) AS act_id, IF(dt IS NULL, CAST(0 as datetime), dt) AS dt, IF(client_name IS NULL, CAST(0 as utf8), client_name) AS client_name, IF(amt_rur IS NULL, CAST(0 as double), amt_rur) AS amt_rur
FROM `//path/to/prod`;


INSERT INTO `//dwh/dev/only` WITH TRUNCATE
SELECT a.act_id as act_id, a.dt as dt, a.client_name as client_name, CAST(a.amt_rur as float) as amt_rur
FROM $left AS a LEFT JOIN $right AS b
ON a.act_id = b.act_id AND a.dt = b.dt AND a.client_name = b.client_name AND CAST(a.amt_rur as float) = CAST(b.amt_rur as float)
WHERE b.act_id is null AND b.dt is null AND b.client_name is null AND b.amt_rur is null
ORDER BY act_id, dt, client_name, amt_rur;
""" # noqa
        self.maxDiff = None
        self.assertEqual(prod_query_expected, prod_query)
        self.assertEqual(dev_query_expected, dev_query)
