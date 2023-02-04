import pytest

from unittest.mock import patch

from intranet.femida.src.candidates.choices import VERIFICATION_RESOLUTIONS, VERIFICATION_STATUSES
from intranet.femida.src.candidates.verifications.controllers import handle_verification_resolution
from intranet.femida.src.startrek.utils import ResolutionEnum
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f


EXPIRATION_DATE = shifted_now(months=3)
INFINITE_DATE = shifted_now(years=1000)


@pytest.mark.parametrize('issue_resolution, verification_resolution', (
    (ResolutionEnum.fixed, VERIFICATION_RESOLUTIONS.hire),
    (ResolutionEnum.declined, VERIFICATION_RESOLUTIONS.nohire),
    (ResolutionEnum.wont_fix, VERIFICATION_RESOLUTIONS.blacklist),
))
@patch(
    'intranet.femida.src.candidates.verifications.controllers.'
    'VerificationFailureNotification.send'
)
@patch(
    'intranet.femida.src.candidates.verifications.controllers.'
    'VerificationSuccessNotification.send'
)
def test_handle_verification_resolution(mocked_success, mocked_failure,
                                        issue_resolution, verification_resolution):
    verification = f.VerificationFactory()
    verification = handle_verification_resolution(verification, issue_resolution)

    assert verification.status == VERIFICATION_STATUSES.closed
    assert verification.resolution == verification_resolution
    if verification_resolution == VERIFICATION_RESOLUTIONS.blacklist:
        assert verification.expiration_date > INFINITE_DATE
    else:
        assert verification.expiration_date > EXPIRATION_DATE
    if verification_resolution == VERIFICATION_RESOLUTIONS.hire:
        assert mocked_success.called
    else:
        assert mocked_failure.called
