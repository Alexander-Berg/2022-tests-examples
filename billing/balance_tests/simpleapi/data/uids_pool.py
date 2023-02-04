# coding=utf-8

import os

from boto.exception import S3ResponseError

import btestlib.reporter as reporter
from btestlib import utils, secrets
from btestlib.secrets import get_secret, TrustOauthTokens
from simpleapi.common import logger
from simpleapi.common.utils import SimpleRandom
from simpleapi.steps import db_steps

log = logger.get_logger()

random = SimpleRandom()

users_holder = dict()

CLIENTUID_PWD = secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD)
TRUSTTESTUSR_PWD = secrets.get_secret(*secrets.UsersPwd.TRUSTTESTUSR_PWD)


class User(object):
    def __init__(self, id_, login=None, password=None, **kwargs):
        self.id_ = id_
        self.login = login
        self.password = password
        self.new_binding = kwargs.get("is_new") or False
        self.ya_money = kwargs.get("ya_money") or None
        self.phones = kwargs.get("phones") or None
        self.ym_phones = kwargs.get("ym_phones") or None
        self.is_fake = kwargs.get("is_fake") or False
        self.uber_user_id = kwargs.get("uber_user_id") or None
        self.token = kwargs.get('token') or None
        self.linked_users = kwargs.get('linked_users') or list()
        self.kinopoisk_user_id = kwargs.get('kinopoisk_user_id') or None
        self.kinopoisk_linked_user = kwargs.get('kinopoisk_linked_user') or None
        self.role_description = kwargs.get('role_description') or None

    @property
    def uid(self):
        return str(self.id_) if self.id_ else None

    def is_test(self):
        return self.id_ > 3 * (10 ** 9)

    def is_mutable(self):
        return get_by_uid_in_secret(self.uid) is None

    @property
    def has_ym_phones(self):
        return self.ym_phones is not None

    @property
    def has_phones(self):
        return self.phones is not None

    def matches_type(self, type_):
        return self.ya_money == type_.ya_money and \
               self.has_ym_phones == type_.has_ym_phones and \
               self.has_phones == type_.has_phones

    def __repr__(self):
        return '{} ({})'.format(self.login, self.uid) if not self.token \
            else 'Phonish ({})'.format(self.uid)

    def __str__(self):
        return self.uid

    def __unicode__(self):
        return unicode(self.uid)


anonymous = User(None, 'Anonymous', None)

mimino = {
    'trust_mimino_user_1': User(388472653, "yndx-tst-trust-mimino-1", CLIENTUID_PWD),
    'trust_mimino_user_2': User(388472672, "yndx-tst-trust-mimino-2", CLIENTUID_PWD),
    'trust_mimino_user_3': User(388472689, "yndx-tst-trust-mimino-3", CLIENTUID_PWD),
    'trust_mimino_user_4': User(388472699, "yndx-tst-trust-mimino-4", CLIENTUID_PWD),
    'trust_mimino_user_5': User(388472709, "yndx-tst-trust-mimino-5", CLIENTUID_PWD),
    'trust_mimino_user_6': User(388472721, "yndx-tst-trust-mimino-6", CLIENTUID_PWD),
    'trust_mimino_user_7': User(388472731, "yndx-tst-trust-mimino-7", CLIENTUID_PWD),
    'trust_mimino_user_8': User(388472753, "yndx-tst-trust-mimino-8", CLIENTUID_PWD),
    'trust_mimino_user_9': User(388472763, "yndx-tst-trust-mimino-9", CLIENTUID_PWD),
    'trust_mimino_user_10': User(388472778, "yndx-tst-trust-mimino-10", CLIENTUID_PWD),
    'trust_mimino_user_11': User(388472797, "yndx-tst-trust-mimino-11", CLIENTUID_PWD),
    'trust_mimino_user_12': User(388472812, "yndx-tst-trust-mimino-12", CLIENTUID_PWD),
    'trust_mimino_user_13': User(388472823, "yndx-tst-trust-mimino-13", CLIENTUID_PWD),
    'trust_mimino_user_14': User(388472833, "yndx-tst-trust-mimino-14", CLIENTUID_PWD),
    'trust_mimino_user_15': User(388472846, "yndx-tst-trust-mimino-15", CLIENTUID_PWD),
    'trust_mimino_user_16': User(388472859, "yndx-tst-trust-mimino-16", CLIENTUID_PWD),
    'trust_mimino_user_17': User(388472876, "yndx-tst-trust-mimino-17", CLIENTUID_PWD),
    'trust_mimino_user_18': User(388472886, "yndx-tst-trust-mimino-18", CLIENTUID_PWD),
    'trust_mimino_user_19': User(388472902, "yndx-tst-trust-mimino-19", CLIENTUID_PWD),
    'trust_mimino_user_20': User(388472918, "yndx-tst-trust-mimino-20", CLIENTUID_PWD),
    'trust_mimino_user_21': User(388472939, "yndx-tst-trust-mimino-21", CLIENTUID_PWD),
    'trust_mimino_user_22': User(388472948, "yndx-tst-trust-mimino-22", CLIENTUID_PWD),
    'trust_mimino_user_23': User(388472957, "yndx-tst-trust-mimino-23", CLIENTUID_PWD),
    'trust_mimino_user_24': User(388472977, "yndx-tst-trust-mimino-24", CLIENTUID_PWD),
    'trust_mimino_user_25': User(388472998, "yndx-tst-trust-mimino-25", CLIENTUID_PWD),
    'trust_mimino_user_26': User(388473016, "yndx-tst-trust-mimino-26", CLIENTUID_PWD),
    'trust_mimino_user_27': User(388473029, "yndx-tst-trust-mimino-27", CLIENTUID_PWD),
    'trust_mimino_user_28': User(388473049, "yndx-tst-trust-mimino-28", CLIENTUID_PWD),
    'trust_mimino_user_29': User(388473066, "yndx-tst-trust-mimino-29", CLIENTUID_PWD),
    'trust_mimino_user_30': User(397604473, "yndx-tst-trust-mimino-30", CLIENTUID_PWD),
    'trust_mimino_user_31': User(397604497, "yndx-tst-trust-mimino-31", CLIENTUID_PWD),
    'trust_mimino_user_32': User(397604515, "yndx-tst-trust-mimino-32", CLIENTUID_PWD),
    'trust_mimino_user_33': User(397604540, "yndx-tst-trust-mimino-33", CLIENTUID_PWD),
    'trust_mimino_user_34': User(397604555, "yndx-tst-trust-mimino-34", CLIENTUID_PWD),
    'trust_mimino_user_35': User(397604580, "yndx-tst-trust-mimino-35", CLIENTUID_PWD),
    'trust_mimino_user_36': User(397604601, "yndx-tst-trust-mimino-36", CLIENTUID_PWD),
    'trust_mimino_user_37': User(397604625, "yndx-tst-trust-mimino-37", CLIENTUID_PWD),
    'trust_mimino_user_38': User(397604663, "yndx-tst-trust-mimino-38", CLIENTUID_PWD),
    'trust_mimino_user_39': User(397604672, "yndx-tst-trust-mimino-39", CLIENTUID_PWD),
    'trust_mimino_user_90': User(556855306, "yndx-tst-trust-mimino-90", CLIENTUID_PWD),
    'trust_mimino_user_91': User(556855314, "yndx-tst-trust-mimino-91", CLIENTUID_PWD),
    'trust_mimino_user_92': User(556855324, "yndx-tst-trust-mimino-92", CLIENTUID_PWD),
    'trust_mimino_user_93': User(556855335, "yndx-tst-trust-mimino-93", CLIENTUID_PWD),
    'trust_mimino_user_94': User(556855347, "yndx-tst-trust-mimino-94", CLIENTUID_PWD),
    'trust_mimino_user_95': User(556855354, "yndx-tst-trust-mimino-95", CLIENTUID_PWD),
    'trust_mimino_user_96': User(556855365, "yndx-tst-trust-mimino-96", CLIENTUID_PWD),
    'trust_mimino_user_97': User(556855376, "yndx-tst-trust-mimino-97", CLIENTUID_PWD),
    'trust_mimino_user_98': User(556855385, "yndx-tst-trust-mimino-98", CLIENTUID_PWD),
    'trust_mimino_user_99': User(556855400, "yndx-tst-trust-mimino-99", CLIENTUID_PWD),
    'trust_mimino_user_100': User(556855192, "yndx-tst-trust-mimino-100", CLIENTUID_PWD),
    'trust_mimino_user_101': User(556855209, "yndx-tst-trust-mimino-101", CLIENTUID_PWD),
    'trust_mimino_user_102': User(556855216, "yndx-tst-trust-mimino-102", CLIENTUID_PWD),
    'trust_mimino_user_103': User(556855224, "yndx-tst-trust-mimino-103", CLIENTUID_PWD),
    'trust_mimino_user_104': User(556855233, "yndx-tst-trust-mimino-104", CLIENTUID_PWD),
    'trust_mimino_user_105': User(556855250, "yndx-tst-trust-mimino-105", CLIENTUID_PWD),
    'trust_mimino_user_106': User(556855258, "yndx-tst-trust-mimino-106", CLIENTUID_PWD),
    'trust_mimino_user_107': User(556855270, "yndx-tst-trust-mimino-107", CLIENTUID_PWD),
    'trust_mimino_user_108': User(556855278, "yndx-tst-trust-mimino-108", CLIENTUID_PWD),
    'trust_mimino_user_109': User(556855286, "yndx-tst-trust-mimino-109", CLIENTUID_PWD),
    'trust_mimino_user_110': User(556855295, "yndx-tst-trust-mimino-110", CLIENTUID_PWD),
    'trust_mimino_user_161': User(642349003, "yndx-tst-trust-mimino-161", CLIENTUID_PWD),
    'trust_mimino_user_162': User(642349017, "yndx-tst-trust-mimino-162", CLIENTUID_PWD),
    'trust_mimino_user_163': User(642349027, "yndx-tst-trust-mimino-163", CLIENTUID_PWD),
    'trust_mimino_user_164': User(642349039, "yndx-tst-trust-mimino-164", CLIENTUID_PWD),
    'trust_mimino_user_165': User(642349047, "yndx-tst-trust-mimino-165", CLIENTUID_PWD),
    'trust_mimino_user_166': User(642349063, "yndx-tst-trust-mimino-166", CLIENTUID_PWD),
    'trust_mimino_user_167': User(642349079, "yndx-tst-trust-mimino-167", CLIENTUID_PWD),
    'trust_mimino_user_168': User(642349085, "yndx-tst-trust-mimino-168", CLIENTUID_PWD),
    'trust_mimino_user_169': User(642349093, "yndx-tst-trust-mimino-169", CLIENTUID_PWD),
    'trust_mimino_user_170': User(642349107, "yndx-tst-trust-mimino-170", CLIENTUID_PWD),
    'trust_mimino_user_171': User(642349117, "yndx-tst-trust-mimino-171", CLIENTUID_PWD),
    'trust_mimino_user_172': User(642349125, "yndx-tst-trust-mimino-172", CLIENTUID_PWD),
    'trust_mimino_user_173': User(642349136, "yndx-tst-trust-mimino-173", CLIENTUID_PWD),
    'trust_mimino_user_174': User(642349146, "yndx-tst-trust-mimino-174", CLIENTUID_PWD),
    'trust_mimino_user_175': User(642349155, "yndx-tst-trust-mimino-175", CLIENTUID_PWD),
}

