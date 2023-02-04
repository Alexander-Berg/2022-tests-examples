import json
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from mock import patch, call

from plan.services.models import Service

from common import factories
from utils import Response

pytestmark = pytest.mark.django_db


@patch('plan.common.utils.http.Session.get')
def test_suggest_service(get, client, django_assert_num_queries, staff_factory):
    service_1 = factories.ServiceFactory(owner=staff_factory())
    service_2 = factories.ServiceFactory()
    service_3 = factories.ServiceFactory()

    intrasearch_response = {
        'services': {
            'result': [
                {
                    'title': 'Сервис 1',
                    'id': service_1.id,
                },
                {
                    'title': 'Сервис 2',
                    'id': service_2.id,
                },
                {
                    'title': 'Сервис 3',
                    'id': service_3.id,
                }
            ],
            'pagination': {
                'page': 0,
                'per_page': 5,
                'pages': 1,
                'count': 3
            }
        }
    }

    service_ticket = '1abc1'
    user_ticket = '1abc1'

    get.return_value = Response(200, json.dumps(intrasearch_response))
    headers = {'HTTP_X_YA_USER_TICKET': user_ticket}
    client.login(service_1.owner.login)

    with patch('plan.api.suggests.services.views.get_tvm_ticket') as get_tvm_ticket:
        get_tvm_ticket.return_value = service_ticket

        with django_assert_num_queries(5):
            # 1 select intranet_staff join auth_user
            # 1 select middleware
            # 1 select service
            # 1 pg_is_in_recovery()
            # 1 select в waffle readonly switch
            response = client.json.get(reverse('api-frontend:suggest-service-list'), **headers)

    params = {
        'layers': 'services',
        'version': 2,
        'text': None,
        'services.page': 0,
        'services.per_page': 5,
        'services.query': 's_status:"needinfo" | s_status:"develop" | s_status:"supported"'
    }
    header = {
        'Content-type': 'application/json',
        'X-Ya-Service-Ticket': service_ticket,
        'X-Ya-User-Ticket': user_ticket,
    }

    assert get.call_args_list == [call(
        settings.INTRASEARCH_ENDPOINT,
        params=params,
        headers=header
    )]

    results = response.json()['result']
    for res in results:
        service = Service.objects.get(pk=res['id'])
        assert res['is_base_non_leaf'] == service.is_base_non_leaf()
        assert res['name'] == {
            'ru': service.name,
            'en': service.name_en,
        }


@patch('plan.common.utils.http.Session.get')
def test_suggest_service_500(get, client, staff_factory):
    get.return_value = Response(500, json.dumps({}))
    headers = {'HTTP_X_YA_USER_TICKET': 'eee'}
    service_ticket = '1abc1'
    client.login(factories.ServiceFactory(owner=staff_factory()).owner.login)

    with patch('plan.api.suggests.services.views.get_tvm_ticket') as get_tvm_ticket:
        get_tvm_ticket.return_value = service_ticket
        response = client.json.get(reverse('api-frontend:suggest-service-list'), **headers)

    assert response.status_code == 424
    assert response.json()['error']['message']['ru'] == 'Intrasearch вернул 500.'
