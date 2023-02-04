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
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                headers_hasher = {
                    header_name = header_name_prev;
                    randomize_empty_match = false;
                    headers_hasher = {
                        header_name = header_name;
                        randomize_empty_match = randomize_empty_match;
                        surround = surround;
                        file_switch = file_switch;
                        combine_hashes = combine_hashes;
                        balancer2 = {
                            attempts = attempts or 1;
                            hashing = gen_backends(backends_count or 3);
                        }; -- balancer2
                    }; -- headers_hasher
                }; -- headers_hasher
            }; -- http
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance
