from paysys.sre.tools.monitorings.configs.diehard.base import L7
from paysys.sre.tools.monitorings.lib.checks.rtc import rtc_monitorings
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = 'diehard.test.L7'
GEOS = ['sas', 'iva', 'myt']
DEPLOY_UNIT = 'diehard-L7'
STAGE = 'diehard-L7-test-stage'
WORKLOAD = 'nginx'
children = ['diehard@stage={stage};deploy_unit={deploy_unit}'.format(stage=STAGE, deploy_unit=DEPLOY_UNIT)]
SERVICE_FQDN = 'pci-tf.fin.yandex.net'


def get_deploy_prj(deploy_unit, stage, workload):
    return '{stage}.{deploy_unit}.{workload}'.format(stage=stage, deploy_unit=deploy_unit, workload=workload)


def checks():
    return merge(
        L7.cert('cert_check', SERVICE_FQDN),
        L7.get_checks(children),
        L7.l3(SERVICE_FQDN, GEOS),
        L7.nginx_monitorings(
            prj=get_deploy_prj(DEPLOY_UNIT, STAGE, WORKLOAD),
            ctype='none',
            itype='deploy',
            geos=GEOS,
        ),
        rtc_monitorings(
            prj=get_deploy_prj(DEPLOY_UNIT, STAGE, WORKLOAD),
            ctype='none',
            itype='deploy',
            geos=GEOS,
            disks=['main-disk']
        )
    )
