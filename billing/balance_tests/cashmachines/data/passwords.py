# coding: utf-8
__author__ = 'a-vasin'

# mysecret для случаев, когда sn = None
DEFAULT_MYSECRET = u'xxx_oke_666'

# временный хардкод для автору
SUPER_SECRET_AUTORU_TVM_TIKEN = '666:PMfsczcpDUjoKLqqKv2yv3Ijp3sMvT-ycp4lzK22ez3BY2n5TQdVFSSopER3a1f9DUkE2OLdNFjuxcfGF'

CASHMACHINES_PASSWORDS = {
    # u'00000003820034331902': 351426,
    u'6664608727292649': 666666,
    u"00000003820034331904": 666666,
    u'00000000381001942057': 204153,
    u'00000000381001543159': 164756,
    u'00000000381007528533': 755904,
    u'00000000381004956051': 501832,
    u'00000000381002827554': 291610,
    u'00000000381005563105': 561788,
    u"00000003820034331903": 666666,
    u"00000003820034331904": 666666,
    u"00000003820034331905": 666666,
    # u'00000000381007739900': 776780  # боевой ФН
}


def mysecret(serial_number):
    return u'xxx_oke_{}'.format(serial_number[-4:])


def payload(serial_number):
    return {u"password": CASHMACHINES_PASSWORDS[serial_number]}
