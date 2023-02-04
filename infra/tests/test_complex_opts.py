from infra.reconf import ConfNode, DictConf, ListConf, OptHandler, OptFactory
from infra.reconf.resolvers import AbstractResolver


class L0_Handler(OptHandler):
    def get_defaults(self):
        return {'l0': L1_List(bound=self._bound)}


class L0_Node(ConfNode):
    validate_class = False
    opt_handlers = ('hdl',)
    hdl = L0_Handler


class L1_Handler(OptHandler):
    def get_defaults(self):
        return [L2_Dict(bound=self._bound)]


class L1_List(ListConf):
    handler = L1_Handler


class L2_Handler(OptHandler):
    def get_defaults(self):
        return {'l2': 'string'}


class L2_Dict(DictConf):
    handlers = ('hdl',)  # not 'opt_handlers'!
    hdl = L2_Handler


def test_build():
    assert {'l0': [{'l2': 'string'}]} == L0_Node().build()


def test_essentials_propagation():
    class Factory(OptFactory):
        pass

    class Resolver(AbstractResolver):
        pass

    factory = Factory()
    resolver = Resolver()
    conf = L0_Node(opt_factory=factory, resolver=resolver).build()

    assert conf['l0'].opt_factory is factory
    assert conf['l0'].resolver is resolver

    assert conf['l0'][0].opt_factory is factory
    assert conf['l0'][0].resolver is resolver


def test_opt_factory():
    class L1_Handler_Replaced(OptHandler):
        def get_defaults(self):
            return [L2_Dict(bound=self._bound), 'l1_replaced']

    class L2_Handler_Replaced(OptHandler):
        def get_defaults(self):
            return {'l2': 'replaced'}

    class Factory(OptFactory):
        def create_handler(self, handler_class, conf):
            if issubclass(handler_class, L1_Handler):
                return L1_Handler_Replaced(bound=conf)
            elif issubclass(handler_class, L2_Handler):
                return L2_Handler_Replaced(bound=conf)

            return super().create_handler(handler_class, conf)

    expected = {'l0': [{'l2': 'replaced'}, 'l1_replaced']}
    assert expected == L0_Node(opt_factory=Factory()).build()
