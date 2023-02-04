def dump_model(obj, keys):
    return {k: getattr(obj, k) for k in keys}


def dump_model_list(obj_list, keys):
    return [dump_model(obj, keys) for obj in obj_list]
