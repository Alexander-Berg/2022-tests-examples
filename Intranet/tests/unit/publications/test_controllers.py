import pytest

from intranet.femida.src.publications.controllers import archive_vacancy_publications
from intranet.femida.src.publications.choices import PUBLICATION_STATUSES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('status, target', (
    (PUBLICATION_STATUSES.draft, PUBLICATION_STATUSES.draft),
    (PUBLICATION_STATUSES.published, PUBLICATION_STATUSES.archived),
))
def test_archive_vacancy_publications(status, target):
    vacancy = f.VacancyFactory()
    publication = f.PublicationFactory(
        vacancy=vacancy,
        status=status,
    )
    archive_vacancy_publications(vacancy)
    publication.refresh_from_db()
    assert publication.status == target