test_passport = {
    'trust_test_user_26': User(4003406631, "yndx-tst-trust-test-26", CLIENTUID_PWD),
    'trust_test_user_27': User(4003406633, "yndx-tst-trust-test-27", CLIENTUID_PWD),
    'trust_test_user_28': User(4003406635, "yndx-tst-trust-test-28", CLIENTUID_PWD),
    'trust_test_user_29': User(4003406639, "yndx-tst-trust-test-29", CLIENTUID_PWD),
    'trust_test_user_30': User(4003406641, "yndx-tst-trust-test-30", CLIENTUID_PWD),
    'trust_test_user_31': User(4003406643, "yndx-tst-trust-test-31", CLIENTUID_PWD),
    'trust_test_user_32': User(4003406645, "yndx-tst-trust-test-32", CLIENTUID_PWD),
    'trust_test_user_33': User(4003406647, "yndx-tst-trust-test-33", CLIENTUID_PWD),
    'trust_test_user_34': User(4003406649, "yndx-tst-trust-test-34", CLIENTUID_PWD),
    'trust_test_user_35': User(4003406767, "yndx-tst-trust-test-35", CLIENTUID_PWD),
    'trust_test_user_36': User(4003406781, "yndx-tst-trust-test-36", CLIENTUID_PWD),
    'trust_test_user_37': User(4003406793, "yndx-tst-trust-test-37", CLIENTUID_PWD),
    'trust_test_user_38': User(4003406807, "yndx-tst-trust-test-38", CLIENTUID_PWD),
    'trust_test_user_39': User(4003406955, "yndx-tst-trust-test-39", CLIENTUID_PWD),
    'trust_test_user_40': User(4003407005, "yndx-tst-trust-test-40", CLIENTUID_PWD),
    'trust_test_user_41': User(4003407025, "yndx-tst-trust-test-41", CLIENTUID_PWD),
    'trust_test_user_42': User(4003407027, "yndx-tst-trust-test-42", CLIENTUID_PWD),
    'trust_test_user_43': User(4003407029, "yndx-tst-trust-test-43", CLIENTUID_PWD),
    'trust_test_user_44': User(4003407031, "yndx-tst-trust-test-44", CLIENTUID_PWD),
    'trust_test_user_45': User(4003407035, "yndx-tst-trust-test-45", CLIENTUID_PWD),
    'trust_test_user_46': User(4003407037, "yndx-tst-trust-test-46", CLIENTUID_PWD),
    'trust_test_user_47': User(4003407041, "yndx-tst-trust-test-47", CLIENTUID_PWD),
    'trust_test_user_48': User(4003407043, "yndx-tst-trust-test-48", CLIENTUID_PWD),
    'trust_test_user_49': User(4003407045, "yndx-tst-trust-test-49", CLIENTUID_PWD),
    'trust_test_user_50': User(4003407049, "yndx-tst-trust-test-50", CLIENTUID_PWD),
    'trust_test_user_52': User(4004970223, "yndx-tst-trust-test-52", CLIENTUID_PWD),
    'trust_test_user_53': User(4004970225, "yndx-tst-trust-test-53", CLIENTUID_PWD),
    'trust_test_user_54': User(4004970227, "yndx-tst-trust-test-54", CLIENTUID_PWD),
    'trust_test_user_55': User(4004970229, "yndx-tst-trust-test-55", CLIENTUID_PWD),
    'trust_test_user_56': User(4004970231, "yndx-tst-trust-test-56", CLIENTUID_PWD),
    'trust_test_user_57': User(4004970233, "yndx-tst-trust-test-57", CLIENTUID_PWD),
    'trust_test_user_58': User(4004970235, "yndx-tst-trust-test-58", CLIENTUID_PWD),
    # todo fellow замьютил этого пользователя для проверки TRUST-5810
    # 'trust_test_user_59': User(4004970237, "yndx-tst-trust-test-59", CLIENTUID_PWD),
    'trust_test_user_61': User(4004970239, "yndx-tst-trust-test-61", CLIENTUID_PWD),
    'trust_test_user_62': User(4004970241, "yndx-tst-trust-test-62", CLIENTUID_PWD),
    'trust_test_user_63': User(4004970243, "yndx-tst-trust-test-63", CLIENTUID_PWD),
    'trust_test_user_64': User(4004970245, "yndx-tst-trust-test-64", CLIENTUID_PWD),
    'trust_test_user_65': User(4004970247, "yndx-tst-trust-test-65", CLIENTUID_PWD),
    'trust_test_user_66': User(4004970249, "yndx-tst-trust-test-66", CLIENTUID_PWD),
    'trust_test_user_67': User(4004970251, "yndx-tst-trust-test-67", CLIENTUID_PWD),
    'trust_test_user_68': User(4004970253, "yndx-tst-trust-test-68", CLIENTUID_PWD),
    'trust_test_user_69': User(4004970255, "yndx-tst-trust-test-69", CLIENTUID_PWD),
    'trust_test_user_70': User(4004970257, "yndx-tst-trust-test-70", CLIENTUID_PWD),
    'trust_test_user_71': User(4006193624, "yndx-tst-trust-test-71", CLIENTUID_PWD),
    'trust_test_user_72': User(4006193646, "yndx-tst-trust-test-72", CLIENTUID_PWD),
    'trust_test_user_73': User(4006193654, "yndx-tst-trust-test-73", CLIENTUID_PWD),
    'trust_test_user_74': User(4006193656, "yndx-tst-trust-test-74", CLIENTUID_PWD),
    'trust_test_user_75': User(4006193664, "yndx-tst-trust-test-75", CLIENTUID_PWD),
    'trust_test_user_76': User(4006193672, "yndx-tst-trust-test-76", CLIENTUID_PWD),
    'trust_test_user_77': User(4006193674, "yndx-tst-trust-test-77", CLIENTUID_PWD),
    'trust_test_user_78': User(4006193682, "yndx-tst-trust-test-78", CLIENTUID_PWD),
    'trust_test_user_79': User(4006193688, "yndx-tst-trust-test-79", CLIENTUID_PWD),
    'trust_test_user_80': User(4006193698, "yndx-tst-trust-test-80", CLIENTUID_PWD),
    'trust_test_user_81': User(4006193804, "yndx-tst-trust-test-81", CLIENTUID_PWD),
    'trust_test_user_82': User(4006193814, "yndx-tst-trust-test-82", CLIENTUID_PWD),
    'trust_test_user_83': User(4006193818, "yndx-tst-trust-test-83", CLIENTUID_PWD),
    'trust_test_user_84': User(4006193820, "yndx-tst-trust-test-84", CLIENTUID_PWD),
    'trust_test_user_85': User(4006193832, "yndx-tst-trust-test-85", CLIENTUID_PWD),
    'trust_test_user_86': User(4006193836, "yndx-tst-trust-test-86", CLIENTUID_PWD),
    'trust_test_user_87': User(4006193840, "yndx-tst-trust-test-87", CLIENTUID_PWD),
    'trust_test_user_88': User(4006193844, "yndx-tst-trust-test-88", CLIENTUID_PWD),
    'trust_test_user_89': User(4006193850, "yndx-tst-trust-test-89", CLIENTUID_PWD),
    'trust_test_user_90': User(4006193852, "yndx-tst-trust-test-90", CLIENTUID_PWD),
    'trust_test_user_121': User(4011631838, "yndx-tst-trust-test-121", CLIENTUID_PWD),
    'trust_test_user_122': User(4011631958, "yndx-tst-trust-test-122", CLIENTUID_PWD),
    'trust_test_user_123': User(4011632014, "yndx-tst-trust-test-123", CLIENTUID_PWD),
    'trust_test_user_124': User(4011632262, "yndx-tst-trust-test-124", CLIENTUID_PWD),
    'trust_test_user_125': User(4011632264, "yndx-tst-trust-test-125", CLIENTUID_PWD),
    'trust_test_user_126': User(4011632132, "yndx-tst-trust-test-126", CLIENTUID_PWD),
    'trust_test_user_127': User(4011632150, "yndx-tst-trust-test-127", CLIENTUID_PWD),
    'trust_test_user_128': User(4011632196, "yndx-tst-trust-test-128", CLIENTUID_PWD),
    'trust_test_user_129': User(4011632206, "yndx-tst-trust-test-129", CLIENTUID_PWD),
    'trust_test_user_130': User(4011632212, "yndx-tst-trust-test-130", CLIENTUID_PWD),
    'trust_test_user_131': User(4011632380, "yndx-tst-trust-test-131", CLIENTUID_PWD),
    'trust_test_user_132': User(4011632752, "yndx-tst-trust-test-132", CLIENTUID_PWD),
    'trust_test_user_133': User(4011632498, "yndx-tst-trust-test-133", CLIENTUID_PWD),
    'trust_test_user_134': User(4011632544, "yndx-tst-trust-test-134", CLIENTUID_PWD),
    'trust_test_user_135': User(4011632608, "yndx-tst-trust-test-135", CLIENTUID_PWD),
    'trust_test_user_136': User(4011632682, "yndx-tst-trust-test-136", CLIENTUID_PWD),
    'trust_test_user_137': User(4011632728, "yndx-tst-trust-test-137", CLIENTUID_PWD),
    'trust_test_user_138': User(4011632730, "yndx-tst-trust-test-138", CLIENTUID_PWD),
    'trust_test_user_139': User(4011632734, "yndx-tst-trust-test-139", CLIENTUID_PWD),
    'trust_test_user_140': User(4011632736, "yndx-tst-trust-test-140", CLIENTUID_PWD),
    'trust_test_user_141': User(4011633228, "yndx-tst-trust-test-141", CLIENTUID_PWD),
    'trust_test_user_142': User(4011633234, "yndx-tst-trust-test-142", CLIENTUID_PWD),
    'trust_test_user_143': User(4011633238, "yndx-tst-trust-test-143", CLIENTUID_PWD),
    'trust_test_user_144': User(4011633240, "yndx-tst-trust-test-144", CLIENTUID_PWD),
    'trust_test_user_145': User(4011633242, "yndx-tst-trust-test-145", CLIENTUID_PWD),
    'trust_test_user_146': User(4011633246, "yndx-tst-trust-test-146", CLIENTUID_PWD),
    'trust_test_user_147': User(4011633248, "yndx-tst-trust-test-147", CLIENTUID_PWD),
    'trust_test_user_148': User(4011633260, "yndx-tst-trust-test-148", CLIENTUID_PWD),
    'trust_test_user_149': User(4011633270, "yndx-tst-trust-test-149", CLIENTUID_PWD),
    'trust_test_user_150': User(4011633266, "yndx-tst-trust-test-150", CLIENTUID_PWD),
    'trust_test_user_151': User(4011821322, "yndx-tst-trust-test-151", CLIENTUID_PWD),
    'trust_test_user_152': User(4011821330, "yndx-tst-trust-test-152", CLIENTUID_PWD),
    'trust_test_user_153': User(4011821340, "yndx-tst-trust-test-153", CLIENTUID_PWD),
    'trust_test_user_154': User(4011821352, "yndx-tst-trust-test-154", CLIENTUID_PWD),
    'trust_test_user_155': User(4011821886, "yndx-tst-trust-test-155", CLIENTUID_PWD),
    'trust_test_user_156': User(4011821974, "yndx-tst-trust-test-156", CLIENTUID_PWD),
    'trust_test_user_157': User(4011821982, "yndx-tst-trust-test-157", CLIENTUID_PWD),
    'trust_test_user_158': User(4011822000, "yndx-tst-trust-test-158", CLIENTUID_PWD),
    'trust_test_user_159': User(4011822112, "yndx-tst-trust-test-159", CLIENTUID_PWD),
}

