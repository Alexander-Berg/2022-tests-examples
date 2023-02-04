# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import AabCookieVerify
from awtest.wrappers import get_validation_exception


def test_aab_cookie_verify():
    pb = modules_pb2.AabCookieVerifyModule()
    aab_cookie_verify = AabCookieVerify(pb)

    e = get_validation_exception(aab_cookie_verify.validate, chained_modules=True)
    e.match('antiadblock.*is required')
    pb.antiadblock.SetInParent()
    aab_cookie_verify.update_pb(pb)

    e = get_validation_exception(aab_cookie_verify.validate)
    e.match('aes_key_path.*is required')
    pb.aes_key_path = './private.key'
    aab_cookie_verify.update_pb(pb)

    with mock.patch.object(aab_cookie_verify.antiadblock, 'validate') as validate_antiadblock:
        aab_cookie_verify.validate(chained_modules=True)
    validate_antiadblock.assert_called_once()
