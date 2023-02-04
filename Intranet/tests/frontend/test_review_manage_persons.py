import datetime
import mock
import pytest
from django.conf import settings
from django.db.models import Count

from review.core import const, models
from review.core.models import (
    PersonReview,
    PersonReviewChange,
    PersonReviewRole,
)
from review.staff.models import (
    HR,
    Person,
    PersonHeads,
)
from tests.helpers import post_json, post_multipart_data, get_json

pytestmark = pytest.mark.usefixtures("test_person_as_review_creator")


@pytest.fixture(autouse=True)
def mock_freeze_gradient_data():
    with mock.patch('review.core.tasks.freeze_gradient_data_task.delay') as mocked_task:
        yield mocked_task


def test_review_person_add(
    client,
    review_role_admin,
    person_builder_bulk,
    department_root_builder,
    staff_structure_builder
):
    departments = [department_root_builder() for _ in range(5)]
    persons = []
    for dep in departments:
        persons.extend(person_builder_bulk(department=dep, _count=20))
    structure_change = [staff_structure_builder(persons=persons) for _ in range(2)]
    request = {
        'data': [
            {
                'type': 'department',
                'slug': departments[0].slug,
                'structure_change': structure_change[0]['structure_change'].id
            },
            {
                'type': 'department',
                'slug': departments[1].slug,
                'structure_change': structure_change[0]['structure_change'].id
            },
            {
                'type': 'department',
                'slug': departments[2].slug,
                'structure_change': structure_change[0]['structure_change'].id
            },
            {
                'type': 'department',
                'slug': departments[3].slug,
                'structure_change': structure_change[1]['structure_change'].id
            },
            {
                'type': 'person',
                'login': persons[0].login,
                'structure_change': structure_change[0]['structure_change'].id
            },
            {
                'type': 'person',
                'login': persons[10].login,
                'structure_change': structure_change[1]['structure_change'].id
            },
            {
                'type': 'person',
                'login': persons[33].login,
                'structure_change': structure_change[1]['structure_change'].id
            },
        ]
    }
    review = review_role_admin.review

    post_json(
        client=client,
        path='/frontend/reviews/{}/add-persons/'.format(review.id),
        request=request,
        login=review_role_admin.person.login,
    )

    created_reviews = PersonReview.objects.filter(review=review)
    assert len(created_reviews) > 0
    assert all(pr.person in persons for pr in created_reviews)
    created_roles = PersonReviewRole.objects.filter(
        person_review__in=created_reviews
    ).values("person_review").annotate(cnt=Count("person_review"))
    assert all(role['cnt'] == 5 for role in created_roles)


@pytest.mark.parametrize('is_available', (True, False))
def test_review_person_add_by_accompanying_hr(
    client,
    review,
    hr_builder,
    department_root,
    person_builder,
    review_role_builder,
    staff_structure_builder,
    is_available,
):
    review.status = const.REVIEW_STATUS.DRAFT
    review.save()
    person = person_builder(department=department_root)
    structure_change = staff_structure_builder(persons=[person])
    hr = review_role_builder(review=review, type=const.ROLE.REVIEW.ACCOMPANYING_HR).person
    if is_available:
        hr_builder(cared_person=person, hr_person=hr, type=const.ROLE.DEPARTMENT.HR_PARTNER)

    post_json(
        client=client,
        path='/frontend/reviews/{}/add-persons/'.format(review.id),
        login=hr.login,
        request=dict(data=[dict(
            type='department',
            slug=department_root.slug,
            structure_change=structure_change['structure_change'].id,
        )]),
    )

    assert models.PersonReview.objects.filter(review=review, person=person).exists() == is_available
    response = get_json(
        client,
        login=hr.login,
        path='/frontend/reviews/{}/person-reviews/'.format(review.id),
    )
    assert bool(response['person_reviews']) == is_available


@pytest.mark.parametrize('is_available', (True, False))
def test_review_person_delete_by_accompanying_hr(
    client,
    review,
    hr_builder,
    department_root,
    person_review_builder,
    person_builder,
    review_role_builder,
    staff_structure_builder,
    is_available,
):
    person = person_builder()
    person_review = person_review_builder(person=person, review=review)
    hr = review_role_builder(review=review, type=const.ROLE.REVIEW.ACCOMPANYING_HR).person
    if is_available:
        hr_builder(cared_person=person, hr_person=hr)

    post_json(
        client=client,
        path='/frontend/reviews/{}/delete-persons/'.format(review.id),
        login=hr.login,
        request=dict(person_reviews=[person_review.id]),
    )

    assert models.PersonReview.objects.filter(review=review, person=person).exists() != is_available


def _get_add_params_as_dep(person, structure_change):
    return dict(data=[dict(
        type='department',
        slug=person.department.slug,
        structure_change=structure_change['structure_change'].id,
    )])


