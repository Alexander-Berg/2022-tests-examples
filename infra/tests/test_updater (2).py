import os
import json
import asyncio

import mock
import pytest
import aiohttp
from yarl import URL

from infra.yp_dru import updater


pytestmark = pytest.mark.asyncio


async def test_fetching_resources(drcu):
    url = drcu.make_url('/')
    pod_info = {
        "resources": ["x"],
        "revision": 'some_new',
    }

    async with aiohttp.ClientSession() as session:
        async with session.post(str(url.join(URL("test_pod1"))), data=json.dumps(pod_info)) as resp:
            assert 200 <= resp.status < 300

    assert (await updater.fetch_resources_info(url, 'test_pod1')) == pod_info
    assert (await updater.fetch_resources_info(url, 'test_pod1', 'some_old')) == pod_info

    await drcu.close()

    with pytest.raises(RuntimeError):
        await asyncio.wait_for(updater.fetch_resources_info(url, 'test_pod1'), timeout=2.)


async def test_check_resource(stdout, installed_resource):
    state, info = installed_resource
    # hashsums of empty data
    sha256 = 'SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
    md5 = 'MD5:d41d8cd98f00b204e9800998ecf8427e'

    print("empty hashsum")
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()

    print("sha256 hashsum")
    info['storage_options']['verification']['checksum'] = sha256
    state['last_check_time'] = None
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()

    print("md5 hashsum")
    info['storage_options']['verification']['checksum'] = md5
    state['last_check_time'] = None
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()


async def test_only_one_notify(stdout, installed_resource):
    state, info = installed_resource
    with mock.patch('infra.yp_dru.updater.notify_service') as _notify:
        await updater.process_resource(state, info)
        await updater.process_resource(state, info)
        await updater.process_resource(state, info)

        assert _notify.call_count == 0


async def test_download_resource(stdout, uninstalled_resource):
    state, info = uninstalled_resource
    await updater.process_resource(state, info)
    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert 'path' in state and os.path.exists(state['path']), stdout.getvalue()
    assert 'mark' in state and state['mark'] == 'test2'

    state['last_check_time'] = None
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()


async def test_upgrade_resource(stdout, installed_resource):
    state, info = installed_resource
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()

    old_path = state['revs'][-1]['path']
    assert os.path.exists(old_path), stdout.getvalue()

    info['revision'] = 'newer'
    info['urls'] = ['raw:x']
    info['storage_options']['verification']['checksum'] = 'MD5:9dd4e461268c8034f5c8564e155c67a6'

    await updater.process_resource(state, info)
    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert 'path' in state and os.path.exists(state['path']), stdout.getvalue()
    assert 'mark' in state and state['mark'] == 'test1'
    assert len(state['revs']) <= info['storage_options']['cached_revisions_count'], stdout.getvalue()
    assert not os.path.exists(old_path), stdout.getvalue()
    assert state['revision'] == 'newer', stdout.getvalue()


async def test_downgrade_resource(stdout, installed_resource):
    state, info = installed_resource

    old_path = state['revs'][-1]['base_path']
    assert os.path.exists(old_path), stdout.getvalue()

    info['revision'] = 'newer'
    old_urls = state['revs'][-1]['urls']
    assert info['urls'] != old_urls
    info['urls'] = old_urls

    check_flag = asyncio.Future()
    old_check = updater.check

    def mocked_check(*args, **kwargs):
        nonlocal check_flag, old_check

        check_flag.set_result(True)
        return old_check(*args, **kwargs)

    with mock.patch('infra.yp_dru.updater.check') as _check:
        _check.side_effect = mocked_check
        await updater.process_resource(state, info)

    assert not check_flag.done()
    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert 'path' in state and os.path.exists(state['path']), stdout.getvalue()
    assert 'mark' in state and state['mark'] == 'test1', stdout.getvalue()
    assert len(state['revs']) <= info['storage_options']['cached_revisions_count'], stdout.getvalue()
    assert state['revision'] == 'newer', stdout.getvalue()

    assert os.readlink(state['symlink']) == old_path, stdout.getvalue()


async def test_rollback_on_upgrade(stdout, installed_resource):
    state, info = installed_resource

    info['revision'] = 'new'
    info['urls'] = ['raw:new']
    info['storage_options']['cached_revisions_count'] = 3

    old_notify_service = updater.notify_service

    def mocked_notify_service(state, info, process):
        if state['revision'] != 'older':
            raise updater.ResourceUpdateException(
                info['id'],
                state['revision'],
                'test notify failed'
            )
        return old_notify_service(state, info, process)

    with mock.patch('infra.yp_dru.updater.notify_service') as _notify_service:
        _notify_service.side_effect = mocked_notify_service
        await updater.process_resource(state, info)

    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert 'path' in state and os.path.exists(state['path']), stdout.getvalue()
    assert 'mark' in state and state['mark'] == 'test0', stdout.getvalue()
    assert state['revision'] == 'older', stdout.getvalue()


