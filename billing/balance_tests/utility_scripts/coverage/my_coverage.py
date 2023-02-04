# -*- coding: utf-8 -*-

import json
import os
import xmlrpclib
from decimal import Decimal as D
from decimal import ROUND_HALF_UP

import coverage

import balance.balance_api as api
import btestlib.reporter as reporter

PATH = 'C:\\torvald\\_TOOLS\\tools'

# TODO: cluster_tools, notifier - separate coverage should be developed
# ALLOWED = ['/balance/', '/cluster_tools/', '/medium/', '/muzzle/', '/notifier/']
ALLOWED = ['/balance/', '/medium/', '/muzzle/']

PY_MASK = '.py'

TRUST = ['apple_api.py', 'fastpay.py', 'google_api.py', 'microsoft_api.py', 'simple_service.py', 'yamoney_api.py',
         'yamusic_api.py']

BLACKLIST_DIR = ['balance/application/', 'balance/actions/transfers/', 'balance/exc/', 'balance/mailing/',
                 'balance/utils/', 'balance/rules/skulpt/', 'balance/simple_monitorings/']

BLACKLIST_FILES = ['balance/actions/acts/coverage_util.py', 'balance/actions/process_completions_old.py',
                   'balance/actions/t_on_err.py', 'balance/agency_bonus.py', 'balance/analyze_selects.py',
                   'balance/barcode.py', 'balance/bmonstat.py', 'balance/catalog_api.py', 'balance/cron.py',
                   'balance/check_account.py', 'balance/check_inn.py', 'balance/deco.py', 'balance/dictify.py',
                   'balance/exporter.py', 'balance/exporterxml.py', 'balance/get_operator_codes.py',
                   'balance/invoker.py', 'balance/log_analyzer.py', 'balance/mncloselib.py', 'balance/muzzle_util.py',
                   'balance/operability.py', 'balance/passport.py', 'balance/sa_util.py',
                   'balance/scheduled_procedure.py', 'balance/test_cmn.py', 'balance/util.py',
                   'balance/usercustomize.py', 'balance/version.py', 'medium/medium_servant.py']

BLACKLIST = BLACKLIST_DIR + BLACKLIST_FILES

D_2 = lambda x: D(x).quantize(D('.01'), rounding=ROUND_HALF_UP)
merge_lists = lambda a, b: list(set(a) | set(b))


def trust_filter(filepath):
    for item in TRUST:
        if item in filepath:
            return True
    return False


def blacklist_filter(filepath):
    for item in BLACKLIST:
        if item in filepath:
            return True
    return False


def allowed(filepath):
    for item in ALLOWED:
        if item in filepath:
            return True
    return False


class NodeStats(object):
    @staticmethod
    def percentage_calculation(total, used):
        if total > 0:
            return (D(used) / D(total) * D('100'))
        return D('0')

    def __init__(self, total, used):
        self.total = total
        self.used = used
        self.percentage = self.percentage_calculation(self.total, self.used)

    def __iadd__(self, other):
        self.total += other.total
        self.used += other.used
        self.percentage = self.percentage_calculation(self.total, self.used)
        return self

    def __repr__(self):
        # return 'Percentage: {}, total: {}, used: {}'.format(self.percentage, self.total, self.used)
        return '{}%'.format(D_2(self.percentage))


def tree_builder(items, struct, value):
    if len(items) > 0:
        item = items[0]
        if item in struct:
            _, running_total = tree_builder(items[1:], struct[item], value)
            struct['___value'] += running_total
        else:
            struct[item] = {}
            struct['___value'] = struct['___value'] if '___value' in struct else NodeStats(0, 0)
            struct['___diff'] = list()
            struct[item], running_total = tree_builder(items[1:], struct[item], value)
            struct['___value'] += running_total
            struct['___base'] = struct['___value']
        return struct, running_total
    else:
        struct['___value'] = value
        struct['___diff'] = list()
        struct['___base'] = struct['___value']
    return struct, struct['___value']


