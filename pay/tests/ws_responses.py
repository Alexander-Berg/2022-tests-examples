# -*- encoding: utf-8 -*-
import datetime
from copy import deepcopy
from typing import Any, Dict, List, Optional

from yb_darkspirit import scheme
from yb_darkspirit.constants import as_str_list, BSO_WORKMODE, DEFAULT_WORKMODE


CR_SN = "381002827555"
CR_LONG_SN = CR_SN.zfill(20)
CR_ADDRESS_CODE = 'MOW>MOROZOV'
FS_SN = "9999078900003131"
SUPPORTED_FS_TYPE_CODE = scheme.SUPPORTED_FISCAL_STORAGE_TYPES[0]
UNSUPPORTED_FS_TYPE_CODE = scheme.FISCAL_STORAGE_TYPE_CODE_FN_1
SECOND_FS_SN = "9999078900003130"
NOW = datetime.datetime.now()


def empty_registration_info(cr_sn, fn_sn, work_mode=None):
    # type: (str, str, Optional[List[str]]) -> Dict[str, Any]
    return {
        "user_inn": "",
        "sn": cr_sn.zfill(20),
        "rnm": "",
        "tax_mode": [],
        "work_mode": work_mode or [],
        "user_name": "",
        "account_address": "",
        "representative_for_registration": "",
        "ofd_inn": "",
        "register_sn": "",
        "representative_inn": "",
        "account_address_name": "",
        "ofd_name": "",
        "agent_mode": [
            "none_agent"
        ],
        "terminal_number": "",
        "fn_sn": fn_sn,
        "user_reply_email": ""
    }

