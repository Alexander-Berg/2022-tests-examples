import json
from datetime import date, datetime
from decimal import Decimal


class CustomJSONEncoder(json.JSONEncoder):

    def default(self, obj):
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        elif isinstance(obj, Decimal):
            return str(obj)
        return super(CustomJSONEncoder, self).default(obj)


def json_dumps(v):
    return json.dumps(v, cls=CustomJSONEncoder)
