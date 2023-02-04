from intranet.trip.src.lib.registry import Registry


def test_registry_workflow():
    test_registry = Registry()

    class BaseHandler:
        def run(self) -> str:
            return self.__class__.__name__

    @test_registry.add('event1', 'event2')
    class Handler12(BaseHandler):
        ...

    @test_registry.add('event3')
    class Handler3(BaseHandler):
        ...

    assert test_registry.get('event1')().run() == 'Handler12'
    assert test_registry.get('event2')().run() == 'Handler12'
    assert test_registry.get('event3')().run() == 'Handler3'
    assert test_registry.get('event4') is None
