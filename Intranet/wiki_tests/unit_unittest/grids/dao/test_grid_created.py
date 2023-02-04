
from datetime import datetime

from wiki.grids.dao import create_grid, grid_created
from wiki.users.models import User
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class GridWasCreatedTestCase(WikiDjangoTestCase):
    def test_creation(self):
        chapson = User.objects.create_user('chapson', 'chapson@yandex-team.ru')
        grid = create_grid('hello', 'hello', chapson)
        page_event = grid_created(chapson, datetime.now(), grid)
        self.assertTrue('revision_id' in page_event.meta)
