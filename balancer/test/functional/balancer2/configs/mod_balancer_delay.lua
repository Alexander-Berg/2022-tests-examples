backend_host = backend_host or "localhost"
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

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
                balancer2 = {
                    fast_503 = fast_503;
                    attempts = attempts;
                    fast_attempts = fast_attempts;
                    first_delay = first_delay;
                    delay_multiplier = delay_multiplier;
                    delay_on_fast = delay_on_fast;
                    max_random_delay = max_random_delay;
                    simple_policy = {};
                    rr = {
                        randomize_initial_state = false;
                        qwerty = {
                            proxy = {
                                host = backend_host; port = backend_port;
                                backend_timeout = backend_timeout;
                            }; -- proxy
                        };
                    }; -- rr
                }; -- balancer
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
