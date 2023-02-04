# coding=utf-8
from __future__ import unicode_literals

import mock
import pytest
import gevent
from sepelib.util.retry import RetrySleeper

from its_client.poller import ItsPoller
from .conftest import ITAGS


CONTROLS = [
    {},
    {'ruchka1': 'value1'},
    {'ruchka1': 'value1', 'ruchka2': 'value2'},
    {'ruchka1': 'value2'},
    {'ruchka3': 'значение1'},
]


@pytest.fixture
def its_poller(its_server, tmpdir):

    controls_dir = tmpdir.join('controls')
    shared_dir = tmpdir.join('shared')
    flag = tmpdir.join('flag')

    conf = {
        'url': 'http://localhost:{0}/v1'.format(its_server.wsgi.socket.getsockname()[1]),
        'itags': ITAGS,
        'controls_dir': controls_dir.strpath,
        'shared_dir': shared_dir.strpath,
        'refresh_flag': flag.strpath,
        'req_timeout': 0.5,
        'poll_timeout': 0.5,
        'max_poll_timeout': 300,
        'max_timeout_jitter': 0.0,
    }

    return ItsPoller.from_config(conf)


def test_its_poller(request, tmpdir, its_poller, its_params):
    controls_event = its_poller.controls_requested_event
    its_poller.start()
    request.addfinalizer(its_poller.stop)

    controls_dir = tmpdir.join('controls')

    def get_current_controls():
        return dict((f.basename, f.read().decode('utf8')) for f in controls_dir.listdir() if f.isfile())

    for its_params['controls'] in CONTROLS:
        controls_event.clear()
        controls_event.wait()
        assert get_current_controls() == its_params['controls']

    # проверяем подгрузку кешированной версии ручек и одновременную отправку заголовка Expect: 200-ok

    its_poller.stop()
    its_params['expect_200_ok'] = True

    its_poller.start()
    request.addfinalizer(its_poller.stop)

    for its_params['controls'] in CONTROLS:
        controls_event.clear()
        controls_event.wait()
        assert get_current_controls() == its_params['controls']


def test_its_poller_request_timeout(request, its_poller, its_params):
    controls_event = its_poller.controls_requested_event
    its_params['wait_before_response'] = 100

    its_poller.start()
    request.addfinalizer(its_poller.stop)

    assert controls_event.wait(10)


def test_its_poller_refresh_flag(request, tmpdir, its_poller, its_params):
    controls_event = its_poller.controls_requested_event

    its_params['controls'] = {'ruchka_for_flag': 'before_flag'}
    its_params['max_age'] = 100

    its_poller.start()
    request.addfinalizer(its_poller.stop)

    controls_event.wait()

    assert tmpdir.join('controls', 'ruchka_for_flag').read() == 'before_flag'

    its_params['controls'] = {'ruchka_for_flag': 'after_flag'}

    controls_event.clear()
    assert its_poller.clear_controls_cache_time()

    assert controls_event.wait(10)
    assert tmpdir.join('controls', 'ruchka_for_flag').read() == 'after_flag'


@pytest.mark.parametrize('shared_version,target_etag', [('1', 'local_etag'), ('2', 'shared_etag')])
def test_its_poller_shared(request, tmpdir, its_params, its_poller, shared_version, target_etag):
    controls_event = its_poller.controls_requested_event

    tmpdir.join('shared', '.its_client', 'etag').write('shared_etag', ensure=True)
    tmpdir.join('shared', '.its_client', 'version').write(shared_version, ensure=True)
    tmpdir.join('shared', 'shared_ruchka').write('shared_value', ensure=True)

    tmpdir.join('controls', '.its_client', 'etag').write('local_etag', ensure=True)
    tmpdir.join('controls', '.its_client', 'version').write('1', ensure=True)
    tmpdir.join('controls', 'local_ruchka').write('local_value', ensure=True)

    its_params['etag'] = target_etag
    its_params['controls'] = {'one_more_ruchka': 'value'}
    its_params['wait_before_response'] = 100

    its_poller.start()
    request.addfinalizer(its_poller.stop)

    assert controls_event.wait(10)

    if target_etag == 'shared_etag':
        assert tmpdir.join('controls', 'shared_ruchka').read() == 'shared_value'
        assert not tmpdir.join('controls', 'local_ruchka').exists()
    else:
        assert tmpdir.join('controls', 'local_ruchka').read() == 'local_value'
        assert not tmpdir.join('controls', 'shared_ruchka').exists()

    its_params['wait_before_response'] = 0
    controls_event.clear()
    controls_event.wait()
    assert tmpdir.join('controls', 'one_more_ruchka').read() == 'value'


