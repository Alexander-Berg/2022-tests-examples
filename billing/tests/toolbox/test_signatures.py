import pytest

from bcl.exceptions import DigitalSignError
from bcl.toolbox.signatures import XmlSignature, DetachedMultipleSignature


def test_xml_signature(read_fixture):
    raw_signature = read_fixture('signature.xml', decode='utf-8')

    signature = XmlSignature(raw_signature)
    text = signature.as_text()

    assert signature.serial == '12000d3ece1cbf936c80c766fb0000000d3ece'

    assert '<KeyName>12000d3ece1cbf936c80c766fb0000000d3ece</KeyName>' in text
    assert XmlSignature.from_signed(f'<SignedDoc>{raw_signature}</SignedDoc>').as_text() == text

    expected = '<outer><inner>sign</inner></outer>'
    assert XmlSignature.inject('<outer></outer>', '<inner>sign</inner>', 'outer') == expected
    assert XmlSignature.inject('<outer/>', '<inner>sign</inner>', 'outer') == expected

    assert 'Signature' in signature.get_template(mx_type='camt')

    with pytest.raises(DigitalSignError):
        _ = XmlSignature('Not a signature').serial


def test_detached_signature(read_fixture):
    payment_signature = read_fixture('detached_signature_base64.txt', decode='ascii')
    bundle_signature_text = [payment_signature, payment_signature]

    bundle_signature = DetachedMultipleSignature.from_signed(bundle_signature_text)
    text = bundle_signature.as_text()

    assert bundle_signature.serial == '216f0a8c000100001380'

    assert text == '|'.join(bundle_signature_text)
    assert DetachedMultipleSignature.from_bytes(bundle_signature.as_bytes()).as_text() == text

    assert DetachedMultipleSignature.from_signed([payment_signature]).as_text() == payment_signature

    with pytest.raises(DigitalSignError):
        _ = DetachedMultipleSignature.from_signed('Not a signature').serial