'''
эти уиды используем только в тестах на привязку/отвязку
так как к ним там активно привязываются/отвязываются карты
и это может сказаться на других тестах с теми же уидами
'''
mutable = {
    'trust_test_user_27': User(315454068, "testtrustusr1", TRUSTTESTUSR_PWD),
    'trust_test_user_28': User(315454077, "testtrustusr2", TRUSTTESTUSR_PWD),
    'trust_test_user_29': User(315454084, "testtrustusr3", TRUSTTESTUSR_PWD),
    'trust_test_user_30': User(315454092, "testtrustusr4", TRUSTTESTUSR_PWD),
    'trust_test_user_31': User(315454097, "testtrustusr5", TRUSTTESTUSR_PWD),
    'trust_mimino_user_50': User(397604825, "yndx-tst-trust-mimino-50", CLIENTUID_PWD),
    'trust_mimino_user_51': User(397604843, "yndx-tst-trust-mimino-51", CLIENTUID_PWD),
    'trust_mimino_user_52': User(397604857, "yndx-tst-trust-mimino-52", CLIENTUID_PWD),
    'trust_mimino_user_53': User(397604873, "yndx-tst-trust-mimino-53", CLIENTUID_PWD),
    'trust_mimino_user_54': User(397604885, "yndx-tst-trust-mimino-54", CLIENTUID_PWD),
    'trust_mimino_user_55': User(397604904, "yndx-tst-trust-mimino-55", CLIENTUID_PWD),
    'trust_mimino_user_56': User(397604928, "yndx-tst-trust-mimino-56", CLIENTUID_PWD),
    'trust_mimino_user_57': User(397604944, "yndx-tst-trust-mimino-57", CLIENTUID_PWD),
    'trust_mimino_user_58': User(397604967, "yndx-tst-trust-mimino-58", CLIENTUID_PWD),
    'trust_mimino_user_59': User(397604980, "yndx-tst-trust-mimino-59", CLIENTUID_PWD),
    'trust_test_user_91': User(4006194070, "yndx-tst-trust-test-91", CLIENTUID_PWD),
    'trust_test_user_92': User(4006194084, "yndx-tst-trust-test-92", CLIENTUID_PWD),
    'trust_test_user_93': User(4006194096, "yndx-tst-trust-test-93", CLIENTUID_PWD),
    'trust_test_user_94': User(4006194110, "yndx-tst-trust-test-94", CLIENTUID_PWD),
    'trust_test_user_95': User(4006194128, "yndx-tst-trust-test-95", CLIENTUID_PWD),
    'trust_test_user_96': User(4006194138, "yndx-tst-trust-test-96", CLIENTUID_PWD),
    'trust_test_user_97': User(4006194154, "yndx-tst-trust-test-97", CLIENTUID_PWD),
    'trust_test_user_98': User(4006194164, "yndx-tst-trust-test-98", CLIENTUID_PWD),
    'trust_test_user_99': User(4006194176, "yndx-tst-trust-test-99", CLIENTUID_PWD),
    'trust_test_user_100': User(4006194188, "yndx-tst-trust-test-100", CLIENTUID_PWD),
    'trust_test_user_101': User(4006235250, "yndx-tst-trust-test-101", CLIENTUID_PWD),
    'trust_test_user_102': User(4006235280, "yndx-tst-trust-test-102", CLIENTUID_PWD),
    'trust_test_user_103': User(4006235326, "yndx-tst-trust-test-103", CLIENTUID_PWD),
    'trust_test_user_104': User(4006235346, "yndx-tst-trust-test-104", CLIENTUID_PWD),
    'trust_test_user_105': User(4006235348, "yndx-tst-trust-test-105", CLIENTUID_PWD),
    'trust_test_user_106': User(4006235352, "yndx-tst-trust-test-106", CLIENTUID_PWD),
    'trust_test_user_107': User(4006235354, "yndx-tst-trust-test-107", CLIENTUID_PWD),
    'trust_test_user_108': User(4006235358, "yndx-tst-trust-test-108", CLIENTUID_PWD),
    'trust_test_user_109': User(4006235360, "yndx-tst-trust-test-109", CLIENTUID_PWD),
    'trust_test_user_110': User(4006235362, "yndx-tst-trust-test-110", CLIENTUID_PWD),
    'trust_test_user_111': User(4006235366, "yndx-tst-trust-test-111", CLIENTUID_PWD),
    'trust_test_user_112': User(4006235370, "yndx-tst-trust-test-112", CLIENTUID_PWD),
    'trust_test_user_113': User(4006235390, "yndx-tst-trust-test-113", CLIENTUID_PWD),
    'trust_test_user_114': User(4006235392, "yndx-tst-trust-test-114", CLIENTUID_PWD),
    'trust_test_user_115': User(4006235400, "yndx-tst-trust-test-115", CLIENTUID_PWD),
    'trust_test_user_116': User(4006235402, "yndx-tst-trust-test-116", CLIENTUID_PWD),
    'trust_test_user_117': User(4006235404, "yndx-tst-trust-test-117", CLIENTUID_PWD),
    'trust_test_user_118': User(4006235406, "yndx-tst-trust-test-118", CLIENTUID_PWD),
    'trust_test_user_119': User(4006235410, "yndx-tst-trust-test-119", CLIENTUID_PWD),
    'trust_test_user_120': User(4006235414, "yndx-tst-trust-test-120", CLIENTUID_PWD),
    'trust_test_user_160': User(4011822124, "yndx-tst-trust-test-160", CLIENTUID_PWD),
    'trust_test_user_161': User(4011830128, "yndx-tst-trust-test-161", CLIENTUID_PWD),
    'trust_test_user_162': User(4011830154, "yndx-tst-trust-test-162", CLIENTUID_PWD),
    'trust_test_user_163': User(4011830156, "yndx-tst-trust-test-163", CLIENTUID_PWD),
    'trust_test_user_164': User(4011830158, "yndx-tst-trust-test-164", CLIENTUID_PWD),
    'trust_test_user_165': User(4011830160, "yndx-tst-trust-test-165", CLIENTUID_PWD),
    'trust_test_user_166': User(4011830162, "yndx-tst-trust-test-166", CLIENTUID_PWD),
    'trust_test_user_167': User(4011830166, "yndx-tst-trust-test-167", CLIENTUID_PWD),
    'trust_test_user_168': User(4011830170, "yndx-tst-trust-test-168", CLIENTUID_PWD),
    'trust_test_user_169': User(4011830172, "yndx-tst-trust-test-169", CLIENTUID_PWD),
    'trust_test_user_170': User(4011830174, "yndx-tst-trust-test-170", CLIENTUID_PWD),
    'trust_mimino_user_60': User(494527492, "yndx-tst-trust-mimino-60", CLIENTUID_PWD),
    'trust_mimino_user_61': User(494527550, "yndx-tst-trust-mimino-61", CLIENTUID_PWD),
    'trust_mimino_user_62': User(494527569, "yndx-tst-trust-mimino-62", CLIENTUID_PWD),
    'trust_mimino_user_63': User(494527594, "yndx-tst-trust-mimino-63", CLIENTUID_PWD),
    'trust_mimino_user_64': User(494527612, "yndx-tst-trust-mimino-64", CLIENTUID_PWD),
    'trust_mimino_user_65': User(494527627, "yndx-tst-trust-mimino-65", CLIENTUID_PWD),
    'trust_mimino_user_66': User(494527637, "yndx-tst-trust-mimino-66", CLIENTUID_PWD),
    'trust_mimino_user_67': User(494527660, "yndx-tst-trust-mimino-67", CLIENTUID_PWD),
    'trust_mimino_user_68': User(494527677, "yndx-tst-trust-mimino-68", CLIENTUID_PWD),
    'trust_mimino_user_69': User(494527704, "yndx-tst-trust-mimino-69", CLIENTUID_PWD),
    'trust_mimino_user_70': User(494527722, "yndx-tst-trust-mimino-70", CLIENTUID_PWD),
    'trust_mimino_user_71': User(494527743, "yndx-tst-trust-mimino-71", CLIENTUID_PWD),
    'trust_mimino_user_72': User(494527760, "yndx-tst-trust-mimino-72", CLIENTUID_PWD),
    'trust_mimino_user_73': User(494527781, "yndx-tst-trust-mimino-73", CLIENTUID_PWD),
    'trust_mimino_user_74': User(494527806, "yndx-tst-trust-mimino-74", CLIENTUID_PWD),
    'trust_mimino_user_75': User(494527827, "yndx-tst-trust-mimino-75", CLIENTUID_PWD),
    'trust_mimino_user_76': User(494527849, "yndx-tst-trust-mimino-76", CLIENTUID_PWD),
    'trust_mimino_user_77': User(494527865, "yndx-tst-trust-mimino-77", CLIENTUID_PWD),
    'trust_mimino_user_78': User(494527884, "yndx-tst-trust-mimino-78", CLIENTUID_PWD),
    'trust_mimino_user_79': User(494527901, "yndx-tst-trust-mimino-79", CLIENTUID_PWD),
    'trust_mimino_user_80': User(494527922, "yndx-tst-trust-mimino-80", CLIENTUID_PWD),
    'trust_mimino_user_111': User(556863470, "yndx-tst-trust-mimino-111", CLIENTUID_PWD),
    'trust_mimino_user_112': User(556863480, "yndx-tst-trust-mimino-112", CLIENTUID_PWD),
    'trust_mimino_user_113': User(556863489, "yndx-tst-trust-mimino-113", CLIENTUID_PWD),
    'trust_mimino_user_114': User(556863498, "yndx-tst-trust-mimino-114", CLIENTUID_PWD),
    'trust_mimino_user_115': User(556863508, "yndx-tst-trust-mimino-115", CLIENTUID_PWD),
    'trust_mimino_user_116': User(556863518, "yndx-tst-trust-mimino-116", CLIENTUID_PWD),
    'trust_mimino_user_117': User(556863530, "yndx-tst-trust-mimino-117", CLIENTUID_PWD),
    'trust_mimino_user_118': User(556863539, "yndx-tst-trust-mimino-118", CLIENTUID_PWD),
    'trust_mimino_user_119': User(556863542, "yndx-tst-trust-mimino-119", CLIENTUID_PWD),
    'trust_mimino_user_120': User(556868448, "yndx-tst-trust-mimino-120", CLIENTUID_PWD),
    'trust_mimino_user_121': User(556868458, "yndx-tst-trust-mimino-121", CLIENTUID_PWD),
    'trust_mimino_user_122': User(556868476, "yndx-tst-trust-mimino-122", CLIENTUID_PWD),
    'trust_mimino_user_123': User(556868497, "yndx-tst-trust-mimino-123", CLIENTUID_PWD),
    'trust_mimino_user_124': User(556868506, "yndx-tst-trust-mimino-124", CLIENTUID_PWD),
    'trust_mimino_user_125': User(556868516, "yndx-tst-trust-mimino-125", CLIENTUID_PWD),
    'trust_mimino_user_126': User(556868527, "yndx-tst-trust-mimino-126", CLIENTUID_PWD),
    'trust_mimino_user_127': User(556868534, "yndx-tst-trust-mimino-127", CLIENTUID_PWD),
    'trust_mimino_user_128': User(556868541, "yndx-tst-trust-mimino-128", CLIENTUID_PWD),
    'trust_mimino_user_129': User(556868550, "yndx-tst-trust-mimino-129", CLIENTUID_PWD),
    'trust_mimino_user_130': User(557056210, "yndx-tst-trust-mimino-130", CLIENTUID_PWD),
    'trust_mimino_user_131': User(557056217, "yndx-tst-trust-mimino-131", CLIENTUID_PWD),
    'trust_mimino_user_132': User(557056229, "yndx-tst-trust-mimino-132", CLIENTUID_PWD),
    'trust_mimino_user_133': User(557056242, "yndx-tst-trust-mimino-133", CLIENTUID_PWD),
    'trust_mimino_user_134': User(557056251, "yndx-tst-trust-mimino-134", CLIENTUID_PWD),
    'trust_mimino_user_135': User(557056262, "yndx-tst-trust-mimino-135", CLIENTUID_PWD),
    'trust_mimino_user_136': User(557056269, "yndx-tst-trust-mimino-136", CLIENTUID_PWD),
    'trust_mimino_user_137': User(557056273, "yndx-tst-trust-mimino-137", CLIENTUID_PWD),
    'trust_mimino_user_138': User(557056282, "yndx-tst-trust-mimino-138", CLIENTUID_PWD),
    'trust_mimino_user_139': User(557056290, "yndx-tst-trust-mimino-139", CLIENTUID_PWD),
    'trust_mimino_user_140': User(557056305, "yndx-tst-trust-mimino-140", CLIENTUID_PWD),
    'trust_mimino_user_141': User(562482651, "yndx-tst-trust-mimino-141", CLIENTUID_PWD),
    'trust_mimino_user_142': User(562482673, "yndx-tst-trust-mimino-142", CLIENTUID_PWD),
    'trust_mimino_user_143': User(562482685, "yndx-tst-trust-mimino-143", CLIENTUID_PWD),
    'trust_mimino_user_144': User(562482696, "yndx-tst-trust-mimino-144", CLIENTUID_PWD),
    'trust_mimino_user_145': User(562482710, "yndx-tst-trust-mimino-145", CLIENTUID_PWD),
    'trust_mimino_user_146': User(562482714, "yndx-tst-trust-mimino-146", CLIENTUID_PWD),
    'trust_mimino_user_147': User(562482727, "yndx-tst-trust-mimino-147", CLIENTUID_PWD),
    'trust_mimino_user_148': User(562482733, "yndx-tst-trust-mimino-148", CLIENTUID_PWD),
    'trust_mimino_user_149': User(562482750, "yndx-tst-trust-mimino-149", CLIENTUID_PWD),
    'trust_mimino_user_150': User(562482769, "yndx-tst-trust-mimino-150", CLIENTUID_PWD),
    'trust_mimino_user_151': User(562482785, "yndx-tst-trust-mimino-151", CLIENTUID_PWD),
    'trust_mimino_user_152': User(562482795, "yndx-tst-trust-mimino-152", CLIENTUID_PWD),
    'trust_mimino_user_153': User(562482806, "yndx-tst-trust-mimino-153", CLIENTUID_PWD),
    'trust_mimino_user_154': User(562482819, "yndx-tst-trust-mimino-154", CLIENTUID_PWD),
    'trust_mimino_user_155': User(562482828, "yndx-tst-trust-mimino-155", CLIENTUID_PWD),
    'trust_mimino_user_156': User(562482840, "yndx-tst-trust-mimino-156", CLIENTUID_PWD),
    'trust_mimino_user_157': User(562482847, "yndx-tst-trust-mimino-157", CLIENTUID_PWD),
    'trust_mimino_user_158': User(562482855, "yndx-tst-trust-mimino-158", CLIENTUID_PWD),
    'trust_mimino_user_159': User(562482869, "yndx-tst-trust-mimino-159", CLIENTUID_PWD),
    'trust_mimino_user_160': User(562482877, "yndx-tst-trust-mimino-160", CLIENTUID_PWD),

}

