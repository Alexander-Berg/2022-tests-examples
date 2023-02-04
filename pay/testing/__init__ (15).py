from paysys.sre.tools.monitorings.configs.mdh.testing import mdh

defaults = mdh.cfg.get_defaults()
notifications = mdh.cfg.env.get_notifications()


CONFIGS = [
    'mdh',
]
