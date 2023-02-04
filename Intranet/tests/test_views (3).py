from builtins import object

from mock import MagicMock, call

from rest_framework.response import Response

from kelvin.certificates.models import CertificateContext, RenderedCertificate
from kelvin.certificates.serializers import RenderedCertificateSerializer
from kelvin.certificates.views import RenderedCertificateViewSet


class TestCertificateViewSet(object):
    """
    Тесты вьюсета сертификатов
    """
    def test_my(self, mocker):
        """Тест получения собственных сертификатов"""
        mocked_get_queryset = mocker.patch.object(
            RenderedCertificateViewSet, 'get_queryset')
        mocked_filter_queryset = mocker.patch.object(
            RenderedCertificateViewSet, 'filter_queryset')
        mocked_get_serializer = mocker.patch.object(
            RenderedCertificateViewSet, 'get_serializer')
        mocked_get_serializer.return_value = RenderedCertificateSerializer(
            many=True)
        mocked_request = MagicMock()

        viewset = RenderedCertificateViewSet()

        mocked_filter_queryset.return_value.filter.return_value = [
            RenderedCertificate(
                file=MagicMock(url='cert_url1'),
                user_id=1,
                context=CertificateContext(name='name1',
                                           additional_context={'color': 'a'}),
            ),
            RenderedCertificate(
                file=MagicMock(url='cert_url2'),
                user_id=1,
                context=CertificateContext(name='name2',
                                           additional_context={'color': 'b'}),
            ),
        ]

        response = viewset.my(mocked_request)
        assert isinstance(response, Response)
        assert response.data == [
            {
                'file': 'cert_url1',
                'name': 'name1',
                'additional_context': {'color': 'a'},
            },
            {
                'file': 'cert_url2',
                'name': 'name2',
                'additional_context': {'color': 'b'},
            },
        ]
        assert mocked_get_serializer.mock_calls == [
            call(many=True),
        ]
        assert mocked_get_queryset.mock_calls == [
            call(),
        ]
        assert mocked_filter_queryset.mock_calls == [
            call(mocked_get_queryset.return_value),
            call().filter(user=mocked_request.user),
        ]
