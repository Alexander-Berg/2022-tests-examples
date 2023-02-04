from maps.garden.sdk.core import Demands, Creates, Task
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.utils import KB
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties


ANTIQUE_PLAQUE_RESOURCE_NAME = "antique_plaque"
CAPTURED_FLAG_RESOURCE_NAME = "captured_flag"
MODULE_NAME = "test_module"


class ExceptionalException(BaseException):
    pass


class CaptureTheFlagTask(Task):
    propagate_properties = EnsureEqualProperties(["inscription"])

    def predict_consumption(self, demands, creates):
        return {
            # Demand 1 cpu per byte of data
            "cpu": demands["plaque"].size["bytes"],
            # 640K ought to be enough for anybody
            "ram": 640 * KB,
            "tmpfs": True,
            "porto_layer": False,
        }

    def __call__(self, plaque, flag):
        flag.value = plaque.value * 2


class ExceptionalTask(Task):
    def propagate_properties(self, demands, creates):
        raise ExceptionalException("Wish You Were Here")

    def predict_consumption(self, demands, creates):
        raise ExceptionalException("Shine on You Crazy Diamond")

    def __call__(self, *args, **kwargs):
        raise ExceptionalException("Another Brick in the Wall")


def fill_graph(graph_builder, regions):
    graph_builder.add_resource(
        FlagResource(
            name=CAPTURED_FLAG_RESOURCE_NAME
        )
    )
    graph_builder.add_task(
        demands=Demands(
            plaque=ANTIQUE_PLAQUE_RESOURCE_NAME
        ),
        creates=Creates(
            flag=CAPTURED_FLAG_RESOURCE_NAME
        ),
        task=CaptureTheFlagTask()
    )
