import datetime
import time
from infra.dist.cacus.lib.notifications import factory
from infra.dist.cacus.lib.notifications import policy


def test_new_changes():
    now = time.time()
    n = factory.new_changes(
        'repo',
        'env',
        'pkg',
        now,
        ('/f1', '/f2'),
        'user@domain.tld',
    )
    now = datetime.datetime.fromtimestamp(now).strftime('%Y-%m-%d %H:%M:%S')
    assert n.type == policy.Type.NEW
    for i in (now, 'repo', 'env', 'pkg', 'f1', 'f2', 'user@domain.tld'):
        assert i in n.text
    assert 'user@domain.tld' in n.rcpts


def test_malformed_changes():
    now = time.time()
    n = factory.malformed_changes(
        'repo',
        'changes',
        'reason',
        now,
        ['user@domain.tld'],
    )
    now = datetime.datetime.fromtimestamp(now).strftime('%Y-%m-%d %H:%M:%S')
    assert n.type == policy.Type.MALFORMED
    for i in ('repo', 'changes', 'reason', now):
        assert i in n.text
    assert 'user@domain.tld' in n.rcpts


def test_reject_changes():
    now = time.time()
    n = factory.reject_changes(
        'repo',
        'changes',
        now,
        'reason',
        'user@domain.tld',
    )
    now = datetime.datetime.fromtimestamp(now).strftime('%Y-%m-%d %H:%M:%S')
    assert n.type == policy.Type.REJECT
    for i in ('repo', 'changes', 'reason', now):
        assert i in n.text
    assert 'user@domain.tld' in n.rcpts


def test_repeated_changes():
    now = time.time()
    n = factory.repeated_changes(
        'repo',
        'env',
        'pkg',
        now,
        ('/f1', '/f2'),
        'user@domain.tld',
    )
    now = datetime.datetime.fromtimestamp(now).strftime('%Y-%m-%d %H:%M:%S')
    assert n.type == policy.Type.REPEAT
    for i in ('repo', 'env', 'pkg', now, 'f1', 'f2'):
        assert i in n.text
    assert 'user@domain.tld' in n.rcpts


def test_conflict_changes():
    now = time.time()
    n = factory.conflict_changes(
        'repo',
        'pkg',
        'ver',
        'current',
        'target',
        now,
        'user@domain.tld',
    )
    now = datetime.datetime.fromtimestamp(now).strftime('%Y-%m-%d %H:%M:%S')
    assert n.type == policy.Type.CONFLICT
    for i in ('repo', 'pkg', now, 'current', 'target'):
        assert i in n.text
    assert 'ver' in n.subject
    assert 'user@domain.tld' in n.rcpts
