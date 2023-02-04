from django.utils import timezone
from unittest.mock import patch, MagicMock

from intranet.femida.src.candidates.models import CandidateAttachment, Verification
from intranet.femida.src.interviews.models import InterviewRound
from intranet.femida.src.yt.base import (
    AttachmentYTTable,
    InterviewRoundYTTable,
    VerificationYTTable,
)

from intranet.femida.tests import factories as f


@patch('intranet.femida.src.yt.base.yt', MagicMock())
def test_attachment_queries_count(django_assert_num_queries):
    """
    Проверяет кол-во запросов при записи резюме в YT
    """
    f.CandidateAttachmentFactory.create_batch(5)

    # Ожидается 3 запроса:
    # 1. Получение аттачей
    # 2. Префетч ответственных
    # 3. Префетч профессий
    with django_assert_num_queries(3):
        qs = CandidateAttachment.unsafe.all()
        table = AttachmentYTTable(timezone.now())
        table.write(qs)


@patch('intranet.femida.src.yt.base.yt', MagicMock())
def test_verification_queries_count(django_assert_num_queries):
    """
    Проверяет кол-во запросов при записи проверки оффера в YT
    """
    f.VerificationFactory.create_batch(5)

    # Ожидается 2 запроса:
    # 1. Получение верификаций
    # 2. Префетч контактов
    with django_assert_num_queries(2):
        qs = Verification.objects.all()
        table = VerificationYTTable(timezone.now())
        table.write(qs)


@patch('intranet.femida.src.yt.base.yt', MagicMock())
def test_interview_round_queries_count(django_assert_num_queries):
    """
    Проверяет кол-во запросов при записи раунда секций в YT
    """
    interview_rounds = f.InterviewRoundFactory.create_batch(2)
    for r in interview_rounds:
        interviews = [r.interviews.create(created_by=r.created_by) for _ in range(2)]
        for i in interviews:
            i.potential_interviewers.set(f.UserFactory.create_batch(2))

    # Ожидается 4 запроса:
    # 1. Получение раундов
    # 2. Префетч тайм-слотов
    # 3. Префетч секций
    # 4. Префетч потенциальных собеседующих
    with django_assert_num_queries(4):
        qs = InterviewRound.objects.all()
        table = InterviewRoundYTTable()
        table.write(qs)
