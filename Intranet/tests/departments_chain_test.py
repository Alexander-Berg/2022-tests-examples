import pytest

from staff.lib.models.departments_chain import get_departments_tree


@pytest.mark.django_db
def test_for_root_department(company):
    # given
    fields = ('id', )
    looking_for_department_id = company.yandex.id

    # when
    result = get_departments_tree([looking_for_department_id], fields)

    # then
    assert result == {company.yandex.id: [{'id': company.yandex.id}]}


@pytest.mark.django_db
def test_for_deep_department(company):
    # given
    fields = ('id', )
    looking_for_department_id = company.dep111.id

    # when
    result = get_departments_tree([looking_for_department_id], fields)

    # then
    assert result == {company.dep111.id: [
        {'id': company.yandex.id},
        {'id': company.dep1.id},
        {'id': company.dep11.id},
        {'id': company.dep111.id},
    ]}


@pytest.mark.django_db
def test_for_several_departments(company):
    # given
    fields = ('id', )
    departments = [company.dep111.id, company.dep2.id, company.out2.id, company.dep11.id]

    # when
    result = get_departments_tree(departments, fields)

    # then
    assert result == {
        company.dep111.id: [
            {'id': company.yandex.id},
            {'id': company.dep1.id},
            {'id': company.dep11.id},
            {'id': company.dep111.id},
        ],
        company.dep2.id: [
            {'id': company.yandex.id},
            {'id': company.dep2.id},
        ],
        company.out2.id: [
            {'id': company.outstaff.id},
            {'id': company.out2.id},
        ],
        company.dep11.id: [
            {'id': company.yandex.id},
            {'id': company.dep1.id},
            {'id': company.dep11.id},
        ],
    }


@pytest.mark.django_db
def test_for_fields(company):
    # given
    fields = ('id', 'name', 'url')
    looking_for_department_id = company.dep111.id

    # when
    result = get_departments_tree([looking_for_department_id], fields)

    # then
    assert result == {company.dep111.id: [
        {'id': company.yandex.id, 'name': company.yandex.name, 'url': company.yandex.url},
        {'id': company.dep1.id, 'name': company.dep1.name, 'url': company.dep1.url},
        {'id': company.dep11.id, 'name': company.dep11.name, 'url': company.dep11.url},
        {'id': company.dep111.id, 'name': company.dep111.name, 'url': company.dep111.url},
    ]}
