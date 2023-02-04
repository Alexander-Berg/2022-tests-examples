# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import balance.balance_db as db

# dt = datetime.datetime.today().strftime("%Y-%m-%d")
# print dt
# t = "select rate from T_CURRENCY_RATE_V2  where RATE_DT  = date'{0}' and BASE_CC = 'RUR' and CC='USD'".format(dt)
# print db.balance().execute(t)

# dt = datetime.datetime.today().strftime("%Y-%m-%d")
# print dt
# t = "select rate from T_CURRENCY_RATE_V2  where RATE_DT  = date'" + str(datetime.datetime.today().strftime("%Y-%m-%d")) + "' and BASE_CC = 'RUR' and CC='USD'"
# print db.balance().execute(t)


def get_rate_on_date(ccy, date=None):
    dt = (date or datetime.datetime.today()).strftime("%Y-%m-%d")
    select = "select rate from T_CURRENCY_RATE_V2  where RATE_DT  = date'{0}' and BASE_CC = 'RUR' and CC='{1}'".format(
        dt,
        ccy)
    result = db.balance().execute(select)
    if len(result) ==0: return 1
    else: return result[0]['rate']



print get_rate_on_date('RUR',datetime.datetime(2016,5,10))