@pytest.mark.parametrize('command,expected_answer', [
    ('echo 123', '123'),
    ('echo -n 12 && echo 3', '123'),
])
async def test_exec_notify(stdout, uninstalled_resource, command, expected_answer):
    state, info = uninstalled_resource

    info['storage_options']['exec_action'] = {
        'command_line': command,
        'expected_answer': 'INVALID ANSWER',
    }

    await updater.process_resource(state, info)
    assert 'error' in state and state['error'], stdout.getvalue()

    info['storage_options']['exec_action']['expected_answer'] = expected_answer

    await updater.process_resource(state, info)
    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert 'path' in state and os.path.exists(state['path']), stdout.getvalue()
    assert 'mark' in state and state['mark'] == 'test2'


async def test_http_notify(stdout, uninstalled_resource, ping_server):
    state, info = uninstalled_resource

    url = ping_server.make_url('/ping')

    info['storage_options']['http_action'] = {
        'url': str(url),
        'expected_answer': 'Ok no',
    }

    await updater.process_resource(state, info)
    assert 'error' in state and state['error'], stdout.getvalue()

    info['storage_options']['http_action']['expected_answer'] = 'Ok'

    await updater.process_resource(state, info)
    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert 'path' in state and os.path.exists(state['path']), stdout.getvalue()
    assert 'mark' in state and state['mark'] == 'test2'


async def test_failed_rollback(stdout, installed_resource):
    state, info = installed_resource

    info['revision'] = 'new'
    info['urls'] = ['raw:new']

    def mocked_notify_service(state, info, process):
        raise updater.ResourceUpdateException(
            info['id'],
            state['revision'],
            'test notify failed'
        )

    with mock.patch('infra.yp_dru.updater.notify_service') as _notify_service:
        _notify_service.side_effect = mocked_notify_service
        await updater.process_resource(state, info)

    assert 'ready' in state and not state['ready'], stdout.getvalue()
    assert 'error' in state and state['error'], stdout.getvalue()
    assert 'reason' in state and state['reason'], stdout.getvalue()
    assert 'mark' in state and state['mark'] is None, stdout.getvalue()
    assert state['revision'] == 'old', stdout.getvalue()


async def test_remove_resource(stdout, installed_resource):
    old_state, info = installed_resource
    state = old_state.copy()
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()

    await updater.remove_resource(info['id'], state)
    assert state['ready'], stdout.getvalue()
    assert 'symlink' not in state and not os.path.exists(old_state['symlink'])
    assert 'path' not in state and not os.path.exists(old_state['path'])
    assert not state['revs']
    for rev in old_state['revs']:
        assert not os.path.exists(rev['path'])
    assert state['revision'] is None


async def test_process_multiple_resources(stdout, tempdir, installed_resource, uninstalled_resource):
    state1, info1 = installed_resource
    state2, info2 = uninstalled_resource
    infos = [info1, info2]

    for _ in range(2):
        statefile = os.path.join(tempdir, 'state.json')
        await updater.process_all(statefile, infos, None)
        assert os.path.exists(statefile), stdout.getvalue()

        states = json.load(open(statefile, 'r'))
        assert isinstance(states, dict), stdout.getvalue()
        assert info1['id'] in states['resources'], stdout.getvalue()
        assert info2['id'] in states['resources'], stdout.getvalue()

        state1_new = states['resources'][info1['id']]
        assert state1_new['ready'], stdout.getvalue()
        assert state1_new['last_check_time'], stdout.getvalue()
        assert state1_new['path'] == state1['path'], stdout.getvalue()
        assert state1_new['revision'] == state1['revision'], stdout.getvalue()
        assert state1_new['symlink'] == state1['symlink'], stdout.getvalue()
        assert state1_new['mark'] == 'test1', stdout.getvalue()

        state2_new = states['resources'][info2['id']]
        assert state2_new['ready'], stdout.getvalue()
        assert state2_new['last_check_time'], stdout.getvalue()
        assert state2_new['symlink'] and os.path.exists(state2_new['symlink']), stdout.getvalue()
        assert state2_new['path'] and os.path.exists(state2_new['path']), stdout.getvalue()
        assert state2_new['revision'] == info2['revision'], stdout.getvalue()
        assert state2_new['mark'] == 'test2', stdout.getvalue()


async def test_hardlinks(stdout, tempdir, uninstalled_resource):
    state, info = uninstalled_resource
    info['allow_deduplication'] = True
    info['urls'] = ['rbtorrent:unknown']

    def mocked_download(*args, **kwargs):
        raise Exception("fail")

    with mock.patch('infra.yp_dru.download.download_rbtorrent') as _download:
        _download.side_effect = mocked_download

        await updater.process_resource(state, info)

        assert _download.called_with(allow_deduplication=True)


