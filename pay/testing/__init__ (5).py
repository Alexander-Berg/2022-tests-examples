from paysys.sre.tools.monitorings.configs.bcl.testing import bcl

defaults = bcl.cfg.get_defaults()
notifications = bcl.cfg.env.get_notifications()


CONFIGS = [
    'bcl'
]
