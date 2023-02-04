from paysys.sre.tools.monitorings.configs.oplata.testing import oplata


notifications = {
    'default': oplata.notifications.noop,
    'by_host': {
        oplata.host: oplata.notifications.noop,
    }
}


CONFIGS = [
    "oplata"
]
