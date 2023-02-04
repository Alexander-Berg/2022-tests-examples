from staff.groups.objects import WikiGroupCtl


def test_url_root_group(test_data):
    assert WikiGroupCtl(group=test_data.root).url == '__wiki__'


def test_url_first_lvl_group(test_data):
    assert WikiGroupCtl(group=test_data.first_lvl).url == 'category_first'


def test_url_second_lvl_group(test_data):
    assert WikiGroupCtl(group=test_data.second_lvl).url == 'second'


def test_url_third_lvl_group(test_data):
    assert WikiGroupCtl(group=test_data.third_lvl).url == 'second_third'


def test_update_root_group(test_data):
    cleaned_data = {
        'name': 'root',
        'code': 'rt',
        'description': 'This is root group',
        'position': 1,
    }

    root = WikiGroupCtl(group=test_data.root).update(data=cleaned_data)

    assert root.name == 'root'
    assert root.code == 'rt'
    assert root.description == 'This is root group'
    assert root.position == 1
