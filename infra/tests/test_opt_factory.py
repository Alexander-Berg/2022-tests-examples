from infra.reconf import ConfNode, ConfSet, OptFactory, OptHandler, SubnodesHandler


# inits
def test_conf_init_without_factory():
    conf = ConfNode({})
    assert conf.opt_factory is None


def test_confset_init_without_factory():
    confset = ConfSet()
    assert confset.opt_factory is None


class Factory(OptFactory):
    pass


def test_conf_init_with_factory():
    conf = ConfNode({}, opt_factory=Factory())
    assert isinstance(conf.opt_factory, Factory)


def test_confset_init_with_factory():
    confset = ConfSet(opt_factory=Factory())
    assert isinstance(confset.opt_factory, Factory)


# factory should be propagated to the leaves
def test_factory_propogation():
    conf = ConfNode({'children': {'0': {}}}, opt_factory=Factory())
    assert isinstance(conf['children']['0:ConfNode'].opt_factory, Factory)


# behaviors testing
class OriginalOpt(OptHandler):
    @staticmethod
    def get_defaults():
        return {'foo': 'bar'}


class Foo(ConfNode):
    validate_class = False
    opt_handlers = ('foo', 'baz')
    foo = OriginalOpt
    baz = None


def test_default_factory_behavior():
    conf = Foo({'children': {'0': {}}}, opt_factory=OptFactory()).build()
    assert {'children': {'0:Foo': {'foo': 'bar'}}, 'foo': 'bar'} == conf


def test_custom_factory_behavior():
    class RedefinedOpt(OptHandler):
        @staticmethod
        def get_defaults():
            return {'foo': 'baz'}

    class OptMutator(OptFactory):
        def create_handler(self, handler_class, conf):
            if issubclass(handler_class, SubnodesHandler):
                return super().create_handler(handler_class, conf)
            return RedefinedOpt(bound=conf)

    conf = Foo({'children': {'0': {}}}, opt_factory=OptMutator()).build()
    assert {'children': {'0:Foo': {'foo': 'baz'}}, 'foo': 'baz'} == conf
