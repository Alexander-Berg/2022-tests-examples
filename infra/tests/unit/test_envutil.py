from __future__ import unicode_literals

import mock
import pytest

from instancectl.lib import envutil
from instancectl import constants
from yp_proto.yp.client.hq.proto import types_pb2


def test_get_yp_hq_spec():
    pod_spec = {
        "issPayload": "CqAeMhMKB0hCRl9OQVQSCGRpc2FibGVkMj0KEU5BTk5ZX1NOQVBTSE9UX0lEEihlYjdkZGM0Y2M0MDIyZjY4MjUwM"
                      "2U3YjE0MWRjMDBhMDVmNjAyMjRiMlgKGm5hbm55X2NvbnRhaW5lcl9hY2Nlc3NfdXJsEjpodHRwOi8vbmFubnkueW"
                      "FuZGV4LXRlYW0ucnUvYXBpL3JlcG8vQ2hlY2tDb250YWluZXJBY2Nlc3MvMhUKClNLWU5FVF9TU0gSB2VuYWJsZWQS"
                      "ihQKhxQKvgcaHwoSaXNzX2hvb2tfc3RhcnQubmV0Eglpbmhlcml0ZWQaJQocaXNzX2hvb2tfc3RhdHVzLmVuYWJsZV9"
                      "wb3J0bxIFZmFsc2UaHAoRbWV0YS5lbmFibGVfcG9ydG8SB2lzb2xhdGUaKAoaaXNzX2hvb2tfaW5zdGFsbC5uZXRfb"
                      "GltaXQSCmRlZmF1bHQ6IDAaKgocaXNzX2hvb2tfdW5pbnN0YWxsLm5ldF9saW1pdBIKZGVmYXVsdDogMBolChxpc3N"
                      "faG9va19ub3RpZnkuZW5hYmxlX3BvcnRvEgVmYWxzZRooCh9pc3NfaG9va191bmluc3RhbGwub29tX2lzX2ZhdGFsE"
                      "gVmYWxzZRpKCgttZXRhLnVsaW1pdBI7bWVtbG9jazogNTQ5NzU1ODEzODg4IDU0OTc1NTgxMzg4ODsgbm9maWxlOiA"
                      "xMDI0MDAgMTAwMDAwMDsaJgoYaXNzX2hvb2tfc3RhcnQubmV0X2xpbWl0EgpkZWZhdWx0OiAwGikKIGlzc19ob29rX3"
                      "Jlb3BlbmxvZ3MuZW5hYmxlX3BvcnRvEgVmYWxzZRpFCgZ1bGltaXQSO21lbWxvY2s6IDU0OTc1NTgxMzg4OCA1NDk3N"
                      "TU4MTM4ODg7IG5vZmlsZTogMTAyNDAwIDEwMDAwMDA7GiIKFWlzc19ob29rX3ZhbGlkYXRlLm5ldBIJaW5oZXJpdGVk"
                      "GiYKHWlzc19ob29rX2luc3RhbGwub29tX2lzX2ZhdGFsEgVmYWxzZRojChppc3NfaG9va19zdG9wLmVuYWJsZV9wb3J"
                      "0bxIFZmFsc2UaHgoRaXNzX2hvb2tfc3RvcC5uZXQSCWluaGVyaXRlZBohChRpc3NfaG9va19pbnN0YWxsLm5ldBIJaW"
                      "5oZXJpdGVkGiQKG2lzc19ob29rX3N0YXJ0Lm9vbV9pc19mYXRhbBIFZmFsc2UaNwojaXNzX2hvb2tfc3RhcnQuY2FwYW"
                      "JpbGl0aWVzX2FtYmllbnQSEE5FVF9CSU5EX1NFUlZJQ0UaIAoTaXNzX2hvb2tfc3RhdHVzLm5ldBIJaW5oZXJpdGVkGi"
                      "AKE2lzc19ob29rX25vdGlmeS5uZXQSCWluaGVyaXRlZBonCh5pc3NfaG9va192YWxpZGF0ZS5lbmFibGVfcG9ydG8SBW"
                      "ZhbHNlGiQKF2lzc19ob29rX3Jlb3BlbmxvZ3MubmV0Eglpbmhlcml0ZWQaIwoWaXNzX2hvb2tfdW5pbnN0YWxsLm5ldB"
                      "IJaW5oZXJpdGVkGqsBChNpc3NfaG9va19yZW9wZW5sb2dzEpMBCpABQgAiMnJidG9ycmVudDpjOTliYTBkYjAwN2QxMz"
                      "ZiZjUxZTA1ZGYxMDdmYTA0ZmUxMTI1YWFhCihlNDMzNjRmNWFhMzE4MmIzZmU4ZDUwNjQ3OTYzMTUzNzM4ZmIxMGZiGi"
                      "4SBjBkMGgwbQokTUQ1OjI5ZTUxZTc4NDIwNzAxOTgzNTdiMzU3YjkyZDI0NzdlGqoBChJpc3NfaG9va191bmluc3RhbG"
                      "wSkwEKkAFCACIycmJ0b3JyZW50OmU4ZmUwNTRjNjc3YWQ5NTIxOTc3MzkwMWYwZTc0NjM2MzA5MDg4NWEKKGU1NTYwNT"
                      "VjMzA2NjA5Yzk4ZDNhNTAyYjYyZWY5MGRkODgzOTZjZmEaLhIGMGQwaDBtCiRNRDU6MDIyMWM1YTNmMTMxMzI4NmFlNz"
                      "JlNTc0OWUxMDU0MTMapQEKDWlzc19ob29rX3N0b3ASkwEKkAFCACIycmJ0b3JyZW50OmUyNWY0ZGJjYjY4YTkzMDRlNG"
                      "M0ZWM1Mzc3ZmRiOGZhYWE5NTI5OTkKKDE2NTQ0MzQzYzIyZjQ4MzQyNGMxZTU1YjJmMjNjMWEyOTgxMjliMzAaLhIGMG"
                      "QwaDBtCiRNRDU6MTk1Mzg1ZmQzOGYwNWZjNDkyMjljZTMwN2YzOThjMTQapgEKDmlzc19ob29rX3N0YXJ0EpMBCpABQg"
                      "AiMnJidG9ycmVudDo2ZDY5NGQ2NGRiMjYwOTg2OGEyNDdiNWMyYTYyYzUxOGE2MmMzYzM4Cig1NzczNDk2NmFlZGY0MG"
                      "FmMThmZDQxOGM1OWRlMTA0YmM2MWRjYmY4Gi4SBjBkMGgwbQokTUQ1OjAwNThlY2UzNzE1NTViOWFlMDlmNjA0NWYyNT"
                      "JlNGUzGqgBChBpc3NfaG9va19pbnN0YWxsEpMBCpABQgAiMnJidG9ycmVudDpmNjhkNDFiMGFiOGZiNmFhZjVjYzhlM2"
                      "FjNWIzZjQ4ZjZjZTUwZjdmCig4MWFhOGEyODMwYWUyNzQ3Njg3YTAxN2JkZTIzYjE0MDAwZDUxMzE2Gi4SBjBkMGgwbQ"
                      "okTUQ1OmE5MWZlMzFlMDU1YTMzNzdiZWQzYTUzNTQxNWJmYzhjGoMBCgtpbnN0YW5jZWN0bBJ0CnJCACIycmJ0b3JyZW"
                      "50OjBkNjg1ZGNmZWQ5ODgyMWMyOTJiOTBjOTYyYThhYWI1YWVlYmNiZWQKKGNiMjc1NTFmMzMzNjBkOTEwMGZhNTE5OT"
                      "Y3ODZlYmNlYWQwNjAyZmQaEBIGMGQwaDBtCgZFTVBUWToapwEKD2lzc19ob29rX3N0YXR1cxKTAQqQAUIAIjJyYnRvcn"
                      "JlbnQ6NWJlNmE2OGY2YWM1MWYzMjQ1MzFlNDhkZmNjODEzZWU4MWJmMjIyYwooNDgzNmE4NWVjMDU0MGU1ODQxNjA1NTA"
                      "zNDIwMzJlMTcyYThhMTZhZRouEgYwZDBoMG0KJE1ENTplYThmYmY5YzA2ZWUyOTlkYzI5M2VkNzhjYjU4NzcwYhqnAQoP"
                      "aXNzX2hvb2tfbm90aWZ5EpMBCpABQgAiMnJidG9ycmVudDo4MTQ4NjBhODM3ODllNTUxMjVmNmQ2MjA0YWRlZDU2Zjk0M"
                      "mQwNmYzCig5ODk0YWMwYTUxNzM5N2U3OWIyNzAyNWI0Nzk2YTgyZTViNTkwMzQ3Gi4SBjBkMGgwbQokTUQ1OmJhMGVkNz"
                      "EzODYyMmYzYThlMDc3YjdhNzg3NjE5YWNlKgYvcGxhY2UiJAoPaXNzX2hvb2tfc3RhdHVzEhEowO5tGODUAyCw6gEQAgj"
                      "oBxKkASqKAToGL3BsYWNlQgAiMnJidG9ycmVudDo0OTk2OWJiZGJjNTgxMzQyMjdhOGUzMjc2ZTkyZDg0M2YwZjkwNGIy"
                      "CjhsYXllcl9yYnRvcnJlbnRfNDk5NjliYmRiYzU4MTM0MjI3YThlMzI3NmU5MmQ4NDNmMGY5MDRiMhoQEgYwZDBoMG0KB"
                      "kVNUFRZOhoBLwiAgICABBCAgICABDoGL3BsYWNlEj4aBS9sb2dzCICAgIAUEICAgIAUOgYvcGxhY2UyIXdmcGJuY3pvbT"
                      "djeDR1cmIteHdjbmRsbjQ3a2U2Z3BuNQpsElYKImhxX3NwZWNfc3RvcmFnZV9lbmFibGVkX2lfZHlhY2hrb3YSMGhxX3N"
                      "wZWNfc3RvcmFnZV9lbmFibGVkX2lfZHlhY2hrb3YtMTYxMTMyNjI3MzMyNwoSChB3ZnBibmN6b203Y3g0dXJiOrsCGjwa"
                      "Ci9iaW4vc2xlZXAaCDEwMDAwMDA3MgQKABgKCgR0ZXN0IggQBSA8GAUoAkoCCgAqCBg8EAEgAigUQgBaJHdmcGJuY3pvb"
                      "TdjeDR1cmIuaXZhLnlwLWMueWFuZGV4Lm5ldAowaHFfc3BlY19zdG9yYWdlX2VuYWJsZWRfaV9keWFjaGtvdi0xNjExMz"
                      "I2MjczMzI3KglhX2dlb19tc2sqCGFfZGNfaXZhKg9hX2l0eXBlX3Vua25vd24qD2FfY3R5cGVfdW5rbm93biodYV9wcmp"
                      "fZGVmYXVsdC1jb250ZW50LXNlcnZpY2UqEWFfbWV0YXByal91bmtub3duKgthX3RpZXJfbm9uZSoLdXNlX2hxX3NwZWMq"
                      "EGVuYWJsZV9ocV9yZXBvcnQqDmVuYWJsZV9ocV9wb2xsGh0KEklOU1RBTkNFX1RBR19DVFlQRRIHdW5rbm93bhodChd5Y"
                      "XNtVW5pc3RhdEZhbGxiYWNrUG9ydBICODAaHQoSSU5TVEFOQ0VfVEFHX0lUWVBFEgd1bmtub3duGi4KE0JBQ0tCT05FX0"
                      "lQX0FERFJFU1MSFyUlQkFDS0JPTkVfSVBfQUREUkVTUyUlGjAKCEhPU1ROQU1FEiR3ZnBibmN6b203Y3g0dXJiLml2YS5"
                      "5cC1jLnlhbmRleC5uZXQaKwoQSU5TVEFOQ0VfVEFHX1BSShIXZGVmYXVsdC1jb250ZW50LXNlcnZpY2UaHgoYeWFzbUlu"
                      "c3RhbmNlRmFsbGJhY2tQb3J0EgI4MBqjAQoEdGFncxKaAWFfZ2VvX21zayBhX2RjX2l2YSBhX2l0eXBlX3Vua25vd24gY"
                      "V9jdHlwZV91bmtub3duIGFfcHJqX2RlZmF1bHQtY29udGVudC1zZXJ2aWNlIGFfbWV0YXByal91bmtub3duIGFfdGllcl"
                      "9ub25lIHVzZV9ocV9zcGVjIGVuYWJsZV9ocV9yZXBvcnQgZW5hYmxlX2hxX3BvbGwaWQoOSFFfSU5TVEFOQ0VfSUQSR3d"
                      "mcGJuY3pvbTdjeDR1cmIuaXZhLnlwLWMueWFuZGV4Lm5ldEBocV9zcGVjX3N0b3JhZ2VfZW5hYmxlZF9pX2R5YWNoa292"
                      "GjkKFUhRX0lOU1RBTkNFX1NQRUNfSEFTSBIgODU5ZDIwYmFhMmU1YzA2YjlkMzhmZWJkMjJmYjQxOTAaGAoNREVQTE9ZX"
                      "0VOR0lORRIHWVBfTElURRo2ChBOQU5OWV9TRVJWSUNFX0lEEiJocV9zcGVjX3N0b3JhZ2VfZW5hYmxlZF9pX2R5YWNoa29"
                      "2IgZBQ1RJVkU=",
    }
    conf_id = "hq_spec_storage_enabled_i_dyachkov-1611326273327"
    hq_spec = envutil.get_yp_hq_spec(pod_spec, conf_id, None)
    assert isinstance(hq_spec, types_pb2.InstanceRevision)
    assert hq_spec.id == conf_id

    wrong_conf_id = "wrong_conf_id"
    hq_spec = envutil.get_yp_hq_spec(pod_spec, wrong_conf_id, None)
    assert hq_spec is None

    wrong_pod_spec = {}
    hq_spec = envutil.get_yp_hq_spec(wrong_pod_spec, wrong_conf_id, None)
    assert hq_spec is None


