from infra.reconf import ConfNode, ConfSet, OptHandler, iterate_depth_first


class foo(ConfNode):
    validate_class = False


class bar(ConfNode):
    validate_class = False


class baz(ConfNode):
    validate_class = False


def test_conf_init():
    assert {'children': {}} == foo({'children': {}})


def test_conf_init_recursive():
    conf = bar({'children': {'0': {'children': {'00': {}}}}})
    assert {'children': {'0:bar': {'children': {'00:bar': {}}}}} == conf


def test_confset_init():
    class FooConfSet(ConfSet):
        branches = (foo, bar)

    cset = FooConfSet({'0': {}, '1': {}})
    assert {'0:foo': {}, '1:foo': {}, '0:bar': {}, '1:bar': {}} == cset


def test_confset_repr():
    class FooConfSet(ConfSet):
        branches = (foo, bar)

    cset = FooConfSet({'0': {}})
    assert "FooConfSet({'0:foo': foo({}), '0:bar': bar({})})" == repr(cset)


def test_confset_init_recursive():
    class BarConfSet(ConfSet):
        branches = (foo,)

    cset = BarConfSet({'0': {'children': {'00': {}}}, '1': {}})
    assert {"0:foo": {"children": {"00:foo": {}}}, "1:foo": {}} == cset


def test_conf_origin():
    class BarConfSet(ConfSet):
        branches = (foo,)

    cset = BarConfSet({'0': {}, '1': {}})
    assert '0' == cset['0:foo'].origin
    assert '1' == cset['1:foo'].origin


def test_conf_build():
    class Hdl(OptHandler):
        @staticmethod
        def get_defaults():
            return {'built': False}

    class Foo(ConfNode):
        validate_class = False
        default_handler = Hdl  # will allow to write any k-v in a conf dict

        def build(self):
            self['built'] = True
            return super().build()

    conf = Foo({'children': {'0': {'children': {'00': {}}}}}).build()
    expected = \
        {'built': True,
         'children': {'0:Foo': {'built': True,
                                'children': {'00:Foo': {'built': True}}}}}
    assert expected == conf


def test_conf_iterate_subnodes_method():
    conf = foo({
        'children': {
            '0': {
                'children': {
                    '00': {},
                    '01': None,
                },
            },
        },
    })

    expected = [
        ('00:foo', {}),
        ('0:foo', {'children': {'00:foo': {}, '01:foo': None}}),
    ]

    assert expected == list(conf.iterate_subnodes())


def test_conf_get_upward_method():
    conf = foo({'children': {'0': {}}})

    assert conf['children']['0:foo'].get_upward() is conf['children']
    assert conf['children']['0:foo'].get_upward(type_=ConfSet) is conf['children']
    assert conf['children']['0:foo'].get_upward(type_=ConfNode) is conf


def test_confset_iterate_nodes_method():
    class BarConfSet(ConfSet):
        branches = (foo,)

    tree = {
        '0': {
            'children': {'00': {}}
        },
        '1': {
            'children': {
                '01': {
                    'children': {'001': None}
                },
            }
        },
    }
    cset = BarConfSet(tree)

    expected = [
        ('00:foo', {}),  # this is not trimmed conf (it's real but empty)
        ('0:foo', {'children': {'00:foo': {}}}),
        ('01:foo', {'children': {'001:foo': None}}),
        ('1:foo', {'children': {'01:foo': {'children': {'001:foo': None}}}}),
    ]

    assert expected == list(cset.iterate_nodes())


def test_iterate_depth_first_func__confs_struct():
    class BarConfSet(ConfSet):
        branches = (foo,)

    tree = {
        '0': {
            'children': {
                '00': {}  # real but empty conf, not trimmed
            }
        },
        '1': {
            'children': {
                '01': {
                    'children': {
                        '001': None  # endpoint, should be skipped by iterator
                    }
                },
            }
        },
    }
    cset = BarConfSet(tree)

    expected = [
        ('00:foo', {}),  # real but empty conf, not trimmed
        ('0:foo', {'children': {'00:foo': {}}}),
        ('01:foo', {'children': {'001:foo': None}}),
        ('1:foo', {'children': {'01:foo': {'children': {'001:foo': None}}}}),
    ]

    assert expected == list(iterate_depth_first(cset))


def test_iterate_depth_first_func__raw_dicts():
    tree = {
        '0:foo': {
            'children': {
                '00:foo': {}  # trimmed conf (empty dict), should be skipped by iterator
            }
        },
        '1:foo': {
            'children': {
                '01:foo': {
                    'children': {
                        '001:foo': None  # endpoint, should be skipped by iterator
                    }
                },
            }
        },
    }

    expected = [
        ('0:foo', {'children': {'00:foo': {}}}),
        ('01:foo', {'children': {'001:foo': None}}),
        ('1:foo', {'children': {'01:foo': {'children': {'001:foo': None}}}})
    ]

    assert expected == list(iterate_depth_first(tree))


def test_children_methods():
    class C(ConfNode):
        validate_class = False

    c = C({
        'children': {
            0: {
                'children': {'endpoint': None, 'conf': {}},
            },
            1: {
                'children': {'endpoint': None},
            },
            2: {
                'children': {'conf': {}},
            },
            3: {  # treated as endpoint - empty children
                'children': {},
            },
            4: {},  # treated as endpoint - no children
        }
    })

    assert c['children']['0:C'].has_endpoints()
    assert c['children']['0:C'].has_subnodes()

    assert c['children']['1:C'].has_endpoints()
    assert not c['children']['1:C'].has_subnodes()

    assert not c['children']['2:C'].has_endpoints()
    assert c['children']['2:C'].has_subnodes()

    assert not c['children']['3:C'].has_endpoints()
    assert not c['children']['3:C'].has_subnodes()

    assert not c['children']['4:C'].has_endpoints()
    assert not c['children']['4:C'].has_subnodes()
