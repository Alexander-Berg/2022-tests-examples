import balance.balance_api


for i in xrange(0,3000):
    balance.balance_api.medium().GetPassportByLogin(0,'apikeys-autotest-{}'.format(i))