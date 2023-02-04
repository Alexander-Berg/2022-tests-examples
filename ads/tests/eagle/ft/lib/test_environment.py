# -*- coding: utf-8 -*-

import base64
import uuid
import itertools
import json
import struct
from urllib.parse import urlencode
from enum import Enum
from http.server import BaseHTTPRequestHandler, HTTPServer
from contextlib import contextmanager

import library.python.codecs
import requests
import threading
from ads.bsyeti.libs import py_testlib
from ads.bsyeti.eagle.collect.proto import query_params_pb2
from search.idl.meta_pb2 import TReport


def patch_dict(target, additional):
    for key, value in additional.items():
        if key in target:
            if isinstance(value, (bool, int, float, str, tuple)):
                target[key] = value
            elif isinstance(target[key], type(value)):
                if isinstance(target[key], list) and isinstance(value, list):
                    target[key].extend(value)
                elif isinstance(target[key], dict) and isinstance(value, dict):
                    patch_dict(target[key], value)
                else:
                    raise Exception("exp_json: Unknown merge of type %s" % type(value))
            else:
                raise Exception("exp_json: Unknown merge of types %s and %s" % (type(target[key]), type(value)))
        else:
            target[key] = value


class GlueType(Enum):
    NO_GLUE = 0
    VULTURE_CRYPTA = 2


class EagleResponse:
    def __init__(self, answer=None, access_log=None):
        self.answer = answer
        self.access_log = None


