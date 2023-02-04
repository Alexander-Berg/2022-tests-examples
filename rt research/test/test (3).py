import yql.library.fastcheck.python as fastcheck

import bannerland.monitoring.top_clients_monitor.lib.queries as queries


def check_query(query):
    query = 'USE hahn;\n\n' + query
    yql_errors = []
    res = fastcheck.check_program(
        query,
        cluster_mapping={'hahn': 'yt'},
        errors=yql_errors,
    )

    assert len(yql_errors) == 0, 'Errors: {}'.format('\n'.join(yql_error.message for yql_error in yql_errors))
    assert res


def test_yql_query_validation():
    input_table = '//home/input'
    output_table = '//home/output'
    contexttype = 8
    check_query(queries.yql_aggregate_clients_stats(input_table, output_table, contexttype))
    check_query(queries.yql_total_clients_expenses(input_table, output_table))

    clients_expenses = 41876.979
    fixed_top_clients_ids = [1291525]
    min_cost_ratio = 0.02
    entry_time = 1645776000
    check_query(queries.yql_top_clients(input_table, output_table, clients_expenses, fixed_top_clients_ids, min_cost_ratio, entry_time))

    old_top = output_table
    agr_log = '//tmp/aeou'
    max_days = 3
    cur_log_timestamp = entry_time
    check_query(queries.yql_update_old_top(old_top, agr_log, output_table, clients_expenses, max_days, min_cost_ratio, cur_log_timestamp))

    new_top = output_table
    check_query(queries.yql_combine_tops(old_top, new_top, output_table))

    stats_table = '//tmp/snth'
    target_domains = '//tmp/lrcg'
    check_query(queries.yql_append_domain(stats_table, target_domains, output_table))

    fs_banners = '//home/full_state'
    check_query(queries.yql_preprocess_full_state(fs_banners, output_table))

    full_state = fs_banners
    queue_table = '//tmp/zwvm'
    check_query(queries.yql_active_orders(full_state, queue_table, output_table))

    clients_stat = '//tmp/fdb'
    active_orders = '//tmp/qjk'
    check_query(queries.yql_final_top(clients_stat, active_orders, output_table))
