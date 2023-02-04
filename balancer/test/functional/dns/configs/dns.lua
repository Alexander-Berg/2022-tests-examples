instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    log = log;

    reset_dns_cache_file = reset_dns_cache_file;
    dns_timeout = dns_timeout;
    dns_ttl = dns_ttl;
    dns_async_resolve = dns_async_resolve;
    dns_resolve_cached_ip_if_not_set = dns_resolve_cached_ip_if_not_set;
    dns_ip = dns_ip;
    dns_port = dns_port;
    dns_its_switch_file = dns_its_switch_file;
    dns_its_switch_check = dns_its_switch_check;

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
                            cached_ip = cached_ip;
                            resolve_timeout = resolve_timeout;
                            connect_timeout = connect_timeout;
                            backend_timeout = backend_timeout;
                        }; -- proxy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
