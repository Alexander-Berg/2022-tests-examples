# -*- coding: utf-8 -*-
import datetime
import pickle
import pprint

import btestlib.reporter as reporter
from btestlib.secrets import get_secret, Tokens
from utility_scripts.startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'chihiro', u'igogor', u'blubimov', u'fellow', u'slppls',
           u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

# OLD_TOKEN = 'c654ff06d1f04f05808cb85f58e98db2'
ST_TOKEN = get_secret(*Tokens.PIPELINER_OAUTH_TOKEN)


# ST_TOKEN = '1b092553764a412587eb15279198475c'
#
def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def get_metrics(base_dt, output):
    month_stat = dict()
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    base_dt = base_dt.strftime('%Y-%m-%d')

    ##    query = 'queue: Баланс AND Fix Version: "2.40"'
    query = u'''queue: Баланс
        AND (Updated: >= {0} OR Created: >= {0})
        AND ((type: !Bug type: !"Sub-bug" AND Status: !New Status: !Open Status: !CVS Status: !"In Progress")
        OR (type: Bug OR type: "Sub-bug"))
        AND Type: !Deployment Type: !DDL
        AND (summary: !"хотфикс" AND summary: !"релиз")
        AND ("Internal Design": !"Да" AND "Test Scope": !"Нет" AND Security: !"Да" AND Security: !"Yes")
        AND (Resolution: !Duplicate Resolution: !"Won't fix" Resolution: !"Will Not Fix" Resolution: !"Can't reproduce" Resolution: !"Invalid")'''
    query = query.format(base_dt)
    tickets = client.issues.find(query)

    unknown = []
    counter = 0
    for ticket in tickets:
        counter += 1
        reporter.log((u'{0} from {1}: {2}'.format(counter, len(tickets), ticket.key)))
        try:
            # For tickets search for the DATE of LAST "to testing\cvs" transition.
            # If transition was made by [TESTERS]: don't use it, unless it is single transition.
            # If no transition "to testing\cvs" were found: use DATE of LAST "to close" transition
            if ticket.type.key != u'bug' and ticket.type.key != u'sub-bug':
                curr_str = None
                curr_login = None
                weak_curr_str = None
                weak_login = None
                close_str = None
                close_login = None
                # Travers thru changelog
                for item in ticket.changelog:
                    # Search for 'to testing' and 'to cvs' transitions:
                    if item.fields and item.fields[0][u'field'].id == u'status' \
                            and item.fields[0][u'to'].key in (u'testing', u'cvs'):
                        # If transition made by [TESTERS]: save the LAST date for no other 'to testing' transitions case
                        if item.updatedBy.login in TESTERS:
                            weak_curr_str = item.updatedAt[0:7]
                            weak_login = item.updatedBy.login
                        # Use LAST date "to testing" transition
                        else:
                            curr_str = item.updatedAt[0:7]
                            curr_login = item.updatedBy.login
                        reporter.log((u'\t\t{} : curr_str: {} by {:<20} | weak: {} by {}').format(ticket.key, curr_str,
                                                                                                  curr_login,
                                                                                                  weak_curr_str,
                                                                                                  weak_login))
                    # Search for 'to closing' transitions:
                    if item.fields and item.fields[0][u'field'].id == u'status' \
                            and item.fields[0][u'to'].key in (u'closed'):
                        close_str = item.updatedAt[0:7]
                        close_login = item.updatedBy.login
                # If no "to testing\cvs" transition by [OTHERS]
                if not curr_str:
                    # If no "to testing\cvs" transition by [TESTERS]: use date of closing
                    if not weak_curr_str:
                        curr_str = close_str
                        reporter.log((u'------------------------- {} : {}').format(curr_str, close_login))
                        unknown.append(ticket.key)
                    # If have "to testing\cvs" transition by [TESTERS]: use date of this transition
                    else:
                        curr_str = weak_curr_str
                if curr_str and curr_str not in month_stat:
                    month_stat[curr_str] = {'tasks': [], 'issues': [], 'bugs': []}
                if curr_str and curr_str in month_stat:
                    month_stat[curr_str]['tasks'].append((ticket, ticket.summary))
            else:
                curr_str = ticket.createdAt[0:7]
                if curr_str not in month_stat:
                    month_stat[curr_str] = {'tasks': [], 'issues': [], 'bugs': []}
                if curr_str and curr_str in month_stat:
                    ##                    pos = 'issues' if ticket.createdBy.login in TESTERS else 'bugs'
                    pos = 'issues' if ticket.stage == u'Testing' else 'bugs'
                    month_stat[curr_str][pos].append((ticket, ticket.summary))
        except:
            reporter.log((u'Exception with: {0}'.format(ticket.id)))

    reporter.log(unknown)

    f1 = file(output, 'w')
    pckl = pickle.dumps(month_stat)
    f1.write(pckl)
    f1.close()


