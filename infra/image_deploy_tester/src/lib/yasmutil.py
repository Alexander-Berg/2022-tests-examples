import json
import requests


def push_to_yasm(url, values, tags, ttl):
    """
    :type url: str
    :type values: dict[str, int]
    :type tags: dict[str, str]
    :type ttl: int
    """
    signals = [{'name': name, 'val': value} for name, value in values.items()]
    data = {
        "tags": tags,
        "values": signals,
        "ttl": ttl,
    }
    resp = requests.post(url, data=json.dumps([data]))
    resp.raise_for_status()
