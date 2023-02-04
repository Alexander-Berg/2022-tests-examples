# coding: utf-8


def ok(owned=False):
    """Возвращает фейковую процедуру, которая эмулирует ответ Вебмастера.

    Если owned=True, то эмулируется ситуация, как будто домен в вебмастере уже
    был подтверждён.
    """
    def fake_info(domain_name, uid, ignore_errors=None):
        return {
            "application": "webmaster3-internal",
            "version": "1.0.57@2017-10-17T15:40:44",
            "startDate": "2017-12-04T15:36:18.613Z",
            "action": "/user/host/verification/add",
            "status": "SUCCESS",
            "duration": 40,
            "request": {
                "internalClient": {
                    "clientId": 61,
                    "grants": [
                        "SEARCH_BASE",
                        "USER_HOST",
                        "HOST_VERIFICATION"
                    ],
                    "name": "All grants for testing"
                },
                "userId": uid,
                "webmasterUser": {
                    "userId": uid
                },
                "hostName": "foo-bar.ru"
            },
            "data": {
                "hostId": "http:%s:80" % domain_name,
                "verificationStatus": "VERIFIED" if owned else "NOT_VERIFIED"
            },
            "trace": {
                "timeMs": 40,
                "queryTimeMs": 16,
                "cassRead": 6,
                "cassReadMs": 9,
                "cassWrite": 1,
                "cassWriteMs": 7,
                "cassBatch": 1,
                "batchMs": 7,
                "chread": 0,
                "chreadMs": 0,
                "chwrite": 0,
                "chwriteMs": 0,
                "cassQs": [],
                "chQs": []
            }
        }

    return fake_info


def bad_tvm_ticket():
    def fake_add(domain, admin):
        return {
            "startDate": "2018-01-16T19:09:56.387Z",
            "data": None,
            "action": "/user/host/verification/add",
            "trace": {
                "timeMs": 1,
                "chwrite": 0,
                "cassRead": 0,
                "cassWriteMs": 0,
                "chwriteMs": 0,
                "cassReadMs": 0,
                "batchMs": 0,
                "chread": 0,
                "cassWrite": 0,
                "cassBatch": 0,
                "queryTimeMs": 0,
                "cassQs": [],
                "chQs": [],
                "chreadMs": 0
            },
            "application": "webmaster3-internal",
            "errors": [
                {
                    "stackTrace": None,
                    "subsystem": "REQUEST",
                    "class": None,
                    "code": "SECURITY_INVALID_TIKET",
                    "message": "TVM1 ticket validation failed"
                }
            ],
            "request": {
                "webmasterUser": None,
                "hostName": "lisp.website",
                "userId": 13558447,
                "internalClient": None
            },
            "status": "SUCCESS",
            "duration": 1,
            "version": "1.0.63@2017-12-22T17:43:28"
        }
    return fake_add


def applicable():
    def fake_applicable(domain, admin_uid):
        return {
          "application": "webmaster3-internal",
          "version": "1.0.63@2017-12-22T17:43:28",
          "startDate": "2018-01-17T09:29:13.918Z",
          "action": "/user/host/verification/listApplicable",
          "status": "SUCCESS",
          "duration": 30,
          "request": {
            "internalClient": {
              "clientId": 61,
              "grants": [
                "SEARCH_BASE",
                "USER_HOST",
                "HOST_VERIFICATION",
                "METRIKA"
              ],
              "name": "All grants for testing"
            },
            "userId": 13558447,
            "webmasterUser": {
              "userId": 13558447
            },
            "hostIdString": "http:lisp.website:80",
            "hostId": "http:lisp.website:80"
          },
          "data": {
            "applicableVerifications": [
              "META_TAG",
              "HTML_FILE",
              "DNS",
              "WHOIS",
              "DNS_DELEGATION",
            ]
          },
          "trace": {
            "timeMs": 30,
            "queryTimeMs": 28,
            "cassRead": 3,
            "cassReadMs": 28,
            "cassWrite": 0,
            "cassWriteMs": 0,
            "cassBatch": 0,
            "batchMs": 0,
            "chread": 0,
            "chreadMs": 0,
            "chwrite": 0,
            "chwriteMs": 0,
            "cassQs": [],
            "chQs": []
          }
        }

    return fake_applicable


def info(uin):
    """Возвращает фейковую процедуру, которая эмулирует ответ ручки info Вебмастера.
    """
    def fake_info(domain_name, uid, ignore_errors=None):
        host_id = 'http:{0}:80'.format(domain_name)
        return {
          "application": "webmaster3-internal",
          "version": "1.0.63@2017-12-22T17:43:28",
          "startDate": "2018-01-18T08:16:57.482Z",
          "action": "/user/host/verification/info",
          "status": "SUCCESS",
          "duration": 67,
          "request": {
            "internalClient": {
                "clientId": 61,
                "grants": [
                    "SEARCH_BASE",
                    "USER_HOST",
                    "HOST_VERIFICATION",
                    "METRIKA"
                ],
                "name": "All grants for testing",
                "domainVerificationState": {
                    "preferredHost": host_id,
                },
            },
            "userId": uid,
            "webmasterUser": {
                "userId": uid,
            },
            "hostIdString": host_id,
            "hostId": host_id,
          },
          "data": {
            "verificationUin": str(uin),
          },
          "trace": {
            "timeMs": 67,
            "queryTimeMs": 62,
            "cassRead": 4,
            "cassReadMs": 62,
            "cassWrite": 0,
            "cassWriteMs": 0,
            "cassBatch": 0,
            "batchMs": 0,
            "chread": 0,
            "chreadMs": 0,
            "chwrite": 0,
            "chwriteMs": 0,
            "cassQs": [],
            "chQs": []
          }
        }

    return fake_info


