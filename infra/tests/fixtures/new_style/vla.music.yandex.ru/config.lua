default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function _call_func_providers(overridable_func_names)
  for _, func_name in pairs(overridable_func_names) do
    local func_provider_path = _G[func_name .. "_provider"]
    if func_provider_path ~= nil then
      local env = {}
      setmetatable(env, {__index = _G})
      local provider, err = loadfile(func_provider_path, nil, env)
      if provider == nil then
        error(string.format('Failed to import provider "%s": %s', func_provider_path, err))
      end
      ok, rv = pcall(provider)
      if ok then
        if type(rv) ~= 'function' then
          error(string.format('Provider "%s" must return a function, not %s.', func_provider_path, type(rv)))
        end
        _G["do_" .. func_name] = rv
      else
        error(string.format('Provider "%s" failed: %s', func_provider_path, rv))
      end
    end
  end
end


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


function get_str_env_var(name, default)
  rv = os.getenv(name)
  if rv == nil then
    if default == nil then
      error(string.format('Environment variable "%s" is not set.', name))
    else
      return default
    end
  else
    return rv
  end
end


function get_str_var(name, default)
  return _G[name] or default
end


function do_get_workers()
  -- actual get_workers() implementation, can be overridden
  value = _G["workers"]
  if value == nil then
    error('Variable "workers" is not specified.')
  end
  int_value = tonumber(value)
  if int_value == nil then
    error('Could not cast variable "workers" to a number.')
  end
  return int_value
end


function get_workers()
  value = do_get_workers()
  if type(value) ~= 'number' then
    error(string.format('Provided get_workers() implementation must return a number, not %s.', type(value)))
  end
  if value < 0 or value % 1 ~= 0 then
    error(string.format('Provided get_workers() implementation must return a non-negative integer, not %s', value))
  end
  return value
end


_call_func_providers({
  "get_workers";
})


