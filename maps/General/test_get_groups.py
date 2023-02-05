from collections import Counter
from data_types.price_item import PriceItem, generate_prices, generate_image
from data_types.group import Group
from data_types.moderation import Moderation
from data_types.company import Company
from data_types.chain import Chain

from lib.random import printable_str
from lib.server import server

import random

DEFAULT_SEARCH_GROUPS_LIMIT = 20
DEFAULT_ORGANIZATIONS_GROUPS_LIMIT = 100


def test_get_groups_from_empty_db(user):
    response_json = server.get_groups(user) >> 200
    assert len(response_json["groups"]) == 0
    assert response_json["pager"]["limit"] == DEFAULT_SEARCH_GROUPS_LIMIT
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == 0


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


def make_groups(groups_count):
    groups = []
    for _ in range(groups_count):
        group = Group(None, printable_str(5, 20))
        group.register()
        groups.append(group)
    return groups


def test_get_groups(user):
    make_test_groups()
    response_json = server.get_groups(user) >> 200

    assert len(response_json["groups"]) == 2
    check_group(response_json["groups"][1], 1, 'Hippopotamus', statuses=["Waiting", "InProgress", "Approved"])
    check_group(response_json["groups"][0], 2, 'Crocodiles')
    assert response_json["pager"]["limit"] == DEFAULT_SEARCH_GROUPS_LIMIT
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == 2


def test_get_groups_limit(user):
    make_test_groups()
    response_json = server.get_groups(user, limit=1) >> 200

    assert len(response_json["groups"]) == 1
    assert response_json["pager"]["limit"] == 1
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == 2


def test_get_groups_default_limit(user, alien_user, alien_company):
    prices_count = 2 * DEFAULT_SEARCH_GROUPS_LIMIT
    generate_prices(alien_user, alien_company, count=prices_count)

    response_json = server.get_groups(user) >> 200
    assert len(response_json["groups"]) == DEFAULT_SEARCH_GROUPS_LIMIT
    assert response_json["pager"]["limit"] == DEFAULT_SEARCH_GROUPS_LIMIT
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == prices_count


def test_get_groups_by_name(user):
    make_test_groups()
    response_json = server.get_groups(user, query='cRoCoDil') >> 200

    assert len(response_json["groups"]) == 1
    check_group(response_json["groups"][0], 2, 'Crocodiles')


def test_get_ru_groups_by_upcase_name(user):
    Group(1, 'едА ВКУСНАЯ').register()
    response_json = server.get_groups(user, query='Еда') >> 200

    assert len(response_json["groups"]) == 1
    check_group(response_json["groups"][0], 1, 'едА ВКУСНАЯ')


def make_test_company_groups(user, organization):
    Group(1, 'Elephants').register()
    Group(2, 'Hippopotamus').register()
    Group(3, 'Crocodiles').register()

    server.post_price(user, PriceItem(group={'id': '1'}, photos=[generate_image(user)]), organization) >> 200
    server.post_price(user, PriceItem(group={'id': '2'}), organization) >> 200


