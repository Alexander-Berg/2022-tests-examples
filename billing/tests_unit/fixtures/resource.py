# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import wrapt
import importlib
import contextlib

from yb_snout_api.utils import deco

ENDPOINT = 'test_snout'
ROUTE_URL = '/test_url'
URL = '/v1' + ROUTE_URL


@contextlib.contextmanager
def build_custom_resource_cxt(
        get_params_description={},
        post_params_description={},
        ui_permissions=[],
        use_client_mixin=False,
        admins_only=False,
        return_func=None,
):
    from yb_snout_api.servant import flask_app, resource_groups
    from yb_snout_api.core.resource import Resource, ClientAccessMixin

    v1_api = resource_groups['v1']

    class SnoutTestResource(Resource):

        @deco.add_doc(v1_api, **get_params_description)
        def get(self, **kwargs):
            return return_func() if return_func else kwargs

        @deco.add_doc(v1_api, **post_params_description)
        def post(self, **kwargs):
            return return_func() or kwargs

    SnoutTestResource.ui_permissions = ui_permissions

    if use_client_mixin:
        class SnoutTestResource(ClientAccessMixin, SnoutTestResource):
            pass

        SnoutTestResource.admins_only = admins_only

    v1_api.add_resource(SnoutTestResource, ROUTE_URL, endpoint=ENDPOINT)

    try:
        yield SnoutTestResource

    finally:
        del flask_app.view_functions['.'.join([v1_api.app.name, ENDPOINT])]


def mock_client_resource(resource_path, already_client=False):
    """Add ClientMixin to resource
    """

    @wrapt.decorator
    def wrapper(func, self, args, kwargs):
        from yb_snout_api.servant import flask_app, resource_groups
        from yb_snout_api.core.resource import ClientAccessMixin

        resource_path_splitted = resource_path.split('.')
        module_path, resource_name = '.'.join(resource_path_splitted[:-1]), resource_path_splitted[-1]
        module = importlib.import_module(module_path)
        resource = getattr(module, resource_name)

        old_url = self.BASE_API
        api = resource_groups[old_url.split('/')[1]]

        if not already_client:

            class MockResource(ClientAccessMixin, resource):
                pass

        else:

            class MockResource(resource):
                pass

        api.add_resource(MockResource, ROUTE_URL, endpoint=ENDPOINT)

        try:
            self.BASE_API = URL
            return func(*args, **kwargs)

        finally:
            self.BASE_API = old_url
            del flask_app.view_functions['.'.join([api.app.name, ENDPOINT])]

    return wrapper
