
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import create_grid


class ColumnsDaoTestCase(WikiDjangoTestCase):
    def prepare_grid(self):
        return create_grid(
            """
{
  "fields": [
    {
      "name": "T",
      "type": "string"
    },
    {
      "name": "U",
      "type": "number"
    }
  ],
  "sorting": [],
  "width": "100%",
  "title": "Ff"
}
    """,
            tag='hello',
            owner=None,
        )

    def test_columns_generator(self):
        grid = self.prepare_grid()
        columns = list(grid.columns())
        self.assertEqual((0, 'string', {'name': 'T', 'required': False, 'sorting': True, 'type': 'string'}), columns[0])
        self.assertEqual(
            columns[1],
            (1, 'number', {'name': 'U', 'format': '%.2f', 'required': False, 'sorting': True, 'type': 'number'}),
        )
