from infra.walle.server.tests.lib.util import set_project_owners
from walle.expert.types import CheckType, CheckStatus
from walle.host_health import HealthCheck
from walle.models import timestamp


def juggler_stored_check(host_name, check_type=None, check_status=None, status_mtime=None, ts=None, metadata=None):
    if check_type is None:
        check_type = CheckType.SSH

    if check_status is None:
        check_status = CheckStatus.PASSED

    check_key = HealthCheck.mk_check_key(host_name, check_type)
    ts = ts if ts is not None else timestamp()

    check_mock = {
        "id": check_key,
        "fqdn": host_name,
        "type": check_type,
        "status": check_status,
        "status_mtime": status_mtime or ts,
        "timestamp": ts,
        "metadata": metadata or None,
    }

    return check_mock


def delete_default_project(walle_test):
    default_project = walle_test.default_project
    set_project_owners(default_project, [])
    default_project.delete()
