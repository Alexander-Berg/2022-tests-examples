import pytest

from django.conf import settings
from ujson import loads

from wiki import access as wiki_access
from wiki.api_core.errors.permissions import AlreadyRequestedAccess, UserHasNoAccess
from wiki.pages.models import Page, Revision
from wiki.users import DEFAULT_AVATAR_ID

from waffle.testutils import override_switch
from wiki.api_core.waffle_switches import OWNER_AS_FIRST_AUTHOR

pytestmark = [
    pytest.mark.django_db,
]

# структура, описывающая поля, которые должны быть в ответе про страницу.
page_json_struct = [
    'tag',
    'supertag',
    'url',
    'page_type',
    'is_redirect',
    'lang',
    'last_author',
    'title',
    'breadcrumbs',
    'created_at',
    'modified_at',
    'owner',
    'access',
    'version',
    'current_user_subscription',
    'user_css',
    'bookmark',
    'actuality_status',
    'qr_url',
    'comments_count',
    'is_official',
    'org',
    'comments_status',
    'authors',
    'with_new_wf',
    'is_readonly',
    'notifier',
]


class TestAPIPagesHandlerTest:
    """
    Tests for pages api handlers
    """

    def test_error404(self, client, wiki_users, api_url):
        """
        404
        """
        client.login('thasonic')
        response = client.get(f'{api_url}/NonExistentPage')

        json = loads(response.content)
        assert 'error' in json
        assert response.status_code == 404

    def test_access_error(self, client, wiki_users, test_page, api_url):
        """
        403
        """
        wiki_access.set_access(test_page, wiki_access.TYPES.OWNER, wiki_users.thasonic)

        client.login('chapson')
        response = client.get(f'{api_url}/{test_page.supertag}')
        assert response.status_code == 403

        json = loads(response.content)
        error = json['error']

        assert error['message'] == ['You have no access to requested resource']
        assert error['error_code'] == UserHasNoAccess.error_code

        if settings.IS_INTRANET:
            reason = 'хочу всё\nзнать'
            response = client.put(f'{api_url}/{test_page.supertag}/.requestaccess', {'reason': reason})
            assert response.status_code == 200

            response = client.get(f'{api_url}/{test_page.supertag}')
            error = loads(response.content)['error']
            assert response.status_code == 403
            assert error['error_code'] == AlreadyRequestedAccess.error_code

    def test_simple_show(self, client, wiki_users, test_page, api_url):
        """
        Получение страницы
        """
        client.login('thasonic')
        request_url = f'{api_url}/{test_page.supertag}'
        response = client.get(request_url)
        assert response.status_code == 200

        page_data = loads(response.content)['data']

        for example_key in page_json_struct:
            assert example_key in page_data

        # Проверить, что нет никаких новых полей в ответе.
        for key in page_data:
            assert key in page_json_struct

        assert page_data['actuality_status'] == 'actual'

        author = page_data['authors'][0]

        assert author['uid'] == int(wiki_users.thasonic.staff.uid)
        assert author['login'] == 'thasonic'
        assert author['display'] == 'Александр Покатилов'
        assert author['first_name'] == 'Александр'
        assert author['last_name'] == 'Покатилов'
        if settings.IS_INTRANET:
            assert not author['is_dismissed']

        authors = page_data['authors']
        assert len(authors) == 1
        assert author['uid'] == authors[0]['uid']
        assert author['login'] == authors[0]['login']
        assert author['avatar_id'] == DEFAULT_AVATAR_ID

        wiki_users.thasonic.staff.native_lang = 'en'
        wiki_users.thasonic.staff.save()

        response = client.get(request_url)
        assert response.status_code == 200
        page_data = loads(response.content)['data']

        assert page_data['with_new_wf']
        author = page_data['authors'][0]
        assert author['display'] == 'Alexander Pokatilov'
        assert author['first_name'] == 'Alexander'
        assert author['last_name'] == 'Pokatilov'
        assert not page_data['is_readonly']

    def test_get_owner_as_first_author(self, client, wiki_users, test_page, api_url):
        client.login(wiki_users.asm)

        request_url = f'{api_url}/{test_page.supertag}'

        assert test_page.owner == wiki_users.thasonic
        test_page.authors.add(wiki_users.volozh, wiki_users.asm)

        with override_switch(OWNER_AS_FIRST_AUTHOR, active=True):

            # 1. owner in authors
            assert test_page.authors.filter(id=wiki_users.thasonic.id).exists()

            response = client.get(request_url)
            assert response.status_code == 200

            authors = loads(response.content)['data']['authors']
            assert authors[0]['login'] == test_page.owner.username
            assert {author['login'] for author in authors} == {'thasonic', 'asm', 'volozh'}

            # 2. owner not in authors
            test_page.authors.set([wiki_users.volozh, wiki_users.asm])
            assert test_page.authors.filter(id=wiki_users.thasonic.id).exists() is False

            response = client.get(request_url)
            assert response.status_code == 200

            authors = loads(response.content)['data']['authors']
            assert authors[0]['login'] == test_page.owner.username
            assert {author['login'] for author in authors} == {'thasonic', 'asm', 'volozh'}

            # 3. owner is None
            test_page.owner = None
            test_page.save()

            response = client.get(request_url)
            assert response.status_code == 200

            authors = loads(response.content)['data']['authors']
            assert authors[0]['login'] in ['asm', 'volozh']  # нельзя гарантировать кто будет 1 автор
            assert {author['login'] for author in authors} == {'asm', 'volozh'}

    def test_create_simple_page(self, client, wiki_users, api_url):
        page_data = {'title': 'ЙаЗаголовок', 'body': 'Йа %%тельце%%, и я хочу быть в базе'}

        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag='testpage')

        tag = 'ТестПаге'
        client.login('thasonic')
        request_url = f'{api_url}/{tag}'
        response = client.post(request_url, data=page_data)
        assert response.status_code == 200

        data = loads(response.content)['data']
        assert data['supertag'] == 'testpage'
        assert data['tag'] == tag
        assert data['url'] == '/testpage'

        page = Page.active.filter(supertag='testpage').get()
        assert page.tag == tag
        assert page.body == 'Йа %%тельце%%, и я хочу быть в базе'
        assert page.title == 'ЙаЗаголовок'

        rev = Revision.objects.get(page=page)
        assert rev.body == 'Йа %%тельце%%, и я хочу быть в базе'
        assert rev.created_at == page.modified_at

    def test_create_reserved_supertag_page(self, client, wiki_users, api_url):
        page_data = {'title': 'Ковологаз', 'body': 'Просто текст без %%смысла%%'}
        tag = supertag = 'users/testuser'
        client.login('thasonic')

        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag)

        request_url = f'{api_url}/{tag}'
        response = client.post(request_url, data=page_data)
        assert response.status_code == 409
        assert response.json()['error']['message'][0] == f'Bad tag given: "{supertag}"'

        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag)

        request_url = f'{api_url}/{tag}/.grid/create'
        response = client.post(request_url, data=page_data)
        assert response.status_code == 409
        assert response.json()['error']['message'][0], 'Bad tag given: "{supertag}"'

        with pytest.raises(Page.DoesNotExist):
            Page.active.get(supertag=supertag)
