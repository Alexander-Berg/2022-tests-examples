def serialize(field, data):
    return field.serialize("field", {"field": data})


def reverse_parameters(params):
    return list(map(lambda t: (*t[:-2], t[-1], t[-2]), params))