instance = {
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 14540, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 14540;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 14540;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 14540;
      ip = "127.0.0.4";
    };
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14540;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14540;
      ip = get_ip_by_iproute("v6");
    };
    {
      ip = "*";
      port = 443;
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14541;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14541;
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
        14540;
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
        14540;
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
    http_section_80 = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "7793887642305716874";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14540, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 14540, "/place/db/www/logs");
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
                headers = {
                  create_func = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Forwarded-For-Y"] = "realip";
                    ["X-Real-IP"] = "realip";
                    ["X-Scheme"] = "scheme";
                    ["X-Source-Port-Y"] = "realport";
                    ["X-Start-Time"] = "starttime";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Request-Id"] = "reqid";
                  }; -- create_func_weak
                  create = {
                    ["X-Yandex-HTTP"] = "yes";
                    ["X-Yandex-L7"] = "yes";
                  }; -- create
                  log_headers = {
                    name_re = "X-Request-Id";
                    response_headers = {
                      create_weak = {
                        ["X-Content-Type-Options"] = "nosniff";
                        ["X-XSS-Protection"] = "1; mode=block";
                      }; -- create_weak
                      regexp = {
                        ["awacs-balancer-health-check"] = {
                          priority = 3;
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
                          priority = 2;
                          match_fsm = {
                            url = "/ping(\\.xml)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "1683180570280210434";
                          }; -- shared
                        }; -- slbping
                        default = {
                          priority = 1;
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
                                  rewrite = "https://%{host}%{url}";
                                };
                              }; -- actions
                              regexp = {
                                unsafe_methods = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "(DELETE|PATCH|POST|PUT).*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  errordocument = {
                                    status = 307;
                                    force_conn_close = false;
                                    remain_headers = "Location";
                                  }; -- errordocument
                                }; -- unsafe_methods
                                default = {
                                  priority = 1;
                                  errordocument = {
                                    status = 301;
                                    force_conn_close = false;
                                    remain_headers = "Location";
                                  }; -- errordocument
                                }; -- default
                              }; -- regexp
                            }; -- rewrite
                          }; -- headers
                        }; -- default
                      }; -- regexp
                    }; -- response_headers
                  }; -- log_headers
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_14540 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14540;
      }; -- ports
      shared = {
        uuid = "7793887642305716874";
      }; -- shared
    }; -- http_section_14540
    https_section_443 = {
      ips = {
        "*";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "8636338677327858807";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14541, "/place/db/www/logs");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            ja3_enabled = true;
            contexts = {
              default = {
                priority = 1;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14541, "/place/db/www/logs");
                priv = get_private_cert_path("music.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-music.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.music.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.music.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.music.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 14541, "/place/db/www/logs");
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
                  response_headers = {
                    create = {
                      ["Strict-Transport-Security"] = "max-age=31536000";
                    }; -- create
                    create_weak = {
                      ["X-Content-Type-Options"] = "nosniff";
                      ["X-XSS-Protection"] = "1; mode=block";
                    }; -- create_weak
                    regexp = {
                      ["awacs-balancer-health-check"] = {
                        priority = 8;
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
                        priority = 7;
                        match_fsm = {
                          url = "/ping(\\.xml)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "1683180570280210434";
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
                        }; -- shared
                      }; -- slbping
                      https_ya_redirect = {
                        priority = 6;
                        match_fsm = {
                          host = "music.ya.ru";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "ya_redirect";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
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
                                  rewrite = "https://music.yandex.ru/pay?from=short";
                                };
                              }; -- actions
                              errordocument = {
                                status = 302;
                                force_conn_close = false;
                                remain_headers = "Location";
                              }; -- errordocument
                            }; -- rewrite
                          }; -- headers
                        }; -- report
                      }; -- https_ya_redirect
                      https_yandex_redirect = {
                        priority = 5;
                        match_fsm = {
                          host = "music.yandex";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "yandex_redirect";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
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
                                  rewrite = "https://music.yandex.ru%{url}";
                                };
                              }; -- actions
                              errordocument = {
                                status = 302;
                                force_conn_close = false;
                                remain_headers = "Location";
                              }; -- errordocument
                            }; -- rewrite
                          }; -- headers
                        }; -- report
                      }; -- https_yandex_redirect
                      https_yandex_uz_redirect = {
                        priority = 4;
                        match_fsm = {
                          host = "music.yandex.uz";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "yandex_redirect_uz";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
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
                                  rewrite = "https://music.yandex.com%{url}";
                                };
                              }; -- actions
                              errordocument = {
                                status = 302;
                                force_conn_close = false;
                                remain_headers = "Location";
                              }; -- errordocument
                            }; -- rewrite
                          }; -- headers
                        }; -- report
                      }; -- https_yandex_uz_redirect
                      ["https_mts-admin_music_yandex_ru"] = {
                        priority = 3;
                        match_fsm = {
                          host = "mts-admin.music.yandex.ru";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "mts_admin_req";
                          headers = {
                            create_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Real-IP"] = "realip";
                              ["X-Request-Id"] = "reqid";
                              ["X-Scheme"] = "scheme";
                              ["X-Source-Port-Y"] = "realport";
                              ["X-Start-Time"] = "starttime";
                            }; -- create_func
                            create = {
                              ["X-Antirobot-Service-Y"] = "music";
                              ["X-Yandex-HTTPS"] = "yes";
                              ["X-Yandex-L7"] = "yes";
                            }; -- create
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                mts_admin_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_mts_admin_to_sas";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      watermark_policy = {
                                        lo = 0.005;
                                        hi = 0.020;
                                        params_file = "./controls/watermark_policy.params_file";
                                        unique_policy = {};
                                      }; -- watermark_policy
                                      attempts = 1;
                                      connection_attempts = 2;
                                      active = {
                                        delay = "3s";
                                        request = "GET /ping HTTP/1.0\nHost: mts-admin.music.yandex.ru\n\n";
                                        steady = true;
                                        unpack(gen_proxy_backends({
                                          { "music-stable-export-sas-1.sas.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1b:3290:10d:5cc7:7029:0"; };
                                          { "music-stable-export-sas-2.sas.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1b:3107:10d:5cc7:6d01:0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = true;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "409";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- mts_admin_sas
                                mts_admin_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_mts_admin_to_man";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      watermark_policy = {
                                        lo = 0.005;
                                        hi = 0.020;
                                        params_file = "./controls/watermark_policy.params_file";
                                        unique_policy = {};
                                      }; -- watermark_policy
                                      attempts = 1;
                                      connection_attempts = 2;
                                      active = {
                                        delay = "3s";
                                        request = "GET /ping HTTP/1.0\nHost: mts-admin.music.yandex.ru\n\n";
                                        steady = true;
                                        unpack(gen_proxy_backends({
                                          { "music-stable-export-man-1.man.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c0b:6e15:10d:5cc9:c370:0"; };
                                          { "music-stable-export-man-2.man.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1a:31a6:10d:5cc9:1994:0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = true;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "409";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- mts_admin_man
                                mts_admin_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_mts_admin_to_vla";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      watermark_policy = {
                                        lo = 0.005;
                                        hi = 0.020;
                                        params_file = "./controls/watermark_policy.params_file";
                                        unique_policy = {};
                                      }; -- watermark_policy
                                      attempts = 1;
                                      connection_attempts = 2;
                                      active = {
                                        delay = "3s";
                                        request = "GET /ping HTTP/1.0\nHost: mts-admin.music.yandex.ru\n\n";
                                        steady = true;
                                        unpack(gen_proxy_backends({
                                          { "music-stable-export-vla-1.vla.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c0d:360c:10d:5cc5:efb1:0"; };
                                          { "music-stable-export-vla-2.vla.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1d:1c82:10d:5cc5:2ee0:0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = true;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "409";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- mts_admin_vla
                              }; -- rr
                              on_error = {
                                report = {
                                  uuid = "onerror_mts_admin";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      mts_admin_sas = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "music-stable-export-sas-1.sas.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1b:3290:10d:5cc7:7029:0"; };
                                                { "music-stable-export-sas-2.sas.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1b:3107:10d:5cc7:6d01:0"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = true;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                          refers = "requests_mts_admin_to_sas";
                                        }; -- report
                                      }; -- mts_admin_sas
                                      mts_admin_man = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "music-stable-export-man-1.man.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c0b:6e15:10d:5cc9:c370:0"; };
                                                { "music-stable-export-man-2.man.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1a:31a6:10d:5cc9:1994:0"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = true;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                          refers = "requests_mts_admin_to_man";
                                        }; -- report
                                      }; -- mts_admin_man
                                      mts_admin_vla = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "music-stable-export-vla-1.vla.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c0d:360c:10d:5cc5:efb1:0"; };
                                                { "music-stable-export-vla-2.vla.yp-c.yandex.net"; 85; 1.000; "2a02:6b8:c1d:1c82:10d:5cc5:2ee0:0"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = true;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                          refers = "requests_mts_admin_to_vla";
                                        }; -- report
                                      }; -- mts_admin_vla
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- on_error
                            }; -- balancer2
                          }; -- headers
                        }; -- shared
                      }; -- ["https_mts-admin_music_yandex_ru"]
                      ["https_music-partner_yandex_ru"] = {
                        priority = 2;
                        match_fsm = {
                          host = "music-partner.yandex.ru";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "music_partner_upstreams";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
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
                                  rewrite = "https://music.yandex.ru/partner";
                                };
                              }; -- actions
                              errordocument = {
                                status = 302;
                                force_conn_close = false;
                                remain_headers = "Location";
                              }; -- errordocument
                            }; -- rewrite
                          }; -- headers
                        }; -- report
                      }; -- ["https_music-partner_yandex_ru"]
                      default = {
                        priority = 1;
                        report = {
                          uuid = "music";
                          ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,150ms,200ms,250ms,300ms,350ms,400ms,450ms,500ms,550ms,600ms,650ms,700ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms";
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          threshold = {
                            lo_bytes = 102400;
                            hi_bytes = 1024000;
                            recv_timeout = "1s";
                            pass_timeout = "10s";
                            regexp = {
                              translate_s3 = {
                                priority = 10;
                                match_fsm = {
                                  path = "/translate/sitemap.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    translate_s3 = {
                                      weight = 1.000;
                                      headers = {
                                        create = {
                                          Host = "translate.s3.mds.yandex.net";
                                        }; -- create
                                        rewrite = {
                                          actions = {
                                            {
                                              global = false;
                                              literal = false;
                                              rewrite = "/lyrics/%1";
                                              case_insensitive = false;
                                              regexp = "/translate/(.*)";
                                            };
                                          }; -- actions
                                          balancer2 = {
                                            timeout_policy = {
                                              timeout = "5000ms";
                                              simple_policy = {};
                                            }; -- timeout_policy
                                            attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "translate.s3.mds.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3:147"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5000ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                                status_code_blacklist = {
                                                  "4xx";
                                                }; -- status_code_blacklist
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- rewrite
                                      }; -- headers
                                    }; -- translate_s3
                                  }; -- rr
                                  on_error = {
                                    shared = {
                                      uuid = "6882737606313467076";
                                    }; -- shared
                                  }; -- on_error
                                }; -- balancer2
                              }; -- translate_s3
                              translate = {
                                priority = 9;
                                match_fsm = {
                                  path = "/translate/.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    translate_misc = {
                                      weight = 1.000;
                                      headers = {
                                        create = {
                                          Host = "translate-misc.yandex.net";
                                        }; -- create
                                        rewrite = {
                                          actions = {
                                            {
                                              global = false;
                                              literal = false;
                                              rewrite = "/lyrics/%1";
                                              case_insensitive = false;
                                              regexp = "/translate/(.*)";
                                            };
                                          }; -- actions
                                          balancer2 = {
                                            timeout_policy = {
                                              timeout = "5000ms";
                                              simple_policy = {};
                                            }; -- timeout_policy
                                            attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "translate-misc.yandex.net"; 80; 1.000; "2a02:6b8:0:3400:0:71d:0:71"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5000ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                                status_code_blacklist = {
                                                  "4xx";
                                                }; -- status_code_blacklist
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- rewrite
                                      }; -- headers
                                    }; -- translate_misc
                                  }; -- rr
                                  on_error = {
                                    shared = {
                                      uuid = "6882737606313467076";
                                    }; -- shared
                                  }; -- on_error
                                }; -- balancer2
                              }; -- translate
                              zemfira_rewrite = {
                                priority = 8;
                                match_fsm = {
                                  path = "/zemfira.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
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
                                        rewrite = "https://zemfiroom.ru";
                                      };
                                    }; -- actions
                                    errordocument = {
                                      status = 302;
                                      force_conn_close = false;
                                      remain_headers = "Location";
                                    }; -- errordocument
                                  }; -- rewrite
                                }; -- headers
                              }; -- zemfira_rewrite
                              promo_rewrite = {
                                priority = 7;
                                match_fsm = {
                                  path = "/promo.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
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
                                        rewrite = "https://music.yandex.ru/family-plus";
                                      };
                                    }; -- actions
                                    errordocument = {
                                      status = 302;
                                      force_conn_close = false;
                                      remain_headers = "Location";
                                    }; -- errordocument
                                  }; -- rewrite
                                }; -- headers
                              }; -- promo_rewrite
                              aab_proxy = {
                                priority = 6;
                                match_fsm = {
                                  header = {
                                    name = "x-aab-proxy";
                                    value = ".*";
                                  }; -- header
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "6882737606313467076";
                                  shared = {
                                    uuid = "music_upstreams";
                                  }; -- shared
                                }; -- shared
                              }; -- aab_proxy
                              aab_http_check = {
                                priority = 5;
                                match_fsm = {
                                  header = {
                                    name = "x-aab-http-check";
                                    value = ".*";
                                  }; -- header
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "2310043735242945754";
                                  shared = {
                                    uuid = "main_cryprox_section";
                                  }; -- shared
                                }; -- shared
                              }; -- aab_http_check
                              aab_url_prefix_check = {
                                priority = 4;
                                match_fsm = {
                                  path = "/_crpd/.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "2310043735242945754";
                                }; -- shared
                              }; -- aab_url_prefix_check
                              http_post = {
                                priority = 3;
                                match_fsm = {
                                  match = "POST.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                headers = {
                                  create_func = {
                                    ["X-Forwarded-For"] = "realip";
                                    ["X-Forwarded-For-Y"] = "realip";
                                    ["X-Real-IP"] = "realip";
                                    ["X-Req-Id"] = "reqid";
                                    ["X-Request-Id"] = "reqid";
                                    ["X-Scheme"] = "scheme";
                                    ["X-Source-Port-Y"] = "realport";
                                    ["X-Start-Time"] = "starttime";
                                    ["X-Yandex-Ja3"] = "ja3";
                                  }; -- create_func
                                  create = {
                                    ["X-Antirobot-Service-Y"] = "music";
                                    ["X-Yandex-HTTPS"] = "yes";
                                    ["X-Yandex-L7"] = "yes";
                                  }; -- create
                                  hasher = {
                                    mode = "subnet";
                                    take_ip_from = "X-Real-IP";
                                    h100 = {
                                      cutter = {
                                        bytes = 512;
                                        timeout = "0.1s";
                                        antirobot = {
                                          cut_request = true;
                                          no_cut_request_file = "./controls/no_cut_request_file";
                                          file_switch = "./controls/disable_antirobot_module";
                                          cut_request_bytes = 512;
                                          checker = {
                                            shared = {
                                              uuid = "1220882788782260093";
                                              report = {
                                                uuid = "antirobot";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  by_name_policy = {
                                                    name = get_geo("antirobot_", "random");
                                                    simple_policy = {};
                                                  }; -- by_name_policy
                                                  attempts = 1;
                                                  rr = {
                                                    antirobot_man = {
                                                      weight = 1.000;
                                                      balancer2 = {
                                                        unique_policy = {};
                                                        attempts = 2;
                                                        hashing = {
                                                          unpack(gen_proxy_backends({
                                                            { "man1-0313.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                                            { "man1-0510.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                                            { "man1-0619.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                                            { "man1-0694.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                                            { "man1-1193.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6012:f652:14ff:fe48:9680"; };
                                                            { "man1-1593.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b020"; };
                                                            { "man1-1676.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:e4e0"; };
                                                            { "man1-1987.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2320"; };
                                                            { "man1-2387.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6012:f652:14ff:fe8c:130"; };
                                                            { "man1-2710.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6078:f652:14ff:fe8b:f2f0"; };
                                                            { "man1-2858.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e6a0"; };
                                                            { "man1-2910.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f170"; };
                                                            { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
                                                            { "man1-3306.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:19e0"; };
                                                            { "man1-4045.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7fbc"; };
                                                            { "man2-4712.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:761:0:604:dbc:a444"; };
                                                            { "man2-4804.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:75e:0:604:dbc:a2f1"; };
                                                            { "man2-4997.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:839:0:604:5cba:a2b2"; };
                                                            { "man2-5047.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:766:0:604:dde:f430"; };
                                                            { "man2-5094.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:839:0:604:5cba:a27a"; };
                                                            { "man2-6654.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:645:0:604:14a9:6a7e"; };
                                                            { "man2-7104.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:648:0:604:14a7:669f"; };
                                                            { "man2-7200.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:651:0:604:14a9:69a5"; };
                                                          }, {
                                                            resolve_timeout = "10ms";
                                                            connect_timeout = "30ms";
                                                            backend_timeout = "100ms";
                                                            fail_on_5xx = true;
                                                            http_backend = true;
                                                            buffering = false;
                                                            keepalive_count = 0;
                                                            need_resolve = true;
                                                          }))
                                                        }; -- hashing
                                                      }; -- balancer2
                                                    }; -- antirobot_man
                                                    antirobot_sas = {
                                                      weight = 1.000;
                                                      balancer2 = {
                                                        unique_policy = {};
                                                        attempts = 2;
                                                        hashing = {
                                                          unpack(gen_proxy_backends({
                                                            { "sas1-0670.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:154:225:90ff:fe83:1800"; };
                                                            { "sas1-0980.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:153:225:90ff:fe83:aa2"; };
                                                            { "sas1-2218.search.yandex.net"; 13512; 343.000; "2a02:6b8:b000:66a:225:90ff:fe94:1792"; };
                                                            { "sas2-4686.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:665:225:90ff:fe92:894a"; };
                                                            { "sas2-8870.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:5cc:0:604:9092:8666"; };
                                                            { "sas2-8992.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:792:0:604:9094:2fb4"; };
                                                            { "sas2-8993.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1530"; };
                                                            { "sas2-9021.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b11:0:604:90c2:a40a"; };
                                                            { "sas2-9033.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:783:0:604:9094:138e"; };
                                                            { "sas2-9036.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1748"; };
                                                            { "sas2-9189.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bb94"; };
                                                            { "sas2-9190.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:3436:7284"; };
                                                            { "sas2-9191.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2ce6"; };
                                                            { "sas2-9192.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3fe4"; };
                                                            { "sas2-9193.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2436"; };
                                                            { "sas2-9194.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:35f8"; };
                                                            { "sas2-9195.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bdea"; };
                                                            { "sas2-9196.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:badc"; };
                                                            { "sas2-9197.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bf90"; };
                                                            { "sas2-9198.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:e87e"; };
                                                            { "sas2-9199.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e962"; };
                                                            { "sas2-9200.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea60"; };
                                                            { "sas2-9201.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:354b:5cef"; };
                                                            { "sas2-9202.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bef0"; };
                                                            { "sas2-9203.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd16"; };
                                                            { "sas2-9204.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b96e"; };
                                                            { "sas2-9205.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b07"; };
                                                            { "sas2-9206.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bc22"; };
                                                            { "sas2-9207.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2730"; };
                                                            { "sas2-9208.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:ea16"; };
                                                            { "sas2-9209.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2238"; };
                                                            { "sas2-9210.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:d9e4"; };
                                                            { "sas2-9211.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd5c"; };
                                                            { "sas2-9212.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2f8c"; };
                                                            { "sas2-9213.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf66"; };
                                                            { "sas2-9214.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34c8:caf6"; };
                                                            { "sas2-9215.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7a1f"; };
                                                            { "sas2-9216.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b3f"; };
                                                            { "sas2-9217.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:b90a"; };
                                                            { "sas2-9218.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2df2"; };
                                                            { "sas2-9219.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7cce"; };
                                                            { "sas2-9220.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2dca"; };
                                                            { "sas2-9221.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:808e:7c06"; };
                                                            { "sas2-9222.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c018"; };
                                                            { "sas2-9223.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3c3e"; };
                                                            { "sas2-9224.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:d98c"; };
                                                            { "sas2-9225.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2b10"; };
                                                            { "sas2-9226.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c006"; };
                                                            { "sas2-9227.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:baf8"; };
                                                            { "sas2-9228.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:325e"; };
                                                            { "sas2-9229.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b93e"; };
                                                            { "sas2-9230.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2454"; };
                                                            { "sas2-9231.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bb86"; };
                                                            { "sas2-9232.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2cfc"; };
                                                            { "sas2-9233.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:bb4e"; };
                                                            { "sas2-9234.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:20ca"; };
                                                            { "sas2-9235.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e562"; };
                                                            { "sas2-9236.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:31e2"; };
                                                            { "sas2-9237.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf4c"; };
                                                            { "sas2-9238.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d902"; };
                                                            { "sas2-9239.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dc34"; };
                                                            { "sas2-9240.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d7a8"; };
                                                            { "sas2-9241.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c0ce"; };
                                                            { "sas2-9242.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea3a"; };
                                                            { "sas2-9243.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c016"; };
                                                            { "sas2-9244.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bf48"; };
                                                            { "sas2-9245.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:8089:7456"; };
                                                            { "sas2-9246.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e4ea"; };
                                                            { "sas2-9247.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e964"; };
                                                            { "sas2-9411.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:1856"; };
                                                            { "sas2-9412.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:17e4"; };
                                                            { "sas2-9528.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:726:0:604:90c1:d270"; };
                                                            { "sas2-9530.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:530:0:604:34cf:2336"; };
                                                            { "sas2-9532.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2eac"; };
                                                            { "sas2-9533.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2c94"; };
                                                          }, {
                                                            resolve_timeout = "10ms";
                                                            connect_timeout = "30ms";
                                                            backend_timeout = "100ms";
                                                            fail_on_5xx = true;
                                                            http_backend = true;
                                                            buffering = false;
                                                            keepalive_count = 0;
                                                            need_resolve = true;
                                                          }))
                                                        }; -- hashing
                                                      }; -- balancer2
                                                    }; -- antirobot_sas
                                                    antirobot_vla = {
                                                      weight = 1.000;
                                                      balancer2 = {
                                                        unique_policy = {};
                                                        attempts = 2;
                                                        hashing = {
                                                          unpack(gen_proxy_backends({
                                                            { "vla1-1343.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                                            { "vla1-1526.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:4d:0:604:db7:a142"; };
                                                            { "vla1-1797.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                                            { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                            { "vla1-3421.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:11:0:604:db7:998f"; };
                                                            { "vla1-3568.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                                            { "vla1-3679.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                                            { "vla1-3709.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                                            { "vla1-3710.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                                            { "vla1-3716.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                                            { "vla1-3863.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                                            { "vla1-3881.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a5e5"; };
                                                            { "vla1-3965.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                                            { "vla1-4006.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                                            { "vla1-4025.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                                            { "vla1-4041.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                                            { "vla1-4063.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8b:0:604:db7:aa8e"; };
                                                            { "vla1-4114.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                                            { "vla1-4117.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                                            { "vla1-4119.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                                            { "vla1-4130.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                                            { "vla1-4153.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                                            { "vla1-4167.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                                            { "vla1-4168.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                                            { "vla1-4177.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                                            { "vla1-4183.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                                            { "vla1-4192.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                                            { "vla1-4200.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                                            { "vla1-4321.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                                            { "vla1-4344.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                                            { "vla1-4354.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                                            { "vla1-4385.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:63:0:604:db7:9e77"; };
                                                            { "vla1-4406.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                                            { "vla1-4472.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                                            { "vla1-4475.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:a1:0:604:db7:a2e0"; };
                                                            { "vla1-4553.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                                            { "vla1-4554.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
                                                          }, {
                                                            resolve_timeout = "10ms";
                                                            connect_timeout = "30ms";
                                                            backend_timeout = "100ms";
                                                            fail_on_5xx = true;
                                                            http_backend = true;
                                                            buffering = false;
                                                            keepalive_count = 0;
                                                            need_resolve = true;
                                                          }))
                                                        }; -- hashing
                                                      }; -- balancer2
                                                    }; -- antirobot_vla
                                                  }; -- rr
                                                }; -- balancer2
                                              }; -- report
                                            }; -- shared
                                          }; -- checker
                                          module = {
                                            geobase = {
                                              trusted = false;
                                              geo_host = "laas.yandex.ru";
                                              take_ip_from = "X-Forwarded-For-Y";
                                              laas_answer_header = "X-LaaS-Answered";
                                              file_switch = "./controls/disable_geobase.switch";
                                              geo_path = "/region?response_format=header&version=1&service=balancer";
                                              geo = {
                                                shared = {
                                                  uuid = "6796952198230220343";
                                                  report = {
                                                    uuid = "geobasemodule";
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
                                                        attempts = 2;
                                                        rr = {
                                                          unpack(gen_proxy_backends({
                                                            { "laas.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::1022"; };
                                                          }, {
                                                            resolve_timeout = "10ms";
                                                            connect_timeout = "15ms";
                                                            backend_timeout = "20ms";
                                                            fail_on_5xx = true;
                                                            http_backend = true;
                                                            buffering = false;
                                                            keepalive_count = 10;
                                                            need_resolve = true;
                                                          }))
                                                        }; -- rr
                                                      }; -- balancer2
                                                    }; -- stats_eater
                                                  }; -- report
                                                }; -- shared
                                              }; -- geo
                                              shared = {
                                                uuid = "6882737606313467076";
                                              }; -- shared
                                            }; -- geobase
                                          }; -- module
                                        }; -- antirobot
                                      }; -- cutter
                                    }; -- h100
                                  }; -- hasher
                                }; -- headers
                              }; -- http_post
                              cookie_bltsr = {
                                priority = 2;
                                match_fsm = {
                                  cookie = "OvWpxdyhuQMVC6KVXled0rz6kqJb8JAqqN3YU496";
                                  case_insensitive = true;
                                  surround = true;
                                }; -- match_fsm
                                shared = {
                                  uuid = "main_cryprox_section";
                                  headers = {
                                    create_func = {
                                      ["X-Forwarded-For"] = "realip";
                                      ["X-Forwarded-For-Y"] = "realip";
                                      ["X-Real-IP"] = "realip";
                                      ["X-Req-Id"] = "reqid";
                                      ["X-Request-Id"] = "reqid";
                                      ["X-Scheme"] = "scheme";
                                      ["X-Source-Port-Y"] = "realport";
                                      ["X-Start-Time"] = "starttime";
                                      ["X-Yandex-Ja3"] = "ja3";
                                    }; -- create_func
                                    create = {
                                      ["X-Antirobot-Service-Y"] = "music";
                                      ["X-Yandex-HTTPS"] = "yes";
                                      ["X-Yandex-L7"] = "yes";
                                    }; -- create
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1220882788782260093";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              geobase = {
                                                trusted = false;
                                                geo_host = "laas.yandex.ru";
                                                take_ip_from = "X-Forwarded-For-Y";
                                                laas_answer_header = "X-LaaS-Answered";
                                                file_switch = "./controls/disable_geobase.switch";
                                                geo_path = "/region?response_format=header&version=1&service=balancer";
                                                geo = {
                                                  shared = {
                                                    uuid = "6796952198230220343";
                                                  }; -- shared
                                                }; -- geo
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 1;
                                                  rr = {
                                                    aab_on = {
                                                      weight = 1.000;
                                                      report = {
                                                        uuid = "aab";
                                                        ranges = get_str_var("default_ranges");
                                                        just_storage = false;
                                                        disable_robotness = true;
                                                        disable_sslness = true;
                                                        events = {
                                                          stats = "report";
                                                        }; -- events
                                                        headers = {
                                                          create = {
                                                            ["X-Forwarded-Proto"] = "https";
                                                            ["x-aab-partnertoken"] = get_str_env_var("CRY_TOKEN");
                                                          }; -- create
                                                          balancer2 = {
                                                            timeout_policy = {
                                                              timeout = "5000ms";
                                                              simple_policy = {};
                                                            }; -- timeout_policy
                                                            attempts = 1;
                                                            rr = {
                                                              unpack(gen_proxy_backends({
                                                                { "cryprox.yandex.net"; 80; 1.000; "2a02:6b8::402"; };
                                                              }, {
                                                                resolve_timeout = "10ms";
                                                                connect_timeout = "100ms";
                                                                backend_timeout = "10s";
                                                                fail_on_5xx = true;
                                                                http_backend = true;
                                                                buffering = true;
                                                                keepalive_count = 0;
                                                                need_resolve = true;
                                                              }))
                                                            }; -- rr
                                                            on_error = {
                                                              regexp = {
                                                                aab_http_check = {
                                                                  priority = 2;
                                                                  match_fsm = {
                                                                    header = {
                                                                      name = "x-aab-http-check";
                                                                      value = ".*";
                                                                    }; -- header
                                                                    case_insensitive = true;
                                                                    surround = false;
                                                                  }; -- match_fsm
                                                                  errordocument = {
                                                                    status = 404;
                                                                    force_conn_close = false;
                                                                  }; -- errordocument
                                                                }; -- aab_http_check
                                                                default = {
                                                                  priority = 1;
                                                                  shared = {
                                                                    uuid = "6882737606313467076";
                                                                  }; -- shared
                                                                }; -- default
                                                              }; -- regexp
                                                            }; -- on_error
                                                          }; -- balancer2
                                                        }; -- headers
                                                      }; -- report
                                                    }; -- aab_on
                                                    aab_off = {
                                                      weight = -1.000;
                                                      shared = {
                                                        uuid = "6882737606313467076";
                                                      }; -- shared
                                                    }; -- aab_off
                                                  }; -- rr
                                                }; -- balancer2
                                              }; -- geobase
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- headers
                                }; -- shared
                              }; -- cookie_bltsr
                              default = {
                                priority = 1;
                                headers = {
                                  create_func = {
                                    ["X-Forwarded-For"] = "realip";
                                    ["X-Forwarded-For-Y"] = "realip";
                                    ["X-Real-IP"] = "realip";
                                    ["X-Req-Id"] = "reqid";
                                    ["X-Request-Id"] = "reqid";
                                    ["X-Scheme"] = "scheme";
                                    ["X-Source-Port-Y"] = "realport";
                                    ["X-Start-Time"] = "starttime";
                                    ["X-Yandex-Ja3"] = "ja3";
                                  }; -- create_func
                                  create = {
                                    ["X-Antirobot-Service-Y"] = "music";
                                    ["X-Yandex-HTTPS"] = "yes";
                                    ["X-Yandex-L7"] = "yes";
                                  }; -- create
                                  hasher = {
                                    mode = "subnet";
                                    take_ip_from = "X-Real-IP";
                                    h100 = {
                                      cutter = {
                                        bytes = 512;
                                        timeout = "0.1s";
                                        antirobot = {
                                          cut_request = true;
                                          no_cut_request_file = "./controls/no_cut_request_file";
                                          file_switch = "./controls/disable_antirobot_module";
                                          cut_request_bytes = 512;
                                          checker = {
                                            shared = {
                                              uuid = "1220882788782260093";
                                            }; -- shared
                                          }; -- checker
                                          module = {
                                            geobase = {
                                              trusted = false;
                                              geo_host = "laas.yandex.ru";
                                              take_ip_from = "X-Forwarded-For-Y";
                                              laas_answer_header = "X-LaaS-Answered";
                                              file_switch = "./controls/disable_geobase.switch";
                                              geo_path = "/region?response_format=header&version=1&service=balancer";
                                              geo = {
                                                shared = {
                                                  uuid = "6796952198230220343";
                                                }; -- shared
                                              }; -- geo
                                              shared = {
                                                uuid = "music_upstreams";
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 1;
                                                  rr = {
                                                    weights_file = "./controls/traffic_control.weights";
                                                    music_sas = {
                                                      weight = 1.000;
                                                      report = {
                                                        uuid = "requests_music_to_sas";
                                                        ranges = get_str_var("default_ranges");
                                                        just_storage = false;
                                                        disable_robotness = true;
                                                        disable_sslness = true;
                                                        events = {
                                                          stats = "report";
                                                        }; -- events
                                                        balancer2 = {
                                                          watermark_policy = {
                                                            lo = 0.005;
                                                            hi = 0.020;
                                                            params_file = "./controls/watermark_policy.params_file";
                                                            unique_policy = {};
                                                          }; -- watermark_policy
                                                          attempts = 1;
                                                          connection_attempts = 16;
                                                          active = {
                                                            delay = "3s";
                                                            request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music.yandex.ru\n\n";
                                                            steady = true;
                                                            unpack(gen_proxy_backends({
                                                              { "music-stable-musfront-sas-10.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c10:249c:0:442b:2316:0"; };
                                                              { "music-stable-musfront-sas-11.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:310e:0:442b:189b:0"; };
                                                              { "music-stable-musfront-sas-12.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:211:0:442b:bff2:0"; };
                                                              { "music-stable-musfront-sas-14.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2487:0:442b:7bce:0"; };
                                                              { "music-stable-musfront-sas-15.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2916:0:442b:da06:0"; };
                                                              { "music-stable-musfront-sas-16.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:2a9e:0:442b:546c:0"; };
                                                              { "music-stable-musfront-sas-17.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:381b:0:442b:38b5:0"; };
                                                              { "music-stable-musfront-sas-2.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:a18b:0:442b:ff0b:0"; };
                                                              { "music-stable-musfront-sas-21.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2687:0:442b:8b3c:0"; };
                                                              { "music-stable-musfront-sas-22.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c11:915:0:442b:8753:0"; };
                                                              { "music-stable-musfront-sas-3.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:7310:0:442b:1065:0"; };
                                                              { "music-stable-musfront-sas-4.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:1c83:0:442b:122e:0"; };
                                                              { "music-stable-musfront-sas-5.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c10:378e:0:442b:5621:0"; };
                                                              { "music-stable-musfront-sas-7.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c11:a83:0:442b:ce5b:0"; };
                                                              { "music-stable-musfront-sas-8.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2289:0:442b:cbde:0"; };
                                                              { "music-stable-musfront-sas-9.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:3286:0:442b:9bfd:0"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "100ms";
                                                              backend_timeout = "10s";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = true;
                                                              keepalive_count = 0;
                                                              need_resolve = true;
                                                              status_code_blacklist = {
                                                                "409";
                                                              }; -- status_code_blacklist
                                                            }))
                                                          }; -- active
                                                        }; -- balancer2
                                                      }; -- report
                                                    }; -- music_sas
                                                    music_man = {
                                                      weight = 1.000;
                                                      report = {
                                                        uuid = "requests_music_to_man";
                                                        ranges = get_str_var("default_ranges");
                                                        just_storage = false;
                                                        disable_robotness = true;
                                                        disable_sslness = true;
                                                        events = {
                                                          stats = "report";
                                                        }; -- events
                                                        balancer2 = {
                                                          watermark_policy = {
                                                            lo = 0.005;
                                                            hi = 0.020;
                                                            params_file = "./controls/watermark_policy.params_file";
                                                            unique_policy = {};
                                                          }; -- watermark_policy
                                                          attempts = 1;
                                                          connection_attempts = 19;
                                                          active = {
                                                            delay = "3s";
                                                            request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music.yandex.ru\n\n";
                                                            steady = true;
                                                            unpack(gen_proxy_backends({
                                                              { "music-stable-musfront-man-10.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:290f:0:442b:f54b:0"; };
                                                              { "music-stable-musfront-man-11.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:13a3:0:442b:b9bd:0"; };
                                                              { "music-stable-musfront-man-12.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:2f26:0:442b:c4ba:0"; };
                                                              { "music-stable-musfront-man-13.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:785:0:442b:f3bb:0"; };
                                                              { "music-stable-musfront-man-14.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:1b23:0:442b:9bbb:0"; };
                                                              { "music-stable-musfront-man-15.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:4725:0:442b:7049:0"; };
                                                              { "music-stable-musfront-man-16.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:32c2:0:442b:a6c7:0"; };
                                                              { "music-stable-musfront-man-17.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:32a8:0:442b:394:0"; };
                                                              { "music-stable-musfront-man-18.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3643:0:442b:f704:0"; };
                                                              { "music-stable-musfront-man-19.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:1824:0:442b:5b5a:0"; };
                                                              { "music-stable-musfront-man-2.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a1d:0:442b:8d15:0"; };
                                                              { "music-stable-musfront-man-20.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2c91:0:442b:85a7:0"; };
                                                              { "music-stable-musfront-man-21.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:19d:0:442b:c7c5:0"; };
                                                              { "music-stable-musfront-man-3.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2927:0:442b:938b:0"; };
                                                              { "music-stable-musfront-man-4.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:1325:0:442b:7767:0"; };
                                                              { "music-stable-musfront-man-5.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:1b70:0:442b:2fee:0"; };
                                                              { "music-stable-musfront-man-6.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:5212:0:442b:f623:0"; };
                                                              { "music-stable-musfront-man-7.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:27ac:0:442b:fa4d:0"; };
                                                              { "music-stable-musfront-man-9.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3ea0:0:442b:521:0"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "100ms";
                                                              backend_timeout = "10s";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = true;
                                                              keepalive_count = 0;
                                                              need_resolve = true;
                                                              status_code_blacklist = {
                                                                "409";
                                                              }; -- status_code_blacklist
                                                            }))
                                                          }; -- active
                                                        }; -- balancer2
                                                      }; -- report
                                                    }; -- music_man
                                                    music_myt = {
                                                      weight = 1.000;
                                                      report = {
                                                        uuid = "requests_music_to_myt";
                                                        ranges = get_str_var("default_ranges");
                                                        just_storage = false;
                                                        disable_robotness = true;
                                                        disable_sslness = true;
                                                        events = {
                                                          stats = "report";
                                                        }; -- events
                                                        balancer2 = {
                                                          watermark_policy = {
                                                            lo = 0.005;
                                                            hi = 0.020;
                                                            params_file = "./controls/watermark_policy.params_file";
                                                            unique_policy = {};
                                                          }; -- watermark_policy
                                                          attempts = 1;
                                                          connection_attempts = 18;
                                                          active = {
                                                            delay = "3s";
                                                            request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music.yandex.ru\n\n";
                                                            steady = true;
                                                            unpack(gen_proxy_backends({
                                                              { "music-stable-musfront-myt-10.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:50a7:0:442b:f20d:0"; };
                                                              { "music-stable-musfront-myt-11.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:162e:0:442b:232f:0"; };
                                                              { "music-stable-musfront-myt-12.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:39a8:0:442b:89f0:0"; };
                                                              { "music-stable-musfront-myt-13.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:5900:0:442b:19e1:0"; };
                                                              { "music-stable-musfront-myt-14.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:4886:0:442b:b82d:0"; };
                                                              { "music-stable-musfront-myt-15.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:238c:0:442b:ec49:0"; };
                                                              { "music-stable-musfront-myt-16.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:3a93:0:442b:7431:0"; };
                                                              { "music-stable-musfront-myt-18.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:221d:0:442b:b60d:0"; };
                                                              { "music-stable-musfront-myt-19.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:519a:0:442b:f9c5:0"; };
                                                              { "music-stable-musfront-myt-20.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:416:0:442b:90c7:0"; };
                                                              { "music-stable-musfront-myt-21.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:4e98:0:442b:4708:0"; };
                                                              { "music-stable-musfront-myt-3.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:418:0:442b:c549:0"; };
                                                              { "music-stable-musfront-myt-4.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:4ea9:0:442b:987b:0"; };
                                                              { "music-stable-musfront-myt-5.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:51a1:0:442b:506:0"; };
                                                              { "music-stable-musfront-myt-6.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:3992:0:442b:549f:0"; };
                                                              { "music-stable-musfront-myt-7.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:2bab:0:442b:3fe3:0"; };
                                                              { "music-stable-musfront-myt-8.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:39af:0:442b:75d7:0"; };
                                                              { "music-stable-musfront-myt-9.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:27af:0:442b:bfa7:0"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "100ms";
                                                              backend_timeout = "10s";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = true;
                                                              keepalive_count = 0;
                                                              need_resolve = true;
                                                              status_code_blacklist = {
                                                                "409";
                                                              }; -- status_code_blacklist
                                                            }))
                                                          }; -- active
                                                        }; -- balancer2
                                                      }; -- report
                                                    }; -- music_myt
                                                  }; -- rr
                                                  on_error = {
                                                    report = {
                                                      uuid = "onerror_music";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      balancer2 = {
                                                        unique_policy = {};
                                                        attempts = 1;
                                                        rr = {
                                                          weights_file = "./controls/traffic_control.weights";
                                                          music_sas = {
                                                            weight = 1.000;
                                                            report = {
                                                              ranges = get_str_var("default_ranges");
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              balancer2 = {
                                                                unique_policy = {};
                                                                attempts = 1;
                                                                connection_attempts = 16;
                                                                rr = {
                                                                  unpack(gen_proxy_backends({
                                                                    { "music-stable-musfront-sas-10.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c10:249c:0:442b:2316:0"; };
                                                                    { "music-stable-musfront-sas-11.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:310e:0:442b:189b:0"; };
                                                                    { "music-stable-musfront-sas-12.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:211:0:442b:bff2:0"; };
                                                                    { "music-stable-musfront-sas-14.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2487:0:442b:7bce:0"; };
                                                                    { "music-stable-musfront-sas-15.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2916:0:442b:da06:0"; };
                                                                    { "music-stable-musfront-sas-16.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:2a9e:0:442b:546c:0"; };
                                                                    { "music-stable-musfront-sas-17.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:381b:0:442b:38b5:0"; };
                                                                    { "music-stable-musfront-sas-2.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:a18b:0:442b:ff0b:0"; };
                                                                    { "music-stable-musfront-sas-21.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2687:0:442b:8b3c:0"; };
                                                                    { "music-stable-musfront-sas-22.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c11:915:0:442b:8753:0"; };
                                                                    { "music-stable-musfront-sas-3.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:7310:0:442b:1065:0"; };
                                                                    { "music-stable-musfront-sas-4.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:1c83:0:442b:122e:0"; };
                                                                    { "music-stable-musfront-sas-5.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c10:378e:0:442b:5621:0"; };
                                                                    { "music-stable-musfront-sas-7.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c11:a83:0:442b:ce5b:0"; };
                                                                    { "music-stable-musfront-sas-8.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2289:0:442b:cbde:0"; };
                                                                    { "music-stable-musfront-sas-9.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:3286:0:442b:9bfd:0"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "100ms";
                                                                    backend_timeout = "10s";
                                                                    fail_on_5xx = false;
                                                                    http_backend = true;
                                                                    buffering = true;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- rr
                                                              }; -- balancer2
                                                              refers = "requests_music_to_sas";
                                                            }; -- report
                                                          }; -- music_sas
                                                          music_man = {
                                                            weight = 1.000;
                                                            report = {
                                                              ranges = get_str_var("default_ranges");
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              balancer2 = {
                                                                unique_policy = {};
                                                                attempts = 1;
                                                                connection_attempts = 19;
                                                                rr = {
                                                                  unpack(gen_proxy_backends({
                                                                    { "music-stable-musfront-man-10.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:290f:0:442b:f54b:0"; };
                                                                    { "music-stable-musfront-man-11.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:13a3:0:442b:b9bd:0"; };
                                                                    { "music-stable-musfront-man-12.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:2f26:0:442b:c4ba:0"; };
                                                                    { "music-stable-musfront-man-13.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:785:0:442b:f3bb:0"; };
                                                                    { "music-stable-musfront-man-14.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:1b23:0:442b:9bbb:0"; };
                                                                    { "music-stable-musfront-man-15.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:4725:0:442b:7049:0"; };
                                                                    { "music-stable-musfront-man-16.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:32c2:0:442b:a6c7:0"; };
                                                                    { "music-stable-musfront-man-17.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:32a8:0:442b:394:0"; };
                                                                    { "music-stable-musfront-man-18.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3643:0:442b:f704:0"; };
                                                                    { "music-stable-musfront-man-19.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:1824:0:442b:5b5a:0"; };
                                                                    { "music-stable-musfront-man-2.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a1d:0:442b:8d15:0"; };
                                                                    { "music-stable-musfront-man-20.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2c91:0:442b:85a7:0"; };
                                                                    { "music-stable-musfront-man-21.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:19d:0:442b:c7c5:0"; };
                                                                    { "music-stable-musfront-man-3.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2927:0:442b:938b:0"; };
                                                                    { "music-stable-musfront-man-4.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:1325:0:442b:7767:0"; };
                                                                    { "music-stable-musfront-man-5.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:1b70:0:442b:2fee:0"; };
                                                                    { "music-stable-musfront-man-6.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:5212:0:442b:f623:0"; };
                                                                    { "music-stable-musfront-man-7.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:27ac:0:442b:fa4d:0"; };
                                                                    { "music-stable-musfront-man-9.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3ea0:0:442b:521:0"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "100ms";
                                                                    backend_timeout = "10s";
                                                                    fail_on_5xx = false;
                                                                    http_backend = true;
                                                                    buffering = true;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- rr
                                                              }; -- balancer2
                                                              refers = "requests_music_to_man";
                                                            }; -- report
                                                          }; -- music_man
                                                          music_myt = {
                                                            weight = 1.000;
                                                            report = {
                                                              ranges = get_str_var("default_ranges");
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              balancer2 = {
                                                                unique_policy = {};
                                                                attempts = 1;
                                                                connection_attempts = 18;
                                                                rr = {
                                                                  unpack(gen_proxy_backends({
                                                                    { "music-stable-musfront-myt-10.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:50a7:0:442b:f20d:0"; };
                                                                    { "music-stable-musfront-myt-11.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:162e:0:442b:232f:0"; };
                                                                    { "music-stable-musfront-myt-12.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:39a8:0:442b:89f0:0"; };
                                                                    { "music-stable-musfront-myt-13.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:5900:0:442b:19e1:0"; };
                                                                    { "music-stable-musfront-myt-14.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:4886:0:442b:b82d:0"; };
                                                                    { "music-stable-musfront-myt-15.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:238c:0:442b:ec49:0"; };
                                                                    { "music-stable-musfront-myt-16.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:3a93:0:442b:7431:0"; };
                                                                    { "music-stable-musfront-myt-18.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:221d:0:442b:b60d:0"; };
                                                                    { "music-stable-musfront-myt-19.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:519a:0:442b:f9c5:0"; };
                                                                    { "music-stable-musfront-myt-20.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:416:0:442b:90c7:0"; };
                                                                    { "music-stable-musfront-myt-21.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:4e98:0:442b:4708:0"; };
                                                                    { "music-stable-musfront-myt-3.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c00:418:0:442b:c549:0"; };
                                                                    { "music-stable-musfront-myt-4.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:4ea9:0:442b:987b:0"; };
                                                                    { "music-stable-musfront-myt-5.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:51a1:0:442b:506:0"; };
                                                                    { "music-stable-musfront-myt-6.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:3992:0:442b:549f:0"; };
                                                                    { "music-stable-musfront-myt-7.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:2bab:0:442b:3fe3:0"; };
                                                                    { "music-stable-musfront-myt-8.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:39af:0:442b:75d7:0"; };
                                                                    { "music-stable-musfront-myt-9.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:27af:0:442b:bfa7:0"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "100ms";
                                                                    backend_timeout = "10s";
                                                                    fail_on_5xx = false;
                                                                    http_backend = true;
                                                                    buffering = true;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- rr
                                                              }; -- balancer2
                                                              refers = "requests_music_to_myt";
                                                            }; -- report
                                                          }; -- music_myt
                                                        }; -- rr
                                                      }; -- balancer2
                                                    }; -- report
                                                  }; -- on_error
                                                }; -- balancer2
                                              }; -- shared
                                            }; -- geobase
                                          }; -- module
                                        }; -- antirobot
                                      }; -- cutter
                                    }; -- h100
                                  }; -- hasher
                                }; -- headers
                              }; -- default
                            }; -- regexp
                          }; -- threshold
                        }; -- report
                      }; -- default
                    }; -- regexp
                  }; -- response_headers
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_14541 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14541;
      }; -- ports
      shared = {
        uuid = "8636338677327858807";
      }; -- shared
    }; -- https_section_14541
  }; -- ipdispatch
}