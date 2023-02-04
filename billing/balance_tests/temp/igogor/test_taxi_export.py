# coding: utf-8

import pytest
from hamcrest import anything

from balance import balance_db
from balance.balance_objects import Context
from btestlib import utils, matchers

context = Context().new()

pairs = [context.new(payment_id_branch=payment_id_branch, payment_id_tm=payment_id_tm, testname=testname)
         for payment_id_branch, payment_id_tm, testname in [
             (1032925701, 1032926850, 'test_taxi_payment[TAXI_RU_CONTEXT_UBER'),
             (1032925728, 1032926860, 'test_taxi_payment[TAXI_RU_CONTEXT_UBER_ROAMING'),
             (1032925781, 1032926864, 'test_taxi_payment[TAXI_KZ_CONTEXT_UBER'),
             (1032925892, 1032926872, 'test_taxi_payment[TAXI_KZ_CONTEXT_UBER_ROAMING'),
             (1032926144, 1032926917, 'test_taxi_payment[TAXI_UBER_BY_CONTEXT_UBER_ROAMING'),
             (1032926147, 1032926927, 'test_taxi_payment[TAXI_UBER_AZ_CONTEXT_UBER'),
             (1032926153, 1032926930, 'test_taxi_payment[TAXI_UBER_AZ_CONTEXT_UBER_ROAMING'),
         ]]


@pytest.mark.parametrize('ctx', [
    context.new(payment_id_branch=payment_id_branch, payment_id_tm=payment_id_tm, testname=testname)
    for payment_id_branch, payment_id_tm, testname in [
        (1033018670, 1033018829, 'test_taxi_payment[TAXI_RU_CONTEXT_TAXI'),
        # (1033018670, 1032926850, 'test_taxi_payment[TAXI_RU_CONTEXT_UBER'),
        # (1032925728, 1032926860, 'test_taxi_payment[TAXI_RU_CONTEXT_UBER_ROAMING'),
        # (1032925781, 1032926864, 'test_taxi_payment[TAXI_KZ_CONTEXT_UBER'),
        # (1032925892, 1032926872, 'test_taxi_payment[TAXI_KZ_CONTEXT_UBER_ROAMING'),
        # (1032926144, 1032926917, 'test_taxi_payment[TAXI_UBER_BY_CONTEXT_UBER_ROAMING'),
        # (1032926147, 1032926927, 'test_taxi_payment[TAXI_UBER_AZ_CONTEXT_UBER'),
        # (1032926153, 1032926930, 'test_taxi_payment[TAXI_UBER_AZ_CONTEXT_UBER_ROAMING'),
    ]], ids=lambda ctx: ctx.testname)
def test_thirdparty_difference(ctx):
    branch_payment = balance_db.balance().execute(
        'select * from T_THIRDPARTY_TRANSACTIONS where PAYMENT_ID = ' + str(ctx.payment_id_branch))[0]
    tm_payment = balance_db.balance().execute(
        'select * from T_THIRDPARTY_TRANSACTIONS where PAYMENT_ID = ' + str(ctx.payment_id_tm))[0]
    tm_payment.update(dict(contract_id=anything(), dt=anything(), id=anything(), order_id=anything(),
                           partner_id=anything(), payment_id=anything(), person_id=anything(),
                           transaction_id=anything(), transaction_dt=anything(),
                           trust_id=anything(), trust_payment_id=anything()))
    utils.check_that(branch_payment, matchers.equal_to_casted_dict(tm_payment))


@pytest.mark.parametrize('ctx', [
    context.new(payment_id_branch=payment_id_branch, payment_id_tm=payment_id_tm, testname=testname)
    for payment_id_branch, payment_id_tm, testname in [
        (1033018670, 1033018829, 'test_taxi_payment[TAXI_RU_CONTEXT_TAXI'),
        # (1033018670, 1032926850, 'test_taxi_payment[TAXI_RU_CONTEXT_UBER'),
        # (1032925728, 1032926860, 'test_taxi_payment[TAXI_RU_CONTEXT_UBER_ROAMING'),
        # (1032925781, 1032926864, 'test_taxi_payment[TAXI_KZ_CONTEXT_UBER'),
        # (1032925892, 1032926872, 'test_taxi_payment[TAXI_KZ_CONTEXT_UBER_ROAMING'),
        # (1032926144, 1032926917, 'test_taxi_payment[TAXI_UBER_BY_CONTEXT_UBER_ROAMING'),
        # (1032926147, 1032926927, 'test_taxi_payment[TAXI_UBER_AZ_CONTEXT_UBER'),
        # (1032926153, 1032926930, 'test_taxi_payment[TAXI_UBER_AZ_CONTEXT_UBER_ROAMING'),
    ]], ids=lambda ctx: ctx.testname)
def test_thirdparty_difference(ctx):
    branch_payment = balance_db.balance().execute(
        'select * from T_PAYMENT where ID = ' + str(ctx.payment_id_branch))[0]
    tm_payment = balance_db.balance().execute(
        'select * from T_PAYMENT where ID = ' + str(ctx.payment_id_tm))[0]
    # tm_payment.update(dict(contract_id=anything(), dt=anything(), id=anything(), order_id=anything(),
    #                        partner_id=anything(), person_id=anything(),
    #                        transaction_id=anything(), transaction_dt=anything(),
    #                        trust_id=anything(), trust_payment_id=anything()))
    utils.check_that(branch_payment, matchers.equal_to_casted_dict(tm_payment))
