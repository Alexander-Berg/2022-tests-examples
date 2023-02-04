'''
Created on 14.07.2011

@author: chapson
'''
import datetime

import pytz

from wiki import access as wiki_access
from wiki.grids.models import Grid
from wiki.grids.utils import insert_rows, ticket_field_names
from wiki.pages.models import Page
from wiki.utils.supertag import translit
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [
    {"name" : "name", "type" : "asc"},
    {"name" : "date", "type" : "desc"}
    ],
  "done" : true,
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference",
      "width" : "200px",
      "sorting" : true,
      "required" : true
    },
    {
      "name" : "date",
      "title" : "Date of conference",
      "sorting" : false,
      "required" : false,
      "type" : "date"
    },
    {
      "name" : "is_done",
      "title" : "Is done?",
      "sorting" : true,
      "required" : false,
      "type" : "checkbox",
      "markdone" : true
    }
  ]
}
"""

NEW_GRID_STRUCTURE = """
{
  "title": "List of conference",
  "width": "99%",
  "sorting": [
    {"name" : "name", "type" : "asc"},
    {"name" : "date", "type" : "desc"},
    {"name" : "does_not_exist", "type" : "asc"}
    ],
  "done": true,
  "fields": [
    {
      "name" : "date",
      "title" : "held at",
      "sorting" : true,
      "required" : true,
      "type" : "date",
      "format" : "Y-m-d"
    },
    {
      "name" : "participants",
      "title" : "",
      "type" : "staff"
    }
  ]
}
"""

GRID_WITHOUT_FIELDS = """
{
    "title" : "List of conferences",
    "width" : "100%",
    "sorting" : [
        {"name" : "name", "type" : "asc"},
        {"name" : "date", "type" : "desc"},
        {"name" : "does_not_exist", "type" : "asc"}
        ],
    "done" : true
}
"""

CHECKBOX_GRID = """
{
  "title": "grid of checkboxes",
  "sorting": [],
  "fields": [
    {
      "name" : "done",
      "title" : "Is done?",
      "sorting" : true,
      "required" : false,
      "markdone" : true,
      "type" : "checkbox"
    }
  ]
}
"""

GRID_TICKET_STRUCTURE = """
{
  "title": "List of tickets",
  "sorting": [],
  "done": true,
  "fields": [
    {
      "name" : "todo",
      "title" : "job",
      "type" : "string"
    },
    {
      "name" : "ticket",
      "title" : "Ticket",
      "sorting" : true,
      "required" : true,
      "type" : "ticket"
    }
  ]
}
"""

GRID_WITH_ALL_KIND_OF_FIELDS_STRUCTURE = """
{
  "fields": [
    {
      "title": "X",
      "sorting": true,
      "type": "string",
      "name": "0",
      "required": false
    },
    {
      "sorting": true,
      "name": "1",
      "title": "Y",
      "format": "%.2f",
      "required": false,
      "type": "number"
    },
    {
      "markdone": true,
      "sorting": true,
      "name": "2",
      "title": "Z",
      "required": false,
      "type": "checkbox"
    },
    {
      "sorting": true,
      "name": "3",
      "title": "W",
      "format": "d.m.Y",
      "required": false,
      "type": "date"
    },
    {
      "sorting": true,
      "multiple": false,
      "name": "4",
      "title": "V",
      "required": false,
      "type": "select",
      "options": [
        "V1",
        "V2",
        "V3"
      ]
    },
    {
      "sorting": false,
      "multiple": true,
      "name": "5",
      "title": "U",
      "required": false,
      "type": "select",
      "options": [
        "U1",
        "U2",
        "U3"
      ]
    },
    {
      "sorting": true,
      "multiple": false,
      "name": "6",
      "format": "i_first_name i_last_name",
      "required": false,
      "title": "T",
      "type": "staff"
    },
    {
      "sorting": false,
      "multiple": true,
      "name": "7",
      "format": "i_first_name i_last_name",
      "required": false,
      "title": "S",
      "type": "staff"
    },
    {
      "title": "R",
      "sorting": true,
      "type": "ticket",
      "name": "8",
      "required": false
    },
    {
      "title": "Q",
      "required": false,
      "type": "ticket-subject",
      "sorting": true,
      "name": "9"
    }
  ],
  "sorting": [],
  "done": false,
  "width": "100%",
  "title": "Ff"
}
"""


class BaseGridsTest(BaseTestCase):
    def setUp(self):
        super(BaseGridsTest, self).setUp()
        self.setUsers()
        grid = Grid()
        grid.change_structure(GRID_STRUCTURE)
        grid.tag = 'thasonic/грид'
        grid.supertag = 'thasonic/grid'
        grid.page_type = Page.TYPES.GRID
        grid.status = 1
        grid.last_author = self.user_thasonic
        grid.modified_at = datetime.datetime(2001, 1, 1)
        data = [
            {
                'name': 'Sussex search',
                'date': '2010-05-10',
                'is_done': False,
            },
            {
                'name': 'iCode',
                'date': '2011-06-30',
                'is_done': True,
            },
        ]
        self.hash1, self.hash = insert_rows(grid, data, None)
        grid.save()
        grid.authors.add(self.user_thasonic)
        self.grid = grid

        grid1 = Grid()
        grid1.change_structure(GRID_STRUCTURE)
        grid1.tag = 'thasonic/грид1'
        grid1.supertag = 'thasonic/grid1'
        grid1.page_type = Page.TYPES.GRID
        grid1.status = 1
        grid1.save()
        grid1.authors.add(self.user_chapson)
        wiki_access.set_access(grid1, wiki_access.TYPES.OWNER, self.user_chapson)
        self.grid1 = grid1

        grid2 = Grid()
        grid2.change_structure(GRID_STRUCTURE)
        grid2.page_type = Page.TYPES.GRID
        grid2.tag = 'thasonic/грид2'
        grid2.supertag = 'thasonic/grid2'
        grid2.status = False
        grid2.save()
        grid2.authors.add(self.user_thasonic)
        self.grid2 = grid2

        grid = Grid()
        grid.change_structure(CHECKBOX_GRID)
        grid.tag = 'thasonic/checkboxgrid'
        grid.supertag = 'thasonic/checkboxgrid'
        grid.page_type = Page.TYPES.GRID
        grid.status = 1
        grid.last_author = self.user_thasonic
        grid.modified_at = datetime.datetime(2001, 1, 1)
        insert_rows(
            grid,
            [
                {'done': True},
                {'done': False},
            ],
            None,
        )
        grid.save()
        grid.authors.add(self.user_thasonic)
        self.checkbox_grid = grid

        self.ticket_grid = self.grid_ticket()

        self.client.login('thasonic')

    def grid_ticket(self):
        grid = Grid()
        grid.change_structure(GRID_TICKET_STRUCTURE)
        grid.handler = 'grid'
        grid.status = 1
        grid.user = 'thasonic'
        grid.supertag = 'developer/plans'
        grid.tag = 'Developer/Планс'
        grid.time = datetime.datetime(2001, 1, 1)
        grid.save()
        grid.authors.add(self.user_thasonic)
        return grid


def create_grid(structure, tag, owner, data=None, **params):
    grid = Grid()
    grid.change_structure(structure)
    grid.tag = tag
    grid.supertag = translit(tag)
    grid.title = params.get('title', tag)
    grid.last_author = owner
    grid.page_type = Page.TYPES.GRID
    grid.status = 1
    grid.modified_at = datetime.datetime(2001, 1, 1, tzinfo=pytz.utc)
    grid.formatter_version = params.get('formatter_version', '10')
    grid.description = params.get('description', 'i am grid')
    grid.keywords = params.get('keywords', 'grid')
    if data:
        insert_rows(grid, data, None)
    grid.save()
    if owner:
        grid.authors.add(owner)
    return grid


def default_row(grid, values_dict=None):
    field_names = list(grid.column_names_by_type())
    return dict(((n, '') for n in field_names), **(values_dict or {}))


GRID_FIELD = """
    {
      "name" : "%s",
      "title" : "%s",
      "type" : "%s"
    }"""

TICKET_GRID = """
{
  "title" : "List of tickets",
  "width" : "100%%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "ticket",
      "title" : "The ticket",
      "type" : "ticket"
    },
%s
  ]
}
""" % ','.join(
    GRID_FIELD % (name[0].replace('ticket-', ''), 'The ' + name[0], name[0]) for name in ticket_field_names.choices()
)