def _get_add_params_as_person(person, structure_change):
    return dict(data=[dict(
        type='person',
        login=person.login,
        structure_change=structure_change['structure_change'].id,
    )])


@pytest.mark.parametrize('get_request_params', [_get_add_params_as_dep, _get_add_params_as_person])
def test_add_dismissed_after_review_include_dismissed(
    client,
    department_root,
    person_builder,
    review_builder,
    review_role_builder,
    staff_structure_builder,
    get_request_params,
):
    person = person_builder(
        department=department_root,
        quit_at=datetime.date(2017, 6, 6),
        is_dismissed=True,
    )
    staff_structure_builder(
        persons=[person],
        date=datetime.date(2017, 6, 4),
    )
    staff_structure_builder(
        date=datetime.date(2017, 6, 5),
        heads_chain=[],
    )
    ssc_without_person = staff_structure_builder(
        date=datetime.date(2017, 6, 7),
    )
    review = review_builder(
        include_dismissed_after_date=datetime.date(2017, 1, 6),
    )
    admin = review_role_builder(
        type=const.ROLE.REVIEW.ADMIN,
        review=review,
    ).person

    post_json(
        client=client,
        path='/frontend/reviews/{}/add-persons/'.format(review.id),
        login=admin.login,
        request=get_request_params(person, ssc_without_person),
    )
    person_heads = PersonHeads.objects.get(person_id=person.id)
    person_heads_ids = set(map(int, person_heads.heads.split(',')))
    reviewers_ids = PersonReviewRole.objects.filter(
        person_review__review=review,
        person_review__person=person,
        type__in=(
            const.ROLE.PERSON_REVIEW.REVIEWER,
            const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        ),
    ).values_list('person_id', flat=True)
    assert set(reviewers_ids) == person_heads_ids


@pytest.mark.parametrize('get_request_params,is_added', [
    (_get_add_params_as_dep, False),
    (_get_add_params_as_person, True),
])
def test_add_dismissed_before_review_include_dismissed(
    client,
    department_root,
    person_builder,
    review_builder,
    review_role_builder,
    staff_structure_builder,
    get_request_params,
    is_added,
):
    person = person_builder(
        department=department_root,
        quit_at=datetime.date(2017, 6, 6),
        is_dismissed=True,
    )
    ssc = staff_structure_builder(
        persons=[person],
        date=datetime.date(2017, 6, 5),
    )
    review = review_builder(
        include_dismissed_after_date=datetime.date(2017, 10, 6),
    )
    admin = review_role_builder(
        type=const.ROLE.REVIEW.ADMIN,
        review=review,
    ).person
    post_json(
        client=client,
        path='/frontend/reviews/{}/add-persons/'.format(review.id),
        login=admin.login,
        request=get_request_params(person, ssc),
    )
    assert PersonReview.objects.filter(person=person, review=review).exists() == is_added


@pytest.mark.parametrize('get_request_params,is_added', [
    (_get_add_params_as_dep, False),
    (_get_add_params_as_person, True),
])
def test_add_dismissed_without_review_include_dismissed(
    client,
    department_root,
    person_builder,
    review_builder,
    review_role_builder,
    staff_structure_builder,
    get_request_params,
    is_added,
):
    person = person_builder(
        department=department_root,
        quit_at=datetime.date(2017, 6, 6),
        is_dismissed=True,
    )
    ssc = staff_structure_builder(
        persons=[person],
        date=datetime.date(2017, 6, 5),
    )
    review = review_builder()
    admin = review_role_builder(
        type=const.ROLE.REVIEW.ADMIN,
        review=review,
    ).person
    post_json(
        client=client,
        path='/frontend/reviews/{}/add-persons/'.format(review.id),
        login=admin.login,
        request=get_request_params(person, ssc),
    )
    assert PersonReview.objects.filter(person=person, review=review).exists() == is_added


def test_review_person_add_no_department(review, client):
    request = {
        'data': [
            {
                'type': 'department',
                'slug': 'very_unknown_department',
                'structure_change': 1
            },
        ]
    }
    with pytest.raises(AssertionError):
        post_json(client, '/frontend/reviews/{}/add-persons/'.format(review.id), request)


