ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

port = port or 8085;
admin_port = admin_port or 8086;
timeout = timeout or "3s";

function file_path(file_name)
    return certs_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");

instance = {
    thread_mode = thread_mode; set_no_file = false;
    _coro_fail_on_error = 1;
    buffer = 0;
    workers = workers;
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB: "" ]]
    };
    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            regexp = {
                default = {
                    report = {
                        ranges = "0,100s";
                        uuid = "service_total";
                        just_storage = true;
                        errorlog = {
                            log_level = "ERROR";
                            ssl_sni = {
                                force_ssl = false;
                                contexts = {
                                    default = {
                                        cert = cert; priv = priv; ca = ca;
                                        ciphers = ciphers;
                                        timeout = timeout;
                                        ticket_keys_list = {
                                        }; -- ticket_keys_list
                                    }; -- default
                                }; -- contexts
                                [mod] = {
                                    http2 = {
                                        http = {
                                            maxreq = 64 * 1024; maxlen = 64 * 1024;
                                            errordocument = {
                                                status = 200;
                                            };
                                        }; -- http
                                    }; -- http2
                                }; -- mod
                            }; -- ssl_sni
                        }; -- errorlog
                    }; -- report
                }; -- default
            }; -- regexp
        }; -- test
    }; -- ipdispatch
}; -- instance
