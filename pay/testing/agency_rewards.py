from paysys.sre.tools.monitorings.lib.util.helpers import merge, empty_children
from paysys.sre.tools.monitorings.lib.notifications import Notifications, \
    FIN_TOOLS_ESCALATION_USERS, FIN_TOOLS_TELEGRAM_GROUP
from paysys.sre.tools.monitorings.configs.agency_rewards.base import agency_rewards

namespace = 'agency-rewards'
host = '{}.test'.format(namespace)
children = empty_children
greed_children = ['ps-front-test', 'ps-back-test']
defaults = {'namespace': namespace}

notifications = Notifications() \
    .set_iron_woman(delay=1200, logins=FIN_TOOLS_ESCALATION_USERS) \
    .set_telegram(FIN_TOOLS_TELEGRAM_GROUP)


def checks():
    return merge(
        agency_rewards.yb_ar_bunker_token(greed_children),
        agency_rewards.unreachable(greed_children),
    )
