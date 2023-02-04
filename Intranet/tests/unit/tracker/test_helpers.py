import pytest

from unittest.mock import patch

from ok.tracker.helpers import (
    get_macro_iframe_url,
    fetch_ok_iframe_tracker_url,
    replace_url_in_macro_body,
)


@pytest.mark.parametrize('params, expected', (
    ({}, 'https://ok.test.yandex-team.ru/tracker?_embedded=1'),
    ({'scenario': 'abc'}, 'https://ok.test.yandex-team.ru/tracker?_embedded=1&scenario=abc'),
    ({'a': '1', 'b': '2'}, 'https://ok.test.yandex-team.ru/tracker?_embedded=1&a=1&b=2'),
))
@patch('ok.tracker.helpers.TRACKER_MACRO_MAIN_QUERY', '_embedded=1')
def test_get_macro_iframe_url(params, expected):
    assert get_macro_iframe_url(params) == expected


@pytest.mark.parametrize('data', [
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=terrmit&object_id=ISSUE-1&uid=123456" '
                'frameborder=0 width=100% height=400px}}'
            ),
            'expected': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=terrmit&object_id=ISSUE-1&uid=123456'
            ),
        },
        id='ok-iframe-only',
    ),
    pytest.param(
        {
            'text': (
                'Запущено согласование в сервисе ((https://wiki.yandex-team.ru/intranet/ok/ OK)):\n'
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&users=qazaq&users=agrml&users=tmalikova'
                '&author=terrmit'
                '&object_id=ISSUE-1'
                '&uid=123456" '
                'frameborder=0 width=100% height=400px}}\n'
                'Еще какой-то бесполезный текст, с **вики**-разметкой, ((# ссылками)) и т.д.'
            ),
            'expected': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&users=qazaq&users=agrml&users=tmalikova'
                '&author=terrmit'
                '&object_id=ISSUE-1'
                '&uid=123456'
            ),
        },
        id='ok-iframe-with-other-text',
    ),
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=tmalikova'
                '&object_id=ISSUE-1'
                '&uid=123456" '
                'frameborder=0 width=100% height=400px}} '
                'и ещё пара слов о том, как хороши кавычки ", '
                'фигурные скобочки {{}} и жадные регулярки'
            ),
            'expected': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=tmalikova'
                '&object_id=ISSUE-1'
                '&uid=123456'
            ),
        },
        id='text-with-curly-brackets',
    ),
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=qazaq&object_id=ISSUE-1&uid=123456&text=Привет!\nВы ок?" '
                'frameborder=0 width=100% height=400px}}'
            ),
            'expected': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=qazaq&object_id=ISSUE-1&uid=123456&text=Привет!\nВы ок?'
            ),
        },
        id='ok-iframe-with-newline',
    ),
    pytest.param(
        {
            'text': '{{iframe src="https://staff/"}}',
            'expected': None,
        },
        id='non-ok-iframe',
    ),
    pytest.param(
        {
            'text': 'Просто какой-то текст',
            'expected': None,
        },
        id='text-only',
    ),
])
def test_fetch_ok_iframe_tracker_url(data):
    assert fetch_ok_iframe_tracker_url(data['text']) == data['expected']


@pytest.mark.parametrize('data', [
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=terrmit&object_id=ISSUE-1&uid=123456" '
                'frameborder=0 width=100% height=400px}}'
            ),
            'expected': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&scenario=scenario" '
                'frameborder=0 width=100% height=400px}}'
            ),
        },
        id='ok-iframe-only',
    ),
    pytest.param(
        {
            'text': (
                'Запущено согласование в сервисе ((https://wiki.yandex-team.ru/intranet/ok/ OK)):\n'
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&users=qazaq&users=agrml&users=tmalikova'
                '&author=terrmit'
                '&object_id=ISSUE-1'
                '&uid=123456" '
                'frameborder=0 width=100% height=400px}}\n'
                'Еще какой-то бесполезный текст, с **вики**-разметкой, ((# ссылками)) и т.д.'
            ),
            'expected': (
                'Запущено согласование в сервисе ((https://wiki.yandex-team.ru/intranet/ok/ OK)):\n'
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&scenario=scenario" '
                'frameborder=0 width=100% height=400px}}\n'
                'Еще какой-то бесполезный текст, с **вики**-разметкой, ((# ссылками)) и т.д.'
            ),
        },
        id='ok-iframe-with-other-text',
    ),
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=tmalikova'
                '&object_id=ISSUE-1'
                '&uid=123456" '
                'frameborder=0 width=100% height=400px}} '
                'и ещё пара слов о том, как хороши кавычки ", '
                'фигурные скобочки {{}} и жадные регулярки'
            ),
            'expected': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&scenario=scenario" '
                'frameborder=0 width=100% height=400px}} '
                'и ещё пара слов о том, как хороши кавычки ", '
                'фигурные скобочки {{}} и жадные регулярки'
            ),
        },
        id='text-with-curly-brackets',
    ),
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=qazaq&object_id=ISSUE-1&uid=123456&text=Привет!\nВы ок?" '
                'frameborder=0 width=100% height=400px}}'
            ),
            'expected': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&scenario=scenario" '
                'frameborder=0 width=100% height=400px}}'
            ),
        },
        id='ok-iframe-with-newline',
    ),
    pytest.param(
        {
            'text': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=<%currentUser.login%>'
                '&object_id=<%issue.key%>'
                '&uid=<%currentDateTime.iso8601%>'
                '&text=Привет!\nВы ок?" '
                'frameborder=0 width=100% height=400px}}'
            ),
            'expected': (
                '{{iframe src="https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&scenario=scenario" '
                'frameborder=0 width=100% height=400px}}'
            ),
        },
        id='ok-iframe-with-tracker-params',
    ),
    pytest.param(
        {
            'text': '{{iframe src="https://staff/"}}',
            'expected': '{{iframe src="https://staff/"}}',
        },
        id='non-ok-iframe',
    ),
    pytest.param(
        {
            'text': 'Просто какой-то текст',
            'expected': 'Просто какой-то текст',
        },
        id='text-only',
    ),
])
@pytest.mark.parametrize('new_url', (
    'https://ok.test.yandex-team.ru/tracker?_embedded=1&scenario=scenario',
))
def test_update_ok_iframe_tracker_url(new_url, data):
    assert replace_url_in_macro_body(data['text'], new_url) == data['expected']
