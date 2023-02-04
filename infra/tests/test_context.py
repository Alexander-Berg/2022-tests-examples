from infra.ya_salt.lib import context, policy


def test_callback():
    disabled = False
    call_count = [0]

    def callback():
        call_count[0] += 1
        if disabled:
            return True, 'disabled'
        else:
            return False, ''

    ctx = context.CallbackCtx(callback)
    assert not ctx.done()
    assert call_count[0] == 1
    disabled = True
    assert ctx.done()
    assert call_count[0] == 2
    assert ctx.done()
    assert call_count[0] == 2
    assert ctx.error() == 'disabled'


def test_execution_policy():
    ctx = context.Ctx(policy=policy.FailFast())
    ctx.ok('one')
    assert not ctx.done()
    ctx.fail('two', 'second step failed')
    assert ctx.done() and ctx.error() == 'execution policy violated: second step failed'
    ctx.fail('three', 'too late, ctx already in "done" state, this message will be ignored')
    assert ctx.done() and ctx.error() == 'execution policy violated: second step failed'