def tree_viewer(tree, lvl):
    def join_diff(diff):
        diff_history = ''
        for row in diff:
            diff_history += '{:>6} > '.format(D_2(row) if row != 0 else '')
        return diff_history

    def draw_progressbar(value):
        return '=' * int(round(value.percentage, -1) / 10)

    if '___diff' not in tree:
        reporter.log('Empty tree')
        return []

    diff_align = len(tree['___diff']) * 8

    # HEADER:
    # 53.20% >   53.20%   [=====.....]   TOTAL

    if not lvl:
        reporter.log(
            ('{:>7} > {:<' + str(diff_align) + '} {:>7}   [{:.<10}]   TOTAL   ({}\{})').format(tree['___base'],
                                                                                               join_diff(
                                                                                                     tree['___diff']),
                                                                                               tree['___value'],
                                                                                               draw_progressbar(
                                                                                                     tree['___value']),
                                                                                               tree['___value'].used,
                                                                                               tree['___value'].total
                                                                                               ))
        # DIVIDER:
        # -------------------------------------------------------------------------------------------------------------
        reporter.log('-' * 120)

    # BODY:
    #  52.10% >   52.10%   [=====.....]    balance   (18182\34895)
    #  79.81% >   79.81%   [========..]   .... actions   (3367\4219)
    # 100.00% >  100.00%   [==========]   ........ __init__.py   (4\4)
    #  94.44% >   94.44%   [=========.]   ........ act_create.py   (17\18)

    for item in sorted(tree.keys()):
        if item not in ('___value', '___diff', '___base'):
            reporter.log(('{:>7} > {:<' + str(diff_align) + '} {:>7}   [{:.<10}]   {} {}   ({}\{})').format(
                tree[item]['___base'],
                join_diff(tree[item]['___diff']),
                tree[item]['___value'],
                draw_progressbar(tree[item]['___value']),
                '....' * lvl,
                item,
                tree[item]['___value'].used,
                tree[item]['___value'].total
            ))
            tree_viewer(tree[item], lvl + 1)


def tree_differ(base, new):
    for new_item in new:
        if new_item not in base:
            base[new_item] = {'___base': NodeStats(0, 0), '___value': NodeStats(0, 0), '___diff': list()}
    for item in base:
        if item in new:
            if item == '___value':
                base['___diff'].append(new[item].percentage - base[item].percentage)
                base['___value'] = new['___value']
            elif item not in ('___diff', '___base'):
                tree_differ(base[item], new[item])
        else:
            base[item]['___diff'].append(base[item]['___value'].percentage * D('-1'))
            base[item]['___value'] = NodeStats(0, 0)
    return 1


def build_struct(coverage):
    struct = {}
    for filepath in coverage:
        if allowed(filepath) and not (trust_filter(filepath) or blacklist_filter(filepath)):
            total_lines = len(coverage[filepath]['executable_lines'])
            used_lines = len(coverage[filepath]['covered_lines'])
            value = NodeStats(total_lines, used_lines)
            splitted = filepath.split('/')[4:]
            struct, total = tree_builder(splitted, struct, value)
    return struct


def get_coverage(context, reset):
    return api.coverage().server.Coverage.Collect(context, reset)


def save_coverage_to_file(coverage, filename):
    jsoned = json.dumps(coverage)
    with open(filename, 'w') as f:
        f.write(jsoned)


def load_coverage_from_file(filename):
    with open(filename, 'r') as f:
        jsoned = f.read()
        return json.loads(jsoned)


def get_all_files():
    M_ALLOWED = [item.replace('/', '\\') for item in ALLOWED]

    allowed = lambda item: any(allowed_item in item for allowed_item in M_ALLOWED)
    comb = lambda lvl, item: '{0}\\{1}'.format(lvl, item)

    # Для директории path получаем все файлы c расширением PY_MASK
    return [comb(level[0], item) for level in os.walk(PATH)
            for item in level[2]
            if item.endswith(PY_MASK)
            and allowed(comb(level[0], item))]


