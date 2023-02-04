from paysys.sre.tools.monitorings.configs.trust.base import timeline

host = "trust.test.timeline"

children = ["paysys-timeline@stage=timeline-test-stage"]


def checks():
    return timeline.get_checks(
        children,
        timeline.l7_balancer(
            "timeline-test.paysys.yandex-team.ru",
            ["sas", "man"]
        )
    )
