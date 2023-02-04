import grpc
import threading

import logbroker.unified_agent.plugins.grpc_input.proto.unified_agent_pb2 as unified_agent_pb2
import logbroker.unified_agent.plugins.grpc_input.proto.unified_agent_pb2_grpc as unified_agent_pb2_grpc

from concurrent import futures


class UnifiedAgentService(unified_agent_pb2_grpc.UnifiedAgentServiceServicer):
    payload = []

    def Session(self, request, context):
        for i in request:
            if i.HasField("initialize"):
                init = unified_agent_pb2.Response.Initialized()
                init.session_id = "sess0"
                init.last_seq_no = 0
                resp = unified_agent_pb2.Response(initialized=init)
                yield resp
            elif i.HasField("data_batch"):
                for c, p in enumerate(i.data_batch.payload):
                    self.payload.append(p)
                    ack = unified_agent_pb2.Response.Ack()
                    ack.seq_no = i.data_batch.seq_no[c]
                    resp = unified_agent_pb2.Response(ack=ack)
                    yield resp


def InitUnifiedAgentService(socket):
    def run(service, socket):
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        unified_agent_pb2_grpc.add_UnifiedAgentServiceServicer_to_server(service, server)

        server.add_insecure_port("unix://{0}".format(socket))
        server.start()
        server.wait_for_termination(timeout=3)

    service = UnifiedAgentService()
    thr = threading.Thread(target=run, args=(service, socket))
    return [service, thr]
