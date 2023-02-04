if port == nil then port = "8181"; end
if admin_port == nil then admin_port = "8282"; end
if backend1_port == nil then backend1_port = 9765; end
if backend2_port == nil then backend2_port = 9766; end
if backend3_port == nil then backend3_port = 9767; end
if weight == nil then weight = 1; end
if weights_file == nil then weights_file = ""; end
if reload_duration == nil then reload_duration = "1s"; end
empty_balancer = empty_balancer and empty_balancer == "true"

function genBalancer()
    result = {}
    if not empty_balancer then
        result = {
            weights_file = weights_file;
            reload_duration = reload_duration;
            {
                weight=weight;
                debug = {
                    dump = false;
                    tag = "backend1";
                    proxy = {
                        host = "localhost"; port = backend1_port;
                        connect_timeout = "5s"; backend_timeout = "5s";
                        resolve_timeout = "1s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; --debug
            };
            {
                weight=weight;
                debug = {
                    dump = false;
                    tag = "backend2";
                    proxy = {
                        host = "localhost"; port = backend2_port;
                        connect_timeout = "5s"; backend_timeout = "5s";
                        resolve_timeout = "1s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; --debug
            };
            {
                weight=weight;
                debug = {
                    dump = false;
                    tag = "backend3";
                    proxy = {
                        host = "localhost"; port = backend3_port;
                        connect_timeout = "5s"; backend_timeout = "5s";
                        resolve_timeout = "1s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; --debug
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

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        }; -- stats_addrs
    };

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
                                errorlog = {
                                    log = errorlog;
                                    balancer2 = {
                                        attempts = 3;
                                        rendezvous_hashing = genBalancer(); -- rendezvous_hashing
                                    }; -- balancer
                                }; -- errorlog
                            }; -- hasher
                        }; -- default
                    }; -- regexp
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
