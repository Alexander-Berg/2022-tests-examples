from builtins import object, str

import pytest

from kelvin.certificates.models import CertificateContext, CertificateTemplate


class TestCertificateTemplate(object):
    """
    Тесты шаблона сертификатов
    """
    unicode_cases = (
        (
            CertificateTemplate(id=1, name='', template='<b>template</b>'),
            u' (1)'
        ),
        (
            CertificateTemplate(name='1', template='<b>template</b>'),
            u'1 (None)'
        ),
        (
            CertificateTemplate(name=u'имя', template='<b>template</b>', id=1),
            u'имя (1)'
        ),
    )

    @pytest.mark.parametrize('case,expected', unicode_cases)
    def test_unicode(self, case, expected):
        """
        Тесты строкового представления
        """
        assert str(case) == expected


class TestCertificateContext(object):
    """
    Тесты готовых сертификатов
    """
    unicode_cases = (
        (
            CertificateContext(id=1, course_id=2, template_id=3),
            u'Курс id 2, шаблон id 3',
        ),
        (
            CertificateContext(id=1, template_id=3),
            u'Курс id None, шаблон id 3',
        ),
        (
            CertificateContext(id=1, course_id=2,),
            u'Курс id 2, шаблон id None',
        ),
    )

    @pytest.mark.parametrize('case,expected', unicode_cases)
    def test_unicode(self, case, expected):
        """
        Тесты строкового представления
        """
        assert str(case) == expected
