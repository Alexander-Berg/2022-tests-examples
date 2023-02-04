default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function gen_proxy_backends(backends, proxy_options)
  local result = {}

  for index, backend in pairs(backends) do
    local proxy = {
      host = backend[1] or backend['host'];
      port = backend[2] or backend['port'];
      cached_ip = backend[4] or backend['cached_ip'];
    };

    if proxy_options ~= nil then
      for optname, optvalue in pairs(proxy_options) do
        proxy[optname] = optvalue
      end
    end

    result[index] = {
      weight = backend[3] or backend['weight'];
      proxy = proxy;
    };
  end

  if next(result) == nil then
    error("backends list is empty")
  end

  return result
end


function get_ca_cert_path(name, default_ca_cert_dir)
  default_ca_cert_dir = default_ca_cert_dir or "/dev/shm/balancer/priv"
  return (ca_cert_dir or default_ca_cert_dir) .. "/" .. name;
end


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
end


function get_ip_by_iproute(addr_family)
  if disable_external then
    if addr_family == "v4" then
      return "127.1.1.1"
    elseif addr_family == "v6" then
      return "127.2.2.2"
    else
      error("invalid parameter")
    end
  end

  local ipcmd
  if addr_family == "v4" then
    ipcmd = "ip route get 77.88.8.8 2>/dev/null| awk '/src/ {print $NF}'"
  elseif addr_family == "v6" then
    ipcmd = "ip route get 2a00:1450:4010:c05::65 2>/dev/null | grep -oE '2a02[:0-9a-f]+' | tail -1"
  else
    error("invalid parameter")
  end
  local handler = io.popen(ipcmd)
  local ip = handler:read("*l")
  handler:close()
  if ip == nil or ip == "" or ip == "proto" then
    return "127.0.0.2"
  end
  return ip
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
end


function get_port_var(name, offset, default)
  value = get_int_var(name, default)
  if value == nil then
    error("Neither port variable \"" .. name .. "\" nor default port is specified.")
  end
  if value < 0 or value > 65535 then
    error("Variable \"" .. name .. "\" is not a valid port: " .. value)
  end
  if offset ~= nil then
    value = value + offset
  end
  return value
end


function get_private_cert_path(name, default_private_cert_dir)
  default_private_cert_dir = default_private_cert_dir or "/dev/shm/balancer/priv"
  return (private_cert_dir or default_private_cert_dir) .. "/" .. name;
end


function get_public_cert_path(name, default_public_cert_dir)
  default_public_cert_dir = default_public_cert_dir or "/dev/shm/balancer"
  return (public_cert_dir or default_public_cert_dir) .. "/" .. name;
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


