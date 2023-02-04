# -*- coding: utf-8 -*-

import re
import pprint


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


def data_struct():
    data = dict()
    f = open('hot_fixes.dat')
    f.readline()
    for line in f.readlines():
        if line != '\n':
            # 2.39 (10): 1
            if line and line[0] != '\t':
                release = re.search('2\.\d?\d?', line)
                if release:
                    release = int(release.group()[2:].replace('\n', ''))
                    data[release] = dict()
            if line and line[0] == '\t' and line[1] != '\t':
                # ____BALANCE-20699
                if line[1] == 'B':
                    hot_fix = re.search('(BALANCE-\d*)', line)
                    if hot_fix:
                        hot_fix = hot_fix.group().replace('\n', '')
                        data[release][hot_fix] = dict()
                # ____1	[TRUST]	[BALANCE-20672] - comment
                if line[1] != 'B':
                    hot_fix = 'Unknown hot-fix'
                    if not data[release].has_key(hot_fix):
                        data[release][hot_fix] = dict()
                    splitted = line[1:].split('\t')
                    group = splitted[1].replace('\n', '')[1:-1]
                    if not group in data[release][hot_fix]:
                        data[release][hot_fix][group] = dict()
                        data[release][hot_fix][group]['count'] = 0
                        data[release][hot_fix][group]['tickets'] = []
                    data[release][hot_fix][group]['count'] += int(splitted[0].replace('\n', ''))
                    if len(splitted) == 3:
                        data[release][hot_fix][group]['tickets'].append(splitted[2].replace('\n', ''))
                        ##                    if len(splitted) == 4:
                        ##                        data[release][hot_fix][group]['comment'] = splitted[3].replace('\n', '')
            # ________1	[bug]	[BALANCE-20672] - comment
            if line and line[0:2] == '\t\t' and line[2] != '\t':
                splitted = line[2:].split('\t')
                group = splitted[1].replace('\n', '')[1:-1]
                data[release][hot_fix][group] = dict()
                data[release][hot_fix][group]['count'] = splitted[0].replace('\n', '')
                if len(splitted) == 3:
                    data[release][hot_fix][group]['tickets'] = splitted[2].replace('\n', '')
                if len(splitted) == 4:
                    data[release][hot_fix][group]['comment'] = splitted[3].replace('\n', '')

    return data


def stat_by_type(_type):
    items_in_release = dict()
    for release in data:
        items_in_release_cnt = 0
        items_in_release_tkt = []
        for hot_fix in data[release]:
            if data[release][hot_fix].has_key(_type):
                items_in_release_cnt += int(data[release][hot_fix][_type]['count'])
                if 'tickets' in data[release][hot_fix][_type]:
                    items_in_release_tkt.append(data[release][hot_fix][_type]['tickets'])
        items_in_release[release] = (items_in_release_cnt, items_in_release_tkt)
    return items_in_release


def total_count():
    total_cnt = dict()
    total = 0
    for release in data:
        ##        items_in_release_cnt = 0
        ##        items_in_release_tkt = []
        for hot_fix in data[release]:
            for issue_type in data[release][hot_fix]:
                if not total_cnt.has_key(issue_type):
                    total_cnt[issue_type] = 0
                total_cnt[issue_type] += int(data[release][hot_fix][issue_type]['count'])
    for item in total_cnt:
        total += total_cnt[item]

    total_pct = dict()
    for item in total_cnt:
        total_pct[item] = round(total_cnt[item] / (total / 100.0), 2)
        total_cnt[item] = '{0} ({1}%)'.format(total_cnt[item], total_pct[item])
    total_cnt['TOTAL'] = total

    print (total_cnt)


##            if data[release][hot_fix].has_key('bug'):
##                items_in_release_cnt += int(data[release][hot_fix][_type]['count'])
##                items_in_release_tkt.append(data[release][hot_fix][_type]['tickets'])
##        items_in_release[release] = (items_in_release_cnt, items_in_release_tkt)
##        return bugs_in_release


if __name__ == '__main__':
    data = data_struct()
    bugs_in_release = stat_by_type('bug')
    pass
