
from wiki.pages.dao.revision import get_revision_by_id
from wiki.pages.models import Revision
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class RevisionDaoTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_revision_returned(self):
        self.create_page()
        revision = Revision.objects.all()[0]
        self.assertEqual(revision, get_revision_by_id(revision.id))

    def test_no_such_revision(self):
        self.assertEqual(None, get_revision_by_id(1))
