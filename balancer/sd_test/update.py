#!/skynet/python/bin/python

import argparse
import json
import requests
import os
import sys


def main():
    files = {}
    for path in ['load.sh', 'instancectl.conf', 'balancer.cfg']:
        with open(path) as f:
            files[path] = f.read()

    session = requests.Session()

    with open(os.path.expanduser('~/.nanny/token'), 'r') as token_file:
        token =  token_file.read().strip()

    session.headers = {'Authorization': 'OAuth {}'.format(token), 'Content-Type': 'application/json', 'Accept-Encoding': ''}

    parser = argparse.ArgumentParser()
    parser.add_argument('comment', nargs='?', default='update from balancer/production/x/sd_test')
    parser.add_argument('--binary', default=None)
    args = parser.parse_args()

    for service in ['sd_balancer_test', 'man_sd_balancer_test', 'vla_sd_balancer_test']:
        if not update_service(session, service, files, args.comment, args.binary):
            return

def update_service(session, service_name, files, comment, balancer_executable):
    print "start update {}...".format(service_name)

    r = session.get('http://nanny.yandex-team.ru/v2/services/{}/runtime_attrs/'.format(service_name))

    if not r.ok:
        print "get error:"
        print r.status_code
        print r.text
        return False

    data = r.json()

    snapshot_id = data['_id']
    content = data['content']

    updated = 0

    for item in content['resources']['static_files']:
        if item['local_path'] in files:
            item['content'] = files[item['local_path']]
            updated += 1

    assert updated == len(files)

    if balancer_executable:
        for item in content['resources']['url_files']:
            if item['local_path'] == 'balancer':
                item['url'] = balancer_executable


    update_request = {
        'snapshot_id': snapshot_id,
        'content': content,
        'comment': comment,
    }

    r = session.put('http://nanny.yandex-team.ru/v2/services/{}/runtime_attrs/'.format(service_name), data=json.dumps(update_request))

    if not r.ok:
        print "update error:"
        print r.status_code
        print r.text
        return False

    if snapshot_id == r.json()['_id']:
        print 'nothing changed'
        return True

    print 'updated, new snapshot_id is', r.json()['_id']

    activate_request = {
        'type': 'SET_SNAPSHOT_STATE',
        'content': {
            'snapshot_id': r.json()['_id'],
            'comment': comment,
            'recipe': 'default',
            'state': 'ACTIVE',
            'set_as_current': True
        }
    }

    r = session.post('http://nanny.yandex-team.ru/v2/services/{}/events/'.format(service_name), data=json.dumps(activate_request))

    if not r.ok:
        print "activate error:"
        print r.status_code
        print r.text
        return False

    return True

if __name__ == '__main__':
    main()
