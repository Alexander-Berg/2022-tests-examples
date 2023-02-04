from paysys.sre.tools.monitorings.configs.ift.testing import ift


defaults = ift.cfg.get_defaults()
notifications = ift.cfg.env.get_notifications()


CONFIGS = [
    'ift',
]
