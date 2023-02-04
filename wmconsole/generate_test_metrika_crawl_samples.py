import os
from datetime import datetime

from yt.wrapper.client import Yt
from yt.wrapper.ypath import TablePath, ypath_dirname
from webmaster3.cass import Cass, Keyspaces
from webmaster3.task import Task
from cassandra import ConsistencyLevel

SESSION = None

YT_TABLE_SCHEMA = [
    {'name': 'Domain', 'type': 'string'},
    {'name': 'CounterId', 'type': 'uint64'},
    {'name': 'Url', 'type': 'string'},
    {'name': 'Title', 'type': 'string'},
    {'name': 'Code', 'type': 'uint8'},
    {'name': 'IsSuspicious', 'type': 'boolean'},
]


def get_fake_counters():
    c1 = {
        'counter_id': 100000,
        'name': 'no_samples_enabled_counter',
        'state': 'enabled',
        'samples_size': 0
    }

    c2 = {
        'counter_id': 100001,
        'name': 'samples_enabled_counter_1',
        'state': 'enabled',
        'samples_size': 19
    }

    c3 = {
        'counter_id': 100002,
        'name': 'samples_enabled_counter_2',
        'state': 'enabled',
        'samples_size': 20
    }

    c4 = {
        'counter_id': 100003,
        'name': 'samples_enabled_counter_3',
        'state': 'enabled',
        'samples_size': 21
    }

    c5 = {
        'counter_id': 100004,
        'name': 'no_samples_disabled_counter',
        'state': 'disabled',
        'samples_size': 0
    }

    c6 = {
        'counter_id': 100005,
        'name': 'samples_disabled_counter',
        'state': 'disabled',
        'samples_size': 110
    }

    c7 = {
        'counter_id': 100006,
        'name': 'no_samples_suspended_counter',
        'state': 'suspended',
        'samples_size': 0
    }

    c8 = {
        'counter_id': 100007,
        'name': 'samples_suspended_counter',
        'state': 'suspended',
        'samples_size': 110
    }

    return [c1, c2, c3, c4, c5, c6, c7, c8]


def store_fake_counters_in_cassandra(domain, counters):
    enabled_counters = list()
    suspended_counters = list()

    for c in counters:
        if c['state'] == 'enabled':
            enabled_counters.append(c['counter_id'])

        if c['state'] == 'suspended':
            suspended_counters.append(c['counter_id'])

        st = SESSION.prepare("INSERT INTO webmaster3.metrika_counter_state (domain, counter_id, last_update, metrika_user_id, origin, state, user_id, user_login) "
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
        SESSION.execute(st, (domain, c['counter_id'], datetime.now(), 365069240, "fake", 3, 365069240, 'robot-webmaster'))

        st = SESSION.prepare("INSERT INTO webmaster3.metrika_crawl_state (domain, counter_id, state, last_update) VALUES (?, ?, ?, ?)")
        SESSION.execute(st, (domain,  c['counter_id'], c['state'].upper(), datetime.now()))


def get_samples_for_domain(domain, counters):
    samples = []
    for c in counters:
        if c['samples_size'] == 0:
            continue

        for i in range(c['samples_size']):
            url = 'https://' + domain + '/' + 'fake_sample_' + str(i + 1) + '/'
            title = 'Fake title ' + str(i + 1)
            s = {
                'Domain': domain,
                'CounterId': c['counter_id'],
                'Url': url,
                'Title': title,
                'Code': 200,
                'IsSuspicious': False
            }

            samples.append(s)

    return samples


def get_yt_token():
    yt_token = os.environ.get('YT_TOKEN')
    if yt_token:
        return yt_token

    from os.path import expanduser
    home_path = expanduser("~")

    try:
        with open(home_path + '/' + '.yt/token') as f:
            l = list(f)
            if l:
                return l[0].strip()
    except:
        return None


YT_CLIENT = Yt(proxy='arnold.yt.yandex.net', token=get_yt_token())

def write_yt_table(rows, table_name):
    tp = TablePath(table_name, schema=YT_TABLE_SCHEMA, append=False)
    YT_CLIENT.write_table(tp, rows, raw=False, force_create=True)


def generate_samples_for_domains(domains):
    samples = []
    for domain in domains:
        fake_counters = get_fake_counters()
        store_fake_counters_in_cassandra(domain, fake_counters)
        domain_samples = get_samples_for_domain(domain, fake_counters)
        samples = samples + domain_samples

    write_yt_table(samples, '//home/webmaster/users/leonidrom/metrika/crawl_samples')


def run(cluster):
    global SESSION

    SESSION = Cass.cluster(cluster).connect()
    SESSION.set_keyspace(Keyspaces.WEBMASTER3)
    SESSION.default_timeout = 1200
    SESSION.default_consistency_level = ConsistencyLevel.LOCAL_QUORUM

    generate_samples_for_domains(['2gis.ru', 'bloodygame08.narod.ru'])


Task.run(run, "test")
