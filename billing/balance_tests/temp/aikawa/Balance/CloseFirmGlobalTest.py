import datetime

from balance import balance_db as db
from btestlib import utils as ut


# class IntercompanyInvoice(object):


# # Turkey
# db.BalanceBO.execute()


def hide_all_intercompany_invoices():
    query_to_hide_previous_invoices = '''
    UPDATE (
      SELECT *
      FROM t_invoice i
      WHERE (EXISTS
      (SELECT t_extprops.object_id
       FROM t_extprops
       WHERE t_extprops.classname = 'Invoice'
             AND t_extprops.object_id = i.id
             AND t_extprops.attrname = 'crossfirm'
             AND t_extprops.value_num > 0)))
    SET hidden = 4'''
    db.balance().execute(query_to_hide_previous_invoices)


def change_command_of_generate_acts_task_for_intercompany_invoices(month=None):
    if not month:
        month = datetime.datetime.now()
    next_month, _ = ut.Date.next_month_first_and_last_days(month)
    month_to_command = next_month.strftime("%Y.%m")
    command = 'yb-python -pysupport cluster_tools/generate_acts.py --month {0} --task monthly_close_firms_enq --debug'.format(month_to_command)
    query_to_update_command_of_generate_acts_task = '''
    UPDATE (
      SELECT command
      FROM T_PYCRON_DESCR
      WHERE
        name = 'generate-acts')
    SET COMMAND = '{0}'
    '''.format(command)
    db.balance().execute(query_to_update_command_of_generate_acts_task)


def check_state_of_firms():
    query_to_check_state_of_firms = '''
    SELECT DISTINCT state
    FROM t_export
    WHERE classname = 'Firm' AND type = 'MONTH_PROC'
    '''
    result = db.balance().execute(query_to_check_state_of_firms)
    states_list = [x['state'] for x in result]
    return states_list


def terminate_generate_acts_task(states_list):
    if 0 in states_list:
        terminate_task('generate-acts')


def terminate_task(task_name, terminate_value=1):
    query_to_terminate_generate_acts_task = '''
        UPDATE (
          SELECT *
          FROM T_PYCRON_DESCR
          WHERE name = :task_name
        )
        SET TERMINATE = :terminate_value
    '''
    query_params = {'task_name': task_name, 'terminate_value': terminate_value}
    db.balance().execute(query_to_terminate_generate_acts_task, query_params)


def unterminate_task(task_name):
    terminate_task(task_name, terminate_value=0)


def update_state(classname, type):
    query_to_update_state_to_1 = '''
        UPDATE (
          SELECT *
          FROM T_EXPORT
          WHERE CLASSNAME = :classname AND TYPE = '{1}'
        )
        SET state = 1
    '''
    query_params = {'classname': classname}
    db.balance().execute(query_to_update_state_to_1)


def check_finished_value_of_tasks_process(task_name):
    query = '''
        SELECT finished
        FROM T_PYCRON_STATE WHERE id IN (
          SELECT state_id FROM V_PYCRON WHERE NAME = :task_name)
    '''
    query_params = {'task_name': task_name}
    result = db.balance().execute(query, query_params)
    finished_dt_list = [process['finished'] for process in result]
    return finished_dt_list

def wait_for_all_process_are_finished(task_name, timeout = 600, interval=5):
    timer = 0
    while timer < timeout:
        datetime.time.sleep(interval)
        timer += interval
        finished_dt_list = check_finished_value_of_tasks_process(task_name)
        for finished_dt in finished_dt_list:
            try:
                assert finished_dt_list < datetime.datetime.now()
            except AssertionError:
                continue

def generate_intercompany_invoices():
    terminate_task('month_proc-processor')
    wait_for_all_process_are_finished('month_proc-processor')
    terminate_task('generate-acts')
    wait_for_all_process_are_finished('generate-acts')
    update_state(classname='Firm', type='MONTH_PROC')
    # change_command_of_generate_acts_task_for_intercompany_invoices()
    # unterminate_task('generate-acts')

    # hide_all_intercompany_invoices()
    # states_list = check_state_of_firms()
    # stop_generating_intercompany_invoices(states_list)
#
#
# print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
#                            {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])


generate_intercompany_invoices()