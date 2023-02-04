import argparse

import gevent.pywsgi
from google.protobuf import text_format

import infra.callisto.protos.deploy.tables_pb2 as tables  # noqa
import search.plutonium.deploy.proto.rescan_pb2 as rescan  # noqa


def main():
    args = parse_args()

    if args.extended:
        # linter doesn't recognize `encoding` arg in bytes(..., encoding='utf-8'), hence noqas
        def render_response(env, start_response):
            request_body = env['wsgi.input'].read()
            request = text_format.Parse(request_body, tables.TExtendedNotification())

            response = tables.TExtendedStatus(
                DaemonStatus=bytes('notified {} resources'.format(len(request.Resources)), encoding='utf-8'),  # noqa
                Resources=[tables.TExtendedStatus.TResourceStatus(
                    Namespace=resource.Namespace,
                    LocalPath=resource.LocalPath,
                    Valid=args.active,
                    Extended=bytes('extended', encoding='utf-8')  # noqa
                ) for resource in request.Resources]
            )
            response_body = text_format.MessageToString(response)
            start_response('200 OK', [('Content-Type', 'application/protobuf')])
            return [bytes(response_body, encoding='utf-8')]  # noqa
    elif args.rescan:
        # linter doesn't recognize `encoding` arg in bytes(..., encoding='utf-8'), hence noqas
        def render_response(env, start_response):
            request_body = env['wsgi.input'].read()
            request = text_format.Parse(request_body, rescan.TExtendedNotification())

            response = rescan.TExtendedStatus(
                DaemonStatus=bytes('notified {} resources'.format(len(request.Resources)), encoding='utf-8'),  # noqa
                Resources=[rescan.TResourceStatus(
                    Resource=resource,
                    Valid=args.active,
                    Extended=bytes('extended', encoding='utf-8')  # noqa
                ) for resource in request.Resources]
            )
            response_body = text_format.MessageToString(response)
            start_response('200 OK', [('Content-Type', 'application/protobuf')])
            return [bytes(response_body, encoding='utf-8')]  # noqa
    else:
        def render_response(_, start_response):
            start_response('{} WHATEVER'.format(200 if args.active else 500),
                           [('Content-type', 'application/json')])
            return ''

    gevent.pywsgi.WSGIServer(('::', args.port), render_response).serve_forever()


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, required=True)
    parser.add_argument('--active', action='store_true', default=False)
    parser.add_argument('--extended', action='store_true', default=False)
    parser.add_argument('--rescan', action='store_true', default=False)
    return parser.parse_args()
