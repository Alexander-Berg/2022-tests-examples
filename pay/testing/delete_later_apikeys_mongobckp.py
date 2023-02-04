from paysys.sre.tools.monitorings.configs.paysys.base import mongo_backup

host = "apikeys-mongo-backup.delete-later.testing"
children = ['apikeys-backup-test']


def checks():
    return mongo_backup.get_checks(children, 'apikeys-test', 9)
