from enum import Enum


class DC(Enum):
    SAS1 = 0
    SAS2 = 1
    VLA = 2
    VLX = 3
    MAN = 4
    IVA = 5
    MYT = 6


class NETTYPE(Enum):
    BACKBONE = 0
    FASTBONE = 1


def dc_name_to_enum(name):
    return DC[name.upper()]
