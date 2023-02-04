from paysys.sre.tools.monitorings.configs.octopus.base import octopus

host = "octopus-test.octopus-test"

children = ['octopus-test']


def checks():
    return octopus.get_checks(children, 'octopus-test', 1)
