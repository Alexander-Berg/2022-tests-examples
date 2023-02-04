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


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
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
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("port"), "/place/db/www/logs");
  admin_addrs = {
    {
      ip = "127.0.0.1";
      port = get_port_var("port");
    };
    {
      ip = "::1";
      port = get_port_var("port");
    };
  }; -- admin_addrs
  addrs = {
    {
      ip = "127.0.0.4";
      port = get_port_var("port");
    };
    {
      ip = "*";
      port = 443;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        get_port_var("port");
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
        get_port_var("port");
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
      }; -- report
    }; -- stats_storage
    https_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 443, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("arcanum.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-arcanum.yandex-team.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.arcanum.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.arcanum.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.arcanum.yandex-team.ru.key", "/dev/shm/balancer/priv");
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
              log = get_log_path("access_log", 443, "/place/db/www/logs");
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
                shared = {
                  uuid = "modules";
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
        get_port_var("port");
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 80, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 80, "/place/db/www/logs");
            report = {
              uuid = "http";
              refers = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              shared = {
                uuid = "modules";
                regexp = {
                  ["awacs-balancer-health-check"] = {
                    priority = 5;
                    match_fsm = {
                      URI = "/awacs-balancer-health-check";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    errordocument = {
                      status = 200;
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- ["awacs-balancer-health-check"]
                  slbping = {
                    priority = 4;
                    match_fsm = {
                      url = "/api/ping";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    stats_eater = {
                      balancer2 = {
                        unique_policy = {};
                        attempts = 1;
                        rr = {
                          weights_file = "./controls/slb_check.weights";
                          to_upstream = {
                            weight = 1.000;
                            errordocument = {
                              status = 200;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- to_upstream
                          switch_off = {
                            weight = -1.000;
                            errordocument = {
                              status = 503;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- switch_off
                        }; -- rr
                      }; -- balancer2
                    }; -- stats_eater
                  }; -- slbping
                  arcanum_redirect = {
                    priority = 3;
                    match_fsm = {
                      host = "arcanum";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "2152849320179494026";
                      headers = {
                        create = {
                          Location = "1";
                        }; -- create
                        rewrite = {
                          actions = {
                            {
                              regexp = ".*";
                              global = false;
                              literal = false;
                              case_insensitive = false;
                              header_name = "Location";
                              rewrite = "https://a.yandex-team.ru%{url}";
                            };
                          }; -- actions
                          errordocument = {
                            status = 301;
                            force_conn_close = false;
                            remain_headers = "Location";
                          }; -- errordocument
                        }; -- rewrite
                      }; -- headers
                    }; -- shared
                  }; -- arcanum_redirect
                  a_redirect = {
                    priority = 2;
                    match_fsm = {
                      host = "a";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "2152849320179494026";
                    }; -- shared
                  }; -- a_redirect
                  default = {
                    priority = 1;
                    threshold = {
                      lo_bytes = 734003;
                      hi_bytes = 838860;
                      recv_timeout = "1s";
                      pass_timeout = "10s";
                      balancer2 = {
                        watermark_policy = {
                          lo = 0.100;
                          hi = 0.100;
                          params_file = "./controls/watermark_policy.params_file";
                          unique_policy = {};
                        }; -- watermark_policy
                        attempts = 3;
                        rr = {
                          unpack(gen_proxy_backends({
                            { "man1-9001-man-arcanum-prod-22770.gencfg-c.yandex.net"; 40000; 1.000; "2a02:6b8:c09:d8e:10b:7bed:0:58f2"; };
                            { "sas1-0311-sas-arcanum-prod-14670.gencfg-c.yandex.net"; 40000; 1.000; "2a02:6b8:c08:10a9:10b:7bf0:0:394e"; };
                            { "vla1-1974-vla-arcanum-prod-28030.gencfg-c.yandex.net"; 40000; 1.000; "2a02:6b8:c0d:921:10b:7bf2:0:6d7e"; };
                          }, {
                            resolve_timeout = "10ms";
                            connect_timeout = "70ms";
                            backend_timeout = "65s";
                            fail_on_5xx = true;
                            http_backend = true;
                            buffering = false;
                            keepalive_count = 1;
                            need_resolve = true;
                            keepalive_timeout = "65s";
                          }))
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- threshold
                  }; -- default
                }; -- regexp
              }; -- shared
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}