import contextlib
import flask
from werkzeug.wrappers import Response

from maps.b2bgeo.test_lib.http_server import mock_http_server
from maps.doc.proto.analyzer.gpssignal_pb2 import GpsSignalCollectionData
from yandex.maps.proto.matcher.matched_path_pb2 import MatchedPath


@contextlib.contextmanager
def mock_graph_matcher():
    def _handler(environ, start_response):
        request = flask.Request(environ)

        if request.path == '/collect_batch_of_gpssignal_sync/':
            if request.method == 'GET':
                message = """<?xml version="1.0" encoding="UTF-8"?>' \
                <error><status>405</status><message>GET method is not allowed for /collect_batch_of_gpssignal_sync/</message></error>"""
                headers = {'Content-Type': 'text/xml', 'Allow': 'POST'}
                return Response(message, status=405, headers=headers)(environ, start_response)

            if request.method == 'POST':
                signals = GpsSignalCollectionData()
                signals.ParseFromString(request.data)

                matched = MatchedPath()
                for signal in signals.signals:
                    point = matched.points.add()
                    point.point.lat = signal.lat
                    point.point.lon = signal.lon
                    point.timestamp = signal.time

                headers = {
                    "Content-Type": "application/x-protobuf",
                }
                return Response(matched.SerializeToString(), status=200, headers=headers)(environ, start_response)

        return Response(f"unknown path {request.path}", status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
