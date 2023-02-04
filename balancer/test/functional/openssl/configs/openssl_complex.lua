ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

port = port or 8081;
admin_port = admin_port or 8082;
timeout = timeout or "3s";

default_reload_ocsp = default_reload_ocsp or "default_reload_ocsp"
default_reload_tickets = default_reload_tickets or "default_reload_tickets"
default_force_reload_tickets = default_force_reload_tickets or "default_force_reload_tickets"

detroit_reload_ocsp = detroit_reload_ocsp or "detroit_reload_ocsp"
detroit_reload_tickets = detroit_reload_tickets or "detroit_reload_tickets"
detroit_server_name = detroit_server_name or "detroit\\.yandex\\.ru"

vegas_reload_ocsp = vegas_reload_ocsp or "vegas_reload_ocsp"
vegas_reload_tickets = vegas_reload_tickets or "vegas_reload_tickets"
vegas_server_name = vegas_server_name or "vegas\\.yandex\\.ru"
if log_ciphers_stats == nil then log_ciphers_stats = false; end

tickets_validate = tickets_validate or false

early_data = early_data or false
default_item_early_data = default_item_early_data or false
alpn_freq = alpn_freq or 0

function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

function gen_ssl_protocols(protocols)
    if protocols == nil then
        return nil;
    end

    vals = {}
    for i in string.gmatch(protocols, "%S+") do
        table.insert(vals, i)
    end
    return vals
end

function make_default_context(name, log, secrets_log, tickets_count)
    local result = {
        cert = file_path(name .. ".crt");
        priv = file_path(name .. ".key");
        ca = file_path("root_ca.crt");
        ocsp = file_path(name .. "_ocsp.0.der");
        ciphers = ciphers;
        timeout = timeout;
        log = log;
        secrets_log = secrets_log;
        secrets_log_freq = secrets_log_freq;
        secrets_log_freq_file = secrets_log_freq_file;
        ssl_protocols = gen_ssl_protocols(ssl_protocols);
        ticket_keys_list = {};
        ticket_keys_validate = tickets_validate;
        log_ciphers_stats = log_ciphers_stats;
        earlydata_enabled = default_item_early_data;
        events = {
            reload_ocsp_response = _G[name .. "_reload_ocsp"];
            reload_ticket_keys = _G[name .. "_reload_tickets"];
            force_reload_ticket_keys = _G[name .. "_force_reload_tickets"];
        };
    };
    for i = 0, tickets_count - 1 do
        table.insert(result.ticket_keys_list, {
            keyfile = file_path(name .. "_ticket." .. i .. ".key");
            priority = _G[name .. "_ticket_prio_" .. i] or tickets_count - i;
        })
    end
    return result
end

function make_context(name, log, secrets_log, tickets_count, priority)
    local result = make_default_context(name, log, secrets_log, tickets_count)
    result["priority"] = priority;
    result["servername"] = { servername_regexp = _G[name .. "_server_name"]; };
    return result
end

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
                    http2_alpn_freq = alpn_freq;
                    earlydata_enabled = early_data;
                    force_ssl = force_ssl;
                    contexts = {
                        default = make_default_context("default", default_log, default_secrets_log, 3);
                        detroit = make_context("detroit", detroit_log, detroit_secrets_log, 1, 2);
                        vegas = make_context("vegas", vegas_log, vegas_secrets_log, 1, 1);
                    }; -- contexts
                    events = {
                        stats = 'ssl_sni_stats';
                    }; -- events
                    http2 = {
                        http = {
                            maxreq = 64 * 1024; maxlen = 64 * 1024;
                            response_headers = {
                                create_func = {
                                    ["early-data"] = "ssl_early_data";
                                };
                                prefix_path_router = {
                                    early = {
                                        route = "/early";
                                        errordocument = {
                                            status = 200;
                                            content = "Early";
                                        }; -- errordocument
                                    }; -- early
                                    default = {
                                        errordocument = {
                                            status = 200;
                                            content = "Hello";
                                        }; -- errordocument
                                    }; -- default
                                }; -- prefix_path_router
                            }; -- response_headers
                        }; -- http
                    }; -- http2
                }; -- ssl_sni
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
