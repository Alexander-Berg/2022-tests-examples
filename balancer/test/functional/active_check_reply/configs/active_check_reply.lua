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
    };

    workers = workers;
    worker_start_delay = worker_start_delay or '0s';
    shutdown_accept_connections = shutdown_accept_connections;

    cpu_limiter = {
        active_check_subnet = "127.1.2.3"; -- active_check_subnet
    }; -- cpu_limiter

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
            http = {
                maxlen = 65536; maxreq = 65536;
                active_check_reply = {
                    default_weight = default_weight;
                    weight_file = weight_file;
                    use_header = use_header;
                    use_body = use_body;
                    force_conn_close = force_conn_close;
                    zero_weight_at_shutdown = zero_weight_at_shutdown;
                };
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
