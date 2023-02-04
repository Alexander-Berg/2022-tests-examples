# coding: utf-8
from __future__ import unicode_literals

from django import http

from mock import patch
from pretend import stub
import pytest
import operator

from static_api import resources
from static_api.views import docs


if __name__ == '__main__':
    user_resource = stub(
        name='user',
        plural='users',
        list_order=20,
        doc_tpl_location='docs/user.html',
    )
    group_resource = stub(
        name='group',
        plural='groups',
        list_order=10,
    )

    resources.registry.register(
        user_resource,
        group_resource,
    )


@patch('static_api.views.docs.render')
def test_doc_view_index(render_mock, rf):
    request = rf.get('whatever')

    docs.index(request)

    render_mock.assert_called_with(
        request=request,
        template_name='static_api/docs/index.html',
        context={
            'resources': sorted(
                resources.registry.by_name.values(),
                key=operator.attrgetter('list_order'),
            )
        }
    )


def test_doc_view_resource_404(rf):
    request = rf.get('whatever')

    with pytest.raises(http.Http404):
        docs.doc(request, 'tickets')


@patch('static_api.views.docs.render')
def test_doc_view_resource_200(render_mock, rf):
    request = rf.get('whatever')

    docs.doc(request, 'persons')

    render_mock.assert_called_with(
        request=request,
        template_name='api/docs/person.html',
        context={
            'registry': resources.registry.by_name,
            'resource': resources.registry.by_plural.get('persons')
        },
    )