sberbank = {
    'trust_test_user_1': User(4003404817, "yndx-tst-trust-test-1", CLIENTUID_PWD),
    'trust_test_user_2': User(4003404821, "yndx-tst-trust-test-2", CLIENTUID_PWD),
    'trust_test_user_3': User(4003406329, "yndx-tst-trust-test-3", CLIENTUID_PWD),
    'trust_test_user_4': User(4003406331, "yndx-tst-trust-test-4", CLIENTUID_PWD),
    'trust_test_user_5': User(4003406333, "yndx-tst-trust-test-5", CLIENTUID_PWD),
    'trust_mimino_user_40': User(397604687, "yndx-tst-trust-mimino-40", CLIENTUID_PWD),
    'trust_mimino_user_41': User(397604698, "yndx-tst-trust-mimino-41", CLIENTUID_PWD),
    'trust_mimino_user_42': User(397604709, "yndx-tst-trust-mimino-42", CLIENTUID_PWD),
    'trust_mimino_user_43': User(397604724, "yndx-tst-trust-mimino-43", CLIENTUID_PWD),
    'trust_mimino_user_44': User(397604736, "yndx-tst-trust-mimino-44", CLIENTUID_PWD),
}

rbs = {
    'trust_test_user_6': User(4003406335, "yndx-tst-trust-test-6", CLIENTUID_PWD),
    'trust_test_user_7': User(4003406337, "yndx-tst-trust-test-7", CLIENTUID_PWD),
    'trust_test_user_8': User(4003406339, "yndx-tst-trust-test-8", CLIENTUID_PWD),
    'trust_test_user_9': User(4003406341, "yndx-tst-trust-test-9", CLIENTUID_PWD),
    'trust_test_user_10': User(4003406343, "yndx-tst-trust-test-10", CLIENTUID_PWD),
    'trust_mimino_user_45': User(397604750, "yndx-tst-trust-mimino-45", CLIENTUID_PWD),
    'trust_mimino_user_46': User(397604760, "yndx-tst-trust-mimino-46", CLIENTUID_PWD),
    'trust_mimino_user_47': User(397604773, "yndx-tst-trust-mimino-47", CLIENTUID_PWD),
    'trust_mimino_user_48': User(397604785, "yndx-tst-trust-mimino-48", CLIENTUID_PWD),
    'trust_mimino_user_49': User(397604800, "yndx-tst-trust-mimino-49", CLIENTUID_PWD),
}

