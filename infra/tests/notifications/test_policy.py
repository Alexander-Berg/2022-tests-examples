from infra.dist.cacus.lib.notifications import policy


def test_policy():
    p = policy.Policy()
    assert p.valid() is False
    p.configure({})
    assert p.valid() is True


def test_extra_rcpts():
    p = policy.default_policy()
    # mocked in conftest.mock_common via cacus.yaml
    assert p.extra_rcpts(policy.Type.NEW, 'cacus') == ['user@domain.tld']


def test_allowed_extra():
    p = policy.default_policy()
    assert p.allowed(policy.Type.NEW, 'cacus', 'user@domain.tld')


def test_allow_new():
    p = policy.Policy()
    p.configure({})
    assert p.allowed(policy.Type.NEW, 'cacus', 'user@domain.tld') is False
    p.configure({'notify_on_success': True})
    assert p.allowed(policy.Type.NEW, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_success': False})
    assert p.allowed(policy.Type.NEW, 'cacus', 'user@domain.tld') is False


def test_allow_reject():
    p = policy.Policy()
    p.configure({})
    assert p.allowed(policy.Type.REJECT, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_reject': True})
    assert p.allowed(policy.Type.REJECT, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_reject': False})
    assert p.allowed(policy.Type.REJECT, 'cacus', 'user@domain.tld') is False


def test_allow_conflict():
    p = policy.Policy()
    p.configure({})
    assert p.allowed(policy.Type.CONFLICT, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_conflict': True})
    assert p.allowed(policy.Type.CONFLICT, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_conflict': False})
    assert p.allowed(policy.Type.CONFLICT, 'cacus', 'user@domain.tld') is False


def test_allow_malformed():
    p = policy.Policy()
    p.configure({})
    assert p.allowed(policy.Type.MALFORMED, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_malformed': True})
    assert p.allowed(policy.Type.MALFORMED, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_malformed': False})
    assert p.allowed(policy.Type.MALFORMED, 'cacus', 'user@domain.tld') is False


def test_allow_repeat():
    p = policy.Policy()
    p.configure({})
    assert p.allowed(policy.Type.REPEAT, 'cacus', 'user@domain.tld') is False
    p.configure({'notify_on_repeat': True})
    assert p.allowed(policy.Type.REPEAT, 'cacus', 'user@domain.tld') is True
    p.configure({'notify_on_repeat': False})
    assert p.allowed(policy.Type.REPEAT, 'cacus', 'user@domain.tld') is False
