# coding: utf-8

import balance.balance_db as db
import btestlib.reporter as reporter
import maintenance.test_clear_notifications_script as cn


def test_clear_ton():
    cn.test_clear_ton()


def test_clear_notifications_log():
    cn.test_clear_notifications_log()
