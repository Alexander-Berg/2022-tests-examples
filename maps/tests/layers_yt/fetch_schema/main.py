import json
import os
import urllib.request

from maps.pylibs.utils.lib.common import retry
from sandbox.common import rest


@retry(tries=3)
def _fetch_schema():
    params = {
        "type": "MAPS_RENDERER_DENORMALIZATION_YT_SCHEMA",
        "limit": 1,
        "state": "READY",
        "order": "-time.created",
        "attrs": json.dumps({
            "garden_environment": os.environ["garden_environment"],
            "vendor": "yandex",
        })
    }

    sandbox = rest.Client()
    resource = sandbox.resource.read(params)["items"]
    assert len(resource) == 1

    url = resource[0]["http"]["proxy"]
    filename = resource[0]["file_name"]
    urllib.request.urlretrieve(url, filename)


def main():
    _fetch_schema()
