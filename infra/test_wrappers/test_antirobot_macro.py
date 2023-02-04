import pytest
import semantic_version

from awacs.wrappers import l7macro
from awacs.wrappers.base import Holder, ValidationCtx, wrap, ANY_MODULE
from awacs.wrappers.main import AntirobotMacro
from awtest.wrappers import get_validation_exception
from infra.awacs.proto import modules_pb2


AM = [ANY_MODULE]


@pytest.mark.parametrize('version', (
    None, '0.0.1', '0.0.2', '0.0.3', '0.0.5',
    '0.0.6', '0.0.7', '0.0.8', '0.0.9'
))
def test_antirobot_macro_0(version):
    l7_macro_pb = modules_pb2.L7Macro()
    l7_macro_pb.compat.SetInParent()
    l7_macro = l7macro.L7Macro(l7_macro_pb)

    holder_pb = modules_pb2.Holder()

    antirobot_macro_pb = holder_pb.antirobot_macro
    antirobot_macro_pb.SetInParent()
    if version is not None:
        antirobot_macro_pb.version = version
        version = semantic_version.Version(version)
    else:
        version = semantic_version.Version('0.0.0')
    holder = Holder(holder_pb)

    if version >= AntirobotMacro.VERSION_0_0_2:
        def validate_holder():
            return holder.validate(preceding_modules=[l7_macro])
    else:
        validate_holder = holder.validate

    e = get_validation_exception(validate_holder)
    e.match('must have nested module')

    antirobot_macro_pb.nested.shared.uuid = 'test'
    antirobot_macro_pb.instances.add(host='ya.ru', port=80, weight=1)
    holder.update_pb(holder_pb)

    if version >= AntirobotMacro.VERSION_0_0_2:
        l7_macro.compat.pb.disable_sd = True
        e = get_validation_exception(validate_holder)
        e.match("antirobot_macro -> sd: "
                "can only be used if preceded by l7_macro, instance_macro or main module with enabled SD")
        l7_macro.compat.pb.disable_sd = False

    max_cut_request_bytes = AntirobotMacro.MAX_CUT_REQUEST_BYTES
    antirobot_macro_pb.cut_request_bytes = max_cut_request_bytes + 1

    holder.update_pb()
    e = get_validation_exception(validate_holder)
    e.match('antirobot_macro -> cut_request_bytes: must be less or equal to {}'.format(max_cut_request_bytes))

    antirobot_macro_pb.cut_request_bytes = max_cut_request_bytes
    holder.update_pb()
    validate_holder()

    if version >= AntirobotMacro.VERSION_0_0_2 and version < AntirobotMacro.VERSION_0_0_6:
        antirobot_macro_pb.trust_x_yandex_ja_x = True
        e = get_validation_exception(validate_holder)
        e.match('antirobot_macro -> trust_x_yandex_ja_x: not allowed in versions prior to 0.0.6')
        antirobot_macro_pb.trust_x_yandex_ja_x = False
        holder.update_pb()

    if version >= AntirobotMacro.VERSION_0_0_6:
        antirobot_macro_pb.trust_x_yandex_ja_x = True
        holder.update_pb()

    validate_holder()

    def check_config(cfg):
        if version < AntirobotMacro.VERSION_0_0_8:
            hasher_config = cfg.table.get('hasher')
        else:
            headers_config = cfg.table.get('headers')
            assert headers_config
            hasher_config = headers_config.table.get('hasher')

        if version < AntirobotMacro.VERSION_0_0_7:
            assert hasher_config.table.get('subnet_v4_mask') is None
            assert hasher_config.table.get('subnet_v6_mask') is None
        else:
            assert hasher_config.table.get('subnet_v4_mask') == AntirobotMacro.HASHER_SUBNET_V4_MASK == 32
            assert hasher_config.table.get('subnet_v6_mask') == AntirobotMacro.HASHER_SUBNET_V6_MASK == 64

        if version < AntirobotMacro.VERSION_0_0_8:
            assert hasher_config.table.get('take_ip_from') == 'X-Real-IP'
        else:
            assert hasher_config.table.get('take_ip_from') == 'X-Forwarded-For-Y'

        assert hasher_config
        h100_config = hasher_config.table.get('h100')
        assert h100_config
        cutter_config = h100_config.table.get('cutter')
        assert cutter_config

        antirobot_config = cutter_config.table.get('antirobot')

        if version < AntirobotMacro.VERSION_0_0_8:
            if version >= AntirobotMacro.VERSION_0_0_3:
                headers_config = cutter_config.table.get('headers')
                antirobot_config = headers_config.table.get('antirobot')
                assert headers_config
            if version >= AntirobotMacro.VERSION_0_0_5:
                icookie_config = headers_config.table.get('icookie')
                assert icookie_config
                antirobot_config = icookie_config.table.get('antirobot')
            if version >= AntirobotMacro.VERSION_0_0_6:
                ja3_config = icookie_config.table.get('headers')
                antirobot_config = ja3_config.table.get('antirobot')
                assert ja3_config
        else:
            icookie_config = cutter_config.table.get('icookie')
            assert icookie_config
            ja3_config = icookie_config.table.get('headers')
            assert ja3_config
            antirobot_config = ja3_config.table.get('antirobot')
            assert icookie_config

        assert antirobot_config
        assert antirobot_config.table.get('cut_request_bytes') == max_cut_request_bytes
        checker_config = antirobot_config.table.get('checker')
        assert checker_config
        report_config = checker_config.table.get('report')
        assert report_config
        stats_eater_config = report_config.table.get('stats_eater')
        assert stats_eater_config
        balancer2_config = stats_eater_config.table.get('balancer2')
        assert balancer2_config
        hashing = balancer2_config.table.get('hashing')
        assert hashing

        module_config = antirobot_config.table.get('module')
        assert module_config
        shared_config = module_config.table.get('shared')
        assert shared_config

    holder.expand_macroses()
    config = holder.to_config()

    if version < AntirobotMacro.VERSION_0_0_8:
        assert list(config.table.keys()) == ['hasher']
    else:
        assert list(config.table.keys()) == ['headers']

    check_config(config)

    holder_pb = modules_pb2.Holder()
    holder_pb.modules.add(antirobot_macro=antirobot_macro_pb)
    holder = Holder(holder_pb)
    assert holder.chain
    assert len(holder.chain.modules) == 1
    holder.expand_macroses()

    config = holder.to_config()
    if version < AntirobotMacro.VERSION_0_0_8:
        assert list(config.table.keys()) == ['hasher']
    else:
        assert list(config.table.keys()) == ['headers']
    check_config(config)


@pytest.mark.parametrize('version, trust_x_forwarded_for_y, expect_xffy, action_fn', [
    ('0.0.2', False, False, None),
    ('0.0.3', False, True, 'create_func_weak'),
    ('0.0.4', False, True, 'create_func'),
    ('0.0.4', True, True, 'create_func_weak'),
])
def test_xffy_in_upstream_antirobot(version, trust_x_forwarded_for_y, expect_xffy, action_fn):
    antirobot_macro_pb = modules_pb2.AntirobotMacro()
    antirobot_macro_pb.version = version
    antirobot_macro_pb.trust_x_forwarded_for_y = trust_x_forwarded_for_y

    ret = wrap(antirobot_macro_pb).expand(
        ctx=ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_UPSTREAM),
        preceding_modules=AM)
    headers_pb = ret[3].headers
    if expect_xffy:
        action_map = getattr(headers_pb, action_fn)
        assert list(action_map.keys()) == ['X-Forwarded-For-Y']
        assert action_map['X-Forwarded-For-Y'] == 'realip'
    else:
        assert not list(headers_pb.create_func_weak.keys())
