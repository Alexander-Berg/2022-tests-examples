timeout = timeout or "5s";
url = url or "/proxy/";
host = host or "localhost";
dry_run = dry_run or 0;
if keepalive_count == nil then keepalive_count = 1; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    log = log;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            };
        };
        test = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    accesslog = {
                        log = accesslog;
                        rpcrewrite = {
                            url = url;
                            host = host;
                            dry_run = dry_run;
                            rpc = {
                                proxy = {
                                    keepalive_count = 1;
                                    host = "localhost";
                                    port = rpc_port;
                                    connect_timeout = "0.3s";
                                    backend_timeout = timeout;
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            }; -- rpc
                            balancer2 = {
                                attempts = 2;
                                ["weighted2"] = {
                                    {
                                        weight = 1;
                                        proxy = {
                                            keepalive_count = keepalive_count;
                                            host = "localhost";
                                            port = backend1_port;
                                            connect_timeout = "0.3s";
                                            backend_timeout = timeout;
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                        }; -- proxy
                                    };
                                    {
                                        weight = 1;
                                        proxy = {
                                            keepalive_count = keepalive_count;
                                            host = "localhost";
                                            port = backend2_port;
                                            connect_timeout = "0.3s";
                                            backend_timeout = timeout;
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                        }; -- proxy
                                    };
                                }; -- weighted2
                            }; -- balancer2
                        }; -- rpcrewrite
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
