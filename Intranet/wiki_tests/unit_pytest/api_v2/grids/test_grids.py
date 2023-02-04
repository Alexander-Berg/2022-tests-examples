import pytest

from wiki.api_v2.public.grids.exceptions import ColumnRequiredError, ExtraRowValue, RowValidationError

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json

pytestmark = [pytest.mark.django_db]

GRID_STRUCTURE = {
    'fields': [
        {'slug': 'c1', 'title': 'Column1', 'type': 'string', 'required': True},
        {
            'slug': 'c2',
            'title': 'Column2',
            'type': 'number',
            'required': False,
        },
        {
            'slug': 'c3',
            'title': 'Column3',
            'type': 'date',
            'required': True,
        },
    ]
}


def test_get_grid(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/grids/{test_inline_grid.id}')
    assert response.status_code == 200
    page = test_inline_grid.page
    expected_response = {
        'id': test_inline_grid.id,
        'page': {'id': page.id, 'slug': page.slug},
        'rows': test_inline_grid.rows,
        'structure': test_inline_grid.structure | {'sorting': None},
        'title': test_inline_grid.title,
    }
    assert response.json() == expected_response


def test_get_grid_no_access(client, wiki_users, test_inline_grid):
    page = test_inline_grid.page
    set_access_author_only(page)
    client.login(wiki_users.chapson)
    response = client.get(f'/api/v2/public/grids/{test_inline_grid.id}')
    assert response.status_code == 403


def test_create_empty_grid(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/grids', {'title': 'Test Grid', 'page': {'slug': test_page.slug}})
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'page': {'id': test_page.id, 'slug': test_page.slug},
            'rows': [],
            'structure': {'fields': [], 'sorting': None},
            'title': 'Test Grid',
        },
    )


def test_create_grid_no_access(client, wiki_users, test_page):
    set_access_author_only(test_page)
    client.login(wiki_users.chapson)
    response = client.post('/api/v2/public/grids', {'title': 'Test Grid', 'page': {'slug': test_page.slug}})
    assert response.status_code == 403


def test_create_grid_with_structure(client, wiki_users, test_page):
    request_data = {
        'title': 'Test Grid',
        'page': {'slug': test_page.slug},
        'structure': GRID_STRUCTURE,
    }

    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/grids', data=request_data)
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'page': {'id': test_page.id, 'slug': test_page.slug},
            'rows': [],
            'structure': GRID_STRUCTURE | {'sorting': None},
            'title': request_data['title'],
        },
    )


def test_create_grid_with_rows(client, wiki_users, test_page):
    grid_rows = [
        ['1.1', 1.2, '1655842678'],
        ['2.1', '2.2', '2007-10-07'],
        ['3.1', None, '2007-10-08'],
    ]
    request_data = {
        'title': 'Test Grid',
        'page': {'slug': test_page.slug},
        'structure': GRID_STRUCTURE,
        'rows': grid_rows,
    }

    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/grids', data=request_data)
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'page': {'id': test_page.id, 'slug': test_page.slug},
            'rows': [
                {'id': 1, 'row': ['1.1', 1.2, '2022-06-21']},
                {'id': 2, 'row': ['2.1', 2.2, '2007-10-07']},
                {'id': 3, 'row': ['3.1', None, '2007-10-08']},
            ],
            'structure': GRID_STRUCTURE | {'sorting': None},
            'title': request_data['title'],
        },
    )


def test_create_grid_validation_error(client, wiki_users, test_page):
    grid_rows = [
        ['1.1', 'not a number', '1655842678'],
    ]
    request_data = {
        'title': 'Test Grid',
        'page': {'slug': test_page.slug},
        'structure': GRID_STRUCTURE,
        'rows': grid_rows,
    }

    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/grids', data=request_data)

    assert response.status_code == 400
    assert response.json()['error_code'] == RowValidationError.error_code


def test_create_grid_required_field_error(client, wiki_users, test_page):
    grid_rows = [
        ['1.1', 1.2],
    ]
    request_data = {
        'title': 'Test Grid',
        'page': {'slug': test_page.slug},
        'structure': GRID_STRUCTURE,
        'rows': grid_rows,
    }

    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/grids', data=request_data)
    assert response.status_code == 400
    assert response.json()['error_code'] == ColumnRequiredError.error_code


def test_create_grid_extra_field_error(client, wiki_users, test_page):
    grid_rows = [
        ['1.1', 1.2, '1655842678', 'One more'],
    ]
    request_data = {
        'title': 'Test Grid',
        'page': {'slug': test_page.slug},
        'structure': GRID_STRUCTURE,
        'rows': grid_rows,
    }

    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/grids', data=request_data)
    assert response.status_code == 400
    assert response.json()['error_code'] == ExtraRowValue.error_code


def test_add_rows(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {'rows': []}
    response = client.post(f'/api/v2/public/grids/{test_inline_grid.id}/rows', data=request_data)
    assert response.status_code == 200


def test_add_rows_with_data(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {'rows': []}
    response = client.post(f'/api/v2/public/grids/{test_inline_grid.id}/rows', data=request_data)
    assert response.status_code == 200


def test_remove_rows(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {'row_ids': []}
    response = client.delete(f'/api/v2/public/grids/{test_inline_grid.id}/rows', data=request_data)
    assert response.status_code == 200


def test_add_columns(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {
        'columns': [
            {'slug': 'newcolumn', 'title': 'New Column', 'type': 'string', 'required': True},
        ]
    }
    response = client.post(f'/api/v2/public/grids/{test_inline_grid.id}/columns', data=request_data)
    assert response.status_code == 200


def test_remove_columns(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {'column_slugs': ['col1', 'col4']}
    response = client.delete(f'/api/v2/public/grids/{test_inline_grid.id}/columns', data=request_data)
    assert response.status_code == 200, response.json()


def test_move_row(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {'position': 50, 'row_id': 1}
    response = client.post(f'/api/v2/public/grids/{test_inline_grid.id}/rows/move', data=request_data)
    assert response.status_code == 200


def test_move_column(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {'position': 50, 'column_slug': 'newcolumn'}
    response = client.post(f'/api/v2/public/grids/{test_inline_grid.id}/columns/move', data=request_data)
    assert response.status_code == 200, response.json()


def test_update_cells_value(client, wiki_users, test_inline_grid):
    client.login(wiki_users.thasonic)
    request_data = {
        'cells': [
            {'value': 'some text', 'location': {'row_id': 1, 'column_slug': 'col1'}},
        ]
    }
    response = client.put(f'/api/v2/public/grids/{test_inline_grid.id}/cells', data=request_data)
    assert response.status_code == 200, response.json()
