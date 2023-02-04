from datetime import datetime
from balance.processors.stager.defaults.actors import store_nds_pct


def test_nds_pct(session, xmlrpcserver):
    actor = store_nds_pct()
    assert actor.send(datetime.strptime('2018-12-01', '%Y-%m-%d')) == '0.18'
    assert actor.send(datetime.strptime('2019-02-01', '%Y-%m-%d')) == '0.2'
