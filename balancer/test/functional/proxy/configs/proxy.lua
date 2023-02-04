instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    }; -- unistat

    dns_async_resolve = dns_async_resolve;
    dns_resolve_cached_ip_if_not_set = dns_resolve_cached_ip_if_not_set;

    log = log;

    buffer = buffer;
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
                        report = {
                            uuid = "default";
                            ranges = "7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms";
                            disable_robotness = true; disable_sslness = true;
                            events = {
                                stats = "report";
                            }; -- events
                            proxy = {
                                host = host; port = backend_port;
                                use_only_ipv4 = use_only_ipv4;
                                use_only_ipv6 = use_only_ipv6;
                                keepalive_count = keepalive_count;
                                keepalive_timeout = keepalive_timeout;
                                tcp_keep_intvl = tcp_keep_intvl;
                                tcp_keep_cnt = tcp_keep_cnt;
                                tcp_keep_idle = tcp_keep_idle;
                                backend_timeout = backend_timeout or "5s";
                                client_read_timeout = client_read_timeout;
                                client_write_timeout = client_write_timeout;
                                backend_read_timeout = backend_read_timeout;
                                backend_write_timeout = backend_write_timeout;
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                fail_on_5xx = fail_on_5xx;
                                need_resolve = need_resolve;
                                cached_ip = cached_ip;
                                allow_connection_upgrade = allow_connection_upgrade;
                                watch_client_close = watch_client_close;
                                _sock_outbufsize = socket_out_buffer;
                            }; -- proxy
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
