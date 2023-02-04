from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.configs.paysys.base import salt

host = "paysys-test.salt-test"

children = ['salt-test']


def checks():
    return merge(
        salt.get_checks(children)
    )
