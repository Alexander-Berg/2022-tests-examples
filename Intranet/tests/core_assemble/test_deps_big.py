# coding: utf-8


from review.core import const
from review.core.logic.assemble import deps

from .test_deps import UNORDERED, assert_steps_equal


FIELDS = const.FIELDS
FILTERS = const.FILTERS
FETCHERS = const.FETCHERS
D = deps.D
S = deps.Step.typed


class DEPS(deps.DEPS):

    FIELDS_DEPS = {
        # subject fields
        FIELDS.ROLES: (
            FETCHERS.COLLECT,
        ),
        FIELDS.MODIFIERS: (
            FIELDS.ROLES,
            D(FETCHERS.REVIEWS, db_fields=list(const.REVIEW_MODE.FIELDS_TO_MODIFIERS.values()))
        ),
        FIELDS.PERMISSIONS_WRITE: (
            FIELDS.ROLES,
        ),
        FIELDS.SUBORDINATION: (
            FETCHERS.SUBORDINATION,
        ),

        # безусловно ставятся, не нужно от них зависеть
        FIELDS.ID: (
            D(FETCHERS.COLLECT, db_fields='id'),
        ),
        FIELDS.PERSON_ID: (
            D(FETCHERS.COLLECT, db_fields='person_id'),
        ),
        FIELDS.REVIEW_ID: (
            D(FETCHERS.COLLECT, db_fields='review_id'),
        ),

        # collect
        FIELDS.STATUS: (
            FIELDS.PERMISSIONS_WRITE,
            D(FETCHERS.COLLECT, db_fields='status'),
        ),
        FIELDS.MARK: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.MODIFIERS,
            D(FETCHERS.COLLECT, db_fields='mark'),
        ),
        FIELDS.GOLDSTAR: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.MODIFIERS,
            D(FETCHERS.COLLECT, db_fields='goldstar'),
        ),
        FIELDS.LEVEL_CHANGE: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.MODIFIERS,
            D(FETCHERS.COLLECT, db_fields='level_change'),
        ),
        FIELDS.SALARY_CHANGE: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.MODIFIERS,
            D(FETCHERS.COLLECT, db_fields='salary_change'),
        ),
        FIELDS.BONUS: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.MODIFIERS,
            D(FETCHERS.COLLECT, db_fields='bonus'),
        ),
        FIELDS.OPTIONS_RSU: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.MODIFIERS,
            D(FETCHERS.COLLECT, db_fields='options_rsu'),
        ),
        FIELDS.FLAGGED: (
            FIELDS.PERMISSIONS_WRITE,
            D(FETCHERS.COLLECT, db_fields='flagged'),
        ),
        FIELDS.APPROVE_LEVEL: (
            FIELDS.PERMISSIONS_WRITE,
            D(FETCHERS.COLLECT, db_fields='approve_level'),
        ),

        # person data
        FIELDS.PERSON_LOGIN: (
            D(FETCHERS.PERSONS, db_fields='login'),
        ),
        FIELDS.PERSON_FIRST_NAME: (
            D(FETCHERS.PERSONS, db_fields='first_name'),
        ),
        FIELDS.PERSON_LAST_NAME: (
            D(FETCHERS.PERSONS, db_fields='last_name'),
        ),
        FIELDS.PERSON_IS_DISMISSED: (
            D(FETCHERS.PERSONS, db_fields='is_dismissed'),
        ),
        FIELDS.PERSON_JOIN_AT: (
            D(FETCHERS.PERSONS, db_fields='join_at'),
        ),
        FIELDS.PERSON_POSITION: (
            D(FETCHERS.PERSONS, db_fields='position'),
        ),
        FIELDS.PERSON_DEPARTMENT_ID: (
            D(FETCHERS.PERSONS, db_fields='department__id'),
        ),
        FIELDS.PERSON_DEPARTMENT_SLUG: (
            D(FETCHERS.PERSONS, db_fields='department__slug'),
        ),
        FIELDS.PERSON_DEPARTMENT_NAME: (
            D(FETCHERS.PERSONS, db_fields='department__name'),
        ),
        FIELDS.PERSON_DEPARTMENT_PATH: (
            D(FETCHERS.PERSONS, db_fields='department__path'),
        ),

        # finance data
        FIELDS.LEVEL: (
            FIELDS.REVIEW_START_DATE,
            D(FETCHERS.PERSONS, db_fields='finance__grade_history'),
        ),
        FIELDS.PROFESSION: (
            FIELDS.REVIEW_START_DATE,
            D(FETCHERS.PERSONS, db_fields='finance__grade_history'),
        ),
        FIELDS.SALARY_VALUE: (
            FIELDS.REVIEW_START_DATE,
            D(FETCHERS.PERSONS, db_fields='finance__salary_history'),
        ),
        FIELDS.SALARY_CURRENCY: (
            FIELDS.REVIEW_START_DATE,
            D(FETCHERS.PERSONS, db_fields='finance__salary_history'),
        ),

        # reviews
        FIELDS.REVIEW_NAME: (
            D(FETCHERS.REVIEWS, db_fields='name'),
        ),
        FIELDS.REVIEW_TYPE: (
            D(FETCHERS.REVIEWS, db_fields='type'),
        ),
        FIELDS.REVIEW_STATUS: (
            D(FETCHERS.REVIEWS, db_fields='status'),
        ),
        FIELDS.REVIEW_START_DATE: (
            D(FETCHERS.REVIEWS, db_fields='start_date'),
        ),
        FIELDS.REVIEW_FINISH_DATE: (
            D(FETCHERS.REVIEWS, db_fields='finish_date'),
        ),
        FIELDS.REVIEW_EVALUATION_FROM_DATE: (
            D(FETCHERS.REVIEWS, db_fields='evaluation_from_date'),
        ),
        FIELDS.REVIEW_EVALUATION_TO_DATE: (
            D(FETCHERS.REVIEWS, db_fields='evaluation_to_date'),
        ),

        # complex
        FIELDS.SALARY_CHANGE_IS_DIFFER_FROM_DEFAULT: (
            FIELDS.LEVEL,
            FIELDS.MARK,
            FIELDS.GOLDSTAR,
            FIELDS.SALARY_CHANGE,
            D(FETCHERS.GOODIES, db_fields='salary_change_history'),
        ),
        FIELDS.BONUS_IS_DIFFER_FROM_DEFAULT: (
            FIELDS.LEVEL,
            FIELDS.MARK,
            FIELDS.GOLDSTAR,
            FIELDS.BONUS,
            D(FETCHERS.GOODIES, db_fields='bonus_history'),
        ),
        FIELDS.OPTIONS_RSU_IS_DIFFER_FROM_DEFAULT: (
            FIELDS.LEVEL,
            FIELDS.MARK,
            FIELDS.GOLDSTAR,
            FIELDS.OPTIONS_RSU,
            D(FETCHERS.GOODIES, db_fields='options_rsu_history'),
        ),
        FIELDS.ACTION_AT: (
            FIELDS.STATUS,
            FIELDS.APPROVE_LEVEL,
            FIELDS.REVIEWERS,
        ),
        FIELDS.REVIEWERS: (
            FETCHERS.REVIEWERS,
        ),
        FIELDS.MARK_LEVEL_HISTORY: (
            D(FETCHERS.PERSON_REVIEW_HISTORY, db_fields='type'),
            D(FETCHERS.REVIEWS, db_fields=('type', 'start_date')),
        ),

        # actions
        FIELDS.ACTION_MARK: (
            FIELDS.MODIFIERS,
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_GOLDSTAR: (
            FIELDS.MODIFIERS,
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_LEVEL_CHANGE: (
            FIELDS.MODIFIERS,
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_SALARY_CHANGE: (
            FIELDS.MODIFIERS,
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_BONUS: (
            FIELDS.MODIFIERS,
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_OPTIONS_RSU: (
            FIELDS.MODIFIERS,
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_FLAGGED: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_FLAGGED_POSITIVE: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_APPROVE: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_ALLOW_ANNOUNCE: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_ANNOUNCE: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_COMMENT: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
        FIELDS.ACTION_REVIEWERS: (
            FIELDS.PERMISSIONS_WRITE,
            FIELDS.STATUS,
            FIELDS.REVIEW_STATUS,
            FIELDS.ACTION_AT,
        ),
    }

    FILTERS_DEPS = {
        FILTERS.IDS: None,
        FILTERS.REVIEWS: None,
        FILTERS.REVIEW_ACTIVITY: None,
        FILTERS.PERSONS: None,
        FILTERS.REVIEW_STATUSES: None,
        FILTERS.MARK: FIELDS.MARK,
        FILTERS.GOLDSTAR: FIELDS.GOLDSTAR,
        FILTERS.LEVEL_CHANGE: FIELDS.LEVEL_CHANGE,
        FILTERS.SALARY_CHANGE: FIELDS.SALARY_CHANGE,
        FILTERS.BONUS: FIELDS.BONUS,
        FILTERS.OPTIONS_RSU: FIELDS.OPTIONS_RSU,
        FILTERS.DEPARTMENTS: FIELDS.PERSON_DEPARTMENT_PATH,
        FILTERS.PROFESSION: FIELDS.PROFESSION,
        FILTERS.LEVEL: FIELDS.LEVEL,
        FILTERS.SALARY_VALUE: FIELDS.SALARY_VALUE,
        FILTERS.SALARY_CURRENCY: FIELDS.SALARY_CURRENCY,
        FILTERS.PERSON_JOIN_AT: FIELDS.PERSON_JOIN_AT,
        FILTERS.STATUS: FIELDS.STATUS,
        FILTERS.FLAGGED: FIELDS.FLAGGED,
        FILTERS.ACTION_AT: FIELDS.ACTION_AT,
        FILTERS.IS_DIFFER_FROM_DEFAULT: (
            FIELDS.BONUS_IS_DIFFER_FROM_DEFAULT,
            FIELDS.SALARY_CHANGE_IS_DIFFER_FROM_DEFAULT,
            FIELDS.OPTIONS_RSU_IS_DIFFER_FROM_DEFAULT,
        ),
        FILTERS.SUBORDINATION: FIELDS.SUBORDINATION,
    }

    FETCHERS_ORDER = (
        FETCHERS.COLLECT,
        FETCHERS.REVIEWS,
        FETCHERS.GOODIES,
        FETCHERS.SUBORDINATION,
        FETCHERS.REVIEWERS,
        FETCHERS.PERSONS,
        FETCHERS.PERSON_REVIEW_HISTORY,
    )


WHATEVER = 'WHATEVER'


def test_all_fields_and_filters():
    used_fields = {
        field
        for fields in list(DEPS.FIELDS_DEPS.values())
        for field in fields
        if field in FIELDS.ALL
    }
    for field in list(DEPS.FIELDS_DEPS.keys()):
        if field in FIELDS.ALL:
            used_fields.add(field)
    for fields in list(DEPS.FILTERS_DEPS.values()):
        if fields is not None:
            if isinstance(fields, tuple):
                for field in fields:
                    if field in FIELDS.ALL:
                        used_fields.add(field)
            elif fields in FIELDS.ALL:
                used_fields.add(fields)
    for field in list(DEPS.FILTERS_DEPS.keys()):
        if field in FIELDS.ALL:
            used_fields.add(field)
    steps = deps.get_steps(
        fields=used_fields,
        filters={
            FILTERS.IDS: WHATEVER,
            FILTERS.REVIEWS: WHATEVER,
            FILTERS.REVIEW_ACTIVITY: WHATEVER,
            FILTERS.PERSONS: WHATEVER,
            FILTERS.REVIEW_STATUSES: WHATEVER,
            FILTERS.MARK: WHATEVER,
            FILTERS.GOLDSTAR: WHATEVER,
            FILTERS.LEVEL_CHANGE: WHATEVER,
            FILTERS.SALARY_CHANGE: WHATEVER,
            FILTERS.BONUS: WHATEVER,
            FILTERS.OPTIONS_RSU: WHATEVER,
            FILTERS.DEPARTMENTS: WHATEVER,
            FILTERS.PROFESSION: WHATEVER,
            FILTERS.LEVEL: WHATEVER,
            FILTERS.SALARY_VALUE: WHATEVER,
            FILTERS.SALARY_CURRENCY: WHATEVER,
            FILTERS.PERSON_JOIN_AT: WHATEVER,
            FILTERS.STATUS: WHATEVER,
            FILTERS.FLAGGED: WHATEVER,
            FILTERS.ACTION_AT: WHATEVER,
            FILTERS.IS_DIFFER_FROM_DEFAULT: WHATEVER,
            FILTERS.SUBORDINATION: WHATEVER,
        },
        deps=DEPS,
    )

    expected = deps.Steps([
        UNORDERED([
            S(FILTERS.IDS, value=WHATEVER),
            S(FILTERS.REVIEWS, value=WHATEVER),
            S(FILTERS.REVIEW_ACTIVITY, value=WHATEVER),
            S(FILTERS.REVIEW_STATUSES, value=WHATEVER),
            S(FILTERS.PERSONS, value=WHATEVER),
        ]),
        S(FETCHERS.COLLECT, params={
            'db_fields': tuple(sorted([
                'person_id',
                'review_id',
                'approve_level',
                'bonus',
                'flagged',
                'goldstar',
                'id',
                'level_change',
                'mark',
                'options_rsu',
                'salary_change',
                'status',
            ])),
        }),
        UNORDERED([
            S(FIELDS.ID),
            S(FIELDS.PERSON_ID),
            S(FIELDS.REVIEW_ID),
            S(FIELDS.ROLES),
            S(FIELDS.PERMISSIONS_WRITE),
            S(FIELDS.STATUS),
            S(FIELDS.FLAGGED),
            S(FIELDS.APPROVE_LEVEL),
            S(FILTERS.STATUS, value=WHATEVER),
            S(FILTERS.FLAGGED, value=WHATEVER),
        ]),
        S(FETCHERS.REVIEWS, params={
            'db_fields': tuple(sorted([
                'status',
                'type',
                'name',
                'mark_mode',
                'goldstar_mode',
                'level_change_mode',
                'salary_change_mode',
                'bonus_mode',
                'options_rsu_mode',
                'deferred_payment_mode',
                'finish_date',
                'start_date',
                'evaluation_from_date',
                'evaluation_to_date',
            ]))
        }),
        UNORDERED([
            S(FIELDS.MODIFIERS),
            S(FIELDS.REVIEW_TYPE),
            S(FIELDS.REVIEW_NAME),
            S(FIELDS.REVIEW_STATUS),
            S(FIELDS.REVIEW_START_DATE),
            S(FIELDS.REVIEW_FINISH_DATE),
            S(FIELDS.REVIEW_EVALUATION_FROM_DATE),
            S(FIELDS.REVIEW_EVALUATION_TO_DATE),
            S(FIELDS.MARK),
            S(FIELDS.GOLDSTAR),
            S(FIELDS.BONUS),
            S(FIELDS.LEVEL_CHANGE),
            S(FIELDS.OPTIONS_RSU),
            S(FIELDS.SALARY_CHANGE),
            S(FILTERS.MARK, value=WHATEVER),
            S(FILTERS.GOLDSTAR, value=WHATEVER),
            S(FILTERS.LEVEL_CHANGE, value=WHATEVER),
            S(FILTERS.SALARY_CHANGE, value=WHATEVER),
            S(FILTERS.BONUS, value=WHATEVER),
            S(FILTERS.OPTIONS_RSU, value=WHATEVER),
        ]),
        S(FETCHERS.GOODIES, params={
            'db_fields': tuple(sorted([
                'salary_change_history',
                'bonus_history',
                'options_rsu_history',
            ]))
        }),
        S(FETCHERS.SUBORDINATION),
        S(FIELDS.SUBORDINATION),
        S(FILTERS.SUBORDINATION, value=WHATEVER),

        S(FETCHERS.REVIEWERS),
        UNORDERED([
            S(FIELDS.REVIEWERS),
            S(FIELDS.ACTION_AT),
        ]),
        S(FILTERS.ACTION_AT, value=WHATEVER),
        UNORDERED([
            S(FIELDS.ACTION_MARK),
            S(FIELDS.ACTION_GOLDSTAR),
            S(FIELDS.ACTION_LEVEL_CHANGE),
            S(FIELDS.ACTION_SALARY_CHANGE),
            S(FIELDS.ACTION_BONUS),
            S(FIELDS.ACTION_OPTIONS_RSU),
            S(FIELDS.ACTION_FLAGGED),
            S(FIELDS.ACTION_FLAGGED_POSITIVE),
            S(FIELDS.ACTION_COMMENT),
            S(FIELDS.ACTION_APPROVE),
            S(FIELDS.ACTION_ALLOW_ANNOUNCE),
            S(FIELDS.ACTION_ANNOUNCE),
            S(FIELDS.ACTION_REVIEWERS),
        ]),

        S(FETCHERS.PERSONS, params={
            'db_fields': tuple(sorted([
                'login',
                'first_name',
                'last_name',
                'is_dismissed',
                'join_at',
                'position',
                'department__id',
                'department__slug',
                'department__path',
                'department__name',
                'finance__salary_history',
                'finance__grade_history',
            ]))
        }),
        UNORDERED([
            S(FIELDS.PERSON_LOGIN),
            S(FIELDS.PERSON_FIRST_NAME),
            S(FIELDS.PERSON_LAST_NAME),
            S(FIELDS.PERSON_IS_DISMISSED),
            S(FIELDS.PERSON_JOIN_AT),
            S(FIELDS.PERSON_POSITION),
            S(FIELDS.PERSON_DEPARTMENT_ID),
            S(FIELDS.PERSON_DEPARTMENT_SLUG),
            S(FIELDS.PERSON_DEPARTMENT_PATH),
            S(FIELDS.PERSON_DEPARTMENT_NAME),
            S(FIELDS.SALARY_CURRENCY),
            S(FIELDS.SALARY_VALUE),
            S(FIELDS.LEVEL),
            S(FIELDS.PROFESSION),
            S(FIELDS.SALARY_CHANGE_IS_DIFFER_FROM_DEFAULT),
            S(FIELDS.BONUS_IS_DIFFER_FROM_DEFAULT),
            S(FIELDS.OPTIONS_RSU_IS_DIFFER_FROM_DEFAULT),
            S(FILTERS.PERSON_JOIN_AT, value=WHATEVER),
            S(FILTERS.DEPARTMENTS, value=WHATEVER),
            S(FILTERS.SALARY_CURRENCY, value=WHATEVER),
            S(FILTERS.SALARY_VALUE, value=WHATEVER),
            S(FILTERS.LEVEL, value=WHATEVER),
            S(FILTERS.PROFESSION, value=WHATEVER),
            S(FILTERS.IS_DIFFER_FROM_DEFAULT, value=WHATEVER),
        ]),
        S(FETCHERS.PERSON_REVIEW_HISTORY, params={
            'db_fields': tuple(sorted([
                'type',
            ]))
        }),
        S(FIELDS.MARK_LEVEL_HISTORY),
    ])

    assert_steps_equal(expected, steps)
