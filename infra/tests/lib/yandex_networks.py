#!/usr/bin/env python3

import collections
import ipaddress
import requests
import bisect

if __name__ == '__main__':
    import defs
else:
    from . import defs


class YandexNetwork:

    @classmethod
    def get_dc(self, location):
        if location.startswith(u"Сасово-2"):
            return defs.DC.SAS2
        elif location.startswith(u"Сасово-1"):
            return defs.DC.SAS1
        elif location.startswith(u"Владимир AZ"):
            return defs.DC.VLX
        elif any(location.startswith(s) for s in [u"Владимир", u"лаборатория Владимир"]):
            return defs.DC.VLA
        elif location.startswith(u"Мянтсяля"):
            return defs.DC.MAN
        elif location.startswith(u"Ивантеевка"):
            return defs.DC.IVA
        elif location.startswith(u"Мытищи"):
            return defs.DC.MYT
        else:
            return None

    @classmethod
    def get_nettype(self, nettype):
        if nettype == "backbone":
            return defs.NETTYPE.BACKBONE
        elif nettype == "fastbone":
            return defs.NETTYPE.FASTBONE
        else:
            return None

    networks = []
    grouped_networks = collections.defaultdict(lambda: collections.defaultdict(list))

    @classmethod
    def download_networks(self):
        r = requests.get("https://ro.racktables.yandex-team.ru/export/networklist.php?report=perdc-agg")
        for line in r.text.splitlines():
            net, location, nettype = line.split("\t")
            dc = YandexNetwork.get_dc(location)
            nettype = YandexNetwork.get_nettype(nettype)
            if dc is not None and nettype is not None and net.startswith("2a02:6b8"):
                net = ipaddress.IPv6Network(net)
                for n in self.networks:
                    assert not net.subnet_of(n.net), "{} is subnet of {}".format(net, n.net)
                    assert not n.net.subnet_of(net), "{} is subnet of {}".format(n.net, net)
                net = YandexNetwork(dc, nettype, net)
                bisect.insort(self.networks, net)
                bisect.insort(self.grouped_networks[dc][nettype], net)

    @classmethod
    def get_networks(self):
        if not self.networks:
            self.download_networks()
        return self.networks

    @classmethod
    def get_grouped_networks(self):
        if not self.grouped_networks:
            self.download_networks()
        return self.grouped_networks

    l3_networks = []

    @classmethod
    def download_l3_networks(self):
        r = requests.get("https://ro.racktables.yandex-team.ru/export/networklist.php?report=slb")
        for net in r.text.splitlines():
            if net.startswith("2a02") or net.startswith("2620"):
                net = ipaddress.IPv6Network(net)
                skip = False
                for n in self.l3_networks:
                    if net.subnet_of(n):
                        skip = True
                        break
                    if n.subnet_of(net):
                        self.l3_networks.remove(n)
                if not skip:
                    bisect.insort(self.l3_networks, net)

    @classmethod
    def get_l3_networks(self):
        if not self.l3_networks:
            self.download_l3_networks()
        return self.l3_networks

    def __init__(self, dc, nettype, net):
        if None in (dc, nettype, net):
            raise ValueError("Invalid params in YandexNetwork: {}".format((dc, nettype, net)))
        self.dc = dc
        self.nettype = nettype
        self.net = net

    def __lt__(self, other):
        return ((self.dc.value, self.nettype.value, self.net) < (other.dc.value, other.nettype.value, other.net))

    def __eq__(self, other):
        return ((self.dc, self.nettype, self.net) == (other.dc, other.nettype, other.net))

    def __hash__(self):
        return hash((self.dc, self.nettype, self.net))

    def __repr__(self):
        return "{} {} {}".format(self.net, self.dc.name, self.nettype.name.lower())


def get_networks():
    return YandexNetwork.get_networks()


def get_grouped_networks():
    return YandexNetwork.get_grouped_networks()


def get_nettype_networks(nettype):
    nets = get_grouped_networks()

    nettype_nets = []
    for dc in defs.DC:
        nettype_nets.extend(nets[dc][nettype])

    return nettype_nets


def get_dc_networks(dc):
    nets = get_grouped_networks()

    dc_nets = []
    for nettype in defs.NETTYPE:
        dc_nets.extend(nets[dc][nettype])

    return dc_nets


def get_l3_networks():
    return YandexNetwork.get_l3_networks()


def main():
    import argparse

    parser = argparse.ArgumentParser(description="Get Yandex networks from racktables")
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument("--l3", action="store_true")
    args = parser.parse_args()

    if not args.l3:
        nets = get_networks()
        for n in nets:
            print(n if args.verbose else n.net)
    else:
        nets = get_l3_networks()
        for n in nets:
            print(n)

if __name__ == "__main__":
    main()
