ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

port = port or 8083;
admin_port = admin_port or 8084;
timeout = timeout or "3s";

function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

if no_ca == nil then ca = ca or file_path("root_ca.crt") else ca = nil end
cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ocsp = ocsp or file_path("default_ocsp.0.der");
ticket = ticket or file_path("default_ticket.0.key");

instance = {
    thread_mode = thread_mode; set_no_file = false;
    workers = workers;
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB: "" ]]
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
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
            errorlog = {
                log = errorlog;
                debug = {
                    client_write_delay = client_write_delay;
                    client_read_delay = client_read_delay;
                    client_write_size = client_write_size;
                    client_read_size = client_read_size;
                    ssl_sni = {
                        force_ssl = force_ssl;
                        max_send_fragment = max_send_fragment;
                        contexts = {
                            default = {
                                cert = cert; priv = priv; ca = ca;
                                ocsp = ocsp; ciphers = ciphers;
                                timeout = timeout;
                                log = log;
                                ocsp_file_switch = ocsp_file_switch;
                                ticket_keys_list = {
                                    {
                                        keyfile = ticket;
                                        priority = 1;
                                    },
                                }; -- ticket_keys_list
                                events = {
                                    reload_ocsp_response = "default_reload_ocsp";
                                    reload_ticket_keys = "default_reload_tickets";
                                }; -- events
                            }; -- default
                        }; -- contexts
                        events = {
                            stats = 'ssl_sni_stats';
                        }; -- events
                        http = {
                            maxreq = 64 * 1024; maxlen = 64 * 1024;
                            errordocument = {
                                status = 200;
                                content = content or "Hello";
                            }; -- errordocument
                        }; -- http
                    }; -- ssl_sni
                }; -- debug
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
