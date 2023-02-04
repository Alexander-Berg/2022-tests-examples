import pytest

from staff.gap.controllers.templates import TemplatesCtl
from staff.gap.tests.constants import TEMPLATES_MONGO_COLLECTION


@pytest.yield_fixture
def templates_ctl():
    ctl = TemplatesCtl()
    assert ctl.MONGO_COLLECTION == TEMPLATES_MONGO_COLLECTION

    yield ctl

    ctl.recreate_collection()


@pytest.mark.django_db
def test_find_one_strict(gap_test, templates_ctl):
    data = {
        'title': 'test title',
        'type': 'email',
        'workflows': ['absence', 'illness'],
        'tag': 'new_gap',
        'organization_ids': [1, 2],
        'city_ids': [3, 4],
        'langs': ['ru'],
        'template': 'test_template',
    }

    templates_ctl.new(**data)

    template = templates_ctl.find_one_strict(
        type='email',
        workflow='absence',
        tag='new_gap',
        organization_id=1,
        city_id=3,
        lang='ru',
    )
    assert template is not None
    del template['_id']
    assert template == data

    template = templates_ctl.find_one_strict(
        type='email',
        workflow='absence',
        tag='new_gap',
    )
    assert template is None


@pytest.mark.django_db
def test_find_one_not_strict(gap_test, templates_ctl):
    data = {
        'title': 'test title',
        'type': 'email',
        'workflows': ['absence', 'illness'],
        'tag': 'new_gap',
        'organization_ids': [1, 2],
        'city_ids': [3, 4],
        'langs': ['ru'],
        'template': 'test_template',
    }

    templates_ctl.new(**data)

    template = templates_ctl.find_one_not_strict(
        type='email',
        workflow='absence',
        tag='new_gap',
        organization_id=1,
        city_id=3,
        lang='ru',
    )
    assert template is not None
    del template['_id']
    assert template == data

    data = {
        'title': 'test title',
        'type': 'email',
        'workflows': ['absence', 'illness'],
        'tag': 'new_gap',
        'organization_ids': None,
        'city_ids': None,
        'langs': None,
        'template': 'test_template',
    }

    templates_ctl.new(**data)

    template = templates_ctl.find_one_not_strict(
        type='email',
        workflow='absence',
        tag='new_gap',
    )
    assert template is not None
    del template['_id']
    assert template == data
