import re
import datetime

import pytest
from freezegun import freeze_time

from django.conf import settings
from django.utils import timezone

from intranet.crt.constants import CERT_TYPE, CERT_STATUS
from intranet.crt.core.models import Certificate
from intranet.crt.tasks.import_internal_ca_certs import ImportInternalCaCertsTask
from intranet.crt.tasks.sync_crl import SyncCrlTask
from __tests__.tasks.internal_ca_certs_data import all_data, dated_data
from __tests__.utils.common import create_certificate, capture_raw_http, ResponseMock
from __tests__.utils.ssl import CrlBuilder

pytestmark = pytest.mark.django_db

crl_data = (
    CrlBuilder()
    .set_last_update(timezone.now())
    .add_cert('3E5A79CA000100017F89', hold=True)
    .add_cert('1FC214130001000159CB')
    .get_der_bytes()
)


def dated_content(sync_all=False):
    def _dated_content(method, url, **kwargs):
        if url == settings.INTERNAL_CA_CRL:
            return ResponseMock(crl_data)

        if sync_all:
            return ResponseMock(all_data)

        assert method == 'GET'

        url_pattern = re.compile(settings.CRT_INTERNAL_CA_EXPORT_DIFF_URL_PATTERN.replace('{}', '(\d+)'))
        match = url_pattern.match(url)
        assert match is not None

        data = match.group(1)
        assert data in dated_data

        content = dated_data[data]

        return ResponseMock(content)

    return _dated_content


@pytest.fixture
def certificates(crt_robot, certificate_types):
    type_imdm = certificate_types[CERT_TYPE.IMDM]
    create_certificate(crt_robot, type_imdm, serial_number='182503E900000000000C')
    create_certificate(crt_robot, type_imdm, serial_number='1F2F8BBA000000000011')

    type_win_pc_auto = certificate_types[CERT_TYPE.WIN_PC_AUTO]
    create_certificate(crt_robot, type_win_pc_auto, serial_number='1450D0FB00000000001B')

    type_zombie = certificate_types[CERT_TYPE.ZOMBIE]
    create_certificate(crt_robot, type_zombie, serial_number='3389A810000100015A93', status=CERT_STATUS.HOLD)

    type_vpn_token = certificate_types[CERT_TYPE.VPN_TOKEN]
    create_certificate(crt_robot, type_vpn_token, serial_number='615EDECB000100018109')


def get_certs(imported=None):
    return set(
        Certificate.objects
        .imported(imported)
        .values_list('type__name', 'serial_number', 'status', 'user__username')
    )


def check_no_imported_certs():
    assert get_certs(imported=False) == {
        (CERT_TYPE.IMDM, '182503E900000000000C', CERT_STATUS.ISSUED, 'robot-crt'),
        (CERT_TYPE.IMDM, '1F2F8BBA000000000011', CERT_STATUS.ISSUED, 'robot-crt'),
        (CERT_TYPE.WIN_PC_AUTO, '1450D0FB00000000001B', CERT_STATUS.ISSUED, 'robot-crt'),
        (CERT_TYPE.ZOMBIE, '3389A810000100015A93', CERT_STATUS.HOLD, 'robot-crt'),
        (CERT_TYPE.VPN_TOKEN, '615EDECB000100018109', CERT_STATUS.ISSUED, 'robot-crt'),
    }


def test_internal_ca_certs_full_sync(settings, certificates, users):
    settings.CRT_IN_SLICE_COUNT = 1

    with capture_raw_http(side_effect=dated_content(sync_all=True)):
        ImportInternalCaCertsTask().run(full_sync=True)

    check_no_imported_certs()

    assert get_certs(imported=True) == {
        (CERT_TYPE.IMDM, '31B11627000000000016', CERT_STATUS.ISSUED, 'shelby'),
        (CERT_TYPE.IMDM, '33AE01D3000000000003', CERT_STATUS.EXPIRED, 'hot_dog'),
        (CERT_TYPE.WIN_PC_AUTO, '612CFA65000000000017', CERT_STATUS.EXPIRED, None),
        (CERT_TYPE.WIN_PC_AUTO, '1A91A2BB00000000001F', CERT_STATUS.ISSUED, None),
        (CERT_TYPE.ZOMBIE, '1FC214130001000159CB', CERT_STATUS.REVOKED, 'zomb-user'),
        (CERT_TYPE.ZOMBIE, '1A418F6D0001000075CF', CERT_STATUS.EXPIRED, 'zomb-user'),
        (CERT_TYPE.ZOMBIE, '3E7048350001000147E9', CERT_STATUS.ISSUED, 'zomb-user'),
        (CERT_TYPE.VPN_TOKEN, '3E5A79CA000100017F89', CERT_STATUS.HOLD, 'shelby'),
        (CERT_TYPE.VPN_TOKEN, '3D84806A000100017F56', CERT_STATUS.ISSUED, 'bubblegum'),
        (CERT_TYPE.WIN_WH_SHARED, '1100250AEB66619D8935B276E0000100250AEB', CERT_STATUS.ISSUED, None),
        (CERT_TYPE.WIN_WH_SHARED, '1100250AEC4A44F1747E05B4D4000100250AEC', CERT_STATUS.ISSUED, None),
    }