def test_get_company_groups(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    response_json = server.get_company_groups(user, organization) >> 200

    assert len(response_json["groups"]) == 2
    assert response_json["pager"]["limit"] == DEFAULT_ORGANIZATIONS_GROUPS_LIMIT
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == 2

    names_counter = Counter(group['name'] for group in response_json["groups"])
    assert names_counter['Elephants'] == 1
    assert names_counter['Hippopotamus'] == 1


def test_get_company_groups_limit(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    response_json = server.get_company_groups(user, organization, limit=1) >> 200

    assert len(response_json["groups"]) == 1
    assert response_json["pager"]["limit"] == 1
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == 2


def test_get_company_groups_default_limit(user, organization, feeds_companies):
    prices_count = 2 * DEFAULT_ORGANIZATIONS_GROUPS_LIMIT
    generate_prices(user, organization, count=prices_count)

    response_json = server.get_company_groups(user, organization) >> 200
    assert len(response_json["groups"]) == DEFAULT_ORGANIZATIONS_GROUPS_LIMIT
    assert response_json["pager"]["limit"] == DEFAULT_ORGANIZATIONS_GROUPS_LIMIT
    assert response_json["pager"]["offset"] == 0
    assert response_json["pager"]["total"] == prices_count


def test_get_company_groups_with_photos(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    groups_with_photo = ['Elephants']  # see make_test_company_groups()

    response_json = server.get_company_groups(user, organization, with_photos=True) >> 200

    assert len(response_json["groups"]) == 1

    for group in response_json["groups"]:
        assert group['name'] in groups_with_photo


def test_get_company_groups_by_name(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    response_json = server.get_company_groups(user, organization, query='Hippopotamus') >> 200

    assert len(response_json["groups"]) == 1
    check_group(response_json["groups"][0], 2, 'Hippopotamus')


def test_get_unexistent_company_groups_fails(user, organization, feeds_companies):
    make_test_company_groups(user, organization)

    if organization.organization_type == "company":
        unexisting_organization = Company()
    else:
        unexisting_organization = Chain()
    server.get_company_groups(user, unexisting_organization) >> 403


def test_get_company_empty_groups(user, organization, feeds_companies):
    server.post_price(user, PriceItem().without_group(), organization) >> 200
    response_json = server.get_company_groups(user, organization) >> 200
    assert len(response_json["groups"]) == 0


def test_get_company_groups_with_doublets(user, organization, feeds_companies):
    make_test_company_groups(user, organization)
    server.post_price(user, PriceItem(group={'id': '1'}), organization) >> 200

    response_json = server.get_company_groups(user, organization) >> 200
    assert len(response_json["groups"]) == 2


def test_check_suggest_escaping(user):
    Group(id=1, name="Some' group").register()
    response_json = server.get_groups(user, query="Some'") >> 200
    assert len(response_json["groups"]) == 1


def test_list_chain_groups(user, chain, chain_company):
    Group(1, 'Chain group').register()
    server.post_price(user, PriceItem(group={'id': '1'}), chain) >> 200

    Group(2, 'Company group').register()
    server.post_price(user, PriceItem(group={'id': '2'}), chain_company) >> 200

    response_json = server.get_company_groups(user, chain_company) >> 200

    assert len(response_json["groups"]) == 2
    names_counter = Counter(group['name'] for group in response_json["groups"])
    assert names_counter['Chain group'] == 1
    assert names_counter['Company group'] == 1


def test_get_groups_with_big_parameter_values(user):
    PGSQL_BIGINT_MAX = (1 << 63) - 1
    LIMIT_MAX = 10000
    PGSQL_BIGINT_OVERFLOW = (1 << 64) - 1
    response_json = server.get_groups(user, limit=PGSQL_BIGINT_OVERFLOW, offset=PGSQL_BIGINT_OVERFLOW) >> 200

    assert response_json["pager"]["limit"] == LIMIT_MAX
    assert response_json["pager"]["offset"] == PGSQL_BIGINT_MAX


def test_get_many_groups(user):
    limit = random.randint(5, 10)
    offset = random.randint(5, 10)
    expected_total_count = offset + limit * 10
    groups_count = random.randint(expected_total_count + 1, 2 * expected_total_count)
    for _ in range(groups_count):
        Group(None, printable_str(5, 20)).register()

    response_json = server.get_groups(user, limit=limit, offset=offset) >> 200
    assert response_json["pager"]["total"] == expected_total_count


def test_get_many_company_groups(user, organization):
    limit = random.randint(5, 10)
    offset = random.randint(5, 10)
    expected_total_count = offset + limit * 10

    groups_count = random.randint(expected_total_count + 1, 2 * expected_total_count)
    groups = make_groups(groups_count)

    for group in groups:
        server.post_price(user, PriceItem(group={'id': str(group.id)}), organization) >> 200

    response_json = server.get_company_groups(user, organization, limit=limit, offset=offset) >> 200
    assert response_json["pager"]["total"] == expected_total_count