def test_rewrite_controls_on_content_changes_only(tmpdir, its_poller, request, its_params):
    controls_event = its_poller.controls_requested_event

    its_poller.start()
    request.addfinalizer(its_poller.stop)

    controls_dir = tmpdir.join('controls')

    def get_current_controls():
        return dict((f.basename, f.read().decode('utf8')) for f in controls_dir.listdir() if f.isfile())

    # Write first ITS controls
    its_params['controls'] = {'my_little_key': 'my_little_value'}
    controls_event.clear()
    controls_event.wait()
    assert get_current_controls() == its_params['controls']

    # The same ITS control but new config version: control file should be rewritten
    # Sleep some time to make new mtime value
    gevent.sleep(3)
    mtime = controls_dir.join('my_little_key').mtime()
    its_params['config_version'] += 1
    controls_event.clear()
    controls_event.wait()
    assert get_current_controls() == its_params['controls']
    new_mtime = controls_dir.join('my_little_key').mtime()
    assert mtime != new_mtime

    # The same controls and config version but new statistics period: control file must not be rewritten
    # Sleep some time to make new mtime value
    mtime = new_mtime
    gevent.sleep(3)
    its_params['statistics_period'] += 1
    controls_event.clear()
    controls_event.wait()
    assert get_current_controls() == its_params['controls']
    assert mtime == controls_dir.join('my_little_key').mtime()

    # The same ITS control but new config version: control file should be rewritten again
    # Sleep some time to make new mtime value
    gevent.sleep(3)
    its_params['config_version'] += 1
    controls_event.clear()
    controls_event.wait()
    assert get_current_controls() == its_params['controls']
    assert mtime != controls_dir.join('my_little_key').mtime()


def test_its_poller_force_poll_timeout(tmpdir, its_server, its_poller, request, its_params):
    """
    Проверяет:

    * Без указания force_poll_timeout its_client ходит в ITS с таймаутом, указанном в заголовке Cache-Control,
      полученном из ITS
    * При указании force_poll_timeout its_client ходит в ITS с этим таймаутом
    """

    force_timeout = 666.6
    max_age = 777.7
    max_timeout = 888.8
    its_params['controls'] = {'control': 'value'}
    controls_event = its_poller.controls_requested_event

    sleepers = []

    controls_dir = tmpdir.join('controls')
    shared_dir = tmpdir.join('shared')
    flag = tmpdir.join('flag')

    def get_current_controls():
        return dict((f.basename, f.read().decode('utf8')) for f in controls_dir.listdir() if f.isfile())

    class FakeRetrySleeper(RetrySleeper):
        def __init__(self, delay=None, max_delay=None, **kwargs):
            super(FakeRetrySleeper, self).__init__(delay=delay, max_delay=max_delay, **kwargs)
            sleepers.append(self)

    with mock.patch('its_client.poller.RetrySleeper', FakeRetrySleeper):
        its_poller.start()
        request.addfinalizer(its_poller.stop)

        controls_event.wait()

        assert len(sleepers) == 2

        assert sleepers[0].delay == ItsPoller.MINIMAL_POLL_TIMEOUT
        assert sleepers[0].max_delay == ItsPoller.MAXIMAL_POLL_TIMEOUT

        assert sleepers[1].delay == 0.5
        assert sleepers[1].max_delay == ItsPoller.MAXIMAL_POLL_TIMEOUT
        assert get_current_controls() == its_params['controls']

        its_poller.stop()
        its_params['expect_200_ok'] = True
        controls_event.clear()

        sleepers[:] = []

        its_params['controls'] = {'other_control': 'other_value'}

        conf = {
            'itags': ITAGS,
            'controls_dir': controls_dir.strpath,
            'refresh_flag': flag.strpath,
            'shared_dir': shared_dir.strpath,
            'url': 'http://localhost:{0}/v1'.format(its_server.wsgi.socket.getsockname()[1]),
            'req_timeout': 0.5,
            'poll_timeout': force_timeout,
            'max_poll_timeout': max_timeout,
            'max_timeout_jitter': 0.0,
        }

        its_poller = ItsPoller.from_config(conf)
        controls_event = its_poller.controls_requested_event

        its_poller.start()
        request.addfinalizer(its_poller.stop)

        controls_event.wait()

        assert len(sleepers) == 2

        assert sleepers[0].delay == ItsPoller.MINIMAL_POLL_TIMEOUT
        assert sleepers[0].max_delay == max_timeout

        assert sleepers[1].delay == force_timeout
        assert sleepers[1].max_delay == max_timeout
        assert get_current_controls() == its_params['controls']


def test_its_poller_gevent_timeout(its_poller, request):
    its_poller.req_timeout = 1.0

    def sleeper(*args, **kwargs):
        gevent.sleep(50)

    with mock.patch.object(its_poller._its_client, 'get_controls', side_effect=sleeper) as m:
        its_poller.start()
        request.addfinalizer(its_poller.stop)
        assert its_poller.controls_requested_event.wait(10)
        assert m.called


def test_its_poller_fail(its_poller, request):
    with mock.patch.object(its_poller._its_client, 'get_controls', side_effect=Exception) as m:
        its_poller.start()
        request.addfinalizer(its_poller.stop)
        gevent.sleep(3.0)
        assert m.called
        assert not its_poller.ready()

