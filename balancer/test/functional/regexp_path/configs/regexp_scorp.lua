function val_or(val, default)
    if val ~= nil then
        return val
    else
        return default
    end
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
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
                regexp_path = {
                    scorp = {
                        pattern = val_or(scorp_pattern, "/scorp.*");
                        priority = val_or(scorp_priority, 1);
                        case_insensitive = val_or(scorp_insensitive, true);

                        errordocument = {
                            status = 200;
                            content = "scorp";
                        };
                    };
                    scorpions = {
                        pattern = val_or(scorpions_pattern, "/scorpions.*");
                        priority = val_or(scorpions_priority, 1);
                        case_insensitive = val_or(scorpions_insensitive, true);

                        errordocument = {
                            status = 200;
                            content = "scorpions";
                        };
                    };
                    default = {
                        errordocument = {
                            status = 200;
                            content = "default";
                        };
                    };
                }; -- regexp_path
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

