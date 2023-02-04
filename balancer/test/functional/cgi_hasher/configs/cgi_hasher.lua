function gen_backends(backend_count)
    local retval = {}
    for i = 1, backend_count do
        retval[i] = {
            errordocument = {
                status = "200";
                content = string.format("%d", i);
            };
        };
    end

    return retval
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- hhtp
        }; -- ipdispatch/admin
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                cgi_hasher = {
                    parameters = {
                        param_prev;
                    };
                    randomize_empty_match = false;
                    case_insensitive = false;
                    cgi_hasher = {
                        parameters = {
                            param1, param2, param3;
                        };
                        mode = mode;
                        case_insensitive = case_insensitive;
                        randomize_empty_match = randomize_empty_match;
                        combine_hashes = combine_hashes;
                        balancer2 = {
                            unique_policy = {};
                            attempts = attempts or 1;
                            hashing = gen_backends(backends_count or 10);
                        }; -- balancer
                    }; -- cgi_hasher
                }; -- cgi_hasher
            }; -- http
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance
