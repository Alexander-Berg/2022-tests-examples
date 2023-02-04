if rr_count == nil then rr_count = 1; end
if first_weight == nil then first_weight = 2.0 ; end
if second_weight == nil then second_weight = 1.0 ; end

function gen_section(index)
    local route = "/" .. index;
    local subtree = {
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
    };

    if use_randomize_initial_state then
        subtree.rr.randomize_initial_state = corrowa and tonumber(corrowa) > 0;
    else
        subtree.rr.count_of_randomized_requests_on_weights_application = corrowa and tonumber(corrowa) or nil;
    end;

    subtree.unique_policy = {};
    return {
        route = route;
        balancer2 = subtree;
    };
end

function gen()
    result = {};
    for i = 0, rr_count - 1 do
        result["section_" .. i] = gen_section(i);
    end
    return result
end

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
                prefix_path_router = gen();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
