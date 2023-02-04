import pytest

from hamcrest import all_of, assert_that, contains, equal_to, has_length, has_properties

from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank, normalize_card_issuer


class TestNormalizeCardIssuer:
    @pytest.fixture
    def get_dummy_log_records(self, caplog):
        def _inner():
            return [record for record in caplog.records if record.name == 'dummy_logger']

        return _inner

    @pytest.mark.parametrize('issuer, expected_result', (
        ('ALFA BANK', 'ALFABANK'),
        ('CJSC ALFA-BANK', 'ALFABANK'),
        ('CLOSED JOINT-STOCK COMPANY ALFA-BANK', 'ALFABANK'),
        ('COMMERCIAL INNOVATION BANK ALFA-BANK', 'ALFABANK'),
        ('JSC SUBSIDIARY BANK ALFA-BANK', 'ALFABANK'),
        ('PJSC ALFA-BANK', 'ALFABANK'),
        ('PUBLIC JOINT STOCK COMPANY ALFA BANK', 'ALFABANK'),
        ('PUBLIC JSC ALFA-BANK', 'ALFABANK'),
        ('OJSC BPS-BANK', 'SBERBANK'),
        ('SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK)', 'SBERBANK'),
        ('SB SBERBANK JSC', 'SBERBANK'),
        ('SBERBANK OF RUSSIA', 'SBERBANK'),
        ('SBERBANK', 'SBERBANK'),
        ('SUBSIDIARY BANK SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK) CJSC (UKRAINE)', 'SBERBANK'),
        ('SUBSIDIARY BANK SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK) PJSC', 'SBERBANK'),
        ('SUBSIDIARY BANK SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK)', 'SBERBANK'),
        ('SUBSIDIARY BANK SBERBANK OF RU', 'SBERBANK'),
        ('SUBSIDIARY BANK SBERBANK OF RUSSIA PJSC', 'SBERBANK'),
        ('SUBSIDIARY BANK SBERBANK OF RUSSIA', 'SBERBANK'),
        ('TCS BANK (CJSC)', 'TINKOFF'),
        ('TCS BANK', 'TINKOFF'),
        ('TINKOFF CREDIT SYSTEMS BANK (C', 'TINKOFF'),
        ('TINKOFF CREDIT SYSTEMS BANK CJSC', 'TINKOFF'),
        ('BANK VTB24 (CJSC)', 'VTB'),
        ('VTB24 (JSC)', 'VTB'),
        ('VTB24', 'VTB'),
        ('BELARUSIAN-RUSSIAN BELGAZPROMBANK JS', 'BELGAZPROMBANK'),
        ('BELGAZPROMBANK', 'BELGAZPROMBANK'),
        ('GAZPROMBANK OJSC', 'GAZPROMBANK'),
        ('GAZPROMBANK OPEN JOINT STOCK', 'GAZPROMBANK'),
        ('GAZPROMBANK (OPEN JOINT STOCK COMPANY)', 'GAZPROMBANK'),
        ('JSB GAZPROMBANK', 'GAZPROMBANK'),
        ('JSB GAZPROMBANK (CJSC)', 'GAZPROMBANK'),
        ('COMMERCIAL BANK OF MOSCOW', 'MKB'),
        ('CREDIT BANK OF MOSCOW', 'MKB'),
        ('CREDIT BANK OF MOSCOW (MKB BANK)', 'MKB'),
        ('JS COMMERCIAL BANK BANK OF MOSCOW', 'MKB'),
        ('JSC BANK OF MOSCOW', 'MKB'),
        ('OJSC BANK OF MOSCOW', 'MKB'),
        ('CB OTKRITIE CJSC', 'OTKRITIE'),
        ('KHANTY-MANSIYSK BANK OTKRITIE (PJSC)', 'OTKRITIE'),
        ('OTKRITIE BANK JSC', 'OTKRITIE'),
        ('COMMERCIAL BANK PROMSVYAZBANK', 'PSB'),
        ('OJSC PROMSVYAZBANK', 'PSB'),
        ('PROMSVYAZBANK', 'PSB'),
        ('COMMERCIAL BANK ROSBANK', 'ROSBANK'),
        ('JS COMMERCIAL BANK ROSBANK', 'ROSBANK'),
        ('JSCB ROSBANK', 'ROSBANK'),
        ('PJSC ROSBANK', 'ROSBANK'),
        ('QIWI BANK (JSC)', 'QIWI'),
        ('QIWI BANK JSC', 'QIWI'),
        ('CJSC COMMERCIAL BANK CITIBANK', 'CITIBANK'),
        ('CITIBANK MOSCOW', 'CITIBANK'),
        ('BANK SIBIR CJSC', 'UNICREDIT'),
        ('CJSC UNICREDIT BANK', 'UNICREDIT'),
        ('ZAO RAIFFEISENBANK', 'RAIFFEISEN'),
        ('OOO RAIFFEISENBANK AUSTRIA', 'RAIFFEISEN'),
    ))
    def test_issuer_resolved_from_bank_names(
        self, issuer, expected_result, get_dummy_log_records, dummy_logger
    ):
        normalized_card_issuer = normalize_card_issuer(issuer, logger=dummy_logger, log_unknown=True)

        assert_that(normalized_card_issuer, equal_to(IssuerBank(expected_result)))
        assert_that(get_dummy_log_records(), has_length(0))

    @pytest.mark.parametrize('issuer, expected_result', (
        ('JSCB AK  BARS', 'AK_BARS'),
        ('JSCB AK-BARS', 'AK_BARS'),
        ('JOINT STOCK COMMERCIAL BANK AK BARS', 'AK_BARS'),
        ('zetasigma alfa-bank', 'ALFABANK'),
        ('ASIAN PACIFIC BANK OJSC', 'ATB'),
        ('ASIAN-PACIFIC BANK (PUBLIC JOINT STOCK COMPANY)', 'ATB'),
        ('ASIAN PACIFIC BANK', 'ATB'),
        ('ASIAN-PACIFIC BANK', 'ATB'),
        ('JOINT STOCK COMPANY ASIAN-PACIFIC BANK', 'ATB'),
        ('JSB AVANGARD', 'AVANGARD'),
        ('OJSC JSB ROSSIYA', 'BANKROSSIYA'),
        ('JS SAVINGS BANK BELARUSBANK', 'BELARUSBANK'),
        ('BELARUSSIAN BANK OF DEVELOPMENT AND RECONSTRUCTION BELINVESTBANK JSC', 'BELINVEST'),
        ('BELARUSSIAN BANK OF DEVELOPMENT AND RECONSTRUCTION BELINVEST', 'BELINVEST'),
        ('JSCB BINBANK', 'BINBANK'),
        ('BINBANK', 'BINBANK'),
        ('BANCO CITIBANK (PANAMA), S.A.', 'CITIBANK'),
        ('CJSC CREDIT EUROPE BANK', 'CREDIT_EUROPE'),
        ('LLC HOME CREDIT AND FINANCE BANK', 'HOME_CREDIT'),
        ('LIMITED LIABILITY COMPANY HOME CREDIT & FINANCE BANK', 'HOME_CREDIT'),
        ('KASPI BANK JSC', 'KASPI'),
        ('JSC KASPI BANK', 'KASPI'),
        ('BANK LEVOBEREZHNY', 'LEVOBEREZHNY'),
        ('JSCB MOSCOW INDUSTRIAL BANK', 'MINBANK'),
        ('JOINT STOCK COMMERCIAL BANK MOSCOW INDUSTRIAL BANK', 'MINBANK'),
        ('JS COMMERCIAL BANK MOSCOW INDUSTRIAL BANK', 'MINBANK'),
        ('CREDIT BANK OF MOSCOW (MKB BANK)', 'MKB'),
        ('MODULBANK', 'MODULBANK'),
        ('JOINT-STOCK COMPANY MINSK TRANSIT BANK', 'MTBANK'),
        ('OJSC MTS-BANK', 'MTSBANK'),
        ('MTSBANK', 'MTSBANK'),
        ('JSCB NOVIKOMBANK', 'NOVIKOMBANK'),
        ('CB OTKRITIE CJSC', 'OTKRITIE'),
        ('OTP BANKA HRVATSKA D.D.', 'OTPBANK'),
        ('OTP BANK NYRT.', 'OTPBANK'),
        ('BANKA OTP ALBANIA SH.A.', 'OTPBANK'),
        ('POCHTA BANK OJSC', 'POCHTABANK'),
        ('PRIORBANK JSCB', 'PRIORBANK'),
        ('OJSC PROMSVYAZBANK OJSC', 'PSB'),
        ('CJSC KAZKOMMERTSBANK TAJIKISTAN', 'QAZKOM'),
        ('QIWI BANK (JSC)', 'QIWI'),
        ('RAIFFEISENBANK (BULGARIA) AD', 'RAIFFEISEN'),
        ('PUBLIC JSC RAIFFEISEN BANK AVAL', 'RAIFFEISEN'),
        ('CB RENAISSANCE CREDIT (LLC)', 'RENAISSANCE'),
        ('RNKB', 'RNKB'),
        ('PJSC ROSBANK', 'ROSBANK'),
        ('JOINT STOCK COMPANY RUSSIAN AGRICULTURAL BANK', 'ROSSELKHOZ'),
        ('LLC BANK ROUND', 'ROUNDBANK'),
        ('JSC RUSSIAN STANDARD BANK', 'RUSSTANDARD'),
        ('SUBSIDIARY BANK SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK) PJSC', 'SBERBANK'),
        ('OJSC BPS-SBERBANK', 'SBERBANK'),
        ('JS CB OF SUPPORT TO COMMERCE AND BUSINESS SKB-BANK', 'SKB'),
        ('SKB-BANK', 'SKB'),
        ('PUBLIC JSC INVESTMENT COMMERCIAL BANK SOVCOMBANK', 'SOVCOMBANK'),
        ('SOVKOMBANK', 'SOVCOMBANK'),
        ('BANK SAINT PETERSBURG PUBLIC JOINT-STOCK COMPANY', 'SPBBANK'),
        ('BANK SAINT-PETERSBURG OJSC', 'SPBBANK'),
        ('TINKOFF BANK / YANDEX', 'TINKOFF'),
        ('TCS BANK (CJSC)', 'TINKOFF'),
        ('ENTERCARD (SWEDBANK)', 'SWEDBANK'),
        ('SWEDBANK, A.S.', 'SWEDBANK'),
        ('URAL BANK FOR RECONSTRUCTION AND DEVELOPMENT JSC', 'UBRR'),
        ('CJSC UNICREDIT BANK', 'UNICREDIT'),
        ('JOINT-STOCK COMPANY RUSSIAN REGIONAL DEVELOPMENT BANK (RRDB)', 'VBRR'),
        ('BANK VOZROZHDENIE OJSC', 'VOZROZHDENIE'),
        ('PUBLIC JSC VTB BANK', 'VTB'),
        ('YOOMONEY, NBCO LLC', 'YOOMONEY'),
    ))
    def test_issuer_resolved_from_regexp(
        self, issuer, expected_result, get_dummy_log_records, dummy_logger,
    ):
        normalized_card_issuer = normalize_card_issuer(issuer, logger=dummy_logger, log_unknown=True)

        assert_that(normalized_card_issuer, equal_to(IssuerBank(expected_result)))
        assert_that(get_dummy_log_records(), has_length(0))

    @pytest.mark.parametrize('issuer', (
        'UNKBANK',
        'MAK BARS',
        'BANK ALFALAH',
        'BANK ALFA LAH BANK, LTD.',
        'SOUTH EAST ASIAN BANK, LTD.',
        'JSB SOBINBANK',
        'UKRGAZPROMBANK OJSC',
        'CITIZENS BANK',
        'CITI CARDS CANADA',
        'CITI',
        'CB EUROCREDITBANK SA JSC',
        'HOMELAND CREDIT UNION, INC.',
        'HOME DEPOT CREDIT CARD',
        'CREDIT-MOSCOW BANK (CLOSED)',
        'MOSCOW-MINSK BANK OJSC',
        'ORIENT COMMERCIAL JSB',
        'ORIENTAL BANK',
        'ORIENT CORPORATION',
        'HEBROSBANK A.D.',
        'ABERDEEN PROVING GROUND F.C.U.',
        'DSKBANK',
        'SAINT PETERSBURG JSCB TAVRICHESKIY',
        'TINKER',
        'CREDIT URAL BANK',
    ))
    def test_issuer_is_unresolved(
        self, issuer, yandex_pay_settings, get_dummy_log_records, dummy_logger,
    ):
        normalized_card_issuer = normalize_card_issuer(issuer, logger=dummy_logger, log_unknown=True)

        assert_that(
            normalized_card_issuer,
            equal_to(IssuerBank.UNKNOWN),
        )
        assert_that(
            get_dummy_log_records(),
            all_of(
                has_length(1),
                contains(
                    has_properties(
                        message='Unknown issuer',
                        _context={'raw_card_issuer': issuer},
                    )
                )
            )
        )

    @pytest.mark.parametrize('issuer', (None, ''))
    def test_issuer_is_empty(
        self, issuer, yandex_pay_settings, get_dummy_log_records, dummy_logger
    ):
        normalized_card_issuer = normalize_card_issuer(issuer, log_unknown=True, logger=dummy_logger)

        assert_that(
            normalized_card_issuer,
            equal_to(IssuerBank.UNKNOWN),
        )
        assert_that(
            get_dummy_log_records(),
            all_of(has_length(1), contains(has_properties(message='Empty issuer')))
        )
