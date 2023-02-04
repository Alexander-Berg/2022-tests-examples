DPI_SIZE_MAP = {
    160: 48,
    240: 72,
    320: 96,
    480: 144,
    640: 192,
}

SIZE_NAME_MAP = {
    16: 'icon-s',
    32: 'icon-s-retina',
    48: 'icon-mdpi',
    65: 'icon',
    72: 'icon-l',
    80: 'icon-ld',
    90: 'icon-ldd',
    96: 'icon-xhdpi',
    144: 'icon-xl',
    150: 'icon-xld',
    160: 'icon-ld-retina',
    180: 'icon-ldd-retina',
    192: 'icon-xxxhdi',
    300: 'icon-xld-retina',
}

DPI_NAME_MAP = {
    160: 'mdpi',
    240: 'hdpi',
    320: 'xhdpi',
    480: 'xxhdpi',
    640: 'xxxhdpi',
}

NAME_SIZE_MAP = {SIZE_NAME_MAP[size]: size for size in SIZE_NAME_MAP}

XXHDPI_DENSITY = 480


def get_nearest(icon_size_map, key):
    nearest_key = min(icon_size_map.keys(), key=lambda x: abs(key - x))
    return icon_size_map[nearest_key]


def dpi_to_size(dpi):
    return get_nearest(DPI_SIZE_MAP, dpi)


def size_to_name(size):
    return get_nearest(SIZE_NAME_MAP, size)


def name_to_size(name):
    return NAME_SIZE_MAP[name]


def dpi_to_name(dpi):
    return size_to_name(dpi_to_size(dpi))


def dpi_to_name_short(dpi):
    return get_nearest(DPI_NAME_MAP, dpi)


def dp_to_px(dp, dpi):
    px = int(dp) * int(dpi) / 160
    return px
