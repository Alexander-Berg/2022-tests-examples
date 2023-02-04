import dns.rdatatype

import ipaddress


# constants
BACKSLASH_CHAR = '\\'


# solomon, sensors

def get_sensors(sensors, **labels):
    return filter(lambda sensor: labels.items() <= sensor["labels"].items(), sensors["sensors"])


def get_sensor(sensors, **labels):
    return next(get_sensors(sensors, **labels), None)


def get_sensors_values(sensors, **labels):
    return map(lambda sensor: sensor["value"], get_sensors(sensors, **labels))


def get_sensor_value(sensors, **labels):
    return next(get_sensors_values(sensors, **labels), None)


# dns stuff
def get_one_soa(resp, section):
    if section == 'ANSWER':
        rrsets = resp.answer
    elif section == 'AUTHORITY':
        rrsets = resp.authority

    assert len(rrsets) == 1
    rrset = rrsets[0]
    assert len(rrset) == 1
    assert rrset[0].rdtype == dns.rdatatype.SOA
    return rrset[0]


def get_soa_serial(resp, section):
    return get_one_soa(resp, section).serial


# project id stuff

def substitute_project_id(ip6_address, project_id):
    full = ipaddress.IPv6Address(ip6_address).exploded
    splitted = full.split(':')
    splitted[4], splitted[5] = format(project_id >> 16, 'x'), format(project_id & 0xffff, 'x')
    return ipaddress.IPv6Address(':'.join(splitted)).compressed
