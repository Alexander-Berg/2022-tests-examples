import os
import flask
import time
import json
import uuid
import pytest
import urllib
import base64
import select
import socket
import logging
import threading
import requests_unixsocket
from six.moves import http_client
from waitress.server import UnixWSGIServer
from yp_proto.yp.client.api.proto import cluster_api_pb2
from infra.qyp.proto_lib import vmset_pb2
from infra.qyp.vmagent.src.config import VmagentContext, PodSpec


class StopableUnixWSGIServer(UnixWSGIServer):
    was_shutdown = False

    def __init__(self, application, *args, **kwargs):
        super(StopableUnixWSGIServer, self).__init__(application, *args, **kwargs)
        self.runner = None
        self.test_app = application
        self.application_url = 'http://%s:%s/' % (self.adj.host, self.adj.port)

    def run(self):
        """Run the server"""
        try:
            self.asyncore.loop(.5, map=self._map)
        except select.error:  # pragma: no cover
            if not self.was_shutdown:
                raise

    def shutdown(self):
        """Shutdown the server"""
        # avoid showing traceback related to asyncore
        self.was_shutdown = True
        self.logger.setLevel(logging.FATAL)
        while self._map:
            triggers = list(self._map.values())
            for trigger in triggers:
                trigger.handle_close()
        self.maintenance(0)
        self.task_dispatcher.shutdown()
        return True

    @classmethod
    def create(cls, application, **kwargs):
        """
        Start a server to serve ``application``. Return a server
        instance."""
        if 'expose_tracebacks' not in kwargs:
            kwargs['expose_tracebacks'] = True
        server = cls(application, **kwargs)
        server.runner = threading.Thread(target=server.run)
        server.runner.daemon = True
        server.runner.start()
        return server

    def check_server(self, host, port, path_info='/', timeout=3, retries=30):
        """Perform a request until the server reply"""
        if retries < 0:
            return 0
        time.sleep(.3)
        for i in range(retries):
            try:
                conn = http_client.HTTPConnection(host, int(port), timeout=timeout)
                conn.request('GET', path_info)
                res = conn.getresponse()
                return res.status
            except (socket.error, http_client.HTTPException):
                time.sleep(.3)
        return 0

    def wait(self, retries=30):
        """Wait until the server is started"""
        running = self.check_server(self.adj.host, self.adj.port,
                                    '/__application__', retries=retries)
        if running:
            return True
        try:
            self.shutdown()
        finally:
            return False