def test_make_yp_lite_env_vars(monkeypatch):
    pod_spec = {
        "portoProperties": [{
            "key": "cpu_guarantee",
            "value": "1.144c"
        }, {
            "key": "cpu_limit",
            "value": "1.144c"
        }, {
            "key": "memory_guarantee",
            "value": "536870912"
        }, {
            "key": "memory_limit",
            "value": "536870912"
        }, {
            "key": "anon_limit",
            "value": "0"
        }, {
            "key": "hostname",
            "value": "yp-test-alonger-1.sas-test.yp-test.yandex.net"
        }, {
            "key": "net",
            "value": "L3 veth"
        }, {
            "key": "ip",
            "value": "veth 2a02:6b8:fc00:1226:0:43a6:1988:0;veth 2a02:6b8:c08:12a6:0:43a6:1da:0"
        }],
        "ip6AddressAllocations": [{
            "address": "2a02:6b8:fc00:1226:0:43a6:1988:0",
            "vlanId": "fastbone",
            "persistentFqdn": "fb-yp-test-alonger-1.sas-test.yp-test.yandex.net",
            "transientFqdn": "fb-sas2-6802-2.yp-test-alonger-1.sas-test.yp-test.yandex.net"
        }, {
            "address": "2a02:6b8:c08:12a6:0:43a6:1da:0",
            "vlanId": "backbone",
            "persistentFqdn": "yp-test-alonger-1.sas-test.yp-test.yandex.net",
            "transientFqdn": "sas2-6802-2.yp-test-alonger-1.sas-test.yp-test.yandex.net"
        }],
        "podDynamicAttributes": {
            "labels": {
                "attributes": [{
                    "key": "test_label",
                    "value": "ARR0ZXN0X3ZhbHVl"
                }, {
                    "key": "nanny_version",
                    "value": "AUg5OGRjMWJhYy04NDdiLTRjYzItYTg5YS02NjVkNGZmZDE1MGM="
                }, {
                    "key": "deploy_engine",
                    "value": "AQ5ZUF9MSVRF"
                }, {
                    "key": "nanny_service_id",
                    "value": "ARxkZXYteXAtc2VydmljZQ=="
                }, {
                    "key": "non_string_label",
                    "value": "ZmFrZWxhYmVs"
                }]
            }
        },
    }

    # Case 1: OK
    assert envutil._make_yp_lite_env_vars(pod_spec) == {
        'MEM_GUARANTEE': '536870912',
        'MEM_LIMIT': '536870912',
        'CPU_LIMIT': '1.144c',
        'CPU_GUARANTEE': '1.144c',
        'BACKBONE_IP_ADDRESS': '2a02:6b8:c08:12a6:0:43a6:1da:0',
        'LABELS_deploy_engine': 'YP_LITE',
        'LABELS_nanny_service_id': 'dev-yp-service',
        'LABELS_nanny_version': '98dc1bac-847b-4cc2-a89a-665d4ffd150c',
        'LABELS_test_label': 'test_value',
    }

    # Case 2: no ip addresses
    pod_spec['ip6AddressAllocations'][1]['vlanId'] = 'bad'
    with pytest.raises(envutil.InstanceCtlEnvironmentError):
        envutil._make_yp_lite_env_vars(pod_spec)


