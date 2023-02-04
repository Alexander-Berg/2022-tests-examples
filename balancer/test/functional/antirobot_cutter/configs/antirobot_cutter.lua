cert_dir = cert_dir or "./";
function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");

instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_port

    thread_mode = thread_mode; set_no_file = false;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                cutter = {
                    bytes = cutter_bytes;
                    timeout = cutter_timeout;

                    antirobot = {
                        cut_request = true;
                        cut_request_bytes = antirobot_cut_bytes;
                        checker = {
                            proxy = {
                                host = "localhost"; port = antirobot_backend_port;
                                connect_timeout = "0.3s"; backend_timeout = antirobot_timeout;
                                resolve_timeout = "0.3s";
                                keepalive_count = antirobot_keepalive_count;
                            };
                        }; -- antirobot/checker
                        module = {
                            proxy = {
                                host = "localhost"; port = main_backend_port;
                                connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                resolve_timeout = "0.3s";
                                keepalive_count = backend_keepalive_count;
                            }; -- proxy
                        }; -- antirobot/module
                    }; -- antirobot
                }; -- cutter
            }; -- http
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance

function add_ssl(use_ssl)
    if use_ssl then
        local default_section = instance.ipdispatch.default;
        local ssl_sni = {
            contexts = {
                default = {
                    cert = cert; priv = priv; ca = ca;
                    ciphers = ciphers;
                };
            };
        };

        for k, v in pairs(default_section) do
            ssl_sni[k] = v;
        end

        instance.ipdispatch.default["ssl_sni"] = ssl_sni;

    end

    return instance
end

add_ssl(use_ssl)
