from infra.orly.lib import selector
from infra.orly.proto import orly_pb2


def test_match():
    sel = orly_pb2.Selector()
    op = orly_pb2.Operation()
    op.meta.labels['ctype'] = 'production'
    # Test no required key in operation
    m_in = sel.match_in.add()
    m_in.key = 'geo'
    m_in.values.append('sas')
    assert selector.match(sel, op) == "no key='geo' in op labels"
    # Test no value for required key
    op.meta.labels['geo'] = 'vla'
    assert selector.match(sel, op) == "label geo=vla did not match selector values: ['sas']"
    # Test OK
    op.meta.labels['geo'] = 'sas'
    assert selector.match(sel, op) is None
