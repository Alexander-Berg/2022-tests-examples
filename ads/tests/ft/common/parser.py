import importlib

from ads.bsyeti.caesar.libs.profiles import proto
from ads.bsyeti.caesar.libs.profiles import python


def to_snake_case(camel):
    snake = "".join(c.isupper() and "_%s" % c.lower() or c for c in camel)
    if camel and camel[0].isupper():
        return snake[1:]
    return snake


def to_single(table):
    if table.endswith("ies"):  # Strategies -> Strategy
        return table[:-3] + "y"
    return table[:-1]  # Banners -> Banner


def get_profile_class(table):
    pb_name = "%s_pb2" % to_snake_case(to_single(table))
    try:
        module = importlib.import_module("%s.%s" % (proto.__name__, pb_name))
    except ModuleNotFoundError:
        return
    return getattr(module, "T%sProfileProto" % to_single(table))


def get_extract_profile_proto(profile_class):
    name = profile_class.__name__[1:]  # T*
    return getattr(python, "extract_%s" % to_snake_case(name))


def parse_profile(table, row, profile_class=None):
    profile_class = profile_class or get_profile_class(table)
    if not profile_class:
        raise RuntimeError("Unable to find profile class by table name, please specify it")
    extract_profile_proto = get_extract_profile_proto(profile_class)

    profile = profile_class()
    profile.ParseFromString(extract_profile_proto(row))
    return profile
