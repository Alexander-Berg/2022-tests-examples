# -*- coding: utf-8 -*-

import datetime

import yt.wrapper as yt

from common import read_table, create_yt_client

cluster_to_read = 'hahn'
cluster_to_write = 'arnold'

tables_list = [
    'backend_duty_stat',
    'frontend_duty_stat',
    'qa_team_stat',
    'testing_tickets_month_stat',
    'autotests_queue_size',
    'balanceduty_queue_size',
    'doc_for_assessors_queue_size',
    'qa_front_balance_stat',
    'review_month_stat',
    'review_status_by_groups',
    'test_cases_ai_stat',
    'test_cases_ci_stat',
    'testing_month_stat',
    'testing_status',
    'testing_tickets_month_stat',
]

base_url = "//home/balance-test/metrics"


def backup_to_arnold():
    client_a = create_yt_client(cluster='arnold')
    today = datetime.datetime.today().strftime('%Y-%m-%d')
    dir_path = base_url + '/' + today
    client_a.mkdir(dir_path)

    for table in tables_list:
        table_path_to_read = base_url + '/' + table
        table_path_to_write = dir_path + '/' + table
        data = read_table(table_path_to_read)
        client_a.write_table(table_path_to_write, data, format=yt.JsonFormat())


if __name__ == "__main__":
    backup_to_arnold()
