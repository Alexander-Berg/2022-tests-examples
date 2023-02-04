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

if verify_peer == "false" then verify_peer = false end
if verify_peer == "true" then verify_peer = true end

if verify_once == "false" then verify_once = false end
if verify_once == "true" then verify_once = true end

if fail_if_no_peer_cert == "false" then fail_if_no_peer_cert = false end
if fail_if_no_peer_cert == "true" then fail_if_no_peer_cert = true end

ja3_func = nil
if ja3_enabled ~= nil and ja3_enabled then ja3_func = "ja3" end 

instance = {
    thread_mode = thread_mode; set_no_file = false;

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
            errorlog = {
                log = errorlog;
                ssl_sni = {
                    ja3_enabled = ja3_enabled;
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
                    }; -- contexts
                    events = {
                        stats = 'ssl_sni_stats';
                    }; -- events
                    http = {
                        maxreq = 64 * 1024; maxlen = 64 * 1024;
                        headers = {
                            create_func = {
                                ["cn"] = "ssl_client_cert_cn";
                                ["subj"] = "ssl_client_cert_subject";
                                ["handshake"] = "ssl_handshake_info";
                                ["serial"] = "ssl_client_cert_serial_number";
                                ["ja3"] = ja3_func;
                                ["ticket-name"] = "ssl_ticket_name";
                                ["ticket-iv"] = "ssl_ticket_iv";
                            }; -- create_func
                            proxy = {
                                host = "localhost"; port = backend_port;
                                backend_timeout = "5s";
                                connect_timeout = "1s"; resolve_timeout = "1s";
                            }; -- proxy
                        }; -- headers
                    }; -- http
                }; -- ssl_sni
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
