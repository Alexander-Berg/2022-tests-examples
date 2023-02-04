function prepare_blacklist(str)
    if not str then
        return nil
    end

    local retval = {}
    for w in string.gmatch(str, ",?([^,]+)") do
        retval[#retval + 1] = w
    end
    return retval
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    ipdispatch = {
        test = {
            http = {
                maxlen = 65536; maxreq = 65536;
                balancer2 = {
                    attempts = 3;
                    by_name_policy = {
                        name = "backend1";
                        unique_policy = {};
                    };
                    status_code_blacklist = prepare_blacklist(outer_status_code_blacklist);
                    return_last_5xx = outer_return_last_5xx;

                    rr = {
                        randomize_initial_state = false;
                        backend1 = {
                            balancer2 = {
                                attempts = 2;
                                status_code_blacklist = prepare_blacklist(inner_status_code_blacklist);
                                return_last_5xx = inner_return_last_5xx;
                                rr = {
                                    randomize_initial_state = false;
                                    {
                                        errordocument = {
                                            status = 500;
                                            content = "backend1";
                                        }; -- errordocument
                                    };
                                }; -- rr
                            }; -- balancer2
                        };
                        backend2 = {
                            balancer2 = {
                                attempts = 2;
                                status_code_blacklist = prepare_blacklist(inner_status_code_blacklist);
                                return_last_5xx = inner_return_last_5xx;
                                rr = {
                                    randomize_initial_state = false;
                                    {
                                        errordocument = {
                                            status = 501;
                                            content = "backend2";
                                        }; -- errordocument
                                    };
                                }; -- rr
                            }; -- balancer2
                        };
                    }; -- rr
                }; -- balancer
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
