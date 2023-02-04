from wiki.grids.models.grid import Grid
from wiki.pages.models.page import Page

from intranet.wiki.tests.wiki_tests.common.factories.page import PageFactory


class GridFactory(PageFactory):
    class Meta:
        model = Grid

    page_type = Page.TYPES.GRID