afs = {
    'trust_mimino_afs_user_1': User(112984384, "clientuid11", CLIENTUID_PWD),
    'trust_mimino_afs_user_2': User(112984867, "clientuid12", CLIENTUID_PWD),
    'trust_mimino_afs_user_3': User(112985256, "clientuid13", CLIENTUID_PWD),
    'trust_mimino_afs_user_4': User(112985422, "clientuid14", CLIENTUID_PWD),
    'trust_mimino_afs_user_5': User(72794913, "clientuid15", CLIENTUID_PWD),
    'trust_mimino_afs_user_6': User(112985965, "clientuid16", CLIENTUID_PWD),
    'trust_test_afs_user_1': User(4003406345, "yndx-tst-trust-test-11", CLIENTUID_PWD),
    'trust_test_afs_user_2': User(4003406347, "yndx-tst-trust-test-12", CLIENTUID_PWD),
    'trust_test_afs_user_3': User(4003406349, "yndx-tst-trust-test-13", CLIENTUID_PWD),
    'trust_test_afs_user_4': User(4003406351, "yndx-tst-trust-test-14", CLIENTUID_PWD),
    'trust_test_afs_user_5': User(4003406353, "yndx-tst-trust-test-15", CLIENTUID_PWD),
    'trust_test_afs_user_6': User(4003406355, "yndx-tst-trust-test-16", CLIENTUID_PWD),
    'trust_test_afs_user_7': User(4003406357, "yndx-tst-trust-test-17", CLIENTUID_PWD),
    'trust_test_afs_user_8': User(4003406359, "yndx-tst-trust-test-18", CLIENTUID_PWD),
    'trust_test_afs_user_9': User(4003406361, "yndx-tst-trust-test-19", CLIENTUID_PWD),
    'trust_test_afs_user_10': User(4003406431, "yndx-tst-trust-test-20", CLIENTUID_PWD),
}

ecommpay = {
    'trust_test_user_21': User(4003406433, "yndx-tst-trust-test-21", CLIENTUID_PWD),
    'trust_test_user_22': User(4003406439, "yndx-tst-trust-test-22", CLIENTUID_PWD),
    'trust_test_user_23': User(4003406451, "yndx-tst-trust-test-23", CLIENTUID_PWD),
    'trust_test_user_24': User(4003406453, "yndx-tst-trust-test-24", CLIENTUID_PWD),
    'trust_test_user_25': User(4003406455, "yndx-tst-trust-test-25", CLIENTUID_PWD),
    'trust_mimino_user_176': User(642349165, "yndx-tst-trust-mimino-176", CLIENTUID_PWD),
    'trust_mimino_user_177': User(642349177, "yndx-tst-trust-mimino-177", CLIENTUID_PWD),
    'trust_mimino_user_178': User(642349187, "yndx-tst-trust-mimino-178", CLIENTUID_PWD),
    'trust_mimino_user_179': User(642349199, "yndx-tst-trust-mimino-179", CLIENTUID_PWD),
    'trust_mimino_user_180': User(642349210, "yndx-tst-trust-mimino-180", CLIENTUID_PWD),
}

phone_test_passport = {
    'trust_test_phone_user_1': User(4008816714, "yndx-tst-trust-phone-000001", CLIENTUID_PWD, phones=['+70002026001']),
    'trust_test_phone_user_2': User(4008816716, "yndx-tst-trust-phone-000002", CLIENTUID_PWD, phones=['+70002026002']),
    'trust_test_phone_user_3': User(4008816718, "yndx-tst-trust-phone-000003", CLIENTUID_PWD, phones=['+70002026003']),
    'trust_test_phone_user_4': User(4008816720, "yndx-tst-trust-phone-000004", CLIENTUID_PWD, phones=['+70002026004']),
    'trust_test_phone_user_5': User(4008816722, "yndx-tst-trust-phone-000005", CLIENTUID_PWD, phones=['+70002026005']),
    'trust_test_phone_user_6': User(4008816724, "yndx-tst-trust-phone-000006", CLIENTUID_PWD, phones=['+70002026006']),
    'trust_test_phone_user_7': User(4008816726, "yndx-tst-trust-phone-000007", CLIENTUID_PWD, phones=['+70002026007']),
    'trust_test_phone_user_8': User(4008816728, "yndx-tst-trust-phone-000008", CLIENTUID_PWD, phones=['+70002026008']),
    'trust_test_phone_user_9': User(4008816730, "yndx-tst-trust-phone-000009", CLIENTUID_PWD, phones=['+70002026009']),
    'trust_test_phone_user_10': User(4008816732, "yndx-tst-trust-phone-000010", CLIENTUID_PWD, phones=['+70002026010']),

    'trust_test_phone_user_11': User(4008816734, "yndx-tst-trust-phone-000011", CLIENTUID_PWD, phones=['+70002026011']),
    'trust_test_phone_user_12': User(4008816736, "yndx-tst-trust-phone-000012", CLIENTUID_PWD, phones=['+70002026012']),
    'trust_test_phone_user_13': User(4008816738, "yndx-tst-trust-phone-000013", CLIENTUID_PWD, phones=['+70002026013']),
    'trust_test_phone_user_14': User(4008816740, "yndx-tst-trust-phone-000014", CLIENTUID_PWD, phones=['+70002026014']),
    'trust_test_phone_user_15': User(4008816742, "yndx-tst-trust-phone-000015", CLIENTUID_PWD, phones=['+70002026015']),
    'trust_test_phone_user_16': User(4008816744, "yndx-tst-trust-phone-000016", CLIENTUID_PWD, phones=['+70002026016']),
    'trust_test_phone_user_17': User(4008816746, "yndx-tst-trust-phone-000017", CLIENTUID_PWD, phones=['+70002026017']),
    'trust_test_phone_user_18': User(4008816748, "yndx-tst-trust-phone-000018", CLIENTUID_PWD, phones=['+70002026018']),
    'trust_test_phone_user_19': User(4008816750, "yndx-tst-trust-phone-000019", CLIENTUID_PWD, phones=['+70002026019']),
    'trust_test_phone_user_20': User(4008816752, "yndx-tst-trust-phone-000020", CLIENTUID_PWD, phones=['+70002026020']),

    'trust_test_phone_user_21': User(4008816754, "yndx-tst-trust-phone-000021", CLIENTUID_PWD, phones=['+70002026021']),
    'trust_test_phone_user_22': User(4008816756, "yndx-tst-trust-phone-000022", CLIENTUID_PWD, phones=['+70002026022']),
    'trust_test_phone_user_23': User(4008816758, "yndx-tst-trust-phone-000023", CLIENTUID_PWD, phones=['+70002026023']),
    'trust_test_phone_user_24': User(4008816760, "yndx-tst-trust-phone-000024", CLIENTUID_PWD, phones=['+70002026024']),
    'trust_test_phone_user_25': User(4008816762, "yndx-tst-trust-phone-000025", CLIENTUID_PWD, phones=['+70002026025']),
    'trust_test_phone_user_26': User(4008816764, "yndx-tst-trust-phone-000026", CLIENTUID_PWD, phones=['+70002026026']),
    'trust_test_phone_user_27': User(4008816766, "yndx-tst-trust-phone-000027", CLIENTUID_PWD, phones=['+70002026027']),
    'trust_test_phone_user_28': User(4008816768, "yndx-tst-trust-phone-000028", CLIENTUID_PWD, phones=['+70002026028']),
    'trust_test_phone_user_29': User(4008816770, "yndx-tst-trust-phone-000029", CLIENTUID_PWD, phones=['+70002026029']),
    'trust_test_phone_user_30': User(4008816772, "yndx-tst-trust-phone-000030", CLIENTUID_PWD, phones=['+70002026030']),

    'trust_test_phone_user_31': User(4008816774, "yndx-tst-trust-phone-000031", CLIENTUID_PWD, phones=['+70002026031']),
    'trust_test_phone_user_32': User(4008816776, "yndx-tst-trust-phone-000032", CLIENTUID_PWD, phones=['+70002026032']),
    'trust_test_phone_user_33': User(4008816778, "yndx-tst-trust-phone-000033", CLIENTUID_PWD, phones=['+70002026033']),
    'trust_test_phone_user_34': User(4008816780, "yndx-tst-trust-phone-000034", CLIENTUID_PWD, phones=['+70002026034']),
    'trust_test_phone_user_35': User(4008816782, "yndx-tst-trust-phone-000035", CLIENTUID_PWD, phones=['+70002026035']),
    'trust_test_phone_user_36': User(4008816784, "yndx-tst-trust-phone-000036", CLIENTUID_PWD, phones=['+70002026036']),
    'trust_test_phone_user_37': User(4008816786, "yndx-tst-trust-phone-000037", CLIENTUID_PWD, phones=['+70002026037']),
    'trust_test_phone_user_38': User(4008816788, "yndx-tst-trust-phone-000038", CLIENTUID_PWD, phones=['+70002026038']),
    'trust_test_phone_user_39': User(4008816790, "yndx-tst-trust-phone-000039", CLIENTUID_PWD, phones=['+70002026039']),
    'trust_test_phone_user_40': User(4008816792, "yndx-tst-trust-phone-000040", CLIENTUID_PWD, phones=['+70002026040']),

    'trust_test_phone_user_41': User(4008816794, "yndx-tst-trust-phone-000041", CLIENTUID_PWD, phones=['+70002026041']),
    'trust_test_phone_user_42': User(4008816796, "yndx-tst-trust-phone-000042", CLIENTUID_PWD, phones=['+70002026042']),
    'trust_test_phone_user_43': User(4008816798, "yndx-tst-trust-phone-000043", CLIENTUID_PWD, phones=['+70002026043']),
    'trust_test_phone_user_44': User(4008816800, "yndx-tst-trust-phone-000044", CLIENTUID_PWD, phones=['+70002026044']),
    'trust_test_phone_user_45': User(4008816802, "yndx-tst-trust-phone-000045", CLIENTUID_PWD, phones=['+70002026045']),
    'trust_test_phone_user_46': User(4008816804, "yndx-tst-trust-phone-000046", CLIENTUID_PWD, phones=['+70002026046']),
    'trust_test_phone_user_47': User(4008816806, "yndx-tst-trust-phone-000047", CLIENTUID_PWD, phones=['+70002026047']),
    'trust_test_phone_user_48': User(4008816808, "yndx-tst-trust-phone-000048", CLIENTUID_PWD, phones=['+70002026048']),
    'trust_test_phone_user_49': User(4008816810, "yndx-tst-trust-phone-000049", CLIENTUID_PWD, phones=['+70002026049']),
    'trust_test_phone_user_50': User(4008816812, "yndx-tst-trust-phone-000050", CLIENTUID_PWD, phones=['+70002026050']),

    'trust_test_phone_user_51': User(4008816814, "yndx-tst-trust-phone-000051", CLIENTUID_PWD, phones=['+70002026051']),
    'trust_test_phone_user_52': User(4008816816, "yndx-tst-trust-phone-000052", CLIENTUID_PWD, phones=['+70002026052']),
    'trust_test_phone_user_53': User(4008816820, "yndx-tst-trust-phone-000053", CLIENTUID_PWD, phones=['+70002026053']),
    'trust_test_phone_user_54': User(4008816822, "yndx-tst-trust-phone-000054", CLIENTUID_PWD, phones=['+70002026054']),
    'trust_test_phone_user_55': User(4008816824, "yndx-tst-trust-phone-000055", CLIENTUID_PWD, phones=['+70002026055']),
    'trust_test_phone_user_56': User(4008816826, "yndx-tst-trust-phone-000056", CLIENTUID_PWD, phones=['+70002026056']),
    'trust_test_phone_user_57': User(4008816828, "yndx-tst-trust-phone-000057", CLIENTUID_PWD, phones=['+70002026057']),
    'trust_test_phone_user_58': User(4008816830, "yndx-tst-trust-phone-000058", CLIENTUID_PWD, phones=['+70002026058']),
    'trust_test_phone_user_59': User(4008816832, "yndx-tst-trust-phone-000059", CLIENTUID_PWD, phones=['+70002026059']),
    'trust_test_phone_user_60': User(4008816834, "yndx-tst-trust-phone-000060", CLIENTUID_PWD, phones=['+70002026060']),

    'trust_test_phone_user_61': User(4008816836, "yndx-tst-trust-phone-000061", CLIENTUID_PWD, phones=['+70002026061']),
    'trust_test_phone_user_62': User(4008816838, "yndx-tst-trust-phone-000062", CLIENTUID_PWD, phones=['+70002026062']),
    'trust_test_phone_user_63': User(4008816842, "yndx-tst-trust-phone-000063", CLIENTUID_PWD, phones=['+70002026063']),
    'trust_test_phone_user_64': User(4008816844, "yndx-tst-trust-phone-000064", CLIENTUID_PWD, phones=['+70002026064']),
    'trust_test_phone_user_65': User(4008816846, "yndx-tst-trust-phone-000065", CLIENTUID_PWD, phones=['+70002026065']),
    'trust_test_phone_user_66': User(4008816848, "yndx-tst-trust-phone-000066", CLIENTUID_PWD, phones=['+70002026066']),
    'trust_test_phone_user_67': User(4008816850, "yndx-tst-trust-phone-000067", CLIENTUID_PWD, phones=['+70002026067']),
    'trust_test_phone_user_68': User(4008816852, "yndx-tst-trust-phone-000068", CLIENTUID_PWD, phones=['+70002026068']),
    'trust_test_phone_user_69': User(4008816854, "yndx-tst-trust-phone-000069", CLIENTUID_PWD, phones=['+70002026069']),
    'trust_test_phone_user_70': User(4008816856, "yndx-tst-trust-phone-000070", CLIENTUID_PWD, phones=['+70002026070']),

    'trust_test_phone_user_71': User(4008816860, "yndx-tst-trust-phone-000071", CLIENTUID_PWD, phones=['+70002026071']),
    'trust_test_phone_user_72': User(4008816862, "yndx-tst-trust-phone-000072", CLIENTUID_PWD, phones=['+70002026072']),
    'trust_test_phone_user_73': User(4008816864, "yndx-tst-trust-phone-000073", CLIENTUID_PWD, phones=['+70002026073']),
    'trust_test_phone_user_74': User(4008816866, "yndx-tst-trust-phone-000074", CLIENTUID_PWD, phones=['+70002026074']),
    'trust_test_phone_user_75': User(4008816868, "yndx-tst-trust-phone-000075", CLIENTUID_PWD, phones=['+70002026075']),
    'trust_test_phone_user_76': User(4008816870, "yndx-tst-trust-phone-000076", CLIENTUID_PWD, phones=['+70002026076']),
    'trust_test_phone_user_77': User(4008816872, "yndx-tst-trust-phone-000077", CLIENTUID_PWD, phones=['+70002026077']),
    'trust_test_phone_user_78': User(4008816874, "yndx-tst-trust-phone-000078", CLIENTUID_PWD, phones=['+70002026078']),
    'trust_test_phone_user_79': User(4008816876, "yndx-tst-trust-phone-000079", CLIENTUID_PWD, phones=['+70002026079']),
    'trust_test_phone_user_80': User(4008816878, "yndx-tst-trust-phone-000080", CLIENTUID_PWD, phones=['+70002026080']),

    'trust_test_phone_user_81': User(4008816880, "yndx-tst-trust-phone-000081", CLIENTUID_PWD, phones=['+70002026081']),
    'trust_test_phone_user_82': User(4008816882, "yndx-tst-trust-phone-000082", CLIENTUID_PWD, phones=['+70002026082']),
    'trust_test_phone_user_83': User(4008816884, "yndx-tst-trust-phone-000083", CLIENTUID_PWD, phones=['+70002026083']),
    'trust_test_phone_user_84': User(4008816886, "yndx-tst-trust-phone-000084", CLIENTUID_PWD, phones=['+70002026084']),
    'trust_test_phone_user_85': User(4008816888, "yndx-tst-trust-phone-000085", CLIENTUID_PWD, phones=['+70002026085']),
    'trust_test_phone_user_86': User(4008816890, "yndx-tst-trust-phone-000086", CLIENTUID_PWD, phones=['+70002026086']),
    'trust_test_phone_user_87': User(4008816892, "yndx-tst-trust-phone-000087", CLIENTUID_PWD, phones=['+70002026087']),
    'trust_test_phone_user_88': User(4008816894, "yndx-tst-trust-phone-000088", CLIENTUID_PWD, phones=['+70002026088']),
    'trust_test_phone_user_89': User(4008816896, "yndx-tst-trust-phone-000089", CLIENTUID_PWD, phones=['+70002026089']),
    'trust_test_phone_user_90': User(4008816898, "yndx-tst-trust-phone-000090", CLIENTUID_PWD, phones=['+70002026090']),

    'trust_test_phone_user_91': User(4008816900, "yndx-tst-trust-phone-000091", CLIENTUID_PWD, phones=['+70002026091']),
    'trust_test_phone_user_92': User(4008816902, "yndx-tst-trust-phone-000092", CLIENTUID_PWD, phones=['+70002026092']),
    'trust_test_phone_user_93': User(4008816904, "yndx-tst-trust-phone-000093", CLIENTUID_PWD, phones=['+70002026093']),
    'trust_test_phone_user_94': User(4008816906, "yndx-tst-trust-phone-000094", CLIENTUID_PWD, phones=['+70002026094']),
    'trust_test_phone_user_95': User(4008816908, "yndx-tst-trust-phone-000095", CLIENTUID_PWD, phones=['+70002026095']),
    'trust_test_phone_user_96': User(4008816910, "yndx-tst-trust-phone-000096", CLIENTUID_PWD, phones=['+70002026096']),
    'trust_test_phone_user_97': User(4008816912, "yndx-tst-trust-phone-000097", CLIENTUID_PWD, phones=['+70002026097']),
    'trust_test_phone_user_98': User(4008816914, "yndx-tst-trust-phone-000098", CLIENTUID_PWD, phones=['+70002026098']),
    'trust_test_phone_user_99': User(4008816916, "yndx-tst-trust-phone-000099", CLIENTUID_PWD, phones=['+70002026099']),
    'trust_test_phone_user_100': User(4008816918, "yndx-tst-trust-phone-000100", CLIENTUID_PWD,
                                      phones=['+70002026100']),
}

