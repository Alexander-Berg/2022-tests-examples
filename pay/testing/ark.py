from paysys.sre.tools.monitorings.configs.paysys.base import ark

host = "paysys-test.ark-test"

children = ['ark-test']


def checks():
    return ark.get_checks(children)
