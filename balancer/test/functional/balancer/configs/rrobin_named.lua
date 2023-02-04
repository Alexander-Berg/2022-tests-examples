if first_weight == nil then first_weight = 2.0 ; end
if second_weight == nil then second_weight = 1.0 ; end

instance = {
    set_no_file = false;
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
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                balancer2 = {
                    attempts = 1;
                    rr = {
                        randomize_initial_state = false;
                        weights_file = weights_file;
                        first = {
                            weight = first_weight;
                            errordocument = {
                                status = 200;
                                content = "id 0";
                            }; -- errordocument
                        }, -- backend 0
                        second = {
                            weight = second_weight;
                            errordocument = {
                                status = 200;
                                content = "id 1";
                            }; -- errordocument
                        }, -- backend 1
                    }; -- rr
                    on_error = {
                        errordocument = {
                            status = 200;
                            content = "on_error";
                        }; -- errordocument
                    }; -- on_error
                }; -- balancer2
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

mod_http = instance.ipdispatch.test.http;
mod_b = mod_http.balancer2;

if use_randomize_initial_state then
    mod_b.rr.randomize_initial_state = corrowa and tonumber(corrowa) > 0;
else
    mod_b.rr.count_of_randomized_requests_on_weights_application = corrowa and tonumber(corrowa) or nil;
end;

mod_http.balancer2 = {
    unique_policy = {};
};

for k, v in pairs(mod_b) do
    mod_http.balancer2[k] = v;
end

mod_http.balancer = nil;