@pytest.mark.parametrize('role_const', [const.ROLE.REVIEW.SUPERREVIEWER, const.ROLE.REVIEW.ADMIN])
def test_review_person_delete(
    role_const,
    review_role_builder,
    review_role_admin,
    client,
    staff_structure_builder,
):
    review = review_role_admin.review
    staff_structure = staff_structure_builder()

    create_review_persons(
        client=client,
        subject=review_role_admin.person,
        staff_structure=staff_structure,
        review=review,
    )

    person_reviews = [
        p['id'] for
        p in PersonReview.objects.filter(review=review).values("id")
    ]
    reviews_to_del = person_reviews[:10]
    request = {'person_reviews': reviews_to_del}

    checking_role = review_role_builder(type=role_const, review=review)
    post_multipart_data(
        client=client,
        path='/frontend/reviews/{}/delete-persons/'.format(review.id),
        request=request,
        login=checking_role.person.login,
    )

    new_person_reviews = [p['id'] for p in PersonReview.objects.filter(review=review).values("id")]
    assert all(p not in new_person_reviews for p in person_reviews[:10])
    assert all(p in new_person_reviews for p in person_reviews[10:])


@pytest.fixture
def review_with_approvers(
    review_role_builder,
    review_role_admin,
    client,
    person_builder,
    person_review_role_builder,
    staff_structure_builder,
):
    review = review_role_admin.review
    review.status = const.REVIEW_STATUS.ARCHIVE
    review.save()

    staff_structure = staff_structure_builder()
    create_review_persons(
        client=client,
        subject=review_role_admin.person,
        staff_structure=staff_structure,
        review=review,
    )

    approvers = [
        person_builder(login='approver1'),
        person_builder(login='approver2'),
        person_builder(login='approver3'),
    ]

    person_review_statuses = list(const.PERSON_REVIEW_STATUS.ALL)
    for i, pr in enumerate(PersonReview.objects.filter(review=review)):
        pr.status = person_review_statuses[i % len(person_review_statuses)]
        pr.approve_level = i % 3
        pr.save()

    return review


def get_next_approver_logins(person_review):
    approver_logins = list(
        person_review.roles
        .filter(
            position=person_review.approve_level,
            type__in=[
                const.ROLE.PERSON_REVIEW.REVIEWER,
                const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
            ]
        )
        .order_by('position')
        .values_list('person__login', flat=True)
    )
    return approver_logins


@pytest.mark.parametrize('add_type', const.PERSON_REVIEW_ACTIONS.ADD_POSITION_CHOICE)
def test_review_chain_add_approver(
    add_type,
    review_with_approvers,
    client,
    person_builder,
):
    review = review_with_approvers
    new_approver_login = 'approverX'
    new_approver = person_builder(login=new_approver_login)

    review_admin = review.roles.get(type=const.ROLE.REVIEW.ADMIN).person

    for pr in review.person_reviews.all():
        next_approver_logins = get_next_approver_logins(pr)
        third_reviewer_person = pr.roles.order_by('position')[2].person

        request = {
            'ids': [pr.id],
            'reviewers': {
                'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_ADD,
                'position': add_type,
                'person': new_approver.login,
                'person_to': third_reviewer_person.login,
            }
        }
        if add_type in (
            const.PERSON_REVIEW_ACTIONS.ADD_POSITION_END,
            const.PERSON_REVIEW_ACTIONS.ADD_POSITION_START
        ):
            del request['reviewers']['person_to']

        with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
            result = post_json(
                client=client,
                login=review_admin.login,
                path='/frontend/person-reviews/bulk/actions/',
                request=request,
            )
        assert 'errors' not in result
        pr.refresh_from_db()

        if (pr.status == const.PERSON_REVIEW_STATUS.APPROVED and
                add_type == const.PERSON_REVIEW_ACTIONS.ADD_POSITION_END):
            assert set(get_next_approver_logins(pr)) == {new_approver_login}
        elif pr.approve_level == 2 and add_type == const.PERSON_REVIEW_ACTIONS.ADD_POSITION_BEFORE:
            assert set(get_next_approver_logins(pr)) == {new_approver_login}
        elif pr.approve_level == 0 and add_type == const.PERSON_REVIEW_ACTIONS.ADD_POSITION_START:
            assert set(get_next_approver_logins(pr)) == {new_approver_login}
        else:
            assert set(next_approver_logins) == set(get_next_approver_logins(pr)) - {new_approver_login}


def test_review_add_persons_duplicate(
    review_role_admin,
    client,
    staff_structure_builder,
):
    review = review_role_admin.review
    staff_structure = staff_structure_builder()
    create_review_persons(
        client,
        review_role_admin.person,
        staff_structure,
        review,
    )
    persons = staff_structure['persons']
    person_reviews = PersonReview.objects.filter(
        person__in=persons
    )
    heads = {h.id for h in staff_structure['heads_chain']}
    assert all([
        heads == {v[0] for v in pr.roles.values_list('person_id')}
        for pr in person_reviews
    ])
    changes_before = models.PersonReviewChange.objects.count()
    staff_structure = staff_structure_builder(
        persons=persons,
        department=staff_structure['department'],
    )
    create_review_persons(
        client,
        review_role_admin.person,
        staff_structure,
        review,
    )
    person_reviews = PersonReview.objects.filter(
        person__in=persons
    )
    heads = {h.id for h in staff_structure['heads_chain']}
    assert all([
        heads == {v[0] for v in pr.roles.values_list('person_id')}
        for pr in person_reviews
    ])
    changes_after = models.PersonReviewChange.objects.all()
    assert changes_after.count() == changes_before + len(person_reviews)
    assert all('reviewers' in change.diff for change in changes_after)


