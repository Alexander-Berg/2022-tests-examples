from wiki.grids.models import Grid
from wiki.grids.utils import changes_of_structure
from wiki.utils.supertag import tag_to_supertag


def create_grid(client, api_url, grid_tag, grid_structure, user):
    response = client.post(
        f'{api_url}/{grid_tag}/.grid/create',
        dict(title='grid title'),
    )
    if not response.status_code == 200:
        raise ValueError(f'Unexpected status code {response.status_code}')

    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))

    previous_structure = grid.access_structure.copy()

    grid.change_structure(grid_structure)
    grid.save()

    changes_of_structure(user, grid, previous_structure)
    return grid


def edit_row(client, api_url, grid_tag, row_id, new_row_data, expected_status_code=200):
    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
    response = client.post(
        f'{api_url}/{grid_tag}/.grid/change',
        data={
            'version': grid.get_page_version(),
            'changes': [
                {
                    'edited_row': {
                        'id': row_id,
                        'data': new_row_data,
                    }
                }
            ],
        },
    )
    if not response.status_code == expected_status_code:
        raise ValueError(f'Unexpected status code {response.status_code}')


def add_row(client, api_url, grid_tag, row_data, after_id='-1', expected_status_code=200):
    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
    response = client.post(
        f'{api_url}/{grid_tag}/.grid/change',
        data={
            'version': grid.get_page_version(),
            'changes': [
                {
                    'added_row': {
                        'after_id': after_id,
                        'data': row_data,
                    }
                }
            ],
        },
    )
    if not response.status_code == expected_status_code:
        raise ValueError(f'Unexpected status code {response.status_code}')


def move_row(client, api_url, grid_tag, row_id, after_id, before_id, expected_status_code=200):
    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
    response = client.post(
        f'{api_url}/{grid_tag}/.grid/change',
        data={
            'version': grid.get_page_version(),
            'changes': [
                {
                    'row_moved': {
                        'id': row_id,
                        'after_id': after_id,
                        'before_id': before_id,
                    }
                }
            ],
        },
    )
    if not response.status_code == expected_status_code:
        raise ValueError(f'Unexpected status code {response.status_code}')


def remove_row(client, api_url, grid_tag, row_id, expected_status_code=200):
    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
    response = client.post(
        f'{api_url}/{grid_tag}/.grid/change',
        data={
            'version': grid.get_page_version(),
            'changes': [
                {
                    'removed_row': {
                        'id': row_id,
                    }
                }
            ],
        },
    )
    if not response.status_code == expected_status_code:
        raise ValueError(f'Unexpected status code {response.status_code}')


def remove_column(client, api_url, grid_tag, column_name, expected_status_code=200):
    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
    response = client.post(
        f'{api_url}/{grid_tag}/.grid/change',
        data={
            'version': grid.get_page_version(),
            'changes': [
                {
                    'removed_column': {
                        'name': column_name,
                    }
                }
            ],
        },
    )
    if not response.status_code == expected_status_code:
        raise ValueError(f'Unexpected status code {response.status_code}')


def get_grid(client, api_url, grid_tag):
    return client.get(f'{api_url}/{grid_tag}/.grid')
