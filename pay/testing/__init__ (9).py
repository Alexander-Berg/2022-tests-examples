from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.lib.notifications import Notifications, BALANCE_ESCALATION_USERS

calendar_id = 38178

notifications_object_nonprod = Notifications()
notifications_object_nonprod.set_calendar(calendar_id)
notifications_object_nonprod.set_iron_woman(logins=BALANCE_ESCALATION_USERS, delay=420)
notifications_object_nonprod.set_startrek('PAYSYSDBA', status='CRIT', components='monitoring', priority='normal')
notifications_object_nonprod.set_telegram('yndx-market-duty-monitoring')

defaults = merge(
    ttl(620, 300),
    {'namespace': 'oebs.crm.testing'}
)

notifications = {
    # Telegram only
    'default': notifications_object_nonprod.telegram_critonly,
    'by_service': {
        # tracker + telegram
        'asm-status-ng': notifications_object_nonprod.startrek_and_telegram_critonly,
        'disk': notifications_object_nonprod.startrek_and_telegram_critonly,
        'ecc': notifications_object_nonprod.startrek_and_telegram_critonly,
        'gpu': notifications_object_nonprod.startrek_and_telegram_critonly,
        'mem': notifications_object_nonprod.startrek_and_telegram_critonly,

        # iron_woman daytime + TG
        'UNREACHABLE': notifications_object_nonprod.iron_woman_daytime_and_telegram_critonly,
        'uptime': notifications_object_nonprod.iron_woman_daytime_and_telegram_critonly,
        'grid-up': notifications_object_nonprod.iron_woman_daytime_and_telegram_critonly,
        'database-up': notifications_object_nonprod.iron_woman_daytime_and_telegram_critonly,
    }
}

CONFIGS = [
    "database",
]
