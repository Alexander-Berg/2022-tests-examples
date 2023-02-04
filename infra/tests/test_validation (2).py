import pytest

from infra.qyp.proto_lib import accounts_api_pb2, vmset_pb2, qdm_pb2, vmset_api_pb2
from infra.qyp.vmproxy.src import errors, security_policy
from infra.qyp.vmproxy.src.action import validation


def test_validate_ok(vm, user_login, ctx_mock):
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    validation.validate_macro(ctx_mock, vm.spec.qemu.network_id, user_login)
    ctx_mock.pod_ctl.check_use_macro_permission.assert_called_with(vm.spec.qemu.network_id, user_login)


def test_validate_unsupported_vm_type(vm, user_login, ctx_mock):
    vm.spec.type = vmset_pb2.VMSpec.OS_CONTAINER
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Unknown VM type 1'


def test_validate_meta_id(vm, ctx_mock, user_login):
    vm.meta.id = ''
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"meta.id" field not set'

    vm.meta.id = 'a' * (validation.MAX_ID_LENGTH + 1)
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'id length should be less than {}, got {}'.format(
        validation.MAX_ID_LENGTH, len(vm.meta.id)
    )


def test_validate_scheduling_hints(config, vm, ctx_mock, user_login):
    config.set_value('vmproxy.root_users', ['root'])
    h = vm.spec.scheduling.hints.add()
    h.node_id = 'sas0-0000.search.yandex.net'
    h.strong = True
    with pytest.raises(ValueError):
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)


# uncomment after https://st.yandex-team.ru/QEMUKVM-1571
# def test_validate_scheduling_node_filter(config, vm, ctx_mock, user_login):
#     config.set_value('vmproxy.root_users', ['root'])
#     vm.spec.scheduling.node_filter = 'test'
#     with pytest.raises(ValueError):
#         validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)


def test_validate_owners(vm, ctx_mock, user_login):
    # Case: too many logins
    vm.meta.auth.owners.logins.extend(['a'] * (validation.MAX_LOGINS_COUNT + 1))
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Too much literal owners, maximum {}, given {}'.format(
        validation.MAX_LOGINS_COUNT, len(vm.meta.auth.owners.logins))

    # Case: no logins and group ids
    vm.meta.auth.owners.ClearField('logins')
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Authorization fields not set'

    # Case: only group ids passed
    vm.meta.auth.owners.group_ids.extend(['1'])
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    # Case: Incorrect logins
    vm.meta.auth.owners.logins.append('golomolz,inami')
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'incorrect meta.auth.owners.logins: golomolz,inami'


def test_validate_network_id(vm, config, ctx_mock, user_login, rt_client_mock):
    config.set_value('vmproxy.network_whitelist', ['_WHITELIST_NETS_'])

    def _check_use_macro_permission(macro_name, subject_id):
        return macro_name in config.get_value('vmproxy.network_whitelist')

    ctx_mock.pod_ctl.check_use_macro_permission.side_effect = _check_use_macro_permission

    # Case: network_id field not set
    vm.spec.qemu.network_id = ''
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"spec.qemu.network_id" field not set'

    # Case: no macro owning
    vm.spec.qemu.network_id = '_SOME_OTHER_NETS_'
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    with pytest.raises(errors.AuthorizationError) as error:
        validation.validate_macro(ctx_mock, vm.spec.qemu.network_id, user_login)
    assert error.value.message == 'User {} must have access to {} macro'.format(user_login, vm.spec.qemu.network_id)

    # Case: validate with no macro check
    vm.spec.qemu.network_id = '_SOME_OTHER_NETS_'
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    # Case: macro in whitelist
    vm.spec.qemu.network_id = '_WHITELIST_NETS_'
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    validation.validate_macro(ctx_mock, vm.spec.qemu.network_id, user_login)
    ctx_mock.pod_ctl.check_use_macro_permission.assert_called_with(vm.spec.qemu.network_id, user_login)


def test_validate_node_segments(vm, config, ctx_mock, user_login):
    config.set_value('vmproxy.node_segment', ['dev', 'default'])

    # Case : node segment not set
    vm.spec.qemu.node_segment = ''
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"spec.qemu.node_segment" field not set'

    # Case : node segment not available
    vm.spec.qemu.node_segment = 'real_prod_node_segment'
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"spec.qemu.node_segment" should be one of {}, got {}'.format(
        config.get_value('vmproxy.node_segment'), vm.spec.qemu.node_segment
    )


