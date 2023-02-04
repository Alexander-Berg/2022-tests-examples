from datetime import datetime

import pytest

from bcl.core.models import Prove, Svo, SvoItem, Spd, SpdItem, Letter, DocRegistryRecord, states
from bcl.banks.registry import Ing
from bcl.banks.common.letters import ProveLetter
from bcl.exceptions import LogicError


def test_prove(init_uploaded, init_doc_prove):

    # инициализируем с минимальным набором атрибутов
    prove = init_doc_prove(delivery=Prove.DELIV_PREPAY_FROM_RESIDENT)
    assert f'[{Prove.TYPE_ACT}]' in f'{prove}'
    assert prove.display_type == 'Акт'

    # крепим вложение
    attach = prove.attachment_add_from_uploaded(init_uploaded())
    assert attach.id

    assert prove.attachments.count() == 1
    assert prove.display_delivery == '3 - Аванс от резидента'


def test_svo(init_user, init_doc_prove, get_source_payment):

    user = init_user()
    associate = Ing

    # инициализируем с минимальным набором атрибутов
    doc = Svo.objects.create(associate_id=associate.id, user=user)
    assert f'{doc}'
    assert doc.correction == 0

    # платёж вяжем с ПД и дополнительно со строкой СВО
    payment = get_source_payment(associate=associate)
    prove = init_doc_prove(payments=[payment])

    item = doc.set_items([
        SvoItem(
            prove=prove,
            payment=payment,
        ),
    ])[0]

    assert f'{item}'
    assert item.num == 1
    assert doc.items.count() == 1

    # По данным платежа.
    svo = Svo.from_payment(payment=payment, contract=None, user=user, proves=[prove])
    assert svo.id


def test_spd(init_user, init_doc_prove, get_source_payment):

    user = init_user()
    associate = Ing

    # инициализируем с минимальным набором атрибутов
    doc = Spd.objects.create(associate_id=associate.id, user=user)
    assert f'{doc}'

    items = doc.set_items([
        SpdItem(
            prove=init_doc_prove(payments=[{}]),
        ),
        SpdItem(
            prove=init_doc_prove(payments=[{}]),
        ),
        SpdItem(
            prove=init_doc_prove(payments=[{}]),
            num=44,
        ),
    ])

    item = items[0]
    assert f'{item}'
    assert item.num == 1
    assert doc.items.count() == 3

    # проверим писвоение номера
    assert items[1].num == 2
    assert items[2].num == 44

    # По данным платежа.
    prove = init_doc_prove(payments=[{}])
    payment = get_source_payment(associate=associate)

    spd = Spd.from_payment(payment=payment, contract=None, user=user, proves=[prove])
    assert spd.id


def test_letter(init_user, init_doc_prove, init_uploaded, get_source_payment, time_freeze):

    user = init_user()
    associate = Ing

    prove = init_doc_prove()

    # инициализируем с минимальным набором атрибутов
    doc = Letter.objects.create(associate_id=associate.id, user=user, type_id=ProveLetter.id)
    assert f'{doc}'

    doc.proves.add(prove)
    doc.attachment_add_from_uploaded(init_uploaded())

    assert doc.proves.count() == 1
    assert doc.attachments.count() == 1

    # По данным платежа.
    dt = datetime(2021, 10, 15)

    letter = ProveLetter.from_payment(
        payment=get_source_payment({'number': '10', 'date': dt}, associate=associate),
        contract=None, user=user, proves=[prove])
    assert letter.subject == 'Документы к п/п 10 15.10.2021, дог №12345678/1234/1234/1/1'

    subject = ProveLetter.get_subject_from_payment(
        payment=get_source_payment({
            'trans_pass': '', 'number': '20', 'contract_num': '3434', 'date': dt}, associate=associate))
    assert subject == 'Документы к п/п 20 15.10.2021, дог №3434'

    subject = ProveLetter.get_subject_from_payment(
        payment=get_source_payment({'trans_pass': '', 'number': '15', 'date': dt}, associate=associate))
    assert subject == 'Документы к п/п 15 15.10.2021'


def test_doc_registry(init_user):

    user = init_user()
    associate = Ing

    # СВО
    svo = Svo.objects.create(associate_id=associate.id, user=user)
    rec_svo = DocRegistryRecord.add_doc(
        doc=svo,
        associate=associate,
        user=user,
    )
    assert rec_svo.is_svo
    assert rec_svo.link_svo == svo
    assert rec_svo.user == user
    assert f'{rec_svo}'

    # СПД
    spd = Spd.objects.create(associate_id=associate.id, user=user)
    rec_spd = DocRegistryRecord.add_doc(
        doc=spd,
        associate=associate,
        user=user,
    )
    assert rec_spd.is_spd
    assert rec_spd.link_spd == spd
    assert rec_spd.user == user

    # Письмо
    letter = Letter.objects.create(associate_id=associate.id, user=user, type_id=ProveLetter.id)
    rec_letter = DocRegistryRecord.add_doc(
        doc=letter,
        associate=associate,
        user=user,
    )
    assert rec_letter.is_letter
    assert rec_letter.link_letter == letter
    assert rec_letter.user == user

    # провальная попытка зарегистировать неподдерживаемый документ
    with pytest.raises(LogicError) as e:
        DocRegistryRecord.add_doc(doc=user, associate=associate, user=user)
    assert 'Unknown document type' in f'{e.value}'

    # провальная попытка зарегистировать входящий документ без базового
    with pytest.raises(LogicError) as e:
        DocRegistryRecord.add_doc(
            doc=svo, associate=associate, user=user, incoming=True)
    assert 'Initial document needs to be' in f'{e.value}'

    # провальная попытка зарегистировать исходящий документ байтами, а не объектом
    with pytest.raises(LogicError) as e:
        DocRegistryRecord.add_doc(doc=b'some', associate=associate, user=user)
    assert 'only allowed for incoming' in f'{e.value}'

    # удачная попытка зарегистировать ответный документ и проставить статус
    answer = DocRegistryRecord.add_doc(
        doc=b'other', associate=associate, user=user, incoming=True,
        initial=rec_letter, status=states.COMPLETE)

    assert answer.initial == rec_letter
    assert answer.is_letter
    assert answer.is_complete
    assert answer.content == b'other'
    assert rec_letter.responses.count() == 1  # это и есть наш ответный входящий документ

    # Проверим, что накопилось в реестре.
    assert DocRegistryRecord.objects.count() == 4


def test_doc_registry_scheduling(init_user):
    user = init_user()
    associate = Ing

    rec_svo = DocRegistryRecord.add_doc(
        doc=Svo.objects.create(associate_id=associate.id, user=user),
        associate=associate, user=user)

    # Проверяем аспекты отправки
    rec_svo.schedule()
    assert rec_svo.is_processing_ready

    scheduled = DocRegistryRecord.get_scheduled()
    assert scheduled == rec_svo