all_ = test_passport.copy()
# all_.update(mimino.copy())
# all_ = mimino.copy()

uber = {
    'uber_user_1': User(4008866854, "", "", uber_user_id='uber_id_for_billing_1'),
    'uber_user_2': User(4008866858, "", "", uber_user_id='uber_id_for_billing_2'),
    'uber_user_3': User(4008866862, "", "", uber_user_id='uber_id_for_billing_3'),
    'uber_user_4': User(4008866864, "", "", uber_user_id='uber_id_for_billing_4'),
    'uber_user_5': User(4008866866, "", "", uber_user_id='uber_id_for_billing_5'),
}

new_yamoney_api = {
    'new_yamoney_api_user_1': User(599632695, "yndx-tst-trust-new-yam-1", CLIENTUID_PWD),
    'new_yamoney_api_user_2': User(599633360, "yndx-tst-trust-new-yam-2", CLIENTUID_PWD),
    'new_yamoney_api_user_3': User(599633568, "yndx-tst-trust-new-yam-3", CLIENTUID_PWD),
    'new_yamoney_api_user_4': User(599633728, "yndx-tst-trust-new-yam-4", CLIENTUID_PWD),
    'new_yamoney_api_user_5': User(599633894, "yndx-tst-trust-new-yam-5", CLIENTUID_PWD),
}

secret = {
    'test_wo_proxy_old': User(4012031722, "ydx11525774986660", "123456qW", ya_money='kirin',
                              ym_phones=['+79083527465', ]),
    'test_wo_proxy_new': User(4001311217, "balancesimpletestusr2", TRUSTTESTUSR_PWD),
    'test_proxy_new': User(4001311219, "balancesimpletestusr3", TRUSTTESTUSR_PWD),
    'test_with_phone': User(3000355852, "clientuidtest1", CLIENTUID_PWD, phones=['+79219702387', '+79211850416']),
    'mimino_wo_proxy_old': User(330696784, "balancesimpletestusr4", TRUSTTESTUSR_PWD, ya_money='koga'),
    'mimino_wo_proxy_new': User(330696869, "balancesimpletestusr5", TRUSTTESTUSR_PWD),
    'mimino_proxy_new': User(330696980, "balancesimpletestusr6", TRUSTTESTUSR_PWD),
    'just_a_user': User(4001311221, "balancesimpletestusr7", TRUSTTESTUSR_PWD)
}

fake = {
    'fake_user': User(112233, "some_login", "some_pwd", is_fake=True)
}

routing = {
    'rbs': User(4006396862, "yb-rbs-routing-test1", TRUSTTESTUSR_PWD)
}

