function gen_balancer()
    res = {
        attempts = 1;
        by_location = {
            weights_file = weights_file;
            preferred_location = preferred_location;
            preferred_location_switch = preferred_location_switch;
            id0 = {
                weight = 2.0;
                errordocument = {
                    status = 200;
                    content = "id 0";
                }; -- errordocument
            }, -- id0
            id1 = {
                weight = 1.0;
                errordocument = {
                    status = 200;
                    content = "id 1";
                }; -- errordocument
            }, -- id1
            id2 = {
                weight = 0.0;
                errordocument = {
                    status = 200;
                    content = "id 2";
                }; -- errordocument
            }, -- id2
        }; -- by_location
        on_error = {
            errordocument = {
                status = 200;
                content = "on_error";
            }; -- errordocument
        }; -- on_error
    }; -- balancer

    if policy == "by_name_from_header" then
        res["by_name_from_header_policy"] = {
            unique_policy = {};
            hints = {
                {hint = "id0"; backend = "id0";},
                {hint = "id1"; backend = "id1";},
                {hint = "id2"; backend = "id2";},
            };
        };
    elseif policy == "by_hash" then
        res["by_hash_policy"] = {
            unique_policy = {};
        };
    end

    return res
end

instance = {
    set_no_file = false;
    config_check = {
        quorums_file = quorums_file;
    }; -- config_check
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    };

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
                errorlog = {
                    log = errorlog;
                    headers_hasher = {
                        header_name = "hash";
                        balancer = gen_balancer();
                    }; -- headers_hasher
                }; -- errorlog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
