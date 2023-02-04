import mock
import pytest

from infra.qyp.account_manager.src.web import account_service
from infra.qyp.proto_lib import accounts_api_pb2, vmset_pb2
from infra.swatlib.rpc import exceptions


def test_list_user_accounts(ctx_mock, call):
    # Case 1: no login
    req = accounts_api_pb2.ListUserAccountsRequest()
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        with pytest.raises(exceptions.BadRequestError):
            call(account_service.list_user_accounts, req)

    # Case 2: invalid login
    req.login = 'root'
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        req.clusters.append('TEST_SAS')
        with pytest.raises(exceptions.BadRequestError):
            call(account_service.list_user_accounts, req)

    # Case 3: OK
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        acc1 = vmset_pb2.Account()
        acc1.id = 'acc1'
        acc1.limits.per_segment['dev'].cpu = 3000
        acc1.limits.per_segment['dev'].mem = 21474836480
        acc1.limits.per_segment['dev'].disk_per_storage['hdd'] = 21474836480
        acc1.limits.per_segment['dev'].internet_address = 1
        acc1.limits.per_segment['dev'].io_guarantees_per_storage['hdd'] = 30 * 1024 ** 2
        acc1.members_count = 1
        acc2 = vmset_pb2.Account()
        acc2.id = 'acc2'
        acc2.limits.per_segment['dev'].cpu = 4000
        acc2.limits.per_segment['dev'].disk_per_storage['hdd'] = 21474836480
        acc2.limits.per_segment['dev'].disk_per_storage['ssd'] = 21474836480
        acc2.limits.per_segment['dev'].internet_address = 1
        acc2.limits.per_segment['dev'].io_guarantees_per_storage['hdd'] = 30 * 1024 ** 2
        acc2.members_count = 2
        mock_res = [acc1, acc2]

        with mock.patch('infra.qyp.account_manager.src.action.list_user_accounts.run', return_value=mock_res):
            req.login = 'test_user'
            pod_ctl = mock.Mock()
            ctx_mock.pod_ctl_map['TEST_SAS'] = pod_ctl
            ctx_mock.personal_quota = {'abc:service:4172': {}}
            r = call(account_service.list_user_accounts, req)
            cpus = [a.limits.per_segment['dev'].cpu // a.members_count for a in mock_res]
            mems = [a.limits.per_segment['dev'].mem // a.members_count for a in mock_res]
            disk_hdd = [a.limits.per_segment['dev'].disk_per_storage['hdd'] // a.members_count for a in mock_res]
            disk_ssd = [a.limits.per_segment['dev'].disk_per_storage['ssd'] // a.members_count for a in mock_res]
            assert r.personal_summary[0].total_usage.per_segment['dev'].cpu == sum(cpus)
            assert r.personal_summary[0].total_usage.per_segment['dev'].mem == sum(mems)
            for k, v in r.personal_summary[0].total_usage.per_segment['dev'].disk_per_storage.items():
                if k == 'hdd':
                    assert v == sum(disk_hdd)
                if k == 'ssd':
                    assert v == sum(disk_ssd)
