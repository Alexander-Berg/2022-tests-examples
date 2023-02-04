# select
# ts.name_id
# src_name_id,
# ts.name
# src_parent_name,
# ts.td_shift
# src_td_shift,
# ts.td_duration
# src_td_duration,
# td.name_id
# dst_name_id,
# td.name
# dst_parent_name,
# td.td_shift
# dst_td_shift,
# td.td_duration
# dst_td_duration
# from mnclose.t_tasks ts
#
# join
# MNCLOSE.T_TASK_RELATIONS r
# on ts.id = r.task_source_id
# join
# mnclose.t_tasks
# td
# on
# r.task_destination_id = td.id
# where
# ts.is_active = 1 and td.is_active = 1
# order by ts.name_id, td.name_id
# ;

data = open('/Users/aikawa/Work/MNCLOSE/actual_graph.tsv', 'r')
headers = ['SRC_NAME_ID', 'SRC_PARENT_NAME', 'SRC_TD_SHIFT', 'SRC_TD_DURATION', 'DST_NAME_ID', 'DST_PARENT_NAME',
           'DST_TD_SHIFT', 'DST_TD_DURATION']
task_list = []
for line in data:
    task_list.append(dict(zip(headers, line.split('||'))))
for task in task_list:
    # print '{0} -> {1}'.format(task['SRC_PARENT_NAME'], task['DST_PARENT_NAME'])
    print '{0} -> {1}'.format(task['SRC_NAME_ID'], task['DST_NAME_ID'])
    # if task['DST_NAME_ID'] == '172800':
    #     print task

file_header = ''
