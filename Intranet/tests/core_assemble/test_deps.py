# coding: utf-8
from review.core import const
from review.core.logic.assemble import deps

FIELDS = const.FIELDS
FILTERS = const.FILTERS
FETCHERS = const.FETCHERS
D = deps.D
S = deps.Step.typed


class DEPS_SMALL(deps.DEPS):
    FIELDS_DEPS = {
        FIELDS.ROLES: (
            FETCHERS.COLLECT,
        ),
        FIELDS.PERMISSIONS_WRITE: (
            FIELDS.ROLES,
        ),

        # person_review table fields
        FIELDS.STATUS: (
            D(FETCHERS.COLLECT, db_fields='status'),
        ),
        FIELDS.APPROVE_LEVEL: (
            D(FETCHERS.COLLECT, db_fields='approve_level'),
        ),
        # review table fields
        FIELDS.REVIEW_STATUS: (
            D(FETCHERS.REVIEWS, db_fields='status'),
        ),

        # complex fields
        FIELDS.ACTION_AT: (
            FIELDS.STATUS,
            FIELDS.APPROVE_LEVEL,
            FIELDS.REVIEWERS,
        ),
        FIELDS.REVIEWERS: (
            FETCHERS.REVIEWERS,
        ),
        FIELDS.ACTION_ALLOW_ANNOUNCE: (
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
    }

    FILTERS_DEPS = {
        FILTERS.MARK: FIELDS.MARK,
        FILTERS.ACTION_AT: FIELDS.ACTION_AT,
    }


class UNORDERED(set):
    pass


def assert_steps_equal(expected, two):
    assert type(expected) == type(two)
    index_two = 0
    for item in expected:
        if isinstance(item, UNORDERED):
            left = set(item)
            right = set(two[index_two:index_two + len(item)])
            assert left == right, (index_two, left, right)
            index_two += len(item)
        else:
            assert item == two[index_two], (item, two[index_two])
            index_two += 1

    assert not two[index_two:]


def test_state_init():
    state = deps.State(
        deps=DEPS_SMALL,
        fields=[FIELDS.ACTION_ALLOW_ANNOUNCE],
        filters=[FILTERS.MARK],
    )

    assert state.filters_deps == {
        FILTERS.MARK: {
            D(FIELDS.MARK),
        }
    }
    assert state.fields_deps.keys() == {
        FIELDS.ACTION_ALLOW_ANNOUNCE,
        FIELDS.STATUS,
        FIELDS.REVIEW_STATUS,
        FIELDS.ACTION_AT,
        FIELDS.APPROVE_LEVEL,
        FIELDS.REVIEWERS,
        FIELDS.MARK,
    }
    assert state.fields_deps[FIELDS.ACTION_ALLOW_ANNOUNCE] == {
        D(FIELDS.STATUS),
        D(FIELDS.REVIEW_STATUS),
        D(FIELDS.ACTION_AT),
    }
    assert state.fields_deps[FIELDS.STATUS] == {
        D(FETCHERS.COLLECT, db_fields='status'),
    }
    assert state.fields_deps[FIELDS.REVIEW_STATUS] == {
        D(FETCHERS.REVIEWS, db_fields='status'),
    }
    assert state.fields_deps[FIELDS.ACTION_AT] == {
        D(FIELDS.STATUS),
        D(FIELDS.APPROVE_LEVEL),
        D(FIELDS.REVIEWERS),
    }


def test_collect_params():
    steps = deps.Steps([
        S(FETCHERS.COLLECT, params={'db_fields': 'a'}),
        S(FILTERS.PERSONS, value='volozh'),
        S(FILTERS.IDS, value=(1, 2)),
        S(FIELDS.ACTION_AT),
    ])

    assert steps.collect_step_params == {'db_fields': 'a'}


def test_collect_filters():
    steps = deps.Steps([
        S(FETCHERS.COLLECT, params={'db_fields': 'a'}),
        S(FILTERS.PERSONS, value='volozh'),
        S(FILTERS.IDS, value=(1, 2)),
        S(FIELDS.ACTION_AT),
    ])

    assert steps.collect_step_filters == {
        FILTERS.PERSONS: 'volozh',
        FILTERS.IDS: (1, 2),
    }


def test_non_collect_steps():
    steps = deps.Steps([
        S(FETCHERS.COLLECT, params={'db_fields': ('a',)}),
        S(FILTERS.PERSONS, value='volozh'),
        S(FILTERS.IDS, value=(1, 2)),
        S(FIELDS.ACTION_AT),
        S(FETCHERS.GOODIES, params={'db_fields': ('b',)}),
    ])

    assert steps.non_collect == [
        S(FIELDS.ACTION_AT),
        S(FETCHERS.GOODIES, params={'db_fields': ('b',)}),
    ]


def test_steps_order():
    fields = (
        FIELDS.ACTION_ALLOW_ANNOUNCE,
    )
    filters = {}
    expected_steps = deps.Steps([
        S(FETCHERS.COLLECT, params={'db_fields': ('approve_level', 'status')}),
        UNORDERED([
            S(FIELDS.STATUS),
            S(FIELDS.APPROVE_LEVEL),
        ]),
        S(FETCHERS.REVIEWS, params={'db_fields': ('status',)}),
        S(FIELDS.REVIEW_STATUS),
        S(FETCHERS.REVIEWERS),
        S(FIELDS.REVIEWERS),
        S(FIELDS.ACTION_AT),
        S(FIELDS.ACTION_ALLOW_ANNOUNCE),
    ])

    steps = deps.get_steps(fields, filters, deps=DEPS_SMALL)

    assert_steps_equal(expected_steps, steps)
