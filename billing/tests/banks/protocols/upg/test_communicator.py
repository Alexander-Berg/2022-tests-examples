from datetime import datetime

import pytest

from bcl.banks.registry import Raiffeisen
from bcl.banks.protocols.upg.communicator import UpgCommunicator
from bcl.banks.protocols.upg.exceptions import UpgAuthError, UpgRequestError, UpgProcessingException
from bcl.banks.protocols.upg.raiffeisen.base import RaiffRequest
from bcl.banks.protocols.upg.raiffeisen.doc_paydocru import PayDocRu


def test_communicator_login(mock_post):

    session_id = 'af444bb4-a8b4-47e2-8e82-a475d28e8c66'

    pre_login_success = '''
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
            <preLoginResponse xmlns="http://upg.sbns.bssys.com/">
                <return>Qlof9J3aLi2x7A==</return>
                <return>P1MYdjhYTAAdKxMyEVAzF1mLcGN5h8N/8X21Dbnq2Z8=</return>
            </preLoginResponse>
        </soap:Body>
        </soap:Envelope>
        '''

    tpl_login_success = '''
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
            <loginResponse xmlns="http://upg.sbns.bssys.com/">
                <return>%s</return>
            </loginResponse>
        </soap:Body>
        </soap:Envelope>
        '''

    mock_post(pre_login_success, tpl_login_success % session_id)
    comm = UpgCommunicator(Raiffeisen, 'https://localhost/sbns-upg/upg', 'offlinet', 'W')
    assert comm.auth() == session_id

    comm = UpgCommunicator(Raiffeisen, 'https://localhost/sbns-upg/upg', 'offlinet', 'W')

    mock_post(pre_login_success, tpl_login_success % 'SYSTEM_ERROR')
    with pytest.raises(UpgAuthError):
        comm.auth()


def test_communicator_request(mock_post):

    ticket_id = '9150ca09-d23b-42d0-9ac8-62a342c38048'

    tpl_result = '''
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
       <soap:Body>
          <sendRequestsResponse xmlns="http://upg.sbns.bssys.com/">
             <return>%s</return>
          </sendRequestsResponse>
       </soap:Body>
    </soap:Envelope>
    '''

    mock_post(tpl_result % ticket_id)

    comm = UpgCommunicator(Raiffeisen, 'https://localhost/sbns-upg/upg', 'offlinet', 'W')
    comm._session_id = '123123'

    with pytest.raises(Exception):
        PayDocRu({}).to_xml()

    dict_tpl = {
        'doc_ext_id': '1',
        'payee': {
            'acc': 'acc12345678901234567',
            'inn': 'inn123',
            'name': 'name123',
        },
        'payer': {
            'acc': 'acc12345678901234567',
            'inn': 'inn123',
            'name': 'name123',
        },
        'document_data': {
            'tax_sum': 123123,
            'tax_type': 'Vat1',
            'tax_rate': '1',
            'sum': 123123,
            'payment_kind': '',
            'priority': 1,
            'date': datetime(2016, 11, 17),
            'op_kind': '1',
            'num': '100',
            'purpose': 'a purpose',
        }
    }

    doc1 = RaiffRequest(PayDocRu(dict_tpl).to_xml()).to_xml()

    new_dict = dict(dict_tpl)
    new_dict['doc_ext_id'] = '2'

    doc2 = RaiffRequest(PayDocRu(new_dict).to_xml()).to_xml()

    ticket_ids = comm.send_requests([doc1, doc2])

    assert len(ticket_ids) == 1
    assert ticket_ids[0] == ticket_id

    mock_post(tpl_result % '<!--NONEXISTENT SESSION-->')
    with pytest.raises(UpgRequestError):
        comm.send_requests([doc1, doc2])


