from datetime import datetime

from freezegun import freeze_time

from bcl.banks.party_raiff.syncers import LetterSyncer
from bcl.banks.registry import Raiffeisen
from bcl.core.models import Request, states


def test_letter_creator(
    read_fixture, get_document, get_assoc_acc_curr, response_mock, init_user,
    get_signing_right, mock_signer, validate_xml, mock_post, run_task,
):
    associate, acc, _ = get_assoc_acc_curr(associate=Raiffeisen, account='12345678909876543210')

    def get_record(rec_id, **kwargs):

        record = get_document(
            associate=associate, id=rec_id, account=acc, generate='letter_prove',
            payments=[{'f_name': 'some'}],
            attachments={'one.txt': b'data1', 'two.txt': b'data2'},
            **kwargs
        )
        prove = record.dyn_prove
        org = prove.account.org
        org.inn = '123456789011'
        org.save()
        return record

    letter_record = get_record(10)

    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')
    get_signing_right(associate.id, 'serial')
    xml_response = read_fixture('payment/sendrequest_response.xml', decode='utf-8')

    with freeze_time(datetime(2020, 8, 10)):
        letter_record.refresh_from_db()
        with response_mock(f'POST https://localhost/sbns-upg/upg -> 200:{xml_response}'):
            associate.automate_documents(letter_record)

    letter_record.refresh_from_db()
    content = letter_record.content.decode()
    assert 'accNum="12345678909876543210" bic="044525700" inn="123456789011"' in content
    assert '<upg:SN>216f0a8c000100001380<' in content
    assert '<upg:AttachmentName>one.txt</upg:AttachmentName><upg:Body>ZGF0YTE=<' in content

    request = Request.objects.first()
    assert request.type == Request.TYPE_LETTER
    assert request.object_id == letter_record.id
    assert request.remote_id == '867299b9-1c10-4c65-b001-1b72431e905c'
    assert not request.doc_id
    assert not request.doc_request_id

    # тест невалидного документа
    letter_record_2 = get_record(11)
    letter_record_2.original.subject = ''  # невалидня тема
    letter_record_2.original.save()
    letter_record_2.schedule()
    run_task('process_documents')
    letter_record_2.refresh_from_db()
    assert letter_record_2.is_error
    assert 'не соответствует' in letter_record_2.processing_notes

    # далее тест синхронизации.
    xml_response_tickets = read_fixture('payment/get_status_delivered.xml', decode='utf-8')
    xml_response_docids = read_fixture('payment/sendrequest_response.xml', decode='utf-8')

    with response_mock([
        f'POST https://localhost/sbns-upg/upg -> 200:{xml_response_tickets}',
        f'POST https://localhost/sbns-upg/upg -> 200:{xml_response_docids}',
    ]):
        LetterSyncer(associate=Raiffeisen).prepare()

    request.refresh_from_db()
    assert request.doc_id == '29b60892-10a4-48d4-8dd6-3f7c7ccefa4b'
    assert not request.processed

    mock_post(read_fixture('req_status_err.xml'))
    LetterSyncer(associate=Raiffeisen).sync()

    request.refresh_from_db()
    assert request.processed
    assert request.status == states.DECLINED_BY_BANK

    letter_record.refresh_from_db()
    assert letter_record.is_declined_by_bank
    assert 'invalid xml' in letter_record.processing_notes
    assert letter_record.original.is_declined_by_bank
    assert 'invalid xml' in letter_record.original.processing_notes
