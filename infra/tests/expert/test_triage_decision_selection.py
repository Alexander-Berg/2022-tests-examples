import pytest

from infra.walle.server.tests.expert.util import monkeypatch_health_data, mock_host, run_triage
from walle.application import app
from walle.expert import triage
from walle.expert.decision import Decision
from walle.expert.types import WalleAction
from walle.hosts import DecisionStatus, HostDecision


@pytest.mark.parametrize(
    ["python_decision", "go_decision", "python_win"],
    [
        (Decision(WalleAction.REBOOT, "python reason"), Decision.healthy("go reason"), True),
        (Decision(WalleAction.REBOOT, "python reason"), Decision(WalleAction.REBOOT, "go reason"), True),
        (Decision.healthy("python reason"), Decision.healthy("go reason"), True),
        (Decision.healthy("python reason"), Decision(WalleAction.REBOOT, "go reason"), False),
    ],
)
@pytest.mark.parametrize("is_rule_switched", [True, False])
def test_python_vs_go_decision_selection(
    mp, walle_test, mock_health_data, python_decision, go_decision, python_win, is_rule_switched
):
    rule_name = "some_test_rule_name"
    go_decision.rule_name = python_decision.rule_name = rule_name
    if is_rule_switched:
        settings = app.settings()
        settings.dmc_rules_switched = [rule_name]
        settings.save()

    host = mock_host(walle_test, 0, python_decision, DecisionStatus.FAILURE)
    monkeypatch_health_data(mp, [host], mock_health_data)

    triage._go_decision_counter_checker._counter_cache[host.uuid] = 0
    HostDecision(host.uuid, go_decision.to_dict(), counter=1).save()

    processor = triage.TriageShardProcessor(1)
    mock_process = mp.function(triage._process_with_dmc)
    mock_process.return_value = "some message"
    run_triage(processor)

    assert len(mock_process.mock_calls) == 1
    if is_rule_switched:
        oracle_decision = go_decision
    else:
        oracle_decision = python_decision if python_win else go_decision
    assert mock_process.mock_calls[0][1][1] == oracle_decision
