
import pytest

from payplatform.balance_support_dev.tools.common.lib.utils import (
    fill_json, fill_text, fill_zip
)
from payplatform.balance_support_dev.tools.email_sender.checker.lib.main import (
    load_and_validate_template, load_and_validate_attachments, get_letter_data, check_uniqueness
)
from payplatform.balance_support_dev.tools.email_sender.common.lib.utils import KEYWORDS, get_attachment_data


@pytest.mark.parametrize('tpl, tpl_data, expectation, error', [
    *[('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}'
       ' and {{{{{KEYWORD}}}}}'.format(KEYWORD=KEYWORD), [{'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}],
       pytest.raises(RuntimeError), 'Reserved keywords') for KEYWORD in sorted(KEYWORDS)],

    ('Test letter (тестовое письмо) with params {{SUBJECT_COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'SUBJECT_COL0': '1', 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(RuntimeError), 'All body template variables'),

    ('Test letter (тестовое письмо) with params {{SUBJECT_COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': '1', 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(RuntimeError), 'All body template variables'),

    ('Test letter (тестовое письмо)', [], pytest.raises(RuntimeError), 'No recipients'),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 1, 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(RuntimeError), ' is not a string'),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': '', 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(RuntimeError), ' empty string'),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(Exception), ' is undefined'),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(RuntimeError), 'EMAIL field is mandatory in params'),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'ATTACHMENTS': 'unclassified something'}
    ], pytest.raises(RuntimeError), 'ATTACHMENTS field in template params')
])
def test_invalid_load_and_validate_template(tpl_filename, tpl_data_filename, tpl, tpl_data, expectation, error):
    fill_text(tpl_filename, tpl)
    fill_json(tpl_data_filename, tpl_data)

    with expectation as exc:
        load_and_validate_template(tpl_filename, tpl_data_filename)

    assert error in str(exc)

    return


@pytest.mark.parametrize('tpl, tpl_data, expected_output', [
    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ]),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ]),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'CC': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'CC': '', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ], [
        {'EMAIL': 'random@ema.il', 'CC': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'CC': '', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ]),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'CC': 'hahn@ema.il', 'BCC': 'bigbrother@ema.il',
         'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'CC': '', 'BCC': '121500', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ], [
        {'EMAIL': 'random@ema.il', 'CC': 'hahn@ema.il', 'BCC': 'bigbrother@ema.il',
         'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'CC': '', 'BCC': '121500', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ]),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'COL3': 'random bs', 'COL4': None},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20', 'COL3': 3.5, 'COL4': 1}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ]),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'SUBJECT_PARAM': 'test'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20', 'SUBJECT_PARAM': '3.5'}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'SUBJECT_PARAM': 'test'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20', 'SUBJECT_PARAM': '3.5'}
    ]),

    ('Test letter (тестовое письмо) with params {{COL0}}, {{COL1}}, {{COL2}}', [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2', 'COL3': 'random bs', 'COL4': None},
        {'EMAIL': 'definitely not email address', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20', 'COL3': 3.5, 'COL4': 1}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'definitely not email address', 'COL0': 'test1', 'COL1': '3.5', 'COL2': '20'}
    ])
])
def test_valid_load_and_validate_template(tpl_filename, tpl_data_filename, tpl, tpl_data, expected_output):
    fill_text(tpl_filename, tpl)
    fill_json(tpl_data_filename, tpl_data)

    template_data = load_and_validate_template(tpl_filename, tpl_data_filename)

    assert template_data == expected_output

    return