def test_validate_vcpu(vm, config, ctx_mock, user_login):
    config.set_value('vmproxy.node_segment', [validation.DEFAULT_SEGMENT_NAME, validation.DEV_SEMGENT_NAME])

    # Case : vcpu_limit equals zero
    vm.spec.qemu.resource_requests.vcpu_limit = 0
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == '"spec.qemu.resource_requests.vcpu_limit" should be greater than zero'

    # Case : vcpu_limit is greater than vcpu_guarantee in default segment
    vm.spec.qemu.node_segment = validation.DEFAULT_SEGMENT_NAME
    vm.spec.qemu.resource_requests.vcpu_guarantee = 1
    vm.spec.qemu.resource_requests.vcpu_limit = vm.spec.qemu.resource_requests.vcpu_guarantee + 1
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Cpu guarantee ({}) should be equal cpu limit ({}) in default segment'.format(
        vm.spec.qemu.resource_requests.vcpu_guarantee, vm.spec.qemu.resource_requests.vcpu_limit,
    )

    # Case : vcpu_limit is greater than vcpu_guarantee in dev segment
    vm.spec.qemu.node_segment = validation.DEV_SEMGENT_NAME
    vm.spec.qemu.resource_requests.vcpu_guarantee = 1
    vm.spec.qemu.resource_requests.vcpu_limit = vm.spec.qemu.resource_requests.vcpu_guarantee + 1
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    # Case: vcpu_guarantee over commit in tmp account
    vm.spec.qemu.node_segment = validation.DEV_SEMGENT_NAME
    vm.spec.account_id = 'tmp'
    vm.spec.qemu.resource_requests.vcpu_guarantee = validation.MAX_CPU_GUARANTEE + 1000
    vm.spec.qemu.resource_requests.vcpu_limit = vm.spec.qemu.resource_requests.vcpu_guarantee
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == (
        'Maximum for "spec.qemu.resource_requests.vcpu_guarantee" in temporary account is {}, given {}'.format(
            validation.MAX_CPU_GUARANTEE, vm.spec.qemu.resource_requests.vcpu_guarantee
        ))

    # Case 23: validate cpu guarantee in any account except tmp in dev
    vm.spec.account_id = 'abc:service:1'
    vm.spec.qemu.node_segment = validation.DEV_SEMGENT_NAME
    vm.spec.qemu.resource_requests.vcpu_guarantee = validation.MAX_CPU_GUARANTEE + 1000
    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    # Case 24: cpu guarantee over limit
    vm.spec.qemu.resource_requests.vcpu_guarantee = 256000
    vm.spec.qemu.resource_requests.vcpu_limit = 256000
    with pytest.raises(ValueError) as e:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert e.value.message == 'Cpu limit 256000 exceeds maximum allowed value 255000'

    # Case 25: validate cpu guarantee and limit in default segment
    vm.spec.qemu.node_segment = validation.DEFAULT_SEGMENT_NAME
    vm.spec.qemu.resource_requests.vcpu_guarantee = 1
    vm.spec.qemu.resource_requests.vcpu_limit = vm.spec.qemu.resource_requests.vcpu_guarantee + 1
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Cpu guarantee (1) should be equal cpu limit (2) in default segment'


def test_validation_memory(vm, ctx_mock, user_login):
    vm.spec.qemu.resource_requests.memory_guarantee = vm.spec.qemu.resource_requests.memory_limit + 1
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Memory guarantee should be equal memory limit'


def test_validation_resource_requests(vm, ctx_mock, user_login):
    vm.spec.qemu.resource_requests.Clear()
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"spec.qemu.resource_requests.vcpu_limit" should be greater than zero'


def test_validate_enable_internet_use_nat64(vm, ctx_mock, user_login):
    vm.spec.qemu.enable_internet = True
    vm.spec.qemu.use_nat64 = True
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"enable_internet" and "use_nat64" fields are mutually exclusive'


def test_validate_ip4_address_pool_id(vm, ctx_mock, user_login):
    vm.spec.qemu.ip4_address_pool_id = '1:1'
    vm.spec.qemu.enable_internet = True
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"ip4_address_pool_id" mutually excludes fields "enable_internet" and "use_nat64"'

    vm.spec.qemu.enable_internet = False
    vm.spec.qemu.use_nat64 = True
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"ip4_address_pool_id" mutually excludes fields "enable_internet" and "use_nat64"'


