if port == nil then port = "8181"; end
if admin_port == nil then admin_port = "8282"; end
if virtual_nodes == nil then virtual_nodes = 1000; end
if backend_port1 == nil then backend_port1 = 9765; end
if backend_port2 == nil then backend_port2 = 9766; end
if backend_port3 == nil then backend_port3 = 9767; end
if weight == nil then weight = 1; end
empty_balancer = empty_balancer and empty_balancer == "true"

function genBalancer()
    result = {}

    if not empty_balancer then
        result = {
            virtual_nodes = virtual_nodes;
            request = request;
            {
                weight=weight;
                proxy = {
                    host = "localhost"; port = backend_port1;
                    connect_timeout = "5s"; backend_timeout = "5s";
                    resolve_timeout = "1s";
                    fail_on_5xx = 0;
                }; -- proxy
            };
            {
                weight=weight;
                proxy = {
                    host = "localhost"; port = backend_port2;
                    connect_timeout = "5s"; backend_timeout = "5s";
                    resolve_timeout = "1s";
                    fail_on_5xx = 0;
                }; -- proxy
            };
            {
                weight=weight;
                proxy = {
                    host = "localhost"; port = backend_port3;
                    connect_timeout = "5s"; backend_timeout = "5s";
                    resolve_timeout = "1s";
                    fail_on_5xx = 0;
                }; -- proxy
            };
        }
    end;

    return result
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                accesslog = {
                    log = accesslog;
                    regexp = {
                        default = {
                            hasher = {
                                mode = "request";
                                balancer2 = {
                                    attempts = 3;
                                    consistent_hashing = genBalancer(); -- consisitent_hashing
                                }; -- balancer2
                            }; -- hasher
                        }; -- default
                    }; -- regexp
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
