import json

import pytest
from assertpy import assert_that

from django.core.urlresolvers import reverse

from staff.proposal.proposal_builder import ProposalBuilder
from staff.proposal.views import proposals_list


@pytest.mark.django_db
def test_view_is_working(client, mocked_mongo):
    view_url = reverse('proposal-api:proposals_list')
    response = client.get(view_url)
    assert response.status_code == 200


@pytest.mark.django_db
def test_view_gives_to_user_its_own_proposals(rf, mocked_mongo, company):
    # given
    request = rf.get(reverse('proposal-api:proposals_list'))
    request.user = company.persons['yandex-chief'].user

    proposal_id = (
        ProposalBuilder()
        .with_person(
            company.persons['dep11-person'].login,
            lambda person: person.staff_position('some'),
        )
        .build(author_login='yandex-chief')
    )

    # when
    response = proposals_list(request)

    # then
    assert response.status_code == 200
    result = json.loads(response.content)['result']
    assert len(result) == 1

    assert_that({
        'id': proposal_id,
        'author': company.persons['yandex-chief'].login,
    }).is_subset_of(result[0])
