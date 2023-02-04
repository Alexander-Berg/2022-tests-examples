# -*- coding: utf-8 -*-
# class Flag(object):
#     def __init__(self, name, value):
#         super(Flag, self).__init__()
#         self.__name = name
#         self.__value = value
#
#     @property
#     def name(self):
#         return self.__name
#
#     @property
#     def value(self):
#         return self.__value
#
#
# ACK = Flag('ACK', 0x1)
# END_STREAM = Flag('END_STREAM', 0x1)
# END_HEADERS = Flag('END_HEADERS', 0x4)
# PADDED = Flag('PADDED', 0x8)
# PRIORITY = Flag('PRIORITY', 0x20)

ACK = 0x1
END_STREAM = 0x1
END_HEADERS = 0x4
PADDED = 0x8
PRIORITY = 0x20
