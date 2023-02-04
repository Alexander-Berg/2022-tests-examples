from datetime import datetime
from balance.processors.stager.defaults.actors import store_currency_rates

def test_currency_rates(session, xmlrpcserver):
    actor = store_currency_rates()
    assert actor.send(
        date=datetime.strptime('2018-12-01', '%Y-%m-%d'),
        base='RUB',
        currencies=set(['RUB', 'USD', 'EUR']),
    ) == {'EUR': '75.7484', 'RUB': '1.0', 'USD': '66.5335'}
