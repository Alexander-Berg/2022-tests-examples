instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = log;

    dns_async_resolve = dns_async_resolve;
    dns_resolve_cached_ip_if_not_set = dns_resolve_cached_ip_if_not_set;
    dns_ip = dns_ip;
    dns_port = dns_port;
    dns_timeout=dns_timeout;
    dns_ttl=dns_ttl;

    buffer = buffer;
    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        proxy = {
                            host = host;
                            port = backend_port;
                            use_only_ipv4 = use_only_ipv4;
                            use_only_ipv6 = use_only_ipv6;
                            backend_timeout = backend_timeout or "5s";
                            need_resolve = need_resolve;
                            cached_ip = cached_ip;
                            watch_client_close = watch_client_close;
                        }; -- proxy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