phonishes_test = {
    'phonish-test-1': User(4010181324, phones=('+70000802827',), token='AQAAAADvBoLMAAABU4YEyy-fzk9Lhh4_R5MrQbk'),
    'phonish-test-2': User(4010181326, phones=('+70000813003',), token='AQAAAADvBoLOAAABUzN_on93TEJhjsC_p_Oo1sI'),
    'phonish-test-3': User(4010181330, phones=('+70000152776',), token='AQAAAADvBoLSAAABUwDlK8sh8EoXrCyQ9LDDBmk'),
    'phonish-test-4': User(4010181336, phones=('+70000334560',), token='AQAAAADvBoLYAAABUy8GmIBXEk5yrN0zALjGtx0'),
    'phonish-test-5': User(4010181338, phones=('+70000737756',), token='AQAAAADvBoLaAAABU2r0EYoiA0GQjnr0Iz--jf0'),
    'phonish-test-6': User(4010181342, phones=('+70000978143',), token='AQAAAADvBoLeAAABUz2aSjwzLUyzgihlSgLILKk'),
    'phonish-test-7': User(4010181344, phones=('+70000652526',), token='AQAAAADvBoLgAAABU3qx1Yn1R0hEjX2HzP8AjcM'),
    'phonish-test-8': User(4010181346, phones=('+70000203767',), token='AQAAAADvBoLiAAABU0G45Uf9ZE1PtkzDuch6J1s'),
    'phonish-test-9': User(4010181352, phones=('+70000108315',), token='AQAAAADvBoLoAAABUwsjjGT3KkK8kcXlW4ib8PA'),
    'phonish-test-10': User(4010181356, phones=('+70000802508',), token='AQAAAADvBoLsAAABU9-_GJFJ2ElVopBg0-aJ03g'),
    'phonish-test-11': User(4010181362, phones=('+70000022934',), token='AQAAAADvBoLyAAABUyhDCSs4JEYljR96L0barLY'),
    'phonish-test-12': User(4010181364, phones=('+70000946255',), token='AQAAAADvBoL0AAABU2v46rc9W01AoQoNpAR6p9U'),
    'phonish-test-13': User(4010181366, phones=('+70000879193',), token='AQAAAADvBoL2AAABU0kF3v8PYEqJgHZczCJtMSI'),
    'phonish-test-14': User(4010181368, phones=('+70000641225',), token='AQAAAADvBoL4AAABU9l5-R2ml095pVxNcJ1lMSw'),
    'phonish-test-15': User(4010181374, phones=('+70000508196',), token='AQAAAADvBoL-AAABU3AmaDCVtEr-t4tRSQOELPs'),
    'phonish-test-16': User(4010181376, phones=('+70000958094',), token='AQAAAADvBoMAAAABUyzNXpZukEEKhgcriYC_Cug'),
    'phonish-test-17': User(4010181380, phones=('+70000684669',), token='AQAAAADvBoMEAAABU2jHGjn050MluvZ18gOw3FY'),
    'phonish-test-18': User(4010181386, phones=('+70000912354',), token='AQAAAADvBoMKAAABUz3RmQhyBE1oopDpCTjzlQE'),
    'phonish-test-19': User(4010181392, phones=('+70000989260',), token='AQAAAADvBoMQAAABU9-h-vCITEXrmX9p9OogE84'),
    'phonish-test-20': User(4010181722, phones=('+70000103421',), token='AQAAAADvBoRaAAABUyZISnlsEUxklvwiR6yrG2s'),
}

phonishes_mimino = {
    'phonish-mimino-1': User(596081827, phones=('+70000152117',)),
    'phonish-mimino-2': User(596081848, phones=('+70000652984',)),
    'phonish-mimino-3': User(596081871, phones=('+70000800734',)),
    'phonish-mimino-4': User(596081888, phones=('+70000541729',)),
    'phonish-mimino-5': User(596081911, phones=('+70000989566',)),
    'phonish-mimino-6': User(596081933, phones=('+70000786517',)),
    'phonish-mimino-7': User(584410288, phones=('+70000570697',)),
    'phonish-mimino-8': User(596081977, phones=('+70000981147',)),
    'phonish-mimino-9': User(596081992, phones=('+70000854306',)),
    'phonish-mimino-10': User(596082019, phones=('+70000592446',)),
    'phonish-mimino-11': User(596082042, phones=('+70000633203',)),
    'phonish-mimino-12': User(596082076, phones=('+70000919653',)),
    'phonish-mimino-13': User(596082095, phones=('+70000668777',)),
    'phonish-mimino-14': User(596082123, phones=('+70000013812',)),
    'phonish-mimino-15': User(596082144, phones=('+70000858402',)),
    'phonish-mimino-16': User(596082166, phones=('+70000919279',)),
    'phonish-mimino-17': User(596082182, phones=('+70000692124',)),
    'phonish-mimino-18': User(596082203, phones=('+70000338155',)),
    'phonish-mimino-19': User(596082216, phones=('+70000883714',)),
    'phonish-mimino-20': User(596082233, phones=('+70000133884',)),
}
for login, user in phonishes_mimino.iteritems():
    user.token = get_secret(*TrustOauthTokens.TRUST_OAUTH_TOKENS)[login]


phonishes = phonishes_mimino.copy()
phonishes.update(phonishes_test)

# https://paste.yandex-team.ru/385858
with_linked_phonishes_test = {
    'with_linked_user_1': User(4010181898, "yndx-tst-trust-test-main-1", CLIENTUID_PWD,
                               linked_users=(User(4010181900,
                                                  phones=('+70000040911',)),)),
    'with_linked_user_2': User(4010181902, "yndx-tst-trust-test-main-2", CLIENTUID_PWD,
                               linked_users=(User(4010181904,
                                                  phones=('+70000803883',)),)),
    'with_linked_user_3': User(4010181906, "yndx-tst-trust-test-main-3", CLIENTUID_PWD,
                               linked_users=(User(4010181908,
                                                  phones=('+70000045027',)),)),
    'with_linked_user_4': User(4010181910, "yndx-tst-trust-test-main-4", CLIENTUID_PWD,
                               linked_users=(User(4010181912,
                                                  phones=('+70000547166',)),)),
    'with_linked_user_5': User(4010181914, "yndx-tst-trust-test-main-5", CLIENTUID_PWD,
                               linked_users=(User(4010181916,
                                                  phones=('+70000601526',)),)),
    'with_linked_user_6': User(4010181918, "yndx-tst-trust-test-main-6", CLIENTUID_PWD,
                               linked_users=(User(4010181920,
                                                  phones=('+70000345606',)),)),
    'with_linked_user_7': User(4010181922, "yndx-tst-trust-test-main-7", CLIENTUID_PWD,
                               linked_users=(User(4010181924,
                                                  phones=('+70000208360',)),)),
    'with_linked_user_8': User(4010181926, "yndx-tst-trust-test-main-8", CLIENTUID_PWD,
                               linked_users=(User(4010181928,
                                                  phones=('+70000782798',)),)),
    'with_linked_user_9': User(4010181930, "yndx-tst-trust-test-main-9", CLIENTUID_PWD,
                               linked_users=(User(4010181932,
                                                  phones=('+70000512303',)),)),
    'with_linked_user_10': User(4010181934, "yndx-tst-trust-test-main-10", CLIENTUID_PWD,
                                linked_users=(User(4010181936,
                                                   phones=('+70000906271',)),)),
    'with_linked_user_11': User(4010181938, "yndx-tst-trust-test-main-11", CLIENTUID_PWD,
                                linked_users=(User(4010181940,
                                                   phones=('+70000190927',)),)),
    'with_linked_user_12': User(4010181942, "yndx-tst-trust-test-main-12", CLIENTUID_PWD,
                                linked_users=(User(4010181944,
                                                   phones=('+70000649224',)),)),
    'with_linked_user_13': User(4010181946, "yndx-tst-trust-test-main-13", CLIENTUID_PWD,
                                linked_users=(User(4010181948,
                                                   phones=('+70000522613',)),)),
    'with_linked_user_14': User(4010181950, "yndx-tst-trust-test-main-14", CLIENTUID_PWD,
                                linked_users=(User(4010181952,
                                                   phones=('+70000187379',)),)),
    'with_linked_user_15': User(4010181954, "yndx-tst-trust-test-main-15", CLIENTUID_PWD,
                                linked_users=(User(4010181956,
                                                   phones=('+70000044308',)),)),
    'with_linked_user_16': User(4010181958, "yndx-tst-trust-test-main-16", CLIENTUID_PWD,
                                linked_users=(User(4010181960,
                                                   phones=('+70000026840',)),)),
    'with_linked_user_17': User(4010181964, "yndx-tst-trust-test-main-17", CLIENTUID_PWD,
                                linked_users=(User(4010181966,
                                                   phones=('+70000448908',)),)),
}
for key, user in with_linked_phonishes_test.iteritems():
    user.linked_users[0].token = get_secret(*TrustOauthTokens.TRUST_OAUTH_TOKENS)[user.login + '-linked']

with_linked_phonishes = with_linked_phonishes_test.copy()

