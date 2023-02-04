from paysys.sre.tools.monitorings.lib.checks.base import pkgver
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.configs.paysys.base import mongo_backup

host = "tech-mongo-backup.delete-later.testing"
children = ["tech-test-backup"]


def checks():
    return merge(mongo_backup.get_checks(children, "tech-test", 1), pkgver)
