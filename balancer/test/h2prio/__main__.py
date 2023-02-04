# -*- coding: utf-8 -*-
import argparse
import os
import logging
import json
import balancer.test.h2prio.html as html
import graphviz as gv
import multiprocessing as mp
import itertools
from collections import defaultdict

from balancer.test.util.resource import AbstractResourceManager
from balancer.test.util.fs import FileSystemManager
from balancer.test.util.process import ProcessManager
from balancer.test.util.stream import StreamManager
from balancer.test.util.stream.ssl.stream import SSLClientOptions
import balancer.test.util.proto.http2.connection as http2_conn
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef import http2


class SimpleResourceManager(AbstractResourceManager):
    def finish_all(self):
        self._finish_all()


DEFAULT_WINDOW_SIZE = 65525
ENOUGH_FOR_ANYBODY = 20 * 1024 * 1024
DEFAULT_WEIGHT = 16
_DW = DEFAULT_WEIGHT


def build_settings(params):
    return frames.Settings(
        length=None, flags=0, reserved=0, data=[
            frames.Parameter(identifier, value) for (identifier, value) in params
        ],
    )


class ReqType(object):
    SMALL = 0
    LARGE = 1


_RT = ReqType


class RequestInfo(object):
    def __init__(self, raw_msg, size=None):
        super(RequestInfo, self).__init__()
        self.__raw_msg = raw_msg
        self.__size = size

    @property
    def raw_msg(self):
        return self.__raw_msg

    @property
    def size(self):
        return self.__size

    @size.setter
    def size(self, value):
        assert self.__size is None
        self.__size = value


class ServerProps(object):
    def __init__(self, name, host, port):
        super(ServerProps, self).__init__()
        self.__name = name
        self.__host = host
        self.__port = port

    @property
    def name(self):
        return self.__name

    @property
    def host(self):
        return self.__host

    @property
    def port(self):
        return self.__port

    @property
    def req_small(self):
        raise NotImplementedError()

    @property
    def req_large(self):
        raise NotImplementedError()

    def get_req(self, req_type):
        if req_type == ReqType.SMALL:
            return self.req_small
        else:
            return self.req_large

    def _build_request(self, path):
        raise NotImplementedError()

    def _build_req_info(self, path, size):
        return RequestInfo(self._build_request(path).to_raw_request(), size)


class Golang(ServerProps):
    def __init__(self):
        super(Golang, self).__init__('Golang', 'http2.golang.org', 443)

    @property
    def req_small(self):
        return self._build_req_info('/file/gopher.png', 17668)

    @property
    def req_large(self):
        return self._build_req_info('/file/go.src.tar.gz', 10921353)

    def _build_request(self, path):
        return http2.request.get(path=path, scheme='https', authority=self.host)


class DriveApi(ServerProps):
    def __init__(self, small_id, small_size, large_id, large_size, token, token_type='Bearer'):
        super(DriveApi, self).__init__('Google Drive', 'www.googleapis.com', 443)
        self.__small_id = small_id
        self.__small_size = small_size
        self.__large_id = large_id
        self.__large_size = large_size
        self.__token = token
        self.__token_type = token_type

    @staticmethod
    def from_json(path):
        with open(path) as f:
            data = json.load(f)
        return DriveApi(**data)

    @staticmethod
    def _build_path(file_id):
        return '/drive/v3/files/{}?alt=media'.format(file_id)

    @property
    def req_small(self):
        return self._build_req_info(self._build_path(self.__small_id), self.__small_size)

    @property
    def req_large(self):
        return self._build_req_info(self._build_path(self.__large_id), self.__large_size)

    def _build_request(self, path):
        return http2.request.get(
            path=path, scheme='https', authority=self.host,
            headers={'authorization': '{} {}'.format(self.__token_type, self.__token)}
        )


