from bcl.banks.party_raiff import QuarterReportLetter
from bcl.banks.registry import Raiffeisen
from bcl.core.models import Letter


def test_template_context(request_factory, init_user):

    request = request_factory.get('/here')
    request.user = init_user(name_ru='тестовый поль', telephone='1020')

    letter = QuarterReportLetter(Letter(
        associate_id=Raiffeisen.id,
    ))

    context = letter.get_tpl_dict(
        request=request,
        request_context={'base': 1}
    )
    body = context['body']
    assert 'тестовый поль' in body
    assert '1020' in body
