# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Rewrite, RewriteAction
from awacs.wrappers.errors import ValidationError


def test_rewrite():
    pb = modules_pb2.RewriteModule()

    rewrite = Rewrite(pb)

    with pytest.raises(ValidationError) as e:
        rewrite.validate(chained_modules=True)
    e.match('actions.*is required')

    action_1 = pb.actions.add()
    rewrite.update_pb(pb)
    assert rewrite.actions == [RewriteAction(action_1)]

    with pytest.raises(ValidationError) as e:
        rewrite.validate(chained_modules=True)
    e.match('rewrite: is required')

    action_1.rewrite = 'smth'
    rewrite.update_pb(pb)
    rewrite.validate(chained_modules=True)

    action_1.regexp = '^('
    rewrite.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        rewrite.validate(chained_modules=True)
    e.match(r'is not a valid regexp')
    assert list(e.value.path) == ['actions[0]', 'regexp']