class Engine(object):
    def __init__(self, server_props, stream_manager, auto_init=True):
        super(Engine, self).__init__()
        self.__server_props = server_props
        self.__conn = None
        self.__streams = list()
        self.__id_map = dict()
        self.__pub_id_map = dict()
        self.__data = list()
        self.__data_count = 0
        self.__close_count = 0
        self.__resp_sizes = list()
        self.__got_data = list()

        self.__stream_manager = stream_manager

        self.__first_half_resp_fired = False
        self.__first_half_resp_listeners = list()
        self.__stream_close_listeners = list()

        if auto_init:
            self.init_conn()
            self.fill_conn_window()

    def listen_first_half_response(self, listener):
        self.__first_half_resp_listeners.append(listener)

    def __fire_first_half_response(self):
        for listener in self.__first_half_resp_listeners:
            listener()

    def listen_stream_close(self, listener):
        self.__stream_close_listeners.append(listener)

    def __fire_stream_close(self):
        for listener in self.__stream_close_listeners:
            listener()

    def listen_stream_close_count(self, count, listener):
        def count_listener():
            if count == self.__close_count:
                listener()
        self.listen_stream_close(count_listener)

    @property
    def conn(self):
        return self.__conn

    def init_conn(self):
        ssl_stream = self.__stream_manager.create_ssl(
            self.__server_props.port,
            SSLClientOptions(alpn='h2', server_name=self.__server_props.host),
            self.__server_props.host,
        )
        self.__conn = http2_conn.ClientConnection(ssl_stream)
        self.__conn.write_preface()
        self.__conn.write_frame(build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, 0)]))

    def fill_conn_window(self):
        req_info = self.__server_props.req_small
        count = DEFAULT_WINDOW_SIZE / req_info.size
        tail_len = DEFAULT_WINDOW_SIZE - (count * req_info.size)
        if tail_len != 0:
            self.__conn.write_window_update(req_info.size - tail_len)
            count += 1
        for i in range(count):
            s = self.__conn.create_stream()
            s.write_message(req_info.raw_msg)
            s.write_window_update(ENOUGH_FOR_ANYBODY)
            s.read_message()

    def update_conn_window(self, size):
        self.__conn.write_window_update(size)

    def open(self, req_type, pub_id):
        req_info = self.__server_props.get_req(req_type)
        s = self.__conn.create_stream()
        s.write_message(req_info.raw_msg)
        self.__id_map[s.stream_id] = len(self.__streams)
        self.__pub_id_map[s.stream_id] = pub_id
        self.__streams.append(s)
        self.__resp_sizes.append(req_info.size)
        self.__got_data.append(0)
        return s

    def read_data(self):
        frame = self.__conn.wait_frame(frames.Data)
        id_ = self.__id_map[frame.stream_id]
        next_data_count = self.__data_count + frame.length
        self.__data.append({
            'x': self.__data_count,
            'x2': next_data_count,
            'y': id_,
        })
        self.__data_count = next_data_count
        self.__got_data[id_] += frame.length
        if frame.flags & flags.END_STREAM:
            self.__close_count += 1
            self.__fire_stream_close()
            assert self.__got_data[id_] == self.__resp_sizes[id_]
        if (2 * self.__got_data[id_] >= self.__resp_sizes[id_]) and not self.__first_half_resp_fired:
            self.__first_half_resp_fired = True
            self.__fire_first_half_response()
        print ' '.join(['{}% ({})'.format(
            100 * self.__got_data[i] / self.__resp_sizes[i], self.__got_data[i]
        ) for i in range(len(self.__streams))])

    def read_all(self, window_update, force_last=True):
        need_update = True
        while self.__close_count < len(self.__streams):
            if need_update:
                if force_last and (len(self.__streams) - self.__close_count == 1):
                    window_update = ENOUGH_FOR_ANYBODY
                    need_update = False
                self.update_conn_window(window_update)
            self.read_data()

    def get_info(self):
        threshold = self.__data[-1]['x2'] * 2
        return {
            'name': self.__server_props.name,
            'streams': [str(self.__pub_id_map[s.stream_id]) for s in self.__streams],
            'data': self.__data,
            'threshold': threshold,
        }

    def dump(self, path):
        with open(path, 'w') as f:
            f.write(html.gen_html(self.get_info()))


class WindowPolicy(object):
    ON_INIT = 0
    ON_FIRST_HALF_RESP = 1
    AFTER_ALL_CLOSED = 2


_WP = WindowPolicy


class StreamParams(object):
    def __init__(self, req_type, weight=_DW, window_policy=_WP.ON_INIT, children=None):
        super(StreamParams, self).__init__()
        self.__req_type = req_type
        self.__weight = weight
        self.__window_policy = window_policy
        if children is None:
            children = list()
        self.__children = children
        self.__public_id = None

    @property
    def public_id(self):
        return self.__public_id

    @public_id.setter
    def public_id(self, value):
        assert self.__public_id is None
        self.__public_id = value

    @property
    def req_type(self):
        return self.__req_type

    @property
    def weight(self):
        return self.__weight

    @property
    def window_policy(self):
        return self.__window_policy

    @property
    def children(self):
        return self.__children


_SP = StreamParams


def build_window_update_callback(stream):
    def callback():
        stream.write_window_update(ENOUGH_FOR_ANYBODY)
    return callback


def traverse_streams(stream_params, callback):
    for sp in stream_params:
        callback(sp)
        traverse_streams(sp.children, callback)


