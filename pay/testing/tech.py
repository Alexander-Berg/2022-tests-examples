from paysys.sre.tools.monitorings.configs.apikeys.base import tech
from paysys.sre.tools.monitorings.lib.util.helpers import merge


host = "tech.testing"
children = ['tech-test']


def checks():
    return merge(
        tech.get_checks(children),
    )
