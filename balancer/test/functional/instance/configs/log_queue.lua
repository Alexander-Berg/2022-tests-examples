instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port;};
    };

    log = instance_log;

    log_queue_max_size = log_queue_max_size;
    log_queue_submit_attempts_count = log_queue_submit_attempts_count;
    log_queue_flush_interval = log_queue_flush_interval;

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
                    log = instance_log;
                    errordocument = {
                        status = 200;
                        content = "OK";
                    }; -- errordocument
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
