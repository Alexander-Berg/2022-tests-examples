
import json
from unittest import mock

import pytest

from payplatform.balance_support_dev.tools.common.lib.utils import fill_json, fill_text
from payplatform.balance_support_dev.tools.email_sender.common.lib.utils import get_attachment_data
from payplatform.balance_support_dev.tools.email_sender.smtp_sender.lib.main import main


def patched_main(tpl_filename, tpl_data_filename, report_filename, unhandled_data_filename, session):
    args = [
        __name__, '-t', tpl_filename, '-d', tpl_data_filename, '-r', report_filename, '-u', unhandled_data_filename,
        '-subject', 'test/тест', '-from_email', 'noreply', '-reply_to', 'noreply noreply2',
        '-retry_interval', '2', '-retry_count', '3', '-interval', '1',
        '-host', 'test', '-port', '443', '-mode', 'no-SSL'
    ]

    with mock.patch('sys.argv', args),\
         mock.patch('payplatform.balance_support_dev.tools.email_sender.smtp_sender.lib.main.setup_smtp_session',
                    return_value=session):
        main()

    return


@pytest.mark.parametrize('tpl, tpl_data, side_effects', [
    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [
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
     ],
     [{}, {}])
])
def test_valid_main(tpl_filename, tpl_data_filename, report_filename, unhandled_data_filename,
                    tpl, tpl_data, side_effects,
                    session):
    fill_text(tpl_filename, tpl)
    fill_json(tpl_data_filename, tpl_data)

    session.send_message.side_effect = side_effects
    patched_main(tpl_filename, tpl_data_filename, report_filename, unhandled_data_filename, session)

    with open(report_filename, 'r') as report_file, open(tpl_data_filename, 'r') as tpl_data_file:
        report = json.load(report_file)
        tpl_data = json.load(tpl_data_file)
        for tpl_data_row, side_effect in zip(tpl_data, side_effects):
            if side_effect == {}:
                report_rows = list(filter(lambda report_row: report_row['EMAIL'] == tpl_data_row['EMAIL'], report))
                assert len(report_rows) == 1
                report_row, = report_rows
                assert 'MESSAGE_ID' in report_row
                assert 'ATTACHMENTS' not in report_row
                assert 'RESPONSE' in report_row
                assert report_row['RESPONSE'] == json.dumps(side_effect)
            else:
                pass

    return
