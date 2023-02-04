from infra.orly.lib import validation
from infra.orly.proto import orly_pb2


def test_enum_to_string():
    assert validation.day_enum_to_string(0) == 'MON'


def test_validate_rule_id():
    table = [
        ('a', False),
        ('', False),
        ('a' * 300, False),
        ('12345', False),
        ('test_route', True),
        ('test-route-apply', True),
    ]
    for rule_id, ok in table:
        err = validation.validate_rule_id(rule_id)
        if ok:
            assert err is None
        else:
            assert err


def test_validate_operation_id():
    table = [
        ('a', False),
        ('', False),
        ('a' * 129, False),
        ('-12345', False),
        ('sas1_2344.search.yandex.net', False),
        ('sas1-2344.search.yandex.net', True),
    ]
    for op_id, ok in table:
        err = validation.validate_operation_id(op_id)
        if ok:
            assert err is None
        else:
            assert err


def test_validate_hh_mm():
    table = [
        ('a', False),
        ('', False),
        ('hh:mm', False),
        ('13:45f', False),
        ('13:45:', False),
        ('13:', False),
        ('13:99', False),
        ('24:33', False),
        ('13:00', True),
        ('00:59', True),
    ]
    for hhmm, ok in table:
        err = validation.validate_hh_mm(hhmm)
        if ok:
            assert err is None
        else:
            assert err


def test_validate_selector():
    sel = orly_pb2.Selector()
    # Add many match_ins
    for i in range(10):
        sel.match_in.add()
    assert validation.validate_selector(sel) == 'len(spec.selector.match_in) == 10 is too long, max=5'
    # Test empty key
    sel = orly_pb2.Selector()
    m_in = sel.match_in.add()
    assert validation.validate_selector(sel) == 'empty key in spec.selector.match_in[0]'
    # Test too long key
    m_in.key = 'a' * 67
    assert validation.validate_selector(sel) == 'len(spec.selector.match_in[0]) == 67 is too long, max=64'
    # Test empty value
    m_in.key = 'geo'
    assert validation.validate_selector(sel) == 'empty spec.selector.match_in[0].values'
    # Test too many values
    for i in range(11):
        m_in.values.append('x')
    assert validation.validate_selector(sel) == 'len(spec.selector.match_in[0].values) == 11 is too long, max=5'
    # Test empty value
    del m_in.values[:]
    m_in.values.append('')
    assert validation.validate_selector(sel) == 'empty v[0] in spec.selector.match_in[0]'
    # Test too long value
    del m_in.values[:]
    m_in.values.append('444')  # Okay
    m_in.values.append('8' * 80)
    assert validation.validate_selector(sel) == 'len(v[1]) == 80 in spec.selector.match_in[0], max=20'
    # Test okay
    del m_in.values[:]
    m_in.values.append('444')  # Okay
    assert validation.validate_selector(sel) is None
