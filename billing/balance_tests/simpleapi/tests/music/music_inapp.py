# coding: utf-8
'''
Base test class and utils for inapp.
TODO better to use pytest instead of unittest for these cases.
'''

__author__ = 'zhmyh'

import unittest
import xmlrpclib

from music_settings import (SERVER_URI,
    # TRUST_URI,
                            APPSTORE_VALID_RECEIPTS,
                            APPSTORE_VALID_PROCESSED_RECEIPTS,
                            APPSTORE_INVALID_RECEIPTS)
from simpleapi.tests.music.logger import get_logger

log = get_logger()


class TestInApp(unittest.TestCase):
    '''
    Base class for test cases.
    Common methods are defined here.
    '''

    def setUp(self):
        '''
        sets up test case settings
        '''
        self.methods = {'ValidateReceipt': 'ValidateHHStoreReceipt'}
        self.store = 'Horns and Hooves Store'
        self.server = xmlrpclib.ServerProxy(SERVER_URI)

    def _retrieve_valid_receipts(self):
        '''Retrieves from some repo valid store receipts'''
        return []

    def _retrieve_invalid_receipts(self):
        '''Retrieves from some repo invalid store receipts'''
        return []

    def _call_servant(self, method, *args):
        '''Call servant method by name with *args
        '''
        method = getattr(getattr(self.server, 'Balance'), method)
        return method(*args)

    def test_verify_valid_receipt(self):
        '''
        Tests verification of valid receipt from
        Some Store api docs.
        '''
        log.debug('*** {0} valid receipt verification ***'.format(self.store))

        for uid, receipt in self._retrieve_valid_receipts():
            resp = self._call_servant(self.methods['ValidateReceipt'],
                                      uid, receipt)

            log.debug('%s response: %r' % (self.methods['ValidateReceipt'],
                                           resp))
            self.assertEqual(resp[1], True, 'Validation faild')
        log.debug('%s works Ok with valid receipt' %
                  self.methods['ValidateReceipt'])

    def test_verify_invalid_receipt(self):
        '''
        Tests verification of invalid receipt from
        Microsoft Windows Store api docs.
        '''
        log.debug('*** {0} invalid receipt verification ***'.format(self.store))

        for uid, receipt in self._retrieve_invalid_receipts():
            resp = self._call_servant(self.methods['ValidateReceipt'],
                                      uid, receipt)

            log.debug('%s response: %r' % (self.methods['ValidateReceipt'],
                                           resp))
            self.assertEqual(resp[1], False, 'Validation faild')

        log.debug('%s works Ok with valid receipt' %
                  self.methods['ValidateReceipt'])


# class TestWinStoreInApp(TestInApp):
#     '''
#     WinStore InApp payments tests cases.
#     '''

#     def setUp(self):
#         '''
#         sets up test case settings
#         '''
#         super(TestWinStoreInApp, self).setUp()
#         self.store = 'Windows Store'
#         self.methods = {'ValidateReceipt': 'ValidateWinStoreReceipt'}

#     def _retrieve_valid_receipts(self):
#         '''Retrieves from some repo valid store receipts'''
#         return [(LOGIN1['uid'], receipt)
#                 for receipt in WINDOWS_PHONE_STORE_VALID_RECEIPTS]

#     def _retrieve_invalid_receipts(self):
#         '''Retrieves from some repo invalid store receipts'''
#         return [(LOGIN1['uid'], receipt)
#                 for receipt in WINDOWS_PHONE_STORE_INVALID_RECEIPTS]


class TestAppStoreInApp(TestInApp):
    '''
    Apptore InApp payments tests cases.
    '''

    def setUp(self):
        '''
        sets up test case settings
        '''
        super(TestAppStoreInApp, self).setUp()
        self.store = 'Apple Store'
        self.methods = {'ValidateReceipt': 'ValidateAppStoreReceipt',
                        'CheckSubscription': 'CheckInAppSubscription'}

    def _retrieve_valid_receipts(self):
        '''Retrieves from some repo valid store receipts'''
        return [(uid, receipt) for (uid, receipt) in APPSTORE_VALID_RECEIPTS]

    def _retrieve_invalid_receipts(self):
        '''Retrieves from some repo invalid store receipts'''
        return [(uid, receipt) for (uid, receipt) in APPSTORE_INVALID_RECEIPTS]

    def _retrieve_valid_processed_receipts(self):
        '''Retrieves from some repo valid processed store receipts'''
        return [(uid, receipt)
                for (uid, receipt) in APPSTORE_VALID_PROCESSED_RECEIPTS]

        # def test_validate_new_latest_receipt(self):
        #     '''
        #     Tests verification of valid new latest receipt.
        #     '''
        #     log.debug('*** {0} valid new latest receipt verification ***'.format(
        #             self.store))

        #     for uid, receipt in APPSTORE_VALID_NEW_LATEST_RECEIPTS:
        #         resp = self._call_servant(self.methods['ValidateReceipt'],
        #                                   uid, receipt)

        #         log.debug('%s response: %r' % (self.methods['ValidateReceipt'],
        #                                        resp))
        #         self.assertEqual(resp[1], True, 'Receipt is valid')
        #     log.debug('%s works Ok with valid new latest receipt' %
        #               self.methods['ValidateReceipt'])


        # def test_check_expired_inapp_subscription(self):
        #     '''
        #     Verifies expired subscription. Should return Expired = True.
        #     '''
        #     log.debug('*** Expired {0} subscription checking ***'.format(
        #             self.store))

        #     for invoice_id in APPSTORE_INVOICES_FOR_EXPIRED_ORDERS:
        #         resp = self._call_servant(self.methods['CheckSubscription'],
        #                                   {'InvoiceID': invoice_id,
        #                                    'ForceProdMode': True})

        #         log.debug('%s response: %r' % (self.methods['CheckSubscription'],
        #                                        resp))
        #         self.assertEqual(resp['Expired'], True,
        #                          'Subscription should be expired')
        #     log.debug('%s works Ok with expired orders' %
        #               self.methods['CheckSubscription'])

        # def test_check_active_inapp_subscription(self):
        #     '''
        #     Verifies active subscription. Should return Expired = False.
        #     '''
        #     log.debug('*** Active {0} subscription checking ***'.format(
        #             self.store))

        #     for invoice_id in APPSTORE_INVOICES_FOR_ACTIVE_ORDERS:
        #         resp = self._call_servant(self.methods['CheckSubscription'],
        #                                   {'InvoiceID': invoice_id,
        #                                    'ForceProdMode': True})

        #         log.debug('%s response: %r' % (self.methods['CheckSubscription'],
        #                                        resp))
        #         self.assertEqual(resp['Expired'], False,
        #                          'Subscription should be active')
        #     log.debug('%s works Ok with active orders' %
        #               self.methods['CheckSubscription'])


if __name__ == '__main__':
    unittest.main()
