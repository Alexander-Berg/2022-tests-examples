from multiprocessing.connection import Listener
import logging
import os
import stat
import sys
import tempfile
import threading
import uuid

from maps.garden.sdk.module_rpc.module_runner import ModuleProperties, run_loop

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc

from . import graph


def _emulate_daemonization(socket_path):
    module_properties = ModuleProperties(
        module_name=graph.MODULE_NAME,
        fill_graph=graph.fill_graph,
    )
    try:
        run_loop(socket_path, module_properties)
    except:
        logging.exception("daemonization failed")
        sys.exit(1)


def _is_path_socket(path):
    if not os.path.exists(path):
        return False
    return stat.S_ISSOCK(os.stat(path).st_mode)


def _make_module_info_command():
    # FIXME thegeorg@:
    #   The code was copied from garden/libs/module_storage/communicate.py due
    #   to the necessity of PY2 support in these tests.
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.MODULE_INFO
    command.moduleInfoInput.stub = 0
    return command


def test_daemonization():
    socket_path = os.path.join(tempfile.gettempdir(), str(uuid.uuid4()))

    # NB:
    #   Listener creation also creates socket to listen on.
    #   As attempt of connection to nonexistent socket fails,
    #   the test flow should be
    #       * create listener,
    #       * launch daemon thread,
    #       * accept connections from daemon thread in listener
    listener = Listener(address=socket_path, family='AF_UNIX')
    listener._listener._socket.settimeout(10)
    assert _is_path_socket(socket_path)

    interacting_thread = threading.Thread(
        target=_emulate_daemonization,
        args=(socket_path,)
    )
    interacting_thread.start()

    connection = listener.accept()
    raw_command = _make_module_info_command().SerializeToString()
    connection.send_bytes(raw_command)
    output = module_rpc.ModuleInfoOutput()
    output.ParseFromString(connection.recv_bytes())
    assert output.IsInitialized()

    connection.close()
    assert _is_path_socket(socket_path)
    listener.close()
    assert not _is_path_socket(socket_path)

    interacting_thread.join()
