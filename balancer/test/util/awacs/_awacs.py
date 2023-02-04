# -*- coding: utf-8 -*-
import json
import netifaces
import ipaddress
from collections import defaultdict
from balancer.test.util.config import BaseBalancerConfig


class Instance(object):
    def __init__(self, host, port, weight, cached_ip):
        super(Instance, self).__init__()
        self.__host = host
        self.__port = port
        self.__weight = weight
        self.__cached_ip = cached_ip

    @property
    def host(self):
        return self.__host

    @property
    def port(self):
        return self.__port

    @property
    def weight(self):
        return self.__weight

    @property
    def cached_ip(self):
        return self.__cached_ip

    def __repr__(self):
        return '{{ host = {host}, port = {port}, weight = {weight}, cached_ip = {cached_ip} }}'.format(
            host=self.host, port=self.port, weight=self.weight, cached_ip=self.cached_ip
        )

    def __eq__(self, other):
        result = isinstance(other, Instance) and self.host == other.host and self.port == other.port
        if result:
            assert self.__cached_ip == other.__cached_ip
        return result

    def __hash__(self):
        return hash((self.host, self.port))


class Slice(object):
    def __init__(self, index, backends, instances):
        super(Slice, self).__init__()
        self.__index = index
        self.__backends = backends
        self.__instances = instances

    @property
    def index(self):
        return self.__index

    @property
    def backends(self):
        return self.__backends

    @property
    def instances(self):
        return self.__instances

    def intersect(self, other, next_index):
        if self.__instances == other.__instances:
            self.__backends.update(other.__backends)
            other.__backends.update(self.__backends)
        elif self.__instances < other.__instances:
            self.__backends.update(other.__backends)
            other.__instances -= self.__instances
        elif self.__instances > other.__instances:
            other.__backends.update(self.__backends)
            self.__instances -= other.__instances
        else:
            common_instances = self.__instances & other.__instances
            if not common_instances:
                return None
            self.__instances -= common_instances
            other.__instances -= common_instances
            return Slice(next_index, self.__backends | other.__backends, common_instances)

    @classmethod
    def from_list(cls, index, group, instances):
        return cls(index, {group}, {Instance(**inst) for inst in instances})


def split_backends(backends):
    result = list()
    for name, instances in backends.iteritems():
        next_slice = Slice.from_list(len(result), name, instances)
        new_slices = [next_slice]
        for cur_slice in result:
            intersect_result = cur_slice.intersect(next_slice, len(result) + len(new_slices))
            if intersect_result:
                new_slices.append(intersect_result)
        result.extend(new_slices)
    return result


class BackendsJson(object):
    def __init__(self, path, port_manager):
        super(BackendsJson, self).__init__()
        with open(path, 'r') as f:
            backends_json = {k: v for k, v in json.load(f).iteritems() if k.startswith('backends_')}
        slices = split_backends(backends_json)

        self.__slices = {slc.index: sorted(slc.instances) for slc in slices}
        self.__backends = defaultdict(set)

        for slc in slices:
            for name in slc.backends:
                self.__backends[name].add(slc.index)

        self.__ports = dict()
        for slc in slices:
            self.__ports[slc.index] = port_manager.get_port()  # unique port for each slice

    def get_ports(self, backends, exclude_backends=None):
        if exclude_backends is None:
            exclude_backends = list()
        ids = set()
        for name in backends:
            ids |= self.__backends[name]
        for name in exclude_backends:
            ids -= self.__backends[name]
        return {self.__ports[i] for i in ids}


def get_nonlocal_ips():
    for interface in netifaces.interfaces():
        addresses = netifaces.ifaddresses(interface)
        for addr_info in addresses.get(netifaces.AF_INET, []) + addresses.get(netifaces.AF_INET6, []):
            if '%' in addr_info['addr'] or not ipaddress.ip_address(unicode(addr_info['addr'], 'utf-8')).is_loopback:
                yield addr_info['addr']


def load_addrs(addrs):
    if isinstance(addrs, dict):
        addrs = addrs.values()
    result = []
    for addr in addrs:
        if addr['ip'] == '*':
            result += [(ip, addr['port']) for ip in get_nonlocal_ips()]
        else:
            result.append((addr['ip'], addr['port']))
    return result


def build_addr_map(addrs, port_manager):
    result = dict()
    for addr in addrs:
        result[addr] = port_manager.get_port()
    return result


class AwacsConfig(BaseBalancerConfig):
    def __init__(self, path, log_dir, private_cert_dir, public_cert_dir, port_manager, custom_params):
        super(AwacsConfig, self).__init__()
        self.__path = path
        self.add_param('log_dir', log_dir)
        self.add_param('private_cert_dir', private_cert_dir)
        self.add_param('public_cert_dir', public_cert_dir)
        json_conf = self.as_json()

        self.__addrs = load_addrs(json_conf['addrs'])
        self.__default_addr = self.__addrs[0]

        assert 'admin_addrs' in json_conf  # FIXME
        self.__admin_addrs = load_addrs(json_conf['admin_addrs'])
        self.__default_admin_addr = self.__admin_addrs[0]

        self.__addr_map = build_addr_map(self.__addrs, port_manager)
        self.__addr_map.update(build_addr_map(self.__admin_addrs, port_manager))

        if 'private_address' in json_conf:
            self.__private_address = [json_conf['private_address']]
        else:
            self.__private_address = ['127.0.0.1', '::1']

        for name, value in custom_params.iteritems():
            self.add_param(name, value)

    def find_port(self, addr):
        return self.__addr_map[addr]

    @property
    def port(self):
        return self.find_port(self.__default_addr)

    @property
    def admin_port(self):
        return self.find_port(self.__default_admin_addr)

    @property
    def addr_map(self):
        return self.__addr_map

    @property
    def private_address(self):
        return self.__private_address

    def get_path(self):
        return self.__path


class AwacsManager(object):
    def __init__(self, bundle, backends_json, config_path, fs_manager):
        super(AwacsManager, self).__init__()
        self.__bundle = bundle
        self.__backends_json = backends_json
        self.__config_path = config_path

    @property
    def bundle(self):
        return self.__bundle

    @property
    def backends_json(self):
        return self.__backends_json

    @property
    def config_path(self):
        return self.__config_path
