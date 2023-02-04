
from copy import deepcopy
from email.utils import getaddresses
import smtplib

from jinja2 import Environment, StrictUndefined
import pytest
from unittest import mock

from payplatform.balance_support_dev.tools.email_sender.common.lib.utils import (
    email_extractor
)
from payplatform.balance_support_dev.tools.email_sender.smtp_sender.lib.main import (
    build, send, get_email_policy
)


@pytest.mark.parametrize('options, tpl, data, expectation, error', [
    ({'FROM_NAME': 'Ran Dom', 'FROM_EMAIL': 'random@ema.il',
      'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
     pytest.raises(Exception), 'SUBJECT'),

    ({'SUBJECT': 'Test letter (тестовое письмо)', 'FROM_NAME': 'Ran Dom',
      'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
     pytest.raises(Exception), 'FROM_EMAIL'),

    ({'SUBJECT': 'Test letter (тестовое письмо)', 'FROM_NAME': 'Ran Dom', 'FROM_EMAIL': 'random@ema.il',
      'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5'},
     pytest.raises(Exception), ' is undefined'),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Ran Dom', 'FROM_EMAIL': 'random@ema.il', 'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'},
     pytest.raises(Exception), ' is undefined'),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}', 'DEBUG': True,
      'FROM_NAME': 'Ran Dom', 'FROM_EMAIL': 'random@ema.il', 'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'SUBJECT_PARAM1': 'test (тест)'},
     pytest.raises(Exception), 'Bad email address')
])
def test_invalid_build(options, tpl, data, expectation, error):
    env = Environment(undefined=StrictUndefined)
    policy = get_email_policy()
    with expectation as exc:
        build(options, data, tpl, env, policy)

    assert error in str(exc)

    return


@pytest.mark.parametrize('options, tpl, data', [
    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}', 'FROM_NAME': '', 'FROM_EMAIL': '',
      'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)'},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)'}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}', 'FROM_NAME': '', 'FROM_EMAIL': '',
      'DEBUG': True, 'REPLY_TO': []},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@yandex-team.ru; Ран Дом <random2@yandex-team.ru>, hahn@yandex-team.ru',
       'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'SUBJECT_PARAM1': 'test (тест)'},
      {'EMAIL': 'hahn@yandex-team.ru; Ран Дом <random2@yandex-team.ru>, hahn@yandex-team.ru',
       'COL0': 'test1', 'COL1': '3.5', 'COL2': '20', 'SUBJECT_PARAM1': 'test (тест)'}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}', 'FROM_EMAIL': ''},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)'},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)'}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il'},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)'},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)'}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Ран Дом', 'FROM_EMAIL': 'random@ema.il',
      'REPLY_TO': ['Ран Дом <random@ema.il>', 'hahn@ema.il']},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)'},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)'}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Ран Дом', 'FROM_EMAIL': 'random@ema.il',
      'REPLY_TO': ['Ран Дом <random@ema.il>', 'hahn@ema.il']},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'CC': 'Ран Дом <random@ema.il>, hahn@ema.il'},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'BCC': 'random@ema.il, Хан <hahn@ema.il>'}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il'},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': None, 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}])
])
def test_valid_build(options, tpl, data):
    env = Environment(undefined=StrictUndefined)
    policy = get_email_policy()
    for row in data:
        msg = build(options, row, tpl, env, policy)
        assert msg['Subject'] == env.from_string(options['SUBJECT']).render(**row)
        assert msg['From'] == '{} <{}>'.format(
            options.get('FROM_NAME', options['FROM_EMAIL']), options['FROM_EMAIL']).strip()
        assert email_extractor(msg['To']) == email_extractor(row['EMAIL'])
        assert email_extractor(msg.get('Reply-To', '')) == email_extractor(', '.join(options.get('REPLY_TO', [])))
        assert email_extractor(msg.get('Cc', '')) == email_extractor(row.get('CC', ''))
        assert email_extractor(msg.get('Bcc', '')) == email_extractor(row.get('BCC', ''))
        if row.get('ATTACHMENTS', []):
            msg_attachments = list(msg.iter_attachments())
            assert len(msg_attachments) == len(row['ATTACHMENTS'])
            for row_attachment, msg_attachment in zip(row['ATTACHMENTS'], msg_attachments):
                assert msg_attachment.is_attachment()
                assert msg_attachment.get_filename() == row_attachment['FILENAME']
                assert msg_attachment.get_content_type() == row_attachment['MIME_TYPE'] or 'application/octet-stream'
                assert msg_attachment.get_content_disposition() == 'attachment'
                assert msg_attachment['Content-Transfer-Encoding'] == 'base64'
                assert msg_attachment.get_payload() == row_attachment['DATA']
            assert msg.get_content_type() == 'multipart/mixed'
            parts = list(filter(lambda part: not part.is_attachment(), msg.iter_parts()))
            assert len(parts) == 1
            part, = parts
            assert part.get_content()[:-1] == env.from_string(tpl).render(**row)
        else:
            assert not list(msg.iter_attachments())
            assert msg.get_content_type() == 'text/html'
            assert msg['Content-Transfer-Encoding'] == 'base64'
            assert msg.get_content()[:-1] == env.from_string(tpl).render(**row)
        assert msg['Message-ID']

    return


