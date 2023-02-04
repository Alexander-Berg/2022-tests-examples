import pytest

from bcl.banks.protocols.paypal.common import ReportCommon
from bcl.banks.protocols.paypal.enums import T_MASS_PAYMENT
from bcl.banks.protocols.paypal.exceptions import NoDataForReport
from bcl.banks.protocols.paypal.report_settlement import SettlementReport


def test_get_description_tcode():
    assert ReportCommon.get_description_tcode(T_MASS_PAYMENT) == 'MassPay Payment'


def test_no_data_for_report(sftp_client):

    with pytest.raises(NoDataForReport):
        sftp_client(files_contents={})
        return SettlementReport(SettlementReport.get_remote_data('ru'))
