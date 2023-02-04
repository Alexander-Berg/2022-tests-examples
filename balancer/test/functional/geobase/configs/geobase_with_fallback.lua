take_ip_from = take_ip_from or "X-Forwarded-For-Y";
laas_answer_header = laas_answer_header or "X-LaaS-Answered";
geobase_stats = geobase_stats or 'geobase'

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    http = {
        maxlen = 64 * 1024; maxreq = 64 * 1024;
        accesslog = {
            log = accesslog;
            geobase = {
                take_ip_from = 'My-Ip';
                laas_answer_header = 'X-LaaS-Answered';
                geo_host = geo_host;
                geo_path = geo_path;
                file_switch = file_switch;
                trusted = trusted;
                events = {
                    stats = geobase_stats;
                }; -- events
                geo = {
                    balancer2 = {
                        rr = {
                            randomize_initial_state = false;
                            {
                                proxy = {
                                    host = "localhost"; port = geo_backend_port;
                                    connect_timeout = "1s"; resolve_timeout = "1s";
                                    backend_timeout = "5s";
                                }; -- proxy
                            }
                        }; --rr
                        on_error = {
                            proxy = {
                                host = "localhost"; port = geo_fallback_backend_port;
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                backend_timeout = "5s";
                            }; -- proxy
                        }; -- on_error
                    }; -- balancer2
                }; -- geo
                proxy = {
                    host = "localhost"; port = backend_port;
                    connect_timeout = "1s"; resolve_timeout = "1s";
                    backend_timeout = "5s";
                }; -- proxy
            }; -- geobase
        }; -- accesslog
    }; -- http
}; -- instance
