# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance.features import Features

pytestmark = [reporter.feature(Features.NOTIFICATION),
              pytest.mark.tickets('TESTBALANCE-1618')]


##Описание рассылок: https://wiki.yandex-team.ru/balance/no/Oracle/#raspisaniezapuskov

@pytest.mark.parametrize('notify',
                         [
                             'manager_contract_notify',
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('manager_contract_pr_notify'),
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('overdraft_notify'),
                             'signed_notify',
                             'int_dstrbtn_nonres_signed',
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('int_good_debt'),
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('client_contract_pr_notify'),
                             'contract_notify',
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('overdraft_payment_notify'),
                             'partner_contract_notify',
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('partner_nonres_signed_notify'),
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('credit_not_payed_notify'),
                             pytest.mark.skip(reason=u'не укладываемся в таймаут')('credit_notify'),
                             'partner_changed_paysys_notify',
                             'contract_notify_non_res',
                         ])
def test_plsql_notify(notify):
    db.balance().execute("begin notify.{}(); end;".format(notify))
