import contextlib
import logging
import os
import random
import re
import shutil
import socket
import subprocess
import sys
import time
import urllib
import urllib.parse
import uuid

import mock
import requests

import yatest.common

from . import test_helpers

logging.getLogger('requests').setLevel(logging.WARNING)

logger = logging.getLogger("test_logger")


def _read_file(abs_path):
    with open(abs_path) as f:
        return f.read()


def _format_file(abs_path, **formatters):
    return _read_file(abs_path).format(**formatters)


def _sort_query(url):
    parsed_url = urllib.parse.urlsplit(url)
    query = urllib.parse.urlencode(sorted(urllib.parse.parse_qsl(parsed_url.query)))
    # urllib.parse.SplitResult(scheme, netloc, path, query, fragment)
    return urllib.parse.urlunsplit(('', '', parsed_url.path, query, ''))


def _get_environment_files(environments_dir, environment):
    for filename in os.listdir(environments_dir):
        full_path = os.path.join(environments_dir, filename)
        if os.path.isfile(full_path) and filename.endswith('.' + environment):
            yield full_path


class _NginxServer(object):

    def __init__(self, work_dir):
        self._work_dir = work_dir
        self._upstreams = {}
        self._file_templates = []
        self._locations = []
        self._servers = []
        self._replacements = []  # Configuration hacks
        self._variables = {}
        self._require_paths = set()
        self._require_commands = set()
        self._env_vars = {}
        self._confroot_includes = []
        s = socket.socket()
        while True:
            self._port = random.randint(1024, 10000)
            try:
                s.bind(('localhost', self._port))
                s.close()
                break
            except socket.error:
                continue

    @contextlib.contextmanager
    def bring_up(self, environment="production"):
        variables = '\n'.join(
            'set ${key} {value};'.format(key=key, value=value)
            for key, value in self._variables.items())
        confroot_includes = '\n'.join(
            'include {filename};'.format(filename=filename)
            for filename in self._confroot_includes
        )

        params = {
            'port': self._port,
            'work_dir': self._work_dir,
            'environment': next(_get_environment_files(self._environments_dir, environment)),
            'variables': variables,
            'confroot_includes': confroot_includes,
        }

        self._save_config(
            _format_file(self.get_config_path('nginx.conf.template'), **params),
            'nginx.conf',
            **params)

        require_paths = '\n'.join(
            '.. ";{path}/?.lua"'.format(path=path)
            for path in self._require_paths)

        require_cpaths = '\n'.join(
            '.. ";{path}/?.so"'.format(path=path)
            for path in self._require_paths)

        require_commands = '\n'.join(
            'require "{package}"'.format(package=package)
            for package in self._require_commands)

        self._save_config(
            _format_file(
                self.get_config_path('init.lua.template'),
                require_paths=require_paths,
                require_cpaths=require_cpaths,
                require_commands=require_commands),
            'init.lua')

        for file_template in self._file_templates:
            file_template.save_config(self._work_dir, self._replacements, params)

        self._save_config(
            '\n'.join(upstream.config() for upstream
                      in self._upstreams.values()),
            'upstreams.conf',
            **params)

        self._save_config(
            '\n'.join(self._locations),
            'locations.conf',
            **params)

        servers = '\n'.join(self._servers)
        for upstream in self._upstreams.values():
            servers = upstream.remove_real_upstream(servers)
        self._save_config(servers, 'servers.conf', **params)

        self._save_config(
            _format_file(self.get_config_path('listen'), **params),
            'listen',
            **params)

        for item in 'listen_https', 'fastcgi_params_yandex', 'upstream.lua':
            if not os.path.isfile(os.path.join(self._work_dir, item)):
                shutil.copy(self.get_config_path(item), self._work_dir)

        args = [self._nginx_path, '-p', self._work_dir, '-c', 'nginx.conf']
        print(' '.join(args))
        process = self._executor(args, stderr=subprocess.PIPE, wait=False, env=self._env_vars).process

        wait_time = 2
        while True:
            time.sleep(0.1)
            wait_time -= 0.1

            if process.poll() is not None:
                sys.stdout.write(process.stderr.read())
                raise Exception('Cannot start nginx: process exited')

            try:
                if (requests.get(self.make_local('/unittest-ping'),
                                 headers=self.headers()).status_code == 200):
                    break

            except Exception as e:
                if wait_time <= 0:
                    sys.stdout.write(process.stderr.read())
                    raise Exception('Cannot start nginx: ' + str(e))

        try:
            yield self

        except:
            with open(os.path.join(self._work_dir, 'error.log')) as log:
                sys.stdout.write(log.read())
            raise

        finally:
            self._executor(args + ['-s', 'stop'], stderr=subprocess.PIPE)
            process.wait()

    def _save_config(self, config_data, file_name, **params):
        for old, new in self._replacements:
            config_data = re.sub(old, new.format(**params), config_data)
        with open(os.path.join(self._work_dir, file_name), 'w') as f:
            f.write(config_data)

    def add_upstream(self, name, host_name, template):
        upstream = _Upstream(name, host_name, self._port, self.get_config_path(template),
                             self._work_dir)
        self._upstreams[name] = upstream
        return upstream

    def get_upstreams(self):
        return self._upstreams

    def add_file_template(self, template_abs_path, destination_name, formatters={}):
        file_template = _FileTemplate(template_abs_path, destination_name, formatters)
        self._file_templates.append(file_template)
        return file_template

    def add_package(self, package):
        self._require_commands.add(package)

    def add_locations(self, config_data):
        self._locations.append(config_data)

    def add_server(self, config_data):
        self._servers.append(config_data)

    def add_variable(self, var_name, var_value):
        self._variables[var_name] = var_value

    def add_confroot_include(self, filename):
        self._confroot_includes.append(filename)

    def set_env_variable(self, key, value):
        if value is not None:
            self._env_vars[key] = value
        elif key in self._env_vars:
            self._env_vars.pop(key)

    def make_local(self, url):
        if not url.startswith('http://'):
            url = 'http://' + url
        parsed_url = urllib.parse.urlsplit(url)
        return 'http://localhost:{port}{path}{query}'.format(
            port=self._port,
            path=parsed_url.path,
            query='?' + parsed_url.query if parsed_url.query else '')

    def headers(self):
        return {'Host': 'unittest-server'}

    def replace_in_config(self, old, new):
        self._replacements.append((old, new))

    def get(self, url, headers=None):
        if headers is None:
            headers = self.headers()
        response = requests.get(self.make_local(url), headers=headers)
        self.poll_upstreams()
        return response.status_code, response.content.decode()

    def post(self, url, data, headers=None):
        if headers is None:
            headers = {}
        response = requests.post(self.make_local(url),
                                 headers=headers,
                                 data=data)
        self.poll_upstreams()
        return response.status_code, response.content.decode()

    def put(self, url, data, headers=None):
        if headers is None:
            headers = {}
        response = requests.put(self.make_local(url),
                                headers=headers,
                                data=data)
        self.poll_upstreams()
        return response.status_code, response.content.decode()

    def delete(self, url, headers=None):
        if headers is None:
            headers = {}
        response = requests.delete(self.make_local(url), headers=headers)

        self.poll_upstreams()
        return response.status_code, response.content.decode()

    def poll_upstreams(self):
        for upstream in self._upstreams.values():
            upstream.poll_request()

    def add_require_path(self, path):
        self._require_paths.add(path)

    def set_config(self, nginx_path, config_dir, environments_dir, executor):
        self._nginx_path = nginx_path
        self._config_dir = config_dir
        self._environments_dir = environments_dir
        self._executor = executor

    def get_config_path(self, rel_path):
        return os.path.join(self._config_dir, rel_path)


