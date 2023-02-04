import mock
from infra.dist.cacus.lib.notifications import notification
from infra.dist.cacus.lib.notifications import policy


def test_notification():
    n = notification.Notification(policy.Type.NEW, 'repo', 'subject', 'text', ['user@domain.tld'], lambda: None)
    assert n.type == policy.Type.NEW
    assert n.subject == 'subject'
    assert n.text == 'text'
    assert 'user@domain.tld' in n.rcpts
    assert n.repo == 'repo'
    assert n.policy is None


def test_extract_addr():
    assert notification.Notification._extract_addr('First Last <user@domain.tld>') == 'user@domain.tld'
    assert notification.Notification._extract_addr('<user@domain.tld>') == 'user@domain.tld'
    assert notification.Notification._extract_addr('user@domain.tld') == 'user@domain.tld'


def test_add_rcpt_s():
    n = notification.Notification(policy.Type.NEW, 'repo', 'subject', 'text', ['user@domain.tld'], lambda: None)
    assert 'user2@domain.tld' not in n.rcpts
    n.add_rcpt('user2@domain.tld')
    assert 'user2@domain.tld' in n.rcpts
    assert 'user3@domain.tld' not in n.rcpts
    assert 'user4@domain.tld' not in n.rcpts
    n.add_rcpts(['user3@domain.tld', 'user4@domain.tld'])
    assert 'user3@domain.tld' in n.rcpts
    assert 'user4@domain.tld' in n.rcpts


def test_send(monkeypatch):
    send = mock.Mock()
    n = notification.Notification(policy.Type.NEW, 'cacus', 'subject', 'text', ['user@domain.tld'])
    monkeypatch.setattr(n, '_send', send)
    n.send()
    n.send()
    send.assert_called_once_with('user@domain.tld')


def test_send_extra(monkeypatch):
    send = mock.Mock()
    n = notification.Notification(policy.Type.NEW, 'cacus', 'subject', 'text', ['user2@domain.tld'])
    monkeypatch.setattr(n, '_send', send)
    n.send()
    n.send()
    send.assert_called_once_with('user@domain.tld')

    def _p():
        p = policy.Policy()
        p.configure({'notify_on_success': True})
        return p

    n = notification.Notification(policy.Type.NEW, 'cacus', 'subject', 'text', ['user2@domain.tld'], _p)
    monkeypatch.setattr(n, '_send', send)
    n.send()
    n.send()
    assert send.call_count == 2
    send.assert_has_calls([mock.call('user@domain.tld'), mock.call('user2@domain.tld')], any_order=True)


def test_actual_send():
    send = mock.Mock()
    n = notification.Notification(policy.Type.NEW, 'cacus', 'subject', 'text', ['user@domain.tld'])
    n._send('abc@def.ghi', send)
    send.assert_called_once_with('dist@yandex-team.ru', 'abc@def.ghi', 'subject', 'text')