def test_internal_ca_certs_base_sync(settings, certificates, users):
    settings.CRT_IN_SLICE_COUNT = 1

    with freeze_time(timezone.utc.localize(datetime.datetime(2018, 1, 28))):
        with capture_raw_http(side_effect=dated_content()):
            ImportInternalCaCertsTask().run(full_sync=False)

    check_no_imported_certs()

    assert get_certs(imported=True) == {
        (CERT_TYPE.IMDM, '31B11627000000000016', CERT_STATUS.ISSUED, 'shelby'),
        (CERT_TYPE.IMDM, '33AE01D3000000000003', CERT_STATUS.EXPIRED, 'hot_dog'),
        (CERT_TYPE.WIN_PC_AUTO, '612CFA65000000000017', CERT_STATUS.EXPIRED, None),
        (CERT_TYPE.ZOMBIE, '1A418F6D0001000075CF', CERT_STATUS.EXPIRED, 'zomb-user'),
        (CERT_TYPE.ZOMBIE, '3E7048350001000147E9', CERT_STATUS.ISSUED, 'zomb-user'),
        (CERT_TYPE.VPN_TOKEN, '3E5A79CA000100017F89', CERT_STATUS.HOLD, 'shelby'),
    }


def test_internal_ca_certs_from_sync(settings, certificates, users):
    settings.CRT_IN_SLICE_COUNT = 1

    date = timezone.utc.localize(datetime.datetime(2018, 1, 29))
    with freeze_time(date):
        with capture_raw_http(side_effect=dated_content()):
            ImportInternalCaCertsTask().run(full_sync=False, from_date=date)

    check_no_imported_certs()

    assert get_certs(imported=True) == {
        (CERT_TYPE.WIN_PC_AUTO, '1A91A2BB00000000001F', CERT_STATUS.ISSUED, None),
        (CERT_TYPE.ZOMBIE, '1FC214130001000159CB', CERT_STATUS.REVOKED, 'zomb-user'),
        (CERT_TYPE.VPN_TOKEN, '3D84806A000100017F56', CERT_STATUS.ISSUED, 'bubblegum'),
    }


def test_sync_crl(crt_robot, certificates, certificate_types):
    now_date = timezone.now() + datetime.timedelta(seconds=1)
    with freeze_time(now_date):
        create_certificate(
            crt_robot, certificate_types[CERT_TYPE.IMDM],
            serial_number='1F2F8BBA0AAA00000011',
            status=CERT_STATUS.HOLD,
            revoked=now_date + datetime.timedelta(seconds=1),
        )

        sync_crl_data = (
            CrlBuilder()
            .set_last_update(now_date)
            .add_cert('182503E900000000000C')
            .add_cert('1450D0FB00000000001B')
            .add_cert('615EDECB000100018109', hold=True)
            .get_der_bytes()
        )

        with capture_raw_http(answer=sync_crl_data):
            SyncCrlTask().run()

        assert get_certs(imported=False) == {
            (CERT_TYPE.IMDM, '1F2F8BBA0AAA00000011', CERT_STATUS.HOLD, 'robot-crt'),
            (CERT_TYPE.ZOMBIE, '3389A810000100015A93', CERT_STATUS.ISSUED, 'robot-crt'),
            (CERT_TYPE.IMDM, '1F2F8BBA000000000011', CERT_STATUS.ISSUED, 'robot-crt'),
            (CERT_TYPE.IMDM, '182503E900000000000C', CERT_STATUS.REVOKED, 'robot-crt'),
            (CERT_TYPE.WIN_PC_AUTO, '1450D0FB00000000001B', CERT_STATUS.REVOKED, 'robot-crt'),
            (CERT_TYPE.VPN_TOKEN, '615EDECB000100018109', CERT_STATUS.HOLD, 'robot-crt'),
        }