class Test(object):
    def __init__(self, name, window_update, params):
        super(Test, self).__init__()
        self.__name = name
        self.__window_update = window_update
        self.__params = params

        self.__streams_count = 0

        def inc_streams_count(sp):
            self.__streams_count += 1
            sp.public_id = self.__streams_count
        traverse_streams(self.__params, inc_streams_count)

        self.__after_all_closed_count = 0

        def inc_after_all_closed_count(sp):
            if sp.window_policy == _WP.AFTER_ALL_CLOSED:
                self.__after_all_closed_count += 1
        traverse_streams(self.__params, inc_after_all_closed_count)

    @property
    def name(self):
        return self.__name

    @property
    def params(self):
        return self.__params

    def run(self, server_props, stream_manager):
        e = Engine(server_props, stream_manager)

        def open_stream(sp, parent_id):
            s = e.open(sp.req_type, sp.public_id)
            s.write_priority(0, parent_id, sp.weight)
            cb = build_window_update_callback(s)
            if sp.window_policy == _WP.ON_INIT:
                cb()
            elif sp.window_policy == _WP.ON_FIRST_HALF_RESP:
                e.listen_first_half_response(cb)
            elif sp.window_policy == _WP.AFTER_ALL_CLOSED:
                e.listen_stream_close_count(self.__streams_count - self.__after_all_closed_count, cb)
            else:
                assert 0, 'Unknown window policy: {}'.format(sp.window_policy)
            return s.stream_id

        def open_all(stream_params, parent_id):
            for sp in stream_params:
                sp_id = open_stream(sp, parent_id)
                open_all(sp.children, sp_id)
        open_all(self.__params, 0)
        e.read_all(self.__window_update)
        return e.get_info()


SMALL_WU = 1024
LARGE_WU = 10 * 1024


ALL_TESTS = [
    Test('plain_small_eq', SMALL_WU, [_SP(_RT.SMALL) for i in range(5)]),
    Test('plain_small_ne', SMALL_WU, [_SP(_RT.SMALL, weight=(i + 1) * _DW) for i in range(5)]),
    Test('plain_small_large_eq', SMALL_WU, [_SP(_RT.SMALL), _SP(_RT.LARGE)]),
    Test('plain_small_large_ne', SMALL_WU, [_SP(_RT.SMALL), _SP(_RT.LARGE, weight=5 * _DW)]),
    Test('plain_large_eq', LARGE_WU, [_SP(_RT.LARGE), _SP(_RT.LARGE)]),
    Test('plain_large_ne', LARGE_WU, [_SP(_RT.LARGE), _SP(_RT.LARGE, weight=2 * _DW)]),
    Test('plain_large_ne_half', LARGE_WU, [
        _SP(_RT.LARGE), _SP(_RT.LARGE, weight=2 * _DW, window_policy=_WP.ON_FIRST_HALF_RESP)
    ]),
    Test('plain_large_ne_last', LARGE_WU, [
        _SP(_RT.LARGE), _SP(_RT.LARGE, weight=2 * _DW, window_policy=_WP.AFTER_ALL_CLOSED)
    ]),
    Test('chain_large_eq', LARGE_WU, [_SP(_RT.LARGE, children=[_SP(_RT.LARGE)])]),
    Test('chain_large_eq_half', LARGE_WU, [
        _SP(_RT.LARGE, window_policy=_WP.ON_FIRST_HALF_RESP, children=[_SP(_RT.LARGE)])
    ]),
    Test('chain_large_eq_last', LARGE_WU, [
        _SP(_RT.LARGE, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.LARGE)])
    ]),
    Test('chain_large_heavy_parent', LARGE_WU, [
        _SP(_RT.LARGE, weight=2 * _DW, children=[_SP(_RT.LARGE)])
    ]),
    Test('chain_large_heavy_child', LARGE_WU, [
        _SP(_RT.LARGE, children=[_SP(_RT.LARGE, weight=2 * _DW)])
    ]),
    Test('chain_small_large', SMALL_WU, [_SP(_RT.SMALL, children=[_SP(_RT.LARGE)])]),
    Test('chain_large_small', LARGE_WU, [_SP(_RT.LARGE, children=[_SP(_RT.SMALL)])]),
    Test('chain_small_large_large', LARGE_WU, [
        _SP(_RT.SMALL, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.LARGE, children=[_SP(_RT.LARGE)])])
    ]),
    Test('parallel_children_eq', LARGE_WU, [
        _SP(_RT.SMALL, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.LARGE), _SP(_RT.LARGE)])
    ]),
    Test('parallel_children_ne', LARGE_WU, [
        _SP(_RT.SMALL, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.LARGE), _SP(_RT.LARGE, weight=2 * _DW)])
    ]),
    Test('sibling_vs_child_eq', SMALL_WU, [
        _SP(_RT.SMALL), _SP(_RT.SMALL, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.SMALL)])
    ]),
    Test('sibling_vs_heavy_child', SMALL_WU, [
        _SP(_RT.SMALL), _SP(_RT.SMALL, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.SMALL, weight=2 * _DW)])
    ]),
    Test('heavy_sibling_vs_child', SMALL_WU, [
        _SP(_RT.SMALL, weight=2 * _DW), _SP(_RT.SMALL, window_policy=_WP.AFTER_ALL_CLOSED, children=[_SP(_RT.SMALL)])
    ]),
]


