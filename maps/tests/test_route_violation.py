from ya_courier_backend.models import PlanNodeType

from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_violation import is_plan_violated

from .utils import gen_plan_node, gen_fact_garage, gen_fact_order


def test_empty_route_has_no_violation():
    assert is_plan_violated([], []) is False


def test_plan_is_not_violated_when_order_sequence_is_unchanged():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_order(seq_pos=0, number='first'),
        gen_fact_order(seq_pos=1, number='second'),
    ]

    assert is_plan_violated(plan, fact) is False


def test_plan_is_violated_when_order_sequence_is_changed():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_order(seq_pos=0, number='second'),
        gen_fact_order(seq_pos=1, number='first'),
    ]

    assert is_plan_violated(plan, fact) is True


def test_plan_is_violated_when_order_is_added_to_the_end():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_order(seq_pos=0, number='first'),
        gen_fact_order(seq_pos=1, number='second'),
        gen_fact_order(seq_pos=2, number='third'),
    ]

    assert is_plan_violated(plan, fact) is True


def test_plan_is_violated_when_order_is_added_to_the_middle():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_order(seq_pos=0, number='first'),
        gen_fact_order(seq_pos=1, number='third'),
        gen_fact_order(seq_pos=2, number='second'),
    ]

    assert is_plan_violated(plan, fact) is True


def test_plan_is_not_violated_when_garage_is_not_in_fact():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.garage, number='garage'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=2, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_order(seq_pos=0, number='first'),
        gen_fact_order(seq_pos=1, number='second'),
    ]

    assert is_plan_violated(plan, fact) is False


def test_plan_is_not_violated_when_depot_is_not_in_fact():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.garage, number='garage'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.depot, number='depot'),
        gen_plan_node(seq_pos=2, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=3, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_garage(seq_pos=0, number='garage'),
        gen_fact_order(seq_pos=1, number='first'),
        gen_fact_order(seq_pos=2, number='second'),
    ]

    assert is_plan_violated(plan, fact) is False


def test_plan_is_violated_when_order_number_is_changed():
    plan = [gen_plan_node(seq_pos=0, type=PlanNodeType.order, number='initial')]
    fact = [gen_fact_order(seq_pos=0, number='final')]

    assert is_plan_violated(plan, fact) is True


def test_plan_is_violated_when_node_type_is_changed():
    plan = [
        gen_plan_node(seq_pos=0, type=PlanNodeType.order, number='first'),
        gen_plan_node(seq_pos=1, type=PlanNodeType.order, number='second'),
    ]
    fact = [
        gen_fact_order(seq_pos=0, number='final'),
        gen_fact_garage(seq_pos=1, number='second'),
    ]

    assert is_plan_violated(plan, fact) is True
