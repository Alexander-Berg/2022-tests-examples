import pytest

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.candidates.contacts import parse_contact_id


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('hh_contact,hh_id', [
    ('hh.ru/resume/1ab2cd3e', '1ab2cd3e'),
    ('https://hh.ru/resume/1ab2cd3e/?query=IT', '1ab2cd3e'),
    ('https://saransk.hh.ru/resume/1ab2cd3e', '1ab2cd3e'),
    ('https://headhunter.kg/resume/1ab2cd3e', '1ab2cd3e'),
    ('https://jobs.tut.by/resume/1ab2cd3e', '1ab2cd3e'),
    ('HH.RU/RESUME/1AB2CD3E', '1AB2CD3E'),
    ('hh.ru/1ab2cd3e', None),
    ('best-candidate@yandex.ru', None),
    ('1ab2cd3e', None),
    ('', None),
])
def test_parse_hh_id(hh_contact, hh_id):
    assert parse_contact_id(CONTACT_TYPES.hh, hh_contact) == hh_id


@pytest.mark.parametrize('ah_contact,ah_id', [
    ('search.amazinghiring.com/profiles/12345', '12345'),
    ('search.amazinghiring.com/profiles/12345?q=text', '12345'),
    ('https://search.amazinghiring.com/profiles/12345', '12345'),
    ('www.search.amazinghiring.com/profiles/12345', '12345'),
    ('SEARCH.AMAZINGHIRING.COM/PROFILES/12345', '12345'),
    ('search.amazinghiring.com/profiles/abcde', None),
    ('search.amazinghiring.ru/profiles/12345', None),
    ('amazinghiring.com/profiles/12345', None),
    ('search.amazinghiring.com/12345', None),
    ('12345', None),
    ('', None),
])
def test_parse_ah_id(ah_contact, ah_id):
    assert parse_contact_id(CONTACT_TYPES.ah, ah_contact) == ah_id


@pytest.mark.parametrize('linkedin_contact,linkedin_id', [
    ('linkedin.com/in/1a2345', '1a2345'),
    ('linkedin.com/in/candidate-1a2345', 'candidate-1a2345'),
    ('linkedin.com/in/кандидат-1a2345', 'кандидат-1a2345'),
    ('linkedin.com/in/ёкандидат-1a2345', 'ёкандидат-1a2345'),
    ('linkedin.com/in/candidate-1a2345?q=text', 'candidate-1a2345'),
    ('https://linkedin.com/in/candidate-1a2345', 'candidate-1a2345'),
    ('www.linkedin.com/in/candidate-1a2345', 'candidate-1a2345'),
    ('LINKEDIN.COM/IN/CANDIDATE-1A2345', 'CANDIDATE-1A2345'),
    ('linkedin.ru/in/candidate-1a2345', None),
    ('linkedin.com/candidate-1a2345', None),
    ('candidate-1a2345', None),
    ('', None),
    (
        'linkedin.com/in/ҐґІіЇїЄєěşŞıİçÇöÖüÜĞğÄäáßĆćČčŁłŃńŚśŠšŹźŽžŬŭ',
        'ҐґІіЇїЄєěşŞıİçÇöÖüÜĞğÄäáßĆćČčŁłŃńŚśŠšŹźŽžŬŭ',
    ),
])
def test_parse_linkedin_id(linkedin_contact, linkedin_id):
    assert parse_contact_id(CONTACT_TYPES.linkedin, linkedin_contact) == linkedin_id
