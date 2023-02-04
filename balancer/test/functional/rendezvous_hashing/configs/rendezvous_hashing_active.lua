if port == nil then port = "8181"; end
if admin_port == nil then admin_port = "8282"; end
if backend1_port == nil then backend1_port = 9765; end
if backend2_port == nil then backend2_port = 9766; end
if backend3_port == nil then backend3_port = 9767; end
if weight == nil then weight = 1; end
if delay == nil then delay = "1s"; end

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
                    hasher = {
                        mode = "request";
                        errorlog = {
                            log = errorlog;
                            balancer2 = {
                                attempts = 1;
                                rendezvous_hashing = {
                                    request = "GET / HTTP/1.1\r\n\r\n";
                                    delay = delay;
                                    steady = true;
                                    {
                                        weight=weight;
                                        proxy = {
                                            host = "localhost"; port = backend1_port;
                                            connect_timeout = "5s"; backend_timeout = "5s";
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                            keepalive_count = 1;
                                        }; -- proxy
                                    };
                                    {
                                        weight=weight;
                                        proxy = {
                                            host = "localhost"; port = backend2_port;
                                            connect_timeout = "5s"; backend_timeout = "5s";
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                            keepalive_count = 1;
                                        }; -- proxy
                                    };
                                    {
                                        weight=weight;
                                        proxy = {
                                            host = "localhost"; port = backend3_port;
                                            connect_timeout = "5s"; backend_timeout = "5s";
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                            keepalive_count = 1;
                                        }; -- proxy
                                    };
                                }; -- rendezvous_hashing
                            }; -- balancer
                        }; -- errorlog
                    }; -- hasher
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