@pytest.mark.parametrize('default_vmagent_version', ['0.10', '0.20', '0.27', '0.28'])
def test_validate_volumes_common(vm, ctx_mock, user_login, default_vmagent_version):
    # case: volumes not set
    del vm.spec.qemu.volumes[:]
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == '"spec.qemu.volumes" field not set'

    # case: main volume has wrong name
    main_volume = vm.spec.qemu.volumes.add()
    main_volume.name = 'this_is_unacceptable'
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert error.value.message == 'Can only set one volume with name /qemu-persistent'

    # Case: unsupported storage
    main_volume.name = '/qemu-persistent'
    main_volume.storage_class = 'place'
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert 'storage_class should be one of (hdd, ssd), got place' in error.value.message


@pytest.mark.parametrize('default_vmagent_version,expected_error', [
    ('0.27', 'Can only set one volume'),
    ('0.28', None)
])
def test_validate_volumes_multi_by_vmagent_version(vm, ctx_mock, user_login, default_vmagent_version, expected_error):
    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = 10 * 1024 ** 3
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = 3 * 1024 ** 2
    try:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
        error = None
    except ValueError as e:
        error = e.message
    assert error == expected_error


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_volumes_multi_rewrite_default_params(vm, config, ctx_mock, user_login, default_vmagent_version):
    config.set_value('vmproxy.volumes.main_volume_vm_mount_path', '/main_volume_vm_mount_path')
    config.set_value('vmproxy.volumes.main_volume_pod_mount_path', '/main_volume_pod_mount_path')
    config.set_value('vmproxy.volumes.extra_volume_vm_mount_path_prefix', '/vm_path_prefix_')
    config.set_value('vmproxy.volumes.extra_volume_pod_mount_path_prefix', '/pod_path_prefix_')
    main_volume = vm.spec.qemu.volumes[0]
    main_volume.pod_mount_path = '/wrong'
    main_volume.vm_mount_path = '/wrong'
    main_volume.req_id = 'any value'

    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = 10 * 1024 ** 3
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW
    extra_volume.pod_mount_path = '/wrong'
    extra_volume.vm_mount_path = '/wrong'
    extra_volume.req_id = 'any value'
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = 3 * 1024 ** 2

    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert main_volume.pod_mount_path == '/main_volume_pod_mount_path'
    assert main_volume.vm_mount_path == '/main_volume_vm_mount_path'
    assert main_volume.req_id == ''

    assert extra_volume.pod_mount_path == '/pod_path_prefix_{}'.format(extra_volume.name)
    assert extra_volume.vm_mount_path == '/vm_path_prefix_{}'.format(extra_volume.name)
    assert extra_volume.req_id == ''


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_volumes_multi_names(vm, ctx_mock, user_login, default_vmagent_version):
    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.capacity = 10 * 1024 ** 3
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW

    # case: not valid name
    extra_volume.name = 'gdgdgdgFHGDGDG'

    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == 'Volume with index 1 should match pattern' \
                                  ' "^[a-z][a-z0-9_]{2,32}$", got "gdgdgdgFHGDGDG"'

    # case: name from blacklist
    extra_volume.name = 'persistent'

    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == 'Volume with index:1 has name from black list: [persistent]'

    # case: extra volumes names not unique
    extra_volume2 = vm.spec.qemu.volumes.add()
    extra_volume2.capacity = 10 * 1024 ** 3
    extra_volume2.storage_class = 'hdd'
    extra_volume2.image_type = vmset_pb2.Volume.RAW
    extra_volume.name = 'test'
    extra_volume2.name = 'test'

    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == "Volume Names should be unique, got: ['/qemu-persistent', 'test', 'test']"


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_volumes_multi_size(vm, config, ctx_mock, user_login, default_vmagent_version):
    main_volume_min_capacity_gb = 10
    extra_volume_min_capacity_gb = 2.1
    config.set_value('vmproxy.volumes.main_volume_min_capacity_gb', main_volume_min_capacity_gb)
    config.set_value('vmproxy.volumes.extra_volume_min_capacity_gb', extra_volume_min_capacity_gb)

    # case: extra volume too small
    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = int(extra_volume_min_capacity_gb * 1024 ** 3) - 1
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW

    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == "Volume:test capacity too small: ({}) < ({})".format(
        extra_volume.capacity, int(extra_volume_min_capacity_gb * 1024 ** 3)
    )

    # case: main volume too small
    extra_volume.capacity = int(extra_volume_min_capacity_gb * 1024 ** 3)
    main_volume = vm.spec.qemu.volumes[0]
    main_volume.capacity = int(main_volume_min_capacity_gb * 1024 ** 3) - 1
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == "Volume:{} capacity too small: ({}) < ({})".format(
        main_volume.name, main_volume.capacity, int(main_volume_min_capacity_gb * 1024 ** 3)
    )


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_volumes_multi_resource_url(vm, config, ctx_mock, user_login, default_vmagent_version):
    config.set_value('vmproxy.volumes.valid_resource_url_prefixes', ['rbtorrent:'])

    main_volume = vm.spec.qemu.volumes[0]

    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = 10 * 1024 ** 3
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW
    # case: main volume empty resource_url
    main_volume.resource_url = ''
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == 'Volume: /qemu-persistent resource_url for main volume required!'

    # case: main volume wrong resource url
    main_volume.resource_url = 'http://wrong'
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == 'Volume:/qemu-persistent resource_url should start with [rbtorrent:]'

    # case: extra volume wrong image_type for empty resource url
    main_volume.resource_url = 'rbtorrent:valid'
    extra_volume.image_type = vmset_pb2.Volume.DELTA
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == 'Volume:test is empty and should have image type RAW'


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_volumes_multi_max_count(vm, config, ctx_mock, user_login, default_vmagent_version):
    config.set_value('vmproxy.volumes.extra_volumes_max_count', 1)

    # case: extra volume too small
    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = 10 * 1024 ** 3
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = 3 * 1024 ** 2

    validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    # case: extra volume too small
    extra_volume2 = vm.spec.qemu.volumes.add()
    extra_volume2.name = 'test2'
    extra_volume2.capacity = 10 * 1024 ** 3
    extra_volume2.storage_class = 'hdd'
    extra_volume2.image_type = vmset_pb2.Volume.RAW

    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)

    assert error.value.message == 'Extra volumes count should be less then: 1, current: 2'


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_empty_io_guarantees(vm, ctx_mock, user_login, default_vmagent_version):
    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = 10 * 1024 ** 3
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert "IO guarantee not set for volumes ['test']" in error.value.message


