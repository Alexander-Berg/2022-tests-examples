from paysys.sre.tools.monitorings.configs.paysys.base import mongo_backup

host = "trust.test.mongo_backup"

children = ['trust-mongobckp-test']


def checks():
    return mongo_backup.get_checks(children, 'trust-test', 3)