instance = {
  workers = 0;
  maxconn = 4000;
  buffer = 1048576;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(300, 360, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("admin_port"), "/place/db/www/logs/");
  admin_addrs = {
    {
      ip = "127.0.0.1";
      port = get_port_var("admin_port");
    };
    {
      ip = "::1";
      port = get_port_var("admin_port");
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 8180;
      ip = "127.0.0.4";
    };
    {
      port = get_port_var("port");
      ip = "2a02:6b8:0:3400::1:16";
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:16";
      port = get_port_var("port", 8);
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = get_port_var("port");
      ip = "2a02:6b8:0:3400::1:17";
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:17";
      port = get_port_var("port", 8);
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = get_ip_by_iproute("v4");
      port = get_int_var("local_port", 0);
    };
    {
      ip = get_ip_by_iproute("v6");
      port = get_int_var("local_port", 0);
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        get_port_var("admin_port");
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {};
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        8180;
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("test", get_int_var("test", 0), "");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            errordocument = {
              status = 204;
              force_conn_close = false;
            }; -- errordocument
          }; -- http
        }; -- errorlog
      }; -- report
    }; -- stats_storage
    section_1_get_port_var_port = {
      ips = {
        "2a02:6b8:0:3400::1:16";
        "2a02:6b8:0:3400::1:17";
      }; -- ips
      ports = {
        get_port_var("port");
        get_port_var("port", 8);
      }; -- ports
      shared = {
        uuid = "16117091372397273";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", get_port_var("port"), "/place/db/www/logs/");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            http2_alpn_file = "./controls/http2_enable.ratefile";
            http2_alpn_freq = 1.000;
            contexts = {
              default = {
                priority = 1;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                ca = get_ca_cert_path("InternalYandexCA", "./");
                secrets_log = get_log_path("hamster_secrets", get_port_var("port"), "./");
                log = get_log_path("ssl_sni", get_port_var("port"), "/place/db/www/logs/");
                priv = get_private_cert_path("hamster.yandex.tld.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-hamster.yandex.tld.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.hamster.yandex.tld.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.hamster.yandex.tld.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.hamster.yandex.tld.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- default
            }; -- contexts
            http2 = {
              goaway_debug_data_enabled = false;
              debug_log_enabled = false;
              events = {
                stats = "report";
              }; -- events
              http = {
                maxlen = 65536;
                maxreq = 65536;
                keepalive = true;
                no_keepalive_file = "./controls/keepalive_disabled";
                events = {
                  stats = "report";
                }; -- events
                accesslog = {
                  log = get_log_path("access_log", get_port_var("port"), "/place/db/www/logs/");
                  report = {
                    uuid = "https";
                    refers = "service_total";
                    ranges = get_str_var("default_ranges");
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    regexp = {
                      yandex = {
                        priority = 2;
                        match_fsm = {
                          host = "(.*\\.)?(xn----7sbhgfw0a0bcg8l1a\\.xn--p1ai|xn--80aefebu0a0bbh8l\\.xn--p1ai|xn--d1acpjx3f\\.xn--p1ai|2yandex\\.ru|jandeks\\.com\\.tr|jandex\\.com\\.tr|kremlyandex\\.ru|video-yandex\\.ru|videoyandex\\.ru|wwwyandex\\.ru|xyyandex\\.net|ya-plus-plus\\.ru|ya-plusplus\\.ru|ya\\.nu|ya\\.rs|ya\\.ru|ya\\.tel|ya\\.tm|yanclex\\.ru|yandeks\\.com|yandeks\\.com\\.tr|yandes\\.ru|yandesk\\.com|yandesk\\.org|yandesk\\.ru|yandex-plus-plus\\.ru|yandex-plusplus\\.ru|yandex-rambler\\.ru|yandex-video\\.ru|yandex\\.asia|yandex\\.az|yandex\\.biz\\.tr|yandex\\.by|yandex\\.co\\.il|yandex\\.co\\.no|yandex\\.com|yandex\\.com\\.de|yandex\\.com\\.kz|yandex\\.com\\.ru|yandex\\.com\\.tr|yandex\\.com\\.ua|yandex\\.de|yandex\\.dk|yandex\\.do|yandex\\.ee|yandex\\.es|yandex\\.ie|yandex\\.in|yandex\\.info\\.tr|yandex\\.it|yandex\\.jobs|yandex\\.jp\\.net|yandex\\.kg|yandex\\.kz|yandex\\.lt|yandex\\.lu|yandex\\.lv|yandex\\.md|yandex\\.mobi|yandex\\.mx|yandex\\.name|yandex\\.net|yandex\\.net\\.ru|yandex\\.no|yandex\\.nu|yandex\\.org|yandex\\.pl|yandex\\.pt|yandex\\.qa|yandex\\.ro|yandex\\.rs|yandex\\.ru|yandex\\.sk|yandex\\.st|yandex\\.sx|yandex\\.tj|yandex\\.tm|yandex\\.ua|yandex\\.uz|yandex\\.web\\.tr|yandex\\.xxx|yandexbox\\.ru|yandexmedia\\.ru|yandexplusplus\\.ru|yandexvideo\\.ru|yandfex\\.ru|yandx\\.ru|yaplusplus\\.ru|yandex\\.com\\.ge|yandex\\.fr|yandex\\.az|yandex\\.uz|yandex\\.com\\.am|yandex\\.co\\.il|yandex\\.kg|yandex\\.lt|yandex\\.lv|yandex\\.md|yandex\\.tj|yandex\\.tm|yandex\\.ee)(:\\d+|\\.)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        hasher = {
                          mode = "subnet";
                          subnet_v4_mask = 32;
                          subnet_v6_mask = 128;
                          headers = {
                            create_func = {
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Req-Id"] = "reqid";
                              ["X-Source-Port-Y"] = "realport";
                              ["X-Start-Time"] = "starttime";
                              ["X-Yandex-RandomUID"] = "yuid";
                            }; -- create_func
                            create = {
                              ["X-Yandex-HTTPS"] = "yes";
                            }; -- create
                            response_headers = {
                              delete = "Strict-Transport-Security|X-Yandex-Report-Type";
                              create_weak = {
                                ["X-Content-Type-Options"] = "nosniff";
                                ["X-XSS-Protection"] = "1; mode=block";
                              }; -- create_weak
                              rpcrewrite = {
                                url = "/proxy";
                                dry_run = false;
                                host = "bolver.yandex-team.ru";
                                rpc_success_header = "X-Metabalancer-Answered";
                                file_switch = "./controls/disable_rpcrewrite_module";
                                errordocument = {
                                  status = 200;
                                  force_conn_close = false;
                                }; -- errordocument
                                on_rpc_error = {
                                  errordocument = {
                                    status = 500;
                                    force_conn_close = false;
                                    content = "Failed to rewrite request using RPC";
                                  }; -- errordocument
                                }; -- on_rpc_error
                                rpc = {
                                  report = {
                                    uuid = "rpcrewrite-backend";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    stats_eater = {
                                      balancer2 = {
                                        simple_policy = {};
                                        attempts = 3;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "bolver.yandex-team.ru"; 80; 1.000; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "150ms";
                                            backend_timeout = "10s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- stats_eater
                                  }; -- report
                                }; -- rpc
                              }; -- rpcrewrite
                            }; -- response_headers
                          }; -- headers
                        }; -- hasher
                      }; -- yandex
                      default = {
                        priority = 1;
                        errordocument = {
                          status = 406;
                          force_conn_close = true;
                        }; -- errordocument
                      }; -- default
                    }; -- regexp
                  }; -- report
                }; -- accesslog
              }; -- http
            }; -- http2
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- section_1_get_port_var_port
    section_1_get_int_var_local_port_0 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        get_int_var("local_port", 0);
      }; -- ports
      shared = {
        uuid = "16117091372397273";
      }; -- shared
    }; -- section_1_get_int_var_local_port_0
  }; -- ipdispatch
}