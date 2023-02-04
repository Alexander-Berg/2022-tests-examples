from infra.ya_salt.lib import pbutil
from infra.ya_salt.proto import ya_salt_pb2

TEST_RESULTS = {
    'file_|'
    '-/place/porto_layers_|'
    '-/place/porto_layers_|'
    '-directory': {'__id__': '/place/porto_layers',
                   '__run_num__': 72,
                   'changes': {'Something': 'changed'},
                   'comment': 'Directory /place/porto_layers is in the correct state',
                   'duration': 1.624,
                   'name': '/place/porto_layers',
                   'pchanges': {},
                   'result': True,
                   'start_time': '11:12:00.052721',
                   'started': 1547194320},
    'pkg_|'
    '-yandex-diskmanager_|'
    '-yandex-diskmanager_|'
    '-installed': {'__id__': 'yandex-diskmanager',
                   '__run_num__': 176,
                   'changes': {},
                   'comment': "Already installed",
                   'duration': 1.314,
                   'name': 'yandex-diskmanager',
                   'result': True,
                   'start_time': '11:12:00.696420',
                   'started': 1547194320},
    'schedule_|'
    '-sync_dotfiles_with_master_|'
    '-sync_dotfiles_with_master_|'
    '-absent': {'__run_num__': 49,
                'changes': {},
                'comment': "State 'schedule.absent' was not found "
                           "in SLS 'system.dotfiles.schedule'\n"
                           "Reason: 'schedule.absent' is not available.\n",
                'name': 'sync_dotfiles_with_master',
                'result': False,
                'started': 1547194319},
    'market.config.rfsd.db': {
        'comment': 'Unable to unmount',
        'name': '/var/remote-log/db',
        'started': 1557857245,
        'start_time': '21:07:25.535397',
        'result': None,
        'duration': 145.083,
        '__run_num__': 269,
        'changes': {
            'umount': "Forced unmount..."
        },
        '__id__': 'market.config.rfsd.db'}
}


def test_update_state_results():
    salt_status = ya_salt_pb2.SaltStatus()
    pbutil.update_status_from_result(TEST_RESULTS, salt_status)
    expected = ya_salt_pb2.SaltStatus().state_results
    m = expected.add()
    m.id = '/place/porto_layers'
    m.ok = True
    m.changes = '{"Something": "changed"}'
    m = expected.add()
    m.id = 'yandex-diskmanager'
    m.ok = True
    m.changes = ''
    m = expected.add()
    m.id = 'sync_dotfiles_with_master'
    m.ok = False
    m.changes = ''
    m = expected.add()
    m.id = 'market.config.rfsd.db'
    m.ok = False
    m.changes = '{"umount": "Forced unmount..."}'
    # Transform to dict for lookups
    expected = {m.id: m for m in expected}
    for r in salt_status.state_results:
        exp = expected.pop(r.id, None)
        assert exp is not None, r.id
        assert r.ok == exp.ok
        assert r.changes == exp.changes
    assert not expected, "not all states transformed"


def test_pb_to_dict():
    d = pbutil.pb_to_dict(ya_salt_pb2.ObjectMeta(version='1.1'))
    assert d == {'version': '1.1'}


def test_dict_from_buf():
    # Test unknown extension
    assert pbutil.dict_from_buf('buf', 'haml') == (None, 'unsupported format: haml')
    # Test bad yaml content
    d, err = pbutil.dict_from_buf(',,-,,,,--666<-::<<<bad:!!- not good', 'yaml')
    assert err is not None and err.startswith('failed to parse as YAML')
    # Test not a mapping
    d, err = pbutil.dict_from_buf('good', 'yaml')
    assert err is not None and err.startswith('content is not a mapping')


def test_pb_digest():
    m = ya_salt_pb2.ObjectMeta(version='1.b-test-pb-digest')
    assert pbutil.pb_digest(m) == 'b8e84fa7f8acbbbd1820369b08627b95f878fa02'
