import hamcrest as hm


def _recursive_wrap_entries(entry):
    if not isinstance(entry, dict):
        return entry
    return hm.has_entries({k: _recursive_wrap_entries(v) for k, v in entry.items()})


def _processor_response_entries(result: dict, success: bool):
    data_matcher = hm.has_entries(result) if not success else hm.has_entries({
        'result': _recursive_wrap_entries(result)
    })
    return hm.has_entries({
        'status': 'ok' if success else 'error',
        'data': data_matcher
    })


def success_processor_response_entries(result: dict):
    return _processor_response_entries(result, success=True)


def error_processor_response_entries(result: dict):
    return _processor_response_entries(result, success=False)


def _accounts_read_batch_response(data: dict, success: bool, strict: bool):
    func = hm.contains_inanyorder if strict else hm.has_items
    return hm.has_entries({
        'status': 'ok' if success else 'error',
        'data': hm.has_entries({
            key: func(
                *[_recursive_wrap_entries(item) for item in val]
            ) if val is not None else val for key, val in data.items()
        })
    })


def success_accounts_read_batch_response(data: dict, strict: bool = True):
    return _accounts_read_batch_response(data, success=True, strict=strict)


def error_accounts_read_batch_response(data: dict, strict: bool = True):
    return _accounts_read_batch_response(data, success=False, strict=strict)


def success_diod_read_batch_response(keys: list[dict]):
    return hm.has_entries({
        'data': hm.has_entries({
            'items': hm.contains_inanyorder(
                *[_recursive_wrap_entries(key_) for key_ in keys]),
        })
    })


def success_payout_info_data_has_item_with(data: dict):
    return hm.has_item(hm.has_entries(data))


def objects_with_properties(properties: dict[str: list]):
    size = min(len(arr) for arr in properties.values())
    assert size == max(len(arr) for arr in properties.values()), 'The lengths of the arrays differ'
    return hm.contains_inanyorder(*[
        hm.has_properties({
            key: properties[key][i]
            for key in properties.keys()
        }) for i in range(size)
    ])
