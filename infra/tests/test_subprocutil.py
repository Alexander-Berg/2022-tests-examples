from infra.ya_salt.lib import subprocutil


def test_tail_of():
    cases = [
        # buf, prefix, out
        ('', '', ''),
        ('*' * 90, 'prefix: ', 'prefix: ' + '*' * 90),
        ('*' * 300, 'prefix: ', 'prefix: ...' + '*' * 97),
    ]
    for buf, prefix, out in cases:
        assert subprocutil.tail_of(buf, prefix=prefix) == out


def test_check_out():
    # Check invalid executable
    out, err, status = subprocutil.check_output(['/ban/bin'], 10)
    assert out == ''
    assert err == ''
    assert status.ok is False
    assert status.message
    # Check timeout
    out, err, status = subprocutil.check_output(['/bin/sleep', '100'], 0.1)
    assert out == ''
    assert err == ''
    assert status.ok is False
    assert 'execution timed out after' in status.message
    # Test return bad code
    out, err, status = subprocutil.check_output(['/bin/false'], 5)
    assert out == ''
    assert err == ''
    assert status.ok is False
    assert 'execution failed with exit code: 1' in status.message
    # Test good result
    out, err, status = subprocutil.check_output(['/bin/true'], 5)
    assert out == ''
    assert err == ''
    assert status.ok is True
