# coding: utf-8
import pytest
import six

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import RedirectsModule
from awacs.wrappers.errors import ValidationError


def test_redirects():
    pb = modules_pb2.RedirectsModule()

    redirects = RedirectsModule(pb)

    with pytest.raises(ValidationError) as e:
        redirects.validate(chained_modules=True)
    e.match('actions.*is required')

    action_1 = pb.actions.add()
    redirects.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        redirects.validate(chained_modules=True)
    e.match('src: is required')

    action_1.src = '//mir.trains.yandex.ru/*'
    redirects.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        redirects.validate(chained_modules=True)
    assert 'actions[0]: at least one of the "forward", "redirect" must be specified' in six.text_type(e)

    def check_field_is_required(field):
        with pytest.raises(ValidationError) as e:
            redirects.validate(chained_modules=True)
        assert '{}: is required'.format(field) in six.text_type(e)

    for field in ['forward', 'redirect']:
        getattr(action_1, field).SetInParent()
        redirects.update_pb()

        check_field_is_required('dst')
        getattr(action_1, field).dst = 'http://yastatic.net/s3/travel/other-projects/mir/robots.txt'

        if field == 'redirect':
            check_field_is_required('code')
            getattr(action_1, field).code = 301
        else:
            check_field_is_required('nested')
            errordocument = getattr(action_1, field).nested.errordocument
            errordocument.SetInParent()
            errordocument.status = 200

        redirects.update_pb()
        redirects.validate(chained_modules=True)

        dst_rewrite = getattr(action_1, field).dst_rewrites.add()
        redirects.update_pb()
        check_field_is_required('regexp')

        dst_rewrite.regexp = '[.]xml$'
        redirects.update_pb()
        redirects.validate(chained_modules=True)

        dst_rewrite.url.SetInParent()
        redirects.update_pb()
        redirects.validate(chained_modules=True)
