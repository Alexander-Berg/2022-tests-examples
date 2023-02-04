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

if no_ca == nil then other_ca = other_ca or file_path("other_root_ca.crt") else other_ca = nil end
other_cert = other_cert or file_path("other.crt");
other_priv = other_priv or file_path("other.key");
other_ocsp = other_ocsp or file_path("other_ocsp.0.der");
other_ticket = other_ticket or file_path("other_ticket.0.key");

if verify_peer == "false" then verify_peer = false end
if verify_peer == "true" then verify_peer = true end

if verify_once == "false" then verify_once = false end
if verify_once == "true" then verify_once = true end

if fail_if_no_peer_cert == "false" then fail_if_no_peer_cert = false end
if fail_if_no_peer_cert == "true" then fail_if_no_peer_cert = true end

if erase_default_client == "true" then erase_default_client = true end
if erase_other_client == "true" then erase_other_client = true end

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
                            ocsp = ocsp; ciphers = ciphers;
                            timeout = timeout;
                            log = log;
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
                            client = {
                                verify_depth = verify_depth;
                                verify_peer = verify_peer;
                                verify_once = verify_once;
                                fail_if_no_peer_cert = fail_if_no_peer_cert;
                                crl = crl;
                            }; -- client
                        }; -- default
                        other = {
                            servername = {
                                servername_regexp = "other.yandex.ru";
                            }; -- servername
                            cert = other_cert; priv = other_priv; ca = other_ca;
                            ocsp = other_ocsp; ciphers = ciphers;
                            timeout = timeout;
                            log = log;
                            ticket_keys_list = {
                                {
                                    keyfile = other_ticket;
                                    priority = 1;
                                },
                            }; -- ticket_keys_list
                            events = {
                                reload_ocsp_response = "other_reload_ocsp";
                                reload_ticket_keys = "other_reload_tickets";
                            }; -- events
                            client = {
                                verify_depth = verify_depth;
                                verify_peer = verify_peer;
                                verify_once = verify_once;
                                fail_if_no_peer_cert = fail_if_no_peer_cert;
                                crl = other_crl;
                            }; -- client
                        }; -- other
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

if erase_default_client then
    instance.ipdispatch.test.errorlog.ssl_sni.contexts.default.client = nil;
end

if erase_other_client then
    instance.ipdispatch.test.errorlog.ssl_sni.contexts.other.client = nil;
end
