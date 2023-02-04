from intranet.table_flow.src.rules import domain_objs


INP_DICT = {
    'in_fields': {
        'first_in': {'field_type': 'int'},
        'second_in': {'field_type': 'str'},
    },
    'out_fields': {
        'first_out': {'field_type': 'str'},
        'second_out': {'field_type': 'str'},
    },
    'cases': [
        {
            'checks': {
                'first_in': {'gt': '1'},
                'second_in': {'eq': 'fds'},
            },
            'out': {
                'first_out': 'wer1',
                'second_out': 'qwe1',
            },
        },
        {
            'checks': {
                'first_in': {'eq': '2'},
                'second_in': {'eq': 'fds'},
            },
            'out': {
                'first_out': 'wer2',
                'second_out': 'qwe2',
            },
        },
    ]
}


def test_deserialize_rule():
    rule_obj = domain_objs.Rule.from_dict(INP_DICT)
    assert rule_obj.in_fields == {
        'first_in': domain_objs.Field('int'),
        'second_in': domain_objs.Field('str'),
    }
    assert rule_obj.out_fields == {
        'first_out': domain_objs.Field('str'),
        'second_out': domain_objs.Field('str'),
    }
    assert rule_obj.cases == [
        domain_objs.Case(**case_kw)
        for case_kw in INP_DICT['cases']
    ]
    for case in rule_obj.cases:
        for check in case.checks.values():
            assert isinstance(check, domain_objs.Check)


def test_serialize_rule():
    assert domain_objs.Rule.from_dict(INP_DICT).as_dict() == INP_DICT
