import pytest

from idm.core.constants.action import ACTION
from idm.core.constants.node_relocation import RELOCATION_STATE
from idm.core.models import Action
from idm.core.node_pipeline import NodePipeline

pytestmark = pytest.mark.django_db


def test_rerun_workflow_create_mass_action(simple_system):
    Action.objects.filter(action=ACTION.MASS_ACTION).delete()
    simple_system.nodes.active().update(relocation_state=RELOCATION_STATE.FIELDS_COMPUTED)
    nodes = simple_system.nodes.active().filter(relocation_state=RELOCATION_STATE.FIELDS_COMPUTED)
    assert nodes is not None
    NodePipeline(simple_system).run()
    assert Action.objects.filter(action=ACTION.MASS_ACTION) is not None


def test_rerun_workflow_empty_queryset(simple_system):
    Action.objects.filter(action=ACTION.MASS_ACTION).delete()
    nodes = simple_system.nodes.active().filter(relocation_state=RELOCATION_STATE.FIELDS_COMPUTED)
    assert not nodes
    NodePipeline(simple_system).run()
    assert not Action.objects.filter(action=ACTION.MASS_ACTION)


def test_apply_f_for_active_descendants(simple_system):
    processed_nodes = []

    def f(node, parent):
        processed_nodes.append((node, parent))

    simple_system.nodes.filter(parent__isnull=False).delete()
    root = simple_system.root_role_node
    root.relocation_state = RELOCATION_STATE.CT_COMPUTED
    root.save(update_fields=['relocation_state'])
    node_1 = simple_system.nodes.create(slug='node_1', parent=root, relocation_state=RELOCATION_STATE.SUPERDIRTY)
    node_2 = simple_system.nodes.create(slug='node_2', parent=node_1)
    node_3 = simple_system.nodes.create(slug='node_3', parent=root, relocation_state=RELOCATION_STATE.FIELDS_COMPUTED)
    node_4 = simple_system.nodes.create(slug='node_4', parent=node_3)
    node_5 = simple_system.nodes.create(slug='node_5', parent=node_4, state='deprived')

    NodePipeline(simple_system).apply_f_for_active_descendants(root, RELOCATION_STATE.CT_COMPUTED, f)
    assert processed_nodes == [(node_3, root), (node_4, node_3)]
    node_3.refresh_from_db()
    assert node_3.relocation_state == RELOCATION_STATE.GOOD