COLOR = {
    _RT.SMALL: '#a0ffa0',
    _RT.LARGE: '#ffa0a0',
}
WINDOW_POLICY_STR = {
    _WP.ON_INIT: 'init',
    _WP.ON_FIRST_HALF_RESP: 'half',
    _WP.AFTER_ALL_CLOSED: 'last',
}


def __print_graph(graph, params, parent_id, next_id):
    for sp in params:
        label = 'id: {}\nweight: {}\nWP: {}'.format(sp.public_id, sp.weight, WINDOW_POLICY_STR[sp.window_policy])
        graph.node(str(next_id), label=label, fillcolor=COLOR[sp.req_type], style='filled', shape='rect')
        graph.edge(str(next_id), str(parent_id))
        next_id = __print_graph(graph, sp.children, next_id, next_id + 1)
    return next_id


def print_graph(test, fs):
    graph = gv.Digraph(format='svg')
    graph.graph_attr['rankdir'] = 'BT'
    root_id = 0
    graph.node(str(root_id), label='root', shape='square')
    __print_graph(graph, test.params, 0, 1)
    path = graph.render(fs.abs_path(test.name))
    with open(path) as f:
        return f.read()


class TestRunner(object):
    def __init__(self, root_fs, openssl_path):
        super(TestRunner, self).__init__()
        self.__root_fs = root_fs
        self.__openssl_path = openssl_path

    def __call__(self, args):
        test, server_props = args
        fs_manager = FileSystemManager(self.__root_fs.create_dir(str(os.getpid())))
        resource_manager = SimpleResourceManager()
        try:
            process_manager = ProcessManager(resource_manager, logging.getLogger(), fs_manager)
            stream_manager = StreamManager(resource_manager, fs_manager, self.__openssl_path, process_manager)
            return test.run(server_props, stream_manager)
        except Exception, e:
            with open(self.__root_fs.create_file('ALARM.txt'), 'w') as f:
                f.write('Exception in {} {}: {}'.format(test.name, server_props.name, e.message))
        finally:
            resource_manager.finish_all()


def main():
    parser = argparse.ArgumentParser(description='Run HTTP/2 priority tests')
    parser.add_argument('--golang', action='store_true', help='run tests over http2.golang.org')
    parser.add_argument('--gdrive', help='file containing Google Drive API tests options')
    parser.add_argument('-j', dest='threads', type=int, default=1, help='Threads count')
    parser.add_argument('--logs', default='./logs', help='path to logs dir')
    parser.add_argument('--openssl-cmd', dest='openssl_path', default='openssl', help='openssl command')
    args = parser.parse_args()

    props_list = list()
    if args.golang:
        props_list.append(Golang())
    if args.gdrive:
        props_list.append(DriveApi.from_json(args.gdrive))

    root_path = os.path.abspath(args.logs)
    if not os.path.exists(root_path):
        os.makedirs(root_path)
    root_fs = FileSystemManager(root_path)

    tr = TestRunner(root_fs, args.openssl_path)

    all_args = list(itertools.product(ALL_TESTS, props_list))

    p = mp.Pool(args.threads)
    all_info = p.map(tr, all_args)

    result = defaultdict(dict)
    errors = list()
    for (test, sp), info in zip(all_args, all_info):
        if info is None:
            errors.append((test, sp))
        result[test.name][sp.name] = info

    if errors:
        print 'List of errors:\n{}'.format('\n'.join(['{} {}'.format(test.name, sp.name) for (test, sp) in errors]))
    result_fs = FileSystemManager(root_fs.create_dir('result'))
    tmp_fs = FileSystemManager(root_fs.create_dir('tmp'))
    for test in ALL_TESTS:
        info_list = [result[test.name][sp.name] for sp in props_list]
        html.dump_html(print_graph(test, tmp_fs), info_list, result_fs.abs_path('{}.html'.format(test.name)))


if __name__ == '__main__':
    main()
