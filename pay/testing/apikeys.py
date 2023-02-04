from paysys.sre.tools.monitorings.configs.apikeys.base import apikeys


host = 'apikeys.testing'
children = ['apikeys-test']


def checks():
    return apikeys.get_checks(children, 'five_min.apikeys.test')
