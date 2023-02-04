from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.lib.notifications import Notifications, BALANCE_ESCALATION_USERS

calendar_id = 38178

defaults = merge(
    ttl(620, 300),
    {'namespace': 'paysysdb.reports.testing'},
)

notifications = {
    'default': Notifications().set_calendar(calendar_id).set_iron_woman(logins=BALANCE_ESCALATION_USERS, delay=420).iron_woman_daytime_and_sms
}

CONFIGS = [
    "apps",
]
