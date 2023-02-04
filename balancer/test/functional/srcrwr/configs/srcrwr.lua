instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    log = log;
    dns_resolve_cached_ip_if_not_set = dns_resolve_cached_ip_if_not_set;

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
                        srcrwr = {
                            id = id;
                            match_and = {
                                {
                                    match_fsm = {
                                        host = "(.*[.])?hamster[.](.*)?";
                                        case_insensitive = true;
                                    }; -- match_fsm
                                };
                                {
                                    match_source_ip = {
                                        source_mask = "::1,127.0.0.1,2a00:1450:4010:c08::66";
                                    }; -- match_source_ip
                                };
                            }; -- match_and
                            balancer2 = {
                                attempts = 2;
                                rr = {
                                    randomize_initial_state = false;
                                    backend1 = {
                                        proxy = {
                                            host = 'localhost';
                                            port = default_backend_port;
                                            keepalive_count = keepalive_count;
                                        }; -- proxy
                                    }; -- backend1
                                }; -- rr
                            }; -- balancer2
                        }; -- srcrwr
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
