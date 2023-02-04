import pytest

from yb_darkspirit.core.pdf_parsing import extract_text_by_page, extract_info_from_card_text, \
    parse_and_assert_reregistration_card, \
    parse_and_assert_registration_card, upload_to_s3_reregistration_card, \
    parse_and_check_cash_card
from hamcrest import assert_that, equal_to, is_, has_entries
from io import open

REG_CARD_PDF = './tests/core/reg_card.pdf'
REG_PARSED_CARD = './tests/core/reg_parsed_card.txt'
REREG_CARD_PDF = './tests/core/rereg_card.pdf'
REREG_PARSED_CARD = './tests/core/rereg_parsed_card.txt'
CR_SN = u'381007152062'
FS_SN = u'9960440300340930'
RNM = u'0001520716048397'


def _test_text_from_pdf_extraction(card_pdf, parsed_card):
    with open(card_pdf, 'rb') as pdf_file:
        content = extract_text_by_page(pdf_file)
    with open(parsed_card, 'r', encoding='utf-8') as parsed_file:
        expected_text = parsed_file.read()

    assert_that(len(content[0]), is_(1))
    assert_that(content[0]['page_1'], equal_to(expected_text))


def test_parsed_text():
    _test_text_from_pdf_extraction(REG_CARD_PDF, REG_PARSED_CARD)


def test_text_from_pdf_extraction():
    _test_text_from_pdf_extraction(REREG_CARD_PDF, REREG_PARSED_CARD)


def test_extract_information_from_reregistration_card():
    with open(REREG_PARSED_CARD, 'r', encoding='utf-8') as parsed_file:
        text = parsed_file.read()

    info = extract_info_from_card_text(text)

    assert_that(info['sn'], equal_to(CR_SN))
    assert_that(info['rnm'], equal_to(RNM))
    assert_that(info['fn'], equal_to(FS_SN))


@pytest.mark.parametrize('cr_sn, card, is_reg', [
    (CR_SN, REREG_CARD_PDF, False),
    (CR_SN, REREG_CARD_PDF, True)
])
def test_parse_and_check_cash_card(cr_sn, card, is_reg):
    with open(card, 'rb') as f:
        content = f.read()

    result = parse_and_check_cash_card(content, cr_sn, FS_SN, is_reg=is_reg).is_success()
    assert_that(result, equal_to(True))


def test_parse_and_upload_rereg_pdf():
    with open(REREG_CARD_PDF, 'rb') as f:
        content = f.read()

    info = parse_and_assert_reregistration_card(content, CR_SN, FS_SN)
    response_json = upload_to_s3_reregistration_card(content, CR_SN, FS_SN, info)

    assert_that(response_json, has_entries({
        'sn': equal_to(CR_SN),
        'fn': equal_to(FS_SN),
        'rnm': equal_to(RNM),
        'mds_file': equal_to(
            u'https://s3.mdst.yandex.net/spirit-documents/{CR}/re-registration/{FS}/{FS}_report_of_re_registration.pdf'
            .format(FS=FS_SN, CR='0'*(20-len(CR_SN)) + CR_SN)
        )
    }))
    assert_that(len(response_json), equal_to(4))


def test_parse_reg_pdf():
    with open(REG_CARD_PDF, 'rb') as f:
        content = f.read()

    response_json = parse_and_assert_registration_card(content, u'381009383128')

    assert_that(response_json, has_entries({
        'sn': equal_to(u'381009383128'),
        'kpp': equal_to(u'770501001'),
        'inn': equal_to(u'9705114405'),
        'rnm': equal_to(u'0005371901022757')
    }))
    assert_that(len(response_json), equal_to(4))
