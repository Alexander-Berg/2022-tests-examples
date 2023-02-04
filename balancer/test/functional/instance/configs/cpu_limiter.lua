instance = {
    thread_mode = thread_mode; set_no_file = false;

    events = {
        stats = "report";
    }; -- events

    log = instance_log;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    }; -- unistat

    cpu_limiter = {
        enable_conn_reject = enable_conn_reject;
        enable_keepalive_close = enable_keepalive_close;
        active_check_subnet = active_check_subnet;
        active_check_subnet_default = active_check_subnet_default;
        active_check_subnet_file = active_check_subnet_file;
        conn_reject_lo = conn_reject_lo; conn_reject_hi = conn_reject_hi;
        conn_hold_count = conn_hold_count; conn_hold_duration = conn_hold_duration;
        checker_address_cache_size = checker_address_cache_size;
        keepalive_close_lo = keepalive_close_lo; keepalive_close_hi = keepalive_close_hi;
        disable_file = disable_file;
    }; -- cpu_limiter

    ipdispatch = {
        default = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        prefix_path_router = {
                            check = {
                                route = "/check";
                                active_check_reply = {
                                    push_checker_address = push_checker_address;
                                }; -- active_check_reply
                            }; -- check
                            default = {
                                debug = {
                                use_cpu = true;
                                    errordocument = {
                                        status = 200;
                                        content = "ok";
                                    }; -- errordocument
                                }; -- debug
                            }; -- default
                        }; -- prefix_path_router
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