@pytest.mark.parametrize('default_vmagent_version', ['0.28'])
def test_validate_limited_volumes_size_for_default(vm, ctx_mock, user_login, default_vmagent_version, config):
    ctx_mock.pod_ctl.yp_cluster = 'IVA'
    config.set_value('vmproxy.clusters_with_limited_volumes_size', ['IVA'])

    main_volume = vm.spec.qemu.volumes[0]
    main_volume.capacity = validation.DEFAULT_SEGMENT_MAX_VOLUMES_SIZE + 1024 ** 3
    with pytest.raises(ValueError) as error:
        validation.validate_allocate_request(ctx_mock, vm.meta, vm.spec, user_login)
    assert "Maximum total volumes size allowed in default segment is 1Tb" in error.value.message


def test_validate_qdm_backup_spec(ctx_mock):
    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        filemap=[qdm_pb2.QDMBackupFileSpec()],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(
                qemu=vmset_pb2.QemuVMSpec(
                    volumes=[vmset_pb2.Volume(
                        storage_class='hdd'
                    )]
                )
            ),
            meta=vmset_pb2.VMMeta(
                id='pod-id',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['volozh']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    req = vmset_api_pb2.RestoreBackupRequest()
    req.vm_id = 'pod-id'
    req.resource_url = 'qdm:testing'
    login = 'volozh'

    # Case 1: user in spec
    validation.validate_qdm_backup_spec(ctx_mock, ctx_mock.qdm_client.get_revision_info(req.resource_url),
                                        login)

    # Case 2: user not in spec
    login = 'definitely-not-volozh'

    with pytest.raises(errors.AuthorizationError) as error:
        validation.validate_qdm_backup_spec(ctx_mock, ctx_mock.qdm_client.get_revision_info(req.resource_url),
                                            login)
    assert 'Attempt to restore VM backup by person-non-owner' == error.value.message

    # Case 3: no spec
    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        filemap=[qdm_pb2.QDMBackupFileSpec()],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(
                qemu=vmset_pb2.QemuVMSpec(
                    volumes=[vmset_pb2.Volume(
                        storage_class='hdd'
                    )]
                )
            )
        )
    )
    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec
    validation.validate_qdm_backup_spec(ctx_mock, ctx_mock.qdm_client.get_revision_info(req.resource_url),
                                        login)


