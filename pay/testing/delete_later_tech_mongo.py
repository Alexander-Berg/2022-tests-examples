from paysys.sre.tools.monitorings.configs.apikeys.base import delete_later_tech_mongo as tech
from paysys.sre.tools.monitorings.lib.util.helpers import merge


host = "tech-mongo.delete-later.testing"
children = ['tech-test']


def checks():
    return merge(
        tech.get_checks(children, 'tech-test', 1, 'tech.testing'),
    )
