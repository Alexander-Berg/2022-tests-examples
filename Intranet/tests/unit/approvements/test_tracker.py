from unittest.mock import patch, Mock

import pytest

from ok.approvements.tracker import (
    _fetch_ok_iframe_url,
    _fetch_approvement_uid_from_url,
    _fetch_comments_from_issue,
    set_approvement_tracker_comment_id,
    _fetch_approvement_uuid,
)
from ok.utils.strings import str_to_md5

from tests import factories as f


pytestmark = pytest.mark.django_db


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
def test_fetch_ok_iframe_url(data):
    assert _fetch_ok_iframe_url(data['text']) == data['expected']


@pytest.mark.parametrize('data', [
    pytest.param(
        {
            'url': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=terrmit&object_id=ISSUE-1&uid=123456'
            ),
            'expected': '123456',
        },
        id='with-get-params',
    ),
    pytest.param(
        {
            'url': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=terrmit&object_id=ISSUE-1&text=Привет!\nВы ок?&uid=123456'
            ),
            'expected': '123456',
        },
        id='newline-in-get-params',
    ),
    pytest.param(
        {
            'url': (
                'https://ok.test.yandex-team.ru/tracker?_embedded=1'
                '&author=terrmit&object_id=ISSUE-1'
            ),
            'expected': None,
        },
        id='no-uid-in-get-params',
    ),
    pytest.param(
        {
            'url': 'https://ok.test.yandex-team.ru',
            'expected': None,
        },
        id='no-get-params',
    ),
])
def test_fetch_approvement_uid_from_url(data):
    assert _fetch_approvement_uid_from_url(data['url']) == data['expected']


@pytest.mark.parametrize('data', [
    {  # 0. В тексте только наш iframe
        'text': (
            '{{iframe src="https://ok.test.yandex-team.ru/approvements/'
            '12345678-1234-1234-1234-1234abcdef09/?_embedded=1" '
            'frameborder=0 width=100% height=400px}}'
        ),
        'expected': '12345678-1234-1234-1234-1234abcdef09',
    },
    {  # 1. В тексте наш iframe и другой текст
        'text': (
            'Запущено согласование в сервисе ((https://wiki.yandex-team.ru/intranet/ok/ OK)):\n'
            '{{iframe src="https://ok.test.yandex-team.ru/approvements/'
            '1234d678-1234-12a4-12e4-1274abcdef09/?_embedded=1" '
            'frameborder=0 width=100% height=400px}}\n'
            'Еще какой-то бесполезный текст, с **вики**-разметкой, ((# ссылками)), '
            'фигурными скобками {{}} и т.д.'
        ),
        'expected': '1234d678-1234-12a4-12e4-1274abcdef09',
    },
    {  # 2. iframe есть, но не наш
        'text': '{{iframe src="https://staff/"}}',
        'expected': None,
    },
    {  # 3. Текст без iframe
        'text': 'Просто какой-то текст',
        'expected': None,
    },
])
def test_fetch_approvement_uuid(data):
    assert _fetch_approvement_uuid(data['text']) == data['expected']


@pytest.mark.parametrize('uid,found', (
    ('uid', 1),
    ('uid-100', 0),
))
@patch('ok.approvements.tracker.client.issues')
@patch('ok.approvements.tracker._fetch_ok_iframe_url', lambda x: 'url')
def test_fetch_comments_from_issue_by_uid(mocked_issues, uid, found):
    issue_key = 'ISSUE-1'
    approvement = f.ApprovementFactory(object_id=issue_key, uid=str_to_md5('uid'))
    mocked_issues[issue_key].comments.get_all.return_value = [Mock()]

    with patch('ok.approvements.tracker._fetch_approvement_uid_from_url', lambda x: uid):
        result = list(_fetch_comments_from_issue(approvement))
    assert len(result) == found


@pytest.mark.parametrize('uuid,found', (
    ('12345678-1234-1234-1234-12345678abcd', 1),
    ('abcdef12-1234-4321-1234-12345678abcd', 0),
))
@patch('ok.approvements.tracker.client.issues')
@patch('ok.approvements.tracker._fetch_ok_iframe_url', lambda x: None)
def test_fetch_comments_from_issue_by_uuid(mocked_issues, uuid, found):
    issue_key = 'ISSUE-1'
    approvement = f.ApprovementFactory(
        object_id=issue_key,
        uuid='12345678-1234-1234-1234-12345678abcd',
    )
    mocked_issues[issue_key].comments.get_all.return_value = [Mock()]

    with patch('ok.approvements.tracker._fetch_approvement_uuid', lambda x: uuid):
        result = list(_fetch_comments_from_issue(approvement))
    assert len(result) == found


@patch('ok.approvements.tracker.logger.warning')
@patch('ok.approvements.tracker._fetch_comments_from_issue')
@patch('ok.approvements.tracker.str_to_md5', lambda x: x)
def test_set_approvement_comment_id_not_found(mocked_fetch, mocked_warning):
    """
    Если не нашли коммент, логгируем и выходим
    """
    mocked_fetch.return_value = []
    approvement = f.ApprovementFactory()

    set_approvement_tracker_comment_id(approvement)
    mocked_warning.assert_called_once_with(
        'Tracker comment for approvement %s was not found',
        approvement.id,
    )

    approvement.refresh_from_db()
    assert approvement.tracker_comment_short_id is None
    assert approvement.tracker_comment_id is None


@patch('ok.approvements.tracker._fetch_comments_from_issue')
@patch('ok.approvements.tracker.str_to_md5', lambda x: x)
def test_set_approvement_comment_id_single_found(mocked_fetch):
    """
    Если нашли один коммент, берем его
    """
    mocked_fetch.return_value = [Mock(id=1, longId='long-1')]
    approvement = f.ApprovementFactory(uid='uid-1')

    set_approvement_tracker_comment_id(approvement)
    approvement.refresh_from_db()
    assert approvement.tracker_comment_short_id == 1
    assert approvement.tracker_comment_id == 'long-1'


@patch('ok.approvements.tracker.logger.warning')
@patch('ok.approvements.tracker._fetch_comments_from_issue')
@patch('ok.approvements.tracker.str_to_md5', lambda x: x)
def test_set_approvement_comment_id_multiple_found(mocked_fetch, mocked_warning):
    """
    Если нашли несколько комментов, берем 0-й
    """
    mocked_fetch.return_value = [Mock(id=i, longId=f'long-{i}') for i in range(3)]
    approvement = f.ApprovementFactory(uid='uid')

    set_approvement_tracker_comment_id(approvement)
    mocked_warning.assert_called_once_with(
        'Found multiple Tracker comments for approvement %s',
        approvement.id,
    )

    approvement.refresh_from_db()
    assert approvement.tracker_comment_short_id == 0
    assert approvement.tracker_comment_id == 'long-0'
