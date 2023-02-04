import infra.reconf_juggler
import infra.reconf_juggler.opts.flaps


class AbstractTestingCheck(infra.reconf_juggler.Check):
    opt_handlers = ('_flap',)  # flaps only, for shortage
    validate_class = False  # disable docstrings check etc


def test_default():
    # flaps should absent by default
    # https://st.yandex-team.ru/RUNTIMECLOUD-14267#5d690d14a2b79e001d656f8f
    assert {} == AbstractTestingCheck({}).build()


def test_some_flap_opt_defined():
    class CustomFlap(infra.reconf_juggler.opts.flaps.FlapHandler):
        @staticmethod
        def get_defaults():
            return {
                'flaps': {
                    'critical': None,
                    'stable': 1800,
                }
            }

    class CustomCheck(AbstractTestingCheck):
        _flap = CustomFlap

    check = CustomCheck({}).build()
    assert {'flaps': {'critical': None, 'stable': 1800}} == check


def test_aggregates_levels():
    class F(infra.reconf_juggler.opts.flaps.FlapHandler):
        @staticmethod
        def get_defaults():
            return {'flaps': {'stable': 1200}}

    class C(AbstractTestingCheck):
        _flap = F

    c = C({
        'children': {
            0: {
                'children': {'endpoint': None, 'aggregate1': {}},
            },
            1: {
                'children': {'endpoint': None},
            },
            2: {
                'children': {'aggregate2': {}},
            },
            3: {  # treated as endpoint - empty children
                'children': {},
            },
            4: {},  # treated as endpoint - no children
        }
    })

    expected = {
        'children': {
            '0:C': {
                'children': {  # mixed, no flaps
                    'aggregate1:C': {  # endpoint -- should be flaps
                        'flaps': {'stable': 1200}
                    },
                    'endpoint:C': None
                }
            },
            '1:C': {  # only filter (endpoint), should be flaps
                'children': {
                    'endpoint:C': None
                },
                'flaps': {'stable': 1200}
            },
            '2:C': {  # contain subnodes (metaaggregate) - no flaps
                'children': {
                    'aggregate2:C': {  # endpoint -- should be flaps
                        'flaps': {'stable': 1200}
                    }
                }
            },
            '3:C': {  # endpoint -- should be flaps
                'children': {},
                'flaps': {'stable': 1200}
            },
            '4:C': {  # endpoint -- should be flaps
                'flaps': {'stable': 1200}
            }
        }
    }

    assert expected == c.build()
