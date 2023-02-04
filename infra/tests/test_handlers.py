from infra.reconf import ConfNode, OptHandler


def test_disabled_handler():
    class Hdl(OptHandler):
        @staticmethod
        def get_defaults():
            return {'test_key': 'test_val'}

    class foo(ConfNode):
        validate_class = False
        opt_handlers = ('hdl',)
        hdl = Hdl

    assert {'test_key': 'test_val'} == foo().build()

    class bar(foo):
        validate_class = False
        hdl = None

    assert {} == bar().build()