def add_all_files_to_stats(cvg):
    files = get_all_files()
    all_files = {}
    ttl = len(files)
    for n, file in enumerate(files):
        reporter.log('{} of {}'.format(n + 1, ttl))
        name, executable, _, _, _ = coverage.Coverage().analysis2(file)
        node = '/usr/share/pyshared{}'.format(file.replace(PATH, '').replace('\\', '/'))
        all_files[node] = {'executable_lines': executable, 'covered_lines': []}

    for item in all_files:
        if item not in cvg:
            cvg[item] = all_files[item]
    return cvg


def get_all_coverages(endpoints, host, cvgs):
    md_url = endpoints['medium']
    md_context, md_reset = cvgs['medium']
    medium = xmlrpclib.ServerProxy(uri=md_url.format(host=host), allow_none=1, use_datetime=1)
    md = medium.Coverage.Collect(md_context, md_reset)[md_context]

    tb_url = endpoints['testbalance']
    tb_context, tb_reset = cvgs['testbalance']
    test_balance = xmlrpclib.ServerProxy(uri=tb_url.format(host=host), allow_none=1, use_datetime=1)
    tb = test_balance.Coverage.Collect(tb_context, tb_reset)[tb_context]

    mz_url = endpoints['muzzle']
    mz_context, mz_reset = cvgs['muzzle']
    muzzle = xmlrpclib.ServerProxy(uri=mz_url.format(host=host), allow_none=1, use_datetime=1)
    mz = muzzle.Coverage.Collect(mz_context, mz_reset)[mz_context]

    return md, tb, mz


def merge_coverage(base, other):
    ttl = base
    for item in other:
        if item in ttl:
            for key in ['covered_lines', 'executable_lines']:
                # ttl[item]['covered_lines'] = list(set(ttl[item]['covered_lines']) | set(other[item]['covered_lines']))
                # ttl[item]['executable_lines'] = list(set(ttl[item]['executable_lines']) | set(other[item]['executable_lines']))
                ttl[item][key] = merge_lists(ttl[item][key], other[item][key])
        else:
            ttl[item] = other[item]
    return ttl


if __name__ == "__main__":

    # Block 1
    #
    # context = 'testcontext'
    # coverage = get_coverage(context, False)
    #
    # filename = 'coverage_20161108T0700_all.txt'
    # save_coverage_to_file(coverage, filename)
    # coverage = load_coverage_from_file(filename)
    #
    # old = build_struct(coverage)
    # # tree_differ(old, new)
    # tree_viewer(old, 0)
    # ------------------------------------------------------------------------------------

    endpoints = {'medium': 'http://greed-{host}.yandex.ru:8002/coverage',
                 'testbalance': 'http://greed-{host}.yandex.ru:30702/coverage',
                 'muzzle': 'http://greed-{host}.yandex.ru:31101/coverage'
                 }

    host = 'tm1f'

    cvgs = {'medium': ('testcontext', False),
            'testbalance': ('testcontext', False),
            'muzzle': ('', False)
            }

    md, tb, mz = get_all_coverages(endpoints, host, cvgs)

    # dt = datetime.datetime.now().strftime('%Y%m%d')
    # dt = '20161125'
    # md = save_coverage_to_file('medium_{}_all.txt'.format(dt))
    # tb = save_coverage_to_file('testxmlrpc_{}_all.txt'.format(dt))
    # mz = save_coverage_to_file('muzzle_{}_all.txt'.format(dt))

    # Смешиваем покрытия из разных источников
    ttl = md
    for cvg in [tb, mz]:
        merge_coverage(ttl, cvg)

    # Подмешиваем покрытые строки к выполняемым, чтобы скомпенсировать разницу подходов к получения покрытия
    for item in ttl:
        ttl[item]['executable_lines'] = merge_lists(ttl[item]['executable_lines'], ttl[item]['covered_lines'])

    ttl = add_all_files_to_stats(ttl)
    reporter.log('Total files count: {}'.format(len(ttl)))
    full = build_struct(ttl)
    tree_viewer(full, 0)

    # jv = load_coverage_from_file('java_20161123.txt')
    # java = build_struct(jv)
    #
    # tree_differ(java, full)
    # tree_viewer(java, 0)
    pass
