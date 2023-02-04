import os
import sys
from tempfile import NamedTemporaryFile

try:
    from oops_agent import Agent
except ImportError:
    try:
        from ya.infra.oops.agent.oops_agent import Agent
    except ImportError:
        print(sys.modules)
        raise

maindir = '.'


class TestParams:
    hostname = 'testhostname'
    daemonize = False
    config = None
    pidfile = None
    logfile = None

    def __init__(self, fn=None):
        self.config = fn


def del_file(f):
    if os.path.isfile(f):
        os.unlink(f)


def teardown_module():
    for f in ('config.json',):
        del_file(f)


def test_save_state():
    with NamedTemporaryFile() as f:
        f.write('{"components": {}, "modules": {}}')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        state_file = a.save_state(-1, None)
        assert os.path.isfile(state_file)
        try:
            open(state_file, 'r').read()
        finally:
            del_file(state_file)


def test_merge_context_absent_file():
    a = Agent(TestParams('ssssss'))
    try:
        a.prepare_config()
    except IOError:
        pass
    else:
        raise Exception("Exception is not raised when expected")


def test_merge_context_invalid_file():
    with NamedTemporaryFile() as f:
        f.write('asdasdasdasdasdasdasdasd')
        f.flush()
        a = Agent(TestParams(f.name))
        try:
            a.prepare_config()
        except ValueError:
            pass
        else:
            raise Exception("Exception is not raised when expected")


def test_add_component():
    with NamedTemporaryFile() as f:
        f.write('{"components": {"test_component":{}}, "modules": {} }')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        assert 'test_component' in a.context_config['components'], 'no test_component in %s' % a.context_config[
            'components'].keys()
        assert len(a.context_config['components']) == 1


def test_change_component():
    with NamedTemporaryFile() as f:
        f.write('{"components": {"reporter": {"config": {"heartbeat": 7777}}}, "modules": {} }')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        assert a.context_config['components']['reporter']['config']['heartbeat'] == 7777
        assert len(a.context_config['components']) == 1


def test_config_propagates_to_component():
    with NamedTemporaryFile() as f:
        f.write('{"components": {"reporter": {"class": "OopsReporter", "config": {"heartbeat": 7777, "server": "http://ya.ru"}}}, "modules": {} }')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        a.load_components_and_modules()
        assert a.context['components']['reporter'].config['heartbeat'] == 7777


def test_disable_component():
    with NamedTemporaryFile() as f:
        f.write('{"components": {"reporter":{"disabled": 1}}, "modules": {} }')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        a.load_components_and_modules()
        assert 'reporter' not in a.context['components']


def test_disable_module():
    with NamedTemporaryFile() as f:
        f.write('{"components": {}, "modules": {"who": {"disabled": 1}} }')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        a.load_components_and_modules()
        assert 'who' not in a.context['modules']


def test_module_hb():
    with NamedTemporaryFile() as f:
        f.write('{"components": {"bus": {"class": "Bus", "config": {"default_module_heartbeat": 300}}}, "modules": {"who": {"heartbeat": 555}} }')
        f.flush()
        a = Agent(TestParams(f.name))
        a.prepare_config()
        a.load_components_and_modules()
        assert a.context['components']['bus'].heartbeat['who'] == 555
