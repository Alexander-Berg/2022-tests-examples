from datetime import timedelta

import pytest
from freezegun import freeze_time

from wiki.api_v2.exceptions import ValidationError
from wiki.api_v2.public.pages.consts import Location
from wiki.api_v2.public.pages.exceptions import AnchorNotFound, SectionNotFound
from wiki.pages.models import Page
from wiki.utils import timezone

pytestmark = [pytest.mark.django_db]


NEW_CONTENT = 'new_content'

OLD_BODY = 'old'
NEW_BODY_TOP = f'{NEW_CONTENT}\nold'
NEW_BODY_BOTTOM = f'old\n{NEW_CONTENT}'

OLD_BODY_ANCHOR = '1. {{ anchor name="customAnchor" }}\ntext'
NEW_BODY_ANCHOR = f'1. {{{{ anchor name="customAnchor" }}}}{NEW_CONTENT}\ntext'

OLD_BODY_A = '1. {{ a name="customAnchor" }}\ntext'
NEW_BODY_A = f'1. {{{{ a name="customAnchor" }}}}{NEW_CONTENT}\ntext'

OLD_BODY_SECTION = '===section1\nparagraph1\n===section2'
NEW_BODY_SECTION_TOP = f'===section1\n{NEW_CONTENT}\nparagraph1\n===section2'
NEW_BODY_SECTION_BOTTOM = f'===section1\nparagraph1\n{NEW_CONTENT}\n===section2'

OLD_BODY_MD_SECTION = '### section1\nparagraph1\n### section2'
NEW_BODY_MD_SECTION_TOP = f'### section1\n{NEW_CONTENT}\nparagraph1\n### section2'
NEW_BODY_MD_SECTION_BOTTOM = f'### section1\nparagraph1\n{NEW_CONTENT}\n### section2'

REQUEST_DATA_BY_DESTINATION = {
    'body': {
        'location': Location.TOP,
    },
    'anchor': {
        'name': 'test',
    },
    'section': {
        'id': 1,
        'location': Location.TOP,
    },
}


@pytest.mark.parametrize(
    'old_body,new_body,request_data',
    [
        (OLD_BODY, NEW_BODY_TOP, {'body': {'location': Location.TOP}}),
        (OLD_BODY, NEW_BODY_BOTTOM, {'body': {'location': Location.BOTTOM}}),
        (OLD_BODY_ANCHOR, NEW_BODY_ANCHOR, {'anchor': {'name': 'customAnchor'}}),
        (OLD_BODY_A, NEW_BODY_A, {'anchor': {'name': 'customAnchor'}}),
        (OLD_BODY_SECTION, NEW_BODY_SECTION_TOP, {'section': {'id': 1, 'location': Location.TOP}}),
        (OLD_BODY_SECTION, NEW_BODY_SECTION_BOTTOM, {'section': {'id': 1, 'location': Location.BOTTOM}}),
        (OLD_BODY_MD_SECTION, NEW_BODY_MD_SECTION_TOP, {'section': {'id': 1, 'location': Location.TOP}}),
        (OLD_BODY_MD_SECTION, NEW_BODY_MD_SECTION_BOTTOM, {'section': {'id': 1, 'location': Location.BOTTOM}}),
    ],
)
def test_page_append_content(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
    old_body: str,
    new_body: str,
    request_data: dict,
):
    test_page.body = old_body
    test_page.save()

    assert test_page.revision_set.all().count() == 1

    dt = timezone.now() + timedelta(days=10)

    with freeze_time(dt):
        client.login(wiki_users.asm)
        response = client.post(
            f'/api/v2/public/pages/{test_page.id}/append_content',
            {
                'content': NEW_CONTENT,
                **request_data,
            },
        )

    assert response.status_code == 200, response.json()
    test_page = _full_page_refresh_from_db(test_page)
    assert test_page.body == new_body
    assert test_page.last_author == wiki_users.asm
    assert test_page.revision_set.all().count() == 2
    last = test_page.revision_set.order_by('-created_at')[0]
    assert last.author == wiki_users.asm
    assert test_page.modified_at == dt
    assert test_page.body_size == len(new_body)


def test_page_append_content__section_not_found(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
):
    test_page.body = ''
    test_page.save()

    client.login(wiki_users.asm)
    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/append_content',
        {
            'content': NEW_CONTENT,
            'section': REQUEST_DATA_BY_DESTINATION['section'],
        },
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == SectionNotFound.error_code


def test_page_append_content__anchor_not_found(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
):
    test_page.body = ''
    test_page.save()

    client.login(wiki_users.asm)
    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/append_content',
        {
            'content': NEW_CONTENT,
            'anchor': REQUEST_DATA_BY_DESTINATION['anchor'],
        },
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == AnchorNotFound.error_code


@pytest.mark.parametrize(
    'destinations',
    [
        (('body', 'anchor', 'section')),
        (('body', 'anchor')),
        (('anchor', 'section')),
        (('body', 'section')),
        (()),
    ],
)
def test_page_append_content__wrong_destinations(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
    destinations,
):
    request_data = {
        'content': NEW_CONTENT,
        **{dest: REQUEST_DATA_BY_DESTINATION[dest] for dest in destinations},
    }

    client.login(wiki_users.asm)
    response = client.post(f'/api/v2/public/pages/{test_page.id}/append_content', request_data)
    assert response.status_code == 400
    assert response.json()['error_code'] == ValidationError.error_code


@pytest.mark.parametrize(
    'request_data',
    (
        {'content': NEW_CONTENT},
        {'anchor': REQUEST_DATA_BY_DESTINATION['anchor']},
        {'section': REQUEST_DATA_BY_DESTINATION['section']},
        {'body': REQUEST_DATA_BY_DESTINATION['body']},
        {'content': NEW_CONTENT, 'section': {'location': Location.TOP}},
    ),
)
def test_page_append_content__something_missed(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
    request_data,
):
    client.login(wiki_users.asm)
    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/append_content',
        request_data,
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == ValidationError.error_code


@pytest.mark.parametrize(
    'content',
    ('', None, {}),
)
def test_page_append_content__invalid_content(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
    content,
):
    client.login(wiki_users.asm)
    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/append_content',
        {
            'content': content,
            'body': REQUEST_DATA_BY_DESTINATION['body'],
        },
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == ValidationError.error_code


@pytest.mark.parametrize(
    'destination',
    ('body', 'anchor', 'section'),
)
@pytest.mark.parametrize(
    'destination_data',
    ('invalid', None, 1, {}),
)
def test_page_append_content__invalid_format_or_empty_destination_data(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
    destination,
    destination_data,
):
    client.login(wiki_users.asm)
    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/append_content',
        {
            'content': NEW_CONTENT,
            destination: destination_data,
        },
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == ValidationError.error_code


@pytest.mark.parametrize(
    'destination, destination_data',
    (
        ('body', {'location': None}),
        ('body', {'location': 'wrong'}),
        ('section', {'location': None, 'id': 1}),
        ('section', {'location': 'wrong', 'id': 1}),
        ('section', {'location': Location.TOP, 'id': 'wrong'}),
        ('section', {'location': Location.TOP, 'id': None}),
        ('anchor', {'name': None}),
    ),
)
def test_page_append_content__invalid_destination_data(
    client,
    wiki_users,
    test_page,
    organizations,
    groups,
    destination,
    destination_data,
):
    client.login(wiki_users.asm)
    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/append_content',
        {
            'content': NEW_CONTENT,
            destination: destination_data,
        },
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == ValidationError.error_code


def _full_page_refresh_from_db(obj: Page) -> Page:
    return Page.objects.get(pk=obj.pk)
