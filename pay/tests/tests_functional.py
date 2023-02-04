
import json
from unittest import mock

import pytest

from payplatform.balance_support_dev.tools.common.lib.utils import (
    fill_json, fill_text, fill_zip
)
from payplatform.balance_support_dev.tools.email_sender.common.lib.utils import (
    get_attachment_data
)
from payplatform.balance_support_dev.tools.email_sender.checker.lib.main import main


def patched_main(tpl_filename, tpl_data_filename, attachments_archive, attachments_markup, letter_data_filename):
    args = [__name__, '-t', tpl_filename, '-d', tpl_data_filename, '-l', letter_data_filename]
    if attachments_archive:
        args.extend(['-a', attachments_archive])
    if attachments_markup:
        args.extend(['-m', attachments_markup])
    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize('attachments_archive, attachments_markup', [
    (True, False),
    (False, True)
])
def test_invoke_main(tpl_filename, tpl_data_filename, attachments_archive_filename, attachments_markup_filename,
                     letter_data_filename, attachments_archive, attachments_markup):
    attachments_archive_filename = attachments_archive_filename if attachments_archive else None
    attachments_markup_filename = attachments_markup_filename if attachments_markup else None
    with pytest.raises(SystemExit):
        patched_main(tpl_filename, tpl_data_filename, attachments_archive_filename, attachments_markup_filename,
                     letter_data_filename)

    return


# @pytest.mark.parametrize('tpl, tpl_data, expectation, error', [
# ])
# def test_invalid_main_simple(tpl_filename, tpl_data_filename, letter_data_filename,
#                              tpl, tpl_data, expectation, error):
#     fill_text(tpl_filename, tpl)
#     fill_json(tpl_data_filename, tpl_data)
#
#     with expectation as exc:
#         patched_main(tpl_filename, tpl_data_filename, None, None, letter_data_filename)
#
#     assert error in str(exc)
#
#     return


@pytest.mark.parametrize('tpl, tpl_data, expected_output', [
    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [
         {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
         {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
     ], [
         {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
         {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
     ])
])
def test_valid_main_simple(tpl_filename, tpl_data_filename, letter_data_filename,
                           tpl, tpl_data, expected_output):
    fill_text(tpl_filename, tpl)
    fill_json(tpl_data_filename, tpl_data)

    patched_main(tpl_filename, tpl_data_filename, None, None, letter_data_filename)

    with open(letter_data_filename, 'r') as output_file:
        output_content = output_file.read()
        output_content = json.loads(output_content)
        assert all(row in expected_output for row in output_content)
        assert all(row in output_content for row in expected_output)

    return


# @pytest.mark.parametrize('tpl, tpl_data, attachments_archive, attachments_markup, expectation, error', [
# ])
# def test_invalid_main(tpl_filename, tpl_data_filename, attachments_archive_filename, attachments_markup_filename,
#                       letter_data_filename,
#                       tpl, tpl_data, attachments_archive, attachments_markup, expectation, error):
#     fill_text(tpl_filename, tpl)
#     fill_json(tpl_data_filename, tpl_data)
#     fill_zip(attachments_archive_filename, attachments_archive)
#     fill_json(attachments_markup_filename, attachments_markup)
#
#     with expectation as exc:
#         patched_main(tpl_filename, tpl_data_filename, attachments_archive_filename, attachments_markup_filename,
#                      letter_data_filename)
#
#     assert error in str(exc)
#
#     return


@pytest.mark.parametrize('tpl, tpl_data, attachments_archive, attachments_markup, expected_output', [
    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [
         {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
         {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
     ], [
         ('Тест1.txt', 'Test attachment # 1 (тестовое вложение № 1)'),
         ('Тест2.txt', 'Test attachment # 2 (тестовое вложение № 2)')
     ], [
         {'DATA_ROWNUM': 0, 'FILENAME': 'test1.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест1.txt'},
         {'DATA_ROWNUM': 1, 'FILENAME': 'test2.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест2.txt'}
     ], [
         {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
          'ATTACHMENTS': [
              {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain',
               'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')}
          ]},
         {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
          'ATTACHMENTS': [
              {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
               'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
          ]}
     ])
])
def test_valid_main(tpl_filename, tpl_data_filename, attachments_archive_filename, attachments_markup_filename,
                    letter_data_filename,
                    tpl, tpl_data, attachments_archive, attachments_markup, expected_output):
    fill_text(tpl_filename, tpl)
    fill_json(tpl_data_filename, tpl_data)
    fill_zip(attachments_archive_filename, attachments_archive)
    fill_json(attachments_markup_filename, attachments_markup)

    patched_main(tpl_filename, tpl_data_filename, attachments_archive_filename, attachments_markup_filename,
                 letter_data_filename)

    with open(letter_data_filename, 'r') as output_file:
        output_content = output_file.read()
        output_content = json.loads(output_content)
        assert all(row in expected_output for row in output_content)
        assert all(row in output_content for row in expected_output)

    return