def test_communicator_request_status(mock_post):

    response_raw = '''
    &lt;upg:Response xmlns:upg=&quot;http://bssys.com/upg/response&quot; xmlns:upgRaif=&quot;http://bssys.com/upg/response/raif&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; createTime=&quot;2014-03-23T23:56:39&quot; receiver=&quot;cc289e3b-04f1-42c7-a156-9dedbd80a6d9&quot; requestId=&quot;85c6b6c3-c314-4cb1-a452-dbbc1c48e754&quot; responseId=&quot;9150ca09-d23b-42d0-9ac8-62a342c38048&quot; sender=&quot;DBO&quot; version=&quot;1.1&quot;&gt;&#xD;
        &lt;upg:Tickets&gt;&#xD;
            &lt;upg:Ticket createTime=&quot;2014-03-23T23:56:40&quot; docId=&quot;ad7c17e5-530f-453e-8c58-90572e2f302b&quot;&gt;&#xD;
                &lt;upg:Info docExtId=&quot;ef5efe58-f720-4088-b1a7-a308b786d39d&quot; statusStateCode=&quot;DELIVERED&quot;/&gt;&#xD;
                &lt;upg:Signs&gt;&#xD;
                    &lt;upg:Sign&gt;&#xD;
                        &lt;upg:Issuer&gt;E=info@cryptopro.ru, C=RU, O=CRYPTO-PRO, CN=Test Center CRYPTO-PRO&lt;/upg:Issuer&gt;&#xD;
                        &lt;upg:SN&gt;6FBBEAFF00020006D6AB&lt;/upg:SN&gt;&#xD;
                        &lt;upg:Value&gt;MIICOQYJKoZIhvcNAQcCoIICKjCCAiYCAQExDDAKBgYqhQMCAgkFADALBgkqhkiG9w0BBwExggIEMIICAAIBATBzMGUxIDAeBgkqhkiG9w0BCQEWEWluZm9AY3J5cHRvcHJvLnJ1MQswCQYDVQQGEwJSVTETMBEGA1UEChMKQ1JZUFRPLVBSTzEfMB0GA1UEAxMWVGVzdCBDZW50ZXIgQ1JZUFRPLVBSTwIKb7vq/wACAAbWqzAKBgYqhQMCAgkFAKCCASowGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0BCQUxDxcNMTQwMzIzMTk1NjQwWjAvBgkqhkiG9w0BCQQxIgQg9/jK+9NproZpEPtPCXR4zeFUoAXhz66HEbIRz7Qm+kQwgb4GCyqGSIb3DQEJEAIvMYGuMIGrMIGoMIGlMAgGBiqFAwICCQQgoB0wfzq1TeDCoCDzBtQva6zc3Fa/QjIMWTby2jJqSNowdzBppGcwZTEgMB4GCSqGSIb3DQEJARYRaW5mb0BjcnlwdG9wcm8ucnUxCzAJBgNVBAYTAlJVMRMwEQYDVQQKEwpDUllQVE8tUFJPMR8wHQYDVQQDExZUZXN0IENlbnRlciBDUllQVE8tUFJPAgpvu+r/AAIABtarMAoGBiqFAwICEwUABEBJaRiir0EOfjeqvse+ktqyokOZmtzTxsK0emq6XdavcB1qg4mj+NAxEjI6401HKSY2r08bIgOKIh3RyaFKNNF4&lt;/upg:Value&gt;&#xD;
                        &lt;upg:DigestName&gt;com.bssys.sbns.integration.upg.TicketUpgSignDigest&lt;/upg:DigestName&gt;&#xD;
                        &lt;upg:DigestVersion&gt;1&lt;/upg:DigestVersion&gt;&#xD;
                        &lt;upg:SignType&gt;BANK_ENGINE&lt;/upg:SignType&gt;&#xD;
                    &lt;/upg:Sign&gt;&#xD;
                &lt;/upg:Signs&gt;&#xD;
            &lt;/upg:Ticket&gt;&#xD;
        &lt;/upg:Tickets&gt;&#xD;
    &lt;/upg:Response&gt;&#xD;
    '''

    tpl_result = '''
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <getRequestStatusResponse xmlns="http://upg.sbns.bssys.com/">
            <return>%s</return>
        </getRequestStatusResponse>
    </soap:Body>
    </soap:Envelope>
    '''

    mock_post(tpl_result % response_raw)

    comm = UpgCommunicator(Raiffeisen, 'https://localhost/sbns-upg/upg', 'offlinet', 'W')
    comm._session_id = '123123'

    mock_post(tpl_result % '<!--UNKNOWN REQUEST-->')

    comm = UpgCommunicator(Raiffeisen, 'https://localhost/sbns-upg/upg', 'offlinet', 'W')
    comm._session_id = '123123'

    with pytest.raises(UpgProcessingException):
        comm.get_request_status('9150ca09-d23b-42d0-9ac8-62a342c38048')
