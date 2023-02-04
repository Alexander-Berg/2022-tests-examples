import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers import l7macro
from awacs.wrappers.base import Holder
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


@pytest.mark.parametrize('version', (None, '0.0.1', '0.0.2'))
def test_click_macro(version):
    l7_macro_pb = modules_pb2.L7Macro()
    l7_macro_pb.compat.SetInParent()
    l7_macro = l7macro.L7Macro(l7_macro_pb)

    holder_pb = modules_pb2.Holder()
    click_macro_pb = holder_pb.click_macro
    click_macro_pb.SetInParent()
    if version is not None:
        click_macro_pb.version = version
    holder = Holder(holder_pb)

    if version == '0.0.2':
        def validate_holder():
            return holder.validate(preceding_modules=[l7_macro])
    else:
        validate_holder = holder.validate

    if version == '0.0.2':
        l7_macro.compat.pb.disable_sd = True
        e = get_validation_exception(validate_holder)
        e.match("click_macro -> sd: "
                "can only be used if preceded by l7_macro, instance_macro or main module with enabled SD")
        l7_macro.compat.pb.disable_sd = False

    validate_holder()

    click_macro_pb.generated_proxy_backends.SetInParent()
    holder.update_pb(holder_pb)
    with pytest.raises(ValidationError, match='click_macro -> proxy_options: is required'):
        validate_holder()
