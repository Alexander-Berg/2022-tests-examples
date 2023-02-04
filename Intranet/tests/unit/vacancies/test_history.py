from intranet.femida.src.vacancies.choices import VACANCY_STATUSES
from intranet.femida.src.vacancies.models import VacancyHistory
from intranet.femida.tests import factories as f


def test_history_for_status_in_progress(django_assert_num_queries):
    vacancy = f.create_vacancy(status=VACANCY_STATUSES.on_approval)

    with django_assert_num_queries(2):  # vacancy update + history insert
        vacancy.status = VACANCY_STATUSES.in_progress
        vacancy.save()

    history = (
        VacancyHistory.objects
        .filter(
            status=VACANCY_STATUSES.in_progress,
            vacancy_id=vacancy.id,
        )
        .first()
    )
    assert history is not None
    assert history.full_name is None


def test_history_for_status_offer_processing(django_assert_num_queries):
    application = f.create_application()
    vacancy = application.vacancy
    offer = f.create_offer(
        application=application,
        full_name='test_full_name',
    )

    with django_assert_num_queries(3):  # vacancy update + history insert + offer select
        vacancy.status = VACANCY_STATUSES.offer_processing
        vacancy.save()

    history = (
        VacancyHistory.objects
        .filter(
            status=VACANCY_STATUSES.offer_processing,
            vacancy_id=vacancy.id,
        )
        .first()
    )
    assert history is not None
    assert history.full_name == offer.full_name


def test_history_for_bp_change():
    vacancy = f.create_vacancy()
    vacancy_creation_history_id = vacancy.vacancy_history.first().id
    old_bp = vacancy.budget_position_id

    vacancy.budget_position_id += 1
    vacancy.save()

    new_history_records = list(
        VacancyHistory.objects
        .filter(vacancy_id=vacancy.id)
        .exclude(id=vacancy_creation_history_id)
        .values_list('budget_position_id', 'status')
    )
    new_history_records.sort()
    assert new_history_records == [
        (old_bp, VACANCY_STATUSES.closed),
        (vacancy.budget_position_id, vacancy.status),
    ]
