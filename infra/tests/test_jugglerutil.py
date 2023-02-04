import json
from infra.ya_salt.lib import jugglerutil


def test_check():
    Check = jugglerutil.Check
    c = Check("test-juggler-util", "CRIT", "Some tin won")
    assert c.to_event('test-localhost') == {'description': 'Some tin won',
                                            'host': 'test-localhost',
                                            'instance': '',
                                            'service': 'test-juggler-util',
                                            'status': 'CRIT',
                                            'tags': []}
    c = Check.make_walle("test-juggler-util", "OK", "All went OK")
    d = c.to_event('test-localhost')
    # Pop description, because it contains timestamp, no need to compare it
    desc = json.loads(d.pop('description'))
    assert desc['timestamp'] > 0
    assert desc['reason']
    assert d == {'host': 'test-localhost',
                 'instance': '',
                 'service': 'test-juggler-util',
                 'status': 'OK',
                 'tags': []}
