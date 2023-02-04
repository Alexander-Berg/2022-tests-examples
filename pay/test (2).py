from payplatform.spirit.graphs_alerts_helpers import alerts


def test_trust_cashregisters_alerts():
    list(alerts.get_trust_cashregisters_alerts())
