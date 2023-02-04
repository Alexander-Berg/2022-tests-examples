from paysys.sre.tools.monitorings.configs.trust.base import trust
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.checks.services import ps_ping, ps_closedc
from paysys.sre.tools.monitorings.lib.checks.active.http import http_bundle

host = "trust.test.hw"

children = ["trust-test"]

split_by_dc = True


def checks():
    return merge(
        trust.get_checks(children, "trust-test", [27017, 27018, 27219]),
        ps_ping,
        ps_closedc,
        http_bundle("jaeger-agent", port=14271, path="/"),
        http_bundle("yb-trust-worker", port=18040),
        http_bundle("yb-trust-worker-ng", port=18041),
        http_bundle("yb-trust-worker-xg", port=18042),
    )