async def test_dl_speed(stdout, tempdir, uninstalled_resource):
    state, info = uninstalled_resource
    info['max_download_speed'] = 10
    info['url'] = 'rbtorrent:unknown'

    def mocked_download(*args, **kwargs):
        raise Exception("fail")

    with mock.patch('infra.yp_dru.download.download_rbtorrent') as _download:
        _download.side_effect = mocked_download

        await updater.process_resource(state, info)

        assert _download.called_with(max_download_speed=10)


async def test_switch_destination(stdout, installed_resource):
    state, info = installed_resource
    await updater.check_resource(state, info)
    assert state['ready'], stdout.getvalue()

    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    old_symlink = state['symlink']

    info['revision'] = 'newer'
    info['urls'] = ['raw:x']
    info['storage_options']['verification']['checksum'] = 'MD5:9dd4e461268c8034f5c8564e155c67a6'
    info['storage_options']['destination'] = info['storage_options']['destination'] + '.new'

    await updater.process_resource(state, info)
    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'symlink' in state and os.path.exists(state['symlink']), stdout.getvalue()
    assert state['symlink'] == old_symlink + '.new'
    assert not os.path.exists(old_symlink), stdout.getvalue()


async def test_aliases(stdout, installed_resource):
    state, info = installed_resource

    storage_dir = info['storage_options']['storage_dir']
    old_path = state['revs'][-1]['path']
    old_base_path = state['revs'][-1]['base_path']
    destination = state['symlink']
    assert os.path.exists(old_path), stdout.getvalue()
    assert os.path.islink(destination) and os.readlink(destination) == state['base_path'], stdout.getvalue()

    info['revision'] = 'newer'
    old_urls = state['revs'][-1]['urls']
    assert info['urls'] != old_urls
    info['urls'] = old_urls

    await updater.process_resource(state, info)

    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'aliases' in state and len(state['aliases']) == 1, stdout.getvalue()
    alias = state['aliases'][0]
    destination = state['symlink']
    assert alias == os.path.join(storage_dir, state['revision']), stdout.getvalue()
    assert os.path.islink(alias) and os.readlink(alias) == old_path, stdout.getvalue()
    assert os.path.islink(destination) and os.readlink(destination) == old_base_path, stdout.getvalue()

    info['revision'] = 'newerer'
    await updater.process_resource(state, info)

    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert 'aliases' in state and len(state['aliases']) == 2, stdout.getvalue()
    aliases = state['aliases']
    destination = state['symlink']
    assert alias in aliases, stdout.getvalue()
    assert os.path.join(storage_dir, state['revision']) in aliases, stdout.getvalue()
    assert os.path.islink(aliases[0]) and os.readlink(aliases[0]) == old_path, stdout.getvalue()
    assert os.path.islink(aliases[1]) and os.readlink(aliases[1]) == old_path, stdout.getvalue()
    assert os.path.islink(destination) and os.readlink(destination) == old_base_path, stdout.getvalue()

    info['revision'] = 'newererer'
    info['urls'] = ['raw:newewerer']
    info['storage_options']['verification']['checksum'] = 'EMPTY:'
    info['storage_options']['cached_revisions_count'] = 1

    await updater.process_resource(state, info)

    destination = state['symlink']

    assert 'ready' in state and state['ready'], stdout.getvalue()
    assert not os.path.exists(old_path), stdout.getvalue()
    assert not os.path.exists(aliases[0]), stdout.getvalue()
    assert not os.path.exists(aliases[1]), stdout.getvalue()
    assert os.path.islink(destination) and os.readlink(destination) != old_base_path, stdout.getvalue()
    assert os.readlink(destination) == os.path.join(storage_dir, info['revision']), stdout.getvalue()


async def test_switch_after_fail(stdout, uninstalled_resource):
    state, info = uninstalled_resource
    info['urls'] = ['unsupported:url']

    with mock.patch('infra.yp_dru.updater.switch_path') as _switch_path:
        for _ in range(5):
            await updater.process_resource(state, info)
            assert not state['ready']
            assert not state['revs'][0]['aliases']
            _switch_path.assert_not_called()


async def test_broken_state(stdout, uninstalled_resource):
    state, info = uninstalled_resource
    storage_dir = info['storage_options']['storage_dir']

    info['storage_options']['storage_dir'] = os.path.join(storage_dir, 'nonexistent', 'path')
    await updater.process_resource(state, info)

    assert not state.get('ready'), stdout.getvalue()
    assert not state.get('in_progress'), stdout.getvalue()
    assert state.get('error'), stdout.getvalue()

    info['storage_options']['storage_dir'] = storage_dir
    await updater.process_resource(state, info)

    assert state.get('ready'), stdout.getvalue()
    assert not state.get('in_progress'), stdout.getvalue()
    assert not state.get('error'), stdout.getvalue()
