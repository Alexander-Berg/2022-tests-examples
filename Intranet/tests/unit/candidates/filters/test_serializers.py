import pytest

from functools import partial
from typing import Callable

from django.utils import timezone

from intranet.femida.src.api.candidates.serializers import RecruiterCandidateFilterSerializer
from intranet.femida.src.candidates.controllers import (
    get_candidates_extended_statuses_and_changed_at,
)
from intranet.femida.src.candidates.helpers import candidate_is_current_employee_subquery
from intranet.femida.src.candidates.models import Candidate
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.permissions.context import context

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import use_cache
from intranet.femida.src.communications.choices import MESSAGE_TYPES


# Количество запросов:
# auth_permission   = 2
# waffle_switch     = 1
# candidates        = 1
# consideration     = 1 + 1 когда есть
# contact           = 1
# education         = 1
# job               = 1
# responsibles      = 1 + 1 когда есть
# profession        = 1 + 1 когда есть
# attachment        = 1
# skill             = 1
# tag               = 1 + 1 когда есть
# city              = 1
# notes             = 1
# waffle switch     = 1 (permissions) TODO: удалить
BASE_QUERIES_COUNT = 16
REQUIRED_FIELDS = {
    'candidate': {
        'id',
        'first_name',
        'last_name',
        'login',
        'is_current_employee',
        'extended_status',
        'extended_status_changed_at',
        'resume',
        'professions',
        'skills',
        'tags',
        'main_recruiter',
        'recruiters',
        'target_cities',
        'educations',
        'jobs',
        'considerations',
        'created',
        'modified',
        'contacts',
        'notes',
    },
    'resume': {
        'id',
        'name',
        'created',
    },
    'professions': {
        'id',
        'name',
    },
    'skills': {
        'id',
        'name',
    },
    'recruiters': {
        'id',
        'login',
        'is_dismissed',
    },
    'target_cities': {
        'id',
        'name',
    },
    'considerations': {
        'id',
        'state',
        'extended_status',
        'resolution',
        'started',
        'finished',
        'source',
        'source_description',
        'interviews',
    },
    'interviews': {
        'id',
        'type',
        'grade',
        'resolution',
    },
    'notes': {
        'id',
        'created',
        'author',
        'html',
        'modified',
    },
}


def check_field_diff(record, name):
    required_fields = REQUIRED_FIELDS.get(name, None)
    if not required_fields:
        return True, None

    result = required_fields <= set(record)
    if not result:
        errors = {}
        for field_name in required_fields - set(record):
            errors[field_name] = 'Field not found'
        return False, errors

    errors = {}
    for key in record.keys():
        value = record[key]
        if not value:
            continue

        if isinstance(value, list):
            value = value[0]

        if key in REQUIRED_FIELDS and not isinstance(value, dict):
            errors[key] = f'Not A dict: {value}'

        if isinstance(value, dict):
            result, error = check_field_diff(value, key)
            if not result:
                errors[key] = error
    if errors:
        return False, errors
    return True, None


def make_candidate(extensions: list[Callable] = None, **kwargs):
    if extensions is None:
        extensions = []

    def maker(**internal_kwargs):
        candidate = f.CandidateFactory()

        options = dict(candidate=candidate)
        options.update(kwargs)
        options.update(internal_kwargs)

        for ext in extensions:
            ext(**options)
        return candidate
    return maker


def add_attachment(candidate, **kwargs):
    f.CandidateAttachmentFactory(candidate=candidate)


def add_profession(candidate, **kwargs):
    f.CandidateProfessionFactory(candidate=candidate)


def add_skill(candidate, **kwargs):
    f.CandidateSkillFactory(candidate=candidate)


def add_responsible(candidate, **kwargs):
    f.CandidateResponsibleFactory(candidate=candidate)


def add_target_city(candidate, **kwargs):
    f.CandidateCityFactory(candidate=candidate)


def add_consideration(candidate, options=None, interviews=None, **kwargs):
    if options is None:
        options = {}
    if interviews is None:
        interviews = []
    if isinstance(interviews, int):
        interviews = [{}] * interviews
    consideration = f.ConsiderationFactory(candidate=candidate, **options)
    for interview_opts in interviews:
        f.create_interview(candidate=candidate, consideration=consideration, **interview_opts)


def add_job(candidate, **kwargs):
    f.CandidateJobFactory(candidate=candidate)


def add_education(candidate, **kwargs):
    f.CandidateEducationFactory(candidate=candidate)


def add_contacts(candidate, **kwargs):
    f.CandidateContactFactory(candidate=candidate)


def add_note(candidate, **kwargs):
    f.MessageFactory(
        candidate=candidate,
        type=MESSAGE_TYPES.note,
    )


def apply_request_context(context, qs, candidate_ids):
    qs = qs.annotate(
        is_current_employee=candidate_is_current_employee_subquery,
    )
    context['extended_statuses'] = get_candidates_extended_statuses_and_changed_at(candidate_ids)
    return qs


