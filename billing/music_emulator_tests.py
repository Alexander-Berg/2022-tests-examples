# -*- coding: utf-8 -*-
"""Music tests
"""
import time

import pytest

from appstore_cli import AppStoreClient
from logger import get_logger

log = get_logger()

STORE_URL = 'https://dmongo1f.yandex.ru:8020/store/api/v1/'

USER = {'_id': 'f6c5626e-23c6-46d0-a0c0-2c83269fc254'}
INAPP_TMPL = {"product_id": "io.drewnoff.subscriptions.%s",
              "title": "Simple InApp",
              "author": "DrewNoff",
              "autorenewable": True,
              "period": 10,
              "description": "Test InApp"}


class TestAppStoreInApps(object):
    def _crete_user_fixture(self, user):
        '''Registers user.
        '''
        resp = self.iface.register_user(user)
        assert 'value' in resp and resp['value'] == 'Registered$', \
            'Error while create user, response: %s' % resp

    def _subscribe(self, user, period, subscription_plan):
        '''Creates InApp autorenewable subscription with @period sec period.
        and subscribes @user according to @subscription_plan.
        '''
        product_id = INAPP_TMPL['product_id'] % str(time.time())
        inapp = dict(INAPP_TMPL)
        inapp['product_id'] = product_id
        inapp['period'] = period
        resp1 = self.iface.create_inapp(inapp)
        resp2 = self.iface.subscribe(user['_id'], product_id, subscription_plan)
        return product_id

    def setup_method(self, method):
        """Should restore only orig receipt at the beginning of subscription.
        """
        self.store_url = STORE_URL
        self.iface = AppStoreClient(self.store_url)
        self._crete_user_fixture(USER)

    def test_restore_orig_receipt(self):
        '''Should restore one receipt at the beginning
        '''
        user = USER
        log.debug('===test_restore_orig_receipt===')
        subs_plan = [{'value': 'Trial$'},
                     {'value': 'Paid$'},
                     {'value': 'Paid$'},
                     {'idle': 1.4}]

        product_id = self._subscribe(user, 10, subs_plan)
        receipts = self.iface.restore(user['_id'], product_id)['receipts']
        log.debug("restored receipts: %r", receipts)
        assert len(receipts) == 1

    def test_verify_orig_receipt(self):
        '''Should verify orig receipt.
        Last receipt should be equal current receipt.
        '''
        user = USER
        log.debug('===test_verify_orig_receipt===')
        subs_plan = [  # {'value': 'Trial$'},
            {'value': 'Paid$'},
            {'value': 'Paid$'},
            {'idle': 1.4}]
        product_id = self._subscribe(user, 10, subs_plan)
        receipt = self.iface.restore(user['_id'], product_id)['receipts'][0]
        log.debug("orig receipt: %r", receipt)
        resp = self.iface.verify(receipt)
        assert resp['status'] == 0
        assert resp['receipt']['transaction_id'] == receipt['transaction_id']
        assert resp['receipt']['transaction_id'] == resp['latest_receipt_info']['transaction_id']

    def test_verify_prolonged_subscription(self):
        '''Should retrieve next receipt after subscription prolongation.
        '''
        user = USER
        log.debug('===test_verify_prolonged_subscription===')
        subs_plan = [{'value': 'Trial$'},
                     {'value': 'Paid$'},
                     {'value': 'Paid$'},
                     {'idle': 1.4}]
        product_id = self._subscribe(user, 3, subs_plan)
        orig_receipt = self.iface.restore(user['_id'], product_id)['receipts'][-1]
        time.sleep(3)
        resp = self.iface.verify(orig_receipt)
        assert resp['status'] == 0
        latest_receipt = resp['latest_receipt_info']
        receipt = resp['receipt']
        assert receipt['transaction_id'] == orig_receipt['transaction_id']
        assert receipt['transaction_id'] != latest_receipt['transaction_id']
        assert receipt['original_transaction_id'] == orig_receipt['transaction_id']


if __name__ == '__main__':
    pytest.main()