class _FileTemplate(object):
    def __init__(self, template_abs_path, destination_name, formatters={}):
        self._template_abs_path = template_abs_path
        self._destination_name = destination_name
        self._formatters = formatters

    def save_config(self, work_dir, replacements, params):
        config_data = _format_file(self._template_abs_path, **self._formatters)
        for old, new in replacements:
            config_data = re.sub(old, new.format(**params), config_data)
        with open(os.path.join(work_dir, self._destination_name), 'w') as f:
            f.write(config_data)


class _Upstream(object):
    def __init__(self, name, host_name, port, template, work_dir):
        self._mock = mock.Mock()
        self._work_dir = work_dir
        self._own_dir = os.path.join(work_dir, name)
        self._name = name
        self._host_name = host_name
        self._request_file = os.path.join(self._own_dir, 'requests')
        self._response_file = os.path.join(self._own_dir, 'responses')
        self._config = _format_file(template,
                                    name=name,
                                    host=host_name,
                                    port=port,
                                    work_dir=self._work_dir,
                                    request_file=self._request_file,
                                    response_file=self._response_file)
        self._expected_request_parameters = None

        shutil.rmtree(self._own_dir, ignore_errors=True)
        os.mkdir(self._own_dir)

    def config(self):
        return self._config

    def remove_real_upstream(self, config_data):
        match = re.search('upstream +%s *{[^}]*}' % self._name,
                          config_data,
                          flags=re.DOTALL)
        if not match:
            return config_data
        return config_data[:match.start(0)] + config_data[match.end(0):]

    def get(self, url, headers_matcher=test_helpers.Anything()):
        self.request(
            self._host_name,
            test_helpers.And(
                test_helpers.MethodIs('GET'),
                test_helpers.UrlIs(url)
            ),
            '',
            headers_matcher)
        return self

    def post(self, url, data, headers_matcher=test_helpers.Anything()):
        self.request(
            self._host_name,
            test_helpers.And(
                test_helpers.MethodIs('POST'),
                test_helpers.UrlIs(url)
            ),
            data,
            headers_matcher)
        return self

    def put(self, url, data, headers_matcher=test_helpers.Anything()):
        self.request(
            self._host_name,
            test_helpers.And(
                test_helpers.MethodIs('PUT'),
                test_helpers.UrlIs(url)
            ),
            data,
            headers_matcher)
        return self

    def request(
            self,
            host_matcher=test_helpers.Anything(),
            query_matcher=test_helpers.Anything(),
            data_matcher=test_helpers.Anything(),
            headers_matcher=test_helpers.Anything()):
        assert self._expected_request_parameters is None, 'Only one request to upstream is supported'
        self._expected_request_parameters = [host_matcher, query_matcher, data_matcher, headers_matcher]
        return self

    def AndReturn(self, status_code, data=''):
        with open(os.path.join(self._response_file), 'a') as f:
            f.write('%d\n' % status_code)
            f.write('%d\n' % len(data))
            f.write(data)

    def poll_request(self):
        if (os.path.isfile(self._request_file) and
                os.stat(self._request_file).st_size > 1):
            with open(os.path.join(self._request_file)) as f:
                while True:
                    host_name = f.readline().strip()
                    if not host_name:
                        break
                    method = f.readline().strip()
                    url = _sort_query(f.readline().strip())
                    body_size = int(f.readline())
                    body = f.read(body_size)
                    headers_count = int(f.readline())
                    headers = {}
                    for index in range(headers_count):
                        header = f.readline().strip()
                        value = f.readline().strip()
                        headers[header] = value
                    self._mock.request(host_name, method + ' ' + url, body, headers)
            os.remove(self._request_file)

    def assert_called(self):
        if self._expected_request_parameters:
            self._mock.request.assert_called_once_with(*self._expected_request_parameters)


