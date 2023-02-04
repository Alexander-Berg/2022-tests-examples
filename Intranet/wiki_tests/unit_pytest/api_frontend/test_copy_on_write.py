from ujson import loads

import pytest
from django.conf import settings
from django.template.loader import render_to_string
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import ENABLE_COPY_ON_WRITE
from wiki.org import get_org, get_org_lang
from wiki.pages.models import Page
from wiki.sync.connect.org_ctx import org_ctx
from wiki.utils.limits import limit__user_max_rev_num

pytestmark = [
    pytest.mark.django_db,
]


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_show_cow_page(client, wiki_users, api_url):
    """
    Получение copy on write страницы, должна вернуться эталонная страница
    """
    client.login('thasonic')
    current_org = get_org()
    lang = get_org_lang()

    for supertag in ('homepage', 'users'):
        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag, org=current_org)

        request_url = f'{api_url}/{supertag}'
        response = client.get(request_url)

        if supertag in settings.COPY_ON_WRITE_TAGS:
            assert response.status_code == 200

            template_data = settings.COPY_ON_WRITE_TAGS[supertag][lang]

            response_data = loads(response.content)['data']
            assert response_data['title'] == template_data['title']
            assert response_data['supertag'] == supertag
            assert response_data['actuality_status'] == 'actual'
            assert response_data['authors'][0]['login'] == wiki_users.robot_wiki.username
            if current_org:
                assert response_data['org']['dir_id'] == current_org.dir_id
        else:
            assert response.status_code == 404


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_raw_cow_page(client, wiki_users, api_url):
    client.login('thasonic')
    current_org = get_org()
    lang = get_org_lang()

    for supertag in ('homepage', 'users'):
        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag, org=current_org)

        request_url = f'{api_url}/{supertag}/.raw'
        response = client.get(request_url)

        if supertag in settings.COPY_ON_WRITE_TAGS:
            assert response.status_code == 200

            template_data = settings.COPY_ON_WRITE_TAGS[supertag][lang]

            response_data = loads(response.content)['data']
            assert response_data['body'] == render_to_string(template_data['template'])
            assert response_data['title'] == template_data['title']
            assert response_data['supertag'] == supertag
        else:
            assert response.status_code == 404


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_raw_cow_keywords(client, wiki_users, api_url):
    client.login('thasonic')
    current_org = get_org()

    supertag = 'homepage'
    if supertag in settings.COPY_ON_WRITE_TAGS:
        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag, org=current_org)

        request_url = f'{api_url}/{supertag}/.keywords'
        response = client.get(request_url)

        assert response.status_code == 200


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_breadcrumbs(client, wiki_users, api_url):
    if 'users' in settings.COPY_ON_WRITE_TAGS:
        thasonic = wiki_users.thasonic
        client.login('thasonic')
        supertag = 'users/thasonic'
        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag='users', org=get_org())

        request_url = f'{api_url}/{supertag}'
        response = client.get(request_url)
        assert response.status_code == 200
        response_data = loads(response.content)['data']
        template_data = settings.COPY_ON_WRITE_TAGS['users'][get_org_lang()]
        expected_breadcrumbs = [
            {'is_active': True, 'tag': 'users', 'title': template_data['title'], 'url': '/users'},
            {
                'is_active': True,
                'tag': 'users/thasonic',
                'title': f'{thasonic.first_name} {thasonic.last_name} ({thasonic.username})',
                'url': '/users/thasonic',
            },
        ]
        assert response_data['breadcrumbs'] == expected_breadcrumbs


def test_show_cow_page_with_disabled_flag(client, wiki_users, api_url):
    """
    С отключенным флагом должны получить 404
    """
    client.login('thasonic')
    request_url = f'{api_url}/homepage'
    response = client.get(request_url)
    assert response.status_code == 404


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_edit_cow_page(client, wiki_users, api_url, test_org):
    page_data = {'body': 'some body', 'version': '999999'}
    client.login('thasonic')

    with org_ctx(test_org):
        current_org = get_org()
        lang = get_org_lang()

    for supertag in ('homepage', 'users'):
        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag, org=current_org)

        request_url = f'{api_url}/{supertag}'
        response = client.post(request_url, data=page_data)
        if supertag in settings.COPY_ON_WRITE_TAGS:
            Page.active.get(supertag=supertag, org=current_org)

            assert response.status_code == 200
            response_data = loads(response.content)['data']
            template_data = settings.COPY_ON_WRITE_TAGS[supertag][lang]
            # Мы не передавали title,
            # он должен был быть скопирован из шаблона
            assert response_data['title'] == template_data['title']
            assert response_data['supertag'] == supertag
            assert response_data['body'] == page_data['body']
        else:
            # Пытаемся сохранить не cow страницу без title
            # Нет title нет новой страницы
            assert response.status_code == 409


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_show_watchers(client, wiki_users, api_url, test_org):
    supertag = 'homepage'

    current_org = test_org
    with pytest.raises(Page.DoesNotExist):
        Page.active.get(supertag=supertag, org=current_org)

    client.login('thasonic')

    watchers_url = f'{api_url}/{supertag}/.watchers'
    response = client.get(watchers_url)

    if supertag in settings.COPY_ON_WRITE_TAGS:
        assert response.status_code == 200
        response_data = loads(response.content)['data']
        assert len(response_data) == 0

        # Подписываемся на страницу через API, при этом должна
        # создаться страница на основе эталонной
        watch_url = f'{api_url}/{supertag}/.watch'
        response = client.post(watch_url)
        assert response.status_code == 200
        Page.active.get(supertag=supertag, org=current_org)

        response = client.get(watchers_url)
        assert response.status_code == 200
        response_data = loads(response.content)['data']
        assert any(watch['login'] == 'thasonic' for watch in response_data)

    else:
        assert response.status_code == 404


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_show_revisions(client, wiki_users, api_url):
    client.login('thasonic')
    supertag = 'homepage'

    with pytest.raises(Page.DoesNotExist):
        Page.active.get(supertag=supertag, org=get_org())

    request_url = f'{api_url}/{supertag}/.revisions'
    response = client.get(request_url)
    if supertag in settings.COPY_ON_WRITE_TAGS:
        assert response.status_code == 200
        response_data = loads(response.content)['data']
        assert len(response_data['data']) == 0
        assert response_data['total'] == 0
    else:
        assert response.status_code == 404


@override_switch(ENABLE_COPY_ON_WRITE, active=True)
def test_show_events(client, wiki_users, api_url):
    client.login('thasonic')
    supertag = 'homepage'

    with pytest.raises(Page.DoesNotExist):
        Page.active.get(supertag=supertag, org=get_org())

    request_url = f'{api_url}/{supertag}/.events'
    response = client.get(request_url)
    if supertag in settings.COPY_ON_WRITE_TAGS:
        assert response.status_code == 200
        response_data = loads(response.content)['data']
        limit = limit__user_max_rev_num.get()
        assert len(response_data['data']) == 0
        assert response_data['limit'] == limit
        assert response_data['limit_exceeded'] is False
        assert response_data['total'] == 0
    else:
        assert response.status_code == 404
