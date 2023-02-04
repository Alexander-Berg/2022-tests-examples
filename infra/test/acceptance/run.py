import gevent.monkey
gevent.monkey.patch_all()

import os
HERE = os.path.abspath(os.path.dirname(__file__))

# this has to be done before importing toiler
os.environ.update({
    'GENISYS_WEB_CONFIG': os.path.join(HERE, 'cfg/web.py'),
    'GENISYS_API_CONFIG': os.path.join(HERE, 'cfg/api.py'),
    'GENISYS_TOILER_CONFIG': os.path.join(HERE, 'cfg/toiler.py'),
})

import codecs
import logging
import threading
import unittest
import time
import argparse

import msgpack
import pymongo
import requests
import html5lib
from bs4 import BeautifulSoup
from gevent.pywsgi import WSGIServer
try:
    import click
except ImportError:
    click = None

from genisys.web.app import make_app as make_ui_app
from genisys.web.api import make_app as make_api_app
from genisys.web.model import MongoStorage
from genisys.toiler import base as toiler_base
from genisys.toiler import registry as toiler_registry
from genisys.toiler import stats as toiler_stats
from genisys.toiler.section import _get_config_hash

from test.acceptance.mock_blackbox_staff import MockBlackbox, MockStaff
from test.acceptance.mock_sandbox import MockSandbox
from test.acceptance.mock_cms import MockCMS


