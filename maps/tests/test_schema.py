import json
import os
from pathlib import Path
import psycopg2
import yatest
import pytest

from maps.doc.schemas.ymapsdf.tools.lib import converter
from maps.pylibs.local_postgres import Database


def test_schemas():
    sql_schema_path = yatest.common.binary_path("maps/doc/schemas/ymapsdf/package/ymapsdf.sql")
    json_schema_dir = yatest.common.source_path("maps/doc/schemas/ymapsdf/garden/json")

    converter.sql_to_json(Path(sql_schema_path), Path("."))

    output_dir = "garden/json"

    result_files = os.listdir(output_dir)
    expected_files = os.listdir(json_schema_dir)

    result_files.sort()
    expected_files.sort()

    assert result_files == expected_files

    for filename in expected_files:
        expected_filepath = os.path.join(json_schema_dir, filename)
        result_filepath = os.path.join(output_dir, filename)

        with open(expected_filepath) as expected_file:
            with open(result_filepath) as result_file:
                result_content = result_file.read()
                assert result_content == expected_file.read()

                # Check that key columns go first in the schema
                result_schema = json.loads(result_content)
                columns = result_schema["columns"]
                for i, name in enumerate(result_schema.get("key_columns", [])):
                    assert columns[i]["name"] == name


def _read_queries_from_dir(dir):
    queries = {}

    for filename in os.listdir(dir):
        if not filename.endswith(".sql"):
            continue
        tablename, _ = filename.split(".")
        with open(os.path.join(dir, filename)) as f:
            queries[tablename] = f.read()

    return queries


def test_sql_create_and_finalize_queries():
    create_queries = _read_queries_from_dir(yatest.common.test_source_path("../garden/create"))
    finalize_queries = _read_queries_from_dir(yatest.common.test_source_path("../garden/finalize"))

    database = Database.create_instance()
    database.create_extension("postgis")
    conn = psycopg2.connect(database.connection_string)
    cursor = conn.cursor()

    for tablename, create_query in create_queries.items():
        try:
            cursor.execute(create_query.format(_self=tablename))
            finalize_query = finalize_queries.get(tablename)
            if finalize_query:
                cursor.execute(finalize_query.format(_self=tablename))
        except KeyError as e:
            raise RuntimeError("""
                Table `{}` references another table
                which is prohibited in create and finalize queries
                """.format(tablename)) from e
        except Exception as e:
            raise RuntimeError("""
                Table `{}` has an error in its create or finalize query
                """.format(tablename)) from e

        # Check that the table has been created
        cursor.execute("SELECT * FROM {}".format(tablename))

        conn.rollback()

    cursor.close()
    conn.close()


def test_constraints():
    assert converter.convert_constraint("speed_limit BETWEEN 5 AND 150") == {
        "type": "expression",
        "value": "speed_limit BETWEEN 5 AND 150",
    }

    assert converter.convert_constraint("sidewalk IN ('N', 'B', 'L', 'R')") == {
        "type": "expression",
        "value": "sidewalk IN ('N', 'B', 'L', 'R')",
    }

    assert converter.convert_constraint("isocode SIMILAR TO '[A-Z]{2}|[0-9]{3}'") == {
        "type": "regexp",
        "field": "isocode",
        "pattern": "[A-Z]{2}|[0-9]{3}",
    }

    assert converter.convert_constraint("date_start ~ '^([0-9]{4}|)$'") == {
        "type": "regexp",
        "field": "date_start",
        "pattern": "^([0-9]{4}|)$",
    }

    with pytest.raises(ValueError):
        converter.convert_constraint("(rd_id IS NOT NULL)::int + (ad_id IS NOT NULL)::int + (ft_id IS NOT NULL)::int = 1")

    with pytest.raises(ValueError):
        converter.convert_constraint("(name_type = 6 AND lang = 'zxx' AND (name SIMILAR TO '[A-Za-z0-9_-]+'))")
