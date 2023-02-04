from typing import Sequence, Dict

from dwh.grocery.targets import OracleTableTarget
from dwh.grocery.tools import RW


def insert_into_table(table: OracleTableTarget, values):
    col_names = table.get_col_names()
    q = {
        'table': table.intrabase_uri,
        'columns': ", ".join(col_names),
        'values': ", ".join(":" + col_name for col_name in col_names)
    }
    isql = """
    INSERT INTO {table}
    ({columns})
    VALUES
    ({values})
    """.format(**q)
    print(values, "VALUE TO INSERT")
    table.engine.copy(RW).execute_many(isql, values)


def truncate_table(table: OracleTableTarget):
    table.engine.copy(RW).execute(F"""
        TRUNCATE TABLE {table.intrabase_uri}
    """)


def drop_table(table: OracleTableTarget):
    table.engine.copy(RW).execute(F"""
        DROP TABLE {table.intrabase_uri}
    """)


def update_record(table: OracleTableTarget, old_record: dict, new_record: dict):
    set_expr = [
        f"{field} = :new_{field}"
        for field in
        new_record.keys()
    ]

    where_expr = [
        f"{field} = :old_{field}"
        for field in
        old_record.keys()
    ]

    old_r_params = {
        f"old_{key}": value
        for key, value in
        old_record.items()
    }

    new_r_params = {
        f"new_{key}": value
        for key, value in
        new_record.items()
    }

    params = {
        **old_r_params,
        **new_r_params,
    }

    return table.engine.copy(RW).execute(f"""
    update {table.intrabase_uri}
    set {", ".join(set_expr)}
    where {" and ".join(where_expr)}
    """, params)


def delete_record(table: OracleTableTarget, record: dict):
    where_expr = [
        f"{field} = :{field}"
        for field in
        record.keys()
    ]
    return table.engine.copy(RW).execute(f"""
    delete from {table.intrabase_uri}
    where {" and ".join(where_expr)}
    """, record)


def clean_export_changes(table: OracleTableTarget):
    table.engine.copy(RW).execute(f"""
    delete from bo.t_yt_export_changes
    where source_name = '{table.intrabase_uri}'
    """)


def as_set(records: Sequence[Dict]):
    return set(frozenset(record.items()) for record in records)


def is_eq(table_a, table_b):
    records_a = as_set(table_a.read())
    records_b = as_set(table_b.read())

    d_a = records_a - records_b
    d_b = records_b - records_a
    return not d_a and not d_b
