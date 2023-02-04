import itertools
import pytest

from functools import partial
from unittest.mock import patch

from intranet.femida.src.actionlog.models import LogRecord, Snapshot, SNAPSHOT_REASONS
from intranet.femida.src.offers.choices import (
    OFFER_STATUSES,
    DOCUMENT_TYPES,
    OFFER_DOCS_PROCESSING_STATUSES as DOCS_STATUSES,
    OFFER_DOCS_PROCESSING_RESOLUTIONS as DOCS_RESOLUTIONS,
    OFFER_DOCS_PROCESSING_SKIPPED_RESOLUTIONS as DOCS_SKIPPED_RESOLUTIONS,
)
from intranet.femida.src.offers.models import Offer
from intranet.femida.src.stats.fetchers import OfferDocsProcessingFunnelDataFetcher
from intranet.femida.src.stats.registry import registry

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@patch(
    target=(
        'intranet.femida.src.stats.fetchers.offers.'
        'OfferDocsProcessingFunnelDataFetcher.department_ids'
    ),
    new=[100500],
)
def test_offer_docs_processing_funnel_7d():
    department = f.DepartmentFactory(id=100500)
    application = f.ApplicationFactory()
    SimpleOffer = partial(
        Offer,
        department=department,
        application=application,
        candidate=application.candidate,
        vacancy=application.vacancy,
    )
    AcceptedOffer = partial(SimpleOffer, status=OFFER_STATUSES.accepted)
    FinishedOffer = partial(AcceptedOffer, docs_processing_status=DOCS_STATUSES.finished)
    ExpiredOffer = partial(
        FinishedOffer,
        docs_processing_resolution=DOCS_RESOLUTIONS.expired,
        docs_request_count=1,
    )
    ProcessedOffer = partial(
        FinishedOffer,
        docs_processing_resolution=DOCS_RESOLUTIONS.processed,
        passport_data={'document_type': DOCUMENT_TYPES.russian_passport},
    )

    # Офферы, по которым даже не пытались обработать документы
    offers = [
        FinishedOffer(docs_processing_resolution=resolution)
        for resolution, _ in DOCS_SKIPPED_RESOLUTIONS
    ]
    # Документы в обработке/на перезапросе,
    # или зависли в этом состоянии, потому что оффер закрыли/отклонили
    valid_statuses = OfferDocsProcessingFunnelDataFetcher.valid_statuses
    active_docs_statuses = (DOCS_STATUSES.in_progress, DOCS_STATUSES.need_information)
    offers.extend(
        SimpleOffer(status=status, docs_processing_status=dp_status)
        for status, dp_status in itertools.product(valid_statuses, active_docs_statuses)
    )
    # Остальные финальные состояния
    offers.extend((
        # Не смогли обработать документы
        FinishedOffer(docs_processing_resolution=DOCS_RESOLUTIONS.failed),
        # Не дождались перезапрошенных документов
        ExpiredOffer(),
        # Не дождались перезапрошенных документов, но смогли создать ФЛ из того, что есть
        ExpiredOffer(oebs_person_id=1),
        # Успешно обработали, но не стали создавать ФЛ, потому что паспорт не РФ
        FinishedOffer(
            docs_processing_resolution=DOCS_RESOLUTIONS.processed,
            passport_data={'document_type': DOCUMENT_TYPES.other},
        ),
        # Успешно обработали документы, но не смогли создать ФЛ
        ProcessedOffer(),
        # Успешно обработали документы и создали ФЛ
        ProcessedOffer(oebs_person_id=1),
        # Успешно обработали документы и создали ФЛ после перезапроса документов
        ProcessedOffer(oebs_person_id=2, docs_request_count=1),
    ))

    fielddate = '2020-02-02'
    Offer.objects.bulk_create(offers)
    log_records = LogRecord.objects.bulk_create(
        LogRecord(
            action_time='2020-01-31T00:00:00Z',
            action_name='offer_check_oebs_login',
        )
        for _ in offers
    )
    Snapshot.objects.bulk_create(
        Snapshot(
            log_record=log_record,
            obj_str='offer',
            obj_id=offer.id,
            reason=SNAPSHOT_REASONS.change,
        )
        for offer, log_record in zip(offers, log_records)
    )

    report_class = registry.reports['offer_docs_processing_funnel_7d']
    result_by_rerequest = {
        row['rerequest']: row
        for row in report_class().get_data(fielddate=fielddate)
    }

    expected_all = {
        'fielddate': fielddate,
        'department': [100500],
        'rerequest': 'ALL',
        'total': len(offers),

        # По 1 для каждой skipped-резолюции
        'skipped_no_docs_to_check': 1,
        'skipped_person_found': 1,
        'skipped_no_docs_to_process': 1,
        'skipped_oohrc_org': 1,
        'skipped_foreigner': 1,

        # Всего отправлено в Янг
        'total_sent_to_yang': len(offers) - len(DOCS_SKIPPED_RESOLUTIONS),

        # По 1 для всяких ещё активных/зависших
        'accepted_in_progress': 1,
        'accepted_need_information': 1,
        'closed_in_progress': 1,
        'closed_need_information': 1,
        'rejected_in_progress': 2,
        'rejected_need_information': 2,

        # Остальные результаты обработки документов
        'failed': 1,
        'expired': 2,
        'processed': 4,

        # Результаты создания ФЛ
        'processed_person_created': 2,
        'processed_invalid_passport': 1,
        'processed_person_creation_failed': 1,
        'expired_person_created': 1,
    }
    # Все не 0 значения для офферов с перезапросами
    expected_rerequest = {
        'fielddate': fielddate,
        'department': [100500],
        'rerequest': True,
        'total': 3,
        'total_sent_to_yang': 3,
        'expired': 2,
        'processed': 1,
        'processed_person_created': 1,
        'expired_person_created': 1,
    }
    result_all = result_by_rerequest['ALL']
    result_rerequest = result_by_rerequest[True]

    assert result_all == expected_all
    assert all(result_rerequest[k] == expected_rerequest.get(k, 0) for k in result_rerequest)