@pytest.fixture
def pod_agent_socket():
    app = flask.Flask(__name__)

    unix_socket_path = "/tmp/{}.sock".format(uuid.uuid4())

    @app.route('/pod_spec')
    def pod_spec():
        return flask.jsonify({
            "issPayload": "CrYWMhMKB0hCRl9OQVQSCGRpc2FibGVkMlgKGm5hbm55X2NvbnRhaW5lcl9hY2Nlc3NfdXJsEjpodHRwczovL3ZtcHJv"
                          "eHkuc2FzLXN3YXQueWFuZGV4LXRlYW0ucnUvYXBpL0NoZWNrVm1BY2Nlc3MvMhUKClNLWU5FVF9TU0gSB2VuYWJsZWQ"
                          "SuxAKuBAKoAEaJgobaXNzX2hvb2tfc3RhcnQuZW5hYmxlX3BvcnRvEgdpc29sYXRlGh8KEmlzc19ob29rX3N0YXJ0Lm"
                          "5ldBIJaW5oZXJpdGVkGhwKEW1ldGEuZW5hYmxlX3BvcnRvEgdpc29sYXRlGjcKI2lzc19ob29rX3N0YXJ0LmNhcGFia"
                          "WxpdGllc19hbWJpZW50EhBORVRfQklORF9TRVJWSUNFGrYBCg9pc3NfaG9va19zdGF0dXMSogEKnwEiYWRhdGE6dGV4"
                          "dC9wbGFpbjtjaGFyc2V0PXV0Zi04O2Jhc2U2NCxMaTkyYldGblpXNTBMM1p0WVdkbGJuUXZjMk55YVhCMGN5OXBjM05"
                          "mYUc5dmExOXpkR0YwZFhNdWMyZz0KKGQ3ZThlNThjNmMxM2JkOWJmNjFmM2E3NGY3OGJiNWYxYWU2NDhkYTAaEBIGMG"
                          "QwaDBtCgZFTVBUWToavgEKD2lzc19ob29rX25vdGlmeRKqAQqnASJpZGF0YTp0ZXh0L3BsYWluO2NoYXJzZXQ9dXRmL"
                          "Tg7YmFzZTY0LExpOTJiV0ZuWlc1MEwzWnRZV2RsYm5RdmMyTnlhWEIwY3k5cGMzTmZhRzl2YTE5dWIzUnBabmt1YzJn"
                          "Z0lpUkFJZz09CihjODdjMTM5ODYyYzRiZmFiZmRjNjkzZTExNjgwYjBkMzNhOGNkYmUzGhASBjBkMGgwbQoGRU1QVFk"
                          "6GrABCg1pc3NfaG9va19zdG9wEp4BCpsBIl1kYXRhOnRleHQvcGxhaW47Y2hhcnNldD11dGYtODtiYXNlNjQsTGk5Mm"
                          "JXRm5aVzUwTDNadFlXZGxiblF2YzJOeWFYQjBjeTlwYzNOZmFHOXZhMTl6ZEc5d0xuTm8KKGE2NTI0NjdiZmE5YWYxZ"
                          "DE5OTI0ZTg3YjM0NDdiOWY2MzYzNjhhZDgaEBIGMGQwaDBtCgZFTVBUWToanQEKDmlzc19ob29rX3N0YXJ0EooBCocB"
                          "IklkYXRhOnRleHQvcGxhaW47Y2hhcnNldD11dGYtODtiYXNlNjQsTDNWemNpOXpZbWx1TDNGbGJYVmZiR0YxYm1Ob1p"
                          "YSXVjMmc9CigzMzMwN2Q3MmRkMjNiOTVlMTkzOWRlNzUzNTY4MGNjNTI1NmNmMjBjGhASBjBkMGgwbQoGRU1QVFk6Gn"
                          "sKBXZtY3RsEnIScCIycmJ0b3JyZW50Ojg0Yzg4MmM5NmQ3OTE2M2Q5ZDViZTMxYzY1YzJhNzU4YzVkZWZjMzQKKDVjO"
                          "WFhOWU1Yjg3NGIxZGY4ZDViZTg1ODFlMDc0NGFlMjBmMzI4MzEaEBIGMGQwaDBtCgZFTVBUWToafQoHdm1hZ2VudBJy"
                          "EnAiMnJidG9ycmVudDo0ODg5ZjU0NzVlNmVhZWRlNTY2ODE3MWY5NzRjZTI5OWZhODgyYmIyCigyYTc3YzI4NDMxMmZj"
                          "ZDgwZDFiYzFjMTI4NGU3YTk0NmQ1NTQ3OWI1GhASBjBkMGgwbQoGRU1QVFk6GtkDCgl2bS5jb25maWcSywMKyAMiiQN"
                          "kYXRhOnRleHQvcGxhaW47Y2hhcnNldD11dGYtODtiYXNlNjQsZXdvZ0lDQWdJblpqY0hVaU9pQXhMQW9nSUNBZ0ltMW"
                          "xiU0k2SUNJNU5qWXpOamMyTkRFMklpd0tJQ0FnSUNKa2FYTnJJam9nZXdvZ0lDQWdJQ0FnSUNKMGVYQmxJam9nSWxKQl"
                          "Z5SXNDaUFnSUNBZ0lDQWdJbkpsYzI5MWNtTmxJam9nZXdvZ0lDQWdJQ0FnSUNBZ0lDQWljbUpVYjNKeVpXNTBJam9nSW"
                          "5KaWRHOXljbVZ1ZERwa1lUbGlaV1F6WkdJMFpHWXlPVGM0TkdRMVptVTRaRE5tTVRaaU9XWTJaV1kyT0RFNVpHUmtJZ2"
                          "9nSUNBZ0lDQWdJSDBLSUNBZ0lIMHNDaUFnSUNBaWFXUWlPaUFpTVRreU5tWmtZVFF0TUdVM015MDBPR0k0TFRneVpUVX"
                          "RaV0ZtTnpka05EZzRNekUwSWl3S0lDQWdJQ0poZFhSdmNuVnVJam9nZEhKMVpRcDkKKDExODc4MTJjMGUxZmQ4MzE5Yz"
                          "gwZTdjY2QwNmU3NWViYmZjOWEwMzYaEBIGMGQwaDBtCgZFTVBUWToqBC9zc2QiJwoPaXNzX2hvb2tfc3RhdHVzEhQogO"
                          "DUieRbGODUAyCw6gEQAgjoBxKNASp2OgQvc3NkIjJyYnRvcnJlbnQ6MmEzMDc0ZGJmZmFhYjAxMzFiZmE1OWJlNTM5NT"
                          "NiN2U0MGFhMWY1NwooYTQwMzYwYzdlZGM5YzEzMmFjMDYwNmVjMTczYzc2NzliYjRmMTIxYRoQEgYwZDBoMG0KBkVNUF"
                          "RZOhoBLwiAgICABBCAgICAFDoEL3NzZBJhGhAvcWVtdS1wZXJzaXN0ZW50CICAgIBQEICAgIBQOgQvc3NkMjtpLWR5YW"
                          "Noa292LWFubm90YXRpb25zLTVhMzdjMmE1LTY4NzAtNDljOC05ODExLWQ2NzY2OTU3NzU0NxJdGggvc2FuZGJveAiAgI"
                          "CAkAMQgICAgJADOgYvcGxhY2UyO2ktZHlhY2hrb3YtYW5ub3RhdGlvbnMtMmQzMTRkM2UtMDVlNC00OGI3LThmMmItYj"
                          "ExNjMzMzJmZTZjEm0aFS9CZXJrYW5hdnQvc3VwZXJ2aXNvciIRCglyZWFkX29ubHkSBHRydWUiDwoHYmFja2VuZBIEY"
                          "mluZCIgCgdzdG9yYWdlEhUvQmVya2FuYXZ0L3N1cGVydmlzb3IIgICAgPy0GBCAgICA/LQYCkQSKAoDcXlwEiFpLWR5Y"
                          "WNoa292LWFubm90YXRpb25zLTE1NzMyMDc0OTEKGAoWaS1keWFjaGtvdi1hbm5vdGF0aW9ucxoaChJJTlNUQU5DRV9UQ"
                          "UdfQ1RZUEUSBHByb2QaHwoXeWFzbVVuaXN0YXRGYWxsYmFja1BvcnQSBDcyNTUaEQoJVVNFX05BVDY0EgR0cnVlGhwKE"
                          "klOU1RBTkNFX1RBR19JVFlQRRIGcWVtdXZtGjYKCEhPU1ROQU1FEippLWR5YWNoa292LWFubm90YXRpb25zLnNhcy55c"
                          "C1jLnlhbmRleC5uZXQaKgoQSU5TVEFOQ0VfVEFHX1BSShIWaS1keWFjaGtvdi1hbm5vdGF0aW9ucxogCgxTVE9SQUdFX"
                          "1BBVEgSEC9xZW11LXBlcnNpc3RlbnQabwoEdGFncxJnYV9nZW9fc2FzIGFfZGNfc2FzIGFfaXR5cGVfcWVtdXZtIGFfY"
                          "3R5cGVfcHJvZCBhX3Byal9pLWR5YWNoa292LWFubm90YXRpb25zIGFfbWV0YXByal91bmtub3duIGFfdGllcl95cBoMC"
                          "gRQT1JUEgQ3MjU1GiEKDFZNQUdFTlRfUEFUSBIRLi92bWFnZW50L3ZtYWdlbnQaFwoRSU5TVEFOQ0VfVEFHX1RJRVIS"
                          "AnlwGiAKBlBPRF9JRBIWaS1keWFjaGtvdi1hbm5vdGF0aW9ucxoqChBOQU5OWV9TRVJWSUNFX0lEEhZpLWR5YWNoa292"
                          "LWFubm90YXRpb25zGiUKHVZNQ1RMX0ZPUkNFX0RJUkVDVF9DT05ORUNUSU9OEgRUcnVlIgZBQ1RJVkU=",
            "portoProperties": [{
                "key": "cpu_guarantee",
                "value": "0.995c"
            }, {
                "key": "cpu_limit",
                "value": "0.995c"
            }, {
                "key": "memory_guarantee",
                "value": "10737418240"
            }, {
                "key": "memory_limit",
                "value": "10737418240"
            }, {
                "key": "anon_limit",
                "value": "0"
            }, {
                "key": "dirty_limit",
                "value": "0"
            }, {
                "key": "hostname",
                "value": "i-dyachkov-annotations.sas.yp-c.yandex.net"
            }, {
                "key": "net",
                "value": "L3 veth"
            }, {
                "key": "ip",
                "value": "veth 2a02:6b8:c1c:207:0:696:7f26:0;"
                         "veth 2a02:6b8:fc1d:207:0:696:66ca:0;"
                         "veth 2a02:6b8:c1c:207:0:43e9:67fe:0;"
                         "veth 2a02:6b8:fc1d:207:0:43e9:a179:0"
            }, {
                "key": "devices",
                "value": "/dev/kvm rw;/dev/net/tun rw"
            }, {
                "key": "sysctl",
                "value": "net.ipv6.conf.all.proxy_ndp:1;net.ipv6.conf.all.forwarding:1;net.ipv4.conf.all.forwarding:1"
            }],
            "targetState": "PTS_UNKNOWN",
            "ip6AddressRequests": [{
                "networkId": "_SEARCHSAND_",
                "vlanId": "backbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "AQR2bQ=="
                    }]
                },
                "enableDns": True,
                "dnsPrefix": "",
                "enableInternet": False,
                "virtualServiceIds": []
            }, {
                "networkId": "_SEARCHSAND_",
                "vlanId": "fastbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "AQR2bQ=="
                    }]
                },
                "enableDns": True,
                "dnsPrefix": "",
                "enableInternet": False,
                "virtualServiceIds": []
            }, {
                "networkId": "_VMAGENTNETS_",
                "vlanId": "backbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "ARJjb250YWluZXI="
                    }, {
                        "key": "unistat",
                        "value": "AQ5lbmFibGVk"
                    }]
                },
                "enableDns": False,
                "dnsPrefix": "",
                "enableInternet": False,
                "virtualServiceIds": []
            }, {
                "networkId": "_VMAGENTNETS_",
                "vlanId": "fastbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "ARJjb250YWluZXI="
                    }]
                },
                "enableDns": False,
                "dnsPrefix": "",
                "enableInternet": False,
                "virtualServiceIds": []
            }],
            "ip6AddressAllocations": [{
                "address": "2a02:6b8:c1c:207:0:696:7f26:0",
                "vlanId": "backbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "AQR2bQ=="
                    }]
                },
                "persistentFqdn": "i-dyachkov-annotations.sas.yp-c.yandex.net",
                "transientFqdn": "sas3-7179-1.i-dyachkov-annotations.sas.yp-c.yandex.net",
                "virtualServices": []
            }, {
                "address": "2a02:6b8:fc1d:207:0:696:66ca:0",
                "vlanId": "fastbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "AQR2bQ=="
                    }]
                },
                "persistentFqdn": "fb-i-dyachkov-annotations.sas.yp-c.yandex.net",
                "transientFqdn": "fb-sas3-7179-1.i-dyachkov-annotations.sas.yp-c.yandex.net",
                "virtualServices": []
            }, {
                "address": "2a02:6b8:c1c:207:0:43e9:67fe:0",
                "vlanId": "backbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "ARJjb250YWluZXI="
                    }, {
                        "key": "unistat",
                        "value": "AQ5lbmFibGVk"
                    }]
                },
                "persistentFqdn": "",
                "transientFqdn": "",
                "virtualServices": []
            }, {
                "address": "2a02:6b8:fc1d:207:0:43e9:a179:0",
                "vlanId": "fastbone",
                "labels": {
                    "attributes": [{
                        "key": "owner",
                        "value": "ARJjb250YWluZXI="
                    }]
                },
                "persistentFqdn": "",
                "transientFqdn": "",
                "virtualServices": []
            }],
            "ip6SubnetRequests": [],
            "ip6SubnetAllocations": [],
            "dns": {
                "persistentFqdn": "i-dyachkov-annotations.sas.yp-c.yandex.net",
                "transientFqdn": "sas3-7179-1.i-dyachkov-annotations.sas.yp-c.yandex.net"
            },
            "diskVolumeAllocations": [{
                "id": "i-dyachkov-annotations-e7939d6d-eaf8-4ebc-9b06-9b5f126d18b3",
                "labels": {
                    "attributes": [{
                        "key": "mount_path",
                        "value": "AQIv"
                    }, {
                        "key": "qyp_main_storage",
                        "value": "BA=="
                    }, {
                        "key": "root_fs_snapshot_quota",
                        "value": "AoCAgIAI"
                    }, {
                        "key": "volume_type",
                        "value": "AQ5yb290X2Zz"
                    }, {
                        "key": "work_dir_snapshot_quota",
                        "value": "AoCAgIAo"
                    }]
                },
                "capacity": "6442450944",
                "resourceId": "disk-ssd-sas3-7179-search-yandex-net",
                "volumeId": "9a087f17-38198e2e-6134ec27-2ca2c8e4",
                "device": "/ssd",
                "readBandwidthGuarantee": "0",
                "readBandwidthLimit": "0",
                "writeBandwidthGuarantee": "0",
                "writeBandwidthLimit": "0",
                "readOperationRateGuarantee": "0",
                "readOperationRateLimit": "0",
                "writeOperationRateGuarantee": "0",
                "writeOperationRateLimit": "0"
            }, {
                "id": "i-dyachkov-annotations-5a37c2a5-6870-49c8-9811-d67669577547",
                "labels": {
                    "attributes": [{
                        "key": "mount_path",
                        "value": "ASAvcWVtdS1wZXJzaXN0ZW50"
                    }, {
                        "key": "qyp_image_type",
                        "value": "AQZSQVc="
                    }, {
                        "key": "qyp_main_storage",
                        "value": "BQ=="
                    }, {
                        "key": "qyp_resource_url",
                        "value": "AWRyYnRvcnJlbnQ6ZGE5YmVkM2RiNGRmMjk3ODRkNWZlOGQzZjE2YjlmNmVmNjgxOWRkZA=="
                    }, {
                        "key": "qyp_vm_mount_path",
                        "value": "AQIv"
                    }, {
                        "key": "qyp_volume_name",
                        "value": "ASAvcWVtdS1wZXJzaXN0ZW50"
                    }, {
                        "key": "volume_type",
                        "value": "ARRwZXJzaXN0ZW50"
                    }]
                },
                "capacity": "21474836480",
                "resourceId": "disk-ssd-sas3-7179-search-yandex-net",
                "volumeId": "9a087f18-25f8f8df-17fbea49-fb3e3d37",
                "device": "/ssd",
                "readBandwidthGuarantee": "0",
                "readBandwidthLimit": "0",
                "writeBandwidthGuarantee": "0",
                "writeBandwidthLimit": "0",
                "readOperationRateGuarantee": "0",
                "readOperationRateLimit": "0",
                "writeOperationRateGuarantee": "0",
                "writeOperationRateLimit": "0"
            }, {
                "id": "i-dyachkov-annotations-2d314d3e-05e4-48b7-8f2b-b1163332fe6c",
                "labels": {
                    "attributes": [{
                        "key": "mount_path",
                        "value": "ARAvc2FuZGJveA=="
                    }, {
                        "key": "qyp_image_type",
                        "value": "AQZSQVc="
                    }, {
                        "key": "qyp_main_storage",
                        "value": "BA=="
                    }, {
                        "key": "qyp_resource_url",
                        "value": "AWRyYnRvcnJlbnQ6NmU3YmRjZTA2NDQ1ZGRmMTM2NDAxZmZiZDQwNzFlMGUzNjAwMzU3OQ=="
                    }, {
                        "key": "qyp_vm_mount_path",
                        "value": "AQA="
                    }, {
                        "key": "qyp_volume_name",
                        "value": "AQ5zYW5kYm94"
                    }, {
                        "key": "volume_type",
                        "value": "ARRwZXJzaXN0ZW50"
                    }]
                },
                "capacity": "107374182400",
                "resourceId": "disk-place-sas3-7179-search-yandex-net",
                "volumeId": "9a087f19-77cad992-7f715994-6eef4782",
                "device": "/place",
                "readBandwidthGuarantee": "0",
                "readBandwidthLimit": "0",
                "writeBandwidthGuarantee": "0",
                "writeBandwidthLimit": "0",
                "readOperationRateGuarantee": "0",
                "readOperationRateLimit": "0",
                "writeOperationRateGuarantee": "0",
                "writeOperationRateLimit": "0"
            }],
            "secrets": [],
            "podDynamicAttributes": {
                "labels": {
                    "attributes": [{
                        "key": "version",
                        "value": "AUhhYWExMjc1MC0zNjA0LTQxMjgtODc0ZC02ZTU4MWYyNjlmYjM="
                    }, {
                        "key": "qyp_vm_mark",
                        "value": "e30="
                    }, {
                        "key": "deploy_engine",
                        "value": "AQZRWVA="
                    }, {
                        "key": "vmagent_version",
                        "value": "AQgwLjI4"
                    }, {
                        "key": "qyp_vm_type",
                        "value": "AgA="
                    }, {
                        "key": "qyp_vm_autorun",
                        "value": "BQ=="
                    }]
                },
                "annotations": {
                    "attributes": [{
                        "key": "qyp_vm_spec",
                        "value": "AZwFCigKFmktZHlhY2hrb3YtYW5ub3RhdGlvbnMSDgoMCgppLWR5YWNoa292EqECEpg"
                                 "CCgxfU0VBUkNIU0FORF8SB2RlZmF1bHQaEhCAgICAKCDoByjoBzCAgICAKCpZChAvcWV"
                                 "tdS1wZXJzaXN0ZW50EICAgIBQGgNzc2QiMnJidG9ycmVudDpkYTliZWQzZGI0ZGYyOTc4N"
                                 "GQ1ZmU4ZDNmMTZiOWY2ZWY2ODE5ZGRkKgNSQVcyAS8qSQoHc2FuZGJveBCAgICAkAMaA2hkZ"
                                 "CIycmJ0b3JyZW50OjZlN2JkY2UwNjQ0NWRkZjEzNjQwMWZmYmQ0MDcxZTBlMzYwMDM1Nzk4AU"
                                 "JBCgd2bWFnZW50EjYSMnJidG9ycmVudDo0ODg5ZjU0NzVlNmVhZWRlNTY2ODE3MWY5NzRjZTI5"
                                 "OWZhODgyYmIyIAFQASIEMC4yOA=="
                    }, {
                        "key": "qyp_ssh_authorized_keys",
                        "value": "WwGsBnNzaC1yc2EgQUFBQUIzTnphQzF5YzJFQUFBQURBUUFCQUFBQkFRQzRaK0RGSmwrK1JvaWJ"
                                 "pS0NOUGhGVGVwV3pjQTFyaWFsZC85aStHREhkc29TSUtUUkZKMHpFS1FqNy9EOWVyWU1SN0ZIMWx"
                                 "ZOG90TnJMM3pwcjVERkZnaWFHaVRsb0IrcExDNTlDVU0xM0FzV0MrSW9taUpLeFVENjNadWxvd3h"
                                 "QTDN6OFlnZlJyVGxKM1NzbUFjaE1MT0hZL0F6Wkg4NFhuckM0eDhBU0p3R3psZ0IvQ1BsQ1l3R280"
                                 "VGRuMURnck5QTWRRTXorRUNqY3g2UVNLc1dWTnFSWFRTNW9SNFp4bjhDeTEyN29VMGJMaG0wRmxzK3"
                                 "NuT0NRR2RkNWc1aGYyNEtqNFVuaXFPRzdFOFdsVjRXYmUzSUw5d3RWeHV0Um5NRGJLWlU0R0U4NXd1Q"
                                 "VNVemoycGI0NmRmUXNudG1kVitVc2I2aHRxSHgzWVFmMWl0Zlo3IGktZHlhY2hrb3ZAaS1keWFjaGtvd"
                                 "i1vc3g7XQ=="
                    }]
                }
            },
            "resourceRequests": {
                "vcpuGuarantee": "1000",
                "vcpuLimit": "1000",
                "memoryGuarantee": "10737418240",
                "memoryLimit": "10737418240",
                "anonymousMemoryLimit": "0",
                "dirtyMemoryLimit": "0",
                "slot": "0"
            },
            "resourceCache": {
            },
            "hostInfra": {
            }
        }
        )

    server = StopableUnixWSGIServer.create(app, unix_socket=unix_socket_path)
    yield unix_socket_path
    server.shutdown()


