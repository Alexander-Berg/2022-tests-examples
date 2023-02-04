instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    log = log;
    dns_resolve_cached_ip_if_not_set = dns_resolve_cached_ip_if_not_set;
    dns_async_resolve = true;
    dns_ip = dns_ip;
    dns_port = dns_port;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log_level = "DEBUG";
                log = errorlog;
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        regexp = {
                            for_srcrwr = {
                                match_fsm = { host = "([a-z]?-?)(man|sas|vla)-([a-z0-9]+)-([0-9]+)-((yp.tun.si.turbopages.org)|(yp.tun.si.yandex-team.ru))"; case_insensitive = true; };
                                srcrwr_ext = {
                                    remove_prefix = remove_prefix;
                                    domains = domains;
                                    proxy = {
                                        host = 'not_exists';
                                        port = default_backend_port;
                                    }; -- proxy
                                }; -- srcrwr_ext
                            }; -- for_srcrwr
                            default = {
                                errordocument = {
                                    status = 502;
                                    force_conn_close = true;
                                }; -- errordocument
                            }; -- default
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
