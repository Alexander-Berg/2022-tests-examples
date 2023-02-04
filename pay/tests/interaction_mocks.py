from mock import patch
import pytest


@pytest.fixture
def tracker_client():
    """
    :rtype: yb_darkspirit.interactions.TrackerClient
    """
    with patch('yb_darkspirit.interactions.TrackerClient', spec=True) as mock:
        mock.from_app.return_value = mock.return_value
        yield mock.return_value


@pytest.fixture
def documents_client():
    """
    :rtype: yb_darkspirit.interactions.DocumentsClient
    """
    with patch('yb_darkspirit.interactions.DocumentsClient', spec=True) as mock:
        mock.from_app.return_value = mock.return_value
        yield mock.return_value


@pytest.fixture
def ofd_private_client():
    """
    :rtype: yb_darkspirit.interactions.ofd.OfdPrivateClient
    """
    with patch('yb_darkspirit.interactions.OfdPrivateClient') as mocked_client:
        yield mocked_client


@pytest.fixture
def fnsreg_client():
    """
    :rtype: yb_darkspirit.interactions.FnsregClient
    """
    with patch('yb_darkspirit.interactions.FnsregClient', spec=True) as mock:
        mock.from_app.return_value = mock.return_value
        yield mock.return_value
