from static_api.schema import SCHEMAS, Schema

import os


class TestSchema(Schema):
    def _get_filename(self):
        return os.path.join(os.path.dirname(__file__), 'schemas', self.id + '.json')


SCHEMAS.register(TestSchema('person_test'))
SCHEMAS.register(TestSchema('common_test'))
