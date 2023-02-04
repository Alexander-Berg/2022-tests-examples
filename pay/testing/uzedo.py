from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.checks.mds import mds
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

app = "paysys.uzedo"
children = "paysys.uzedo.testing.uzedo@type=ext"
env = "testing"

host = "uzedo.{}".format(env)


def checks():
    return merge(
        mds("testing", "uzedo-test"),
        postgres("61523d26-35a8-4dce-b50b-b538c22e60fc", "uzedo_unstable"),
    )
