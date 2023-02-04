from maps.garden.sdk.core import Task, Resource

from maps.garden.sdk.extensions.easy_builder import EasyBuilder


class MockGraphBuilder:
    def add_resource(self, *argc, **argv):
        pass

    def add_task(self, *argc, **argv):
        pass


def test_simple():
    graph_builder = MockGraphBuilder()

    add_task = EasyBuilder(graph_builder)

    add_task(
        Task()
    ).creates(
        a='a'
    ).demands(
        b='b'
    )


def test_class_parameters():
    graph_builder = MockGraphBuilder()

    add_task = EasyBuilder(graph_builder)

    add_task(
        Task()
    ).creates(
        a=Resource('a')
    ).demands(
        b=Resource('b')
    )