def reduce_data(input, output):
    f = open(input, 'r')
    pickled_struct = f.read()
    struct = pickle.loads(pickled_struct)

    # # Code to debug any period data
    # for ticket in struct['2015-05']['tasks']:
    #     for item in ticket[0].changelog:
    #         if item.fields and item.fields[0][u'field'].id == u'status' \
    #         and item.fields[0][u'to'].key == u'testing':
    #             Print(ticket[0])
    #             Print(ticket[1])
    #             Print(item.updatedAt)
    #             Print(u'{0} {1} {2}'.format(ticket[0], item.updatedAt, ticket[1]))

    new = dict()
    mn_counter = 0
    for mn in struct:
        mn_counter += 1
        new[mn] = {'tasks': [], 'issues': [], 'bugs': []}
        # Tasks
        counter = 0
        for task in struct[mn]['tasks']:
            counter += 1
            reporter.log((u'Period {0} ({1} from {2}): {3} from {4} tasks'.format(mn, mn_counter, len(struct), counter,
                                                                                  len(struct[mn]['tasks']))))
            cnt_to_testing = 0
            for change in task[0].changelog:
                if change.fields and change.fields[0][u'field'].id == u'status' \
                        and change.fields[0][u'to'].key == u'testing':
                    cnt_to_testing += 1
            new[mn]['tasks'].append({'key': task[0].key, 'summary': task[0].summary, \
                                     'is_trust': 1 if (task[0].createdBy.login in TRUST_DEV or (
                                         task[0].assignee and task[0].assignee.login in TRUST_DEV)) \
                                         else 0, 'reopen_count': cnt_to_testing})
        # Issues
        counter = 0
        for task in struct[mn]['issues']:
            counter += 1
            reporter.log((u'Period {0} ({1} from {2}): {3} from {4} issues'.format(mn, mn_counter, len(struct), counter,
                                                                                   len(struct[mn]['issues']))))
            new[mn]['issues'].append({'key': task[0].key, 'summary': task[0].summary, \
                                      'is_trust': 1 if task[0].assignee and task[0].assignee.login in TRUST_DEV else 0, \
                                      'method': task[0].bugDetectionMethod, 'tags': task[0].tags})
        # Bugs
        counter = 0
        for task in struct[mn]['bugs']:
            counter += 1
            reporter.log((u'Period {0} ({1} from {2}): {3} from {4} bugs'.format(mn, mn_counter, len(struct), counter,
                                                                                 len(struct[mn]['bugs']))))
            new[mn]['bugs'].append({'key': task[0].key, 'summary': task[0].summary, \
                                    'is_trust': 1 if task[0].assignee and task[0].assignee.login in TRUST_DEV else 0, \
                                    'method': task[0].bugDetectionMethod,
                                    'status': 0 if task[0].status.key in [u'new', u'open', u'need_info'] else 1})

    f1 = file(output, 'w')
    pckl = pickle.dumps(new)
    f1.write(pckl)
    f1.close()


