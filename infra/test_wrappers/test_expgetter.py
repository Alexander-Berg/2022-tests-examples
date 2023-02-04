# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import Holder, ValidationCtx
from awacs.wrappers.main import ExpGetter, ExpGetterMacro, InstanceMacro
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_exp_getter():
    pb = modules_pb2.ExpGetterModule()

    exp_getter = ExpGetter(pb)

    with pytest.raises(ValidationError) as e:
        exp_getter.validate(chained_modules=True)
    e.match('uaas.*is required')

    http = pb.uaas.http
    http.maxlen = 100
    http.maxreq = 100
    http.nested.admin.SetInParent()
    exp_getter.update_pb(pb)
    exp_getter.validate(chained_modules=True)


def test_exp_getter_macro_1():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.exp_getter_macro

    pb.service_name = 'xxx'
    pb.testing_mode.SetInParent()
    pb.nested.shared.uuid = 'xxx'
    holder = Holder(holder_pb)

    holder.module.validate(chained_modules=True)
    holder.expand_macroses()

    config = holder.to_config()
    config = config.table['regexp']
    assert config.table['exp_testing'].table['headers'].table['exp_getter'].table['shared'].table['uuid'] == 'xxx'
    assert config.table['default'].table['headers'].table['exp_getter'].table['shared'].table['uuid'] == 'xxx'


def test_exp_getter_macro_2():
    holder_pb = modules_pb2.Holder()
    exp_getter_macro_pb = holder_pb.modules.add().exp_getter_macro
    exp_getter_macro_pb.service_name = 'xxx'
    exp_getter_macro_pb.testing_mode.SetInParent()

    shared_pb = holder_pb.modules.add().shared
    shared_pb.uuid = 'xxx'

    holder = Holder(holder_pb)
    holder.expand_macroses()
    config = holder.to_config()
    regexp_config = config.table['regexp']
    assert (regexp_config.table['exp_testing'].table['headers']
            .table['exp_getter'].table['shared'].table['uuid'] == 'xxx')
    assert regexp_config.table['default'].table['headers'].table['exp_getter'].table['shared'].table['uuid'] == 'xxx'


def test_exp_getter_macro_3():
    holder_pb = modules_pb2.Holder()
    exp_getter_macro_pb = holder_pb.exp_getter_macro
    exp_getter_macro_pb.service_name = 'xxx'
    exp_getter_macro_pb.testing_mode.SetInParent()
    holder = Holder(holder_pb)

    assert holder.module.get_would_be_included_full_backend_ids(current_namespace_id='test_namespace') == {
        ('uaas.search.yandex.net', 'usersplit_sas'),
        ('uaas.search.yandex.net', 'usersplit_man'),
        ('uaas.search.yandex.net', 'usersplit_vla')
    }

    holder_pb = modules_pb2.Holder()
    exp_getter_macro_pb = holder_pb.exp_getter_macro
    exp_getter_macro_pb.service_name = 'xxx'
    exp_getter_macro_pb.testing_mode.SetInParent()
    holder = Holder(holder_pb)

    exp_getter_macro_pb._version = 10
    e = get_validation_exception(holder.validate)
    e.match('exp_getter_macro -> _version: must be less or equal to {}'.format(ExpGetterMacro.MAX_VERSION))

    exp_getter_macro_pb._version = -1
    e = get_validation_exception(holder.validate)
    e.match('exp_getter_macro -> _version: must be greater or equal to {}'.format(ExpGetterMacro.MIN_VERSION))

    exp_getter_macro_pb._version = 1
    holder.validate(chained_modules=True)
    assert holder.module.get_would_be_included_full_backend_ids(current_namespace_id='test_namespace') == {
        ('uaas.search.yandex.net', 'production_balancer_uaas_sas'),
        ('uaas.search.yandex.net', 'production_balancer_uaas_man'),
        ('uaas.search.yandex.net', 'production_balancer_uaas_vla')
    }


def test_exp_getter_macro_4():
    instance_macro_pb = modules_pb2.InstanceMacro()
    instance_macro = InstanceMacro(instance_macro_pb)

    holder_pb = modules_pb2.Holder()
    exp_getter_macro_pb = holder_pb.exp_getter_macro
    exp_getter_macro_pb.service_name = 'xxx'
    exp_getter_macro_pb.testing_mode.SetInParent()
    exp_getter_macro_pb._version = 3
    exp_getter_macro_pb.nested.errordocument.status = 200
    holder = Holder(holder_pb)

    holder.validate(preceding_modules=[instance_macro], ctx=ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_BALANCER))

    e = get_validation_exception(holder.validate, preceding_modules=[instance_macro])
    e.match('_version: can only be used if preceded by l7_macro, instance_macro or main module with enabled SD')

    instance_macro_pb.sd.SetInParent()
    instance_macro_pb.unistat.SetInParent()
    instance_macro.update_pb(instance_macro_pb)
    holder.validate(preceding_modules=[instance_macro])

    assert holder.module.get_would_be_included_full_backend_ids(current_namespace_id='test_namespace') == {
        ('common-uaas', 'yp_production_uaas_man'),
        ('common-uaas', 'yp_production_uaas_sas'),
        ('common-uaas', 'yp_production_uaas_vla'),
        ('common-uaas', 'yp_production_uaas_myt'),
        ('common-uaas', 'yp_production_uaas_iva'),
    }
