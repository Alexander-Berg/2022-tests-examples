import unittest.mock as mock
import re
from infra.rtc.juggler.bundle.checks import ntp
from juggler.bundles import Status

OK = Status.OK
WARN = Status.WARN
CRIT = Status.CRIT


def test_collect_ntp_data__success():
    ntpq_ok = mock.Mock()
    ntpq_ok.stderr = ''
    ntpq_ok.stdout = 'ntpq OK test output'

    with mock.patch('infra.rtc.juggler.bundle.checks.ntp.run', side_effect=[ntpq_ok]):
        assert ntp.collect_ntp_data(('test',)) == {'test': 'ntpq OK test output'}


def test_collect_ntp_data__failure():
    with mock.patch('infra.rtc.juggler.bundle.checks.ntp.run', side_effect=[Exception('oops')]):
        event = ntp.juggler_check()
        assert Status.CRIT == event.status
        assert '"reason": "Exception: oops"' in event.description


def test_rv_decode():
    rv_raw = '''
associd=0 status=0628 leap_none, sync_ntp, 2 events, no_sys_peer,
version="ntpd 4.2.8p10@1.3728-o (1)", processor="x86_64",
system="Linux/4.15.0-64-generic", leap=00, stratum=3, precision=-23,
rootdelay=5.615, rootdisp=47.574, refid=95.108.129.183,
reftime=e13ee4df.a520e228  Wed, Oct  2 2019 11:57:03.645,
clock=e13ee6e9.6680f7fd  Wed, Oct  2 2019 12:05:45.400, peer=27810, tc=9,
mintc=3, offset=0.371635, frequency=-9.999, sys_jitter=0.351770,
clk_jitter=0.270, clk_wander=0.111, tai=37, leapsec=201701010000,
expire=201912280000
    '''.strip()
    # collected: ntpq -nc rv
    rv_parsed = ntp.parse_rv_output(rv_raw)
    for key in ('associd', 'status', 'rootdelay', 'rootdisp', 'refid', 'reftime', 'clock'):
        assert key in rv_parsed, 'No mandatory field {} in parsed rv data'.format(key)
    for key, value in rv_parsed.items():
        assert not value.endswith(','), 'Key: {} Value: Trailing comma must be stripped'.format(key)
        assert not value.endswith('"'), 'Key: {} Value: Trailing doublequota must be stripped'.format(key)
        assert not value.startswith('"'), 'Key: {} Value: Leading doublequota must be stripped'.format(key)
    assert rv_parsed['associd'] == '0'
    assert re.match(r'^[0-9a-f]{4} ', rv_parsed['status']) is not None


def test_as_decode():
    as_raw = '''
ind assid status  conf reach auth condition  last_event cnt
===========================================================
  1 27427  941a   yes   yes  none candidate              1
  2 27428  941a   yes   no   none candidate    sys_peer  1
  3 27429  931a   no    yes  none   outlier              1
  4 27430  961a   yes   yes  none  sys.peer    sys_peer  1
    '''.strip()
    # collected: ntpq -nc as

    as_parsed = ntp.parse_as_output(as_raw)
    for id, item in as_parsed.items():
        assert re.match(r'^[0-9a-f]{4}$', item[
            'status']) is not None, 'Field `status` must be hex in lower case without prefixes and postfixes, 4 symbols [0-9a-f]'
        assert re.match(r'^[0-9]{3,}$',
                        id) is not None, 'Field `association id` must be digit-only, more than 3 symbols length'
        assert isinstance(item['configured'], bool), 'Field `configured` must be boolean'
        assert isinstance(item['reachable'], bool), 'Field `reachable` must be boolean'

    assert as_parsed['27427']['condition'] == 'candidate'
    assert as_parsed['27428']['condition'] == 'candidate'
    assert as_parsed['27429']['condition'] == 'outlier'
    assert as_parsed['27430']['condition'] == 'sys.peer'
    assert all((
        as_parsed['27427']['configured'],
        as_parsed['27427']['reachable'],
        as_parsed['27428']['configured'],
        as_parsed['27429']['reachable'],
        as_parsed['27430']['configured'],
        as_parsed['27430']['reachable']
    ))
    assert all((
        not as_parsed['27428']['reachable'],
        not as_parsed['27429']['configured']
    ))


