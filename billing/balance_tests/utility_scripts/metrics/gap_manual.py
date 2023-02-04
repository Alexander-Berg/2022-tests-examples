DATA = '''2018-03;140;120;160;60;40;20;20;20;0;40;160;20;0;40
2018-04;147;126;168;63;42;21;21;21;0;42;189;21;0;63
2018-05;140;120;160;60;40;20;20;20;0;40;180;20;0;60
2018-06;140;120;160;60;40;20;20;20;0;60;180;20;0;60
2018-07;154;154;176;66;44;22;22;22;0;66;196;22;0;66
2018-08;161;138;161;69;69;23;23;23;0;69;184;46;0;69
2018-09;140;120;140;40;60;20;20;20;0;60;180;40;0;60'''

KEYS = ['trust', 'partner', 'balance', 'bank_clients', 'ui', 'comm', 'reports', 'dcs', 'tel', 'apikeys',
        'test_balance', 'test_dbm', 'autotest_balance', 'autotest_trust']


def temp():
    ttl = []
    for row in DATA.split('\n'):
        stat = row.split(';')
        month, stats = stat[0], stat[1:]
        keyval = zip(KEYS, stats)
        str = ''
        for name, data in keyval:
            str += '["name": "{}", "metric": {}], '.format(name, data)
        str = str.replace('[', '{')
        str = str.replace(']', '}')
        row_final = '"time": "{}", "metrics": [{}]'.format(month, str[:-2])
        row_final = '{' + row_final + '}'
        ttl.append(row_final)
    final = '{"data": [' + ','.join(ttl) + '], "source": "balance_qa_metrics_gap"}'
    pass


temp()