@pytest.mark.parametrize('candidate_creator, queries_count', (
    pytest.param(
        make_candidate(),
        BASE_QUERIES_COUNT,
        id='simple',
    ),
    pytest.param(
        make_candidate([add_attachment]),
        BASE_QUERIES_COUNT,
        id='candidate-with-attachment',
    ),
    pytest.param(
        make_candidate([add_attachment] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-attachment-5',
    ),
    pytest.param(
        make_candidate([add_note]),
        BASE_QUERIES_COUNT,
        id='candidate-with-note',
    ),
    pytest.param(
        make_candidate([add_note] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-note-5',
    ),
    pytest.param(
        make_candidate([add_profession]),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-profession',
    ),
    pytest.param(
        make_candidate([add_profession] * 5),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-profession-5',
    ),
    pytest.param(
        make_candidate([add_skill]),
        BASE_QUERIES_COUNT,
        id='candidate-with-skills',
    ),
    pytest.param(
        make_candidate([add_skill] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-skills-5',
    ),
    pytest.param(
        make_candidate([add_responsible]),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-responsible',
    ),
    pytest.param(
        make_candidate([add_responsible] * 5),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-responsible-5',
    ),
    pytest.param(
        make_candidate([add_target_city]),
        BASE_QUERIES_COUNT,
        id='candidate-with-target-city',
    ),
    pytest.param(
        make_candidate([add_target_city] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-target-city-5',
    ),
    pytest.param(
        make_candidate([add_job]),
        BASE_QUERIES_COUNT,
        id='candidate-with-job',
    ),
    pytest.param(
        make_candidate([add_job] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-job-5',
    ),
    pytest.param(
        make_candidate([add_contacts]),
        BASE_QUERIES_COUNT,
        id='candidate-with-contacts',
    ),
    pytest.param(
        make_candidate([add_contacts] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-contacts-5',
    ),
    pytest.param(
        make_candidate([add_education]),
        BASE_QUERIES_COUNT,
        id='candidate-with-education',
    ),
    pytest.param(
        make_candidate([add_education] * 5),
        BASE_QUERIES_COUNT,
        id='candidate-with-education-5',
    ),
    pytest.param(
        make_candidate([add_consideration]),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-consideration',
    ),
    pytest.param(
        make_candidate([
            add_contacts,
            add_attachment,
            add_profession,
            add_skill,
            add_responsible,
            add_target_city,
            add_job,
            add_education,
            add_consideration,
        ]),
        BASE_QUERIES_COUNT + 3,
        id='candidate-with-all-fields',
    ),
    pytest.param(
        f.create_heavy_candidate,
        BASE_QUERIES_COUNT + 4,
        id='candidate-heavy',
    ),
    pytest.param(
        lambda **kwargs: [f.create_heavy_candidate(**kwargs) for _ in range(10)],
        BASE_QUERIES_COUNT + 4,
        id='candidate-heavy-10',
    ),
    pytest.param(
        make_candidate([add_consideration], interviews=2),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-consideration-1-interview-2',
    ),
    pytest.param(
        make_candidate([add_consideration], interviews=20),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-consideration-1-interview-20',
    ),
    pytest.param(
        make_candidate([
            partial(
                add_consideration,
                options={
                    'state': Interview.STATES.finished,
                    'finished': timezone.now() - timezone.timedelta(days=180),
                },
            ),
            partial(add_consideration, interviews=10),
        ]),
        BASE_QUERIES_COUNT + 1,
        id='candidate-with-consideration-2',
    ),
    pytest.param(
        lambda **kwargs: [make_candidate()(**kwargs) for _ in range(5)],
        BASE_QUERIES_COUNT,
        id='candidate-5',
    ),
    pytest.param(
        lambda **kwargs: [make_candidate([add_note] * 5)(**kwargs) for _ in range(5)],
        BASE_QUERIES_COUNT,
        id='candidate-5-notes-5',
    ),
    pytest.param(
        lambda **kwargs: [
            make_candidate([partial(add_consideration, interviews=2)])(**kwargs)
            for _ in range(5)
        ],
        BASE_QUERIES_COUNT + 1,
        id='candidate-5-with-interview-2',
    ),
    pytest.param(
        lambda **kwargs: [
            make_candidate([partial(add_consideration, interviews=2)])(**kwargs)
            for _ in range(10)
        ],
        BASE_QUERIES_COUNT + 1,
        id='candidate-10-with-interview-2',
    ),
))
@use_cache
def test_candidate_serializer_fields(candidate_creator, queries_count, django_assert_num_queries):
    # Тестируем на RecruiterCandidateFilterSerializer,
    # потому что он содержит максимально полные данные
    serializer_class = RecruiterCandidateFilterSerializer
    recruiter = f.create_recruiter()

    context.init(recruiter)
    request_context = {'user': recruiter}
    candidates = candidate_creator(recruiter=recruiter)

    if not isinstance(candidates, list):
        candidates = [candidates]

    candidate_ids = [record.id for record in candidates]

    qs = Candidate.objects.filter(pk__in=candidate_ids)
    qs = serializer_class.setup_eager_loading(qs)
    qs = apply_request_context(request_context, qs, candidate_ids)

    with django_assert_num_queries(queries_count):
        serializer = serializer_class(
            qs,
            context=request_context,
            many=True,
        )
        result = serializer.data

    assert result != []
    result = result[0]
    success, errors = check_field_diff(result, 'candidate')
    assert success, errors
