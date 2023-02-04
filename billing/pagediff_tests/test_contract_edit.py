# coding: utf-8

import pytest

import balance.balance_web as web
import btestlib.pagediff as pagediff
import filters

contracts = [
    ('general_post_no_agency_market_cpa', 183113),
    ('general_post_no_agency_mediaselling', 60690),
    ('general_pre_no_agency_mediaselling', 57551),
    ('general_pre_taxi', 263017),  ##BALANCE-22607
    ('general_post_taxi', 262135),  ##BALANCE-22607
    ('general_post_taxi_bv', 258986),
    ('general_pre_to_post_no_agency_market_cpa', 177002),
    ('general_post_no_agency_market_cpa_link_contract', 56122),
    pytest.mark.skip(('general_post_commiss', 48121), reason=u'долго открывается договор, 50 допников'),
    ('general_pre_commiss', 182896),
    ('general_post_commiss_market_cpa', 180519),
    ('general_post_commiss_universal_contract', 48191),
    pytest.mark.skip(('general_post_commiss_prolongation', 51539), reason=u'долго открывается договор, 55 допников'),
    ('general_post_pr_agency_discount', 181548),
    ('general_post_pr_agency_nonrez', 76256),
    ('general_pre_pr_agency', 65699),
    ('general_pre_opt_agency', 73074),
    ('general_post_opt_agency', 181503),
    ('general_post_opt_agency_mediaselling_kzt', 181599),
    ('general_pre_opt_agency_prem', 239409),
    ('general_post_opt_agency_prem', 227236),
    ('general_pre_opt_client_mediaselling', 67505),
    ('general_post_opt_client_mediaselling_direct', 62812),
    ('general_post_opt_client_mediaselling', 60198),
    pytest.mark.skip(('general_post_ua_opt_client_market_cpa', 181756), reason=u'отключили Украину'),
    pytest.mark.skip(('general_pre_ua_opt_client_direct', 179999), reason=u'отключили Украину'),
    pytest.mark.skip(('general_post_ua_opt_client_tickets', 183793), reason=u'отключили Украину'),
    pytest.mark.skip(('general_pre_ua_pr_agency', 178398), reason=u'отключили Украину'),
    pytest.mark.skip(('general_post_ua_opt_agency', 62770), reason=u'отключили Украину'),
    pytest.mark.skip(('general_pre_ua_opt_agency_prem', 205145), reason=u'отключили Украину'),
    pytest.mark.skip(('general_post_ua_opt_agency_prem', 237659), reason=u'отключили Украину'),
    pytest.mark.skip(('general_post_ua_commiss_direct_mediaselling', 182030), reason=u'отключили Украину'),
    pytest.mark.skip(('general_post_ua_commiss_direct', 180458), reason=u'отключили Украину'),
    pytest.mark.skip(('general_pre_ua_commiss', 202163), reason=u'отключили Украину'),
    ('general_post_usa_opt_client_direct', 68264),
    ('general_post_usa_opt_client_direct_mediaselling', 179759),
    ('general_pre_usa_opt_client', 226876),
    ('general_post_usa_opt_agency_direct', 68739),
    ('general_post_usa_opt_agency_direct_mediaselling', 65586),
    ('general_pre_usa_opt_agency', 205933),
    ('general_post_sw_opt_client_direct', 68215),
    ('general_post_sw_opt_client_direct_mediaselling', 69009),
    ('general_pre_sw_opt_client', 200118),
    ('general_post_sw_opt_agency_direct', 76637),
    ('general_post_sw_opt_agency_direct_mediaselling', 67888),
    ('general_pre_sw_opt_agency', 196757),
    ('general_post_tr_opt_agency', 200462),
    ('general_post_tr_opt_client', 233741),
    ('general_garant_rus', 177448),
    ('general_garant_bel', 177312),
    ('general_garant_ua', 179081),
    ('general_garant_kzt', 177806),
    ('general_brand', 180237),
    ('general_doverennost', 183351),
    ('general_post_auto_otp_agency_prem', 268775),
    ('general_pre_auto_no_agency', 268666),
    ('partners', 52999),
    ('geocontext', 66490),
    ('afisha', 69890),
    ('preferred_deal', 180008),
    ('distribution_revshare', 67043),
    ('distribution_universal_search', 177153),
    ('distribution_universal_revshare', 197501),
    ('distribution_agile_downloads', 205807),
    ('distribution_agile_all', 227807),
    ('distribution_downloads_nds18', 64224),
    ('distribution_downloads_nds0', 66481),
    ('distribution_revshare_nds0', 67836),
    ('distribution_universal_revshare_nds0', 72645),
    ('distribution_universal_downloads_nds0', 73910),
    ('distribution_group_parent', 257864),  ##BALANCE-22267 BALANCE-22792 BALANCE-22268
    ('distribution_universal_child', 257865),  ##BALANCE-22792 BALANCE-22268
    ('distribution_services_ag', 261828),  ##BALANCE-22626
    ('spendable_market_donate', 239791),
    ('spendable_corp_clients', 240673),
    ('spendable_taxi_donate', 252803)
]


@pytest.mark.parametrize('unique_name, contract_id', contracts,
                         ids=lambda unique_name, contract_id: unique_name)
def test_existing_contracts(unique_name, contract_id):
    page_html = pagediff.get_page(url=web.AdminInterface.ContractEditPage.url(contract_id=contract_id),
        prepare_page=lambda driver_, url_: web.AdminInterface.ContractEditPage.open_and_wait(driver_,contract_id ))
    pagediff.pagediff(unique_name=unique_name, page_html=page_html,
                      filters=filters.contract_edit_filters())

@pytest.mark.parametrize('unique_name, contract_id', contracts,
                         ids=lambda unique_name, contract_id: unique_name)
def test_existing_contracts_page_contract(unique_name, contract_id):
    page_html = pagediff.get_page(url=web.AdminInterface.ContractPage.url(contract_id=contract_id))
    pagediff.pagediff(unique_name='CP_' + unique_name, page_html=page_html, filters=filters.contract_edit_filters())
