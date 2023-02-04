if attempts == nil then attempts = 1; end
if mode == nil then mode = "request"; end
if mode == "subnet" then ip_header = "X-Forwarded-For"; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                headers_hasher = {
                    header_name = header_name_prev;
                    randomize_empty_match = false;
                    hasher = {
                        mode = mode;
                        take_ip_from = ip_header;
                        subnet_v4_mask = subnet_v4_mask;
                        subnet_v6_mask = subnet_v6_mask;
                        combine_hashes = combine_hashes;
                        balancer2 = {
                            attempts = attempts;
                            hashing = {
                                request = request;
                                delay = delay;
                                steady = steady;
                                {
                                    proxy = {
                                        host = "localhost"; port = backend1_port;
                                        connect_timeout = "5s"; backend_timeout = "5s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                };
                                {
                                    proxy = {
                                        host = "localhost"; port = backend2_port;
                                        connect_timeout = "5s"; backend_timeout = "5s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                };
                                {
                                    proxy = {
                                        host = "localhost"; port = backend3_port;
                                        connect_timeout = "5s"; backend_timeout = "5s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                };
                            }; -- hashing
                        }; -- balancer
                    }; -- hasher
                }; -- headers_hasher
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