users_autoremove = {
    'trust-autoremove-1': User(4012959650, "yndx-trust-autoremove-1", CLIENTUID_PWD),
    'trust-autoremove-2': User(4012959662, "yndx-trust-autoremove-2", CLIENTUID_PWD),
    'trust-autoremove-3': User(4012961184, "yndx-trust-autoremove-3", CLIENTUID_PWD),
    'trust-autoremove-4': User(4012963668, "yndx-trust-autoremove-4", CLIENTUID_PWD),
    'trust-autoremove-5': User(4012963896, "yndx-trust-autoremove-5", CLIENTUID_PWD),
    'trust-autoremove-6': User(4012964126, "yndx-trust-autoremove-6", CLIENTUID_PWD),
    'trust-autoremove-7': User(4012964246, "yndx-trust-autoremove-7", CLIENTUID_PWD),
    'trust-autoremove-8': User(4012964326, "yndx-trust-autoremove-8", CLIENTUID_PWD),
    'trust-autoremove-9': User(4012964424, "yndx-trust-autoremove-9", CLIENTUID_PWD),
    'trust-autoremove-10': User(4012964442, "yndx-trust-autoremove-10", CLIENTUID_PWD),
    'trust-autoremove-11': User(4012964468, "yndx-trust-autoremove-11", CLIENTUID_PWD),
    'trust-autoremove-12': User(4012964516, "yndx-trust-autoremove-12", CLIENTUID_PWD),
    'trust-autoremove-13': User(4012964538, "yndx-trust-autoremove-13", CLIENTUID_PWD),
    'trust-autoremove-14': User(4012964556, "yndx-trust-autoremove-14", CLIENTUID_PWD),
    'trust-autoremove-15': User(4012964566, "yndx-trust-autoremove-15", CLIENTUID_PWD),
    'trust-autoremove-16': User(4012964578, "yndx-trust-autoremove-16", CLIENTUID_PWD),
    'trust-autoremove-17': User(4012964582, "yndx-trust-autoremove-17", CLIENTUID_PWD),
    'trust-autoremove-18': User(4012964584, "yndx-trust-autoremove-18", CLIENTUID_PWD),
    'trust-autoremove-19': User(4012964588, "yndx-trust-autoremove-19", CLIENTUID_PWD),
    'trust-autoremove-20': User(4012964590, "yndx-trust-autoremove-20", CLIENTUID_PWD),
    'trust-autoremove-21': User(4012964592, "yndx-trust-autoremove-21", CLIENTUID_PWD),
    'trust-autoremove-22': User(4012964594, "yndx-trust-autoremove-22", CLIENTUID_PWD),
    'trust-autoremove-23': User(4012964600, "yndx-trust-autoremove-23", CLIENTUID_PWD),
    'trust-autoremove-24': User(4012964606, "yndx-trust-autoremove-24", CLIENTUID_PWD),
    'trust-autoremove-25': User(4012964608, "yndx-trust-autoremove-25", CLIENTUID_PWD),
    'trust-autoremove-26': User(4012964610, "yndx-trust-autoremove-26", CLIENTUID_PWD),
    'trust-autoremove-27': User(4012964614, "yndx-trust-autoremove-27", CLIENTUID_PWD),
    'trust-autoremove-28': User(4012964616, "yndx-trust-autoremove-28", CLIENTUID_PWD),
    'trust-autoremove-29': User(4012964622, "yndx-trust-autoremove-29", CLIENTUID_PWD),
    'trust-autoremove-30': User(4012964626, "yndx-trust-autoremove-30", CLIENTUID_PWD),
}

autoremove = users_autoremove.copy()

yamoney_wallet = {
    'yamoney-wallet-1': User(4013523430, "base1531400376936", "123456qW", phones=['+79085043465', ]),
    'yamoney-wallet-2': User(4013526120, "base1531410974057", "123456qW", phones=['+79081431745', ]),
    'yamoney-wallet-3': User(4013526124, "base1531411028691", "123456qW", phones=['+79087118065', ]),
    'yamoney-wallet-4': User(4013526126, "base1531411071192", "123456qW", phones=['+79086854066', ]),
    'yamoney-wallet-5': User(4013526128, "base1531411111071", "123456qW", phones=['+79086263514', ]),
    'yamoney-wallet-6': User(4013526136, "base1531411159163", "123456qW", phones=['+79083100887', ]),
    'yamoney-wallet-7': User(4013526142, "base1531411186302", "123456qW", phones=['+79087818033', ]),
    'yamoney-wallet-8': User(4013526150, "base1531411234617", "123456qW", phones=['+79082242530', ]),
    'yamoney-wallet-9': User(4013526156, "base1531411272481", "123456qW", phones=['+79087573335', ]),
    'yamoney-wallet-10': User(4013526168, "base1531411305607", "123456qW", phones=['+79086010578', ]),
    'yamoney-wallet-11': User(4013526176, "base1531411363556", "123456qW", phones=['+79081305768', ]),
    'yamoney-wallet-12': User(4013526196, "base1531411434014", "123456qW", phones=['+79083708028', ]),
    'yamoney-wallet-13': User(4013526206, "base1531411476460", "123456qW", phones=['+79088513723', ]),
    'yamoney-wallet-14': User(4013526234, "base1531411580678", "123456qW", phones=['+79087731515', ]),
    'yamoney-wallet-15': User(4013526240, "base1531411630054", "123456qW", phones=['+79083438401', ]),
    'yamoney-wallet-16': User(4013526242, "base1531411706133", "123456qW", phones=['+79087234738', ]),
    'yamoney-wallet-17': User(4013526244, "base1531411731931", "123456qW", phones=['+79085374377', ]),
    'yamoney-wallet-18': User(4013526246, "base1531411772396", "123456qW", phones=['+79080130108', ]),
    'yamoney-wallet-19': User(4013526248, "base1531411805668", "123456qW", phones=['+79081111184', ]),
    'yamoney-wallet-20': User(4013526250, "base1531411849037", "123456qW", phones=['+79081668830', ]),
}

yamoney = yamoney_wallet.copy()

passport_users_for_kp = {
    # удалил чтобы не тянуть лишние секреты. Если что, эти пользователи есть в репе trust-tests
}

with_linked_phonishes_for_kp = {
    # удалил чтобы не тянуть лишние секреты. Если что, эти пользователи есть в репе trust-tests
}
for key, user in with_linked_phonishes_for_kp.iteritems():
    user.linked_users[0].token = get_secret(*TrustOauthTokens.TRUST_OAUTH_TOKENS)[user.login + '-linked']

temp_users = {
    'trust_temp_1': User(4013968260, "yndx-temp-test-1", CLIENTUID_PWD),
    'trust_temp_2': User(4013968264, "yndx-temp-test-2", CLIENTUID_PWD),
    'trust_temp_3': User(4013968274, "yndx-temp-test-3", CLIENTUID_PWD),
    'trust_temp_4': User(4013968276, "yndx-temp-test-4", CLIENTUID_PWD),
}


class Type(object):
    def __init__(self, pool=None, name=None, ya_money=None, has_ym_phones=False,
                 has_phones=False, anonymous=False, phonish=False):
        self.pool = pool
        self.name = name
        self.ya_money = ya_money
        self.has_ym_phones = has_ym_phones
        self.has_phones = has_phones
        self.anonymous = anonymous
        self.phonish = phonish

    def __str__(self):
        return '<User type: ' \
               'name={}, ' \
               'ya_money={}, ' \
               'has_ym_phones={}, ' \
               'has_phones={}, ' \
               'anonymous={}, ' \
               'phonish={}>'.format(self.name,
                                    self.ya_money,
                                    self.has_ym_phones,
                                    self.has_phones,
                                    self.anonymous,
                                    self.phonish)


class Types(object):
    random_from_mimino = Type(pool=mimino)
    random_from_test_passport = Type(pool=test_passport)
    random_from_all = Type(pool=all_)
    random_from_mutable = Type(pool=mutable)
    random_for_sberbank = Type(pool=sberbank)
    random_for_rbs = Type(pool=rbs)
    random_for_ecommpay = Type(pool=ecommpay)
    random_for_new_yamoney_api = Type(pool=new_yamoney_api)
    random_with_phone = Type(pool=phone_test_passport, has_phones=True)
    anonymous = Type(anonymous=True)
    with_phone = Type(pool=secret, has_phones=True)
    uber = Type(pool=uber)
    random_from_phonishes = Type(pool=phonishes, phonish=True, has_phones=True)
    random_with_linked_phonishes = Type(pool=with_linked_phonishes)
    fake = Type(pool=fake)
    random_afs = Type(pool=afs)
    random_autoremove = Type(pool=autoremove)
    random_yamoney_wallet = Type(pool=yamoney, has_phones=True)


def get_random_of_type(type_, num=1):
    if isinstance(type_, User):
        return type_
    if type_.anonymous:
        return anonymous
    if type_.name:
        user = type_.pool.get(type_.name)
        if not user:
            raise UidsPoolException("There isn't uid with name {]".format(type_.name))
        return user

    elif type_.pool:
        filtered_pool = {k: v for k, v in type_.pool.iteritems() if v.matches_type(type_)}
        if not filtered_pool:
            raise UidsPoolException("There isn't uid corresponding to type {}".format(type_))
        return get_random_of(filtered_pool, num=num)


max_tries = 20


def get_random_of(pool, try_=1, num=1):
    if try_ > max_tries:
        raise UidsPoolException('Can not choose free user for test for {} attempts. '
                                'Try to clear users holder by maintenance.test_clear_users_holder.py'
                                .format(max_tries))

    if num == 1:
        with reporter.step(u'Выбираем случайного пользователя для теста'):
            user = pool[random.choice(pool.keys())]

            if users_holder_mode_enabled() and user_is_marked(user):
                return get_random_of(pool, try_=try_ + 1)

            if users_holder_mode_enabled():
                current_pid = str(os.getpid())
                if users_holder.get(current_pid):
                    users_holder[current_pid].append(user)
                else:
                    users_holder.update({current_pid: [user, ]})
                mark_user(user)
            if pool == autoremove:
                db_steps.bs().delete_user_info(user.uid)
            return user
    else:
        return [pool[x] for x in random.sample(pool.keys(), num)]


def get_random_except_of(pool, user):
    tmp_user = get_random_of(pool)
    tries = 0
    max_tries = 20
    while user == tmp_user:
        tries += 1
        tmp_user = get_random_of(pool)
        if tries >= max_tries:
            raise UidsPoolException(u"Couldn't find user which differs from %s in %s uids pool" % (user, pool))
    return tmp_user


def get_by_uid_in_secret(uid):
    for user in secret.values():
        if user.uid == uid:
            return user
    return None
    # raise UidsPoolException('No user with uid {} in secret pool'.format(uid))


def mark_user(user):
    try:
        utils.s3storage().set_string_value(key=user.uid, value='1')
    except S3ResponseError:
        return


def unmark_user(user):
    try:
        utils.s3storage().set_string_value(key=user.uid, value='0')
    except S3ResponseError:
        return


def get_user_mark(user):
    try:
        return utils.s3storage().get_string_value(key=user.uid)
    except S3ResponseError:
        return '0'


def user_is_marked(user):
    try:
        return get_user_mark(user) == '1'
    except S3ResponseError:
        return False


def users_holder_mode_enabled():
    return os.environ.get('USERS_HOLDER_MODE', '0') == '1'


def get_all_uids_from_pools(pools):
    if not isinstance(pools, (list, tuple)):
        pools = (pools,)
    return [user.uid for pool in pools for user in pool.values()]


def search_user_in_pools(pools, uid):
    uid = str(uid)
    for pool in pools if isinstance(pools, (list, tuple)) else [pools]:
        for user in pool.values():
            if uid == user.uid:
                return user
        return None


class UidsPoolException(Exception):
    pass