def test_validate_personal_resource_fit(ctx_mock, vm):
    spec = vm.spec
    spec.account_id = 'non-personal-account-id'
    validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)

    acc_manager_resp = accounts_api_pb2.ListUserAccountsResponse()
    ctx_mock.account_manager_client.request_account_data.return_value = acc_manager_resp
    pers_summary = acc_manager_resp.personal_summary.add()
    pers_summary.account_id = 'abc:service:4172'
    usage = pers_summary.total_usage.per_segment[spec.qemu.node_segment]
    acc_cluster = acc_manager_resp.accounts_by_cluster.add()
    acc = acc_cluster.accounts.add()
    acc.id = 'abc:service:4172'
    limits = acc.personal.limits.per_segment[spec.qemu.node_segment]
    spec.account_id = 'abc:service:4172'
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'CPU' in e.value.message

    limits.cpu = 5000
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'Memory' in e.value.message

    limits.mem = 30 * 1024 ** 3
    limits.disk_per_storage['ssd'] = 10 * 1024 ** 3
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'Disk space available for ssd' in e.value.message

    validation.validate_personal_resource_fit(ctx_mock, spec, 'user', True)
    root_quota, workdir_quota = 1073741824, 5368709120
    limits.disk_per_storage['ssd'] += (root_quota + workdir_quota)
    spec.qemu.gpu_request.capacity = 1
    spec.qemu.gpu_request.model = 'big-gpu'
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'GPU available' in e.value.message

    limits.gpu_per_model['big-gpu'] = 1
    usage.cpu = 1000
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'CPU available' in e.value.message

    limits.cpu += 1000
    spec.qemu.enable_internet = True
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'No internet' in e.value.message

    limits.internet_address += 1
    limits.io_guarantees_per_storage['ssd'] = spec.qemu.io_guarantees_per_storage['ssd'] - 1
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)
    assert 'Disk bandwidth available' in e.value.message

    limits.io_guarantees_per_storage['ssd'] += 1
    validation.validate_personal_resource_fit(ctx_mock, spec, 'user', False)


def test_validate_update_personal_resource_fit(ctx_mock, vm):
    spec = vm.spec
    spec.account_id = 'abc:service:4172'
    old_spec = vmset_pb2.VMSpec()
    old_spec.CopyFrom(spec)

    old_spec.qemu.node_segment = 'dev'
    with pytest.raises(errors.ValidationError):
        validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')

    acc_manager_resp = accounts_api_pb2.ListUserAccountsResponse()
    ctx_mock.account_manager_client.request_account_data.return_value = acc_manager_resp
    acc_cluster = acc_manager_resp.accounts_by_cluster.add()
    acc = acc_cluster.accounts.add()
    acc.id = 'abc:service:4172'
    limits = acc.personal.limits.per_segment[spec.qemu.node_segment]
    pers_summary = acc_manager_resp.personal_summary.add()
    pers_summary.account_id = 'abc:service:4172'
    usage = pers_summary.total_usage.per_segment[spec.qemu.node_segment]

    limits.disk_per_storage['ssd'] = 10 * 1024 ** 3
    limits.cpu = 6000
    limits.mem = 30 * 1024 ** 3
    usage.cpu = 1000

    old_spec.qemu.node_segment = 'default'
    validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
    spec.qemu.resource_requests.vcpu_guarantee += 6000
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
    assert 'CPU available' in e.value.message

    old_spec.qemu.resource_requests.vcpu_guarantee += 1000
    spec.qemu.resource_requests.memory_guarantee += 31 * 1024 ** 3
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
    assert 'Memory available' in e.value.message

    old_spec.qemu.resource_requests.memory_guarantee += 1024 ** 3
    v = spec.qemu.volumes.add()
    v.storage_class = 'ssd'
    v.capacity = 11 * 1024 ** 3
    v.name = 'extra_volume'
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
    assert 'Disk space available' in e.value.message

    limits.disk_per_storage['ssd'] += 1024 ** 3
    spec.qemu.gpu_request.capacity = 1
    spec.qemu.gpu_request.model = 'new-gpu'
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
    assert 'GPU available' in e.value.message

    old_spec.qemu.gpu_request.capacity = 1
    old_spec.qemu.gpu_request.model = 'new-gpu'
    spec.qemu.enable_internet = True
    with pytest.raises(errors.ValidationError) as e:
        validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
    assert 'No internet' in e.value.message

    old_spec.qemu.enable_internet = True
    validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')

    old_spec.qemu.enable_internet = False
    limits.internet_address += 1
    validation.validate_update_personal_resource_fit(ctx_mock, spec, old_spec, 'user')
