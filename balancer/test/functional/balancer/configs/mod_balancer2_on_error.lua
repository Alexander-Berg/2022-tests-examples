backend_host = backend_host or "localhost"
instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = log;

    dns_async_resolve = dns_async_resolve;
    dns_resolve_cached_ip_if_not_set = dns_resolve_cached_ip_if_not_set;
    dns_timeout = dns_timeout;
    dns_ip = dns_ip;
    dns_port = dns_port;
    dns_ttl = dns_ttl;

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
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        balancer2 = {
                            attempts = attempts;
                            attempts_file = attempts_file;
                            rewind_limit = rewind_limit;
                            simple_policy = {};
                            events = {
                                enable_on_error = 'enable';
                                disable_on_error = 'disable';
                            }; -- events
                            rr = {
                                randomize_initial_state = false;
                                {
                                    proxy = {
                                        host = backend_host; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "0.3s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                },
                            }; -- rr
                            on_error = {
                                errordocument = {
                                    status = 404;
                                    content = "on_error";
                                }; -- errordocument
                            }; -- on_error
                        }; -- balancer2
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
