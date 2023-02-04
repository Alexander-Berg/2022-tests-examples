from unittest.mock import MagicMock

import pytest

from bcl.toolbox.xls import XlsRow, Style


STYLE_DICT = {
    'font': 'bold yes, italic True, color pink',
    'borders': 'left thin, right medium',
}
STYLE_STR = 'font: bold yes, italic True, color pink; borders: left thin, right medium'


def test_style_join_dict():
    actual = Style._join_dict(STYLE_DICT)

    assert actual == STYLE_STR


def test_style_compile_cache():
    global_style = Style(**STYLE_DICT)

    result1 = global_style.compile()
    result2 = global_style.compile()
    assert result1 == STYLE_STR
    assert result1 is result2

    local_style = Style(font='name Times New Roman', align='vert top')
    result1 = local_style.compile(merge_with=global_style)
    result2 = local_style.compile(merge_with=global_style)
    expected = 'font: name Times New Roman; borders: left thin, right medium; align: vert top'
    assert result1 == expected
    assert result1 is result2


def test_xls_row_empty_cache():
    empty1 = XlsRow.empty()
    empty2 = XlsRow.empty()

    assert empty1 is empty2


def test_xls_row_get_prepared_data():
    row = XlsRow(('a', 'b', 'c', 'd', 'e'), offset=3, merge=(5, 4, 3, 2, 1, 10))
    expected = [
        'a', None, None, None, None,
        'b', None, None, None,
        'c', None, None,
        'd', None,
        'e'
    ]
    actual = list(row._get_prepared_data())

    assert actual == expected


@pytest.mark.parametrize(
    'style_obj, expected_style_str',
    (
        pytest.param(Style(), STYLE_STR, id='empty-style'),
        pytest.param(Style(font='bold yes'), 'font: bold yes; borders: left thin, right medium', id='merge-styles'),
    ))
def test_xls_row_add_row_to_the_writer(style_obj, expected_style_str):
    global_style = Style(**STYLE_DICT)

    row = XlsRow(
        data=('aaa', 'bbb', 'ccc'),
        offset=3,
        merge=(2, 3, 1, 2),
        style=style_obj
    )
    writer = MagicMock()

    row.add_row_to_the_writer(writer, global_style)

    assert writer.merge_cols.call_count == 3  # merge с единицей пропустили
    assert writer.add_row.call_count == 1

    assert [call.args for call in writer.merge_cols.call_args_list] == [(3, 4), (5, 7), (9, 10)]

    assert list(writer.add_row.call_args.args[0]) == ['aaa', None, 'bbb', None, None, 'ccc']
    assert writer.add_row.call_args.kwargs == {'offset': 3, 'style': expected_style_str}
