# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Icookie
from awtest.wrappers import get_validation_exception


def test_icookie():
    pb = modules_pb2.IcookieModule()

    icookie = Icookie(pb)

    e = get_validation_exception(icookie.validate)
    e.match('domains: is required')

    pb.domains.append('.yandex.ru')
    icookie.update_pb(pb)
    e = get_validation_exception(icookie.validate)
    e.match('at least one of the "keys_file", "use_default_keys" must be specified')

    pb.keys_file = '/tmp/keys'
    pb.use_default_keys = True
    icookie.update_pb(pb)
    e = get_validation_exception(icookie.validate)
    e.match('at most one of the "keys_file", "use_default_keys" must be specified')

    pb.use_default_keys = False
    icookie.update_pb(pb)
    icookie.validate(chained_modules=True)

    pb.domains.append('.yandex.tr')
    icookie.update_pb(pb)

    config = icookie.to_config()
    assert config.table == {
        'keys_file': '/tmp/keys',
        'domains': '.yandex.ru,.yandex.tr',
        'trust_parent': icookie.DEFAULT_TRUST_PARENT,
        'trust_children': icookie.DEFAULT_TRUST_CHILDREN,
        'enable_set_cookie': icookie.DEFAULT_ENABLE_SET_COOKIE,
        'enable_decrypting': icookie.DEFAULT_ENABLE_DECRYPTING,
        'decrypted_uid_header': icookie.DEFAULT_DECRYPTED_UID_HEADER,
        'error_header': icookie.DEFAULT_ERROR_HEADER,
    }
