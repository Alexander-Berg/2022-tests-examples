from collections import Counter
from data_types.price_item import PriceItem, generate_prices, generate_image
from data_types.group import Group
from data_types.moderation import Moderation
from data_types.company import Company
from data_types.chain import Chain
from lib.server import server

DEFAULT_SEARCH_GROUPS_LIMIT = 20
DEFAULT_ORGANIZATIONS_GROUPS_LIMIT = 100


def test_get_groups_from_empty_db(user):
    response_json = server.get_groups_1x(user) >> 200
    assert len(response_json) == 0


def check_group(group, id, name, statuses=[], message=''):
    assert group['id'] == str(id)
    assert group['name'] == name

    if statuses or message:
        moderation = group['moderation']
        assert moderation.get('status') in statuses
        assert moderation.get('message') == message


def make_test_groups():
    MODERATION_ID = 1
    FIRST_GROUP_ID = 1
    SECOND_GROUP_ID = 2

    Moderation(MODERATION_ID, 'Waiting', 'Text').register()
    Group(FIRST_GROUP_ID, 'Hippopotamus', MODERATION_ID).register()
    Group(SECOND_GROUP_ID, 'Crocodiles').register()


def test_get_groups(user):
    make_test_groups()
    response_json = server.get_groups_1x(user) >> 200

    assert len(response_json) == 2
    check_group(response_json[1], 1, 'Hippopotamus', statuses=["Waiting", "InProgress", "Approved"])
    check_group(response_json[0], 2, 'Crocodiles')


def test_get_groups_limit(user):
    make_test_groups()
    response_json = server.get_groups_1x(user, limit=1) >> 200

    assert len(response_json) == 1


def test_get_groups_default_limit(user, alien_user, alien_company):
    prices_count = 2 * DEFAULT_SEARCH_GROUPS_LIMIT
    generate_prices(alien_user, alien_company, count=prices_count)

    response_json = server.get_groups_1x(user) >> 200
    assert len(response_json) == DEFAULT_SEARCH_GROUPS_LIMIT


def test_get_groups_by_name(user):
    make_test_groups()
    response_json = server.get_groups_1x(user, query='cRoCoDil') >> 200

    assert len(response_json) == 1
    check_group(response_json[0], 2, 'Crocodiles')


def make_test_company_groups(user, organization):
    Group(1, 'Elephants').register()
    Group(2, 'Hippopotamus').register()
    Group(3, 'Crocodiles').register()

    server.post_price(user, PriceItem(group={'id': '1'}, photos=[generate_image(user)]), organization) >> 200
    server.post_price(user, PriceItem(group={'id': '2'}), organization) >> 200


def test_get_company_groups(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    response_json = server.get_company_groups_1x(user, organization) >> 200

    assert len(response_json) == 2

    names_counter = Counter(group['name'] for group in response_json)
    assert names_counter['Elephants'] == 1
    assert names_counter['Hippopotamus'] == 1


def test_get_company_groups_limit(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    response_json = server.get_company_groups_1x(user, organization, limit=1) >> 200

    assert len(response_json) == 1


def test_get_company_groups_default_limit(user, organization, feeds_companies):
    prices_count = 2 * DEFAULT_ORGANIZATIONS_GROUPS_LIMIT
    generate_prices(user, organization, count=prices_count)

    response_json = server.get_company_groups_1x(user, organization) >> 200
    assert len(response_json) == DEFAULT_ORGANIZATIONS_GROUPS_LIMIT


def test_get_company_groups_with_photos(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    groups_with_photo = ['Elephants']  # see make_test_company_groups()

    response_json = server.get_company_groups_1x(user, organization, with_photos=True) >> 200

    assert len(response_json) == 1

    for group in response_json:
        assert group['name'] in groups_with_photo


def test_get_company_groups_by_name(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    response_json = server.get_company_groups_1x(user, organization, query='Hippopotamus') >> 200

    assert len(response_json) == 1
    check_group(response_json[0], 2, 'Hippopotamus')


def test_get_unexistent_company_groups_fails(user, organization, feeds_companies):
    make_test_company_groups(user, organization)

    if organization.organization_type == "company":
        unexisting_organization = Company()
    else:
        unexisting_organization = Chain()
    server.get_company_groups_1x(user, unexisting_organization) >> 403


def test_get_company_empty_groups(user, organization, feeds_companies):
    server.post_price(user, PriceItem().without_group(), organization) >> 200
    response_json = server.get_company_groups_1x(user, organization) >> 200
    assert len(response_json) == 0


def test_get_company_groups_with_doublets(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    server.post_price(user, PriceItem(group={'id': '1'}), organization) >> 200

    response_json = server.get_company_groups_1x(user, organization) >> 200
    assert len(response_json) == 2
