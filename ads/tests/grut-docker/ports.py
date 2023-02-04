import json
import socket
import sys
import os

def get_free_ports(n=1, udp=False):
    ports = []
    sockets = []
    socktype = socket.SOCK_DGRAM if udp else socket.SOCK_STREAM
    try:
        for i in range(n):
            sock = socket.socket(socket.AF_INET, socktype)
            sockets.append(sock)
            sock.bind(('127.0.0.1', 0))
            ports.append(sock.getsockname()[1])
    finally:
        for sock in sockets:
            sock.close()
    return ports

def __main__():
    unused_ports = get_free_ports(4)
    #todo: use regexp

    # http yt proxy + rpc yt proxy
    file = open('../usr/bin/start_yt_local.sh', 'r')
    data = file.readlines()
    data[2] = "/usr/bin/start.sh --proxy-config \"{address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn=\\\"localhost:" + str(unused_ports[0]) + "\\\"}}\" --rpc-proxy-count 1 --rpc-proxy-port " + str(unused_ports[1]) + " --http-proxy-port " + str(unused_ports[0]) + "\n"
    file = open('../usr/bin/start_yt_local.sh', 'w')
    file.writelines(data)

    # init_db
    file = open('../usr/bin/init_db.sh', 'r')
    data = file.readlines()
    data[12] = 'GRUT_TEST_USER="root" GRUT_TEST_ACCOUNT="default" GRUT_TEST_MASTER="localhost:' + str(unused_ports[0])  +'" GRUT_TEST_PATH="//grut" YT_TOKEN="dummy" /usr/bin/grut-admin db-sync -S local --clean --commit && touch /tmp/db_initialized\n'
    file = open('../usr/bin/init_db.sh', 'w')
    file.writelines(data)

    # grut_orm config
    file = open('../etc/grut_orm/config.yson', 'r')
    data = file.readlines()
    data[244] = '                "address" = "0.0.0.0:' + str(unused_ports[2]) + '";\n'
    data[285] = '            "cluster_url" = "localhost:' + str(unused_ports[0]) + '";\n'
    file = open('../etc/grut_orm/config.yson', 'w')
    file.writelines(data)

    # object_api config
    file = open('../etc/grut_object_api/config.yson', 'r')
    data = file.readlines()
    data[222] = '                "address" = "0.0.0.0:' + str(unused_ports[3]) + '";\n'
    data[243] = '            "address" = "localhost:' + str(unused_ports[2]) + '";\n'
    file = open('../etc/grut_object_api/config.yson', 'w')
    file.writelines(data)

    ports_config = {
        "yt_http_proxy": unused_ports[0],
        "object_api_port": unused_ports[3]
    }

    print(unused_ports)

    file = open('../configs/ports_config.json', 'w')
    file.writelines(json.dumps(ports_config))


__main__()