@pytest.mark.parametrize('attachments_markup, attachments_archive, expectation, error', [
    ([], [], pytest.raises(RuntimeError), 'Empty markup file'),

    ([
        {'DATA_ROWNUM': -1, 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'test.txt'}
    ], [], pytest.raises(RuntimeError), 'Empty attachments archive'),

    ([
        {'DATA_ROWNUM': -1, 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'test.txt'}
    ], [
        ('test2.txt', 'Test attachment (тестовое вложение)')
    ], pytest.raises(RuntimeError), 'Archive with attachments contains extra files'),

    ([
        {'DATA_ROWNUM': -1, 'FILENAME': 'test1.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'test1.txt'},
        {'DATA_ROWNUM': -1, 'FILENAME': 'test2.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'test2.txt'}
    ], [
        ('test1.txt', 'Test attachment (тестовое вложение)')
    ], pytest.raises(RuntimeError), 'Archive with attachments does not contain some files'),

    ([
        {'DATA_ROWNUM': 0, 'FILENAME': 'test1.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt'},
        {'DATA_ROWNUM': 0, 'FILENAME': 'test1.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt'}
    ], [
        ('Тест.txt', 'Test attachment (тестовое вложение)')
    ], pytest.raises(RuntimeError), 'Duplicate markup instances found')
])
def test_invalid_load_and_validate_attachments(attachments_markup_filename, attachments_archive_filename,
                                               attachments_markup, attachments_archive, expectation, error):
    fill_json(attachments_markup_filename, attachments_markup)
    fill_zip(attachments_archive_filename, attachments_archive)

    with expectation as exc:
        load_and_validate_attachments(attachments_markup_filename, attachments_archive_filename)

    assert error in str(exc)

    return


@pytest.mark.parametrize('attachments_markup, attachments_archive, expected_output', [
    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt'}
    ], [
        ('Тест.txt', 'Test attachment (тестовое вложение)')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': 'document/pdf', 'REAL_FILENAME': 'Тест.txt'}
    ], [
        ('Тест.txt', 'Test attachment (тестовое вложение)')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'document/pdf', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': 'document/pdf', 'REAL_FILENAME': 'Тест.bad_mime'}
    ], [
        ('Тест.bad_mime', 'Test attachment (тестовое вложение)')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'document/pdf', 'REAL_FILENAME': 'Тест.bad_mime',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.bad_mime'}
    ], [
        ('Тест.bad_mime', 'Test attachment (тестовое вложение)')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'application/octet-stream',
         'REAL_FILENAME': 'Тест.bad_mime', 'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt'}
    ], [
        ('Тест.txt', '')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': 'bad/mime', 'REAL_FILENAME': 'Тест.txt'}
    ], [
        ('Тест.txt', '')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'bad/mime', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': None, 'MIME_TYPE': 'bad/mime', 'REAL_FILENAME': 'Тест.txt'}
    ], [
        ('Тест.txt', '')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'Тест.txt', 'MIME_TYPE': 'bad/mime', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('')}
    ]),

    ([
        {'DATA_ROWNUM': '0', 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt'},
        {'DATA_ROWNUM': '0', 'FILENAME': 'test2.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест2.txt'}
    ], [
        ('Тест.txt', 'Test attachment # 1 (тестовое вложение № 1)'),
        ('Тест2.txt', 'Test attachment # 2 (тестовое вложение № 2)')
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')},
        {'DATA_ROWNUM': 0, 'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест2.txt',
         'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
    ]),
])
def test_valid_load_and_validate_attachments(attachments_markup_filename, attachments_archive_filename,
                                             attachments_markup, attachments_archive, expected_output):
    fill_json(attachments_markup_filename, attachments_markup)
    fill_zip(attachments_archive_filename, attachments_archive)

    markup = load_and_validate_attachments(attachments_markup_filename, attachments_archive_filename)

    assert markup == expected_output

    return


@pytest.mark.parametrize('template_data, attachments_data, expectation, error', [
    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]},
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': None, 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ], pytest.raises(RuntimeError), 'Collision error'),
])
def test_invalid_get_letter_data(template_data, attachments_data, expectation, error):
    with expectation as exc:
        get_letter_data(template_data, attachments_data)

    assert error in str(exc)

    return


@pytest.mark.parametrize('template_data, attachments_data, expected_output', [
    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]}
    ], [], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'DATA_ROWNUM': 1, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'DATA_ROWNUM': -1, 'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест.txt',
         'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment (тестовое вложение)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'DATA_ROWNUM': 1, 'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест1.txt',
         'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')},
        {'DATA_ROWNUM': 0, 'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест2.txt',
         'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], [
        {'DATA_ROWNUM': 0, 'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест1.txt',
         'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')},
        {'DATA_ROWNUM': -1, 'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain', 'REAL_FILENAME': 'Тест2.txt',
         'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
    ], [
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')},
             {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')}
         ]},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]}
    ])
])
def test_valid_get_letter_data(template_data, attachments_data, expected_output):
    data = get_letter_data(template_data, attachments_data)

    assert data == expected_output

    return


@pytest.mark.parametrize('letter_data, expectation, error', [
    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ], pytest.raises(RuntimeError), 'Duplicate letter instances found'),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]},
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]}
    ], pytest.raises(RuntimeError), 'Duplicate letter instances found')
])
def test_invalid_check_uniqueness(letter_data, expectation, error):
    with expectation as exc:
        check_uniqueness(letter_data)

    assert error in str(exc)

    return


@pytest.mark.parametrize('letter_data', [
    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]},
        {'EMAIL': 'hahn@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]},
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')}
         ]}
    ]),

    ([
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]},
        {'EMAIL': 'random@ema.il', 'COL0': 'test0', 'COL1': '1.5', 'COL2': '2',
         'ATTACHMENTS': [
             {'FILENAME': 'test1.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 1 (тестовое вложение № 1)')},
             {'FILENAME': 'test2.txt', 'MIME_TYPE': 'text/plain',
              'DATA': get_attachment_data('Test attachment # 2 (тестовое вложение № 2)')}
         ]}
    ])
])
def test_valid_check_uniqueness(letter_data):
    check_uniqueness(letter_data)
