from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = "paysys-test.backup-test"

# getting moved to balance_app/testing/backup
children = ['backup-test']


def checks():
    return merge()