class AcceptanceTest(unittest.TestCase):
    DB_NAME = 'genisys_acc_test'
    USER = 'root'
    SESSIONID = 'sessionid-root'
    SSLSESSIONID = 'sslsessionid-root'
    log = logging.getLogger('acc_test')

    def setUp(self):
        super(AcceptanceTest, self).setUp()

        path = os.path.join(HERE, 'fixture.dump')
        with open(path, 'rb') as fixture_file:
            size = fixture_file.seek(0, 2) / (1 << 20)
            fixture_file.seek(0)
            self._echo('loading fixture (%.2f mb)... ' % (size, ), nl=False)
            msgpack_dump = codecs.decode(fixture_file.read(), 'zip')
            self.fixture = msgpack.loads(msgpack_dump, encoding='utf8')
        self._echo('done')

        self._echo('initializing database... ', nl=False)
        self.storage = MongoStorage(pymongo.MongoClient(), self.DB_NAME)
        for collection in self.storage.db.collection_names(False):
            self.storage.db[collection].drop()
        self.storage.init_db([self.USER])
        self._echo('done')

        self._echo('setting up external resources mocks... ', nl=False)
        self.mock_blackbox = MockBlackbox(('127.0.0.1', 7082), {
            (self.SESSIONID, self.SSLSESSIONID): self.USER
        })
        self.mock_staff = MockStaff(
            listen_address=('127.0.0.1', 7083),
            auth_header='OAuth toooken',
            valid_usernames=self.fixture['users'] + [self.USER],
            valid_groupnames=self.fixture['groups'],
        )
        self.mock_sandbox = MockSandbox(
            listen_address=('127.0.0.1', 7081),
            resource_types_to_releases=self.fixture['releases'],
            resource_id_to_info=self.fixture['resources']
        )
        self.mock_cms = MockCMS(
            listen_address=('127.0.0.1', 7080),
            selectors_to_hosts=self.fixture['selectors']
        )
        self.mock_blackbox.start()
        self.mock_staff.start()
        self.mock_sandbox.start()
        self.mock_cms.start()
        self._echo('done')

        self._echo('setting up web, ui and toiler applications... ', nl=False)
        self.ui_app = make_ui_app()
        self.api_app = make_api_app()
        self.ui_server = WSGIServer(('127.0.0.1', 7084), self.ui_app,
                                    log=logging.getLogger('ui_http'))
        self.api_server = WSGIServer(('127.0.0.1', 7085), self.api_app,
                                     log=logging.getLogger('api_http'))
        self.toiler = toiler_base.Toiler(
            pymongo.MongoClient()[self.DB_NAME], toiler_registry.get_registry(),
            stats=toiler_stats.ToilerStats(None, None)
        )
        self.toiler_thread = threading.Thread(target=self.toiler.run)

        self.ui_server.start()
        self.api_server.start()
        self.toiler_thread.start()
        self._echo('done')

    def tearDown(self):
        super(AcceptanceTest, self).tearDown()
        self._echo('tearing down... ', nl=False)
        self.api_app.cleanup()
        self.ui_server.stop()
        self.api_server.stop()
        self.toiler.stop()
        self.toiler_thread.join()

        self.mock_blackbox.stop()
        self.mock_staff.stop()
        self.mock_sandbox.stop()
        self.mock_cms.stop()
        self._echo('done')

    def _pb(self, iterable, label):
        if not click:
            self.log.info(label)
            class NopPb(object):
                def __enter__(self):
                    return iterable
                def __exit__(self, *a, **k):
                    pass
            return NopPb()
        return click.progressbar(iterable, label=label)

    def _echo(self, message, nl=True):
        if click:
            click.echo(message, nl=nl)
        else:
            self.log.info(message)

    def _load_structure(self, storage, structure):
        s = requests.Session()
        s.cookies.update({'Session_id': self.SESSIONID,
                          'sessionid2': self.SSLSESSIONID})
        self._submit_form(s, '/sections/', 'div#ownersform form', extra_data={
            'owners': ' '.join(self.fixture['structure']['owners']),
        })
        self._submit_form(s, '/sections/', 'div#descform form', extra_data={
            'desc': self.fixture['structure']['desc'],
        })

        all_sections = []
        stack = list(self.fixture['structure']['subsections'].values())
        while stack:
            node = stack.pop()
            all_sections.append(node)
            stack.extend(node['subsections'].values())

        for node in all_sections:
            all_rules = [(None, rule) for rule in node['rules']]
            for rule in node['rules']:
                all_rules.extend((rule['name'], subrule)
                                 for subrule in rule['subrules'])
            rules_pb = self._pb(all_rules,
                                'populating section %s' % (node['path'], ))
            parent_path = ''
            if '.' in node['path']:
                parent_path = node['path'].rsplit('.', 1)[0]
            create_section_data = {
                'name': node['name'],
                'stype': node['stype'],
                'owners': ' '.join(node['owners']),
                'desc': node['desc'],
            }
            if node['stype'] == 'sandbox_resource':
                create_section_data['sandbox_resource_type'] = \
                        node['stype_options']['resource_type']
            self._submit_form(
                s, '/sections/' + parent_path, 'div#createsubsection form',
                extra_data=create_section_data
            )
            with rules_pb as all_rules:
                for parent_rule_name, rule in all_rules:
                    action = None
                    if parent_rule_name is not None:
                        action = '/rules/%s/%s' % (node['path'],
                                                   parent_rule_name)
                    create_rule_data = {
                        'name': rule['name'],
                        'desc': rule['desc'],
                        'editors': ' '.join(rule['editors']),
                        'htype': 'all' if rule['selector'] is None else 'some',
                        'selector': rule['selector'] or '',
                    }
                    if node['stype'] == 'yaml':
                        create_rule_data['config'] = rule['config_source']
                    else:
                        create_rule_data.update({
                            'rtype': 'by_id',
                            'resource': rule['config_source']['resource'],
                        })
                    self._submit_form(s, '/sections/' + node['path'],
                                      'div#createrule form', action=action,
                                      extra_data=create_rule_data)

    def _submit_form(self, session, url, form_selector, extra_data,
                     action=None):
        html = self.req_ui_html(session, url)
        form = html.select_one(form_selector)
        if action is None:
            action = form.attrs.get('action') or url
        data = {}
        for inp in form.find_all('input'):
            if inp.attrs['type'] in ('text', 'hidden'):
                data[inp.attrs['name']] = inp.attrs['value']
            elif inp.attrs['type'] == 'radio':
                if inp.attrs.get('checked'):
                    data[inp.attrs['name']] = inp.attrs['value']
        for ta in form.find_all('textarea'):
            data[ta.attrs['name']] = ta.text
        data.update(extra_data)
        self.log.info('posting %r to %r', data, action)
        return self.ui_post(session, url=action, data=data)

    def _request_html(self, session, method, url, **kwargs):
        response = getattr(session, method)(url, **kwargs)
        if response.status_code // 100 in (4, 5):
            self.fail(BeautifulSoup(response.content, 'html5lib').text)
        self.assertEqual(response.headers['content-type'],
                         'text/html; charset=utf-8')

        parser = html5lib.HTMLParser()
        parser.parse(response.content)
        if parser.errors:
            (line, col), error, args = parser.errors[0]
            lines = response.content.decode('utf8').splitlines()
            firstline = max(0, line - 15)
            seen_first_nonempty = False
            for i, line in enumerate(lines[firstline:line]):
                if not line and not seen_first_nonempty:
                    continue
                seen_first_nonempty = True
                print("%3d %s" % (i + firstline + 1, line))
            print("   %s^ %s %s" % (" " * col, error, args or ""))
            self.fail("found %d html parsing errors, first one is shown below"
                      % len(parser.errors))
        return BeautifulSoup(response.content, 'html5lib')

    def req_ui_html(self, session, url, method='get', **kwargs):
        html = self._request_html(session, method,
                                  'http://127.0.0.1:7084' + url, **kwargs)
        flashes = html.find('div', id='flashes')
        if flashes:
            for flash in flashes.find_all('p', class_='alert'):
                self.log.info('FLASH: %s', flash.find('span').text)
        return html

    def ui_post(self, session, data, url, **kwargs):
        html = self.req_ui_html(session, url, method='post',
                                data=data, **kwargs)
        errors = {}
        for formgroup in html.select('.form-group.has-error'):
            field = formgroup.find('label').text
            errorlist = [e.text for e in formgroup.select('.error-text')]
            errors[field] = errorlist
        if errors:
            print("Found form errors:")
            for field in sorted(errors):
                print("  {}".format(field))
                for error in errors[field]:
                    print("    {}".format(error))
            self.fail()
        return html

    def test(self):
        self._load_structure(self.storage, self.fixture['structure'])
        with self._pb(range(60), 'waiting for dust to settle (60 sec)') as pb:
            for _ in pb:
                time.sleep(1)
        pb = self._pb(self.fixture['hosts'].items(),
                      'comparing configurations (%d hosts)' %
                      len(self.fixture['hosts']))
        with pb as host_expected_cfg:
            for host, expected_cfg in host_expected_cfg:
                expected_cfg = dict(zip(self.fixture['paths'], expected_cfg))
                resp = requests.get('http://127.0.0.1:7085/v2/hosts/' + host,
                                    {'fmt': 'msgpack'})
                resp.raise_for_status()
                data = msgpack.loads(resp.content, encoding='utf8')
                stack = [data]
                config_hashes = {}
                matched_rules = {}
                while stack:
                    item = stack.pop()
                    self.assertIn(item['last_status'], ('same', 'modified'))
                    if item['subsections']:
                        config_hashes[item['path']] = None
                        matched_rules[item['path']] = None
                        stack.extend(item['subsections'].values())
                    else:
                        config_hashes[item['path']] = item['config_hash']
                        matched_rules[item['path']] = item['matched_rules']
                        try:
                            self.assertEqual(_get_config_hash(item['config']),
                                             item['config_hash'])
                        except:
                            self.log.error('host %s, section %s: config_hash '
                                           'doesn\'t match config',
                                           host, item['path'])
                            raise
                for path in expected_cfg:
                    ecfg_hash, ematched_rules = expected_cfg[path]
                    try:
                        self.assertEqual(ematched_rules, matched_rules[path])
                        self.assertEqual(ecfg_hash, config_hashes[path])
                    except:
                        self.log.error(
                            'discrepancy found in host %s, section %s',
                            host, path
                        )
                        raise


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('-d', '--debug', action='store_true')
    args = parser.parse_args()
    logging.captureWarnings(True)
    level = logging.WARNING
    if args.debug:
        level = logging.INFO
    elif not click:
        logging.getLogger('acc_test').setLevel(logging.INFO)
    logging.basicConfig(
        level=level,
        format='%(asctime)-15s %(name)s %(levelname)s %(message)s'
    )
    if args.debug:
        click = None
    else:
        logging.getLogger('requests').setLevel(logging.WARNING)
        logging.getLogger('py.warnings').setLevel(logging.ERROR)
        if click:
            logging.getLogger('genisys.toil').setLevel(logging.ERROR)
    import sys
    del sys.argv[1:]
    unittest.main()
