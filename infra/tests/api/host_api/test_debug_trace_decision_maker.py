"""Tests for debug trace decision maker API."""
import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, mock_status_reasons, monkeypatch_function
from walle.expert import decisionmakers, dmc
from walle.expert.decision import Decision
from walle.expert.decisionmakers import ModernDecisionMaker


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.parametrize(
    ["data", "expected_result"],
    [
        (
            {"decision_args": {"include": True}},
            {
                'decision': {
                    'action': 'healthy',
                    'reason': 'Host is healthy.',
                    'restrictions': [],
                    'counter': 0,
                    'rule_name': '',
                }
            },
        ),
        (
            {"alternate_decision_args": {"include": True}},
            {
                'alternate_decision': {
                    'action': 'healthy',
                    'reason': 'Host is available.',
                    'restrictions': [],
                    'counter': 0,
                    'rule_name': '',
                }
            },
        ),
    ],
)
def test_decision_maker_handler(test, monkeypatch, data, expected_result):
    host = test.mock_host()

    mocked_decision_maker = ModernDecisionMaker.for_modern_automation_plot(test.default_project)(test.default_project)

    expected_decisions = [
        Decision.healthy("Mock healthy reason"),
        Decision.failure("Mock fail reason"),
        Decision.healthy("Mock healthy reason"),
    ]

    reasons = mock_status_reasons()

    monkeypatch_function(monkeypatch, dmc.get_host_reasons, module=dmc, return_value=reasons)
    monkeypatch_function(
        monkeypatch,
        mocked_decision_maker.make_decision_trace,
        module=mocked_decision_maker,
        return_value=expected_decisions,
    )
    monkeypatch_function(monkeypatch, decisionmakers.get_decision_maker, return_value=mocked_decision_maker)

    result = test.api_client.post('/v1/hosts/{}/decision-maker'.format(host.inv), data=data)

    assert result.status_code == http.client.OK
    assert expected_result == result.json

    monkeypatch_function(monkeypatch, dmc.get_host_reasons, module=dmc, return_value=reasons)


def test_trace_decision(test, monkeypatch, monkeypatch_timestamp):
    host = test.mock_host()

    mocked_decision_maker = ModernDecisionMaker.for_modern_automation_plot(test.default_project)(test.default_project)

    expected_decisions = [
        Decision.healthy("Mock healthy reason"),
        Decision.failure("Mock fail reason"),
        Decision.healthy("Mock healthy reason"),
    ]

    monkeypatch_function(monkeypatch, dmc.get_host_reasons, module=dmc, return_value=mock_status_reasons())
    monkeypatch_function(
        monkeypatch,
        mocked_decision_maker.make_decision_trace,
        module=mocked_decision_maker,
        return_value=expected_decisions,
    )
    monkeypatch_function(monkeypatch, decisionmakers.get_decision_maker, return_value=mocked_decision_maker)

    result = test.api_client.post(
        '/v1/hosts/{}/decision-maker'.format(host.inv), data={"trace_args": {"include": True}}
    )
    expected_result = dict(
        decision_trace={
            'reasons': mock_status_reasons(),
            'decisions': [
                decision.to_dict()
                for decision in [
                    Decision.healthy("Mock healthy reason"),
                    Decision.failure("Mock fail reason"),
                    Decision.healthy("Mock healthy reason"),
                ]
            ],
        }
    )

    assert result.status_code == http.client.OK
    assert expected_result == result.json
