import os


def arcadia_build_path(path):
    try:
        import yatest.common  # arcadia speciffic
    except ImportError:
        return os.path.realpath(__file__ + '/../' * 5 + path)
    else:
        return yatest.common.build_path(path)


def arcadia_source_path(path):
    try:
        import yatest.common  # arcadia speciffic
    except ImportError:
        return os.path.realpath(__file__ + '/../' * 5 + path)
    else:
        return yatest.common.source_path(path)