class NginxTestBase(object):

    def setup(self):
        self._work_dir = "work_dirs/" + str(uuid.uuid1())
        shutil.rmtree(self.work_dir, ignore_errors=True)
        os.makedirs(self.work_dir)

        self._locations_path = None
        self._nginx = _NginxServer(self.work_dir)
        self.add_require_path(self._work_dir)

    def teardown(self):
        for _, upstream in self._nginx.get_upstreams().items():
            upstream.assert_called()
        error_log = os.path.join(self.work_dir, 'error.log')
        if os.path.exists(error_log):
            with open(error_log) as f:
                print('Error log:\n' + ''.join(f.readlines()))

    @property
    def work_dir(self):
        return self._work_dir

    def set_config(self, nginx_path, config_dir, environments_dir, executor):
        self._nginx.set_config(nginx_path, config_dir, environments_dir, executor)

    def add_dummy_upstreams(self, *names):
        for name in names:
            self.add_upstream(name, 'dummy-' + name)

    def add_upstream(self, name, host_name):
        return self._nginx.add_upstream(name, host_name, 'upstream.conf.template')

    def bring_up_nginx(self, environment="production"):
        return self._nginx.bring_up(environment)

    def add_locations_from_file(self, filename):
        config_file = os.path.join(yatest.common.source_path(self._locations_path), filename)
        self.add_locations(_read_file(config_file))

    def add_locations(self, config_data):
        self._nginx.add_locations(config_data)

    def add_variable(self, key, value):
        self._nginx.add_variable(key, value)

    def add_confroot_include(self, filename):
        self._nginx.add_confroot_include(filename)

    def add_server(self, config_data):
        self._nginx.add_server(config_data)

    def replace_in_config(self, old, new):
        self._nginx.replace_in_config(old, new)

    def add_file_template(self, template_abs_path, destination_name, formatters={}):
        self._nginx.add_file_template(template_abs_path, destination_name, formatters)

    def add_config_file(self, config_abs_path):
        destination_name = os.path.basename(config_abs_path)
        self._nginx.add_file_template(config_abs_path, destination_name)

    def add_require_path(self, path):
        self._nginx.add_require_path(path)

    def add_package(self, package):
        self._nginx.add_package(package)

    def set_env_variable(self, key, value):
        self._nginx.set_env_variable(key, value)

    def assertEqual(self, a, b):
        assert a == b, '{} != {}'.format(a, b)
