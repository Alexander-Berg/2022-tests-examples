function gen_on_backend(name)
    return {
        errordocument = {
            status = 200;
            content = name;
        };
    };
end

function gen_off_backend(name)
    return {
        weight = -1.0;
        errordocument = {
            status = 500;
            content = name;
        };
    };
end


function gen_single_balancer()
    local rr = {
        randomize_initial_state = false;
        weights_file = weights_file;
    };

    for _, name in pairs({"first", "second", "thrid", "fourth", "fifth", "sixth"}) do
        rr[name] = gen_on_backend(name);
    end

    local balancer2 = {
        by_hash_policy = {
            unique_policy = {};
        }; -- by_hash_policy

        rr = rr; -- balancer2/rr
    }; -- balancer2

    return balancer2;
end

function gen_multiple_balancer()
    local single_balancer = gen_single_balancer();
    local rr = single_balancer.rr
    for _, name in pairs({"a", "b", "c", "j", "x", "y", "z", "0null", "seventh", "eigth"}) do
        rr[name] = gen_off_backend(name);
    end
    rr[1] = gen_off_backend("1");
    rr[2] = gen_off_backend("2");
    return single_balancer;
end


instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;
        ipdispatch = {
            admin = {
                ip = "localhost"; port = admin_port;
                admin = {};
            };
            default = {
                headers_hasher = {
                    header_name = "hash";
                    regexp_path = {
                        multi = {
                            pattern = "/multi";
                            balancer2 = gen_multiple_balancer();
                        };
                        single = {
                            pattern = "/single";
                            balancer2 = gen_single_balancer();
                        };
                        default = {
                            errordocument = {
                                status = 404;
                                content = "not found";
                            };
                        };
                    }; -- regexp_path
                }; -- headers_hasher
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