def test_get_pod_spec_from_unixsocket(monkeypatch):
    o = object()
    resp_mock = mock.Mock()
    resp_mock.json.return_value = o
    m = mock.Mock()
    m.side_effect = [Exception, resp_mock]
    monkeypatch.setattr('requests.sessions.Session.get', m)
    assert envutil._get_pod_spec_from_unixsocket() is o
    assert m.call_count == 2


def test_get_script_restart_policy(monkeypatch):
    expected_prepare = {
        "backoff": constants.DEFAULT_PREPARE_SCRIPT_BACKOFF,
        "max_jitter": constants.DEFAULT_PREPARE_SCRIPT_MAX_JITTER,
        "max_delay": constants.DEFAULT_PREPARE_SCRIPT_MAX_DELAY,
        "delay": constants.DEFAULT_PREPARE_SCRIPT_MIN_DELAY,
        "max_tries": constants.DEFAULT_PREPARE_SCRIPT_MAX_TRIES,
    }
    expected_install = {
        "backoff": constants.DEFAULT_INSTALL_SCRIPT_BACKOFF,
        "max_jitter": constants.DEFAULT_INSTALL_SCRIPT_MAX_JITTER,
        "max_delay": constants.DEFAULT_INSTALL_SCRIPT_MAX_DELAY,
        "delay": constants.DEFAULT_INSTALL_SCRIPT_MIN_DELAY,
        "max_tries": constants.DEFAULT_INSTALL_SCRIPT_MAX_TRIES,
    }
    monkeypatch.setenv('HOSTNAME', 'localhost')
    monkeypatch.setenv('BSCONFIG_IPORT', '1543')
    monkeypatch.setenv('BSCONFIG_IDIR', '.')
    monkeypatch.setenv('BSCONFIG_INAME', 'localhost:1543')
    env = envutil.make_instance_ctl_env('fake://url')
    assert env.prepare_script_restart_policy == expected_prepare
    assert env.install_script_restart_policy == expected_install

    monkeypatch.setenv('PREPARE_SCRIPT_BACKOFF', '123')
    monkeypatch.setenv('INSTALL_SCRIPT_MAX_JITTER', '321')
    env = envutil.make_instance_ctl_env('fake://url')
    expected_prepare["backoff"] = 123
    expected_install["max_jitter"] = 321
    assert env.prepare_script_restart_policy == expected_prepare
    assert env.install_script_restart_policy == expected_install