def verification_failed():
    """Эмулируем ответ вебмастера после того, как был начат процесс подтверждения.
    Тут важно то, что вебмастер отдаёт тип подтверждения, который выбрал пользователь,
    и дату последней проверки.

    В данном случае, после нескольких попыток подтвердить через DNS не удалось, и
    нужно чтобы пользователь снова нажал кнопку.
    """
    def fake_info(domain_name, uid, ignore_errors=None):
        return {
          "application": "webmaster3-internal",
          "version": "1.0.64@2018-02-14T17:36:30",
          "startDate": "2018-03-05T06:50:36.508Z",
          "action": "/user/host/verification/info",
          "status": "SUCCESS",
          "duration": 97,
          "request": {
            "internalClient": {
              "clientId": 61,
              "grants": {
                "SEARCH_BASE": {
                  "allowOffline": True
                },
                "USER_HOST": {
                  "allowOffline": True
                },
                "HOST_VERIFICATION": {
                  "allowOffline": True
                },
                "METRIKA": {
                  "allowOffline": True
                }
              },
              "name": "All grants for testing"
            },
            "grantOptions": {
              "allowOffline": True
            },
            "userId": 13558447,
            "webmasterUser": {
              "userId": 13558447
            },
            "hostIdString": "http:lisp.website:80",
            "hostId": "http:lisp.website:80"
          },
          "data": {
            "verificationStatus": "VERIFICATION_FAILED",
            "verificationType": "DNS",
            "verificationUin": "e3e31b8a92197cc1",
            "lastVerificationAttemptTime": "2018-03-02T13:40:10.167Z",
            "lastVerificationFailInfo": {
              "foundTxtRecords": [],
              "foundYandexVerificationRecords": [
                {
                  "name": "lisp.website.",
                  "type": "TXT",
                  "dclass": "IN",
                  "ttl": 86379,
                  "data": "\"yandex-verification: 3f07a871c00b33b0\""
                }
              ],
              "hostFoundInDNS": True,
              "type": "DNS_RECORD_NOT_FOUND",
              "reason": "SOME_ERROR"  # на самом деле для такого типа проверки этого поля нет, но для теста добавим будто оно есть
            }
          },
          "trace": {
            "chQs": [],
            "timeMs": 97,
            "queryTimeMs": 92,
            "cassRead": 4,
            "cassReadMs": 92,
            "cassWrite": 0,
            "cassWriteMs": 0,
            "cassBatch": 0,
            "batchMs": 0,
            "chread": 0,
            "chreadMs": 0,
            "chwrite": 0,
            "chwriteMs": 0,
            "cassQs": []
          }
        }
    return fake_info


def verification_in_progress():
    """Эмулируем ответ вебмастера после того, как был начат процесс подтверждения.
    Тут важно то, что вебмастер отдаёт тип подтверждения, который выбрал пользователь,
    и дату последней проверки.

    В данном случае проверка владения в процессе выполнения
    """
    def fake_info(domain_name, uid, ignore_errors=None):
        return {
          "application": "webmaster3-internal",
          "version": "1.0.64@2018-02-14T17:36:30",
          "startDate": "2018-03-05T06:50:36.508Z",
          "action": "/user/host/verification/info",
          "status": "SUCCESS",
          "duration": 97,
          "request": {
            "internalClient": {
              "clientId": 61,
              "grants": {
                "SEARCH_BASE": {
                  "allowOffline": True
                },
                "USER_HOST": {
                  "allowOffline": True
                },
                "HOST_VERIFICATION": {
                  "allowOffline": True
                },
                "METRIKA": {
                  "allowOffline": True
                }
              },
              "name": "All grants for testing"
            },
            "grantOptions": {
              "allowOffline": True
            },
            "userId": 13558447,
            "webmasterUser": {
              "userId": 13558447
            },
            "hostIdString": "http:lisp.website:80",
            "hostId": "http:lisp.website:80"
          },
          "data": {
            "verificationStatus": "IN_PROGRESS",
            "verificationType": "DNS",
            "verificationUin": "e3e31b8a92197cc1",
            "lastVerificationAttemptTime": "2018-03-02T13:40:10.167Z",
          },
          "trace": {
            "chQs": [],
            "timeMs": 97,
            "queryTimeMs": 92,
            "cassRead": 4,
            "cassReadMs": 92,
            "cassWrite": 0,
            "cassWriteMs": 0,
            "cassBatch": 0,
            "batchMs": 0,
            "chread": 0,
            "chreadMs": 0,
            "chwrite": 0,
            "chwriteMs": 0,
            "cassQs": []
          }
        }
    return fake_info