def test_pod_agent_socket(pod_agent_socket):
    session = requests_unixsocket.Session()
    response = session.get(
        'http+unix://{}/pod_spec'.format(
            urllib.urlencode({'remove_this': pod_agent_socket}).replace('remove_this=', '')))
    pod_spec = json.loads(response.content)

    assert isinstance(pod_spec, dict)

    assert 'issPayload' in pod_spec
    assert 'portoProperties' in pod_spec

    iss_payload_string = base64.b64decode(pod_spec['issPayload'])

    iss_proto = cluster_api_pb2.HostConfiguration()
    iss_proto.ParseFromString(iss_payload_string)
    assert iss_proto


def test_pod_spec(pod_agent_socket):
    pod_spec = PodSpec.build(pod_agent_socket)

    assert pod_spec.vm
    assert pod_spec.ssh_authorized_keys
    assert pod_spec.iss_payload
    assert pod_spec.vm_ip
    assert pod_spec.vm_aux_ip
    assert pod_spec.pod_labels
    assert pod_spec.properties and isinstance(pod_spec.properties, dict)
    assert isinstance(pod_spec.pod_annotations, dict) and pod_spec.pod_annotations
    assert pod_spec.vmagent_port and isinstance(pod_spec.vmagent_port, int)
    assert isinstance(pod_spec.tags, str) and pod_spec.tags
    assert pod_spec.host_dc
    assert isinstance(pod_spec.vm, vmset_pb2.VM)


