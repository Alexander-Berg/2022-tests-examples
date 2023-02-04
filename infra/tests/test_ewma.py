from ya.infra.oops.agent.modules.ewma import EWMA


def test1():
    m = EWMA()
    res = m.dict()
    assert 'last' in res
    assert res['last'] == 0
