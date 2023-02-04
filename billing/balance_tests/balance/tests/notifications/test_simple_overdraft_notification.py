__author__ = 'sandyk'

import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from decimal import Decimal as D

SERVICE_ID = 7
OVERDRAFT_LIMIT = 100
MAIN_DT = datetime.datetime.now()

@pytest.mark.slow
@reporter.feature(Features.NOTIFICATION, Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-21679')
def test_simple_overdraft_notification():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    steps.CommonSteps.wait_and_get_notification(10, client_id, 1, timeout=420)
    # lim1 = steps.CommonSteps.parse_notification(10, client_id, 0, 'args', 'overdraft_limit')
    lim1 = steps.CommonSteps.parse_notification2(10, client_id,
                                                 list_of_keys_or_indexes=[0, 'args', 0, 'OverdraftLimit'])
    assert D(lim1) == D(0)
    steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
                                             currency=None)

    # TODO: commented after moving on sync MIGRATE_TO_CURRENCY processor. Delete this code when refactor
    # client_params = {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
    #                  'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
    #                  'SERVICE_ID': SERVICE_ID, 'CURRENCY_CONVERT_TYPE': 'MODIFY'}
    # client_id = steps.ClientSteps.create(client_params)

    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY',
                                          dt=datetime.datetime.now() + datetime.timedelta(seconds=5),
                                          service_id=SERVICE_ID, region_id='225', currency='RUB')

    # TODO: commented after moving on sync MIGRATE_TO_CURRENCY processor. Delete this code when refactor
    # db.balance().execute(
    #     'update (select * from t_export where object_id = :client_id and CLASSNAME = \'Client\' and type = \'MIGRATE_TO_CURRENCY\') set priority=-1',
    #     {'client_id': client_id})
    # steps.CommonSteps.restart_pycron_task('client_migrate_to_currency')
    #
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :client_id and CLASSNAME = \'Client\' and type = \'MIGRATE_TO_CURRENCY\'',
    #     {'client_id': client_id}, 1)
    reporter.log('migration to currency finished')

    steps.CommonSteps.wait_and_get_notification(10, client_id, 2, timeout=420)
    # overdraft_limit = steps.CommonSteps.parse_notification(10, client_id, 1, 'info', 'overdraft_limit')
    overdraft_limit = steps.CommonSteps.parse_notification2(10, client_id,
                                                            list_of_keys_or_indexes=[0, 'args', 0, 'OverdraftLimit'])
    if D(overdraft_limit) != D('3000'):
        steps.CommonSteps.wait_and_get_notification(10, client_id, 3, timeout=420)
        # overdraft_limit = steps.CommonSteps.parse_notification(10, client_id, 2, 'info', 'overdraft_limit')
        overdraft_limit = steps.CommonSteps.parse_notification2(10, client_id, list_of_keys_or_indexes=[0, 'args', 0,
                                                                                                        'OverdraftLimit'])


    reporter.log(overdraft_limit)
    return overdraft_limit


if __name__ == "__main__":
    # pytest.main("simple_overdraft_notification.py")
    begin = datetime.datetime.now()
    overdraft_limit = test_simple_overdraft_notification()
    reporter.log('total time: %s' % (datetime.datetime.now() - begin))
    assert overdraft_limit == '3000'