@pytest.mark.parametrize('role_const', [const.ROLE.REVIEW.SUPERREVIEWER, const.ROLE.REVIEW.ADMIN])
@pytest.mark.parametrize('is_multi', (True, False))
def test_review_chain_delete(
    role_const,
    review_role_admin,
    client,
    review_role_builder,
    staff_structure_builder,
    is_multi,
):
    staff_structure = staff_structure_builder()
    review = review_role_admin.review
    create_review_persons(client, review_role_admin.person, staff_structure, review)
    review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=Person.objects.filter(login=settings.YAUTH_DEV_USER_LOGIN).first()
    )
    person_reviews = [p['id'] for p in PersonReview.objects.filter(review=review).values("id")]
    other_person = PersonReviewRole.objects.filter(
        person_review_id__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER
    ).first().person

    request = {
        'ids': person_reviews,
        'reviewers': {
            'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_DELETE,
        }
    }

    if is_multi:
        request['reviewers']['persons'] = [other_person.login]
    else:
        request['reviewers']['person'] = other_person.login

    checking_role = review_role_builder(type=role_const, review=review)
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        post_json(
            client=client,
            path='/frontend/person-reviews/bulk/actions/',
            request=request,
            login=checking_role.person.login,
        )
    roles = PersonReviewRole.objects.filter(
        person_review_id__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER
    ).values('person')
    assert roles.count() == len(person_reviews)
    assert all([r['person'] != other_person.id for r in roles])


@pytest.mark.parametrize('role_const', [const.ROLE.REVIEW.SUPERREVIEWER, const.ROLE.REVIEW.ADMIN])
def test_review_chain_replace(
    role_const,
    review_role_admin,
    client,
    person_builder,
    review_role_builder,
    staff_structure_builder,
):
    review = review_role_admin.review
    staff_structure = staff_structure_builder()
    create_review_persons(client, review_role_admin.person, staff_structure, review)
    new_head = person_builder()
    other_person = person_builder()
    review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=Person.objects.filter(login=settings.YAUTH_DEV_USER_LOGIN).first()
    )
    person_reviews = [p['id'] for p in PersonReview.objects.filter(review=review).values("id")]
    PersonReviewRole.objects.filter(
        person_review_id__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER
    ).update(person=new_head)
    request = {
        'ids': person_reviews,
        'reviewers': {
            'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_REPLACE,
            'person': new_head.login,
            'person_to': other_person.login,
        }
    }
    checking_role = review_role_builder(type=role_const, review=review)
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        post_json(
            client,
            '/frontend/person-reviews/bulk/actions/',
            request,
            login=checking_role.person.login,
        )
    roles = PersonReviewRole.objects.filter(
        person_review_id__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER
    ).values('person')
    assert roles.count() == len(person_reviews)
    assert all(r['person'] == other_person.id for r in roles)


@pytest.mark.parametrize('role_type', [const.ROLE.REVIEW.SUPERREVIEWER, const.ROLE.REVIEW.ADMIN])
def test_review_chain_replace_same(
    client,
    person_review_builder,
    person_review_role_builder,
    review_role_builder,
    role_type,
):
    checking_role = review_role_builder(type=role_type)
    review = checking_role.review
    person_review = person_review_builder(review=review)
    reviewer = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    ).person
    request = {
        'ids': [person_review.id],
        'reviewers': {
            'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_REPLACE,
            'person': reviewer.login,
            'person_to': reviewer.login,
        }
    }
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        post_json(
            client,
            '/frontend/person-reviews/bulk/actions/',
            request,
            login=checking_role.person.login,
        )
    changes = PersonReviewChange.objects.filter(person_review=person_review)
    reviewers_changes = (
        it[const.FIELDS.REVIEWERS] for it in changes
        if const.FIELDS.REVIEWERS in it.diff
    )
    assert all(it['old'] != it['new'] for it in reviewers_changes)


def create_review_persons(client, subject, staff_structure, review):
    request = {
        'data': [
            {
                'type': 'department',
                'slug': staff_structure['department'].slug,
                'structure_change': staff_structure['structure_change'].id
            }
        ]
    }
    post_json(
        client=client,
        path='/frontend/reviews/{}/add-persons/'.format(review.id),
        request=request,
        login=subject.login,
    )
