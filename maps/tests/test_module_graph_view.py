import http.client
import pytest

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType, ModuleAutostarter
from maps.garden.libs_server.common.contour_manager import ContourManager

from .common import DEFAULT_SYSTEM_CONTOUR


USER_CONTOUR="apollo_test_contour"


@pytest.fixture
def prepare_db(db, module_helper):
    """
SYSTEM CONTOUR:
K -> J              S

USER CONTOUR:
A -> B -> C <- D    G -> H
|         ^
v         |
E --------
    """
    contour_manager = ContourManager(db)
    contour_manager.create(USER_CONTOUR, "apollo")

    module_helper.add_module_to_system_contour(
        ModuleTraits(
            name="K",
            displayed_name="The K",
            type=ModuleType.SOURCE
        ))
    module_helper.add_module_to_system_contour(
        ModuleTraits(
            name="J",
            displayed_name="The J",
            type=ModuleType.DEPLOYMENT,
            configs=["K"],
            autostarter=ModuleAutostarter(trigger_by=["K", "J"])
        ))
    module_helper.add_module_to_system_contour(
        ModuleTraits(
            name="S",
            displayed_name="Module without links",
            type=ModuleType.DEPLOYMENT,
        ))

    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="A",
            type=ModuleType.SOURCE,
        ),
        user_contour=USER_CONTOUR)
    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="B",
            type=ModuleType.MAP,
            sources=["A"],
        ),
        user_contour=USER_CONTOUR)
    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="C",
            type=ModuleType.DEPLOYMENT,
            configs=["B", "D"],
            autostarter=ModuleAutostarter(trigger_by=["E", "D"])
        ),
        user_contour=USER_CONTOUR)
    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="D",
            type=ModuleType.SOURCE
        ),
        user_contour=USER_CONTOUR)
    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="E",
            type=ModuleType.REDUCE,
            autostarter=ModuleAutostarter(trigger_by=["A"])
        ),
        user_contour=USER_CONTOUR)
    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="G",
            type=ModuleType.SOURCE
        ),
        user_contour=USER_CONTOUR)
    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="H",
            type=ModuleType.DEPLOYMENT,
            sources=["G"],
            autostarter=ModuleAutostarter(trigger_by=["G"])
        ),
        user_contour=USER_CONTOUR)


def test_get_contour_not_specified(garden_client, prepare_db):
    response = garden_client.get("module_graph/")
    assert response.status_code == http.client.BAD_REQUEST


def test_get_contour_not_found(garden_client, prepare_db):
    response = garden_client.get("module_graph/?contour=not_a_contour")
    assert response.status_code == http.client.NOT_FOUND


def test_get_module_not_found(garden_client, prepare_db):
    response = garden_client.get(f"module_graph/?contour={USER_CONTOUR}&module=not_a_module")
    assert response.status_code == http.client.NOT_FOUND


@pytest.mark.parametrize(
    "contour_name",
    [
        DEFAULT_SYSTEM_CONTOUR,         # J, K, S
        USER_CONTOUR                    # A, B, C, D, E, G, H
    ]
)
def test_get_all_modules(garden_client, prepare_db, contour_name):
    response = garden_client.get(f"module_graph/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    "contour_name,module_name",
    [
        (DEFAULT_SYSTEM_CONTOUR, "K"),  # J, K
        (DEFAULT_SYSTEM_CONTOUR, "S"),  # S
        (USER_CONTOUR, "A"),            # A, B, C, E
        (USER_CONTOUR, "B"),            # A, B, C
        (USER_CONTOUR, "C"),            # A, B, C, D, E
        (USER_CONTOUR, "D"),            # C, D
        (USER_CONTOUR, "E"),            # A, C, E
        (USER_CONTOUR, "G")             # G, H
    ]
)
def test_get_module_subgraph(garden_client, prepare_db, contour_name, module_name):
    response = garden_client.get(f"module_graph/?contour={contour_name}&module={module_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    "contour_name,module_name,max_depth",
    [
        (DEFAULT_SYSTEM_CONTOUR, "K", 1),
        (DEFAULT_SYSTEM_CONTOUR, "J", 1),
        (DEFAULT_SYSTEM_CONTOUR, "S", 1),
        (USER_CONTOUR, "A", 1),
        (USER_CONTOUR, "A", 2),
        (USER_CONTOUR, "C", 1),
        (USER_CONTOUR, "C", 2),
    ]
)
def test_get_module_subgraph_with_depth_limit(garden_client, prepare_db, contour_name, module_name, max_depth):
    response = garden_client.get(f"module_graph/?contour={contour_name}&module={module_name}&max_depth={max_depth}")
    assert response.status_code == http.client.OK
    return response.get_json()