CASHMACHINES = {
    "now": "2017-10-08 16:43:07",
    "now_time": 10508547.393,
    "start_dt": "2017-10-02 16:52:05",
    "start_time": 9990685.607,
    "startup_args": "Namespace(config_file='kkt_srv_config.yaml', log_dir=None, log_file=None, port=None, swagger_base='/etc/yandex/balance-whitespirit/', swagger_file=None)",
    "settings": "ChainMap({}, {'event_loop': 'uvloop', 'logging': {'version': 1, 'disable_existing_loggers': False, 'formatters': {'default': {'format': '%(asctime)s: P%(process)d: %(levelname)s: %(name)s: %(message)s'}}, 'handlers': {'log_file': {'level': 'DEBUG', 'formatter': 'default', 'encoding': 'utf8', 'class': 'logging.handlers.TimedRotatingFileHandler', 'filename': '/var/log/yandex/whitespirit/whitespirit.log', 'when': 'midnight', 'backupCount': 10, 'delay': False}}, 'root': {'handlers': ['log_file'], 'level': 'DEBUG', 'propagate': True}, 'loggers': {'proto.starrus': {'level': 'DEBUG', 'propogate': False}}}, 'debug': True, 'darkspirit_baseurl': 'https://greed-tm1f.yandex.ru:8616/', 'filter_sn': ['00000000381001942057', '00000000381002827554', '00000000381001543159'], 'devices': {'tst1': {'proto': 'starrus', 'net': '95.108.178.160/28', 'port': 3333, 'json_port': 4444}}}, {'host': '0.0.0.0', 'port': 18080, 'shutdown_timeout': 10, 'backlog': 128, 'handle_signals': True, 'swagger_file': 'swagger.yaml', 'CHECK_CONN_TIMEOUT': 6, 'CHECK_CONN_FULL_TIMEOUT': 60, 'CHECK_UPLOAD_TIMEOUT': 6, 'DARKSPIRIT_TIMEOUT': 60, 'ARPCACHE_TIMEOUT': 30, 'REFRESH_LOCALTIMEZONE_TIMEOUT': 60, 'DEVICE_FAIL_CLEANUP_TIMEOUT': 600, 'call_stats_max_length': 10000, 'extern_baseurl_template': 'https://{hostname}:8080/', 'render_url_template': 'https://check.greed-ts1f.yandex.ru/?n={fd}&fn={fn}&fpd={fp}', 'event_loop': 'default', 'select4receipt_threshold': 5.0, 'graphite': {'addr': '127.0.0.1', 'port': 42000, 'timeout': 60}, 'environments': {'real_development': {'port': 8080, 'debug': False, 'ssh_keys': ['c:\\\\src\\\\whitespirit\\\\support\\\\starrus\\\\starrus_key'], 'logging': {'version': 1, 'disable_existing_loggers': False, 'formatters': {'default': {'format': '%(asctime)s: T%(threadName)s: %(levelname)s: %(name)s: %(message)s'}}, 'handlers': {'console': {'level': 'DEBUG', 'formatter': 'default', 'class': 'logging.StreamHandler'}}, 'root': {'handlers': ['console'], 'level': 'DEBUG', 'propagate': True}, 'loggers': {'proto.starrus': {'level': 'DEBUG', 'propogate': False}}}, 'devices': {'dev1': {'proto': 'starrus', 'net': '192.168.1.39/32', 'port': 3333, 'json_port': 4444}, 'dev2': {'proto': 'starrus', 'net': '192.168.1.33/32', 'port': 3333, 'json_port': 4444}}}, 'development': {'event_loop': 'uvloop', 'logging': {'version': 1, 'disable_existing_loggers': False, 'formatters': {'default': {'format': '%(asctime)s: P%(process)d: %(levelname)s: %(name)s: %(message)s'}}, 'handlers': {'log_file': {'level': 'DEBUG', 'formatter': 'default', 'encoding': 'utf8', 'class': 'logging.handlers.TimedRotatingFileHandler', 'filename': '/var/log/yandex/whitespirit/whitespirit.log', 'when': 'midnight', 'backupCount': 10, 'delay': False}}, 'root': {'handlers': ['log_file'], 'level': 'DEBUG', 'propagate': True}, 'loggers': {'proto.starrus': {'level': 'DEBUG', 'propogate': False}}}, 'debug': True, 'darkspirit_baseurl': 'https://greed-tm1f.yandex.ru:8616/', 'filter_sn': ['00000000381001942057', '00000000381002827554', '00000000381001543159'], 'devices': {'tst1': {'proto': 'starrus', 'net': '95.108.178.160/28', 'port': 3333, 'json_port': 4444}}}, 'testing': {'event_loop': 'uvloop', 'logging': {'version': 1, 'disable_existing_loggers': False, 'formatters': {'default': {'format': '%(asctime)s: P%(process)d: %(levelname)s: %(name)s: %(message)s'}}, 'handlers': {'log_file': {'level': 'DEBUG', 'formatter': 'default', 'encoding': 'utf8', 'class': 'logging.handlers.TimedRotatingFileHandler', 'filename': '/var/log/yandex/whitespirit/whitespirit.log', 'when': 'midnight', 'backupCount': 10, 'delay': False}}, 'root': {'handlers': ['log_file'], 'level': 'DEBUG', 'propagate': True}, 'loggers': {'proto.starrus': {'level': 'DEBUG', 'propogate': False}}}, 'debug': True, 'darkspirit_baseurl': 'https://greed-tm1f.yandex.ru:8616/', 'filter_sn': ['00000000381002827554', '00000000381004956051', '00000000381007528533'], 'devices': {'tst1': {'proto': 'starrus', 'net': '95.108.178.160/28', 'port': 3333, 'json_port': 4444}}}, 'production': {'event_loop': 'uvloop', 'render_url_template': 'https://check.yandex.ru/?n={fd}&fn={fn}&fpd={fp}', 'CHECK_CONN_TIMEOUT': 12, 'logging': {'version': 1, 'disable_existing_loggers': False, 'formatters': {'default': {'format': '%(asctime)s: P%(process)d: %(levelname)s: %(name)s: %(message)s'}}, 'handlers': {'log_file': {'level': 'DEBUG', 'formatter': 'default', 'encoding': 'utf8', 'class': 'logging.handlers.TimedRotatingFileHandler', 'filename': '/var/log/yandex/whitespirit/whitespirit.log', 'when': 'midnight', 'backupCount': 10, 'delay': False}}, 'root': {'handlers': ['log_file'], 'level': 'DEBUG', 'propagate': True}, 'loggers': {'proto.starrus': {'level': 'INFO', 'propogate': False}}}, 'debug': False, 'devices': {'kkt40': {'proto': 'starrus', 'net': '10.0.0.0/23', 'port': 3333, 'json_port': 4444}}}}})",
    "ENVIRONMENT": "development",
    "VERSION": "1.0.177",
    "localzone": "Europe/Moscow",
    "darkspirit": {
        "registered": True,
        "time": 10508532.144,
        "answer": {
            "url": "https://whitespirit-dev1f.balance.os.yandex.net:8080",
            "version": "1.0.177"
        },
        "url": "https://greed-tm1f.yandex.ru:8616/v1/whitespirits/"
    },
    "extern_baseurl": "https://whitespirit-dev1f.balance.os.yandex.net:8080/",
    "uploads": [],
    "event_loop_policy": "<uvloop.EventLoopPolicy object at 0x7f2d24623fd0>",
    "event_loop": "<uvloop.Loop running=True closed=False debug=True>",
    "devices": {
        "95.108.178.163:3333": {
            "sn": "00000000381001942057",
            "inn": "7736207543",
            "fn_sn": "9999078900005790",
            "proto": "starrus",
            "ws_ctx": None,
            "state": "OPEN_SHIFT",
            "fn_state": "FISCAL",
            "fn_status": [
                "good"
            ],
            "groups": [
                "GROUP0"
            ],
            "hidden": False,
            "logical_state": "OK",
            "lowlevel": {
                "model": "РП Система 1ФА",
                "state": {
                    "name": "OPEN_SHIFT",
                    "value": 2
                },
                "got_fatal_error": False,
                "fatal_reasons": [],
                "proto_type": "ipv4",
                "addr": "95.108.178.163",
                "last_server_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
                "last_device_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
                "software_version": "3.5",
                "software_build": 30,
                "fn_state": "FISCAL",
                "fn_status": "good",
                "monitoring_info": {
                    "cpu_temperature": 36,
                    "disk_free_space_pct": 10
                },
                "reg_result": {
                    "dt": "2017-09-20 23:17:00",
                    "inn": "7736207543  ",
                    "rnm": "3154346023006976    ",
                    "tax_mode": 1,
                    "work_mode": 44,
                    "document_number": 1,
                    "fp": 2474439727
                },
                "macaddr": None
            },
            "locked": False,
            "cur_key": "(0, False, 0, 0, datetime.datetime(2017, 9, 20, 23, 17), -359, '00000000381001942057', 139831775297776)",
            "last_operation_time": 10492796.811,
            "uptime_shift_time": 15750.581000000238,
            "uptime_number_receipts": 359,
            "all_documents_count": 359,
            "ofd_queue_info": {
                "queue_length": 0,
                "first_2transfer_document_number": 0,
                "first_2transfer_document_dt": None
            },
            "registration_info": empty_registration_info("00000000381001942057", "9999078900005790"),
            "ofd_info": {
                "ofd_addr": "test.kkt.ofd.yandex.net:12345",
                "check_url": "www.nalog.gov.ru",
                "transport": "0"
            },
            "fn_last_document_info": {
                "id": 359,
                "dt": "2017-10-08 12:20:00"
            },
            "last_operation_fail": False,
            "last_operation_fail_info": {
                "time": 10158315.011,
                "error": "StarrusDeviceException(57, Внутренняя ошибка устройство, fatal=False, cmd=PrintSavedDocuments())",
                "ctx": "WSCtx(CDO22JFJD7 1bb5a495-23ff-49e9-b2c6-388d2e32fd9f get_document at=2017-10-04 15:25:54.304001 task_type=handler, task_info={'handler_type': <HandlerType.get_document: 5>})"
            },
            "failed_operations": 3,
            "status_dt": "2017-10-08 16:43:03",
            "status_time": 10508543.481,
            "last_shift_info": {}
        },
        "95.108.178.164:3333": {
            "sn": CR_LONG_SN,
            "inn": "7736207543",
            "fn_sn": FS_SN,
            "proto": "starrus",
            "ws_ctx": None,
            "state": "OPEN_SHIFT",
            "fn_state": "FISCAL",
            "fn_status": [
                "ofd_timeouted"
            ],
            "groups": [
                "GROUP0"
            ],
            "hidden": False,
            "logical_state": "OK",
            "lowlevel": {
                "model": "РП Система 1ФА",
                "state": {
                    "name": "OPEN_SHIFT",
                    "value": 2
                },
                "got_fatal_error": False,
                "fatal_reasons": [],
                "proto_type": "ipv4",
                "addr": "95.108.178.164",
                "last_server_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
                "last_device_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
                "software_version": "3.5",
                "software_build": 30,
                "fn_state": "FISCAL",
                "fn_status": "ofd_timeouted",
                "monitoring_info": {
                    "cpu_temperature": 40,
                    "disk_free_space_pct": 45
                },
                "reg_result": {
                    "dt": "2017-09-15 15:24:00",
                    "inn": "7736207543  ",
                    "rnm": "1961475915045215    ",
                    "tax_mode": 1,
                    "work_mode": 44,
                    "document_number": 1,
                    "fp": 374697927
                },
                "macaddr": None
            },
            "locked": False,
            "cur_key": "(0, False, 0, 0, datetime.datetime(2017, 9, 15, 15, 24), -588, '00000000381002827554', 139831775298392)",
            "last_operation_time": 10475115.38,
            "uptime_shift_time": 33432.012000000104,
            "uptime_number_receipts": 588,
            "all_documents_count": 588,
            "ofd_queue_info": {
                "queue_length": 0,
                "first_2transfer_document_number": 0,
                "first_2transfer_document_dt": None
            },
            "registration_info": empty_registration_info("00000000381002827554", FS_SN),
            "ofd_info": {
                "ofd_addr": "test.kkt.ofd.yandex.net:12345",
                "check_url": "www.nalog.gov.ru",
                "transport": "0"
            },
            "fn_last_document_info": {
                "id": 588,
                "dt": "2017-10-08 07:25:00"
            },
            "last_operation_fail": False,
            "last_operation_fail_info": {
                "time": 10158234.359,
                "error": "StarrusDeviceException(57, Внутренняя ошибка устройство, fatal=False, cmd=PrintSavedDocuments())",
                "ctx": "WSCtx(C2DVVI3B3S d0eb546c-3b93-4a71-ba39-39c38f222916 get_document at=2017-10-04 15:24:33.880913 task_type=handler, task_info={'handler_type': <HandlerType.get_document: 5>})"
            },
            "failed_operations": 4,
            "status_dt": "2017-10-08 16:43:03",
            "status_time": 10508543.836,
            "last_shift_info": {}
        },
        "95.108.178.173:3333": {
            "sn": "00000000381001543159",
            "inn": "7736207543",
            "fn_sn": "9999078900005747",
            "proto": "starrus",
            "ws_ctx": None,
            "state": "OPEN_SHIFT",
            "fn_state": "FISCAL",
            "fn_status": [
                "good"
            ],
            "groups": [
                "GROUP0"
            ],
            "hidden": False,
            "logical_state": "OK",
            "lowlevel": {
                "model": "РП Система 1ФА",
                "state": {
                    "name": "OPEN_SHIFT",
                    "value": 2
                },
                "got_fatal_error": False,
                "fatal_reasons": [],
                "proto_type": "ipv4",
                "addr": "95.108.178.173",
                "last_server_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
                "last_device_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
                "software_version": "3.5",
                "software_build": 30,
                "fn_state": "FISCAL",
                "fn_status": "good",
                "monitoring_info": {
                    "cpu_temperature": 33,
                    "disk_free_space_pct": 10
                },
                "reg_result": {
                    "dt": "2017-09-15 15:28:00",
                    "inn": "7736207543  ",
                    "rnm": "5409642728061020    ",
                    "tax_mode": 1,
                    "work_mode": 44,
                    "document_number": 1,
                    "fp": 3143421231
                },
                "macaddr": None
            },
            "locked": False,
            "cur_key": "(0, False, 0, 0, datetime.datetime(2017, 9, 15, 15, 28), -373, '00000000381001543159', 139831775299792)",
            "last_operation_time": 10470313.054,
            "uptime_shift_time": 38234.33899999969,
            "uptime_number_receipts": 373,
            "all_documents_count": 373,
            "ofd_queue_info": {
                "queue_length": 0,
                "first_2transfer_document_number": 0,
                "first_2transfer_document_dt": None
            },
            "registration_info": empty_registration_info("00000000381001543159", "9999078900005747"),
            "ofd_info": {
                "ofd_addr": "test.kkt.ofd.yandex.net:12345",
                "check_url": "www.nalog.gov.ru",
                "transport": "0"
            },
            "fn_last_document_info": {
                "id": 373,
                "dt": "2017-10-08 06:05:00"
            },
            "last_operation_fail": False,
            "last_operation_fail_info": {
                "time": 10158331.434,
                "error": "StarrusDeviceException(57, Внутренняя ошибка устройство, fatal=False, cmd=PrintSavedDocuments())",
                "ctx": "WSCtx(CKGRCBSTJN 51a220ca-6968-4f3e-84a8-7f4e7b2c1610 get_document at=2017-10-04 15:26:10.966191 task_type=handler, task_info={'handler_type': <HandlerType.get_document: 5>})"
            },
            "failed_operations": 4,
            "status_dt": "2017-10-08 16:43:03",
            "status_time": 10508543.404,
            "last_shift_info": {}
        }
    },
    "queues_info": {
        "in_use": {
            "": {
                "length": 10,
                "states": {
                    "OFFLINE": 10,
                    "NOT_MINE": 0
                }
            },
            "7736207543": {
                "length": 0,
                "states": {
                    "OPEN_SHIFT": 0,
                    "CLOSE_SHIFT": 0,
                    "OPEN_DOCUMENT": 0,
                    "OFFLINE": 0
                }
            }
        },
        "queues": {
            "": {
                "length": 3,
                "ready": 0,
                "queue": {
                    "95.108.178.162:3333": {
                        "sn": "00000000381002827554",
                        "state": "NOT_MINE",
                        "last_use": 9990685.608,
                        "cur_key": "(999, False, 0, 0, datetime.datetime(2017, 10, 2, 16, 52, 5, 236716), 0, '00000000381002827554', 139831775297888)"
                    },
                    "95.108.178.166:3333": {
                        "sn": "00000000381004956051",
                        "state": "NOT_MINE",
                        "last_use": 9990685.609,
                        "cur_key": "(999, False, 0, 0, datetime.datetime(2017, 10, 2, 16, 52, 5, 236789), 0, '00000000381004956051', 139831775299064)"
                    },
                    "95.108.178.170:3333": {
                        "sn": "00000000381007528533",
                        "state": "NOT_MINE",
                        "last_use": 9990685.609,
                        "cur_key": "(999, False, 0, 0, datetime.datetime(2017, 10, 2, 16, 52, 5, 236846), 0, '00000000381007528533', 139831775298616)"
                    }
                },
                "states": {
                    "OFFLINE": 0,
                    "NOT_MINE": 3
                }
            },
            "7736207543": {
                "length": 3,
                "ready": 3,
                "queue": {
                    "95.108.178.164:3333": {
                        "sn": "00000000381002827554",
                        "state": "CLOSE_SHIFT",
                        "last_use": 10475115.38,
                        "cur_key": "(0, False, 0, 0, datetime.datetime(2017, 9, 15, 15, 24), -588, '00000000381002827554', 139831775298392)"
                    },
                    "95.108.178.173:3333": {
                        "sn": "00000000381001543159",
                        "state": "OPEN_SHIFT",
                        "last_use": 10470313.054,
                        "cur_key": "(0, False, 0, 0, datetime.datetime(2017, 9, 15, 15, 28), -373, '00000000381001543159', 139831775299792)"
                    },
                    "95.108.178.163:3333": {
                        "sn": "00000000381001942057",
                        "state": "OPEN_SHIFT",
                        "last_use": 10492796.811,
                        "cur_key": "(0, False, 0, 0, datetime.datetime(2017, 9, 20, 23, 17), -359, '00000000381001942057', 139831775297776)"
                    }
                },
                "states": {
                    "OPEN_SHIFT": 3,
                    "CLOSE_SHIFT": 0,
                    "OPEN_DOCUMENT": 0,
                    "OFFLINE": 0
                }
            }
        }
    },
    "used_info": {
        "": [
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))",
            "HLDevice(StarrusDevice, None, DeviceHLState.OFFLINE(None))"
        ],
        "7736207543": []
    }
}

CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD = {
    "sn": CR_LONG_SN,
    "inn": "7704340310",
    "fn_sn": FS_SN,
    "proto": "starrus",
    "ws_ctx": "WSCtx(CAFTNXOPUY 0166dbb9-f4c0-4641-a526-6a5be15713d6 device_status at=2018-03-27 00:32:46.810893 task_type=handler, task_info={'handler_type': <HandlerType.device_status: 12>})",
    "state": "NONCONFIGURED",
    "fn_state": "READY_FISCAL",
    "fn_status": [
        "good"
    ],
    "groups": [
        "_NOGROUP"
    ],
    "hidden": False,
    "logical_state": "NEW",
    "lowlevel": {
        "model": "РП Система 1ФА",
        "state": {
            "name": "AFTER_START",
            "value": 0x0
        },
        "got_fatal_error": False,
        "fatal_reasons": [],
        "proto_type": "ipv4",
        "addr": "10.0.0.45",
        "last_server_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
        "last_device_dt": NOW.strftime('%Y-%m-%d %H:%M:%S'),
        "software_version": "3.5",
        "software_build": 30,
        "fn_state": "READY_FISCAL",
        "fn_status": "good",
        "monitoring_info": {
            "cpu_temperature": 48,
            "disk_free_space_pct": 44
        },
        "prev_registration_info": None,
        "reg_result": {
            "dt": "2018-03-20 18:11:18"
        },
        "macaddr": "16:c7:85:0d:45:ef"
    },
    "locked": True,
    "cur_key": "(6, False, 0, 0, datetime.datetime(2018, 3, 20, 18, 11, 18, 439162), 0, '00000000381008017841', 140215659041680)",
    "last_operation_time": 39.347,
    "uptime_shift_time": 541289.212,
    "uptime_number_receipts": 0,
    "all_documents_count": None,
    "ofd_queue_info": {},
    "registration_info": empty_registration_info(CR_LONG_SN, FS_SN, work_mode=as_str_list(DEFAULT_WORKMODE)),
    "ofd_info": {
        "ofd_addr": "test.kkt.ofd.yandex.net:12345",
        "check_url": "www.nalog.gov.ru",
        "transport": "0"
    },
    "fn_last_document_info": {},
    "last_operation_fail": False,
    "last_operation_fail_info": {
        "time": 39.347,
        "error": None,
        "ctx": None
    },
    "failed_operations": 0,
    "status_dt": "2018-03-27 00:32:47",
    "status_time": 541328.558,
    "last_shift_info": {}
}

CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_BSO_GOOD = deepcopy(
    CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_BSO_GOOD["registration_info"]["work_mode"] = as_str_list(BSO_WORKMODE)
CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_BSO_GOOD["groups"] = ["BSO"]

CASHMACHINES_STATUS_CR_OFFLINE = deepcopy(
    CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_OFFLINE.update(
    {"state": "OFFLINE"}
)

CASHMACHINES_STATUS_CR_OPEN_SHIFT_FS_FISCAL_GOOD = deepcopy(
    CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_OPEN_SHIFT_FS_FISCAL_GOOD.update(
    {"state": "OPEN_SHIFT", "fn_state": "FISCAL"}
)

CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD = deepcopy(
    CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD.update(
    {"state": "CLOSE_SHIFT", "fn_state": "FISCAL"}
)

CASHMACHINES_STATUS_CR_POSTFISCAL_FS_ARCHIVE_READING = deepcopy(
    CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_POSTFISCAL_FS_ARCHIVE_READING.update(
    {"state": "POSTFISCAL", "fn_state": "ARCHIVE_READING"}
)


CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD = deepcopy(
    CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD.update(
    {"state": "OVERDUE_OPEN_SHIFT"}
)

CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_URGENT_REPLACEMENT = deepcopy(
    CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_URGENT_REPLACEMENT.update({
    "fn_status": ["urgent_replacement"]
})

# after a fs replacement
CASHMACHINES_STATUS_CR_FATAL_ERROR_SECOND_FS_FISCAL_GOOD = deepcopy(
    CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_FATAL_ERROR_SECOND_FS_FISCAL_GOOD.update({
    "state": "FATAL_ERROR",
    "fn_sn": SECOND_FS_SN,
    "registration_info": {}
})
CASHMACHINES_STATUS_CR_FATAL_ERROR_SECOND_FS_FISCAL_GOOD["lowlevel"]["prev_registration_info"] = {
    "inn": "7704340310",
    "rnm": "5409642728061020",
    "fn_sn": FS_SN,
}
CASHMACHINES_STATUS_CR_FATAL_ERROR_SECOND_FS_FISCAL_GOOD["lowlevel"]["fatal_reasons"].append(u"ФН не прошёл проверку")

CASHMACHINES_STATUS_CR_NONCONFIGURED_SECOND_FS_READY_FISCAL_GOOD = deepcopy(
    CASHMACHINES_STATUS_CR_FATAL_ERROR_SECOND_FS_FISCAL_GOOD
)
CASHMACHINES_STATUS_CR_NONCONFIGURED_SECOND_FS_READY_FISCAL_GOOD.update({
    "state": "NONCONFIGURED",
})
CASHMACHINES_STATUS_CR_NONCONFIGURED_SECOND_FS_READY_FISCAL_GOOD["registration_info"] = \
    deepcopy(CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD["registration_info"])
CASHMACHINES_STATUS_CR_NONCONFIGURED_SECOND_FS_READY_FISCAL_GOOD["registration_info"].update({
    "fn_sn": SECOND_FS_SN,
})

SHIFT_CLOSE_REPORT = {
    "id": 593,
    "document_type": "ShiftCloseReport",
    "dt": "2017-10-09 11:57:00",
    "fp": 3085713379,
    "ofd_ticket_received": False,
    "shift_number": 48
}

REG_START_DT = datetime.datetime(2017, 12, 13, 17, 50, 00)   # 2017-12-13 17:50:00
DEFAULT_REGISTRATION_REPORT = {
    "id": 1,
    "document_type": "RegistrationReport",
    "dt": REG_START_DT.strftime('%Y-%m-%d %H:%M:%S'),
    "fp": 2689857838,
    "ofd_ticket_received": True,
    "inn": "7704340310",
    "rnm": "0001709626002105",
    "tax_mode": [
        "OSN"
    ],
    "work_mode": as_str_list(DEFAULT_WORKMODE),
}

BSO_REGISTRATION_REPORT = deepcopy(DEFAULT_REGISTRATION_REPORT)
BSO_REGISTRATION_REPORT['work_mode'] = as_str_list(BSO_WORKMODE)

CLOSE_FN_REPORT = {
  "id": 249842,
  "document_type": "CloseFNReport",
  "dt": "2018-03-13 17:50:00",
  "fp": 2536840990,
  "ofd_ticket_received": True,
  "inn": "7704340310",
  "rnm": "0001709626002105",
  "fullform": None
}

DEFAULT_REREGISTRATION_REPORT = {
  "id": 1,
  "document_type": "ReRegistrationReport",
  "dt": "2017-12-13 17:50:00",
  "fp": 2689857838,
  "ofd_ticket_received": True,
  "inn": "7704340310",
  "rnm": "0001709626002105",
  "tax_mode": [
    "OSN"
  ],
  "work_mode": as_str_list(DEFAULT_WORKMODE),
}

BSO_REREGISTRATION_REPORT = deepcopy(DEFAULT_REREGISTRATION_REPORT)
BSO_REREGISTRATION_REPORT['work_mode'] = as_str_list(BSO_WORKMODE)

CASH_SPACE_INFO_WITH_FR_PATCH = {
  "spaces": [
    {
      "filesystem": "/dev/dm-1",
      "total": 1234345984,
      "available": 241172480,
      "mount_point": "/"
    },
    {
      "filesystem": "udev",
      "total": 10485760,
      "available": 10485760,
      "mount_point": "/dev"
    },
    {
      "filesystem": "/dev/mapper/vg-data",
      "total": 2666893312,
      "available": 2100809728,
      "mount_point": "/FR"
    },
    {
      "filesystem": "tmpfs",
      "total": 101240000,
      "available": 88428000,
      "mount_point": "/run"
    },
    {
      "filesystem": "tmpfs",
      "total": 253096000,
      "available": 253096000,
      "mount_point": "/dev/shm"
    },
    {
      "filesystem": "tmpfs",
      "total": 5120000,
      "available": 5120000,
      "mount_point": "/run/lock"
    },
    {
      "filesystem": "tmpfs",
      "total": 253096000,
      "available": 253096000,
      "mount_point": "/sys/fs/cgroup"
    },
    {
      "filesystem": "tmpfs",
      "total": 253096000,
      "available": 253096000,
      "mount_point": "/tmp"
    },
    {
      "filesystem": "tmpfs",
      "total": 50620000,
      "available": 50620000,
      "mount_point": "/run/user/0"
    },
  ]
}

CASH_SPACE_INFO_WITHOUT_FR_PATCH = {
    'spaces': [info for info in CASH_SPACE_INFO_WITH_FR_PATCH['spaces'] if info['mount_point'] != '/FR']
}

WS_UPLOADS = [
    {
        "name": "firmware.bin",
        "checksum": "bc6d05774cb20c9cc2557e8f998fb05a"
    },
    {
        "name": "patch.bin",
        "checksum": "26d08f4b33bff470c5ade5bd32c10bff"
    }
]

WS_POST_UPLOAD_FILE_NAME = 'firmware.bin'
WS_POST_UPLOAD_FILE_CONTENT = 'some content'
WS_POST_UPLOAD = {
    'filname': WS_POST_UPLOAD_FILE_NAME,
    'size': len(WS_POST_UPLOAD_FILE_CONTENT)
}
