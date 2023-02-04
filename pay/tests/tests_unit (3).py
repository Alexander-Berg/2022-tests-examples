
import pytest

from payplatform.balance_support_dev.tools.email_sender.common.lib.utils import (
    validate_subject, filter_for_debug, email_extractor, KEYWORDS
)


@pytest.mark.parametrize('subject_template, tpl_data, expectation, error', [
    *[('Test (тест) {{{{{KEYWORD}}}}}'.format(KEYWORD=KEYWORD), [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}
    ], pytest.raises(RuntimeError), 'Reserved keywords') for KEYWORD in sorted(KEYWORDS)],

    ('Test (тест) {{PARAM}}', [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}
    ], pytest.raises(RuntimeError), 'All subject template variables'),

    ('Test (тест) {{SUBJECT_PARAM}}', [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}
    ], pytest.raises(Exception), ' is undefined'),

    ('Test (тест) {{SUBJECT_PARAM}}', [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}
    ], pytest.raises(Exception), ' is undefined')
])
def test_invalid_validate_subject(subject_template, tpl_data, expectation, error):
    with expectation as exc:
        validate_subject(subject_template, tpl_data)

    assert error in str(exc)

    return


@pytest.mark.parametrize('subject_template, tpl_data', [
    ('Test (тест)', [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
        {'EMAIL': 'random2@ema.il', 'COL1': '1', 'COL2': '2'}
    ]),

    ('Test (тест) {{SUBJECT_PARAM}}', [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
        {'EMAIL': 'random2@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}
    ]),

    ('Test (тест) {{SUBJECT_PARAM}}', [
        {'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'SUBJECT_PARAM2': '4'},
        {'EMAIL': 'random2@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'SUBJECT_PARAM2': '4'}
    ])
])
def test_valid_validate_subject(subject_template, tpl_data):
    validate_subject(subject_template, tpl_data)

    return


@pytest.mark.parametrize('options, data, expectation, error', [
    ({'DEBUG': True, 'DEBUG_EMAILS': [], 'DEBUG_COUNT': 0},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set'),

    ({'DEBUG': True, 'DEBUG_COUNT': 0},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set'),

    ({'DEBUG': True, 'DEBUG_EMAILS': []},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set'),

    ({'DEBUG': True, 'DEBUG_EMAILS': [], 'DEBUG_COUNT': 1},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set'),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 0},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set'),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 0, 'DEBUG_FORCE_SPECIFIC_EMAILS': []},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set'),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_FORCE_SPECIFIC_EMAILS': []},
     [],
     pytest.raises(RuntimeError), 'Debug options should be set')
])
def test_invalid_filter_for_debug(options, data, expectation, error):
    with expectation as exc:
        filter_for_debug(options, data)

    assert error in str(exc)

    return


@pytest.mark.parametrize('options, data, expected_output', [
    ({'DEBUG': False},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}],
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2'}]),

    ({'DEBUG_ARGS_HIGHLIGHTING': True, 'DEBUG_ARGS_TO_HIGHLIGHT': []},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}],
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1, 'DEBUG_ARGS_HIGHLIGHTING': True,
      'DEBUG_ARGS_TO_HIGHLIGHT': []},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2'}],
     [{'EMAIL': 'test', 'COL1': '<span style="background-color: #FF9F9F">1</span>',
       'COL2': '<span style="background-color: #FF9F9F">2</span>'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1, 'DEBUG_ARGS_HIGHLIGHTING': True,
      'DEBUG_ARGS_TO_HIGHLIGHT': []},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test', 'COL1': '<span style="background-color: #FF9F9F">1</span>',
       'COL2': '<span style="background-color: #FF9F9F">2</span>', 'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1, 'DEBUG_ARGS_HIGHLIGHTING': True,
      'DEBUG_ARGS_TO_HIGHLIGHT': ['COL2']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '<span style="background-color: #FF9F9F">2</span>',
       'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1, 'DEBUG_ARGS_HIGHLIGHTING': True,
      'DEBUG_ARGS_TO_HIGHLIGHT': ['COL2', 'SUBJECT_PARAM']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '<span style="background-color: #FF9F9F">2</span>',
       'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1, 'DEBUG_ARGS_HIGHLIGHTING': False,
      'DEBUG_ARGS_TO_HIGHLIGHT': ['COL2']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1, 'DEBUG_ARGS_HIGHLIGHTING': False,
      'DEBUG_ARGS_TO_HIGHLIGHT': []},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': 'random2@ema.il'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': None}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'BCC': 'random2@ema.il'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'BCC': None}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test1', 'test2'], 'DEBUG_COUNT': 1},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test1', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
      {'EMAIL': 'test2', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test1', 'test2'], 'DEBUG_COUNT': 2},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}],
     [{'EMAIL': 'test1', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
      {'EMAIL': 'test2', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test1', 'test2'], 'DEBUG_COUNT': 2},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
      {'EMAIL': 'random2@ema.il', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6'}],
     [{'EMAIL': 'test1', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
      {'EMAIL': 'test1', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6'},
      {'EMAIL': 'test2', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3'},
      {'EMAIL': 'test2', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6'}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 0, 'DEBUG_FORCE_SPECIFIC_EMAILS': ['random@ema.il']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': 'hahn@ema.il'},
      {'EMAIL': 'random2@ema.il', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6', 'BCC': 'fraud@ema.il'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': None}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 2, 'DEBUG_FORCE_SPECIFIC_EMAILS': ['random@ema.il']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': 'hahn@ema.il'},
      {'EMAIL': 'random2@ema.il', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6', 'BCC': 'fraud@ema.il'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': None}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 0,
      'DEBUG_FORCE_SPECIFIC_EMAILS': ['random@ema.il', 'random2@ema.il']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': 'hahn@ema.il'},
      {'EMAIL': 'random2@ema.il', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6', 'BCC': 'fraud@ema.il'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': None},
      {'EMAIL': 'test', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6', 'BCC': None}]),

    ({'DEBUG': True, 'DEBUG_EMAILS': ['test'], 'DEBUG_COUNT': 1,
      'DEBUG_FORCE_SPECIFIC_EMAILS': ['random@ema.il', 'random2@ema.il']},
     [{'EMAIL': 'random@ema.il', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': 'hahn@ema.il'},
      {'EMAIL': 'random2@ema.il', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6', 'BCC': 'fraud@ema.il'}],
     [{'EMAIL': 'test', 'COL1': '1', 'COL2': '2', 'SUBJECT_PARAM': '3', 'CC': None},
      {'EMAIL': 'test', 'COL1': '4', 'COL2': '5', 'SUBJECT_PARAM': '6', 'BCC': None}])
])
def test_valid_filter_for_debug(options, data, expected_output):
    data = filter_for_debug(options, data)

    assert all(row in expected_output for row in data)
    assert all(row in data for row in expected_output)

    return


@pytest.mark.parametrize('line, expected_output', [
    (None, []),
    ('', []),
    (' ', []),
    ('random1@emai.il; random2@ema.il , random3@ema.il', ['random1@emai.il', 'random2@ema.il', 'random3@ema.il']),
    ('Random1  <random1@emai.il>; random2@ema.il', ['Random1 <random1@emai.il>', 'random2@ema.il']),
    ('  <random1@emai.il>; random2@ema.il', ['random1@emai.il', 'random2@ema.il'])
])
def test_valid_email_extractor(line, expected_output):
    assert email_extractor(line) == expected_output

    return
