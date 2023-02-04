function gen_balancer(amount_quorum, group)
    return {
        balancer = {
            attempts = 1;

            dynamic = {
                b1 = {
                    errordocument = {
                        status = 200;
                        content = group;
                    }; -- errordocument
                };
                max_pessimized_share = 0;
            };

            check_backends = {
                amount_quorum = amount_quorum;
                name = group;
                skip = true;
            };
        };
    };
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
                    balancer = {
                        attempts = 1;
                        check_backends = {
                            quorum = root_quorum;
                            amount_quorum = root_amount_quorum;
                            name = "root";
                            skip = true;
                        };
                        by_location = {
                            weights_file = weights_file;
                            preferred_location = preferred_location;
                            preferred_location_switch = preferred_location_switch;
                            id0 = gen_balancer(amount_quorum1, "id0");
                            id1 = gen_balancer(amount_quorum2, "id1");
                            id2 = gen_balancer(amount_quorum3, "id2");
                        }; -- by_location
                        on_error = {
                            errordocument = {
                                status = 200;
                                content = "on_error";
                            }; -- errordocument
                        }; -- on_error
                    }; -- balancer
                }; -- errorlog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
