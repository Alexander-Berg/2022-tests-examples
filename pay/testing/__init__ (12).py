from paysys.sre.tools.monitorings.configs.dwh.testing import dwh


defaults = dwh.cfg.get_defaults()
notifications = dwh.cfg.env.get_notifications()


CONFIGS = [
    'dwh',
]
