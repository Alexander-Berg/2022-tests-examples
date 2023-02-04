from staff.groups.objects import GroupCtl, WikiGroupCtl


def test_has_descendants(test_data):
    assert WikiGroupCtl(group=test_data.first_lvl).has_descendants()


def test_has_not_descendants(test_data):
    assert not GroupCtl(group=test_data.third_lvl).has_descendants()