class TestEnvironment:
    def __init__(self, eagle_port, yt_tables, apphost_port=81, kv_saas_port=None, market_dj_port=None, start=100000000):
        self.eagle_port = eagle_port
        self.profiles = yt_tables["profiles"]
        self.vulture = yt_tables["vulture"]
        self.cookies = yt_tables["cookies"]
        self.user_shows = yt_tables["user_shows"]
        self.search_profiles = yt_tables["search_profiles"]
        self.uid_generator = itertools.count(start=start)
        self.apphost_port = apphost_port
        self.kv_saas_port = kv_saas_port
        self.market_dj_port = market_dj_port

    def new_uid(self):
        return next(self.uid_generator)

    def new_uuid(self):
        return uuid.UUID(int=0xFFEEDDCCBBAAFFEEDDCCBBAA + next(self.uid_generator))

    def sensors(self):
        return requests.get("http://localhost:{port}/sensors".format(port=self.eagle_port))

    @staticmethod
    def make_eagle_request(
        client,
        ids,
        test_time,
        glue_type,
        keywords,
        counters,
        exp_json,
        resp_format,
        domain_ids,
        compression=None,
        compression_type=None,
        skip_tsar_calculation=False,
        no_keywords=None,
    ):
        if exp_json is None:
            exp_json = {}

        possible_resp_format = {
            "legacy-json",
            "protobuf",
            "protobuf-json",
        }  # TODO: take formats from eagle/collect

        if resp_format not in possible_resp_format:
            raise Exception(f"We do not support format {resp_format}, possible formats {possible_resp_format}")
        if keywords and not isinstance(keywords, list):
            keywords = [keywords]
        if counters and not isinstance(counters, list):
            counters = [counters]
        keywords = ",".join(str(k) for k in keywords) if keywords else None
        counters = ",".join(str(c) for c in counters) if counters else None
        no_keywords = ",".join(str(k) for k in no_keywords) if no_keywords else None
        experiment_parameters = {
            "EagleSettings": {
                "LoadSettings": {
                    "UseVultureCrypta": True if glue_type == GlueType.VULTURE_CRYPTA else False,
                }
            }
        }
        patch_dict(experiment_parameters, exp_json)

        eagle_request = {
            "client": client,
            "strict": 1,
            "seed": "100",
            "time": test_time,
            "glue": 0 if glue_type == GlueType.NO_GLUE else 1,
            "timeout": "10000",
            "exp-json": json.dumps(experiment_parameters),
            "format": resp_format,
            "allowed-domain-md5s": domain_ids,
            "skip-tsar-calculation": skip_tsar_calculation,
        }
        if compression is not None:
            eagle_request["compression"] = compression
        if compression_type is not None:
            eagle_request["compression-type"] = compression_type
        if keywords:
            eagle_request["keywords"] = keywords
        if counters:
            eagle_request["counters"] = counters
        if no_keywords:
            eagle_request["no-keywords"] = no_keywords
        eagle_request.update(ids)

        return eagle_request

    def make_eagle_proto_request(self, *args, **kwargs):
        request = self.make_eagle_request(*args, **kwargs)
        proto_request = query_params_pb2.TQueryParams()
        client = proto_request.Clients.add()
        client.Name = request.pop("client")
        client.Format = request.pop("format")

        for fd in proto_request.DESCRIPTOR.fields:
            extensions = fd.GetOptions().Extensions
            v = request.get(extensions[query_params_pb2.Option])
            if v is not None:
                setattr(proto_request, fd.name, type(fd.default_value)(v))
        return proto_request

    def raw_request(
        self,
        client,
        ids,
        test_time,
        glue_type=GlueType.NO_GLUE,
        keywords=None,
        counters=None,
        exp_json=None,
        resp_format="protobuf",
        domain_ids="",
        tvm_service_ticket=None,
        compression=None,
        compression_type=None,
        no_keywords=None,
    ):
        eagle_request = self.make_eagle_request(
            client=client,
            ids=ids,
            test_time=test_time,
            glue_type=glue_type,
            keywords=keywords,
            counters=counters,
            exp_json=exp_json,
            resp_format=resp_format,
            domain_ids=domain_ids,
            compression=compression,
            compression_type=compression_type,
            no_keywords=no_keywords,
        )

        request = ("http://localhost:{port}?{qs}").format(port=self.eagle_port, qs=urlencode(eagle_request))
        headers = {"X-Ya-Service-Ticket": tvm_service_ticket} if tvm_service_ticket else None
        return requests.get(request, headers=headers)

    def request(
        self,
        client,
        ids,
        test_time,
        glue_type=GlueType.NO_GLUE,
        keywords=None,
        counters=None,
        exp_json=None,
        resp_format="protobuf",
        domain_ids="",
        tvm_service_ticket=None,
        compression=None,
        compression_type=None,
        no_keywords=None,
    ):
        response = self.raw_request(
            client,
            ids,
            test_time,
            glue_type,
            keywords,
            counters,
            exp_json=exp_json,
            resp_format=resp_format,
            domain_ids=domain_ids,
            tvm_service_ticket=tvm_service_ticket,
            compression=compression,
            compression_type=compression_type,
            no_keywords=no_keywords,
        )
        assert response, "empty response"
        assert response.ok, response.ok
        return EagleResponse(answer=response.content)

    @staticmethod
    def prepare_kv_saas_response(data={}, keys={}):
        meta_resp = TReport()
        grouping = meta_resp.Grouping.add()
        for key, value in data.items():
            group = grouping.Group.add()
            doc = group.Document.add()
            doc.Url = bytes(keys.get(key, key), "utf-8")
            gta = doc.ArchiveInfo.GtaRelatedAttribute.add()
            gta.Key = b"_body"
            gta.Value = value
        return meta_resp.SerializeToString()

    @contextmanager
    def kv_saas_server(self, data={}, use_incorrect_response=False, send_empty_response=False, keys={}):
        empty_response = self.prepare_kv_saas_response()
        if use_incorrect_response:
            response = b"SomeIncorrectResponse"
        else:
            response = self.prepare_kv_saas_response(data, keys)

        class KVSaasHttpHandler(BaseHTTPRequestHandler):
            def do_GET(self):

                all_ids_matched = len(data) != 0
                for key in data:
                    text_id = "text={key}".format(key=key)
                    if text_id not in self.requestline:
                        all_ids_matched = False
                        break

                if all_ids_matched:
                    self.send_response(200)
                    self.send_header("Content-Length", str(len(response)))
                    self.send_header("Content-Type", "application/x-binary")
                    self.end_headers()
                    self.wfile.write(response)
                elif send_empty_response:
                    self.send_response(200)
                    self.send_header("Content-Length", str(len(empty_response)))
                    self.send_header("Content-Type", "application/x-binary")
                    self.end_headers()
                    self.wfile.write(empty_response)
                else:
                    self.send_response(503)
                    self.wfile.write(b"Oops!")

        try:
            server = HTTPServer(("localhost", self.kv_saas_port), KVSaasHttpHandler)
            server_thread = threading.Thread(target=server.serve_forever)
            server_thread.setDaemon(True)
            server_thread.start()
            yield
        finally:
            server.shutdown()
            server.server_close()
            server_thread.join()

    @contextmanager
    def market_dj_server(self, request, response):
        class MarketDJHttpHandler(BaseHTTPRequestHandler):
            def do_GET(self):
                if request in self.requestline:
                    self.send_response(200)
                    self.send_header("Content-Length", str(len(response)))
                    self.send_header("Content-Type", "application/x-binary")
                    self.end_headers()
                    self.wfile.write(response)
                else:
                    self.send_response(503)
                    self.wfile.write(b"Oops!")

        try:
            server = HTTPServer(("localhost", self.market_dj_port), MarketDJHttpHandler)
            server_thread = threading.Thread(target=server.serve_forever)
            server_thread.setDaemon(True)
            server_thread.start()
            yield
        finally:
            server.shutdown()
            server.server_close()
            server_thread.join()

    def apphost_request(
        self,
        client,
        ids,
        test_time,
        glue_type=GlueType.NO_GLUE,
        keywords=None,
        counters=None,
        exp_json=None,
        resp_format="protobuf",
        domain_ids="",
        skip_tsar_calculation=False,
    ) -> EagleResponse:
        eagle_request = self.make_eagle_proto_request(
            client=client,
            ids=ids,
            test_time=test_time,
            glue_type=glue_type,
            keywords=keywords,
            counters=counters,
            exp_json=exp_json,
            resp_format=resp_format,
            domain_ids=domain_ids,
            skip_tsar_calculation=skip_tsar_calculation,
        )

        apphost_request = {
            "type": "bigb_params_proto",
            "binary": base64.b64encode(eagle_request.SerializeToString()).decode(),
            "__content_type": "protobuf",
        }
        apphost_req = {
            "answers": [
                {"name": "APPHOST_PARAMS", "results": [{"type": "app_host_params"}]},
                {"name": "BIGB_SETUP", "results": [apphost_request]},
            ]
        }

        apphost_context = py_testlib.apphost_convert_from_json(json.dumps(apphost_req), "apphost_request")
        response = requests.post("http://localhost:{}/bigb".format(self.apphost_port), data=apphost_context)
        assert response, "empty response"
        assert response.ok, response.ok
        response_context = py_testlib.apphost_convert_to_json(response.content, "apphost_response", False)
        parsed_response = json.loads(response_context)
        assert 2 == len(parsed_response), "expected two records in apphost response, got %s" % response_context
        profile_id_response = parsed_response[0]["results"][0]
        assert "bigb_response_profile_id" == profile_id_response["type"], (
            "expect bigb_response_profile_id item from apphost request, got %s" % profile_id_response["type"]
        )
        assert "y{id}".format(id=ids["bigb-uid"]) == profile_id_response["data"], (
            "bigb_response_profile_id item data should be equal to requested id, got %s" % profile_id_response["data"]
        )
        try:
            if "profile" in parsed_response[1]["results"][0]:
                return EagleResponse(answer=parsed_response[1]["results"][0]["profile"])
            elif "data" in parsed_response[1]["results"][0]:
                return EagleResponse(answer=json.loads(parsed_response[1]["results"][0]["data"]))
            elif "binary" in parsed_response[1]["results"][0]:
                eagle_response = base64.b64decode(parsed_response[1]["results"][0]["binary"])
                return EagleResponse(answer=eagle_response)
            else:
                raise Exception("EagleResponse has unknown format")
        except Exception as e:
            raise Exception("%s: %s", response_context, e)


def decompress_answer(answer, compression, compression_type):
    if compression is None:
        return answer
    if compression_type == "none":
        return library.python.codecs.loads(compression, answer)
    if compression_type == "stream":
        answer = bytes(answer)
        result = b""
        # trivial implementation of NBlockCodecs::TDecodedInput
        while len(answer) > 0:
            # H - 2 bytes, Q - 8 bytes, 2+8 = 10
            c, l = struct.unpack("=HQ", answer[:10])
            answer = answer[10:]
            result += library.python.codecs.loads(compression, answer[:l])
            answer = answer[l:]

        return result
    raise Exception("Unknown compression type %s" % compression_type)


def parse_profile(data):
    if isinstance(data, py_testlib.TBigbPublicProto):
        return data
    else:
        return py_testlib.TBigbPublicProto(data)


def check_answer(expected, actual, ignored_fields=None):
    expected = parse_profile(expected)
    actual = parse_profile(actual)
    diff = expected.is_equal(
        actual, ignored_fields
    )  # example of ignored_fields: ["queries.predicted_cpc", "source_uniqs", "tsar_vectors"]
    assert diff is None, diff
