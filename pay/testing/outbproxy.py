from paysys.sre.tools.monitorings.configs.diehard.base import outbproxy
from paysys.sre.tools.monitorings.lib.checks.rtc import rtc_monitorings
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = 'diehard.test.outbproxy'
GEOS = ['sas', 'vla', 'myt']
DEPLOY_UNIT = 'outbproxy'
STAGE = 'diehard-outbproxy-test-stage'
WORKLOAD = 'nginx'
children = ['diehard@stage={stage};deploy_unit={deploy_unit}'.format(stage=STAGE, deploy_unit=DEPLOY_UNIT)]
SERVICE_FQDN = 'outbproxy.diehard.test.yandex.net'


def get_deploy_prj(deploy_unit, stage, workload):
    return '{stage}.{deploy_unit}.{workload}'.format(stage=stage, deploy_unit=deploy_unit, workload=workload)


def checks():
    return merge(
        outbproxy.cert('cert_check', SERVICE_FQDN),
        outbproxy.get_checks(children),
        outbproxy.l3(SERVICE_FQDN, GEOS),
        rtc_monitorings(
            prj=get_deploy_prj(DEPLOY_UNIT, STAGE, WORKLOAD),
            ctype='none',
            itype='deploy',
            geos=GEOS,
            disks=['main-disk']
        ),
    )