def test_build_vmagent_context_from_pod_spec(pod_agent_socket):
    os.environ['PORTO_HOST'] = 'test_porto_host_env'
    vmagent_context = VmagentContext.build_from_pod_spec(pod_agent_socket=pod_agent_socket)
    assert vmagent_context.VM
    assert vmagent_context.VM_CONFIG
    assert vmagent_context.VM_ID
    assert vmagent_context.VM_IP
    assert vmagent_context.VM_AUX_IP
    assert vmagent_context.MAIN_VOLUME
    assert vmagent_context.EXTRA_VOLUMES
    assert vmagent_context.LOGS_FOLDER_PATH
    assert vmagent_context.EXTRAS_FOLDER_PATH
    assert vmagent_context.SERIAL_LOG_FILE_PATH
    assert vmagent_context.VNC_PORT
    assert vmagent_context.SERIAL_PORT
    assert vmagent_context.MON_PORT
    assert vmagent_context.VMAGENT_PORT
    assert vmagent_context.REBUILD_QEMU_LAUNCHER
    assert vmagent_context.CLUSTER
    assert vmagent_context.VM_HOSTNAME
    assert vmagent_context.LLADDR
    assert vmagent_context.VMLLADDR
    assert vmagent_context.TAP_DEV
    assert vmagent_context.TAP_LL
    assert vmagent_context.WORKDIR
    assert vmagent_context.QEMU_LAUNCHER_FILE_PATH
    assert vmagent_context.SSH_AUTHORIZED_KEYS
    assert vmagent_context.NODE_HOSTNAME == 'test_porto_host_env'
    assert vmagent_context.CLOUD_INIT_CONFIGS_FOLDER_PATH
    assert not vmagent_context.WINDOWS_READY
    assert isinstance(vmagent_context.VM, vmset_pb2.VM)
    assert vmagent_context.MAIN_VOLUME.resource_url
