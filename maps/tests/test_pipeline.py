from yandex.maps.proto.bizdir.common.business_pb2 import CompanyState
from yandex.maps.proto.bizdir.sps.business_pb2 import Business
from yandex.maps.proto.bizdir.sps.business_diff_pb2 import (
    BusinessDiff,
    ApprovedCompanyState,
    AddBusinessDiffResponse,
    BusinessDiffResult
)
from yandex.maps.proto.bizdir.sps.signal_pb2 import Signal, SignalResult

from maps.doc.proto.testhelper.validator import Validator

COMPANY_ID = '115421291463'

validator = Validator('bizdir/sps')


def test_fb_api_to_sps():
    message = Signal(
        company_id=COMPANY_ID,
        company=Business(
            company_state=CompanyState.CLOSED
        ),
        comment='Довольно долго висела вывеска о скором открытии,'
            ' сейчас вместо неё висит вывеска магазина спорттоваров.'
    )

    validator.validate_example(message, '01_fb_api_to_sps.prototxt')


def test_sps_to_sprav():
    message = BusinessDiff(
        company_id=COMPANY_ID,
        company_state=ApprovedCompanyState(
            value=CompanyState.CLOSED,
            approval_timestamp=1622901158
        ),
        signal_id=['sps1://fbapi?id=2ba1ab2834f5e7da7184d4076127bbfb']
    )

    validator.validate_example(message, '02_sps_to_sprav.prototxt')


def test_sprav_to_sps_add():
    message = AddBusinessDiffResponse(
        business_diff_id='888615ad3d5f145b7812a42160bb8329'
    )

    validator.validate_example(message, '03_sprav_to_sps.prototxt')


def test_sprav_to_sps_result():
    message = BusinessDiffResult(accepted=BusinessDiffResult.Accept())

    validator.validate_example(message, '04_sprav_to_sps.prototxt')


def test_sps_to_fb_api_result():
    message = SignalResult(accepted=SignalResult.Accept())

    validator.validate_example(message, '05_sps_to_fb_api.prototxt')