def analyze_data(data_file):
    f = open(data_file, 'r')
    pickled_struct = f.read()
    reduced = pickle.loads(pickled_struct)
    f.close()

    general_data = {}
    for mn in reduced:
        task_cnt = 0
        is_trust_cnt = 0
        reopened_array = []
        for task in reduced[mn]['tasks']:
            task_cnt += 1
            is_trust_cnt += task['is_trust']
        ##            reopened_array.append (task['reopen_count'])
        if mn not in general_data:
            general_data[mn] = {'tasks': {}, 'issues': {}, 'bugs': {}}
        general_data[mn]['tasks']['ttl'] = task_cnt
        general_data[mn]['tasks']['trust'] = is_trust_cnt
        ##        general_data[mn]['tasks']['testing'] = reopened_array

        issue_cnt = 0
        mt_cnt = 0
        auto_cnt = 0
        is_trust_cnt = 0
        python_cnt = 0
        for issue in reduced[mn]['issues']:
            issue_cnt += 1
            is_trust_cnt += issue['is_trust']
            mt_cnt += 1 if issue['method'] == u'MT' else 0
            auto_cnt += 1 if issue['method'] == u'Autotests' else 0
            python_cnt += 1 if (issue['method'] == u'Autotests' and u'python_regression' in issue['tags']) else 0
        if mn not in general_data:
            general_data[mn] = {'tasks': {}, 'issues': {}, 'bugs': {}}
        general_data[mn]['issues']['ttl'] = issue_cnt
        general_data[mn]['issues']['trust'] = is_trust_cnt
        general_data[mn]['issues']['AUTO'] = auto_cnt
        general_data[mn]['issues']['MT'] = mt_cnt
        general_data[mn]['issues']['PY'] = python_cnt

        bug_cnt = 0
        is_trust_cnt = 0
        is_done_cnt = 0
        for bug in reduced[mn]['bugs']:
            bug_cnt += 1
            is_trust_cnt += bug['is_trust']
            is_done_cnt += bug['status']
        if mn not in general_data:
            general_data[mn] = {'tasks': {}, 'issues': {}, 'bugs': {}}
        general_data[mn]['bugs']['ttl'] = bug_cnt
        general_data[mn]['bugs']['trust'] = is_trust_cnt
        general_data[mn]['bugs']['done'] = is_done_cnt

    final_stats = []
    final_formatted = []
    general_add = {}
    for mn in general_data:
        quart_str = u'{0}-Q{1}'.format(mn[0:4], (int(mn[5:7]) - 1) // 3 + 1)
        if quart_str not in general_add:
            general_add[quart_str] = {'ttl': 0, 'tasks_ttl': 0, 'issues_ttl': 0, \
                                      'mt_cnt': 0, 'py_cnt': 0, 'auto_cnt': 0, 'bugs_ttl': 0, 'done_cnt': 0}

        tasks_ttl = general_data[mn]['tasks']['ttl']
        issues_ttl = general_data[mn]['issues']['ttl']
        bugs_ttl = general_data[mn]['bugs']['ttl']
        ttl = tasks_ttl + issues_ttl + bugs_ttl
        tasks_pct = round(tasks_ttl * 1.0 / ttl, 4) * 100
        issues_pct = round(issues_ttl * 1.0 / ttl, 4) * 100
        bugs_pct = round(bugs_ttl * 1.0 / ttl, 4) * 100
        mt_cnt = general_data[mn]['issues']['MT']
        py_cnt = general_data[mn]['issues']['PY']
        auto_cnt = general_data[mn]['issues']['AUTO']
        done_cnt = general_data[mn]['bugs']['done']
        if tasks_ttl > 0:
            done_pct = round(done_cnt * 1.0 / tasks_ttl, 4) * 100
        else:
            done_pct = 0.00

        general_add[quart_str]['ttl'] += ttl;
        general_add[quart_str]['tasks_ttl'] += tasks_ttl;
        general_add[quart_str]['issues_ttl'] += issues_ttl;
        general_add[quart_str]['mt_cnt'] += mt_cnt;
        general_add[quart_str]['py_cnt'] += py_cnt;
        general_add[quart_str]['auto_cnt'] += auto_cnt;
        general_add[quart_str]['bugs_ttl'] += bugs_ttl;
        general_add[quart_str]['done_cnt'] += done_cnt;

        # row = ('{0}: {1:>3} | {2:>3} {3:>8} | {4:>3} {5:>8} = {6:>8}| {7:>3} {8:>8} = {9:>6}    {10:>8}'.format(
        # mn, ttl, tasks_ttl, '({0}%)'.format(tasks_pct), issues_ttl, \
        # '({0}%)'.format(issues_pct), '{0}+{1}+{2}'.format(issues_ttl-(auto_cnt+mt_cnt), \
        # auto_cnt, mt_cnt), bugs_ttl, '({0}%)'.format(bugs_pct), \
        # '{0}+{1}'.format(done_cnt, bugs_ttl-done_cnt), '({0}%)'.format(done_pct)))
        #
        man_cnt = issues_ttl - (auto_cnt + mt_cnt)
        open_cnt = bugs_ttl - done_cnt
        row = (u'{0} {1} {2} {3} {4} {5} {6} {7} {8} {9} {10}'.format(mn, ttl, tasks_ttl, issues_ttl, man_cnt, auto_cnt, \
                                                                      py_cnt, mt_cnt, bugs_ttl, done_cnt, open_cnt))
        final_formatted.append(row)
        final_stats.append(
            (mn, ttl, tasks_ttl, issues_ttl, man_cnt, auto_cnt, py_cnt, mt_cnt, bugs_ttl, done_cnt, open_cnt))

    for mn in general_add:
        tasks_ttl = general_add[mn]['tasks_ttl']
        issues_ttl = general_add[mn]['issues_ttl']
        bugs_ttl = general_add[mn]['bugs_ttl']
        ttl = general_add[mn]['ttl']
        tasks_pct = round(tasks_ttl * 1.0 / ttl, 4) * 100
        issues_pct = round(issues_ttl * 1.0 / ttl, 4) * 100
        bugs_pct = round(bugs_ttl * 1.0 / ttl, 4) * 100
        mt_cnt = general_add[mn]['mt_cnt']
        py_cnt = general_add[mn]['py_cnt']
        auto_cnt = general_add[mn]['auto_cnt']
        done_cnt = general_add[mn]['done_cnt']
        if tasks_ttl > 0:
            done_pct = round(done_cnt * 1.0 / tasks_ttl, 4) * 100
        else:
            done_pct = 0.00

        # row = ('{0}: {1:>3} | {2:>3} {3:>8} | {4:>3} {5:>8} = {6:>8}| {7:>3} {8:>8} = {9:>6}    {10:>8}'.format(
        # mn, ttl, tasks_ttl, '({0}%)'.format(tasks_pct), issues_ttl, \
        # '({0}%)'.format(issues_pct), '{0}+{1}+{2}'.format(issues_ttl-(auto_cnt+mt_cnt), \
        # auto_cnt, mt_cnt), bugs_ttl, '({0}%)'.format(bugs_pct), \
        # '{0}+{1}'.format(done_cnt, bugs_ttl-done_cnt), '({0}%)'.format(done_pct)))
        #
        row = (u'{0} {1} {2} {3} {4} {5} {6} {7} {8}'.format(mn, ttl, tasks_ttl, issues_ttl, auto_cnt, \
                                                             py_cnt, mt_cnt, bugs_ttl, done_cnt))

        final_formatted.append(row)

    pass
    return final_stats


def get_keys_for_priority():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    queue = '''queue: Баланс AND type: Bug, Sub-bug AND Created: >= "01-10-2014"
    AND Status: !New Status: !Open
    AND Stage: !Testing
    AND (Resolution: !Duplicate Resolution: !"Won't fix" Resolution: !"Will Not Fix" Resolution: !"Can't reproduce" Resolution: !"Invalid")
    AND Internal Design: !"Да"  AND Test Scope: !"Нет"  AND (Security: empty() OR Security: "Нет")
    Sort by: created desc'''
    tickets = client.issues.find(queue)
    a = []
    for item in tickets:
        a.append((item.key, item.createdAt))
    return a


if __name__ == '__main__':
    get_metrics(datetime.datetime(2018, 3, 1), '2018_03_temp.dat')
    # reduce_data('2016_10_temp.dat', '2016_10_temp_reduced.dat')
    # analyze_data('2016_10_temp_reduced.dat')
    ##    plist = get_keys_for_priority ()
    pass
