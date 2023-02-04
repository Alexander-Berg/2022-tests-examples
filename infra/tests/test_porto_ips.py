#! /usr/bin/env python2

import mock
import unittest
import ipaddr
import porto

from hbfagent.mod import porto_ips


class TestPortoIPs(unittest.TestCase):

    porto_reply = {
        u"ISS-AGENT--8100_production_sas_imgsthwide_7mNJbEez13B": {u"ip": u""},
        u"ISS-AGENT--8100_production_sas_imgsthwide_7mNJbEez13B/iss_hook_start": {u"ip": u""},
        u"ISS-AGENT--8100_production_sas_imgsthwide_GzTAIiaThnK": {u"ip": u""},
        u"ISS-AGENT--8101_prodduction_sas_imgsthwide_FgCGK6SassR": {u"ip": u""},
        u"ISS-AGENT--8101_production_sas_imgsthwide_zpBMCLghz6B": {u"ip": u""},
        u"ISS-AGENT--8101_production_sas_imgsthwide_zpBMCLghz6B/iss_hook_start": {u"ip": u""},
        u"ISS-AGENT--8102_production_sas_imgsthwide_0CDZbvggDaP": {u"ip": u""},
        u"ISS-AGENT--8102_production_sas_imgsthwide_0CDZbvggDaP/iss_hook_start": {u"ip": u""},
        u"ISS-AGENT--8102_production_sas_imgsthwide_uFeTDCNxPfQ": {u"ip": u""},
        u"iss-agent": {u"ip": u""},
        u"net64_check": {u"ip": u""},
        u"net64_check/vlan688": {u"ip": u"vlan688 2a02:6b8:c08:6a08:0:604:0:2", "labels": ""},
        u"net64_check/vlan788": {u"ip": u"vlan788 2a02:6b8:fc00:6a08:0:604:0:2", "labels": "HBF.ignore_address: 0"},
        u"net64_check/vlan7255": {u"ip": u"vlan7255 7255::7255", "labels": "HBF.ignore_address: 1"},
        u"net64_check/vlan7256": {u"ip": u"vlan7256 7255::7256", "labels": "HBF.address_ignore: 0; HBF.ignore_address: 1; HBF.address: -1"},
        u"psi": {u"ip": u""},
        u"psi/sas1-1470-17292.vm.search.yandex.net": {u"ip": u"vlan688 2a02:6b8:c08:6a08:10b:6792:9:4866;vlan688 2a02:6b8:fc00:6a08:10b:6792:9:4866"},
        u"psi/sas1-1470-26625.vm.search.yandex.net": {u"ip": u"vlan688 2a02:6b8:c08:6a08:10b:6791:15:7221;vlan688 2a02:6b8:fc00:6a08:10b:6791:15:7221"},
        u"skycore": {u"ip": u""},
        u"skycore/infra": {u"ip": u""},
        u"skycore/infra/netmonagent": {u"ip": u""},
        u"skycore/infra/netmonagent/skycore-382fc782-5315-4826-6974-40417902f034": {u"ip": u""}
    }

    porto_ips = {
        ipaddr.IPv6Address(u"2a02:6b8:c08:6a08:10b:6792:9:4866"),
        ipaddr.IPv6Address(u"2a02:6b8:fc00:6a08:10b:6792:9:4866"),
        ipaddr.IPv6Address(u"2a02:6b8:c08:6a08:10b:6791:15:7221"),
        ipaddr.IPv6Address(u"2a02:6b8:fc00:6a08:10b:6791:15:7221"),
        ipaddr.IPv6Address(u"2a02:6b8:fc00:6a08:0:604:0:2"),
        ipaddr.IPv6Address(u"2a02:6b8:c08:6a08:0:604:0:2"),
    }

    def test_run(self):
        with mock.patch("porto.Connection") as con:
            with mock.patch("hbfagent.config.Config.__getitem__"):
                con.return_value.Get.return_value = self.porto_reply
                ips = porto_ips.run()
                self.assertEqual(ips, self.porto_ips)

    def test_exception(self):
        with mock.patch("porto.Connection") as con:
            with mock.patch("hbfagent.config.Config.__getitem__"):
                con.side_effect = porto.exceptions.SocketTimeout('Timeout')
                with self.assertRaises(porto.exceptions.SocketTimeout):
                    porto_ips.run()


if __name__ == "__main__":
    unittest.main()
