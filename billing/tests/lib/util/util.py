def merge(source: dict, destination: dict) -> dict:
    res = dict(source)
    for key, value in destination.items():
        if isinstance(value, dict):
            res[key] = merge(res.get(key) or {}, value)
        else:
            res[key] = value

    return res


def deep_update(target: dict, update_data: dict) -> dict:
    """
    Recursively update target dict with data from update_data
    >>> event = { "id": 123, "service_id": 1234, "rows": [{"amount": None, "id": 1}, {"amount": None, "id": 2}]}
    >>> updated_fields = {"service_id": 1000, "extra_key": 123, "rows": [{"amount": 10.00}, {"amount": 15.00}]}
    >>> deep_update(target=event, update_data=updated_fields)
    """
    for key, val in update_data.items():
        if key in target:
            if isinstance(val, dict):
                target[key] = deep_update(target[key], val)
            if isinstance(val, list):
                if val and isinstance(val[0], dict):
                    for i, upd_item in enumerate(val):
                        target[key][i] = deep_update(target[key][i], upd_item)
                else:
                    target[key] = val
            else:
                target[key] = val
    return target
