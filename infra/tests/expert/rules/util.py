import logging

import pytest

from sepelib.core import config
from walle.constants import HostType
from walle.expert import juggler
from walle.expert.decision import Decision
from walle.expert.decisionmakers import get_decision_maker
from walle.expert.types import WalleAction
from walle.hosts import Host
from walle.projects import Project

log = logging.getLogger(__name__)


@pytest.fixture(params=(False, True))
def fast(request):
    return request.param


def make_decision(host, reasons: juggler.Reasons, fast, rule=None) -> Decision:
    project = Project(id="test-project", name="Test Project", tags=[config.get_value("infiniband.involvement_tag")])
    decision_maker = get_decision_maker(project)
    if rule:
        decision_maker._rules = [rule]
    return decision_maker.make_decision(host, reasons, fast=fast)


def check_decision(
    reasons,
    fast,
    action,
    reason=None,
    params=None,
    checks=None,
    failure_type=None,
    restrictions=None,
    host=None,
    hw_checks_enabled=False,
    rule=None,
    failures=None,
):
    if host is None:
        host = Host(type=HostType.SERVER)

    if reason is None:
        if action == WalleAction.HEALTHY:
            reason = "Host is healthy." if hw_checks_enabled else "Host is available."
        else:
            assert False

    decision = make_decision(host, reasons, fast, rule=rule)
    expected_decision = Decision(
        action, reason, params, checks=checks, failure_type=failure_type, restrictions=restrictions, failures=failures
    )
    log.info("%s: decision", decision)
    log.info("%s: expected decision", expected_decision)
    assert decision == expected_decision