def test_sys_decode():
    sys_raw = '''
uptime:                 118
sysstats reset:         118
packets received:       21
current version:        13
older version:          0
bad length or format:   0
authentication failed:  0
declined:               0
restricted:             0
rate limited:           0
KoD responses:          0
processed for time:     13
    '''.strip()
    # collected: ntpq -nc sysstat

    sys_parsed = ntp.parse_sys_output(sys_raw)
    for key, value in sys_parsed.items():
        assert re.match(r'[ ]+:?$', key) is None
        assert re.match(r'^[0-9]+$', value) is not None


def test_decision():
    decision = ntp.make_decision(
        status={
            'uptime': '4000',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (
        OK, 'status: 0615 clock_sync; stratum: 4; offset: 0.1000; uptime: 4000'), "Tested good ntp condition"

    decision = ntp.make_decision(
        status={
            'uptime': '30',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (OK, 'warming up, '
                            'status: 0615 clock_sync; '
                            'stratum: 4; offset: 0.1000; uptime: 30'), "Tested 1 min warm up"

    decision = ntp.make_decision(
        status={
            'uptime': '3000',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (OK, 'status: 0615 clock_sync; stratum: 4; '
                            'offset: 0.1000; uptime: 3000'), "Tested <1 hour uptime"

    decision = ntp.make_decision(
        status={
            'uptime': '4000',
            'sysstats reset': '4000',
            'stratum': '14',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (CRIT, 'stratum is too high, '
                              'status: 0615 clock_sync; stratum: 14; '
                              'offset: 0.1000; uptime: 4000'), "Tested High stratum"

    decision = ntp.make_decision(
        status={
            'uptime': '4000',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0015 no_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (
        CRIT, 'no sync, status: 0015 no_sync; stratum: 4; offset: 0.1000; uptime: 4000'), "Tested no ntp sync alert"

    decision = ntp.make_decision(
        status={
            'uptime': '4000',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '901a', 'configured': True, 'reachable': True, 'condition': 'sel_reject'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (WARN, 'invalid peers detected, '
                              'status: 0615 clock_sync; stratum: 4; '
                              'offset: 0.1000; uptime: 4000'), "Tested rejected ntp server"

    decision = ntp.make_decision(
        status={
            'uptime': '4000',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (WARN, 'less than 2 candidates to sync server, '
                              'status: 0615 clock_sync; stratum: 4; '
                              'offset: 0.1000; uptime: 4000'), "Tested only 1 candidate (must be - 2 + sys.peer)"

    decision = ntp.make_decision(
        status={
            'uptime': '4000',
            'sysstats reset': '4000',
            'stratum': '4',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
        }
    )
    assert decision == (CRIT, 'no primary ntp sync server, '
                              'status: 0615 clock_sync; stratum: 4; '
                              'offset: 0.1000; uptime: 4000'), "Tested no sys.peer exist"

    decision = ntp.make_decision(
        status={
            'uptime': '3000',
            'sysstats reset': '3000',
            'stratum': '14',
            'status': '0615 clock_sync',
            'offset': '0.1000'
        },
        associations={
            '27427': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27428': {'status': '941a', 'configured': True, 'reachable': True, 'condition': 'candidate'},
            '27429': {'status': '931a', 'configured': True, 'reachable': True, 'condition': 'outlier'},
            '27430': {'status': '961a', 'configured': True, 'reachable': True, 'condition': 'sys.peer'},
        }
    )
    assert decision == (CRIT, 'stratum is too high, '
                              'status: 0615 clock_sync; stratum: 14; offset: 0.1000; uptime: 3000'
                        ), "Check alert concatenation and worst level set"
