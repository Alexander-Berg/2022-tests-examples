# coding: utf-8
__author__ = 'chihiro'
import pytest

from oebsapi import steps


@pytest.mark.parametrize("table_name, pk", [
    ('apps.xxar_export_edo_uv_akt', 'akt_number')
    , ('apps.xxar_export_edo_uv_akt_lines', 'akt_number')
    , ('apps.xxar_export_edo_uv_schet', 'bill_number')
    , ('apps.xxar_export_edo_uv_schet_lines', 'bill_number')
    , ('apps.xxar_export_edo_uv_sf', 'sf_number')
    , ('apps.xxar_export_edo_uv_sf_lines', 'sf_number')
]
    , ids=lambda x: x
                         )
def test_query_table(table_name, pk):
    result = steps.OebsapiSteps.query_table(table_name, True)
    assert pk in result.text


@pytest.mark.parametrize("table_name, pk", [
    ('apps.xxar_export_edo_uv_akt', 'akt_number')
    , ('apps.xxar_export_edo_uv_schet', 'bill_number')
    , ('apps.xxar_export_edo_uv_sf', 'sf_number')
]
    , ids=lambda x: x
                         )
def test_update_table(table_name, pk):
    row_data = steps.OebsSteps.get_data(table_name)
    doc_id = row_data[pk].split('-')[1] if pk == 'bill_number' else row_data[pk]
    steps.OebsapiSteps.update_table(table_name, 'status={}'.format('null' if row_data['status'] else 2),
                                    u'{} like \'%{}%\''.format(pk, doc_id))
    new_data = steps.OebsSteps.get_data(table_name, data=u'{} = \'{}\''.format(pk, row_data[pk]))
    assert new_data['status'] == (None if row_data['status'] else 2)
