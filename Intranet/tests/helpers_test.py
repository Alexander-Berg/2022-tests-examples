from staff.groups.helpers import get_group_by_id_or_404, get_group_by_url_or_404


def test_get_group__by_id_or_404_existing_group(test_data):
    group = get_group_by_id_or_404(id=test_data.first_lvl.id)

    assert group == test_data.first_lvl


def test_get_group__by_url_or_404_existing_group(test_data):
    group = get_group_by_url_or_404(url=test_data.first_lvl.url)

    assert group == test_data.first_lvl
