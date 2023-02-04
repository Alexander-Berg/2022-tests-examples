from infra.ya_salt.lib import policy


def test_fail_fast_policy():
    p = policy.FailFast()

    p.add_success('foo')
    assert not p.is_violated()

    p.add_failure('bar')
    assert p.is_violated()


def test_fail_permissive_policy():
    p = policy.FailPermissive()

    p.add_success('foo')
    assert not p.is_violated()

    p.add_failure('bar')
    assert not p.is_violated()
