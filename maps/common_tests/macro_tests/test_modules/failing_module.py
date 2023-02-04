from maps.garden.sdk.core import Task, Demands, Creates
from maps.garden.sdk.resources import PythonResource
from maps.pylibs.utils.lib.process import check_output


class UnicodeUnfriendlyFailingTask(Task):
    def __call__(self, *args, **kwargs):
        check_output("dd if=/dev/urandom of=/dev/stdout bs=100 count=1 && false", shell=True)


def fill_graph(gb):
    gb.add_resource(
        PythonResource("something_that_is_never_created"))
    gb.add_task(
        Demands(),
        Creates("something_that_is_never_created"),
        UnicodeUnfriendlyFailingTask())


modules = [{
    "name": "failing_module",
    "fill_graph": fill_graph
}]
