import pytest

from staff.headcounts.headcounts_summary.related_entity_chains_builder import RelatedEntityChainsBuilder
from staff.headcounts.headcounts_summary.query_builder import (
    Aliases,
    HierarchyValues,
    QueryParams,
    RelatedEntity,
)


@pytest.mark.django_db
def test_chains_should_not_have_dep_ids_outside_of_filter(company):
    # given
    grouping = RelatedEntity.department

    builder = RelatedEntityChainsBuilder(
        QueryParams.from_default_args(
            groupings=[grouping],
            department_filters=[HierarchyValues(
                lft=company.dep1.lft,
                rght=company.dep1.rght,
                tree_id=company.dep1.tree_id,
                id=company.dep1.id,
            )]
        ),
        Aliases(),
    )

    # when
    result = builder.create_chains()

    # then
    assert len(result._chains[grouping]) == 5
    assert result.get_chain(grouping, company.dep1.id) == [company.dep1.id]
    assert result.get_chain(grouping, company.dep11.id) == [company.dep1.id, company.dep11.id]
    assert result.get_chain(grouping, company.dep12.id) == [company.dep1.id, company.dep12.id]
    assert result.get_chain(grouping, company.removed1.id) == [company.dep1.id, company.dep12.id, company.removed1.id]
    assert result.get_chain(grouping, company.dep111.id) == [company.dep1.id, company.dep11.id, company.dep111.id]
