# coding=utf-8
import time

import pytest

from btestlib.constants import Services
from logger import get_logger
from simpleapi.data.defaults import Music
from simpleapi.steps import balance_steps as balance
from simpleapi.tests.music import emulator_steps as emulator

__author__ = 'fellow'

log = get_logger()

period = 10


def _periods_number(subs_plan):
    return len(subs_plan)


class Data(object):
    product_ids = [Music.product_id, 507126]  # два самых популярных эпловых продукта (TRUST-1429)


class TestIntegrationMusic(object):
    def check_subscription_prolongation(self, invoice_id, subs_plan):
        for _ in range(10):
            time.sleep(period / 2)
            check_info = balance.check_in_app_subscription(invoice_id=invoice_id)
            assert check_info

    @pytest.mark.parametrize('product_id', Data.product_ids, ids=lambda x: 'product_id={}'.format(x))
    def test_prolongation(self, product_id):
        user, client = balance.user_client(service_id=Services.MUSIC.id)
        transaction_id, receipt, _ = emulator.full_cycle(period=period,
                                                         subs_plan=Music.SubsPlan.PROLONG)
        resp = balance.music_full_payment_cycle(receipt=receipt, user=user,
                                                client=client, transaction_id=transaction_id,
                                                product_id=product_id)
        self.check_subscription_prolongation(resp['invoice_id'], Music.SubsPlan.PROLONG)

    def test_lapse(self):
        user, client = balance.user_client(service_id=Services.MUSIC.id)
        transaction_id, receipt, product_id = emulator.full_cycle(period,
                                                                  Music.SubsPlan.LAPSE)

        resp = balance.music_full_payment_cycle(receipt=receipt, user=user,
                                                client=client, transaction_id=transaction_id)
        self.check_subscription_prolongation(resp['invoice_id'], Music.SubsPlan.LAPSE)

        latest_receipt = emulator.verify(receipt)['latest_receipt_info']
        resp = balance.music_full_payment_cycle(receipt=latest_receipt, user=user,
                                                client=client,
                                                transaction_id=latest_receipt['transaction_id'])
        self.check_subscription_prolongation(resp['invoice_id'], Music.SubsPlan.LAPSE)

    def test_restore_same_user(self):
        user, client = balance.user_client(service_id=Services.MUSIC.id)

        transaction_id, receipt, product_id = \
            emulator.full_cycle(period, Music.SubsPlan.PROLONG)

        resp = balance.music_full_payment_cycle(receipt=receipt, user=user,
                                                client=client, transaction_id=transaction_id)
        self.check_subscription_prolongation(resp['invoice_id'], Music.SubsPlan.PROLONG)

        # product_id = emulator.subscribe(Music.USER, period, Music.SubsPlan.PROLONG)
        receipt = emulator.restore(Music.USER, product_id)['receipts'][0]
        emulator.verify(receipt)

        resp = balance.music_full_payment_cycle(receipt=receipt, user=user,
                                                client=client, transaction_id=transaction_id)
        self.check_subscription_prolongation(resp['invoice_id'], Music.SubsPlan.PROLONG)

    def test_restore_new_user(self):
        user, client = balance.user_client(service_id=Services.MUSIC.id)
        transaction_id, receipt, product_id = \
            emulator.full_cycle(period, Music.SubsPlan.PROLONG)

        resp = balance.music_full_payment_cycle(receipt=receipt, user=user,
                                                client=client, transaction_id=transaction_id)

        product_id = emulator.subscribe(Music.USER, period, Music.SubsPlan.PROLONG)
        receipt = emulator.restore(Music.USER, product_id)

    # basic sequence with new uid-client link
    def test_new_bind_with_invoice_wo_emu(self):
        user, client = balance.user_client(service_id=Services.MUSIC.id)
        balance.music_seq_with_invoice(client, user)

    # template for music prod sequence
    # def test_prod_music_sequence(self, user_client):
    #    user, client = user_client
    #    transaction_id, receipt, product_id = \
    #        emulator.full_cycle(period, Music.SubsPlan.PROLONG)
    #    balance.music_prod_seq(receipt=receipt, user=user,
    #                           client=client, transaction_id=transaction_id)

    # template for music_paystep
    # def test_music_paystep(self, user_client):
    #    user, client = user_client
    #    balance.music_paystep_cycle(client, user)

    """def test_prolongation_new_account(self, user_client):
        user, client = user_client
        transaction_id, receipt, product_id = \
            emulator.full_cycle(period, Music.SubsPlan.PROLONG)

        resp = balance.music_full_payment_cycle(receipt=receipt, user=user,
                                                client=client, transaction_id=transaction_id)

        user1 = user
        user1.uid = 330696869
        user1.login = 'balancesimpletestusr5'
        import pprint
        product_id = emulator.subscribe(Music.USER, period, Music.SubsPlan.PROLONG)
        time.sleep(period)
        balance.check_in_app_subscription(invoice_id=resp['invoice_id'])
        receipt = emulator.restore(Music.USER, product_id)
        pprint.pprint(balance.validate_app_store_receipt(receipt['receipts'][0], user=user1))"""


if __name__ == '__main__':
    pytest.main()
