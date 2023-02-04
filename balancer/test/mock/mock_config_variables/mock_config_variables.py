# -*- coding: utf-8 -*-
import json
import pylua
import netaddr


def generate_mock_data(config_file_path, port_manager, ip_subnet, limit_backends_instances):
    tmpl = '''\
        function string.starts(String,Start)
            return string.sub(String,1,string.len(Start))==Start
        end

        local function extract()
            {data}
            local backends = {{}};

            for key, value in pairs(_G) do
                if key and (string.starts(key, 'port_') or string.starts(key, 'ipdispatch_') or string.starts(key, 'backends_')) then
                    backends[key] = value;
                end;
            end;

            return {{
                backends = backends;
                instance = instance;
            }}
        end

        return require('json').encode(extract())
        '''

    code = tmpl.format(data=open(config_file_path, 'r').read())
    json_data = json.loads(pylua.eval_raw(code))
    backend_data = json_data['backends']

    def rec_get_sd_backends(node, sd_backends):
        if isinstance(node, dict):
            for k, v in node.iteritems():
                if k == 'sd':
                    if isinstance(v, dict) and 'endpoint_sets' in v:
                        sd_backends += [(backend['cluster_name'], backend['endpoint_set_id']) for backend in v['endpoint_sets']]
                else:
                    rec_get_sd_backends(v, sd_backends)
        elif isinstance(node, list):
            for v in node:
                rec_get_sd_backends(v, sd_backends)
    sd_backends = []
    rec_get_sd_backends(json_data['instance'], sd_backends)

    ip_network = netaddr.IPNetwork(ip_subnet)
    ipnet = ip_network.iter_hosts()
    backend_port = port_manager.get_port()

    mocked_instance_addrs = []
    for key in backend_data:
        if key.startswith('ipdispatch_'):
            backend_data[key] = [str(ipnet.next()) for _ in range(len(backend_data[key]))]
            section = key[len('ipdispatch_'):]
            port_key, port_ssl_key = 'port_' + section, 'port_ssl_' + section
            port, ssl_port = None, None
            if port_key in backend_data:
                port = port_manager.get_port()
                backend_data['port_' + section] = port
            if port_ssl_key in backend_data:
                ssl_port = port_manager.get_port()
                backend_data['port_ssl_' + section] = ssl_port
            for ip in backend_data[key]:
                if port:
                    mocked_instance_addrs.append({'ip': ip, 'port': port})
                if ssl_port:
                    mocked_instance_addrs.append({'ip': ip, 'port': ssl_port})
        if key.startswith('backends_') and len(backend_data[key]):
            if limit_backends_instances is not None:
                backend_data[key] = backend_data[key][:limit_backends_instances]
            for k in backend_data[key]:
                backend_ip = str(ipnet.next())
                k['host'] = k['cached_ip'] = backend_ip
                k['port'] = backend_port

    for backend in sd_backends:
        backend_ip = str(ipnet.next())
        k = {}
        k['host'] = k['cached_ip'] = backend_ip
        k['port'] = backend_port
        backend_data['backends_{}#{}'.format(backend[0], backend[1])] = [k]

    backend_data['instance_addrs'] = mocked_instance_addrs

    admin_port = port_manager.get_port()
    backend_data['instance_admin_addrs'] = [
        {'ip': str(ipnet.next()), 'port': admin_port}
    ]

    unistat_port = port_manager.get_port()
    backend_data['instance_unistat_addrs'] = [
        {'ip': str(ipnet.next()), 'port': unistat_port}
    ]

    return backend_data, sd_backends
