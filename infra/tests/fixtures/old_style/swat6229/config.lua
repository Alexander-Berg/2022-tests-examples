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


function get_geo(name, default_geo)
  default_geo = default_geo or "random"
  return name .. (DC or default_geo);
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
  buffer = 65536;
  maxconn = 4000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15220, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15220;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15220;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 15220;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "2a02:6b8::1:62";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "2a02:6b8::1:63";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15221;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15221;
      ip = get_ip_by_iproute("v6");
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        15220;
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
        15220;
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        matcher_map = {
          ru = {
            match_fsm = {
              host = "beta.mobsearch.yandex.ru";
              case_insensitive = true;
              surround = false;
            }; -- match_fsm
          }; -- ru
          ru_ping = {
            match_and = {
              {
                match_fsm = {
                  host = "beta.mobsearch.yandex.ru";
                  case_insensitive = true;
                  surround = false;
                }; -- match_fsm
              };
              {
                match_fsm = {
                  URI = "/ping";
                  case_insensitive = true;
                  surround = false;
                }; -- match_fsm
              };
              {
                match_fsm = {
                  upgrade = "[a-z]";
                  case_insensitive = true;
                  surround = true;
                }; -- match_fsm
              };
            }; -- match_and
          }; -- ru_ping
        }; -- matcher_map
        outgoing_codes = "200,400";
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          keepalive_timeout = "100s";
          keepalive_requests = 1000;
          keepalive_drop_probability = 0.667;
          events = {
            stats = "report";
          }; -- events
          errordocument = {
            status = 204;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
    https_section_443 = {
      ips = {
        "2a02:6b8::1:62";
        "2a02:6b8::1:63";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "2056551865920111747";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15221, "/place/db/www/logs");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            max_send_fragment = 1024;
            validate_cert_date = true;
            contexts = {
              default = {
                priority = 1;
                timeout = "100800s";
                disable_sslv3 = true;
                disable_tlsv1_3 = false;
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15221, "/place/db/www/logs");
                priv = get_private_cert_path("beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- default
            }; -- contexts
            http = {
              maxlen = 65536;
              maxreq = 65536;
              keepalive = true;
              no_keepalive_file = "./controls/keepalive_disabled";
              events = {
                stats = "report";
              }; -- events
              accesslog = {
                log = get_log_path("access_log", 15221, "/place/db/www/logs");
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
                  response_headers_if = {
                    if_has_header = "X-Yandex-STS";
                    erase_if_has_header = true;
                    create_header = {
                      ["Strict-Transport-Security"] = "max-age=600";
                    }; -- create_header
                    cookies = {
                      delete = ".*cookie1.*";
                      create = {
                        cookie2 = "value2";
                      }; -- create
                      create_weak = {
                        cookie3 = "value3";
                      }; -- create_weak
                      regexp = {
                        default = {
                          priority = 1;
                          regexp = {
                            exp_testing = {
                              priority = 2;
                              match_fsm = {
                                cgi = "(exp-testing=da|exp_confs=testing)";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                              headers = {
                                create = {
                                  ["X-L7-EXP-Testing"] = "true";
                                }; -- create
                                shared = {
                                  uuid = "2857957971822442156";
                                  exp_getter = {
                                    trusted = false;
                                    file_switch = "./controls/expgetter.switch";
                                    uaas = {
                                      report = {
                                        uuid = "expgetter";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        stats_eater = {
                                          balancer2 = {
                                            by_name_policy = {
                                              name = get_geo("bygeo_", "random");
                                              simple_policy = {};
                                            }; -- by_name_policy
                                            attempts = 1;
                                            rr = {
                                              bygeo_man = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 1;
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "usersplit-1.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3ae1:100:1101::1111"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "5ms";
                                                      backend_timeout = "10ms";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                }; -- balancer2
                                              }; -- bygeo_man
                                              bygeo_sas = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 1;
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "usersplit-2.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:6a21:100:47b::1111"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "5ms";
                                                      backend_timeout = "10ms";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                }; -- balancer2
                                              }; -- bygeo_sas
                                              bygeo_vla = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 1;
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "usersplit-3.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4fa5:10b:2909::1111"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "5ms";
                                                      backend_timeout = "10ms";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                }; -- balancer2
                                              }; -- bygeo_vla
                                            }; -- rr
                                            on_error = {
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 1;
                                                rr = {
                                                  unpack(gen_proxy_backends({
                                                    { "uaas.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::120"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "20ms";
                                                    backend_timeout = "30ms";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 1;
                                                    need_resolve = true;
                                                  }))
                                                }; -- rr
                                              }; -- balancer2
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- stats_eater
                                      }; -- report
                                    }; -- uaas
                                    shared = {
                                      uuid = "backends";
                                      stats_eater = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          weighted2 = {
                                            slow_reply_time = "1s";
                                            correction_params = {
                                              max_weight = 5.000;
                                              min_weight = 0.050;
                                              history_time = "100s";
                                              feedback_time = "300s";
                                              plus_diff_per_sec = 0.050;
                                              minus_diff_per_sec = 0.100;
                                            }; -- correction_params
                                            unpack(gen_proxy_backends({
                                              { "ws33-340.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9cb1"; };
                                              { "ws34-487.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9ffa"; };
                                              { "ws35-290.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8493"; };
                                              { "ws35-658.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8651"; };
                                              { "ws40-413.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81d0"; };
                                              { "ws40-449.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81e2"; };
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
                                          }; -- weighted2
                                        }; -- balancer2
                                      }; -- stats_eater
                                    }; -- shared
                                  }; -- exp_getter
                                }; -- shared
                              }; -- headers
                            }; -- exp_testing
                            default = {
                              priority = 1;
                              headers = {
                                create = {
                                  ["X-L7-EXP"] = "true";
                                }; -- create
                                shared = {
                                  uuid = "2857957971822442156";
                                }; -- shared
                              }; -- headers
                            }; -- default
                          }; -- regexp
                        }; -- default
                      }; -- regexp
                    }; -- cookies
                  }; -- response_headers_if
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_15221 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15221;
      }; -- ports
      shared = {
        uuid = "2056551865920111747";
      }; -- shared
    }; -- https_section_15221
  }; -- ipdispatch
}