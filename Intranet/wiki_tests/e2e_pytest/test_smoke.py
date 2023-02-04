from intranet.wiki.tests.wiki_tests.common.e2e.config import e2e_settings
from intranet.wiki.tests.wiki_tests.common.e2e.creds import KOPATYCH
from intranet.wiki.tests.wiki_tests.common.e2e.decorators import e2e
from intranet.wiki.tools.wikiclient import EnvType, Flavor, get_contour


@e2e
def test_me_settings():
    s = get_contour(EnvType.TESTING, Flavor.INTRANET)
    s.wiki_api.oauth().use_api_v2_public()

    code, data = s.wiki_api.api_call('get', 'me/settings')

    assert 200 == code


@e2e
def test_homepage_spa():
    s = get_contour(EnvType.TESTING, Flavor.INTRANET)
    s.wiki_api.oauth().use_api_v2_public()

    code, data = s.wiki_api.api_call('get', 'pages/homepage')
    assert 200 == code

    print(data['body'])


@e2e
def test_homepage():
    s = get_contour(EnvType.TESTING, Flavor.B2B)
    s.wiki_api.as_user(KOPATYCH).use_api_v2_public()

    code, data = s.wiki_api.api_call('get', 'pages', params={'slug': 'homepage', 'fields': 'content, breadcrumbs'})

    assert 200 == code, data


@e2e
def test_oauth_b2b():
    s = get_contour(EnvType.TESTING, Flavor.B2B)
    s.wiki_api.org_id = '10001066'
    s.wiki_api.oauth(e2e_settings.oauth_b2b)
    s.wiki_api.use_api_v2_public()
    code, data = s.wiki_api.api_call('get', 'me')
    assert 200 == code