@pytest.mark.parametrize('options, tpl, data, side_effects', [
    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Ран Дом', 'FROM_EMAIL': 'random@ema.il',
      'REPLY_TO': ['Ран Дом <random@ema.il>', 'hahn@ema.il'], 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 1.0},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'CC': 'Ран Дом <random@ema.il>, hahn@ema.il'},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'BCC': 'random@ema.il, Хан <hahn@ema.il>'}], [{}, {}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': '1.0'},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}], [{}, {}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 1.0},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [{'random2@ema.il': (404, b'error')}, {}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': -1.0},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [{'random2@ema.il': (404, b'error')}, {'hahn@ema.il': (404, b'error')}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 0, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 1.0},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random3@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [{'random2@ema.il': (404, b'error'), 'random@ema.il': (404, b'error'), 'hahn@ema.il': (404, b'error')},
      {'hahn@ema.il': (404, b'error')}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 1.0},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'CC': 'Ран Дом3 <random3@ema.il>',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'BCC': 'Ран Дом3 <random3@ema.il>',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [{'random3@ema.il': (404, b'error')}, {'random3@ema.il': (404, b'error')}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 0, 'SMTP_RETRY_INTERVAL': 0, 'SMTP_INTERVAL': 0},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [Exception(), {}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 0.5},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [[smtplib.SMTPException(), smtplib.SMTPException(), {}], {}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 0.5},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [[smtplib.SMTPException(), smtplib.SMTPException(), smtplib.SMTPException(), {}], {}]),

    ({'SUBJECT': 'Test letter (тестовое письмо) {{SUBJECT_PARAM1}}',
      'FROM_NAME': 'Rad Dom', 'FROM_EMAIL': 'random@ema.il', 'SMTP_HOST': '', 'SMTP_PORT': 25,
      'SMTP_RETRY_COUNT': 3, 'SMTP_RETRY_INTERVAL': 2, 'SMTP_INTERVAL': 0.5},
     'Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}',
     [{'EMAIL': 'random@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [
           {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''},
           {'FILENAME': 'test2.txt', 'MIME_TYPE': 'application/pdf', 'DATA': ''}
       ]},
      {'EMAIL': 'hahn@ema.il; Ран Дом <random2@ema.il>, hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20',
       'SUBJECT_PARAM1': 'test (тест)',
       'ATTACHMENTS': [{'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'DATA': ''}]}],
     [[smtplib.SMTPServerDisconnected(), smtplib.SMTPServerDisconnected(), Exception()], {}]),
])
def test_valid_send(session, options, tpl, data, side_effects):
    env = Environment(undefined=StrictUndefined)
    policy = get_email_policy()

    side_effect = []
    for side_effect_ in side_effects:
        if isinstance(side_effect_, list):
            side_effect.extend(side_effect_)
        else:
            side_effect.append(side_effect_)
    session.send_message.side_effect = side_effect
    data_copy = deepcopy(data)
    with mock.patch('payplatform.balance_support_dev.tools.email_sender.smtp_sender.lib.main.smtplib.SMTP.send_message', side_effect=side_effect):
        reports, nonsent_letters = send(options, data, tpl)
    for nonsent_letter in nonsent_letters:
        nonsent_letter['EMAIL'] = ', '.join(sorted(email_extractor(nonsent_letter['EMAIL'])))
    assert len(data_copy) == len(data) == len(side_effects) == len(reports)
    for row, row_, side_effect, report in zip(data_copy, data, side_effects, reports):
        msg = build(options, row, tpl, env, policy)
        assert email_extractor(report['EMAIL']) == email_extractor(msg['To'])
        assert email_extractor(report.get('CC', '')) == email_extractor(msg.get('Cc', ''))
        assert email_extractor(report.get('BCC', '')) == email_extractor(msg.get('Bcc', ''))
        if not side_effect:
            assert report['RESPONSE'] == '{}'
        else:
            if isinstance(side_effect, list):
                assert all(isinstance(se, Exception) or isinstance(se, dict) for se in side_effect)
                side_effect = side_effect[min(options['SMTP_RETRY_COUNT'] - 1, len(side_effect) - 1)]
            if isinstance(side_effect, dict):
                row_emails = email_extractor(msg['To'])
                failed_emails = set()
                for address in side_effect:
                    for row_email in row_emails:
                        row_name, row_address = getaddresses([row_email])[0]
                        if address == row_address:
                            failed_emails.add(row_email)
                if failed_emails:
                    assert row_ in nonsent_letters
                else:
                    assert row_ not in nonsent_letters
            elif isinstance(side_effect, Exception):
                assert row_ in nonsent_letters
            else:
                assert False
