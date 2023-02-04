import xlrd
from django.conf import settings

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_STRUCTURE = '''
{
  "title" : "List of conferences",
  "width" : "100%%",
  "sorting" : [
    {"name" : "name", "type" : "desc"},
    {"name" : "date", "type" : "desc"}
  ],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference",
      "width" : "200px",
      "sorting" : true,
      "required" : true
    },
    {
      "name" : "member",
      "title" : "Member",
      "sorting" : false,
      "required" : false,
      "type" : "staff",
      "multiple" : false
    },
    {
      "name" : "members",
      "title" : "Members",
      "sorting" : false,
      "required" : false,
      "type" : "staff",
      "multiple" : true
    },
    {
      "name" : "attribute",
      "title" : "Attribute",
      "sorting" : false,
      "required" : false,
      "type" : "select",
      "multiple" : false,
      "options": ["1", "2", "3"]
    },
    {
      "name" : "attributes",
      "title" : "Attributes",
      "sorting" : false,
      "required" : false,
      "type" : "select",
      "multiple" : true,
      "options": ["1", "2", "3"]
    },
    {
      "name" : "is_done",
      "title" : "Is done?",
      "sorting" : true,
      "required" : false,
      "type" : "checkbox",
      "markdone" : true
    }%s
  ]
}
''' % (
    ''',
    {
      "title": "Ticket",
      "sorting": true,
      "type": "ticket",
      "name": "ticket",
      "required": false
    },
    {
      "title": "Status",
      "required": false,
      "type": "ticket-status",
      "sorting": true,
      "name": "ticket_status"
    }
'''
    if settings.IS_INTRANET
    else '',
)


class GridsExportTest(BaseGridsTest):
    def setUp(self):
        super(GridsExportTest, self).setUp()

        supertag = 'grid'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        row_data = dict(
            name='some name',
            member=['bolk'],
            members=['bolk', 'thasonic'],
            attribute=['1'],
            attributes=['2', '3'],
            is_done='true',
        )
        if settings.IS_INTRANET:
            row_data['ticket'] = 'STARTREK-100'
        self._add_row(supertag, row_data)

        row_data = dict(
            name='111',
            member=['bolk'],
            members=['bolk', 'thasonic'],
            attribute=['1'],
            attributes=['2', '3'],
            is_done='true',
        )
        if settings.IS_INTRANET:
            row_data['ticket'] = 'STARTREK-100'
        self._add_row(supertag, row_data)

    def test_export_csv(self):
        supertag = 'grid'
        response = self.client.get('/_api/frontend/{0}/.grid/export/csv'.format(supertag))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'text/csv')
        self.assertEqual(
            response['Content-Disposition'], 'attachment; filename="grid.csv"; filename*="UTF-8\'\'grid.csv"'
        )

        valid_csv = """Name of conference,Member,Members,Attribute,Attributes,Is done?{ticket_heads}\r
some name,bolk (bolk),"bolk (bolk), Александр Покатилов (thasonic)",1,"2, 3",True{ticket_data}\r
111,bolk (bolk),"bolk (bolk), Александр Покатилов (thasonic)",1,"2, 3",True{ticket_data}\r
""".format(
            ticket_data=',STARTREK-100,Closed' if settings.IS_INTRANET else '',
            ticket_heads=',Ticket,Status' if settings.IS_INTRANET else '',
        )

        content = response.content.decode('utf-8')
        self.assertEqual(content, valid_csv)

    def test_export_xls(self):
        supertag = 'grid'
        response = self.client.get('/_api/frontend/{0}/.grid/export/xls'.format(supertag))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/vnd.ms-excel')
        self.assertEqual(
            response['Content-Disposition'], 'attachment; filename="grid.xls"; filename*="UTF-8\'\'grid.xls"'
        )

        content = response.content

        workbook = xlrd.open_workbook(file_contents=content)
        sheet = workbook.sheet_by_index(0)
        serialized_sheet = [[sheet.cell_value(i, j) for j in range(6)] for i in range(2)]

        valid_serialized_sheet = [
            ['Name of conference', 'Member', 'Members', 'Attribute', 'Attributes', 'Is done?'],
            ['some name', 'bolk (bolk)', 'bolk (bolk), Александр Покатилов (thasonic)', '1', '2, 3', '✓'],
        ]

        self.assertEqual(serialized_sheet, valid_serialized_sheet)
