from infra.reconf_juggler import Check, CheckOptFactory
from infra.reconf_juggler.opts.namespaces import NamespaceHandler


def test_opt_factory():
    class RedefinedNamespaceHandler(NamespaceHandler):
        def get_default_value(self, key):
            return 'redefined_namespace'

    class Factory(CheckOptFactory):
        def create_handler(self, handler_class, check):
            if issubclass(handler_class, NamespaceHandler):
                return RedefinedNamespaceHandler(check)

            return super().create_handler(handler_class, check)

    class Foo(Check):
        validate_class = False
        opt_handlers = ('_namespace',)

    check = Foo({'children': {'0': {}}}, opt_factory=Factory())

    expected = \
        {'children': {'0:Foo': {'namespace': 'redefined_namespace'}},
         'namespace': 'redefined_namespace'}

    assert expected == check.build()
