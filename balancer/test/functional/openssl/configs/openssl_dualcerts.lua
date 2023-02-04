ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
ciphers = "kEECDH+ECDSA+AESGCM:" .. ciphers

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

if no_ca == nil then old_ca = old_ca or file_path("old_ca.crt") else old_ca = nil end
old_cert = old_cert or file_path("old.crt");
old_priv = old_priv or file_path("old.key");
old_ocsp = old_ocsp or file_path("old_ocsp.0.der");

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
                ssl_sni = {
                    force_ssl = force_ssl;
                    contexts = {
                        default = {
                            cert = cert; priv = priv; ca = ca;
                            ciphers = ciphers;
                            ocsp = ocsp;
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
                            secondary = {
                                cert = old_cert; priv = old_priv; ca = old_ca;
                                ocsp = old_ocsp;
                                ocsp_file_switch = old_ocsp_file_switch;
                                events = {
                                    reload_ocsp_response = "secondary_default_reload_ocsp";
                                }; -- events
                            }; -- secondary
                        }; -- default
                    }; -- contexts
                    events = {
                        stats = 'ssl_sni_stats';
                    }; -- events
                    http = {
                        maxreq = 64 * 1024; maxlen = 64 * 1024;
                        errordocument = {
                            status = 200;
                            content = "Hello";
                        }; -- errordocument
                    }; -- http
                }; -- ssl_sni
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
