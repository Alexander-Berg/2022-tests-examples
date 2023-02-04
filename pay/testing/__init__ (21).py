from paysys.sre.tools.monitorings.configs.refs.testing import refs

defaults = refs.cfg.get_defaults()
notifications = refs.cfg.env.get_notifications()


CONFIGS = [
    'refs'
]
