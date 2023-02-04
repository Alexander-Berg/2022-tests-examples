from infra.reconf import ConfNode


def test_shared_initialized_by_arg():
    class foo(ConfNode):
        validate_class = False

    conf = foo({'children': {'one': {}}}, shared={'present': 'yep'})

    assert {'present': 'yep'} == conf.shared
    assert {'present': 'yep'} == conf['children']['one:foo'].shared
