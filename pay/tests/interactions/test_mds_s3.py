import botocore
from botocore.exceptions import ClientError
from butils.application import getApplication
from StringIO import StringIO
from hamcrest import assert_that, equal_to, matches_regexp
from random import randint
import pytest

from yb_darkspirit.interactions.mds_s3 import DocumentsClient, DocumentTypes, ProcessTypes

SERIAL_NUMBER = '12345'
SERIAL_NUMBER_WITH_LEADING_ZEROS = SERIAL_NUMBER.zfill(20)
FN_SERIAL_NUMBER = '12346'
FILE_CONTENT = 'a document body ' + str(randint(0, 10))


@pytest.mark.parametrize('document_type, process_type, method, pattern', [
    (DocumentTypes.Application, ProcessTypes.Reregistration, lambda document_client: document_client.upload_reregistration_application, '^{serial_number}/re-registration/{fn_serial_number}/[\d\-\.T:]+_application_for_re_registration\.xml$'),
    (DocumentTypes.Registration_application, ProcessTypes.Registration, lambda document_client: document_client.upload_registration_application, '^{serial_number}/registration/{fn_serial_number}/[\d\-\.T:]+_application_for_registration\.xml$'),
    (DocumentTypes.Fns_report, ProcessTypes.Reregistration, lambda document_client: document_client.upload_reregistration_card, '^{serial_number_with_leading_zeros}/re-registration/{fn_serial_number}/{fn_serial_number}_report_of_re_registration.pdf$'),
    (DocumentTypes.Withdraw_application, ProcessTypes.Withdraw,lambda document_client: document_client.upload_withdraw_application, '^{serial_number}/withdraw/{fn_serial_number}/[\d\-\.T:]+_application_for_withdraw\.xml$'),
    (DocumentTypes.Withdraw_report, ProcessTypes.Withdraw, lambda document_client: document_client.upload_withdraw_card, '^{serial_number}/withdraw/{fn_serial_number}/{serial_number}_report_of_withdraw.pdf$')
])
def test_upload_get_delete_document(document_type, process_type, method, pattern):
    documents_client = DocumentsClient.from_app(getApplication())
    returned = method(documents_client)(SERIAL_NUMBER, FN_SERIAL_NUMBER, StringIO(FILE_CONTENT))
    file_name, date_stamp = returned['url'], returned['date_stamp']
    key = file_name.replace('https://s3.mdst.yandex.net/spirit-documents/', '')
    filled_pattern = pattern.format(
        serial_number=SERIAL_NUMBER,
        fn_serial_number=FN_SERIAL_NUMBER,
        serial_number_with_leading_zeros=SERIAL_NUMBER_WITH_LEADING_ZEROS
    )
    assert_that(key, matches_regexp(filled_pattern))

    serial_number_for_get_document = SERIAL_NUMBER
    if SERIAL_NUMBER_WITH_LEADING_ZEROS in key:
        serial_number_for_get_document = SERIAL_NUMBER_WITH_LEADING_ZEROS

    response = documents_client.get_document(process_type, serial_number_for_get_document, FN_SERIAL_NUMBER,
                                             document_type, date_stamp)

    real_content = response.get('Body').read()
    assert_that(real_content, equal_to(FILE_CONTENT))

    delete_response = documents_client.delete_document(process_type, serial_number_for_get_document, FN_SERIAL_NUMBER,
                                     document_type, date_stamp)

    assert_that(delete_response['ResponseMetadata']['HTTPStatusCode'], equal_to(204))




