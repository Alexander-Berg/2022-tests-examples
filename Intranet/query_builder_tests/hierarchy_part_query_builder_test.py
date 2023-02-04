import pytest

from django.db import connections, router

from staff.departments.models import Department

from staff.headcounts.headcounts_summary.query_builder import HierarchyValues
from staff.headcounts.headcounts_summary.query_builder.hierarchy_part_query_builder import HierarchyPartQueryBuilder


@pytest.mark.django_db()
def test_hierarchy_part_query_builder(company):
    # given
    hierarchy_filter = HierarchyValues(
        tree_id=company.dep1.tree_id,
        lft=company.dep1.lft,
        rght=company.dep1.rght,
        id=company.dep1.id,
    )
    query = HierarchyPartQueryBuilder([hierarchy_filter]).build()
    cursor = connections[router.db_for_read(Department)].cursor()

    # when
    cursor.execute(query, tuple())

    # then
    result = sorted(list(cursor.fetchall()))
    assert result == sorted([
        (company.dep1.id,),
        (company.dep11.id,),
        (company.dep12.id,),
        (company.removed1.id,),
        (company.dep111.id,),
    ])


@pytest.mark.django_db()
def test_hierarchy_part_query_builder_uses_or_in_filters(company):
    # given
    hierarchy_filter = [
        HierarchyValues(
            tree_id=company.dep1.tree_id,
            lft=company.dep1.lft,
            rght=company.dep1.rght,
            id=company.dep1.id,
        ),
        HierarchyValues(
            tree_id=company.dep2.tree_id,
            lft=company.dep2.lft,
            rght=company.dep2.rght,
            id=company.dep2.id,
        ),
    ]
    query = HierarchyPartQueryBuilder(hierarchy_filter).build()
    cursor = connections[router.db_for_read(Department)].cursor()

    # when
    cursor.execute(query, tuple())

    # then
    result = sorted(list(cursor.fetchall()))
    assert result == sorted([
        (company.dep1.id,),
        (company.dep11.id,),
        (company.dep12.id,),
        (company.removed1.id,),
        (company.dep111.id,),
        (company.dep2.id,),
    ])
