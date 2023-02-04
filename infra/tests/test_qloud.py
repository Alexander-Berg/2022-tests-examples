from infra.deploy.tools.yd_migrate.lib.migrator_qloud import QloudMigrator

from google.protobuf import json_format


def dump_result(result):
    return {
        'stage': json_format.MessageToJson(result[0]),
        'create_namespace_request': json_format.MessageToJson(result[1]),
        'awacs_l7_macro': json_format.MessageToJson(result[2]),
        'awacs_domains': [json_format.MessageToJson(v) for v in result[3]],
        'awacs_upstreams': [json_format.MessageToJson(v) for v in result[4]],
        'awacs_backends': {k : json_format.MessageToJson(v) for k, v in result[5].iteritems()},
        'awacs_endpoint_sets': {k : json_format.MessageToJson(v) for k, v in result[6].iteritems()},
    }


def test_migrate_environment_simplehttpserver():  # dump from samogon.yanddmitest0.simplehttpserver
    result = QloudMigrator({
        "abcId": 4450,
        "dump": {
            "comment": "",
            "components": [
                {
                    "activateRecipe": {
                        "doneThreshold": "90%",
                        "recipe": "INTERNAL",
                        "updateLimit": "20%",
                        "updatePeriod": "20s",
                        "updateWindow": "100%"
                    },
                    "componentFeatures": [],
                    "componentName": "backend",
                    "componentType": "standard",
                    "embedJugglerClient": True,
                    "environmentVariables": {
                        "CONFIG": "2"
                    },
                    "instanceGroups": [
                        {
                            "backup": False,
                            "location": "MANTSALA",
                            "units": 1,
                            "weight": 1
                        },
                        {
                            "backup": False,
                            "location": "SASOVO",
                            "units": 1,
                            "weight": 1
                        }
                    ],
                    "jugglerBundleResources": [
                        {
                            "dynamic": False,
                            "extract": True,
                            "id": 1144841593,
                            "localName": "juggler-bundle-1144841593",
                            "symlink": "/juggler/build-1144841593"
                        }
                    ],
                    "l3Config": {
                        "vss": []
                    },
                    "overlays": [],
                    "prepareRecipe": {
                        "doneThreshold": "90%",
                        "recipe": "INTERNAL",
                        "updateLimit": "100%",
                        "updatePeriod": "20s",
                        "updateWindow": "100%"
                    },
                    "properties": {
                        "allocationFailThreshold": "0",
                        "allocationStrategy": "dynamic",
                        "allowedCpuNames": "",
                        "componentEnvironment": "CONFIG=2",
                        "deployPolicy": "InPlace",
                        "diskSerial": "0",
                        "diskSize": "5",
                        "dnsCache": "true",
                        "dnsNat64": "false",
                        "ediskSerial": "0",
                        "ediskSize": "0",
                        "failTimeout": "60",
                        "fastboneRequired": "false",
                        "hardwareSegment": "common",
                        "hash": "sha256:f90b7883851eacfad0031dbf478aa02edc099252bb3160812571eea8385378c9",
                        "healthCheckFall": "5",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "2",
                        "healthCheckTimeout": "2000",
                        "healthCheckType": "http",
                        "httpCheckOn": "true",
                        "ioLimit": "0",
                        "isolationGroup": "root",
                        "isolationUser": "root",
                        "java": "false",
                        "listenBacklogSize": "511",
                        "maxFails": "3",
                        "maxInstancesPerHost": "999",
                        "maxWorkerConnections": "16384",
                        "minPrepared": "50",
                        "multiAccept": "false",
                        "network": "SEARCHSAND",
                        "notResolveServerName": "false",
                        "preAuthenticate": "false",
                        "profiles": "production",
                        "qloudCoreDumpDirectory": "/coredumps_qloud",
                        "qloudCoreDumpFileSizeGb": "0",
                        "qloudInitPolicy": "stable",
                        "qloudInitVersion": "639",
                        "qloudMaxCoreDumpedStopsRespawnDelay": "0s",
                        "qloudMaxCoreDumpsOnDisk": "0",
                        "repository": "registry.yandex.net/samogon/simple_http_server:latest",
                        "size": "2;0.5;16;1",
                        "sslStapling": "false",
                        "statusChecksCorrected": "true",
                        "stderr": "line",
                        "stdout": "line",
                        "storage": "",
                        "tmpfsDiskSerial": "0",
                        "tmpfsDiskSize": "1",
                        "unistat": "false",
                        "unistatPath": "/unistat",
                        "unistatPort": "80",
                        "units": "0",
                        "upstreamPort": "80",
                        "useDockerUserGroup": "false",
                        "useHealthCheck": "false",
                        "useHttps": "false",
                        "usedVolumes": ""
                    },
                    "sandboxResources": [
                        {
                            "dynamic": False,
                            "extract": False,
                            "id": 1085555308,
                            "localName": "resource.txt",
                            "symlink": "/sandbox_resource/resource.txt"
                        }
                    ],
                    "secrets": [
                        {
                            "objectId": "secret.yanddmi-test",
                            "target": "YANDDMI_TEST_SECRET",
                            "used": True
                        },
                        {
                            "objectId": "secret.yanddmi-test-bin-file",
                            "target": "/secret/override-yanddmi-test-bin-file",
                            "used": True
                        }
                    ],
                    "statusHookChecks": [
                        {
                            "path": "/ping",
                            "port": 1,
                            "timeout": 1000,
                            "type": "http"
                        },
                        {
                            "path": "/",
                            "port": 80,
                            "timeout": 1000,
                            "type": "http"
                        }
                    ],
                    "tvmConfig": {
                        "blackBoxType": "PROD",
                        "clients": [
                            {
                                "destinations": [
                                    {
                                        "name": "to",
                                        "tvmId": 2015014
                                    }
                                ],
                                "name": "from",
                                "secretName": "yanddmi-tvm-test"
                            }
                        ]
                    },
                    "upstreamComponents": []
                },
                {
                    "componentFeatures": [],
                    "componentName": "backend-proxy",
                    "componentType": "component-proxy",
                    "embedJugglerClient": False,
                    "environmentVariables": {},
                    "instanceGroups": [],
                    "jugglerBundleResources": [],
                    "overlays": [],
                    "properties": {
                        "failTimeout": "60",
                        "healthCheckFall": "5",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "2",
                        "healthCheckTimeout": "2000",
                        "healthCheckType": "http",
                        "host": "simplehttpserver.n.yandex.ru",
                        "maxFails": "3",
                        "notResolveServerName": "false",
                        "preAuthenticate": "false",
                        "proxyRedirect": "http://simplehttpserver.n.yandex.ru/api /api",
                        "useHealthCheck": "false",
                        "useHttps": "false"
                    },
                    "sandboxResources": [],
                    "secrets": [],
                    "statusHookChecks": [],
                    "upstreamComponents": [
                        {
                            "componentId": "samogon.yanddmitest2.bootstrapyanddmitest2.small",
                            "weight": 1
                        },
                        {
                            "componentId": "samogon.yanddmitest0.yanddmi-test.balancer-l7",
                            "weight": 2
                        }
                    ]
                },
                {
                    "componentFeatures": [],
                    "componentName": "my-proxy",
                    "componentType": "proxy",
                    "embedJugglerClient": False,
                    "environmentVariables": {},
                    "instanceGroups": [],
                    "jugglerBundleResources": [],
                    "overlays": [],
                    "properties": {
                        "failTimeout": "60",
                        "healthCheckFall": "5",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "2",
                        "healthCheckTimeout": "2000",
                        "healthCheckType": "http",
                        "host": "simplehttpserver.n.yandex.ru",
                        "maxFails": "3",
                        "notResolveServerName": "false",
                        "preAuthenticate": "false",
                        "proxyRedirect": "http://simplehttpserver.n.yandex.ru/api2 /api",
                        "servers": "awf7gbvo7nejossm.man.yp-c.yandex.net",
                        "useHealthCheck": "false",
                        "useHttps": "false"
                    },
                    "sandboxResources": [],
                    "secrets": [],
                    "statusHookChecks": [],
                    "upstreamComponents": []
                }
            ],
            "engine": "platform",
            "objectId": "samogon.yanddmitest0.simplehttpserver",
            "routeSettings": [
                {
                    "componentName": "backend",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": True,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "5",
                    "proxyRequestBuffering": True,
                    "proxyWriteTimeout": "5",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                },
                {
                    "componentName": "backend-proxy",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/api",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": False,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "5",
                    "proxyRequestBuffering": False,
                    "proxyWriteTimeout": "5",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/api",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                },
                {
                    "componentName": "my-proxy",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/api2",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": False,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "5",
                    "proxyRequestBuffering": False,
                    "proxyWriteTimeout": "5",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/api",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                }
            ],
            "userEnvironment": "",
            "userEnvironmentMap": {}
        },
        "domains": [
            {
                "CA": "InternalCA",
                "certificateIssuer": "DC=ru,DC=yandex,DC=ld,CN=YandexInternalCA",
                "certificateMd5": "EB:9C:D4:89:C5:3A:80:93:D9:3F:2F:AB:9B:5E:D3:44",
                "certificateSerialNumber": "7F:00:0B:1D:9D:E4:DB:9C:F1:CE:E2:5E:3A:00:02:00:0B:1D:9D",
                "certificateSha1": "54:AE:65:B1:5D:51:75:06:98:36:22:C8:69:10:C7:3B:E4:47:75:0E",
                "certificateSha256": "30:FB:3B:C4:75:1E:A2:5A:33:96:02:E3:9A:56:DB:21:1A:9F:E2:65:08:2D:FE:2A:08:EC:A7:AF:5B:6A:9F:5D",
                "certificateSource": "CERTIFICATOR",
                "certificateValidFor": "yanddmi.common.yandex.net",
                "certificateValidNotAfter": "2022-01-08T18:13:34.000+03:00",
                "certificateValidNotBefore": "2020-01-09T18:13:34.000+03:00",
                "certificates": {
                    "CERTIFICATOR": {
                        "certificateIssuer": "DC=ru,DC=yandex,DC=ld,CN=YandexInternalCA",
                        "certificateMd5": "EB:9C:D4:89:C5:3A:80:93:D9:3F:2F:AB:9B:5E:D3:44",
                        "certificateSerialNumber": "7F:00:0B:1D:9D:E4:DB:9C:F1:CE:E2:5E:3A:00:02:00:0B:1D:9D",
                        "certificateSha1": "54:AE:65:B1:5D:51:75:06:98:36:22:C8:69:10:C7:3B:E4:47:75:0E",
                        "certificateSha256": "30:FB:3B:C4:75:1E:A2:5A:33:96:02:E3:9A:56:DB:21:1A:9F:E2:65:08:2D:FE:2A:08:EC:A7:AF:5B:6A:9F:5D",
                        "certificateValidFor": "yanddmi.common.yandex.net",
                        "certificateValidNotAfter": "2022-01-08T18:13:34.000+03:00",
                        "certificateValidNotBefore": "2020-01-09T18:13:34.000+03:00"
                    },
                    "NONE": {},
                    "SECRET": {},
                    "ZONE": {}
                },
                "clientCertificateRequired": False,
                "domainName": "yanddmi.common.yandex.net",
                "domainStatus": "ACTIVE",
                "errorPage": False,
                "errorPageUrl": "http://any.yandex.ru",
                "httpAllowed": False,
                "httpOnly": False,
                "moveTargets": [
                    "samogon.yanddmitest0.simplehttpserver"
                ],
                "published": True,
                "requestClientCertificate": False,
                "requester": "yanddmi",
                "statuses": {
                    "CERTIFICATOR": "ACTIVE",
                    "NONE": "ACTIVE",
                    "SECRET": "ACTIVE",
                    "ZONE": "ACTIVE"
                },
                "type": "yanddmi-test",
                "url": "https://yanddmi.common.yandex.net",
                "warning": "Wrong dns (IPV6), should be 2a02:6b8:0:3400:0:410:0:5. You can set it on https://dns.tt.yandex-team.ru/request\n"
            }
        ],
        "stable": {
            "admin": True,
            "applicationName": "yanddmitest0",
            "author": "yanddmi",
            "availableActions": [],
            "canMigrate": False,
            "comment": "",
            "components": {
                "backend": {
                    "allocatedUnits": 2,
                    "applicationName": "yanddmitest0",
                    "deployable": True,
                    "environmentName": "simplehttpserver",
                    "event": {
                        "componentId": "samogon.yanddmitest0.simplehttpserver.backend",
                        "date": "2020-06-01T18:27:20.240+0300",
                        "instances": [],
                        "status": "NO_CHECKS"
                    },
                    "hash": "sha256:f90b7883851eacfad0031dbf478aa02edc099252bb3160812571eea8385378c9",
                    "name": "backend",
                    "objectId": "samogon.yanddmitest0.simplehttpserver.backend",
                    "projectName": "samogon",
                    "repository": "registry.yandex.net/samogon/simple_http_server:latest",
                    "resourceUsage": -2147483648,
                    "runningInstances": [
                        {
                            "actualState": "ACTIVE",
                            "allocated": True,
                            "box": "man3-0890.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "man4-be8b92e8d7a6.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "backend-1",
                            "instanceIp": "2a02:06b8:0c09:1180:0000:0696:be8b:92e8",
                            "line": "MAN-4#B.1.09",
                            "rack": "4C21",
                            "status": "OK",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "ACTIVE",
                            "updateTimestamp": "2020-06-01T18:27:20.234+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "ACTIVE",
                            "allocated": True,
                            "box": "sas2-4228.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "sas1-14bddbb1f545.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "backend-2",
                            "instanceIp": "2a02:06b8:0c08:d518:0000:0696:14bd:dbb1",
                            "line": "SAS-1.3.1",
                            "rack": "29",
                            "status": "OK",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "ACTIVE",
                            "updateTimestamp": "2020-06-01T18:27:20.240+0300",
                            "weight": 1
                        }
                    ],
                    "statuses": [
                        {
                            "description": "ACTIVE=2",
                            "name": "iss",
                            "status": "OK"
                        }
                    ],
                    "type": "standard",
                    "units": 2
                },
                "backend-proxy": {
                    "allocatedUnits": 8,
                    "applicationName": "yanddmitest0",
                    "deployable": True,
                    "environmentName": "simplehttpserver",
                    "name": "backend-proxy",
                    "objectId": "samogon.yanddmitest0.simplehttpserver.backend-proxy",
                    "projectName": "samogon",
                    "runningInstances": [
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "vla2-9020.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "vla1-d8b397c9fdcb.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-1",
                            "instanceIp": "2a02:06b8:0c0d:2183:0000:0696:d8b3:97c9",
                            "line": "VLA-01",
                            "rack": "1D28",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:19.984+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "vla2-1361.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "vla2-3db8e2505859.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-2",
                            "instanceIp": "2a02:06b8:0c0f:0602:0000:0696:3db8:e250",
                            "line": "VLA-02",
                            "rack": "2A26",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:19.990+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "myt1-3680.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "myt6-811a985dd105.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-3",
                            "instanceIp": "2a02:06b8:0c12:2c05:0000:0696:811a:985d",
                            "line": "MYT-6",
                            "rack": "79",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:19.967+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "iva1-7357.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "iva5-baa870e14951.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-4",
                            "instanceIp": "2a02:06b8:0c0c:069b:0000:0696:baa8:70e1",
                            "line": "IVA-5",
                            "rack": "11",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:20.025+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "vla2-9055.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "vla1-9b545db20e1a.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-5",
                            "instanceIp": "2a02:06b8:0c0d:2600:0000:0696:9b54:5db2",
                            "line": "VLA-01",
                            "rack": "1D18",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:19.996+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "vla2-1368.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "vla2-3f5864df601c.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-6",
                            "instanceIp": "2a02:06b8:0c0f:0783:0000:0696:3f58:64df",
                            "line": "VLA-02",
                            "rack": "2A29",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:20.004+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "PREPARED",
                            "allocated": True,
                            "box": "vla2-9012.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "vla1-c08d7057cd77.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "small-7",
                            "instanceIp": "2a02:06b8:0c0d:1e01:0000:0696:c08d:7057",
                            "line": "VLA-01",
                            "rack": "1A09",
                            "status": "NOT DEPLOYED",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "PREPARED",
                            "updateTimestamp": "2020-06-01T18:27:20.010+0300",
                            "weight": 1
                        },
                        {
                            "actualState": "ACTIVE",
                            "allocated": True,
                            "box": "sas1-2806.search.yandex.net",
                            "checks": [],
                            "detailMessage": "",
                            "errorLevel": "NONE",
                            "host": "sas1-02bf5b9f6df4.qloud-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "balancer-l7-1",
                            "instanceIp": "2a02:06b8:0c08:1f87:0000:0696:02bf:5b9f",
                            "line": "SAS-1.3.1",
                            "rack": "19",
                            "status": "OK",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "ACTIVE",
                            "updateTimestamp": "2020-06-01T18:27:20.114+0300",
                            "weight": 1
                        }
                    ],
                    "statuses": [
                        {
                            "description": "PREPARED=7",
                            "name": "samogon.yanddmitest2.bootstrapyanddmitest2.small:iss",
                            "status": "CRIT"
                        },
                        {
                            "description": "",
                            "name": "samogon.yanddmitest0.yanddmi-test.balancer-l7:juggler",
                            "status": "OK"
                        },
                        {
                            "description": "ACTIVE=1",
                            "name": "samogon.yanddmitest0.yanddmi-test.balancer-l7:iss",
                            "status": "OK"
                        },
                        {
                            "description": "yanddmi-test.stable.qloud-b.yandex.net=WARN",
                            "name": "samogon.yanddmitest0.yanddmi-test.balancer-l7:l3",
                            "status": "WARN"
                        }
                    ],
                    "type": "component-proxy",
                    "units": 0,
                    "upstreamComponents": [
                        {
                            "componentId": "samogon.yanddmitest2.bootstrapyanddmitest2.small",
                            "weight": 1
                        },
                        {
                            "componentId": "samogon.yanddmitest0.yanddmi-test.balancer-l7",
                            "weight": 2
                        }
                    ]
                },
                "my-proxy": {
                    "allocatedUnits": 1,
                    "applicationName": "yanddmitest0",
                    "deployable": True,
                    "environmentName": "simplehttpserver",
                    "host": "simplehttpserver.n.yandex.ru",
                    "name": "my-proxy",
                    "objectId": "samogon.yanddmitest0.simplehttpserver.my-proxy",
                    "projectName": "samogon",
                    "runningInstances": [
                        {
                            "actualState": "-",
                            "address": "[2a02:06b8:0c1a:0505:0000:0696:f5fe:0000]:80",
                            "alive": True,
                            "allocated": True,
                            "backup": False,
                            "box": "awf7gbvo7nejossm.man.yp-c.yandex.net",
                            "checks": [],
                            "errorLevel": "NONE",
                            "host": "awf7gbvo7nejossm.man.yp-c.yandex.net",
                            "instanceHealth": [],
                            "instanceId": "my-proxy1",
                            "instanceIp": "2a02:06b8:0c1a:0505:0000:0696:f5fe:0000",
                            "instanceName": "my-proxy1",
                            "message": "",
                            "options": " weight=1",
                            "port": 80,
                            "quotedAddress": "\\[2a02:06b8:0c1a:0505:0000:0696:f5fe:0000\\]:80",
                            "status": "OK",
                            "statusHookResults": [],
                            "tags": {},
                            "targetState": "-",
                            "updateTimestamp": "2020-06-01T18:27:20.248+0300",
                            "weight": 1
                        }
                    ],
                    "servers": "awf7gbvo7nejossm.man.yp-c.yandex.net",
                    "statuses": [],
                    "type": "proxy",
                    "units": 1
                }
            },
            "configurationId": "NO-ISS-CONFIGURATION",
            "createTimestamp": "2020-05-26T14:16:47.669+0300",
            "creationDate": "2020-05-26T14:16:47.669+0300",
            "deletable": True,
            "deployHooks": [
                ""
            ],
            "domains": [
                {
                    "clientCertificateRequired": False,
                    "domainName": "yanddmi.common.yandex.net",
                    "domainStatus": "ACTIVE",
                    "errorPage": False,
                    "errorPageUrl": "http://any.yandex.ru",
                    "httpAllowed": False,
                    "httpOnly": False,
                    "published": True,
                    "requestClientCertificate": False,
                    "type": "yanddmi-test",
                    "url": "https://yanddmi.common.yandex.net",
                    "warning": "Wrong dns (IPV6), should be 2a02:6b8:0:3400:0:410:0:5. You can set it on https://dns.tt.yandex-team.ru/request\n"
                }
            ],
            "editable": True,
            "engine": "platform",
            "environmentType": "regular",
            "hasSandbox": False,
            "itype": "qloud",
            "logIndexTtl": 3,
            "name": "simplehttpserver",
            "objectId": "samogon.yanddmitest0.simplehttpserver",
            "online": True,
            "process": {
                "completed": "2020-05-26T14:19:00.723+03:00",
                "componentStats": {},
                "currentState": "DEPLOYED",
                "nextState": "DEPLOYED",
                "processState": "DONE",
                "recipes": {},
                "startState": "ACTIVATED",
                "started": "2020-05-26T14:18:58.917+03:00",
                "tasks": [
                    {
                        "componentName": "yanddmi-test",
                        "lastUpdate": "2020-05-26T14:18:59.584+03:00",
                        "name": "Wait balancer yanddmi-test reconfiguration for samogon.yanddmitest0.simplehttpserver",
                        "started": "2020-05-26T14:18:58.841+03:00",
                        "taskId": "deployment-16419331",
                        "taskState": "DONE"
                    }
                ]
            },
            "projectName": "samogon",
            "revertable": False,
            "routers": [
                {
                    "activeRouters": 0,
                    "balancerType": "l7",
                    "domainType": "yanddmi-test",
                    "domainTypes": [
                        "yanddmi-test"
                    ],
                    "expectedRouters": 0,
                    "name": "yanddmi-test",
                    "routers": [],
                    "urlPath": "/l7/yanddmi-test"
                }
            ],
            "routes": [
                {
                    "componentName": "backend",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": False,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "5",
                    "proxyRequestBuffering": False,
                    "proxyWriteTimeout": "5",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                },
                {
                    "componentName": "backend-proxy",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/api",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": False,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "5",
                    "proxyRequestBuffering": False,
                    "proxyWriteTimeout": "5",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/api",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                },
                {
                    "componentName": "my-proxy",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/api2",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": False,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "5",
                    "proxyRequestBuffering": False,
                    "proxyWriteTimeout": "5",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/api",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                }
            ],
            "sandbox": False,
            "shipAccessLog": False,
            "status": "DEPLOYED",
            "statusHistory": {
                "messages": [
                    {
                        "status": "NEW",
                        "time": 1590491807669
                    },
                    {
                        "status": "ALLOCATING",
                        "time": 1590491811357
                    },
                    {
                        "status": "ALLOCATED",
                        "time": 1590491818289
                    },
                    {
                        "status": "CONFIGURING",
                        "time": 1590491818476
                    },
                    {
                        "status": "CONFIGURED",
                        "time": 1590491820837
                    },
                    {
                        "status": "PREPARING",
                        "time": 1590491821768
                    },
                    {
                        "status": "PREPARED",
                        "time": 1590491871384
                    },
                    {
                        "status": "ACTIVATING",
                        "time": 1590491872614
                    },
                    {
                        "status": "ACTIVATED",
                        "time": 1590491938801
                    },
                    {
                        "status": "DEPLOYING",
                        "time": 1590491938927
                    },
                    {
                        "status": "DEPLOYED",
                        "time": 1590491940776
                    }
                ]
            },
            "statusMessage": "Wait balancer yanddmi-test reconfiguration for samogon.yanddmitest0.simplehttpserver (DONE) Done: OK: ",
            "targetState": "DEPLOYED",
            "userVariables": {},
            "version": 1590491807669,
            "versions": [
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [],
                    "comment": "",
                    "creationDate": "2020-05-26T14:16:47.669+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "DEPLOYED",
                    "statusMessage": "Wait balancer yanddmi-test reconfiguration for samogon.yanddmitest0.simplehttpserver (DONE) Done: OK: ",
                    "targetState": "DEPLOYED",
                    "version": 1590491807669
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-11-22T15:22:52.187+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done\nRemove backend-proxy (DONE) Task completed",
                    "targetState": "REMOVED",
                    "version": 1574425372187
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-10-25T14:10:42.926+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1572001842926
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "add juggler",
                    "creationDate": "2019-10-01T16:46:47.943+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1569937607943
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-09-30T19:11:14.782+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1569859874782
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-09-30T19:06:19.253+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1569859579253
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-22T15:21:00.596+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1566476460596
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-22T14:12:40.607+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1566472360607
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-21T17:54:39.492+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1566399279492
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-15T12:39:28.052+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1565861968052
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-08T18:18:58.117+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1565277538117
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-08T18:00:01.460+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1565276401460
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-08-07T17:41:59.400+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1565188919400
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "route",
                    "creationDate": "2019-08-07T17:03:18.821+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1565186598821
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "update sec",
                    "creationDate": "2019-08-05T15:23:02.988+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1565007782988
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-07-30T17:46:00.640+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1564497960640
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-07-25T13:30:27.728+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "Remove backend (DONE) Done",
                    "targetState": "REMOVED",
                    "version": 1564050627728
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [
                        "deploy",
                        "prepare"
                    ],
                    "comment": "",
                    "creationDate": "2019-07-24T19:05:15.591+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "REMOVED",
                    "statusMessage": "",
                    "targetState": "REMOVED",
                    "version": 1563984315591
                },
                {
                    "admin": False,
                    "applicationName": "yanddmitest0",
                    "author": "yanddmi",
                    "availableActions": [],
                    "comment": "Created",
                    "creationDate": "2019-07-24T18:48:42.877+0300",
                    "engine": "platform",
                    "name": "simplehttpserver",
                    "objectId": "samogon.yanddmitest0.simplehttpserver",
                    "projectName": "samogon",
                    "status": "TEMPLATE",
                    "statusMessage": "",
                    "targetState": "NONE",
                    "version": 1563983322877
                }
            ]
        }
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
        awacs_category='users/awacs_test',
        awacs_namespace_id='namespace_id',
        awacs_alerting_recipient=1234,
    ).migrate()
    return dump_result(result)


def test_migrate_environment_nirvana_job_processor():  # dump from nirvana.nirvana-job-processor.test
    result = QloudMigrator({
        "abcId": 4450,
        "dump": {
            "objectId": "nirvana.nirvana-job-processor.test",
            "userEnvironment": """NIRVANA_JOB_PROCESSOR_OAUTH_TOKEN=UNUSED\nRESTART=9\n
JOB_SERVICE_A=https://job-service-a.n.yandex-team.ru\n
DATASOURCE_HOSTS=sas-h90lvt7jj60z94ae.db.yandex.net:6432,man-oh0yeqlolshv5d3m.db.yandex.net:6432,vla-arfj71hqlonfuil9.db.yandex.net:6432\n
DATASOURCE_MAX_TOTAL=20\nDATASOURCE_MAX_IDLE=10\n""",
            "userEnvironmentMap": {
                "DATASOURCE_HOSTS": "sas-h90lvt7jj60z94ae.db.yandex.net:6432,man-oh0yeqlolshv5d3m.db.yandex.net:6432,vla-arfj71hqlonfuil9.db.yandex.net:6432",
                "DATASOURCE_MAX_IDLE": "10",
                "DATASOURCE_MAX_TOTAL": "20",
                "JOB_SERVICE_A": "https://job-service-a.n.yandex-team.ru",
                "NIRVANA_JOB_PROCESSOR_OAUTH_TOKEN": "UNUSED",
                "RESTART": "9"
            },
            "components": [
                {
                    "componentName": "backend",
                    "componentType": "standard",
                    "properties": {
                        "allocationFailThreshold": "0",
                        "allocationStrategy": "dynamic",
                        "allowedCpuNames": "",
                        "componentEnvironment": """REDEPLOY=2\nPORTO_LAYER_UPLOADER_YT_READ_TIMEOUT=60000\n
YT_PROBE_LAYER_PATH=//home/qe/nirvana/testing/porto_layers/system/ubuntu-trusty-base.44afcb12a98b1b45e1ea11d03b644771.tar.xz\n
JOB_INTERNET_ALLOWED_PROJECTS=default\nJOB_PROCESSOR_RESOURCE_LAUNCHER_BETA_ENABLED=true\n
JOB_PROCESSOR_RESOURCE_LAUNCHER_BETA_URI=https://test.nirvana.yandex-team.ru/api/storedData/b0fd7c99-0ff5-401d-adc4-4bf1076f42ac/data\n
JOB_PROCESSOR_RESOURCE_LAUNCHER_BETA_MD5=962ceac939bff5bf4cefd8ac317464b1\n
QLOUD_JAVA_OPTIONS=-Dfile.encoding=UTF-8  -Xmx4000m  -Djava.net.preferIPv6Addresses=true  -XX:+PrintCommandLineFlags  -verbose:gc
-Xlog:gc*  -Xlog:age*=trace  -Xlog:safepoint  -XX:+DisableExplicitGC  -XX:+UseConcMarkSweepGC  -XX:+CMSClassUnloadingEnabled
-XX:ParallelGCThreads=2  -XX:CMSInitiatingOccupancyFraction=80 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:7999\n
QLOUD_PROFILES=test""",
                        "deployPolicy": "InPlace",
                        "diskSerial": "0",
                        "diskSize": "30",
                        "dnsCache": "true",
                        "dnsNat64": "false",
                        "ediskSerial": "0",
                        "ediskSize": "2",
                        "failTimeout": "-1",
                        "fastboneRequired": "false",
                        "hardwareSegment": "mtn",
                        "hash": "sha256:560e96ee03849bd9916daccb3da763ac05fa4893f493a8e7071edb13fadab5a0",
                        "healthCheckFall": "5",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "2",
                        "healthCheckTimeout": "2000",
                        "healthCheckType": "http",
                        "httpCheckOn": "true",
                        "ioLimit": "0",
                        "isolationGroup": "root",
                        "isolationUser": "root",
                        "java": "false",
                        "listenBacklogSize": "511",
                        "maxFails": "-1",
                        "maxInstancesPerHost": "999",
                        "maxWorkerConnections": "16384",
                        "minPrepared": "50",
                        "multiAccept": "false",
                        "network": "NIRVANANETS",
                        "preAuthenticate": "false",
                        "profiles": "production",
                        "qloudCoreDumpDirectory": "/coredumps_qloud",
                        "qloudCoreDumpFileSizeGb": "0",
                        "qloudInitPolicy": "stable",
                        "qloudInitVersion": "607",
                        "qloudMaxCoreDumpedStopsRespawnDelay": "0s",
                        "qloudMaxCoreDumpsOnDisk": "0",
                        "repository": "registry.yandex.net/qe_quality/nirvana_nirvana_job_processor:1.3734_java11",
                        "size": "4;1.0;64;1",
                        "sslStapling": "false",
                        "statusChecksCorrected": "true",
                        "stderr": "ignore",
                        "stdout": "ignore",
                        "storage": "",
                        "tmpfsDiskSerial": "0",
                        "tmpfsDiskSize": "0",
                        "unistat": "true",
                        "unistatPath": "/unistat",
                        "unistatPort": "80",
                        "units": "0",
                        "upstreamPort": "80",
                        "useDockerUserGroup": "false",
                        "useHealthCheck": "false",
                        "useHttps": "false",
                        "useTorrents": "false",
                        "usedVolumes": ""
                    },
                    "secrets": [
                        {
                            "objectId": "secret.job-processor-mds-prod-s3-access-key",
                            "target": "NIRVANA_MDS_STATIC_ACCESS_KEY",
                            "used": True
                        },
                        {
                            "objectId": "secret.job-processor-mds-prod-s3-secret-key",
                            "target": "NIRVANA_MDS_STATIC_SECRET_KEY",
                            "used": True
                        },
                        {
                            "objectId": "secret.job-processor-mds-prod-tvm-client-oauth-token",
                            "target": "NIRVANA_MDS_TVM_CLIENT_OAUTH_TOKEN",
                            "used": True
                        },
                        {
                            "objectId": "secret.nirvana-job-processor-s3-access-key",
                            "target": "NIRVANA_JOB_PROCESSOR_S3_ACCESS_KEY",
                            "used": True
                        },
                        {
                            "objectId": "secret.nirvana-job-processor-s3-secret-key",
                            "target": "NIRVANA_JOB_PROCESSOR_S3_SECRET_KEY",
                            "used": True
                        },
                        {
                            "objectId": "secret.nirvana-mds-prod-tvm-client-id",
                            "target": "NIRVANA_MDS_TVM_CLIENT_ID",
                            "used": True
                        },
                        {
                            "objectId": "secret.nirvana-mds-prod-tvm-client-secret",
                            "target": "NIRVANA_MDS_TVM_CLIENT_SECRET",
                            "used": True
                        },
                        {
                            "objectId": "secret.nirvana-sandbox-oauth-token",
                            "target": "NIRVANA_SECRETS_OAUTH_TOKEN",
                            "used": True
                        },
                        {
                            "objectId": "secret.pgaas-job-processor-db-test-password",
                            "target": "PGAAS_JOB_PROCESSOR_DB_TEST_PASSWORD",
                            "used": True
                        },
                        {
                            "objectId": "secret.robot-job-processor-yt-token",
                            "target": "NIRVANA_JOB_PROCESSOR_YT_TOKEN",
                            "used": True
                        },
                        {
                            "objectId": "secret.robot-nirvana-yt-token",
                            "target": "SCHEDULER_YT_TOKEN",
                            "used": True
                        }
                    ],
                    "instanceGroups": [
                        {
                            "location": "ALL",
                            "units": 1,
                            "backup": False,
                            "weight": 1
                        },
                        {
                            "location": "VLADIMIR",
                            "units": 1,
                            "backup": False,
                            "weight": 1
                        }
                    ],
                    "overlays": [],
                    "sandboxResources": [],
                    "jugglerBundleResources": [],
                    "environmentVariables": {
                        "REDEPLOY": "2",
                        "PORTO_LAYER_UPLOADER_YT_READ_TIMEOUT": "60000",
                        "YT_PROBE_LAYER_PATH": "//home/qe/nirvana/testing/porto_layers/system/ubuntu-trusty-base.44afcb12a98b1b45e1ea11d03b644771.tar.xz",
                        "JOB_INTERNET_ALLOWED_PROJECTS": "default",
                        "JOB_PROCESSOR_RESOURCE_LAUNCHER_BETA_ENABLED": "true",
                        "JOB_PROCESSOR_RESOURCE_LAUNCHER_BETA_URI": "https://test.nirvana.yandex-team.ru/api/storedData/b0fd7c99-0ff5-401d-adc4-4bf1076f42ac/data",
                        "JOB_PROCESSOR_RESOURCE_LAUNCHER_BETA_MD5": "962ceac939bff5bf4cefd8ac317464b1",
                        "QLOUD_JAVA_OPTIONS": """-Dfile.encoding=UTF-8  -Xmx4000m  -Djava.net.preferIPv6Addresses=true  -XX:+PrintCommandLineFlags
-verbose:gc  -Xlog:gc*  -Xlog:age*=trace  -Xlog:safepoint  -XX:+DisableExplicitGC
-XX:+UseConcMarkSweepGC  -XX:+CMSClassUnloadingEnabled  -XX:ParallelGCThreads=2  -XX:CMSInitiatingOccupancyFraction=80
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:7999""",
                        "QLOUD_PROFILES": "test"
                    },
                    "prepareRecipe": {
                        "recipe": "INTERNAL",
                        "updateWindow": "100%",
                        "doneThreshold": "90%",
                        "updateLimit": "100%",
                        "updatePeriod": "20s"
                    },
                    "activateRecipe": {
                        "recipe": "INTERNAL",
                        "updateWindow": "100%",
                        "doneThreshold": "90%",
                        "updateLimit": "100%",
                        "updatePeriod": "20"
                    },
                    "statusHookChecks": [
                        {
                            "type": "http",
                            "port": 1,
                            "path": "/ping",
                            "timeout": 1000
                        },
                        {
                            "type": "tcp",
                            "port": 80,
                            "path": "",
                            "timeout": 1000
                        }
                    ],
                    "embedJugglerClient": False,
                    "componentFeatures": [],
                    "l3Config": {
                        "vss": []
                    },
                    "upstreamComponents": []
                }
            ],
            "routeSettings": [
                {
                    "location": "/api/",
                    "componentName": "backend",
                    "upstreamPath": "/api/",
                    "proxyConnectTimeout": "-1",
                    "proxyReadTimeout": "120s",
                    "proxyWriteTimeout": "-1",
                    "proxyPolicy": "round_robin",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 0,
                    "proxyNextUpstreamTries": 0,
                    "preAuthenticate": False,
                    "geo": False,
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "wallarm": False,
                    "proxyBuffering": False,
                    "proxyRequestBuffering": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffers": "-1",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED",
                    "webAuthCookies": "DISABLED",
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "noUriPart": False,
                    "molly": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultWebAuthToken": "NO_CHECK",
                    "defaultProxyBuffers": "-1",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultNoUriPart": False,
                    "defaultErrorPageUrl": "http://any.yandex.ru"
                }
            ],
            "comment": """Update backend: {repository=registry.yandex.net/qe_quality/nirvana_nirvana_job_processor:1.3734_java11,
 hash=sha256:560e96ee03849bd9916daccb3da763ac05fa4893f493a8e7071edb13fadab5a0}""",
            "engine": "platform"
        },
        "domains": [
            {
                "domainName": "job-processor-test.nirvana.yandex-team.ru",
                "httpOnly": False,
                "type": "nirvana-balancer",
                "httpAllowed": False,
                "requestClientCertificate": False,
                "clientCertificateRequired": False,
                "errorPage": False,
                "errorPageUrl": "http://any.yandex.ru",
                "warning": "",
                "published": True,
                "domainStatus": "ACTIVE",
                "url": "https://job-processor-test.nirvana.yandex-team.ru",
                "certificateIssuer": "DC=ru,DC=yandex,DC=ld,CN=YandexInternalCA",
                "certificateValidFor": "job-processor-test.nirvana.yandex-team.ru",
                "certificateValidNotBefore": "2018-09-03T22:10:45.000+03:00",
                "certificateValidNotAfter": "2020-09-02T22:10:45.000+03:00",
                "moveTargets": [
                    "nirvana.nirvana-job-processor.test",
                    "nirvana.svn-processor.production",
                    "nirvana.svn-processor.testing"
                ],
                "requester": "finiriarh",
                "certificateSerialNumber": "55:0B:F1:DF:00:02:00:03:D0:81",
                "certificateMd5": "2A:44:17:97:B6:FF:E4:85:F0:DE:D5:47:28:3C:B2:A4",
                "certificateSha1": "F5:11:7A:7D:4D:11:8C:66:79:0C:9A:5C:39:38:ED:EA:B5:61:2F:97",
                "certificateSha256": "43:8C:67:74:DD:61:31:35:62:60:A1:8E:31:DE:B7:A9:E3:79:2B:3F:30:7C:15:48:95:1C:4D:72:F9:0E:7D:37",
                "statuses": {
                    "CERTIFICATOR": "ACTIVE",
                    "NONE": "ACTIVE",
                    "SECRET": "ACTIVE",
                    "ZONE": "ACTIVE"
                },
                "certificateSource": "CERTIFICATOR",
                "certificates": {
                    "ZONE": {},
                    "SECRET": {},
                    "CERTIFICATOR": {
                        "certificateIssuer": "DC=ru,DC=yandex,DC=ld,CN=YandexInternalCA",
                        "certificateValidFor": "job-processor-test.nirvana.yandex-team.ru",
                        "certificateValidNotBefore": "2018-09-03T22:10:45.000+03:00",
                        "certificateValidNotAfter": "2020-09-02T22:10:45.000+03:00",
                        "certificateSerialNumber": "55:0B:F1:DF:00:02:00:03:D0:81",
                        "certificateMd5": "2A:44:17:97:B6:FF:E4:85:F0:DE:D5:47:28:3C:B2:A4",
                        "certificateSha1": "F5:11:7A:7D:4D:11:8C:66:79:0C:9A:5C:39:38:ED:EA:B5:61:2F:97",
                        "certificateSha256": "43:8C:67:74:DD:61:31:35:62:60:A1:8E:31:DE:B7:A9:E3:79:2B:3F:30:7C:15:48:95:1C:4D:72:F9:0E:7D:37"
                    },
                    "NONE": {}
                },
                "CA": "InternalCA"
            }
        ],
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
        awacs_category='users/awacs_test',
        awacs_namespace_id='namespace_id',
        awacs_alerting_recipient=1234,
    ).migrate()
    return dump_result(result)


def test_migrate_environment_abt_dashboard_production():  # dump from abt.dashboard.production
    result = QloudMigrator({
        "abcId": 4450,
        "dump": {
            "objectId": "abt.dashboard.production",
            "userEnvironment": "",
            "userEnvironmentMap": {},
            "components": [
                {
                    "componentName": "ui",
                    "componentType": "standard",
                    "properties": {
                        "allocationFailThreshold": "0",
                        "allocationStrategy": "dynamic",
                        "allowedCpuNames": "",
                        "componentEnvironment": "",
                        "deployPolicy": "InPlace",
                        "diskSerial": "0",
                        "diskSize": "5",
                        "dnsCache": "true",
                        "dnsNat64": "false",
                        "ediskSerial": "0",
                        "ediskSize": "0",
                        "failTimeout": "60",
                        "fastboneRequired": "false",
                        "hardwareSegment": "common",
                        "hash": "sha256:ad461b2b6ecaa2453bd3a76f590196e9ee62a3c8fbe137f297f48ff9440091f0",
                        "healthCheckFall": "3",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/ping",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "3",
                        "healthCheckTimeout": "2000",
                        "healthCheckType": "http",
                        "httpCheckOn": "true",
                        "ioLimit": "0",
                        "isolationGroup": "root",
                        "isolationUser": "root",
                        "java": "false",
                        "listenBacklogSize": "511",
                        "maxFails": "0",
                        "maxInstancesPerHost": "999",
                        "maxWorkerConnections": "16384",
                        "minPrepared": "50",
                        "multiAccept": "false",
                        "network": "ABT_PROD_NETS",
                        "preAuthenticate": "false",
                        "profiles": "production",
                        "qloudCoreDumpDirectory": "/coredumps_qloud",
                        "qloudCoreDumpFileSizeGb": "0",
                        "qloudInitPolicy": "stable",
                        "qloudInitVersion": "607",
                        "qloudMaxCoreDumpedStopsRespawnDelay": "0s",
                        "qloudMaxCoreDumpsOnDisk": "0",
                        "repository": "registry.yandex.net/data-ui/abt-dashboard:0.16.3",
                        "size": "2;0.5;16;1",
                        "sslStapling": "false",
                        "statusChecksCorrected": "true",
                        "stderr": "line",
                        "stdout": "line",
                        "storage": "",
                        "tmpfsDiskSerial": "0",
                        "tmpfsDiskSize": "0",
                        "unistat": "false",
                        "unistatPath": "/unistat",
                        "unistatPort": "80",
                        "units": "0",
                        "upstreamPort": "80",
                        "useDockerUserGroup": "false",
                        "useHealthCheck": "true",
                        "useHttps": "false",
                        "useTorrents": "false",
                        "usedVolumes": ""
                    },
                    "secrets": [],
                    "instanceGroups": [
                        {
                            "location": "IVA",
                            "units": 1,
                            "backup": False,
                            "weight": 1
                        },
                        {
                            "location": "MYT",
                            "units": 1,
                            "backup": False,
                            "weight": 1
                        },
                        {
                            "location": "SASOVO",
                            "units": 1,
                            "backup": False,
                            "weight": 1
                        }
                    ],
                    "overlays": [],
                    "sandboxResources": [],
                    "jugglerBundleResources": [],
                    "environmentVariables": {},
                    "prepareRecipe": {
                        "recipe": "INTERNAL",
                        "updateWindow": "33%",
                        "doneThreshold": "90%",
                        "updateLimit": "100%",
                        "updatePeriod": "20s"
                    },
                    "activateRecipe": {
                        "recipe": "INTERNAL",
                        "updateWindow": "100%",
                        "doneThreshold": "90%",
                        "updateLimit": "20%",
                        "updatePeriod": "20s"
                    },
                    "statusHookChecks": [
                        {
                            "type": "http",
                            "port": 80,
                            "path": "/ping",
                            "timeout": 1000
                        }
                    ],
                    "embedJugglerClient": False,
                    "componentFeatures": [],
                    "l3Config": {
                        "vss": []
                    },
                    "upstreamComponents": [],
                    "tvmConfig": {
                        "blackBoxType": "PROD",
                        "clients": [
                            {
                                "secretName": "abt-prod-tvm",
                                "name": "app",
                                "destinations": [
                                    {
                                        "name": "blackbox",
                                        "tvmId": 223
                                    }
                                ]
                            }
                        ]
                    }
                }
            ],
            "routeSettings": [
                {
                    "location": "/",
                    "componentName": "ui",
                    "upstreamPath": "/",
                    "proxyConnectTimeout": "60ms",
                    "proxyReadTimeout": "5",
                    "proxyWriteTimeout": "5",
                    "proxyPolicy": "round_robin",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "preAuthenticate": False,
                    "geo": False,
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "wallarm": False,
                    "proxyBuffering": False,
                    "proxyRequestBuffering": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffers": "-1",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED",
                    "webAuthCookies": "DISABLED",
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "noUriPart": False,
                    "molly": False,
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultWebAuthToken": "NO_CHECK",
                    "defaultProxyBuffers": "-1",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultNoUriPart": False
                }
            ],
            "comment": "",
            "engine": "platform"
        },
        "domains": [
            {
                "domainName": "abt.yandex-team.ru",
                "httpOnly": False,
                "type": "abt",
                "httpAllowed": False,
                "requestClientCertificate": False,
                "clientCertificateRequired": False,
                "errorPage": False,
                "errorPageUrl": "http://any.yandex.ru",
                "warning": "",
                "published": True,
                "domainStatus": "ACTIVE",
                "url": "https://abt.yandex-team.ru",
                "certificateIssuer": "DC=ru,DC=yandex,DC=ld,CN=YandexInternalCA",
                "certificateValidFor": "abt.yandex-team.ru",
                "certificateValidNotBefore": "2019-04-18T18:50:20.000+03:00",
                "certificateValidNotAfter": "2021-04-17T18:50:20.000+03:00",
                "moveTargets": [
                    "abt.dashboard.production"
                ],
                "requester": "tsufiev",
                "certificateSerialNumber": "6A:D6:9D:2E:00:02:00:06:BD:2F",
                "certificateMd5": "AE:F4:30:3A:17:8A:6D:90:22:19:F7:A2:5E:34:B5:D3",
                "certificateSha1": "97:22:01:1A:D6:9D:73:6E:5D:75:8D:3D:3E:EE:F3:17:C2:B8:60:68",
                "certificateSha256": "4B:E0:82:4C:B3:8D:E9:FF:95:53:F1:AE:AC:87:DD:DC:57:BA:E3:A1:23:BB:A0:F4:42:2C:3A:FA:75:65:7D:42",
                "statuses": {
                    "CERTIFICATOR": "ACTIVE",
                    "NONE": "ACTIVE",
                    "SECRET": "ACTIVE",
                    "ZONE": "ACTIVE"
                },
                "certificateSource": "CERTIFICATOR",
                "certificates": {
                    "ZONE": {},
                    "SECRET": {},
                    "CERTIFICATOR": {
                        "certificateIssuer": "DC=ru,DC=yandex,DC=ld,CN=YandexInternalCA",
                        "certificateValidFor": "abt.yandex-team.ru",
                        "certificateValidNotBefore": "2019-04-18T18:50:20.000+03:00",
                        "certificateValidNotAfter": "2021-04-17T18:50:20.000+03:00",
                        "certificateSerialNumber": "6A:D6:9D:2E:00:02:00:06:BD:2F",
                        "certificateMd5": "AE:F4:30:3A:17:8A:6D:90:22:19:F7:A2:5E:34:B5:D3",
                        "certificateSha1": "97:22:01:1A:D6:9D:73:6E:5D:75:8D:3D:3E:EE:F3:17:C2:B8:60:68",
                        "certificateSha256": "4B:E0:82:4C:B3:8D:E9:FF:95:53:F1:AE:AC:87:DD:DC:57:BA:E3:A1:23:BB:A0:F4:42:2C:3A:FA:75:65:7D:42"
                    },
                    "NONE": {}
                },
                "CA": "InternalCA"
            }
        ]
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
        awacs_category='users/awacs_test',
        awacs_namespace_id='namespace_id',
        awacs_alerting_recipient=1234,
    ).migrate()
    return dump_result(result)


def test_migrate_environment_april_vertica_prod():  # dump from april.vertica.prod
    result = QloudMigrator({
        "abcId": 4450,
        "dump": {
            "objectId": "april.vertica.prod",
            "userEnvironment": "",
            "userEnvironmentMap": {},
            "components": [
                {
                    "componentName": "db",
                    "componentType": "standard",
                    "properties": {
                        "allocationFailThreshold": "0",
                        "allocationStrategy": "storage",
                        "allowedCpuNames": "",
                        "componentEnvironment": "",
                        "deployPolicy": "InPlace",
                        "diskSerial": "0",
                        "diskSize": "5",
                        "dnsCache": "true",
                        "dnsNat64": "true",
                        "ediskSerial": "0",
                        "ediskSize": "0",
                        "failTimeout": "60",
                        "fastboneRequired": "false",
                        "hardwareSegment": "april",
                        "hash": "sha256:d820ac06d1a5a043e75dc0fdc8c4bf9dad2c5ee1f7e722300d588487c951ba1f",
                        "healthCheckFall": "5",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "2",
                        "healthCheckTimeout": "2000",
                        "healthCheckType": "http",
                        "httpCheckOn": "true",
                        "ioLimit": "0",
                        "isolationGroup": "root",
                        "isolationUser": "root",
                        "java": "false",
                        "listenBacklogSize": "511",
                        "maxFails": "3",
                        "maxInstancesPerHost": "999",
                        "maxWorkerConnections": "16384",
                        "minPrepared": "50",
                        "multiAccept": "false",
                        "network": "APRIL_NETS",
                        "preAuthenticate": "false",
                        "profiles": "production",
                        "qloudCoreDumpDirectory": "/coredumps_qloud",
                        "qloudCoreDumpFileSizeGb": "0",
                        "qloudInitPolicy": "stable",
                        "qloudInitVersion": "607",
                        "qloudMaxCoreDumpedStopsRespawnDelay": "0s",
                        "qloudMaxCoreDumpsOnDisk": "0",
                        "repository": "registry.yandex.net/april/vertica-db:9-docker57",
                        "size": "115;30.0;1024;1",
                        "sslStapling": "false",
                        "statusChecksCorrected": "true",
                        "stderr": "line",
                        "stdout": "line",
                        "storage": "april-vertica",
                        "tmpfsDiskSerial": "0",
                        "tmpfsDiskSize": "0",
                        "unistat": "false",
                        "unistatPath": "/unistat",
                        "unistatPort": "80",
                        "units": "0",
                        "upstreamPort": "80",
                        "useDockerUserGroup": "false",
                        "useHealthCheck": "false",
                        "useHttps": "false",
                        "useTorrents": "true",
                        "usedVolumes": "april-vertica-v1,april-vertica-v2"
                    },
                    "secrets": [
                        {
                            "objectId": "secret.april-vertica-root-pkey",
                            "target": "/root/.ssh/id_rsa",
                            "used": True
                        }
                    ],
                    "instanceGroups": [],
                    "overlays": [],
                    "sandboxResources": [],
                    "jugglerBundleResources": [],
                    "environmentVariables": {},
                    "prepareRecipe": {
                        "recipe": "INTERNAL",
                        "updateWindow": "100%",
                        "doneThreshold": "1",
                        "updateLimit": "100%",
                        "updatePeriod": "20s"
                    },
                    "activateRecipe": {
                        "recipe": "INTERNAL",
                        "updateWindow": "3",
                        "doneThreshold": "1",
                        "updateLimit": "3",
                        "updatePeriod": "20"
                    },
                    "statusHookChecks": [
                        {
                            "type": "http",
                            "port": 8000,
                            "path": "/status",
                            "timeout": 1000
                        }
                    ],
                    "embedJugglerClient": False,
                    "componentFeatures": [],
                    "stopAction": {
                        "type": "http",
                        "timeoutMs": 60000,
                        "httpPort": 8000,
                        "httpPath": "/stop",
                        "httpMethod": "GET"
                    },
                    "upstreamComponents": []
                }
            ],
            "routeSettings": [],
            "comment": "QLOUDDEV-2290: qloud-init autoupdate",
            "engine": "platform"
        },
        "domains": [],
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
        awacs_category='users/awacs_test',
        awacs_namespace_id='namespace_id',
        awacs_alerting_recipient=1234,
    ).migrate()
    return dump_result(result)


def test_migrate_environment_metrika_cms_production():  # dump from metrika.cms.production
    result = QloudMigrator({
        "abcId": 4450,
        "dump": {
            "comment": "",
            "components": [
                {
                    "activateRecipe": {
                        "doneThreshold": "90%",
                        "recipe": "INTERNAL",
                        "updateLimit": "20%",
                        "updatePeriod": "20s",
                        "updateWindow": "100%"
                    },
                    "componentFeatures": [],
                    "componentName": "frontend",
                    "componentType": "standard",
                    "embedJugglerClient": False,
                    "environmentVariables": {
                        "CMS_BISHOP_ENVIRONMENT": "metrika.qloud.cms.frontend.production",
                        "CMS_BISHOP_PROGRAM": "cms-frontend",
                        "CMS_GUNICORN_BISHOP_PROGRAM": "cms-frontend-gunicorn"
                    },
                    "instanceGroups": [
                        {
                            "backup": False,
                            "location": "SASOVO",
                            "units": 1,
                            "weight": 1
                        },
                        {
                            "backup": False,
                            "location": "VLADIMIR",
                            "units": 1,
                            "weight": 1
                        }
                    ],
                    "jugglerBundleResources": [],
                    "l3Config": {
                        "vss": []
                    },
                    "overlays": [],
                    "prepareRecipe": {
                        "doneThreshold": "90%",
                        "recipe": "INTERNAL",
                        "updateLimit": "100%",
                        "updatePeriod": "20s",
                        "updateWindow": "100%"
                    },
                    "properties": {
                        "allocationFailThreshold": "0",
                        "allocationStrategy": "dynamic",
                        "allowedCpuNames": "",
                        "componentEnvironment": "CMS_GUNICORN_BISHOP_PROGRAM=cms-frontend-gunicorn\nCMS_BISHOP_PROGRAM=cms-frontend\nCMS_BISHOP_ENVIRONMENT=metrika.qloud.cms.frontend.production",
                        "deployPolicy": "InPlace",
                        "diskSerial": "0",
                        "diskSize": "5",
                        "dnsCache": "true",
                        "dnsNat64": "false",
                        "ediskSerial": "0",
                        "ediskSize": "5",
                        "failTimeout": "60",
                        "fastboneRequired": "false",
                        "hardwareSegment": "common",
                        "hash": "sha256:ff1ebfbb1159cad5b4d00a8596758d8cd21c8282ed61e96b0291c8d427bfb0db",
                        "healthCheckFall": "5",
                        "healthCheckHttpExpectedCode": "http_2xx",
                        "healthCheckHttpUrl": "/ping/db_read",
                        "healthCheckInterval": "5000",
                        "healthCheckRise": "2",
                        "healthCheckTimeout": "3000",
                        "healthCheckType": "http",
                        "httpCheckOn": "true",
                        "ioLimit": "0",
                        "isolationGroup": "root",
                        "isolationUser": "root",
                        "java": "false",
                        "listenBacklogSize": "511",
                        "maxFails": "0",
                        "maxInstancesPerHost": "999",
                        "maxWorkerConnections": "16384",
                        "minPrepared": "50",
                        "multiAccept": "false",
                        "network": "YMETRIC",
                        "notResolveServerName": "false",
                        "preAuthenticate": "false",
                        "profiles": "production",
                        "qloudCoreDumpDirectory": "/coredumps_qloud",
                        "qloudCoreDumpFileSizeGb": "0",
                        "qloudInitPolicy": "stable",
                        "qloudInitVersion": "616",
                        "qloudMaxCoreDumpedStopsRespawnDelay": "0s",
                        "qloudMaxCoreDumpsOnDisk": "0",
                        "repository": "registry.yandex.net/metrika/cms-frontend:560782783.6041133",
                        "size": "1;0.5;16;1",
                        "sslStapling": "false",
                        "statusChecksCorrected": "true",
                        "stderr": "line",
                        "stdout": "line",
                        "storage": "",
                        "tmpfsDiskSerial": "0",
                        "tmpfsDiskSize": "0",
                        "unistat": "false",
                        "unistatPath": "/unistat",
                        "unistatPort": "80",
                        "units": "0",
                        "upstreamPort": "80",
                        "useDockerUserGroup": "false",
                        "useHealthCheck": "true",
                        "useHttps": "false",
                        "useTorrents": "false",
                        "usedVolumes": ""
                    },
                    "sandboxResources": [],
                    "secrets": [
                        {
                            "objectId": "secret.robot-metrika-admin-oauth",
                            "target": "ROBOT_METRIKA_ADMIN_OAUTH",
                            "used": True
                        }
                    ],
                    "statusHookChecks": [
                        {
                            "path": "/ping/db_read",
                            "port": 80,
                            "timeout": 3000,
                            "type": "http"
                        }
                    ],
                    "upstreamComponents": []
                }
            ],
            "engine": "platform",
            "objectId": "metrika.cms.production",
            "routeSettings": [
                {
                    "componentName": "frontend",
                    "defaultErrorPageUrl": "http://any.yandex.ru",
                    "defaultNoUriPart": False,
                    "defaultProxyBufferSize": "-1",
                    "defaultProxyBuffers": "-1",
                    "defaultProxyConnectTimeout": "60ms",
                    "defaultProxyNextUpstream": "error timeout",
                    "defaultProxyNextUpstreamTimeout": 10,
                    "defaultProxyNextUpstreamTries": 3,
                    "defaultProxyPolicy": "round_robin",
                    "defaultProxyReadTimeout": "5",
                    "defaultProxyWriteTimeout": "5",
                    "defaultWebAuthCert": "NO_CHECK",
                    "defaultWebAuthCookie": "NO_CHECK",
                    "defaultWebAuthIdmRole": False,
                    "defaultWebAuthToken": "NO_CHECK",
                    "errorPage": "INHERITED",
                    "errorPageUrl": "http://any.yandex.ru",
                    "geo": False,
                    "keepAliveTimeout": "-1s",
                    "location": "/",
                    "molly": False,
                    "noUriPart": False,
                    "preAuthenticate": False,
                    "proxyBufferSize": "-1",
                    "proxyBuffering": False,
                    "proxyBuffers": "-1",
                    "proxyConnectTimeout": "60ms",
                    "proxyNextUpstream": "error timeout",
                    "proxyNextUpstreamTimeout": 10,
                    "proxyNextUpstreamTries": 3,
                    "proxyPolicy": "round_robin",
                    "proxyReadTimeout": "15",
                    "proxyRequestBuffering": False,
                    "proxyWriteTimeout": "15",
                    "resolver": {
                        "resolverTimeout": "-1s",
                        "resolverValid": "-1s"
                    },
                    "upstreamPath": "/",
                    "wallarm": False,
                    "webAuthCert": "DISABLED",
                    "webAuthCheckIdm": False,
                    "webAuthCookies": "DISABLED",
                    "webAuthMode": "OFF",
                    "webAuthToken": "DISABLED"
                }
            ],
            "userEnvironment": "I_WISH_TO_DO_RESTART=4\n",
            "userEnvironmentMap": {
                "I_WISH_TO_DO_RESTART": "4"
            }
        }
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
        awacs_category='users/awacs_test',
        awacs_namespace_id='namespace_id',
        awacs_alerting_recipient=1234,
    ).migrate()
    return dump_result(result)
