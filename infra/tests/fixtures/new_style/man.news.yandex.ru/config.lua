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


function suffix_with_dc(name, default_dc, separator)
  dc = DC or default_dc or "unknown";
  separator = separator or "_";
  return name .. separator .. dc;
end


_call_func_providers({
  "get_workers";
})


instance = {
  maxconn = 5000;
  buffer = 1048576;
  tcp_fastopen = 0;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  state_directory = "/dev/shm/balancer-state";
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15090, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15090;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15090;
    };
  }; -- admin_addrs
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
  }; -- unistat
  sd = {
    client_name = "awacs-l7-balancer(namespace-id:balancer)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  addrs = {
    {
      port = 15090;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "213.180.193.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.204.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.250.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.251.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "2a02:6b8::12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.193.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.204.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.250.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.251.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "2a02:6b8::12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15090;
      ip = "127.0.0.45";
    };
    {
      port = 15090;
      ip = "127.0.0.44";
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::2:38";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15090;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15090;
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
        15090;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {
          disable_xml_stats = true;
        }; -- admin
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        15090;
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
        "213.180.193.12";
        "213.180.204.12";
        "87.250.250.12";
        "87.250.251.12";
        "2a02:6b8::12";
      }; -- ips
      ports = {
        443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 15091, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            ["_.yandexsport.ru"] = {
              priority = 5;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 15091, "/place/db/www/logs");
              priv = get_private_cert_path("_.yandexsport.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-_.yandexsport.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(m\\.)?yandexsport\\.ru";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st._.yandexsport.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd._.yandexsport.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd._.yandexsport.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["_.yandexsport.ru"]
            ["cryprox-test.news.yandex.ru"] = {
              priority = 4;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 15091, "/place/db/www/logs");
              priv = get_private_cert_path("cryprox-test.news.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-cryprox-test.news.yandex.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(m\\.)?cryprox-test\\.news\\.yandex\\.ru";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.cryprox-test.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.cryprox-test.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.cryprox-test.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["cryprox-test.news.yandex.ru"]
            ["test.news.yandex.ru"] = {
              priority = 3;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 15091, "/place/db/www/logs");
              priv = get_private_cert_path("test.news.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-test.news.yandex.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(m\\.)?test\\.news\\.yandex\\.(az|co\\.il|com\\.am|com\\.ge|ee|fr|kg|lt|lv|md|tj|tm|uz|ru|ua|by|kz|com)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.test.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.test.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.test.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["test.news.yandex.ru"]
            ["news.yandex.az"] = {
              priority = 2;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 15091, "/place/db/www/logs");
              priv = get_private_cert_path("news.yandex.az.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-news.yandex.az.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(m\\.)?news(-clck)?\\.yandex\\.(az|co\\.il|com\\.am|com\\.ge|ee|fr|kg|lt|lv|md|tj|tm|uz)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.news.yandex.az.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.news.yandex.az.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.news.yandex.az.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["news.yandex.az"]
            default = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 15091, "/place/db/www/logs");
              priv = get_private_cert_path("news.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-news.yandex.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.news.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.news.yandex.ru.key", "/dev/shm/balancer/priv");
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
              log = get_log_path("access_log", 15091, "/place/db/www/logs");
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
                headers = {
                  create = {
                    ["X-Yandex-HTTPS"] = "yes";
                  }; -- create
                  shared = {
                    uuid = "upstreams";
                  }; -- shared
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
    http_section = {
      ips = {
        "213.180.193.12";
        "213.180.204.12";
        "87.250.250.12";
        "87.250.251.12";
        "2a02:6b8::12";
      }; -- ips
      ports = {
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 15090, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 15090, "/place/db/www/logs");
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
                uuid = "upstreams";
                regexp = {
                  aab_proxied = {
                    priority = 2;
                    match_fsm = {
                      header = {
                        name = "x-aab-partnertoken";
                        value = get_str_env_var("AWACS_AAB_TOKEN");
                      }; -- header
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    rewrite = {
                      actions = {
                        {
                          regexp = ".*";
                          global = false;
                          rewrite = "-1";
                          literal = false;
                          case_insensitive = false;
                          header_name = "X-Is-Yandex-Net";
                        };
                        {
                          regexp = ".*";
                          rewrite = "0";
                          global = false;
                          literal = false;
                          case_insensitive = false;
                          header_name = "X-Yandex-Internal-Request";
                        };
                        {
                          regexp = ".*";
                          rewrite = "0";
                          global = false;
                          literal = false;
                          case_insensitive = false;
                          header_name = "X-Yandex-Is-Staff-Login";
                        };
                      }; -- actions
                      shared = {
                        uuid = "main_chain";
                      }; -- shared
                    }; -- rewrite
                  }; -- aab_proxied
                  default = {
                    priority = 1;
                    headers = {
                      delete = "(x-forwarded-for.*|x-source-port.*)";
                      rewrite = {
                        actions = {
                          {
                            regexp = ".*";
                            global = false;
                            rewrite = "-1";
                            literal = false;
                            case_insensitive = false;
                            header_name = "X-Is-Yandex-Net";
                          };
                          {
                            regexp = ".*";
                            rewrite = "0";
                            global = false;
                            literal = false;
                            case_insensitive = false;
                            header_name = "X-Yandex-Internal-Request";
                          };
                          {
                            regexp = ".*";
                            rewrite = "0";
                            global = false;
                            literal = false;
                            case_insensitive = false;
                            header_name = "X-Yandex-Is-Staff-Login";
                          };
                        }; -- actions
                        shared = {
                          uuid = "main_chain";
                          headers = {
                            create_func = {
                              ["X-Req-Id"] = "reqid";
                              ["X-Start-Time"] = "starttime";
                              ["X-Yandex-RandomUID"] = "yuid";
                            }; -- create_func
                            create_func_weak = {
                              ["X-Forwarded-For"] = "realip";
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Source-Port-Y"] = "realport";
                            }; -- create_func_weak
                            log_headers = {
                              name_re = "X-Req-Id";
                              response_headers = {
                                create_weak = {
                                  ["X-Content-Type-Options"] = "nosniff";
                                  ["X-XSS-Protection"] = "1; mode=block";
                                }; -- create_weak
                                icookie = {
                                  use_default_keys = true;
                                  domains = ".yandex.az,.yandex.by,.yandex.co.il,.yandex.com,.yandex.com.am,.yandex.com.ge,.yandex.com.ua,.yandex.ee,.yandex.fr,.yandex.kg,.yandex.kz,.yandex.lt,.yandex.lv,.yandex.md,.yandex.ru,.yandex.tj,.yandex.tm,.yandex.uz,.yandex.ua";
                                  trust_parent = false;
                                  trust_children = false;
                                  enable_set_cookie = true;
                                  enable_decrypting = true;
                                  decrypted_uid_header = "X-Yandex-ICookie";
                                  error_header = "X-Yandex-ICookie-Error";
                                  take_randomuid_from = "X-Yandex-RandomUID";
                                  force_equal_to_yandexuid = true;
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
                                    slb_ping = {
                                      priority = 2;
                                      match_fsm = {
                                        URI = "/slb_ping";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                      shared = {
                                        uuid = "466145883168241203";
                                      }; -- shared
                                    }; -- slb_ping
                                    default = {
                                      priority = 1;
                                      report = {
                                        uuid = "antirobot_extra_upstream";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
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
                                                    uuid = "8143817713432665990";
                                                  }; -- shared
                                                }; -- checker
                                                module = {
                                                  shared = {
                                                    uuid = "knoss_exp_upstream";
                                                  }; -- shared
                                                }; -- module
                                              }; -- antirobot
                                            }; -- cutter
                                          }; -- h100
                                        }; -- hasher
                                      }; -- report
                                    }; -- default
                                  }; -- regexp
                                }; -- icookie
                              }; -- response_headers
                            }; -- log_headers
                          }; -- headers
                        }; -- shared
                      }; -- rewrite
                    }; -- headers
                  }; -- default
                }; -- regexp
              }; -- shared
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
    fake_section_ext = {
      ips = {
        "127.0.0.45";
      }; -- ips
      ports = {
        15090;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        shared = {
          uuid = "ext_upstreams";
          regexp = {
            ext_redirect = {
              priority = 20;
              match_or = {
                {
                  match_fsm = {
                    URI = "/opensearch.xml.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    URI = "/advanced.html.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    host = "www.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    URI = "/wintergames.html.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_or
              regexp = {
                opensearch = {
                  priority = 5;
                  match_fsm = {
                    URI = "/opensearch.xml.*";
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
                          rewrite = "https://news.yandex.ru";
                        };
                      }; -- actions
                      errordocument = {
                        status = 302;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- rewrite
                  }; -- headers
                }; -- opensearch
                advanced = {
                  priority = 4;
                  match_fsm = {
                    URI = "/advanced.html.*";
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
                          rewrite = "https://news.yandex.ru/yandsearch?text=&rpt=nnews2";
                        };
                      }; -- actions
                      errordocument = {
                        status = 302;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- rewrite
                  }; -- headers
                }; -- advanced
                www = {
                  priority = 3;
                  match_fsm = {
                    host = "www\\.news\\.yandex.*";
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
                          rewrite = "https://news.yandex.ru%{url}";
                        };
                      }; -- actions
                      errordocument = {
                        status = 302;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- rewrite
                  }; -- headers
                }; -- www
                yandexwww = {
                  priority = 2;
                  match_fsm = {
                    host = "www\\.yandex.*";
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
                          rewrite = "https://yandex.ru%{url}";
                        };
                      }; -- actions
                      errordocument = {
                        status = 302;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- rewrite
                  }; -- headers
                }; -- yandexwww
                wintergames = {
                  priority = 1;
                  match_fsm = {
                    URI = "/wintergames.html.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "4188253573388336186";
                  }; -- shared
                }; -- wintergames
              }; -- regexp
            }; -- ext_redirect
            ext_captcha = {
              priority = 19;
              match_fsm = {
                URI = "/x?(show|check)?captcha.*";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "captchasearch";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                h100 = {
                  cutter = {
                    bytes = 512;
                    timeout = "0.1s";
                    antirobot = {
                      cut_request = true;
                      no_cut_request_file = "./controls/no_cut_request_file";
                      file_switch = "./controls/do.not.use.it";
                      cut_request_bytes = 512;
                      checker = {
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
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- antirobot_man
                            antirobot_sas = {
                              weight = 1.000;
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
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- antirobot_sas
                            antirobot_vla = {
                              weight = 1.000;
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
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- antirobot_vla
                          }; -- rr
                        }; -- balancer2
                      }; -- checker
                      module = {
                        errordocument = {
                          status = 403;
                          force_conn_close = false;
                        }; -- errordocument
                      }; -- module
                    }; -- antirobot
                  }; -- cutter
                }; -- h100
              }; -- report
            }; -- ext_captcha
            ext_clck = {
              priority = 18;
              match_fsm = {
                host = "news-clck\\.yandex\\..*";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
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
                      }; -- checker
                      module = {
                        regexp = {
                          sub_clck = {
                            priority = 3;
                            match_fsm = {
                              URI = "/clck/.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            shared = {
                              uuid = "clck";
                              rewrite = {
                                actions = {
                                  {
                                    global = false;
                                    rewrite = "%1";
                                    literal = false;
                                    regexp = "/clck(/.*)";
                                    case_insensitive = false;
                                  };
                                }; -- actions
                                click = {
                                  keys = "./data/clickdaemon.keys";
                                  report = {
                                    uuid = "clcksearch";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      by_name_policy = {
                                        name = get_geo("clck_", "random");
                                        simple_policy = {};
                                      }; -- by_name_policy
                                      attempts = 1;
                                      rr = {
                                        clck_man = {
                                          weight = 1.000;
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 5;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "man";
                                                    endpoint_set_id = "clck_misc_man-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "1s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
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
                                                }; -- weighted2
                                              }; -- sd
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- clck_man
                                        clck_sas = {
                                          weight = 1.000;
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 5;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "man";
                                                    endpoint_set_id = "clck_misc_sas-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "1s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
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
                                                }; -- weighted2
                                              }; -- sd
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- clck_sas
                                        clck_vla = {
                                          weight = 1.000;
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 5;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "man";
                                                    endpoint_set_id = "clck_misc_vla-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "1s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
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
                                                }; -- weighted2
                                              }; -- sd
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- clck_vla
                                      }; -- rr
                                    }; -- balancer2
                                  }; -- report
                                }; -- click
                              }; -- rewrite
                            }; -- shared
                          }; -- sub_clck
                          sub_favicon = {
                            priority = 2;
                            match_fsm = {
                              URI = "/favicon\\.ico(.*)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            shared = {
                              uuid = "nginx_backends";
                            }; -- shared
                          }; -- sub_favicon
                          default = {
                            priority = 1;
                            errordocument = {
                              status = 404;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- default
                        }; -- regexp
                      }; -- module
                    }; -- antirobot
                  }; -- cutter
                }; -- h100
              }; -- hasher
            }; -- ext_clck
            ext_postedit = {
              priority = 17;
              match_fsm = {
                URI = "/edit";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              errordocument = {
                status = 403;
                force_conn_close = false;
              }; -- errordocument
            }; -- ext_postedit
            ext_gazeta = {
              priority = 16;
              match_fsm = {
                host = "gazeta\\.yandex\\..*";
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
                      rewrite = "https://news.yandex.ru/mynews";
                    };
                  }; -- actions
                  errordocument = {
                    status = 302;
                    force_conn_close = false;
                    remain_headers = "Location";
                  }; -- errordocument
                }; -- rewrite
              }; -- headers
            }; -- ext_gazeta
            ext_static_proxy = {
              priority = 15;
              match_and = {
                {
                  match_fsm = {
                    path = "/yastatic/.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    host = "(m\\.)?news\\.yandex\\.[^.]+";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_and
              rewrite = {
                actions = {
                  {
                    regexp = ".*";
                    global = false;
                    literal = false;
                    header_name = "Host";
                    case_insensitive = false;
                    rewrite = "yastatic.net";
                  };
                  {
                    global = false;
                    split = "path";
                    literal = false;
                    rewrite = "/%1";
                    case_insensitive = false;
                    regexp = "/yastatic/(.*)";
                  };
                }; -- actions
                balancer2 = {
                  simple_policy = {};
                  attempts = 3;
                  rr = {
                    unpack(gen_proxy_backends({
                      { "yastatic.net"; 80; 1.000; "2a02:6b8:20::215"; };
                    }, {
                      resolve_timeout = "10ms";
                      connect_timeout = "100ms";
                      backend_timeout = "1000ms";
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
            }; -- ext_static_proxy
            ext_yandexsport_login_status = {
              priority = 14;
              match_and = {
                {
                  match_fsm = {
                    host = "(m\\.)?yandexsport\\.[^.]+";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_or = {
                    {
                      match_fsm = {
                        URI = "/login-status\\.html";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_or
                };
              }; -- match_and
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
                          uuid = "8143817713432665990";
                          report = {
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
                                    attempts = 5;
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
                                    attempts = 5;
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
                                    attempts = 5;
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
                            refers = "antirobot";
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
                            }; -- shared
                          }; -- geo
                          rewrite = {
                            actions = {
                              {
                                regexp = ".*";
                                global = false;
                                literal = false;
                                header_name = "Host";
                                rewrite = "yandex.ru";
                                case_insensitive = false;
                              };
                            }; -- actions
                            balancer2 = {
                              watermark_policy = {
                                lo = 0.500;
                                hi = 0.600;
                                params_file = "./controls/watermark_policy.params_file";
                                unique_policy = {};
                              }; -- watermark_policy
                              attempts = 2;
                              attempts_file = "./controls/news.attempts";
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                morda_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_news_to_sas";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "500ms";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 74;
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
                                          { "sas1-0020.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:147:225:90ff:fe83:5a4"; };
                                          { "sas1-0382.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:144:225:90ff:fe83:b34"; };
                                          { "sas1-0539.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:152:225:90ff:fe83:af6"; };
                                          { "sas1-1128.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:13a:feaa:14ff:fede:414e"; };
                                          { "sas1-1244.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:150:225:90ff:fe83:1946"; };
                                          { "sas1-1627.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:179:215:b2ff:fea8:7174"; };
                                          { "sas1-1663.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:67a:215:b2ff:fea8:aa2"; };
                                          { "sas1-1681.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:67a:215:b2ff:fea8:bfa"; };
                                          { "sas1-1828.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bfc8"; };
                                          { "sas1-2002.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:624:922b:34ff:fecf:3c92"; };
                                          { "sas1-2188.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:62e:922b:34ff:fecf:2e9e"; };
                                          { "sas1-2628.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:60e:225:90ff:fe83:1a54"; };
                                          { "sas1-2724.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:606:225:90ff:fe83:e60"; };
                                          { "sas1-3818.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:103:225:90ff:fe83:1e10"; };
                                          { "sas1-3996.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:168:225:90ff:fe83:2d44"; };
                                          { "sas1-4255.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                          { "sas1-4407.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                          { "sas1-4498.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:643:96de:80ff:fe81:16f8"; };
                                          { "sas1-4528.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:626:96de:80ff:fe17:b766"; };
                                          { "sas1-4780.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:63b:96de:80ff:fe81:920"; };
                                          { "sas1-4798.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:66b:96de:80ff:fe81:17e0"; };
                                          { "sas1-4946.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:63b:96de:80ff:fe81:10cc"; };
                                          { "sas1-4987.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:642:96de:80ff:fe81:fe6"; };
                                          { "sas1-5003.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:642:96de:80ff:fe81:1680"; };
                                          { "sas1-5418.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:663:96de:80ff:fe81:a9a"; };
                                          { "sas1-5851.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:604:225:90ff:feed:2dc2"; };
                                          { "sas1-5855.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:604:225:90ff:feed:2c7e"; };
                                          { "sas1-5856.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:100:225:90ff:fee8:7f34"; };
                                          { "sas1-6272.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:163:225:90ff:fec1:d11c"; };
                                          { "sas1-6942.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b1a8"; };
                                          { "sas1-7007.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:16e:215:b2ff:fea7:9140"; };
                                          { "sas1-7043.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:120:215:b2ff:fea7:9050"; };
                                          { "sas1-7357.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc10"; };
                                          { "sas1-7581.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:129:215:b2ff:fea7:b968"; };
                                          { "sas1-7734.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:127:215:b2ff:fea7:bd40"; };
                                          { "sas1-7823.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                          { "sas1-7898.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab84"; };
                                          { "sas1-8018.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7268"; };
                                          { "sas1-8249.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:131:215:b2ff:fea7:b438"; };
                                          { "sas1-8710.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:137:215:b2ff:fea7:8ef0"; };
                                          { "sas2-0476.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:401:0:604:df5:d703"; };
                                          { "sas2-0743.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:40c:0:604:dde:f71a"; };
                                          { "sas2-1201.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6ad:e61d:2dff:fe14:6460"; };
                                          { "sas2-3213.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:1c3:225:90ff:fee8:f50c"; };
                                          { "sas2-3215.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c3:a236:9fff:fe34:c036"; };
                                          { "sas2-3802.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c4:428d:5cff:fe34:fd59"; };
                                          { "sas2-3803.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c4:428d:5cff:fe34:fd54"; };
                                          { "sas2-3880.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c8:92e2:baff:fea2:30c8"; };
                                          { "sas2-5959.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fc:96de:80ff:fe8c:be9c"; };
                                          { "sas2-5986.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:e570"; };
                                          { "sas2-6041.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:183:225:90ff:fe92:aff8"; };
                                          { "sas2-6061.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:e590"; };
                                          { "sas2-6212.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:b8d6"; };
                                          { "sas2-6217.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fc:96de:80ff:fe8c:ded4"; };
                                          { "sas2-6294.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:dd30"; };
                                          { "sas2-6297.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:d78c"; };
                                          { "sas2-6526.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:606:0:604:5e97:dd65"; };
                                          { "sas2-8128.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:607:0:604:4b28:c996"; };
                                          { "sas2-8146.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:607:0:604:4b28:c8d6"; };
                                          { "sas2-8845.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:606:0:604:5e97:dbee"; };
                                          { "sas2-8942.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:b10:0:604:141d:f200"; };
                                          { "sas2-9417.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:5e3:0:604:7a51:55de"; };
                                          { "sas3-0230.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:7b2:0:604:1461:e972"; };
                                          { "sas3-0240.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:7b2:0:604:1461:e8de"; };
                                          { "slovo001.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:66e:225:90ff:fe6b:c132"; };
                                          { "slovo056.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:1c2:922b:34ff:fecf:2d82"; };
                                          { "slovo066.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:600:922b:34ff:fecf:374c"; };
                                          { "slovo082.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:690:922b:34ff:fecc:7a68"; };
                                          { "slovo088.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:4076"; };
                                          { "slovo090.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:340c"; };
                                          { "slovo101.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:2a8e"; };
                                          { "slovo108.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:33f4"; };
                                          { "slovo142.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:26d4"; };
                                          { "slovo149.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:2234"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "50ms";
                                          backend_timeout = "5000ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "204";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- morda_sas
                                morda_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_morda_to_man";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "500ms";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 73;
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
                                          { "man1-0134.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6035:92e2:baff:fe6e:b612"; };
                                          { "man1-0391.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7d88"; };
                                          { "man1-0587.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:602b:92e2:baff:fe74:79ce"; };
                                          { "man1-0652.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6031:92e2:baff:fe74:79b0"; };
                                          { "man1-0725.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:ba42"; };
                                          { "man1-1048.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6010:92e2:baff:fe56:ea3a"; };
                                          { "man1-1315.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6016:f652:14ff:fe48:99b0"; };
                                          { "man1-1348.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:cab0"; };
                                          { "man1-1385.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b470"; };
                                          { "man1-1806.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b790"; };
                                          { "man1-1873.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:1d40"; };
                                          { "man1-2041.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:24e0"; };
                                          { "man1-2532.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:d0"; };
                                          { "man1-2849.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e820"; };
                                          { "man1-2865.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e160"; };
                                          { "man1-3240.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6000:f652:14ff:fe55:30b0"; };
                                          { "man1-3247.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6000:f652:14ff:fe55:3cf0"; };
                                          { "man1-3639.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7f2a"; };
                                          { "man1-3722.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:7e9c"; };
                                          { "man1-3872.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b8ec"; };
                                          { "man1-4054.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603c:92e2:baff:fe75:47d2"; };
                                          { "man1-4261.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:b6aa"; };
                                          { "man1-4286.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6043:92e2:baff:fe6f:7d74"; };
                                          { "man1-4331.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7c1c"; };
                                          { "man1-4355.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6041:92e2:baff:fe6f:7f74"; };
                                          { "man1-4439.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b818"; };
                                          { "man1-4648.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e8e0"; };
                                          { "man1-4653.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:cb90"; };
                                          { "man1-4719.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:97a0"; };
                                          { "man1-4751.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6047:f652:14ff:fef5:c750"; };
                                          { "man1-4882.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6047:e61d:2dff:fe6c:cab0"; };
                                          { "man1-5221.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:604d:e61d:2dff:fe00:9680"; };
                                          { "man1-7314.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e7d0"; };
                                          { "man1-7394.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6069:e61d:2dff:fe6c:df30"; };
                                          { "man1-7491.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:2590"; };
                                          { "man1-7534.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6503:215:b2ff:fea9:62fa"; };
                                          { "man1-7747.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:2cc0"; };
                                          { "man1-7853.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:606f:e61d:2dff:fe6d:3570"; };
                                          { "man1-9680.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72da"; };
                                          { "man2-0320.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:feaa:14ff:feea:8e83"; };
                                          { "man2-0350.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:428d:5cff:fe34:fce7"; };
                                          { "man2-0372.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6009:428d:5cff:fe34:fe0a"; };
                                          { "man2-0378.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6087:feaa:14ff:feea:8ebd"; };
                                          { "man2-0380.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:428d:5cff:fe34:fc40"; };
                                          { "man2-0396.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6087:428d:5cff:fe34:fdc2"; };
                                          { "man2-0413.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:428d:5cff:fe34:fe1e"; };
                                          { "man2-0415.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6009:428d:5cff:fe34:fdbf"; };
                                          { "man2-0460.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6087:feaa:14ff:feea:8ec0"; };
                                          { "man2-0474.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6095:92e2:baff:fea2:363e"; };
                                          { "man2-0491.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6095:92e2:baff:fea2:381c"; };
                                          { "man2-1066.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:60a:0:604:2d6c:d470"; };
                                          { "man2-1240.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:824:0:604:5e97:dadc"; };
                                          { "man2-1244.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:828:0:604:5e97:de3c"; };
                                          { "man2-1248.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:823:0:604:5e97:e0b2"; };
                                          { "man2-1267.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:828:0:604:5e97:df17"; };
                                          { "man2-1277.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                          { "man2-1297.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:823:0:604:5e97:dd7e"; };
                                          { "man2-1314.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:824:0:604:5e97:e0b1"; };
                                          { "man2-1604.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:82a:0:604:5e97:db03"; };
                                          { "man2-1657.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                          { "man2-1659.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:6d:0:604:14dd:f0d1"; };
                                          { "man2-1664.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:6d:0:604:14dd:f0d2"; };
                                          { "man2-1699.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:847:0:604:5ecc:ee3c"; };
                                          { "man2-2269.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:826:0:604:5e97:dbe9"; };
                                          { "man2-2273.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:827:0:604:5e97:df37"; };
                                          { "man2-6409.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:83d:0:604:14a7:67d7"; };
                                          { "man2-6454.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:640:0:604:14a9:6919"; };
                                          { "man2-6655.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:642:0:604:14a7:67d2"; };
                                          { "man2-6812.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:836:0:604:14a7:66c9"; };
                                          { "man2-6983.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:649:0:604:14a7:b9d2"; };
                                          { "man2-7071.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:64b:0:604:14a7:ba41"; };
                                          { "man2-7283.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:655:0:604:14a7:ba8e"; };
                                          { "man2-7339.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:83d:0:604:14a7:6720"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "50ms";
                                          backend_timeout = "5000ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "204";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- morda_man
                                morda_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_morda_to_vla";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "500ms";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 75;
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
                                          { "vla1-0243.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:9e:0:604:db7:a8a0"; };
                                          { "vla1-0395.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:9d:0:604:d8f:eb84"; };
                                          { "vla1-0532.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:1b:0:604:db7:9988"; };
                                          { "vla1-0541.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12:0:604:db7:9b88"; };
                                          { "vla1-0548.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                          { "vla1-0630.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:13:0:604:db7:9b0d"; };
                                          { "vla1-0636.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:27:0:604:db7:9e94"; };
                                          { "vla1-0661.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12:0:604:db7:9b48"; };
                                          { "vla1-0721.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:53:0:604:db7:9d1d"; };
                                          { "vla1-0806.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:6a:0:604:db7:a378"; };
                                          { "vla1-1064.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:8d:0:604:db7:ab3f"; };
                                          { "vla1-1387.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:54:0:604:db7:a6bd"; };
                                          { "vla1-1416.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:43:0:604:db7:a117"; };
                                          { "vla1-1560.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:69:0:604:db7:a403"; };
                                          { "vla1-1642.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:53:0:604:db7:9d61"; };
                                          { "vla1-1854.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:59:0:604:4b16:3d00"; };
                                          { "vla1-1941.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:88:0:604:db7:a76f"; };
                                          { "vla1-1997.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:1b:0:604:db7:997c"; };
                                          { "vla1-2001.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:71:0:604:db7:a749"; };
                                          { "vla1-2335.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:86:0:604:db7:a839"; };
                                          { "vla1-2425.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:95:0:604:db7:a7cb"; };
                                          { "vla1-2450.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:8b:0:604:d8f:eb00"; };
                                          { "vla1-2490.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                          { "vla1-2536.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a2:0:604:db7:9be2"; };
                                          { "vla1-2592.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:1f:0:604:db7:9c97"; };
                                          { "vla1-2688.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a2:0:604:db7:9f80"; };
                                          { "vla1-2889.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:74:0:604:db7:99a3"; };
                                          { "vla1-2917.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:32:0:604:db7:990c"; };
                                          { "vla1-2958.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:70:0:604:db7:a233"; };
                                          { "vla1-3108.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:72:0:604:db7:a2a5"; };
                                          { "vla1-3128.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a3:0:604:db7:a377"; };
                                          { "vla1-3185.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:70:0:604:db7:a231"; };
                                          { "vla1-3349.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:32:0:604:db7:9ca5"; };
                                          { "vla1-3475.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:13b:0:604:db7:a645"; };
                                          { "vla1-3616.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a0:0:604:db7:a502"; };
                                          { "vla1-3684.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:63:0:604:db7:9e55"; };
                                          { "vla1-3702.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:87:0:604:db7:a84d"; };
                                          { "vla1-3703.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:97:0:604:db7:a9f6"; };
                                          { "vla1-3724.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:86:0:604:db7:ab99"; };
                                          { "vla1-3725.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:86:0:604:db7:abb2"; };
                                          { "vla1-4220.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:72:0:604:db7:a70e"; };
                                          { "vla1-4397.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:95:0:604:db7:a9bc"; };
                                          { "vla1-4427.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:87:0:604:db7:a84a"; };
                                          { "vla1-4567.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:77:0:604:db7:a6f2"; };
                                          { "vla1-4568.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:77:0:604:db7:aa15"; };
                                          { "vla1-4574.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:77:0:604:db7:aa54"; };
                                          { "vla1-5521.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12e:0:604:5e18:d4c"; };
                                          { "vla1-5742.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:108:0:604:dbc:a4e1"; };
                                          { "vla1-5975.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12b:0:604:5e18:c82"; };
                                          { "vla1-6009.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:10d:0:604:db7:a24b"; };
                                          { "vla1-6022.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:10d:0:604:dbc:a0e2"; };
                                          { "vla2-0964.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:15e:0:604:5e92:ceeb"; };
                                          { "vla2-1009.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:4b02:794a"; };
                                          { "vla2-1014.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b14:5ce2"; };
                                          { "vla2-1015.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b16:5bd4"; };
                                          { "vla2-1021.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b14:628e"; };
                                          { "vla2-1022.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b14:5ae6"; };
                                          { "vla2-1027.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                          { "vla2-1029.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:4b02:7736"; };
                                          { "vla2-1031.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:9ad9:197a"; };
                                          { "vla2-5845.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:dee3"; };
                                          { "vla2-5846.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d941"; };
                                          { "vla2-5862.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d974"; };
                                          { "vla2-5864.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d970"; };
                                          { "vla2-5865.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:34d:0:604:5e19:453d"; };
                                          { "vla2-5869.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:34d:0:604:5e19:4533"; };
                                          { "vla2-5876.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:db99"; };
                                          { "vla2-5889.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:e24f"; };
                                          { "vla2-5892.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:dee1"; };
                                          { "vla2-5895.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d984"; };
                                          { "vla2-5900.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d942"; };
                                          { "vla2-5949.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:70c:0:604:4bd7:4f0c"; };
                                          { "vla2-5954.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:716:0:604:4b3e:251c"; };
                                          { "vla2-5959.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:716:0:604:4b3e:2514"; };
                                          { "vla2-7705.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:726:0:604:5ecc:f31f"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "50ms";
                                          backend_timeout = "5000ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "204";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- morda_vla
                              }; -- rr
                            }; -- balancer2
                          }; -- rewrite
                        }; -- geobase
                      }; -- module
                    }; -- antirobot
                  }; -- cutter
                }; -- h100
              }; -- hasher
            }; -- ext_yandexsport_login_status
            ext_yandexsport_efir = {
              priority = 13;
              match_and = {
                {
                  match_fsm = {
                    host = "(m\\.)?yandexsport\\.[^.]+";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_or = {
                    {
                      match_fsm = {
                        URI = "/portal.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/efir.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/instant.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/service-workers.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_or
                };
              }; -- match_and
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
                          uuid = "8143817713432665990";
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
                          report = {
                            uuid = "yandexsport_efir";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            regexp = {
                              service_workers = {
                                priority = 2;
                                match_fsm = {
                                  URI = "/service-workers.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  watermark_policy = {
                                    lo = 0.500;
                                    hi = 0.600;
                                    params_file = "./controls/watermark_policy.params_file";
                                    unique_policy = {};
                                  }; -- watermark_policy
                                  attempts = 2;
                                  attempts_file = "./controls/news.attempts";
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    service_workers_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_efir_service_workers_to_sas";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "500ms";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 5;
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
                                              { "sas1-0732-sas-service-workers--5f0-22085.gencfg-c.yandex.net"; 22085; 80.000; "2a02:6b8:c08:1410:10e:b380:0:5645"; };
                                              { "sas1-2702-sas-service-workers--5f0-22085.gencfg-c.yandex.net"; 22085; 80.000; "2a02:6b8:c08:1f03:10e:b380:0:5645"; };
                                              { "sas1-4550-sas-service-workers--5f0-22085.gencfg-c.yandex.net"; 22085; 80.000; "2a02:6b8:c08:a006:10e:b380:0:5645"; };
                                              { "sas1-4612-sas-service-workers--5f0-22085.gencfg-c.yandex.net"; 22085; 80.000; "2a02:6b8:c08:24af:10e:b380:0:5645"; };
                                              { "sas1-8554-97e-sas-service-work-5f0-22085.gencfg-c.yandex.net"; 22085; 80.000; "2a02:6b8:c08:c827:10e:b380:0:5645"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "50ms";
                                              backend_timeout = "5000ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                              status_code_blacklist = {
                                                "204";
                                              }; -- status_code_blacklist
                                            }))
                                          }; -- weighted2
                                          attempts_rate_limiter = {
                                            limit = 0.200;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- service_workers_sas
                                    service_workers_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_efir_service_workers_to_man";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "500ms";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 5;
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
                                              { "man1-1124-man-service-workers--7dd-28987.gencfg-c.yandex.net"; 28987; 80.000; "2a02:6b8:c0b:1ff5:10e:b381:0:713b"; };
                                              { "man2-0551-man-service-workers--7dd-28987.gencfg-c.yandex.net"; 28987; 80.000; "2a02:6b8:c0b:4f00:10e:b381:0:713b"; };
                                              { "man2-1271-man-service-workers--7dd-28987.gencfg-c.yandex.net"; 28987; 80.000; "2a02:6b8:c13:1214:10e:b381:0:713b"; };
                                              { "man2-3532-742-man-service-work-7dd-28987.gencfg-c.yandex.net"; 28987; 80.000; "2a02:6b8:c0b:579e:10e:b381:0:713b"; };
                                              { "man2-4169-fd1-man-service-work-7dd-28987.gencfg-c.yandex.net"; 28987; 80.000; "2a02:6b8:c0b:571b:10e:b381:0:713b"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "50ms";
                                              backend_timeout = "5000ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                              status_code_blacklist = {
                                                "204";
                                              }; -- status_code_blacklist
                                            }))
                                          }; -- weighted2
                                          attempts_rate_limiter = {
                                            limit = 0.200;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- service_workers_man
                                    service_workers_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_efir_service_workers_to_vla";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "500ms";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 5;
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
                                              { "vla1-1672-248-vla-service-work-487-23376.gencfg-c.yandex.net"; 23376; 80.000; "2a02:6b8:c0f:590a:10e:b37f:0:5b50"; };
                                              { "vla1-4207-vla-service-workers--487-23376.gencfg-c.yandex.net"; 23376; 80.000; "2a02:6b8:c0d:4c8a:10e:b37f:0:5b50"; };
                                              { "vla1-5976-vla-service-workers--487-23376.gencfg-c.yandex.net"; 23376; 80.000; "2a02:6b8:c0f:1593:10e:b37f:0:5b50"; };
                                              { "vla2-0481-fbe-vla-service-work-487-23376.gencfg-c.yandex.net"; 23376; 80.000; "2a02:6b8:c0f:420f:10e:b37f:0:5b50"; };
                                              { "vla2-5850-f45-vla-service-work-487-23376.gencfg-c.yandex.net"; 23376; 80.000; "2a02:6b8:c15:3604:10e:b37f:0:5b50"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "50ms";
                                              backend_timeout = "5000ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                              status_code_blacklist = {
                                                "204";
                                              }; -- status_code_blacklist
                                            }))
                                          }; -- weighted2
                                          attempts_rate_limiter = {
                                            limit = 0.200;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- service_workers_vla
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 500;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- service_workers
                              default = {
                                priority = 1;
                                balancer2 = {
                                  watermark_policy = {
                                    lo = 0.500;
                                    hi = 0.600;
                                    params_file = "./controls/watermark_policy.params_file";
                                    unique_policy = {};
                                  }; -- watermark_policy
                                  attempts = 2;
                                  attempts_file = "./controls/news.attempts";
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    morda_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_efir_to_sas";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "500ms";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 74;
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
                                              { "sas1-0020.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:147:225:90ff:fe83:5a4"; };
                                              { "sas1-0382.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:144:225:90ff:fe83:b34"; };
                                              { "sas1-0539.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:152:225:90ff:fe83:af6"; };
                                              { "sas1-1128.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:13a:feaa:14ff:fede:414e"; };
                                              { "sas1-1244.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:150:225:90ff:fe83:1946"; };
                                              { "sas1-1627.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:179:215:b2ff:fea8:7174"; };
                                              { "sas1-1663.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:67a:215:b2ff:fea8:aa2"; };
                                              { "sas1-1681.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:67a:215:b2ff:fea8:bfa"; };
                                              { "sas1-1828.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bfc8"; };
                                              { "sas1-2002.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:624:922b:34ff:fecf:3c92"; };
                                              { "sas1-2188.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:62e:922b:34ff:fecf:2e9e"; };
                                              { "sas1-2628.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:60e:225:90ff:fe83:1a54"; };
                                              { "sas1-2724.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:606:225:90ff:fe83:e60"; };
                                              { "sas1-3818.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:103:225:90ff:fe83:1e10"; };
                                              { "sas1-3996.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:168:225:90ff:fe83:2d44"; };
                                              { "sas1-4255.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                              { "sas1-4407.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                              { "sas1-4498.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:643:96de:80ff:fe81:16f8"; };
                                              { "sas1-4528.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:626:96de:80ff:fe17:b766"; };
                                              { "sas1-4780.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:63b:96de:80ff:fe81:920"; };
                                              { "sas1-4798.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:66b:96de:80ff:fe81:17e0"; };
                                              { "sas1-4946.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:63b:96de:80ff:fe81:10cc"; };
                                              { "sas1-4987.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:642:96de:80ff:fe81:fe6"; };
                                              { "sas1-5003.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:642:96de:80ff:fe81:1680"; };
                                              { "sas1-5418.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:663:96de:80ff:fe81:a9a"; };
                                              { "sas1-5851.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:604:225:90ff:feed:2dc2"; };
                                              { "sas1-5855.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:604:225:90ff:feed:2c7e"; };
                                              { "sas1-5856.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:100:225:90ff:fee8:7f34"; };
                                              { "sas1-6272.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:163:225:90ff:fec1:d11c"; };
                                              { "sas1-6942.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b1a8"; };
                                              { "sas1-7007.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:16e:215:b2ff:fea7:9140"; };
                                              { "sas1-7043.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:120:215:b2ff:fea7:9050"; };
                                              { "sas1-7357.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc10"; };
                                              { "sas1-7581.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:129:215:b2ff:fea7:b968"; };
                                              { "sas1-7734.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:127:215:b2ff:fea7:bd40"; };
                                              { "sas1-7823.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                              { "sas1-7898.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab84"; };
                                              { "sas1-8018.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7268"; };
                                              { "sas1-8249.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:131:215:b2ff:fea7:b438"; };
                                              { "sas1-8710.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:137:215:b2ff:fea7:8ef0"; };
                                              { "sas2-0476.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:401:0:604:df5:d703"; };
                                              { "sas2-0743.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:40c:0:604:dde:f71a"; };
                                              { "sas2-1201.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6ad:e61d:2dff:fe14:6460"; };
                                              { "sas2-3213.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:1c3:225:90ff:fee8:f50c"; };
                                              { "sas2-3215.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c3:a236:9fff:fe34:c036"; };
                                              { "sas2-3802.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c4:428d:5cff:fe34:fd59"; };
                                              { "sas2-3803.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c4:428d:5cff:fe34:fd54"; };
                                              { "sas2-3880.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6c8:92e2:baff:fea2:30c8"; };
                                              { "sas2-5959.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fc:96de:80ff:fe8c:be9c"; };
                                              { "sas2-5986.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:e570"; };
                                              { "sas2-6041.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:183:225:90ff:fe92:aff8"; };
                                              { "sas2-6061.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:e590"; };
                                              { "sas2-6212.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:b8d6"; };
                                              { "sas2-6217.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fc:96de:80ff:fe8c:ded4"; };
                                              { "sas2-6294.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6fa:96de:80ff:fe8c:dd30"; };
                                              { "sas2-6297.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:d78c"; };
                                              { "sas2-6526.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:606:0:604:5e97:dd65"; };
                                              { "sas2-8128.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:607:0:604:4b28:c996"; };
                                              { "sas2-8146.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:607:0:604:4b28:c8d6"; };
                                              { "sas2-8845.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:606:0:604:5e97:dbee"; };
                                              { "sas2-8942.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:b10:0:604:141d:f200"; };
                                              { "sas2-9417.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:5e3:0:604:7a51:55de"; };
                                              { "sas3-0230.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:7b2:0:604:1461:e972"; };
                                              { "sas3-0240.search.yandex.net"; 29783; 120.000; "2a02:6b8:c02:7b2:0:604:1461:e8de"; };
                                              { "slovo001.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:66e:225:90ff:fe6b:c132"; };
                                              { "slovo056.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:1c2:922b:34ff:fecf:2d82"; };
                                              { "slovo066.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:600:922b:34ff:fecf:374c"; };
                                              { "slovo082.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:690:922b:34ff:fecc:7a68"; };
                                              { "slovo088.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:4076"; };
                                              { "slovo090.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:340c"; };
                                              { "slovo101.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:2a8e"; };
                                              { "slovo108.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:33f4"; };
                                              { "slovo142.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:26d4"; };
                                              { "slovo149.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:64d:922b:34ff:fecf:2234"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "50ms";
                                              backend_timeout = "5000ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                              status_code_blacklist = {
                                                "204";
                                              }; -- status_code_blacklist
                                            }))
                                          }; -- weighted2
                                          attempts_rate_limiter = {
                                            limit = 0.200;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- morda_sas
                                    morda_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_efir_to_man";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "500ms";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 73;
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
                                              { "man1-0134.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6035:92e2:baff:fe6e:b612"; };
                                              { "man1-0391.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7d88"; };
                                              { "man1-0587.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:602b:92e2:baff:fe74:79ce"; };
                                              { "man1-0652.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6031:92e2:baff:fe74:79b0"; };
                                              { "man1-0725.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:ba42"; };
                                              { "man1-1048.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6010:92e2:baff:fe56:ea3a"; };
                                              { "man1-1315.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6016:f652:14ff:fe48:99b0"; };
                                              { "man1-1348.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:cab0"; };
                                              { "man1-1385.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b470"; };
                                              { "man1-1806.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b790"; };
                                              { "man1-1873.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:1d40"; };
                                              { "man1-2041.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:24e0"; };
                                              { "man1-2532.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:d0"; };
                                              { "man1-2849.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e820"; };
                                              { "man1-2865.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e160"; };
                                              { "man1-3240.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6000:f652:14ff:fe55:30b0"; };
                                              { "man1-3247.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6000:f652:14ff:fe55:3cf0"; };
                                              { "man1-3639.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7f2a"; };
                                              { "man1-3722.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:7e9c"; };
                                              { "man1-3872.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b8ec"; };
                                              { "man1-4054.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603c:92e2:baff:fe75:47d2"; };
                                              { "man1-4261.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:b6aa"; };
                                              { "man1-4286.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6043:92e2:baff:fe6f:7d74"; };
                                              { "man1-4331.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7c1c"; };
                                              { "man1-4355.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6041:92e2:baff:fe6f:7f74"; };
                                              { "man1-4439.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b818"; };
                                              { "man1-4648.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e8e0"; };
                                              { "man1-4653.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:cb90"; };
                                              { "man1-4719.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:97a0"; };
                                              { "man1-4751.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6047:f652:14ff:fef5:c750"; };
                                              { "man1-4882.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6047:e61d:2dff:fe6c:cab0"; };
                                              { "man1-5221.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:604d:e61d:2dff:fe00:9680"; };
                                              { "man1-7314.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e7d0"; };
                                              { "man1-7394.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6069:e61d:2dff:fe6c:df30"; };
                                              { "man1-7491.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:2590"; };
                                              { "man1-7534.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6503:215:b2ff:fea9:62fa"; };
                                              { "man1-7747.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:2cc0"; };
                                              { "man1-7853.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:606f:e61d:2dff:fe6d:3570"; };
                                              { "man1-9680.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72da"; };
                                              { "man2-0320.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:feaa:14ff:feea:8e83"; };
                                              { "man2-0350.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:428d:5cff:fe34:fce7"; };
                                              { "man2-0372.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6009:428d:5cff:fe34:fe0a"; };
                                              { "man2-0378.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6087:feaa:14ff:feea:8ebd"; };
                                              { "man2-0380.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:428d:5cff:fe34:fc40"; };
                                              { "man2-0396.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6087:428d:5cff:fe34:fdc2"; };
                                              { "man2-0413.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:600b:428d:5cff:fe34:fe1e"; };
                                              { "man2-0415.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6009:428d:5cff:fe34:fdbf"; };
                                              { "man2-0460.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6087:feaa:14ff:feea:8ec0"; };
                                              { "man2-0474.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6095:92e2:baff:fea2:363e"; };
                                              { "man2-0491.search.yandex.net"; 27474; 120.000; "2a02:6b8:b000:6095:92e2:baff:fea2:381c"; };
                                              { "man2-1066.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:60a:0:604:2d6c:d470"; };
                                              { "man2-1240.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:824:0:604:5e97:dadc"; };
                                              { "man2-1244.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:828:0:604:5e97:de3c"; };
                                              { "man2-1248.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:823:0:604:5e97:e0b2"; };
                                              { "man2-1267.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:828:0:604:5e97:df17"; };
                                              { "man2-1277.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                              { "man2-1297.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:823:0:604:5e97:dd7e"; };
                                              { "man2-1314.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:824:0:604:5e97:e0b1"; };
                                              { "man2-1604.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:82a:0:604:5e97:db03"; };
                                              { "man2-1657.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                              { "man2-1659.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:6d:0:604:14dd:f0d1"; };
                                              { "man2-1664.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:6d:0:604:14dd:f0d2"; };
                                              { "man2-1699.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:847:0:604:5ecc:ee3c"; };
                                              { "man2-2269.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:826:0:604:5e97:dbe9"; };
                                              { "man2-2273.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:827:0:604:5e97:df37"; };
                                              { "man2-6409.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:83d:0:604:14a7:67d7"; };
                                              { "man2-6454.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:640:0:604:14a9:6919"; };
                                              { "man2-6655.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:642:0:604:14a7:67d2"; };
                                              { "man2-6812.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:836:0:604:14a7:66c9"; };
                                              { "man2-6983.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:649:0:604:14a7:b9d2"; };
                                              { "man2-7071.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:64b:0:604:14a7:ba41"; };
                                              { "man2-7283.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:655:0:604:14a7:ba8e"; };
                                              { "man2-7339.search.yandex.net"; 27474; 120.000; "2a02:6b8:c01:83d:0:604:14a7:6720"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "50ms";
                                              backend_timeout = "5000ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                              status_code_blacklist = {
                                                "204";
                                              }; -- status_code_blacklist
                                            }))
                                          }; -- weighted2
                                          attempts_rate_limiter = {
                                            limit = 0.200;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- morda_man
                                    morda_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_efir_to_vla";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "500ms";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 75;
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
                                              { "vla1-0243.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:9e:0:604:db7:a8a0"; };
                                              { "vla1-0395.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:9d:0:604:d8f:eb84"; };
                                              { "vla1-0532.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:1b:0:604:db7:9988"; };
                                              { "vla1-0541.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12:0:604:db7:9b88"; };
                                              { "vla1-0548.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                              { "vla1-0630.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:13:0:604:db7:9b0d"; };
                                              { "vla1-0636.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:27:0:604:db7:9e94"; };
                                              { "vla1-0661.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12:0:604:db7:9b48"; };
                                              { "vla1-0721.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:53:0:604:db7:9d1d"; };
                                              { "vla1-0806.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:6a:0:604:db7:a378"; };
                                              { "vla1-1064.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:8d:0:604:db7:ab3f"; };
                                              { "vla1-1387.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:54:0:604:db7:a6bd"; };
                                              { "vla1-1416.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:43:0:604:db7:a117"; };
                                              { "vla1-1560.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:69:0:604:db7:a403"; };
                                              { "vla1-1642.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:53:0:604:db7:9d61"; };
                                              { "vla1-1854.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:59:0:604:4b16:3d00"; };
                                              { "vla1-1941.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:88:0:604:db7:a76f"; };
                                              { "vla1-1997.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:1b:0:604:db7:997c"; };
                                              { "vla1-2001.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:71:0:604:db7:a749"; };
                                              { "vla1-2335.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:86:0:604:db7:a839"; };
                                              { "vla1-2425.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:95:0:604:db7:a7cb"; };
                                              { "vla1-2450.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:8b:0:604:d8f:eb00"; };
                                              { "vla1-2490.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                              { "vla1-2536.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a2:0:604:db7:9be2"; };
                                              { "vla1-2592.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:1f:0:604:db7:9c97"; };
                                              { "vla1-2688.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a2:0:604:db7:9f80"; };
                                              { "vla1-2889.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:74:0:604:db7:99a3"; };
                                              { "vla1-2917.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:32:0:604:db7:990c"; };
                                              { "vla1-2958.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:70:0:604:db7:a233"; };
                                              { "vla1-3108.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:72:0:604:db7:a2a5"; };
                                              { "vla1-3128.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a3:0:604:db7:a377"; };
                                              { "vla1-3185.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:70:0:604:db7:a231"; };
                                              { "vla1-3349.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:32:0:604:db7:9ca5"; };
                                              { "vla1-3475.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:13b:0:604:db7:a645"; };
                                              { "vla1-3616.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:a0:0:604:db7:a502"; };
                                              { "vla1-3684.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:63:0:604:db7:9e55"; };
                                              { "vla1-3702.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:87:0:604:db7:a84d"; };
                                              { "vla1-3703.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:97:0:604:db7:a9f6"; };
                                              { "vla1-3724.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:86:0:604:db7:ab99"; };
                                              { "vla1-3725.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:86:0:604:db7:abb2"; };
                                              { "vla1-4220.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:72:0:604:db7:a70e"; };
                                              { "vla1-4397.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:95:0:604:db7:a9bc"; };
                                              { "vla1-4427.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:87:0:604:db7:a84a"; };
                                              { "vla1-4567.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:77:0:604:db7:a6f2"; };
                                              { "vla1-4568.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:77:0:604:db7:aa15"; };
                                              { "vla1-4574.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:77:0:604:db7:aa54"; };
                                              { "vla1-5521.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12e:0:604:5e18:d4c"; };
                                              { "vla1-5742.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:108:0:604:dbc:a4e1"; };
                                              { "vla1-5975.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:12b:0:604:5e18:c82"; };
                                              { "vla1-6009.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:10d:0:604:db7:a24b"; };
                                              { "vla1-6022.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:10d:0:604:dbc:a0e2"; };
                                              { "vla2-0964.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:15e:0:604:5e92:ceeb"; };
                                              { "vla2-1009.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:4b02:794a"; };
                                              { "vla2-1014.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b14:5ce2"; };
                                              { "vla2-1015.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b16:5bd4"; };
                                              { "vla2-1021.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b14:628e"; };
                                              { "vla2-1022.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:337:0:604:4b14:5ae6"; };
                                              { "vla2-1027.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                              { "vla2-1029.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:4b02:7736"; };
                                              { "vla2-1031.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:409:0:604:9ad9:197a"; };
                                              { "vla2-5845.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:dee3"; };
                                              { "vla2-5846.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d941"; };
                                              { "vla2-5862.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d974"; };
                                              { "vla2-5864.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d970"; };
                                              { "vla2-5865.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:34d:0:604:5e19:453d"; };
                                              { "vla2-5869.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:34d:0:604:5e19:4533"; };
                                              { "vla2-5876.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:db99"; };
                                              { "vla2-5889.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:e24f"; };
                                              { "vla2-5892.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:dee1"; };
                                              { "vla2-5895.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d984"; };
                                              { "vla2-5900.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:36c:0:604:5e97:d942"; };
                                              { "vla2-5949.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:70c:0:604:4bd7:4f0c"; };
                                              { "vla2-5954.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:716:0:604:4b3e:251c"; };
                                              { "vla2-5959.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:716:0:604:4b3e:2514"; };
                                              { "vla2-7705.search.yandex.net"; 16436; 120.000; "2a02:6b8:c0e:726:0:604:5ecc:f31f"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "50ms";
                                              backend_timeout = "5000ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                              status_code_blacklist = {
                                                "204";
                                              }; -- status_code_blacklist
                                            }))
                                          }; -- weighted2
                                          attempts_rate_limiter = {
                                            limit = 0.200;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- morda_vla
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 500;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- default
                            }; -- regexp
                          }; -- report
                        }; -- geobase
                      }; -- module
                    }; -- antirobot
                  }; -- cutter
                }; -- h100
              }; -- hasher
            }; -- ext_yandexsport_efir
            ext_yandexsport_metrika = {
              priority = 12;
              match_fsm = {
                host = "mc.yandexsport.ru";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              rewrite = {
                actions = {
                  {
                    regexp = ".*";
                    global = false;
                    literal = false;
                    header_name = "Host";
                    case_insensitive = false;
                    rewrite = "mc.yandexsport.ru";
                  };
                }; -- actions
                balancer2 = {
                  unique_policy = {};
                  attempts = 3;
                  rr = {
                    unpack(gen_proxy_backends({
                      { "mc-internal.metrika.yandex.net"; 8080; 1.000; "2a02:6b8:0:3400::735"; };
                    }, {
                      resolve_timeout = "10ms";
                      connect_timeout = "100ms";
                      backend_timeout = "5000ms";
                      fail_on_5xx = true;
                      http_backend = true;
                      buffering = false;
                      keepalive_count = 1;
                      need_resolve = true;
                    }))
                  }; -- rr
                }; -- balancer2
              }; -- rewrite
            }; -- ext_yandexsport_metrika
            ext_yandexsport_live = {
              priority = 11;
              match_and = {
                {
                  match_fsm = {
                    host = "(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    path = "/sport/live/?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_and
              shared = {
                uuid = "5546963713335085101";
                shared = {
                  uuid = "nginx_proxy";
                }; -- shared
              }; -- shared
            }; -- ext_yandexsport_live
            ext_yandex_nginx = {
              priority = 10;
              match_and = {
                {
                  match_fsm = {
                    host = "(www\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    path = "(/sport|/mirror)/api/v1/.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_and
              shared = {
                uuid = "5546963713335085101";
              }; -- shared
            }; -- ext_yandex_nginx
            ext_yandexsport_favicon = {
              priority = 9;
              match_and = {
                {
                  match_fsm = {
                    path = "/sport/favicon\\.ico";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    host = "(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_and
              rewrite = {
                actions = {
                  {
                    regexp = ".*";
                    global = false;
                    literal = false;
                    header_name = "Host";
                    case_insensitive = false;
                    rewrite = "news.s3.mds.yandex.net";
                  };
                  {
                    global = false;
                    split = "path";
                    literal = false;
                    regexp = "/(.*)";
                    case_insensitive = false;
                    rewrite = "/yandexsport/favicon.ico";
                  };
                }; -- actions
                shared = {
                  uuid = "2767060164238971050";
                  balancer2 = {
                    simple_policy = {};
                    attempts = 3;
                    rr = {
                      unpack(gen_proxy_backends({
                        { "news.s3.mds.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3:147"; };
                      }, {
                        resolve_timeout = "10ms";
                        connect_timeout = "100ms";
                        backend_timeout = "1000ms";
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
                }; -- shared
              }; -- rewrite
            }; -- ext_yandexsport_favicon
            ext_yandexsport_static = {
              priority = 8;
              match_and = {
                {
                  match_fsm = {
                    path = "/legal/.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    host = "(m\\.)?yandexsport\\.(stable\\.priemka\\.yandex\\.)?[^.]+";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_and
              rewrite = {
                actions = {
                  {
                    regexp = ".*";
                    global = false;
                    literal = false;
                    header_name = "Host";
                    case_insensitive = false;
                    rewrite = "news.s3.mds.yandex.net";
                  };
                  {
                    global = false;
                    split = "path";
                    literal = false;
                    regexp = "/legal/(.*)";
                    case_insensitive = false;
                    rewrite = "/yandexsport/%1.pdf";
                  };
                }; -- actions
                shared = {
                  uuid = "2767060164238971050";
                }; -- shared
              }; -- rewrite
            }; -- ext_yandexsport_static
            ext_yandexsport_proxies = {
              priority = 7;
              match_and = {
                {
                  match_or = {
                    {
                      match_fsm = {
                        host = "(m\\.)?news\\.yandex\\.[^.]+";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_and = {
                        {
                          match_fsm = {
                            host = "(www\\.)?(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_fsm = {
                            path = "/sport.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_and
                    };
                  }; -- match_or
                };
                {
                  match_or = {
                    {
                      match_fsm = {
                        URI = "/sport/live_info/([^-?]+)/([^-?]+)";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/sport/live_comments/([^-?]+)/([^-?]+)";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_or
                };
              }; -- match_and
              rewrite = {
                actions = {
                  {
                    global = false;
                    literal = false;
                    case_insensitive = false;
                    rewrite = "/v2/%1/events/%2?preset=yandexsport";
                    regexp = "/sport/live_info/(\\w+)/(\\w+)(\\?(.+))?";
                  };
                  {
                    global = false;
                    literal = false;
                    case_insensitive = false;
                    rewrite = "/v2/%1/events/%2/livecomments?tail=5";
                    regexp = "/sport/live_comments/(\\w+)/(\\w+)(\\?(.+))?";
                  };
                  {
                    regexp = ".*";
                    global = false;
                    literal = false;
                    header_name = "Host";
                    case_insensitive = false;
                    rewrite = "api.sport.yandex.ru";
                  };
                }; -- actions
                balancer2 = {
                  simple_policy = {};
                  attempts = 1;
                  rr = {
                    unpack(gen_proxy_backends({
                      { "api.sport.yandex.ru"; 80; 1.000; "2a02:6b8::e"; };
                    }, {
                      resolve_timeout = "10ms";
                      connect_timeout = "100ms";
                      backend_timeout = "1000ms";
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
            }; -- ext_yandexsport_proxies
            ext_yandexsport = {
              priority = 6;
              match_or = {
                {
                  match_fsm = {
                    host = "(.+\\.)?yandexsport\\..*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    host = "sportyandex\\..*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_and = {
                    {
                      match_fsm = {
                        host = "(www\\.)?(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        path = "/sport.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_and
                };
              }; -- match_or
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
                regexp = {
                  turbo = {
                    priority = 7;
                    match_and = {
                      {
                        match_fsm = {
                          host = "(.+\\.)?yandexsport\\..*";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          URI = "/turbo";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                    }; -- match_and
                    balancer2 = {
                      by_name_policy = {
                        name = suffix_with_dc("api");
                        unique_policy = {};
                      }; -- by_name_policy
                      attempts = 2;
                      attempts_file = "./controls/admin.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control.weights";
                        api_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_news_sport_api_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "3000ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 2;
                              connection_attempts = 32;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "sas1-0444-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:12a3:10d:520c:0:51e5"; };
                                  { "sas1-0448-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:1404:10d:520c:0:51e5"; };
                                  { "sas1-0678-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:ca7:10d:520c:0:51e5"; };
                                  { "sas1-0861-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:e99:10d:520c:0:51e5"; };
                                  { "sas1-1112-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:379d:10d:520c:0:51e5"; };
                                  { "sas1-1148-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:326:10d:520c:0:51e5"; };
                                  { "sas1-1244-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:1c1b:10d:520c:0:51e5"; };
                                  { "sas1-1248-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:19a8:10d:520c:0:51e5"; };
                                  { "sas1-1389-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:1f91:10d:520c:0:51e5"; };
                                  { "sas1-2257-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:2919:10d:520c:0:51e5"; };
                                  { "sas1-2430-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:2815:10d:520c:0:51e5"; };
                                  { "sas1-2536-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:189e:10d:520c:0:51e5"; };
                                  { "sas1-2687-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:1618:10d:520c:0:51e5"; };
                                  { "sas1-4590-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:22a5:10d:520c:0:51e5"; };
                                  { "sas1-4596-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:2180:10d:520c:0:51e5"; };
                                  { "sas1-4609-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:399c:10d:520c:0:51e5"; };
                                  { "sas1-4799-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:7e97:10d:520c:0:51e5"; };
                                  { "sas1-4908-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:3328:10d:520c:0:51e5"; };
                                  { "sas1-5299-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:2228:10d:520c:0:51e5"; };
                                  { "sas1-5321-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:39a5:10d:520c:0:51e5"; };
                                  { "sas1-5530-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:3689:10d:520c:0:51e5"; };
                                  { "sas1-5589-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:2880:10d:520c:0:51e5"; };
                                  { "sas1-6005-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:6a28:10d:520c:0:51e5"; };
                                  { "sas1-8396-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:4b24:10d:520c:0:51e5"; };
                                  { "sas2-4906-5d6-sas-turbo-service-b-752-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:7318:10d:520c:0:51e5"; };
                                  { "sas2-5942-sas-turbo-service-ba-752-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c14:42a5:10d:520c:0:51e5"; };
                                  { "sas2-6099-5c8-sas-turbo-servic-752-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c14:1e22:10d:520c:0:51e5"; };
                                  { "sas2-6199-sas-turbo-service-ba-752-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c14:440e:10d:520c:0:51e5"; };
                                  { "slovo101-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:6a8a:10d:520c:0:51e5"; };
                                  { "slovo110-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:6a88:10d:520c:0:51e5"; };
                                  { "slovo120-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:6b05:10d:520c:0:51e5"; };
                                  { "slovo145-sas-turbo-service-balancer-20965.gencfg-c.yandex.net"; 20965; 40.000; "2a02:6b8:c08:6a99:10d:520c:0:51e5"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "500ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 10;
                                  need_resolve = true;
                                }))
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- api_sas
                        api_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_news_sport_api_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "3000ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 2;
                              connection_attempts = 32;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "man1-0347-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:3b01:10d:520e:0:49b2"; };
                                  { "man1-0696-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:3672:10d:520e:0:49b2"; };
                                  { "man1-1672-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:17a0:10d:520e:0:49b2"; };
                                  { "man1-1699-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:2b9e:10d:520e:0:49b2"; };
                                  { "man1-1832-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1791:10d:520e:0:49b2"; };
                                  { "man1-2849-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:304:10d:520e:0:49b2"; };
                                  { "man1-2923-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:c:10d:520e:0:49b2"; };
                                  { "man1-3201-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:8c:10d:520e:0:49b2"; };
                                  { "man1-3446-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:38fd:10d:520e:0:49b2"; };
                                  { "man1-3681-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:fec:10d:520e:0:49b2"; };
                                  { "man1-3825-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1e97:10d:520e:0:49b2"; };
                                  { "man1-3950-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:2612:10d:520e:0:49b2"; };
                                  { "man1-4117-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:3815:10d:520e:0:49b2"; };
                                  { "man1-4852-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:126:10d:520e:0:49b2"; };
                                  { "man1-5224-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:497:10d:520e:0:49b2"; };
                                  { "man1-5253-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:9a3:10d:520e:0:49b2"; };
                                  { "man1-5337-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:725:10d:520e:0:49b2"; };
                                  { "man1-6020-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:13fc:10d:520e:0:49b2"; };
                                  { "man1-6176-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1098:10d:520e:0:49b2"; };
                                  { "man1-6392-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:86f:10d:520e:0:49b2"; };
                                  { "man1-6809-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:5a5:10d:520e:0:49b2"; };
                                  { "man1-6887-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:131d:10d:520e:0:49b2"; };
                                  { "man1-6937-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:101e:10d:520e:0:49b2"; };
                                  { "man1-7325-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:180c:10d:520e:0:49b2"; };
                                  { "man1-7368-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1c83:10d:520e:0:49b2"; };
                                  { "man1-7431-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1287:10d:520e:0:49b2"; };
                                  { "man1-7782-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1a9d:10d:520e:0:49b2"; };
                                  { "man1-8084-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:24a4:10d:520e:0:49b2"; };
                                  { "man1-8313-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c09:309:10d:520e:0:49b2"; };
                                  { "man1-8462-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:1506:10d:520e:0:49b2"; };
                                  { "man2-0445-man-turbo-service-balancer-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0b:4c43:10d:520e:0:49b2"; };
                                  { "man2-1054-9f7-man-turbo-service-b-148-18866.gencfg-c.yandex.net"; 18866; 40.000; "2a02:6b8:c0a:538:10d:520e:0:49b2"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "500ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 10;
                                  need_resolve = true;
                                }))
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- api_man
                        api_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_news_sport_api_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "3000ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 2;
                              connection_attempts = 32;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "vla1-0341-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4995:10d:520b:0:6cc3"; };
                                  { "vla1-0497-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:b93:10d:520b:0:6cc3"; };
                                  { "vla1-0606-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:1391:10d:520b:0:6cc3"; };
                                  { "vla1-0612-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:98e:10d:520b:0:6cc3"; };
                                  { "vla1-0617-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:2986:10d:520b:0:6cc3"; };
                                  { "vla1-0670-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:298b:10d:520b:0:6cc3"; };
                                  { "vla1-0842-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:240c:10d:520b:0:6cc3"; };
                                  { "vla1-0982-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:229e:10d:520b:0:6cc3"; };
                                  { "vla1-1242-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4d1d:10d:520b:0:6cc3"; };
                                  { "vla1-1364-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:260b:10d:520b:0:6cc3"; };
                                  { "vla1-1610-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:388b:10d:520b:0:6cc3"; };
                                  { "vla1-1692-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4408:10d:520b:0:6cc3"; };
                                  { "vla1-1767-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4411:10d:520b:0:6cc3"; };
                                  { "vla1-1965-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:2207:10d:520b:0:6cc3"; };
                                  { "vla1-1997-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:d91:10d:520b:0:6cc3"; };
                                  { "vla1-2032-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:221a:10d:520b:0:6cc3"; };
                                  { "vla1-2045-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:408e:10d:520b:0:6cc3"; };
                                  { "vla1-2382-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4b81:10d:520b:0:6cc3"; };
                                  { "vla1-2436-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4a90:10d:520b:0:6cc3"; };
                                  { "vla1-3120-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:89b:10d:520b:0:6cc3"; };
                                  { "vla1-3773-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:3f82:10d:520b:0:6cc3"; };
                                  { "vla1-3977-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4d90:10d:520b:0:6cc3"; };
                                  { "vla1-4365-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4d86:10d:520b:0:6cc3"; };
                                  { "vla1-4389-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4514:10d:520b:0:6cc3"; };
                                  { "vla1-4413-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:4506:10d:520b:0:6cc3"; };
                                  { "vla1-4676-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:1e23:10d:520b:0:6cc3"; };
                                  { "vla1-4693-vla-turbo-service-balancer-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0d:21a1:10d:520b:0:6cc3"; };
                                  { "vla1-6113-4fe-vla-turbo-service-b-b2a-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0f:2a1c:10d:520b:0:6cc3"; };
                                  { "vla2-0457-9b1-vla-turbo-service-b-b2a-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0f:4203:10d:520b:0:6cc3"; };
                                  { "vla2-0468-455-vla-turbo-service-b-b2a-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c0f:4200:10d:520b:0:6cc3"; };
                                  { "vla2-1012-vla-turbo-service-ba-b2a-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c15:1b99:10d:520b:0:6cc3"; };
                                  { "vla2-1071-vla-turbo-service-ba-b2a-27843.gencfg-c.yandex.net"; 27843; 40.000; "2a02:6b8:c15:1b9f:10d:520b:0:6cc3"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "500ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 10;
                                  need_resolve = true;
                                }))
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- api_vla
                      }; -- rr
                      on_error = {
                        errordocument = {
                          status = 500;
                          force_conn_close = false;
                        }; -- errordocument
                      }; -- on_error
                    }; -- balancer2
                  }; -- turbo
                  sport_aab_proxy = {
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
                      uuid = "6031073199377130062";
                      shared = {
                        uuid = "http_adapter_req";
                      }; -- shared
                    }; -- shared
                  }; -- sport_aab_proxy
                  sport_aab_http_check = {
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
                      uuid = "sport_main_cryprox_section";
                    }; -- shared
                  }; -- sport_aab_http_check
                  sport_cookie_bltsr_test = {
                    priority = 4;
                    match_and = {
                      {
                        match_or = {
                          {
                            match_fsm = {
                              URI = "/sport/_nzp.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "bltsr=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "qgZTpupNMGJBM=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "mcBaGDt=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "BgeeyNoBJuyII=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "orrXTfJaS=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "FgkKdCjPqoMFm=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "EIXtkCTlX=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "JPIqApiY=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "KIykI=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "HgGedof=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "ancQTZw=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "involved=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "instruction=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "engineering=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "telecommunications=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "discussion=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "computer=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "substantial=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "specific=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "engineer=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cookie = "adequate=1";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                        }; -- match_or
                      };
                      {
                        match_fsm = {
                          header = {
                            name = "X-Yandex-Internal-Request";
                            value = "1";
                          }; -- header
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "cryptox-test=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                    }; -- match_and
                    report = {
                      uuid = "aab-sport-test";
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
                          ["x-aab-partnertoken"] = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBQUIiLCJpYXQiOjE1NTM3NzU0MjcsInN1YiI6InlhbmRleF9zcG9ydCIsImV4cCI6MTU4NTMyMjIyN30.gYUqE3qcaJ_0RPOYdlZftQYUFDp-70ADqi8as_Scy71QpiCLTt7fV_pJzJpLxtTTQPtbiWkfZHV9oIbMm9ZqC3QbCkvbTPAMvTL9H8nCtOW4LxIkEKeFC9GOWuMPevVS87gllg843AtJNDc1TT06V22AROQky-jtfmBOvzOdN_KWICcwCIkFVeF7QbxNpcHsTcuFOmK1g_2Ejt-ggcxxssuovjO1eHAItmgisXyylanlAJq_EL01penufSIJvN5v1rrjM7doLHTnTtNkjBtedLyrkF_gCBGqrgcgVNj1BvIsqjdddM9oAKXwa_tP6sE_wpUytiRnlZ8IQ4jYo2l10g";
                        }; -- create
                        balancer2 = {
                          timeout_policy = {
                            timeout = "5000ms";
                            simple_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
                          rr = {
                            unpack(gen_proxy_backends({
                              { "cryprox-test.yandex.net"; 80; 1.000; "2a02:6b8::197"; };
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
                          on_error = {
                            shared = {
                              uuid = "6031073199377130062";
                            }; -- shared
                          }; -- on_error
                        }; -- balancer2
                      }; -- headers
                    }; -- report
                  }; -- sport_cookie_bltsr_test
                  sport_cookie_bltsr = {
                    priority = 3;
                    match_or = {
                      {
                        match_fsm = {
                          URI = "/sport/_nzp.*";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "bltsr=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "qgZTpupNMGJBM=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "mcBaGDt=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "BgeeyNoBJuyII=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "orrXTfJaS=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "FgkKdCjPqoMFm=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "EIXtkCTlX=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "JPIqApiY=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "KIykI=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "HgGedof=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "ancQTZw=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "involved=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "instruction=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "engineering=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "telecommunications=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "discussion=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "computer=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "substantial=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "specific=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "engineer=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          cookie = "adequate=1";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                      };
                    }; -- match_or
                    shared = {
                      uuid = "sport_main_cryprox_section";
                      regexp = {
                        default = {
                          priority = 1;
                          balancer2 = {
                            unique_policy = {};
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              aabsport_on = {
                                weight = 1.000;
                                report = {
                                  uuid = "aab-sport";
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
                                      ["x-aab-partnertoken"] = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBQUIiLCJpYXQiOjE1NTM3NzU0MjcsInN1YiI6InlhbmRleF9zcG9ydCIsImV4cCI6MTU4NTMyMjIyN30.gYUqE3qcaJ_0RPOYdlZftQYUFDp-70ADqi8as_Scy71QpiCLTt7fV_pJzJpLxtTTQPtbiWkfZHV9oIbMm9ZqC3QbCkvbTPAMvTL9H8nCtOW4LxIkEKeFC9GOWuMPevVS87gllg843AtJNDc1TT06V22AROQky-jtfmBOvzOdN_KWICcwCIkFVeF7QbxNpcHsTcuFOmK1g_2Ejt-ggcxxssuovjO1eHAItmgisXyylanlAJq_EL01penufSIJvN5v1rrjM7doLHTnTtNkjBtedLyrkF_gCBGqrgcgVNj1BvIsqjdddM9oAKXwa_tP6sE_wpUytiRnlZ8IQ4jYo2l10g";
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
                                      on_error = {
                                        regexp = {
                                          sport_aab_http_check = {
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
                                          }; -- sport_aab_http_check
                                          default = {
                                            priority = 1;
                                            shared = {
                                              uuid = "6031073199377130062";
                                            }; -- shared
                                          }; -- default
                                        }; -- regexp
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- headers
                                }; -- report
                              }; -- aabsport_on
                              aabsport_off = {
                                weight = -1.000;
                                shared = {
                                  uuid = "6031073199377130062";
                                }; -- shared
                              }; -- aabsport_off
                            }; -- rr
                          }; -- balancer2
                        }; -- default
                      }; -- regexp
                    }; -- shared
                  }; -- sport_cookie_bltsr
                  yandexsport = {
                    priority = 2;
                    match_or = {
                      {
                        match_fsm = {
                          host = "sportyandex\\..*";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          host = "(.+\\.)?yandexsport\\..*";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                    }; -- match_or
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
                            rewrite = "https://yandex.ru/sport/%{url}";
                          };
                        }; -- actions
                        errordocument = {
                          status = 302;
                          force_conn_close = false;
                          remain_headers = "Location";
                        }; -- errordocument
                      }; -- rewrite
                    }; -- headers
                  }; -- yandexsport
                  default = {
                    priority = 1;
                    report = {
                      uuid = "requests_yandexsport_all";
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      shared = {
                        uuid = "6031073199377130062";
                      }; -- shared
                    }; -- report
                  }; -- default
                }; -- regexp
              }; -- geobase
            }; -- ext_yandexsport
            ext_yandexsport_redirect = {
              priority = 5;
              match_and = {
                {
                  match_or = {
                    {
                      match_fsm = {
                        host = "(.+\\.)?yandexsport\\..*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        host = "sportyandex\\..*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_and = {
                        {
                          match_fsm = {
                            host = "(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_fsm = {
                            path = "/sport.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_and
                    };
                  }; -- match_or
                };
                {
                  match_not = {
                    match_fsm = {
                      header = {
                        name = "x-yandex-internal-request";
                        value = "1";
                      }; -- header
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                  }; -- match_not
                };
              }; -- match_and
              shared = {
                uuid = "4188253573388336186";
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
                        rewrite = "https://news.yandex.ru/sport.html";
                      };
                    }; -- actions
                    errordocument = {
                      status = 302;
                      force_conn_close = false;
                      remain_headers = "Location";
                    }; -- errordocument
                  }; -- rewrite
                }; -- headers
              }; -- shared
            }; -- ext_yandexsport_redirect
            ext_yandexmirror = {
              priority = 4;
              match_and = {
                {
                  match_fsm = {
                    host = "(www\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    path = "/mirror.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
              }; -- match_and
              report = {
                uuid = "requests_yandexmirror_all";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                shared = {
                  uuid = "6414857797694963063";
                  shared = {
                    uuid = "ext_default_module";
                  }; -- shared
                }; -- shared
              }; -- report
            }; -- ext_yandexmirror
            ext_turbo_proxy = {
              priority = 3;
              match_or = {
                {
                  match_and = {
                    {
                      match_fsm = {
                        host = "(pda|m)\\.news\\.yandex\\..*(:\\d+)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/turbo(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_and
                };
              }; -- match_or
              shared = {
                uuid = "5546963713335085101";
              }; -- shared
            }; -- ext_turbo_proxy
            ext_m_yandex_rewrite = {
              priority = 2;
              match_fsm = {
                host = "m\\.yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              rewrite = {
                actions = {
                  {
                    global = false;
                    literal = false;
                    header_name = "Host";
                    rewrite = "yandex.%1";
                    case_insensitive = false;
                    regexp = "m\\.yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                  };
                }; -- actions
                shared = {
                  uuid = "6414857797694963063";
                }; -- shared
              }; -- rewrite
            }; -- ext_m_yandex_rewrite
            default = {
              priority = 1;
              shared = {
                uuid = "ext_default_module";
                pinger = {
                  lo = 0.500;
                  hi = 0.700;
                  delay = "1s";
                  histtime = "4s";
                  ping_request_data = "GET /robots.txt HTTP/1.1\r\nHost: news.yandex.ru\r\n\r\n";
                  admin_request_uri = "/robots.txt";
                  admin_ips = "5.45.193.144/29,5.45.208.88/30,5.45.228.160/30,5.45.229.168/30,5.45.232.160/30,5.45.240.168/30,5.45.243.16/30,5.45.247.144/29,5.45.247.176/29,5.255.192.64/30,5.255.194.12/30,5.255.195.144/30,5.255.196.168/30,5.255.197.168/30,5.255.200.160/30,5.255.252.160/30,37.9.73.160/30,37.140.136.88/29,37.140.137.104/29,77.88.35.168/30,77.88.54.168/30,84.201.159.176/28,87.250.226.0/25,87.250.226.128/25,87.250.228.0/24,87.250.234.0/24,95.108.180.40/29,95.108.237.0/25,95.108.237.128/25,100.43.92.144/28,141.8.136.128/25,141.8.154.200/29,141.8.155.104/29,185.32.186.8/29,213.180.202.160/30,213.180.223.16/30,2001:678:384:100::/64,2620:10f:d000:100::/64,2a02:6b8:0:300::/64,2a02:6b8:0:400::/64,2a02:6b8:0:800::/64,2a02:6b8:0:900::/64,2a02:6b8:0:d00::/64,2a02:6b8:0:e00::/64,2a02:6b8:0:1000::/64,2a02:6b8:0:1100::/64,2a02:6b8:0:1200::/64,2a02:6b8:0:1300::/64,2a02:6b8:0:1400::/64,2a02:6b8:0:1500::/64,2a02:6b8:0:1600::/64,2a02:6b8:0:1700::/64,2a02:6b8:0:1800::/64,2a02:6b8:0:1900::/64,2a02:6b8:0:1a00::/64,2a02:6b8:0:1b00::/64,2a02:6b8:0:1d00::/64,2a02:6b8:0:1e00::/64,2a02:6b8:0:1f00::/64,2a02:6b8:0:2000::/64,2a02:6b8:0:2200::/64,2a02:6b8:0:2c00::/64,2a02:6b8:0:3000::/64,2a02:6b8:0:3100::/64,2a02:6b8:0:3401::/64,2a02:6b8:0:3c00::/64,2a02:6b8:0:3d00::/64,2a02:6b8:0:3e00::/64,2a02:6b8:0:3f00::/64,2a02:6b8:0:4000::/64,2a02:6b8:0:4200::/64,2a02:6b8:0:4700::/64,2a02:6b8:b010:b000::/64";
                  enable_tcp_check_file = "./controls/tcp_check_on";
                  switch_off_file = "./controls/slb_check.weights";
                  switch_off_key = "switch_off";
                  admin_error_replier = {
                    errordocument = {
                      status = 503;
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- admin_error_replier
                  module = {
                    report = {
                      uuid = "antirobot_result";
                      ranges = get_str_var("default_ranges");
                      matcher_map = {
                        internal_net = {
                          match_fsm = {
                            header = {
                              name = "X-Yandex-Internal-Request";
                              value = "1";
                            }; -- header
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        }; -- internal_net
                        external_net = {
                          match_not = {
                            match_fsm = {
                              header = {
                                name = "X-Yandex-Internal-Request";
                                value = "1";
                              }; -- header
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          }; -- match_not
                        }; -- external_net
                      }; -- matcher_map
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      regexp = {
                        bad_host = {
                          priority = 7;
                          match_or = {
                            {
                              match_fsm = {
                                header = {
                                  name = "Host";
                                  value = ".*news.yandex.com.ua";
                                }; -- header
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_not = {
                                match_or = {
                                  {
                                    match_fsm = {
                                      header = {
                                        name = "Host";
                                        value = "(www\\.)?([^.]+-d-[^.]+\\.)?((dev-)?nind([^.]*)\\.)?((pda|m)\\.)?(cryprox-test\\.)?((test)\\.)?news\\.(.+\\.)?yandex\\.\\w+(\\.\\w+)?(:[0-9]+)?";
                                      }; -- header
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_and = {
                                      {
                                        match_fsm = {
                                          host = "(www\\.)?yandex\\.\\w+(:[0-9]+)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_or = {
                                          {
                                            match_fsm = {
                                              URI = "/news.*";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              URI = "/mirror.*";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                        }; -- match_or
                                      };
                                    }; -- match_and
                                  };
                                }; -- match_or
                              }; -- match_not
                            };
                          }; -- match_or
                          errordocument = {
                            status = 400;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- bad_host
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
                            uuid = "6268837079173193556";
                            shared = {
                              uuid = "int_upstreams";
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
                            uuid = "main_cryprox_section";
                          }; -- shared
                        }; -- aab_http_check
                        http_post = {
                          priority = 4;
                          match_fsm = {
                            match = "POST.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "6268837079173193556";
                          }; -- shared
                        }; -- http_post
                        testing_bltsr = {
                          priority = 3;
                          match_and = {
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    host = "(www\\.)?([^.]+-d-[^.]+\\.)?((dev-)?nind([^.]*)\\.)?((pda|m)\\.)?(cryprox-test\\.)?((test)\\.)?news\\.(.+\\.)?yandex\\.\\w+(\\.\\w+)?(:[0-9]+)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_and = {
                                    {
                                      match_fsm = {
                                        host = "(www\\.)?yandex\\.\\w+(:[0-9]+)?";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/news.*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                  }; -- match_and
                                };
                              }; -- match_or
                            };
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    URI = "/_nzp.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/news/_nzp.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "bltsr=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "qgZTpupNMGJBM=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "mcBaGDt=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "BgeeyNoBJuyII=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "orrXTfJaS=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "FgkKdCjPqoMFm=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "EIXtkCTlX=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "JPIqApiY=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "KIykI=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "HgGedof=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "ancQTZw=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "involved=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "instruction=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "engineering=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "telecommunications=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "discussion=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "computer=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "substantial=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "specific=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "engineer=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "adequate=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                            {
                              match_fsm = {
                                header = {
                                  name = "X-Yandex-Internal-Request";
                                  value = "1";
                                }; -- header
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    header = {
                                      name = "Host";
                                      value = "(m\\.)?cryprox-test\\.news\\.yandex\\.ru";
                                    }; -- header
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "cryptox-test=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                          shared = {
                            uuid = "testing_cryprox_section";
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                aab_on = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "aab-test";
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
                                        ["x-aab-partnertoken"] = get_str_env_var("AWACS_AAB_TOKEN");
                                      }; -- create
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "5000ms";
                                          simple_policy = {};
                                        }; -- timeout_policy
                                        attempts = 1;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "cryprox-test.yandex.net"; 80; 1.000; "2a02:6b8::197"; };
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
                                        on_error = {
                                          shared = {
                                            uuid = "6268837079173193556";
                                          }; -- shared
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- headers
                                  }; -- report
                                }; -- aab_on
                              }; -- rr
                            }; -- balancer2
                          }; -- shared
                        }; -- testing_bltsr
                        cookie_bltsr = {
                          priority = 2;
                          match_and = {
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    host = "(www\\.)?([^.]+-d-[^.]+\\.)?((dev-)?nind([^.]*)\\.)?((pda|m)\\.)?(cryprox-test\\.)?((test)\\.)?news\\.(.+\\.)?yandex\\.\\w+(\\.\\w+)?(:[0-9]+)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_and = {
                                    {
                                      match_fsm = {
                                        host = "(www\\.)?yandex\\.\\w+(:[0-9]+)?";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/news.*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                  }; -- match_and
                                };
                              }; -- match_or
                            };
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    URI = "/news/_nzp.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/_nzp.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "bltsr=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "qgZTpupNMGJBM=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "mcBaGDt=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "BgeeyNoBJuyII=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "orrXTfJaS=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "FgkKdCjPqoMFm=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "EIXtkCTlX=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "JPIqApiY=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "KIykI=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "HgGedof=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "ancQTZw=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "involved=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "instruction=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "engineering=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "telecommunications=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "discussion=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "computer=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "substantial=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "specific=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "engineer=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "adequate=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                          shared = {
                            uuid = "main_cryprox_section";
                            regexp = {
                              turbo = {
                                priority = 2;
                                match_fsm = {
                                  URI = "/turbo";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "6268837079173193556";
                                }; -- shared
                              }; -- turbo
                              default = {
                                priority = 1;
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
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
                                            ["x-aab-partnertoken"] = get_str_env_var("AWACS_AAB_TOKEN");
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
                                                    uuid = "6268837079173193556";
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
                                        uuid = "6268837079173193556";
                                      }; -- shared
                                    }; -- aab_off
                                  }; -- rr
                                }; -- balancer2
                              }; -- default
                            }; -- regexp
                          }; -- shared
                        }; -- cookie_bltsr
                        default = {
                          priority = 1;
                          shared = {
                            uuid = "6268837079173193556";
                          }; -- shared
                        }; -- default
                      }; -- regexp
                    }; -- report
                  }; -- module
                }; -- pinger
              }; -- shared
            }; -- default
          }; -- regexp
        }; -- shared
      }; -- http
    }; -- fake_section_ext
    fake_section = {
      ips = {
        "127.0.0.44";
      }; -- ips
      ports = {
        15090;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        shared = {
          uuid = "int_upstreams";
          regexp = {
            int_clck = {
              priority = 6;
              match_fsm = {
                URI = "/clck/(.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              shared = {
                uuid = "clck";
              }; -- shared
            }; -- int_clck
            ["int_export-tops"] = {
              priority = 5;
              match_fsm = {
                URI = "/export/tops.*";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "export-tops";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                request_replier = {
                  sink = {
                    shared = {
                      uuid = "2869946922261744982";
                      balancer2 = {
                        retry_policy = {
                          unique_policy = {};
                        }; -- retry_policy
                        attempts = 3;
                        connection_attempts = 3;
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
                            { "news.sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:2"; };
                          }, {
                            resolve_timeout = "10ms";
                            connect_timeout = "100ms";
                            backend_timeout = "10s";
                            fail_on_5xx = true;
                            http_backend = true;
                            buffering = false;
                            keepalive_count = 0;
                            need_resolve = true;
                          }))
                        }; -- weighted2
                        attempts_rate_limiter = {
                          limit = 0.200;
                          coeff = 0.990;
                          switch_default = true;
                        }; -- attempts_rate_limiter
                      }; -- balancer2
                    }; -- shared
                  }; -- sink
                  enable_failed_requests_replication = false;
                  rate = 0.000;
                  rate_file = "./controls/request_repl.ratefile";
                  shared = {
                    uuid = "nginx_backends";
                    balancer2 = {
                      watermark_policy = {
                        lo = 0.050;
                        hi = 0.100;
                        params_file = "./controls/watermark_policy.params_file";
                        unique_policy = {};
                      }; -- watermark_policy
                      attempts = 2;
                      attempts_file = "./controls/export-tops.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control.weights";
                        news_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_export-tops_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "500ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 8;
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
                                  { "hmmlb66wox4dfvsf.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:215:0:46a9:7dd8:0"; };
                                  { "ovgjz7jpuspx5leo.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:5c9a:100:0:e5cf:0"; };
                                  { "pazzdcbkj534gaba.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2794:100:0:c93f:0"; };
                                  { "tgvk73qldrcthhm7.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:6d1a:100:0:c84b:0"; };
                                  { "tsz5cwigxj666f6g.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:7186:100:0:60fb:0"; };
                                  { "v62ffdqkctdkldos.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:b526:100:0:f53c:0"; };
                                  { "x3gkdbfvxaafbxqv.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2719:100:0:ab1c:0"; };
                                  { "ydgt4i4mmp44ce4r.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:6397:100:0:3eb2:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "10s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- report
                        }; -- news_sas
                        news_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_export-tops_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "500ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 8;
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
                                  { "cgmcb7ueqhi7onfo.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:27a4:0:46a9:540a:0"; };
                                  { "ek33hlcm34gjuick.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a40:100:0:609e:0"; };
                                  { "fps7sutokvp44fte.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2c8e:100:0:1f90:0"; };
                                  { "klvpvoq6xggz2ypy.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:279e:100:0:8d9:0"; };
                                  { "w76nwauh3jdrbhok.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:1200:100:0:324f:0"; };
                                  { "xmzhlbwyfcfdz4dj.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38a1:100:0:896d:0"; };
                                  { "xqn3vm2omad7x5c4.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5f10:100:0:4d98:0"; };
                                  { "ze7i5wdtp5o7tbiq.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a5e:0:46a9:2bd0:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "10s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- report
                        }; -- news_man
                        news_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_export-tops_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "500ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 8;
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
                                  { "bdilyfewpmutnvti.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c1f:100:0:7bee:0"; };
                                  { "iz737fa4hbzlngim.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a99:100:0:79f0:0"; };
                                  { "ky3ran4o23ztkbuj.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2f93:100:0:95a6:0"; };
                                  { "s25q6zyfplgo2o7l.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:319c:100:0:e031:0"; };
                                  { "skyla6elcu7vprqo.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c05:100:0:9f6d:0"; };
                                  { "tohikjwig7fe5ek2.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:388c:100:0:b546:0"; };
                                  { "tok4vzhm6mnou2o6.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:381a:100:0:b18d:0"; };
                                  { "zwtizpg4xaozk76t.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a8c:100:0:643b:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "10s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- report
                        }; -- news_vla
                        news_devnull = {
                          weight = -1.000;
                          report = {
                            uuid = "requests_export-tops_to_devnull";
                            ranges = "1ms";
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            errordocument = {
                              status = 204;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- report
                        }; -- news_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- shared
                }; -- request_replier
              }; -- report
            }; -- ["int_export-tops"]
            int_api_proxy = {
              priority = 4;
              match_fsm = {
                URI = "/api/.*";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "api";
                ranges = get_str_var("default_ranges");
                matcher_map = {
                  user_coldness = {
                    match_fsm = {
                      URI = ".*user_coldness.*";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                  }; -- user_coldness
                }; -- matcher_map
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
                    api_on = {
                      weight = 1.000;
                      request_replier = {
                        sink = {
                          balancer2 = {
                            retry_policy = {
                              unique_policy = {};
                            }; -- retry_policy
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
                                { "news.sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:2"; };
                              }, {
                                resolve_timeout = "10ms";
                                connect_timeout = "100ms";
                                backend_timeout = "10s";
                                fail_on_5xx = true;
                                http_backend = true;
                                buffering = false;
                                keepalive_count = 0;
                                need_resolve = true;
                              }))
                            }; -- weighted2
                            attempts_rate_limiter = {
                              limit = 0.200;
                              coeff = 0.990;
                              switch_default = true;
                            }; -- attempts_rate_limiter
                          }; -- balancer2
                        }; -- sink
                        enable_failed_requests_replication = false;
                        rate = 0.000;
                        rate_file = "./controls/request_repl.ratefile";
                        balancer2 = {
                          watermark_policy = {
                            lo = 0.050;
                            hi = 0.100;
                            params_file = "./controls/watermark_policy.params_file";
                            unique_policy = {};
                          }; -- watermark_policy
                          attempts = 2;
                          attempts_file = "./controls/api.attempts";
                          rr = {
                            weights_file = "./controls/traffic_control.weights";
                            news_sas = {
                              weight = 1.000;
                              report = {
                                uuid = "requests_api_to_sas";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "500ms";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 8;
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
                                      { "hmmlb66wox4dfvsf.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:215:0:46a9:7dd8:0"; };
                                      { "ovgjz7jpuspx5leo.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:5c9a:100:0:e5cf:0"; };
                                      { "pazzdcbkj534gaba.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2794:100:0:c93f:0"; };
                                      { "tgvk73qldrcthhm7.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:6d1a:100:0:c84b:0"; };
                                      { "tsz5cwigxj666f6g.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:7186:100:0:60fb:0"; };
                                      { "v62ffdqkctdkldos.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:b526:100:0:f53c:0"; };
                                      { "x3gkdbfvxaafbxqv.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2719:100:0:ab1c:0"; };
                                      { "ydgt4i4mmp44ce4r.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:6397:100:0:3eb2:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "50ms";
                                      backend_timeout = "2500ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                      status_code_blacklist = {
                                        "204";
                                      }; -- status_code_blacklist
                                    }))
                                  }; -- weighted2
                                  attempts_rate_limiter = {
                                    limit = 0.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- report
                            }; -- news_sas
                            news_man = {
                              weight = 1.000;
                              report = {
                                uuid = "requests_api_to_man";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "500ms";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 8;
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
                                      { "cgmcb7ueqhi7onfo.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:27a4:0:46a9:540a:0"; };
                                      { "ek33hlcm34gjuick.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a40:100:0:609e:0"; };
                                      { "fps7sutokvp44fte.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2c8e:100:0:1f90:0"; };
                                      { "klvpvoq6xggz2ypy.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:279e:100:0:8d9:0"; };
                                      { "w76nwauh3jdrbhok.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:1200:100:0:324f:0"; };
                                      { "xmzhlbwyfcfdz4dj.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38a1:100:0:896d:0"; };
                                      { "xqn3vm2omad7x5c4.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5f10:100:0:4d98:0"; };
                                      { "ze7i5wdtp5o7tbiq.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a5e:0:46a9:2bd0:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "50ms";
                                      backend_timeout = "2500ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                      status_code_blacklist = {
                                        "204";
                                      }; -- status_code_blacklist
                                    }))
                                  }; -- weighted2
                                  attempts_rate_limiter = {
                                    limit = 0.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- report
                            }; -- news_man
                            news_vla = {
                              weight = 1.000;
                              report = {
                                uuid = "requests_api_to_vla";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "500ms";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 8;
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
                                      { "bdilyfewpmutnvti.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c1f:100:0:7bee:0"; };
                                      { "iz737fa4hbzlngim.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a99:100:0:79f0:0"; };
                                      { "ky3ran4o23ztkbuj.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2f93:100:0:95a6:0"; };
                                      { "s25q6zyfplgo2o7l.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:319c:100:0:e031:0"; };
                                      { "skyla6elcu7vprqo.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c05:100:0:9f6d:0"; };
                                      { "tohikjwig7fe5ek2.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:388c:100:0:b546:0"; };
                                      { "tok4vzhm6mnou2o6.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:381a:100:0:b18d:0"; };
                                      { "zwtizpg4xaozk76t.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a8c:100:0:643b:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "50ms";
                                      backend_timeout = "2500ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                      status_code_blacklist = {
                                        "204";
                                      }; -- status_code_blacklist
                                    }))
                                  }; -- weighted2
                                  attempts_rate_limiter = {
                                    limit = 0.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- report
                            }; -- news_vla
                            news_devnull = {
                              weight = -1.000;
                              report = {
                                uuid = "requests_api_to_devnull";
                                ranges = "1ms";
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                errordocument = {
                                  status = 204;
                                  force_conn_close = false;
                                }; -- errordocument
                              }; -- report
                            }; -- news_devnull
                          }; -- rr
                        }; -- balancer2
                      }; -- request_replier
                    }; -- api_on
                    api_off = {
                      weight = -1.000;
                      errordocument = {
                        status = 503;
                        force_conn_close = false;
                      }; -- errordocument
                    }; -- api_off
                  }; -- rr
                  on_error = {
                    errordocument = {
                      status = 500;
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- on_error
                }; -- balancer2
              }; -- report
            }; -- int_api_proxy
            int_nginx_proxy = {
              priority = 3;
              match_or = {
                {
                  match_and = {
                    {
                      match_fsm = {
                        host = "(pda|m)\\.news\\.yandex\\..*(:\\d+)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/turbo(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_and
                };
                {
                  match_fsm = {
                    URI = "(.*\\.js|.*\\.rss|/rss/.*|/api/.*|/live(/.*)?|/cl2picture.*|/crossdomain.xml.*|/export/.*|/favicon\\.ico|/lasttimeout|/opensearch\\.xml.*|/robots\\.txt|/apple-touch-icon(|-precomposed)\\.png)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    URI = "/google419fbd824d7ff97d.html";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    URI = "/quotes/([\\d]+/)?graph_[\\d]+.json";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_fsm = {
                    URI = "/news/quotes/([\\d]+/)?graph_[\\d]+.json";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                };
                {
                  match_and = {
                    {
                      match_fsm = {
                        host = "(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/sport/live/?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_and
                };
              }; -- match_or
              shared = {
                uuid = "nginx_proxy";
                report = {
                  uuid = "nginx";
                  ranges = get_str_var("default_ranges");
                  just_storage = false;
                  disable_robotness = true;
                  disable_sslness = true;
                  events = {
                    stats = "report";
                  }; -- events
                  request_replier = {
                    sink = {
                      shared = {
                        uuid = "2869946922261744982";
                      }; -- shared
                    }; -- sink
                    enable_failed_requests_replication = false;
                    rate = 0.000;
                    rate_file = "./controls/request_repl.ratefile";
                    balancer2 = {
                      watermark_policy = {
                        lo = 0.050;
                        hi = 0.100;
                        params_file = "./controls/watermark_policy.params_file";
                        unique_policy = {};
                      }; -- watermark_policy
                      attempts = 2;
                      attempts_file = "./controls/nginx.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control.weights";
                        news_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_nginx_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "500ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 8;
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
                                  { "hmmlb66wox4dfvsf.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:215:0:46a9:7dd8:0"; };
                                  { "ovgjz7jpuspx5leo.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:5c9a:100:0:e5cf:0"; };
                                  { "pazzdcbkj534gaba.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2794:100:0:c93f:0"; };
                                  { "tgvk73qldrcthhm7.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:6d1a:100:0:c84b:0"; };
                                  { "tsz5cwigxj666f6g.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:7186:100:0:60fb:0"; };
                                  { "v62ffdqkctdkldos.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:b526:100:0:f53c:0"; };
                                  { "x3gkdbfvxaafbxqv.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2719:100:0:ab1c:0"; };
                                  { "ydgt4i4mmp44ce4r.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:6397:100:0:3eb2:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "2500ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- report
                        }; -- news_sas
                        news_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_nginx_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "500ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 8;
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
                                  { "cgmcb7ueqhi7onfo.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:27a4:0:46a9:540a:0"; };
                                  { "ek33hlcm34gjuick.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a40:100:0:609e:0"; };
                                  { "fps7sutokvp44fte.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:2c8e:100:0:1f90:0"; };
                                  { "klvpvoq6xggz2ypy.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:279e:100:0:8d9:0"; };
                                  { "w76nwauh3jdrbhok.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c09:1200:100:0:324f:0"; };
                                  { "xmzhlbwyfcfdz4dj.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38a1:100:0:896d:0"; };
                                  { "xqn3vm2omad7x5c4.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5f10:100:0:4d98:0"; };
                                  { "ze7i5wdtp5o7tbiq.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a5e:0:46a9:2bd0:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "2500ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- report
                        }; -- news_man
                        news_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_nginx_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              timeout_policy = {
                                timeout = "500ms";
                                unique_policy = {};
                              }; -- timeout_policy
                              attempts = 8;
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
                                  { "bdilyfewpmutnvti.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c1f:100:0:7bee:0"; };
                                  { "iz737fa4hbzlngim.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a99:100:0:79f0:0"; };
                                  { "ky3ran4o23ztkbuj.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2f93:100:0:95a6:0"; };
                                  { "s25q6zyfplgo2o7l.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:319c:100:0:e031:0"; };
                                  { "skyla6elcu7vprqo.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c05:100:0:9f6d:0"; };
                                  { "tohikjwig7fe5ek2.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:388c:100:0:b546:0"; };
                                  { "tok4vzhm6mnou2o6.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:381a:100:0:b18d:0"; };
                                  { "zwtizpg4xaozk76t.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a8c:100:0:643b:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "50ms";
                                  backend_timeout = "2500ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- report
                        }; -- news_vla
                        news_devnull = {
                          weight = -1.000;
                          report = {
                            uuid = "requests_nginx_to_devnull";
                            ranges = "1ms";
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            errordocument = {
                              status = 204;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- report
                        }; -- news_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- request_replier
                }; -- report
              }; -- shared
            }; -- int_nginx_proxy
            int_tkva_fetch = {
              priority = 2;
              match_fsm = {
                header = {
                  name = "X-Yandex-Tkva";
                  value = "1";
                }; -- header
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "news-tkva-req";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                headers = {
                  create = {
                    ["X-LaaS-Answered"] = "1";
                    ["X-Region-By-IP"] = "213";
                    ["X-Region-City-Id"] = "213";
                    ["X-Region-Id"] = "213";
                    ["X-Region-Is-User-Choice"] = "0";
                    ["X-Region-Isp-Code-By-Ip"] = "0";
                    ["X-Region-Location"] = "55.753960, 37.620393, 15000, 1514220473";
                    ["X-Region-Precision"] = "2";
                    ["X-Region-Should-Update-Cookie"] = "0";
                    ["X-Region-Suspected"] = "213";
                    ["X-Region-Suspected-City"] = "213";
                    ["X-Region-Suspected-Location"] = "55.753960, 37.620393, 15000, 1514220473";
                    ["X-Region-Suspected-Precision"] = "2";
                    ["X-YNews-Use-Report-Renderer"] = "0";
                    ["X-Yandex-Internal-Request"] = "0";
                  }; -- create
                  shared = {
                    uuid = "tkva-http-adapter-req";
                    balancer2 = {
                      unique_policy = {};
                      attempts = 1;
                      rr = {
                        default = {
                          weight = 1.000;
                          shared = {
                            uuid = "tkva_report_balancer";
                            balancer2 = {
                              watermark_policy = {
                                lo = 0.500;
                                hi = 0.600;
                                params_file = "./controls/watermark_policy.params_file";
                                unique_policy = {};
                              }; -- watermark_policy
                              attempts = 2;
                              attempts_file = "./controls/news.attempts";
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                news_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_news_tkva_to_sas";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "500ms";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 20;
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
                                          { "sas1-0399.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:14d:225:90ff:fe88:b79c"; };
                                          { "sas1-0441.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:158:225:90ff:fe83:324"; };
                                          { "sas1-0444.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:158:225:90ff:fe82:ffa8"; };
                                          { "sas1-0732.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:157:225:90ff:fe83:238"; };
                                          { "sas1-1001.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:649:225:90ff:fe93:7dc4"; };
                                          { "sas1-1112.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:61b:922b:34ff:fecf:3a56"; };
                                          { "sas1-2295.search.yandex.net"; 32251; 113.000; "2a02:6b8:b000:627:922b:34ff:fecf:4018"; };
                                          { "sas1-2486.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:62e:922b:34ff:fecf:30a2"; };
                                          { "sas1-2956.search.yandex.net"; 32251; 174.000; "2a02:6b8:b000:108:225:90ff:fe83:2dbc"; };
                                          { "sas1-4498.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:643:96de:80ff:fe81:16f8"; };
                                          { "sas1-4786.search.yandex.net"; 32251; 200.000; "2a02:6b8:b000:65c:96de:80ff:fe81:9a0"; };
                                          { "sas1-5588.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:163:225:90ff:fe95:804c"; };
                                          { "sas1-5593.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:622:225:90ff:fe9b:150e"; };
                                          { "sas1-7688.search.yandex.net"; 32251; 200.000; "2a02:6b8:b000:61d:215:b2ff:fea7:befc"; };
                                          { "sas1-7781.search.yandex.net"; 32251; 130.000; "2a02:6b8:b000:192:922b:34ff:fecf:3f54"; };
                                          { "sas2-0647.search.yandex.net"; 32251; 214.000; "2a02:6b8:c02:407:0:604:dde:f616"; };
                                          { "sas2-3447.search.yandex.net"; 32251; 200.000; "2a02:6b8:b000:6c4:428d:5cff:fe34:fe7f"; };
                                          { "slovo048.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:64d:922b:34ff:fecf:413e"; };
                                          { "slovo098.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:64d:922b:34ff:fecd:1a90"; };
                                          { "slovo108.search.yandex.net"; 32251; 213.000; "2a02:6b8:b000:64d:922b:34ff:fecf:33f4"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "50ms";
                                          backend_timeout = "4000ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "402";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                      attempts_rate_limiter = {
                                        limit = 0.200;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                    }; -- balancer2
                                  }; -- report
                                }; -- news_sas
                                news_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_news_tkva_to_man";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "500ms";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 20;
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
                                          { "man1-0007.search.yandex.net"; 20625; 20.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:b9e4"; };
                                          { "man1-1207.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:6012:f652:14ff:fe48:96c0"; };
                                          { "man1-1618.search.yandex.net"; 20625; 101.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b5e0"; };
                                          { "man1-1930.search.yandex.net"; 20625; 190.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bdc0"; };
                                          { "man1-2849.search.yandex.net"; 20625; 249.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e820"; };
                                          { "man1-4256.search.yandex.net"; 20625; 158.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:bc2c"; };
                                          { "man1-4645.search.yandex.net"; 20625; 249.000; "2a02:6b8:b000:6502:215:b2ff:fea9:653e"; };
                                          { "man1-5488.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:6509:215:b2ff:fea9:647e"; };
                                          { "man1-5910.search.yandex.net"; 20625; 248.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:83f0"; };
                                          { "man1-6449.search.yandex.net"; 20625; 249.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:1f00"; };
                                          { "man1-6937.search.yandex.net"; 20625; 211.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3ee0"; };
                                          { "man1-7324.search.yandex.net"; 20625; 249.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e460"; };
                                          { "man1-7928.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:6084:e61d:2dff:fe6c:ce30"; };
                                          { "man1-8434.search.yandex.net"; 20625; 215.000; "2a02:6b8:b000:6081:e61d:2dff:fe6c:ec00"; };
                                          { "man1-8876.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:6064:e61d:2dff:fe00:9190"; };
                                          { "man1-8961.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:651e:e61d:2dff:fe6d:d360"; };
                                          { "man1-9680.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72da"; };
                                          { "man2-0279.search.yandex.net"; 20625; 248.000; "2a02:6b8:b000:6087:feaa:14ff:feea:8ea1"; };
                                          { "man2-0332.search.yandex.net"; 20625; 200.000; "2a02:6b8:b000:6093:92e2:baff:fea1:8a96"; };
                                          { "man2-0431.search.yandex.net"; 20625; 213.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e41"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "50ms";
                                          backend_timeout = "4000ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "402";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                      attempts_rate_limiter = {
                                        limit = 0.200;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                    }; -- balancer2
                                  }; -- report
                                }; -- news_man
                                news_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_news_tkva_to_vla";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "500ms";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 20;
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
                                          { "vla1-0040.search.yandex.net"; 20045; 41.000; "2a02:6b8:c0e:4f:0:604:5cf4:8eff"; };
                                          { "vla1-0116.search.yandex.net"; 20045; 115.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                                          { "vla1-0285.search.yandex.net"; 20045; 4.000; "2a02:6b8:c0e:98:0:604:db7:a894"; };
                                          { "vla1-0518.search.yandex.net"; 20045; 568.000; "2a02:6b8:c0e:34:0:604:db7:9d03"; };
                                          { "vla1-0555.search.yandex.net"; 20045; 130.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                          { "vla1-0836.search.yandex.net"; 20045; 21.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                          { "vla1-2032.search.yandex.net"; 20045; 25.000; "2a02:6b8:c0e:44:0:604:db7:a0ab"; };
                                          { "vla1-2390.search.yandex.net"; 20045; 91.000; "2a02:6b8:c0e:79:0:604:db7:a957"; };
                                          { "vla1-3143.search.yandex.net"; 20045; 42.000; "2a02:6b8:c0e:32:0:604:db7:a4ad"; };
                                          { "vla1-3839.search.yandex.net"; 20045; 20.000; "2a02:6b8:c0e:63:0:604:db7:a2a1"; };
                                          { "vla1-4094.search.yandex.net"; 20045; 88.000; "2a02:6b8:c0e:8a:0:604:db7:a805"; };
                                          { "vla1-4278.search.yandex.net"; 20045; 57.000; "2a02:6b8:c0e:65:0:604:db7:a713"; };
                                          { "vla1-5740.search.yandex.net"; 20045; 200.000; "2a02:6b8:c0e:12c:0:604:5e18:d8d"; };
                                          { "vla1-5976.search.yandex.net"; 20045; 98.000; "2a02:6b8:c0e:12b:0:604:5e18:d70"; };
                                          { "vla1-5991.search.yandex.net"; 20045; 568.000; "2a02:6b8:c0e:73:0:604:db7:a123"; };
                                          { "vla1-5992.search.yandex.net"; 20045; 568.000; "2a02:6b8:c0e:9c:0:604:db7:a829"; };
                                          { "vla1-5995.search.yandex.net"; 20045; 568.000; "2a02:6b8:c0e:57:0:604:db7:a121"; };
                                          { "vla1-5996.search.yandex.net"; 20045; 569.000; "2a02:6b8:c0e:57:0:604:db7:a023"; };
                                          { "vla2-1041.search.yandex.net"; 20045; 115.000; "2a02:6b8:c0e:337:0:604:4b14:5bbe"; };
                                          { "vla2-1043.search.yandex.net"; 20045; 112.000; "2a02:6b8:c0e:337:0:604:4b14:595a"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "50ms";
                                          backend_timeout = "4000ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "402";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                      attempts_rate_limiter = {
                                        limit = 0.200;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                    }; -- balancer2
                                  }; -- report
                                }; -- news_vla
                              }; -- rr
                            }; -- balancer2
                          }; -- shared
                        }; -- default
                      }; -- rr
                      on_error = {
                        shared = {
                          uuid = "normal-report-req";
                          regexp = {
                            report_req = {
                              priority = 2;
                              match_fsm = {
                                header = {
                                  name = "X-YNews-Use-Report";
                                  value = "1";
                                }; -- header
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              report = {
                                uuid = "news-report-apache-req";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                shared = {
                                  uuid = "report-backend-req";
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      default = {
                                        weight = 1.000;
                                        shared = {
                                          uuid = "report_balancer";
                                          balancer2 = {
                                            watermark_policy = {
                                              lo = 0.500;
                                              hi = 0.600;
                                              params_file = "./controls/watermark_policy.params_file";
                                              unique_policy = {};
                                            }; -- watermark_policy
                                            attempts = 3;
                                            attempts_file = "./controls/news.attempts";
                                            connection_attempts = 3;
                                            rr = {
                                              weights_file = "./controls/traffic_control.weights";
                                              news_sas = {
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
                                                    timeout_policy = {
                                                      timeout = "6200ms";
                                                      unique_policy = {};
                                                    }; -- timeout_policy
                                                    attempts = 22;
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
                                                        { "sas1-5794.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:100:225:90ff:feed:2b02"; };
                                                        { "sas1-6881.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:62c:215:b2ff:fea7:8d84"; };
                                                        { "sas1-6940.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b170"; };
                                                        { "sas1-7016.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ac8c"; };
                                                        { "sas1-7165.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:12c:215:b2ff:fea7:a8f0"; };
                                                        { "sas1-7181.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:12c:215:b2ff:fea7:7390"; };
                                                        { "sas1-7211.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:122:215:b2ff:fea7:8f28"; };
                                                        { "sas1-7491.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:ab50"; };
                                                        { "sas1-7592.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:610:215:b2ff:fea7:baa8"; };
                                                        { "sas1-7731.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:127:215:b2ff:fea7:b034"; };
                                                        { "sas1-7828.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8ce4"; };
                                                        { "sas1-8051.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:618:215:b2ff:fea7:8f70"; };
                                                        { "sas1-8175.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:132:215:b2ff:fea7:77c8"; };
                                                        { "sas1-8249.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:131:215:b2ff:fea7:b438"; };
                                                        { "sas1-8256.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1fc"; };
                                                        { "sas1-8710.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:137:215:b2ff:fea7:8ef0"; };
                                                        { "sas1-8992.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:613:feaa:14ff:fede:3fe4"; };
                                                        { "sas1-9279.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:698:428d:5cff:fef5:b33e"; };
                                                        { "sas1-9427.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:1a3:feaa:14ff:feab:f94a"; };
                                                        { "sas1-9619.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:19a:428d:5cff:fef4:8d17"; };
                                                        { "sas2-0581.search.yandex.net"; 8080; 100.000; "2a02:6b8:c02:412:0:604:df5:d840"; };
                                                        { "sas2-3210.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:69b:215:b2ff:fea8:ce2"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "50ms";
                                                        backend_timeout = "6000ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                        status_code_blacklist = {
                                                          "402";
                                                        }; -- status_code_blacklist
                                                      }))
                                                    }; -- weighted2
                                                    attempts_rate_limiter = {
                                                      limit = 0.200;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                  refers = "requests_news_to_sas";
                                                }; -- report
                                              }; -- news_sas
                                              news_man = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "requests_news_to_man";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  balancer2 = {
                                                    timeout_policy = {
                                                      timeout = "6200ms";
                                                      unique_policy = {};
                                                    }; -- timeout_policy
                                                    attempts = 22;
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
                                                        { "man1-1171.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f280"; };
                                                        { "man1-1208.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6012:f652:14ff:fe48:96d0"; };
                                                        { "man1-1867.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:1e00"; };
                                                        { "man1-3910.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b89a"; };
                                                        { "man1-4252.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:b6ba"; };
                                                        { "man1-4256.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:bc2c"; };
                                                        { "man1-4312.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bf86"; };
                                                        { "man1-4779.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6047:e61d:2dff:fe00:88e0"; };
                                                        { "man1-8275.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:650b:215:b2ff:fea9:6762"; };
                                                        { "man1-8466.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6081:92e2:baff:fe5b:9c28"; };
                                                        { "man2-0333.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6093:92e2:baff:fea2:36ca"; };
                                                        { "man2-0371.search.yandex.net"; 8080; 100.000; "2a02:6b8:b000:6087:feaa:14ff:feea:8e74"; };
                                                        { "man2-6507.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:631:0:604:14a7:baaa"; };
                                                        { "man2-6515.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:83b:0:604:14a7:bc1d"; };
                                                        { "man2-6612.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:630:0:604:14a7:bb07"; };
                                                        { "man2-6622.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:83c:0:604:14a7:bc46"; };
                                                        { "man2-6626.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:63b:0:604:14a7:bb1d"; };
                                                        { "man2-6668.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:64b:0:604:14a7:ba42"; };
                                                        { "man2-6764.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:646:0:604:14a7:bb65"; };
                                                        { "man2-6819.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:656:0:604:14a7:660f"; };
                                                        { "man2-7224.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:643:0:604:14a9:687d"; };
                                                        { "man2-7382.search.yandex.net"; 8080; 100.000; "2a02:6b8:c01:83c:0:604:14a7:bafa"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "50ms";
                                                        backend_timeout = "6000ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                        status_code_blacklist = {
                                                          "402";
                                                        }; -- status_code_blacklist
                                                      }))
                                                    }; -- weighted2
                                                    attempts_rate_limiter = {
                                                      limit = 0.200;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- news_man
                                              news_vla = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "requests_news_to_vla";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  balancer2 = {
                                                    timeout_policy = {
                                                      timeout = "6200ms";
                                                      unique_policy = {};
                                                    }; -- timeout_policy
                                                    attempts = 22;
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
                                                        { "vla1-0490.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:34:0:604:db7:9c8a"; };
                                                        { "vla1-1229.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:78:0:604:db7:a306"; };
                                                        { "vla1-1263.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:8d:0:604:db7:ac34"; };
                                                        { "vla1-1586.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:52:0:604:db7:a465"; };
                                                        { "vla1-1958.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:1b:0:604:db7:99f0"; };
                                                        { "vla1-1968.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:93:0:604:db7:a190"; };
                                                        { "vla1-2068.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:67:0:604:db7:a32c"; };
                                                        { "vla1-2476.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:94:0:604:db7:a7cd"; };
                                                        { "vla1-2587.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:1f:0:604:db7:9fba"; };
                                                        { "vla1-2917.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:32:0:604:db7:990c"; };
                                                        { "vla1-3005.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:70:0:604:db7:a20a"; };
                                                        { "vla1-3700.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:80:0:604:db7:a847"; };
                                                        { "vla1-3816.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:80:0:604:db7:a83a"; };
                                                        { "vla1-3845.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:7f:0:604:db7:a193"; };
                                                        { "vla1-3993.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:87:0:604:db7:98f7"; };
                                                        { "vla1-4095.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:97:0:604:db7:a9ff"; };
                                                        { "vla1-4309.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:72:0:604:db7:a5ce"; };
                                                        { "vla1-4356.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:8a:0:604:db7:a812"; };
                                                        { "vla1-4369.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:90:0:604:db7:a992"; };
                                                        { "vla1-4397.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:95:0:604:db7:a9bc"; };
                                                        { "vla1-4414.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:87:0:604:db7:a821"; };
                                                        { "vla1-4427.search.yandex.net"; 8080; 100.000; "2a02:6b8:c0e:87:0:604:db7:a84a"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "50ms";
                                                        backend_timeout = "6000ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                        status_code_blacklist = {
                                                          "402";
                                                        }; -- status_code_blacklist
                                                      }))
                                                    }; -- weighted2
                                                    attempts_rate_limiter = {
                                                      limit = 0.200;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- news_vla
                                              news_devnull = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "requests_news_to_devnull";
                                                  ranges = "1ms";
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  errordocument = {
                                                    status = 204;
                                                    force_conn_close = false;
                                                  }; -- errordocument
                                                }; -- report
                                              }; -- news_devnull
                                            }; -- rr
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- default
                                    }; -- rr
                                    on_error = {
                                      shared = {
                                        uuid = "go_to_tkva";
                                        regexp = {
                                          no_tkva = {
                                            priority = 2;
                                            match_or = {
                                              {
                                                match_fsm = {
                                                  header = {
                                                    name = "X-YNews-No-Tkva";
                                                    value = "1";
                                                  }; -- header
                                                  case_insensitive = true;
                                                  surround = false;
                                                }; -- match_fsm
                                              };
                                              {
                                                match_fsm = {
                                                  header = {
                                                    name = "X-Yandex-ExpBoxes";
                                                    value = "(89937|.*;89937),.*";
                                                  }; -- header
                                                  case_insensitive = true;
                                                  surround = false;
                                                }; -- match_fsm
                                              };
                                              {
                                                match_fsm = {
                                                  header = {
                                                    name = "X-Yandex-ExpBoxes";
                                                    value = "(89938|.*;89938),.*";
                                                  }; -- header
                                                  case_insensitive = true;
                                                  surround = false;
                                                }; -- match_fsm
                                              };
                                              {
                                                match_fsm = {
                                                  header = {
                                                    name = "X-Yandex-ExpBoxes";
                                                    value = "(89760|.*;89760),.*";
                                                  }; -- header
                                                  case_insensitive = true;
                                                  surround = false;
                                                }; -- match_fsm
                                              };
                                            }; -- match_or
                                            errordocument = {
                                              status = 204;
                                              force_conn_close = false;
                                            }; -- errordocument
                                          }; -- no_tkva
                                          default = {
                                            priority = 1;
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              rr = {
                                                default = {
                                                  weight = 1.000;
                                                  report = {
                                                    uuid = "requests_tkva";
                                                    ranges = get_str_var("default_ranges");
                                                    just_storage = false;
                                                    disable_robotness = true;
                                                    disable_sslness = true;
                                                    events = {
                                                      stats = "report";
                                                    }; -- events
                                                    shared = {
                                                      uuid = "tkva_balancer";
                                                      balancer2 = {
                                                        watermark_policy = {
                                                          lo = 0.050;
                                                          hi = 0.100;
                                                          params_file = "./controls/watermark_policy.params_file";
                                                          unique_policy = {};
                                                        }; -- watermark_policy
                                                        attempts = 2;
                                                        attempts_file = "./controls/news.attempts";
                                                        connection_attempts = 2;
                                                        rr = {
                                                          weights_file = "./controls/traffic_control.weights";
                                                          news_sas = {
                                                            weight = 1.000;
                                                            report = {
                                                              uuid = "requests_tkva_to_sas";
                                                              ranges = get_str_var("default_ranges");
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              balancer2 = {
                                                                timeout_policy = {
                                                                  timeout = "500ms";
                                                                  unique_policy = {};
                                                                }; -- timeout_policy
                                                                attempts = 10;
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
                                                                    { "sas1-0081.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:14b:225:90ff:fe88:b7aa"; };
                                                                    { "sas1-0717.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:156:225:90ff:fe83:96c"; };
                                                                    { "sas1-1325.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:615:922b:34ff:fecf:3a92"; };
                                                                    { "sas1-1542.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:614:922b:34ff:fecf:2784"; };
                                                                    { "sas1-2741.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:60c:225:90ff:fe83:b02"; };
                                                                    { "sas1-3253.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:106:225:90ff:fe88:b204"; };
                                                                    { "sas1-5100.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:663:96de:80ff:fe56:a6b0"; };
                                                                    { "sas1-5336.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                                                    { "sas1-6637.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                                                    { "sas1-7837.search.yandex.net"; 20770; 40.000; "2a02:6b8:b000:12d:215:b2ff:fea7:77e4"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "50ms";
                                                                    backend_timeout = "200ms";
                                                                    fail_on_5xx = true;
                                                                    http_backend = true;
                                                                    buffering = false;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- weighted2
                                                                attempts_rate_limiter = {
                                                                  limit = 0.200;
                                                                  coeff = 0.990;
                                                                  switch_default = true;
                                                                }; -- attempts_rate_limiter
                                                              }; -- balancer2
                                                            }; -- report
                                                          }; -- news_sas
                                                          news_man = {
                                                            weight = 1.000;
                                                            report = {
                                                              uuid = "requests_tkva_to_man";
                                                              ranges = get_str_var("default_ranges");
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              balancer2 = {
                                                                timeout_policy = {
                                                                  timeout = "500ms";
                                                                  unique_policy = {};
                                                                }; -- timeout_policy
                                                                attempts = 10;
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
                                                                    { "man1-0095.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:6034:92e2:baff:fe52:7898"; };
                                                                    { "man1-0268.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:6034:92e2:baff:fe74:76fc"; };
                                                                    { "man1-0409.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                                                    { "man1-2955.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:ed20"; };
                                                                    { "man1-7192.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:6506:215:b2ff:fea9:62ea"; };
                                                                    { "man1-7666.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:606a:e61d:2dff:fe04:3310"; };
                                                                    { "man1-7742.search.yandex.net"; 10530; 40.000; "2a02:6b8:b000:606e:e61d:2dff:fe6d:f050"; };
                                                                    { "man2-6241.search.yandex.net"; 10530; 40.000; "2a02:6b8:c01:630:0:604:1465:cd5b"; };
                                                                    { "man2-6966.search.yandex.net"; 10530; 40.000; "2a02:6b8:c01:632:0:604:14a7:6801"; };
                                                                    { "man2-7353.search.yandex.net"; 10530; 40.000; "2a02:6b8:c01:653:0:604:14a9:6a1f"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "50ms";
                                                                    backend_timeout = "200ms";
                                                                    fail_on_5xx = true;
                                                                    http_backend = true;
                                                                    buffering = false;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- weighted2
                                                                attempts_rate_limiter = {
                                                                  limit = 0.200;
                                                                  coeff = 0.990;
                                                                  switch_default = true;
                                                                }; -- attempts_rate_limiter
                                                              }; -- balancer2
                                                            }; -- report
                                                          }; -- news_man
                                                          news_vla = {
                                                            weight = 1.000;
                                                            report = {
                                                              uuid = "requests_tkva_to_vla";
                                                              ranges = get_str_var("default_ranges");
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              balancer2 = {
                                                                timeout_policy = {
                                                                  timeout = "500ms";
                                                                  unique_policy = {};
                                                                }; -- timeout_policy
                                                                attempts = 10;
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
                                                                    { "vla1-0116.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                                                                    { "vla1-0142.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                                                    { "vla1-0367.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:9d:0:604:d8f:eb9e"; };
                                                                    { "vla1-0563.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:69:0:604:db7:9dde"; };
                                                                    { "vla1-0685.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:53:0:604:db7:a460"; };
                                                                    { "vla1-1387.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:54:0:604:db7:a6bd"; };
                                                                    { "vla1-1600.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:6f:0:604:db7:a2a6"; };
                                                                    { "vla1-4319.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:8c:0:604:db7:aa1f"; };
                                                                    { "vla1-4556.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:77:0:604:d8f:eb7f"; };
                                                                    { "vla1-4564.search.yandex.net"; 29470; 40.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "50ms";
                                                                    backend_timeout = "200ms";
                                                                    fail_on_5xx = true;
                                                                    http_backend = true;
                                                                    buffering = false;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- weighted2
                                                                attempts_rate_limiter = {
                                                                  limit = 0.200;
                                                                  coeff = 0.990;
                                                                  switch_default = true;
                                                                }; -- attempts_rate_limiter
                                                              }; -- balancer2
                                                            }; -- report
                                                          }; -- news_vla
                                                        }; -- rr
                                                        attempts_rate_limiter = {
                                                          limit = 0.200;
                                                          coeff = 0.990;
                                                          switch_default = true;
                                                        }; -- attempts_rate_limiter
                                                      }; -- balancer2
                                                    }; -- shared
                                                  }; -- report
                                                }; -- default
                                              }; -- rr
                                              on_error = {
                                                errordocument = {
                                                  status = 500;
                                                  force_conn_close = false;
                                                }; -- errordocument
                                              }; -- on_error
                                            }; -- balancer2
                                          }; -- default
                                        }; -- regexp
                                      }; -- shared
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- shared
                              }; -- report
                            }; -- report_req
                            default = {
                              priority = 1;
                              report = {
                                uuid = "news-report-renderer-req";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                shared = {
                                  uuid = "report_renderer_req";
                                  regexp = {
                                    report_request = {
                                      priority = 2;
                                      match_or = {
                                        {
                                          match_and = {
                                            {
                                              match_fsm = {
                                                url = "/yandsearch\\?.*rpt=.*";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_not = {
                                                match_fsm = {
                                                  url = ".*cl4url=.*";
                                                  case_insensitive = true;
                                                  surround = false;
                                                }; -- match_fsm
                                              }; -- match_not
                                            };
                                          }; -- match_and
                                        };
                                        {
                                          match_fsm = {
                                            url = "/podpiska.*";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            URI = "/smi/.+";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            url = "/quotes/.*graph_[\\d]+\\.json";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                      }; -- match_or
                                      shared = {
                                        uuid = "report-backend-req";
                                      }; -- shared
                                    }; -- report_request
                                    default = {
                                      priority = 1;
                                      shared = {
                                        uuid = "http_adapter_req";
                                        request_replier = {
                                          sink = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              rr = {
                                                {
                                                  weight = 1.000;
                                                  report = {
                                                    uuid = "requests_news_tkva_sink";
                                                    ranges = get_str_var("default_ranges");
                                                    just_storage = false;
                                                    disable_robotness = true;
                                                    disable_sslness = true;
                                                    events = {
                                                      stats = "report";
                                                    }; -- events
                                                    balancer2 = {
                                                      timeout_policy = {
                                                        timeout = "1s";
                                                        unique_policy = {};
                                                      }; -- timeout_policy
                                                      attempts = 6;
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
                                                          { "ap6ive22ucga6djy.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:3507:0:46a9:8eab:0"; };
                                                          { "iitjs5e663va4wf7.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3e36:0:46a9:2cb1:0"; };
                                                          { "ljrrkm5r2p6hv2sx.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2b0c:100:0:e7d7:0"; };
                                                          { "mpm6pcafjtxji7ho.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:31c0:100:0:6372:0"; };
                                                          { "rdjeduryuf6amkol.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2323:0:46a9:641:0"; };
                                                          { "sacmkajq5sq2j34l.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:3b0a:100:0:8f91:0"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "50ms";
                                                          backend_timeout = "500ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- weighted2
                                                      attempts_rate_limiter = {
                                                        limit = 0.200;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- report
                                                };
                                              }; -- rr
                                            }; -- balancer2
                                          }; -- sink
                                          enable_failed_requests_replication = false;
                                          rate = 0.100;
                                          rate_file = "./controls/request_repliers.ratefile";
                                          threshold = {
                                            lo_bytes = 512;
                                            hi_bytes = 1024;
                                            recv_timeout = "1s";
                                            pass_timeout = "2s";
                                            headers = {
                                              create = {
                                                ["X-YNews-Use-Report-Renderer"] = "1";
                                              }; -- create
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 1;
                                                rr = {
                                                  default = {
                                                    weight = 1.000;
                                                    shared = {
                                                      uuid = "http_adapter_balancer";
                                                      balancer2 = {
                                                        watermark_policy = {
                                                          lo = 0.500;
                                                          hi = 0.600;
                                                          params_file = "./controls/watermark_policy.params_file";
                                                          unique_policy = {};
                                                        }; -- watermark_policy
                                                        attempts = 2;
                                                        attempts_file = "./controls/news.attempts";
                                                        connection_attempts = 2;
                                                        rr = {
                                                          weights_file = "./controls/traffic_control.weights";
                                                          news_sas = {
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
                                                                attempts = 5;
                                                                rr = {
                                                                  weights_file = "./controls/traffic_control.weights";
                                                                  dynamicbalancing_on = {
                                                                    weight = -1.000;
                                                                    report = {
                                                                      uuid = "dynamic_requests_news_to_sas";
                                                                      ranges = get_str_var("default_ranges");
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        timeout_policy = {
                                                                          timeout = "500ms";
                                                                          unique_policy = {};
                                                                        }; -- timeout_policy
                                                                        attempts = 72;
                                                                        dynamic = {
                                                                          max_pessimized_share = 0.100;
                                                                          min_pessimization_coeff = 0.100;
                                                                          weight_increase_step = 0.100;
                                                                          history_interval = "10s";
                                                                          backends_name = "http_adapter_dynamic_requests_to_sas";
                                                                          unpack(gen_proxy_backends({
                                                                            { "sas1-1625-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:460c:10b:8779:0:55aa"; };
                                                                            { "sas1-1663-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:460f:10b:8779:0:55aa"; };
                                                                            { "sas1-6166-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4d1d:10b:8779:0:55aa"; };
                                                                            { "sas1-6835-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9227:10b:8779:0:55aa"; };
                                                                            { "sas1-6854-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a0b:10b:8779:0:55aa"; };
                                                                            { "sas1-6862-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a10:10b:8779:0:55aa"; };
                                                                            { "sas1-6881-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8523:10b:8779:0:55aa"; };
                                                                            { "sas1-6885-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:851f:10b:8779:0:55aa"; };
                                                                            { "sas1-6906-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4288:10b:8779:0:55aa"; };
                                                                            { "sas1-6940-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8b0c:10b:8779:0:55aa"; };
                                                                            { "sas1-6942-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8b10:10b:8779:0:55aa"; };
                                                                            { "sas1-6974-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5283:10b:8779:0:55aa"; };
                                                                            { "sas1-6982-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5281:10b:8779:0:55aa"; };
                                                                            { "sas1-7007-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8c9c:10b:8779:0:55aa"; };
                                                                            { "sas1-7014-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8c85:10b:8779:0:55aa"; };
                                                                            { "sas1-7016-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8c81:10b:8779:0:55aa"; };
                                                                            { "sas1-7027-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8ca5:10b:8779:0:55aa"; };
                                                                            { "sas1-7043-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4923:10b:8779:0:55aa"; };
                                                                            { "sas1-7044-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4901:10b:8779:0:55aa"; };
                                                                            { "sas1-7128-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:998d:10b:8779:0:55aa"; };
                                                                            { "sas1-7131-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:99a4:10b:8779:0:55aa"; };
                                                                            { "sas1-7165-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8a85:10b:8779:0:55aa"; };
                                                                            { "sas1-7170-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8aa8:10b:8779:0:55aa"; };
                                                                            { "sas1-7181-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8a8e:10b:8779:0:55aa"; };
                                                                            { "sas1-7203-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a90:10b:8779:0:55aa"; };
                                                                            { "sas1-7211-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a92:10b:8779:0:55aa"; };
                                                                            { "sas1-7217-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a91:10b:8779:0:55aa"; };
                                                                            { "sas1-7242-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:899f:10b:8779:0:55aa"; };
                                                                            { "sas1-7247-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:898f:10b:8779:0:55aa"; };
                                                                            { "sas1-7257-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:7f85:10b:8779:0:55aa"; };
                                                                            { "sas1-7291-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:430e:10b:8779:0:55aa"; };
                                                                            { "sas1-7357-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:420c:10b:8779:0:55aa"; };
                                                                            { "sas1-7468-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:439d:10b:8779:0:55aa"; };
                                                                            { "sas1-7478-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:85ad:10b:8779:0:55aa"; };
                                                                            { "sas1-7489-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9b90:10b:8779:0:55aa"; };
                                                                            { "sas1-7491-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9b8a:10b:8779:0:55aa"; };
                                                                            { "sas1-7563-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:548f:10b:8779:0:55aa"; };
                                                                            { "sas1-7581-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5480:10b:8779:0:55aa"; };
                                                                            { "sas1-7592-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:859e:10b:8779:0:55aa"; };
                                                                            { "sas1-7701-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8410:10b:8779:0:55aa"; };
                                                                            { "sas1-7707-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:840f:10b:8779:0:55aa"; };
                                                                            { "sas1-7729-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5300:10b:8779:0:55aa"; };
                                                                            { "sas1-7731-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5301:10b:8779:0:55aa"; };
                                                                            { "sas1-7734-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5322:10b:8779:0:55aa"; };
                                                                            { "sas1-7799-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9890:10b:8779:0:55aa"; };
                                                                            { "sas1-7801-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5101:10b:8779:0:55aa"; };
                                                                            { "sas1-7822-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:98a5:10b:8779:0:55aa"; };
                                                                            { "sas1-7828-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:98a7:10b:8779:0:55aa"; };
                                                                            { "sas1-7888-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:53a2:10b:8779:0:55aa"; };
                                                                            { "sas1-7893-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:980f:10b:8779:0:55aa"; };
                                                                            { "sas1-7898-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9829:10b:8779:0:55aa"; };
                                                                            { "sas1-8018-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:550a:10b:8779:0:55aa"; };
                                                                            { "sas1-8022-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5507:10b:8779:0:55aa"; };
                                                                            { "sas1-8051-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:82ac:10b:8779:0:55aa"; };
                                                                            { "sas1-8175-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4980:10b:8779:0:55aa"; };
                                                                            { "sas1-8191-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8107:10b:8779:0:55aa"; };
                                                                            { "sas1-8249-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4184:10b:8779:0:55aa"; };
                                                                            { "sas1-8256-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9b03:10b:8779:0:55aa"; };
                                                                            { "sas1-8376-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:7bae:10b:8779:0:55aa"; };
                                                                            { "sas1-8569-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5602:10b:8779:0:55aa"; };
                                                                            { "sas1-8578-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5629:10b:8779:0:55aa"; };
                                                                            { "sas1-8579-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5600:10b:8779:0:55aa"; };
                                                                            { "sas1-8645-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:442b:10b:8779:0:55aa"; };
                                                                            { "sas1-8703-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:522a:10b:8779:0:55aa"; };
                                                                            { "sas1-8707-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:81a0:10b:8779:0:55aa"; };
                                                                            { "sas1-8710-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5207:10b:8779:0:55aa"; };
                                                                            { "sas1-8848-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:7f95:10b:8779:0:55aa"; };
                                                                            { "sas1-8946-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4e9e:10b:8779:0:55aa"; };
                                                                            { "sas1-8950-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4ea8:10b:8779:0:55aa"; };
                                                                            { "sas1-8992-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8308:10b:8779:0:55aa"; };
                                                                            { "sas1-9427-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9786:10b:8779:0:55aa"; };
                                                                            { "sas1-9471-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8805:10b:8779:0:55aa"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "5000ms";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 0;
                                                                            need_resolve = true;
                                                                            status_code_blacklist = {
                                                                              "204";
                                                                            }; -- status_code_blacklist
                                                                          }))
                                                                        }; -- dynamic
                                                                        attempts_rate_limiter = {
                                                                          limit = 0.200;
                                                                          coeff = 0.990;
                                                                          switch_default = true;
                                                                        }; -- attempts_rate_limiter
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- dynamicbalancing_on
                                                                  dynamicbalancing_off = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "non_dynamic_requests_news_to_sas";
                                                                      ranges = get_str_var("default_ranges");
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        timeout_policy = {
                                                                          timeout = "500ms";
                                                                          unique_policy = {};
                                                                        }; -- timeout_policy
                                                                        attempts = 72;
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
                                                                            { "sas1-1625-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:460c:10b:8779:0:55aa"; };
                                                                            { "sas1-1663-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:460f:10b:8779:0:55aa"; };
                                                                            { "sas1-6166-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4d1d:10b:8779:0:55aa"; };
                                                                            { "sas1-6835-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9227:10b:8779:0:55aa"; };
                                                                            { "sas1-6854-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a0b:10b:8779:0:55aa"; };
                                                                            { "sas1-6862-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a10:10b:8779:0:55aa"; };
                                                                            { "sas1-6881-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8523:10b:8779:0:55aa"; };
                                                                            { "sas1-6885-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:851f:10b:8779:0:55aa"; };
                                                                            { "sas1-6906-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4288:10b:8779:0:55aa"; };
                                                                            { "sas1-6940-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8b0c:10b:8779:0:55aa"; };
                                                                            { "sas1-6942-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8b10:10b:8779:0:55aa"; };
                                                                            { "sas1-6974-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5283:10b:8779:0:55aa"; };
                                                                            { "sas1-6982-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5281:10b:8779:0:55aa"; };
                                                                            { "sas1-7007-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8c9c:10b:8779:0:55aa"; };
                                                                            { "sas1-7014-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8c85:10b:8779:0:55aa"; };
                                                                            { "sas1-7016-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8c81:10b:8779:0:55aa"; };
                                                                            { "sas1-7027-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8ca5:10b:8779:0:55aa"; };
                                                                            { "sas1-7043-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4923:10b:8779:0:55aa"; };
                                                                            { "sas1-7044-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4901:10b:8779:0:55aa"; };
                                                                            { "sas1-7128-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:998d:10b:8779:0:55aa"; };
                                                                            { "sas1-7131-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:99a4:10b:8779:0:55aa"; };
                                                                            { "sas1-7165-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8a85:10b:8779:0:55aa"; };
                                                                            { "sas1-7170-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8aa8:10b:8779:0:55aa"; };
                                                                            { "sas1-7181-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8a8e:10b:8779:0:55aa"; };
                                                                            { "sas1-7203-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a90:10b:8779:0:55aa"; };
                                                                            { "sas1-7211-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a92:10b:8779:0:55aa"; };
                                                                            { "sas1-7217-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4a91:10b:8779:0:55aa"; };
                                                                            { "sas1-7242-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:899f:10b:8779:0:55aa"; };
                                                                            { "sas1-7247-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:898f:10b:8779:0:55aa"; };
                                                                            { "sas1-7257-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:7f85:10b:8779:0:55aa"; };
                                                                            { "sas1-7291-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:430e:10b:8779:0:55aa"; };
                                                                            { "sas1-7357-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:420c:10b:8779:0:55aa"; };
                                                                            { "sas1-7468-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:439d:10b:8779:0:55aa"; };
                                                                            { "sas1-7478-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:85ad:10b:8779:0:55aa"; };
                                                                            { "sas1-7489-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9b90:10b:8779:0:55aa"; };
                                                                            { "sas1-7491-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9b8a:10b:8779:0:55aa"; };
                                                                            { "sas1-7563-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:548f:10b:8779:0:55aa"; };
                                                                            { "sas1-7581-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5480:10b:8779:0:55aa"; };
                                                                            { "sas1-7592-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:859e:10b:8779:0:55aa"; };
                                                                            { "sas1-7701-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8410:10b:8779:0:55aa"; };
                                                                            { "sas1-7707-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:840f:10b:8779:0:55aa"; };
                                                                            { "sas1-7729-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5300:10b:8779:0:55aa"; };
                                                                            { "sas1-7731-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5301:10b:8779:0:55aa"; };
                                                                            { "sas1-7734-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5322:10b:8779:0:55aa"; };
                                                                            { "sas1-7799-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9890:10b:8779:0:55aa"; };
                                                                            { "sas1-7801-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5101:10b:8779:0:55aa"; };
                                                                            { "sas1-7822-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:98a5:10b:8779:0:55aa"; };
                                                                            { "sas1-7828-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:98a7:10b:8779:0:55aa"; };
                                                                            { "sas1-7888-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:53a2:10b:8779:0:55aa"; };
                                                                            { "sas1-7893-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:980f:10b:8779:0:55aa"; };
                                                                            { "sas1-7898-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9829:10b:8779:0:55aa"; };
                                                                            { "sas1-8018-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:550a:10b:8779:0:55aa"; };
                                                                            { "sas1-8022-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5507:10b:8779:0:55aa"; };
                                                                            { "sas1-8051-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:82ac:10b:8779:0:55aa"; };
                                                                            { "sas1-8175-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4980:10b:8779:0:55aa"; };
                                                                            { "sas1-8191-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8107:10b:8779:0:55aa"; };
                                                                            { "sas1-8249-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4184:10b:8779:0:55aa"; };
                                                                            { "sas1-8256-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9b03:10b:8779:0:55aa"; };
                                                                            { "sas1-8376-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:7bae:10b:8779:0:55aa"; };
                                                                            { "sas1-8569-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5602:10b:8779:0:55aa"; };
                                                                            { "sas1-8578-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5629:10b:8779:0:55aa"; };
                                                                            { "sas1-8579-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5600:10b:8779:0:55aa"; };
                                                                            { "sas1-8645-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:442b:10b:8779:0:55aa"; };
                                                                            { "sas1-8703-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:522a:10b:8779:0:55aa"; };
                                                                            { "sas1-8707-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:81a0:10b:8779:0:55aa"; };
                                                                            { "sas1-8710-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:5207:10b:8779:0:55aa"; };
                                                                            { "sas1-8848-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:7f95:10b:8779:0:55aa"; };
                                                                            { "sas1-8946-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4e9e:10b:8779:0:55aa"; };
                                                                            { "sas1-8950-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:4ea8:10b:8779:0:55aa"; };
                                                                            { "sas1-8992-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8308:10b:8779:0:55aa"; };
                                                                            { "sas1-9427-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:9786:10b:8779:0:55aa"; };
                                                                            { "sas1-9471-sas-news-ah-http-adapter-21930.gencfg-c.yandex.net"; 21930; 8.000; "2a02:6b8:c08:8805:10b:8779:0:55aa"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "5000ms";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 0;
                                                                            need_resolve = true;
                                                                            status_code_blacklist = {
                                                                              "204";
                                                                            }; -- status_code_blacklist
                                                                          }))
                                                                        }; -- weighted2
                                                                        attempts_rate_limiter = {
                                                                          limit = 0.200;
                                                                          coeff = 0.990;
                                                                          switch_default = true;
                                                                        }; -- attempts_rate_limiter
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- dynamicbalancing_off
                                                                }; -- rr
                                                              }; -- balancer2
                                                              refers = "requests_news_to_sas";
                                                            }; -- report
                                                          }; -- news_sas
                                                          news_man = {
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
                                                                attempts = 5;
                                                                rr = {
                                                                  weights_file = "./controls/traffic_control.weights";
                                                                  dynamicbalancing_on = {
                                                                    weight = -1.000;
                                                                    report = {
                                                                      uuid = "dynamic_requests_news_to_man";
                                                                      ranges = get_str_var("default_ranges");
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        timeout_policy = {
                                                                          timeout = "500ms";
                                                                          unique_policy = {};
                                                                        }; -- timeout_policy
                                                                        attempts = 56;
                                                                        dynamic = {
                                                                          max_pessimized_share = 0.100;
                                                                          min_pessimization_coeff = 0.100;
                                                                          weight_increase_step = 0.100;
                                                                          history_interval = "10s";
                                                                          backends_name = "http_adapter_dynamic_requests_to_man";
                                                                          unpack(gen_proxy_backends({
                                                                            { "man1-0723-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3423:10b:877a:0:5dac"; };
                                                                            { "man1-0925-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:1f81:10b:877a:0:5dac"; };
                                                                            { "man1-1493-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2a1d:10b:877a:0:5dac"; };
                                                                            { "man1-1666-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:17a1:10b:877a:0:5dac"; };
                                                                            { "man1-1750-2a7-man-news-ah-http-ad-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:5cca:10b:877a:0:5dac"; };
                                                                            { "man1-1867-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2817:10b:877a:0:5dac"; };
                                                                            { "man1-2214-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:361:10b:877a:0:5dac"; };
                                                                            { "man1-3204-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:94:10b:877a:0:5dac"; };
                                                                            { "man1-3293-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3c20:10b:877a:0:5dac"; };
                                                                            { "man1-3487-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3325:10b:877a:0:5dac"; };
                                                                            { "man1-3488-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:310d:10b:877a:0:5dac"; };
                                                                            { "man1-3492-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3225:10b:877a:0:5dac"; };
                                                                            { "man1-3494-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:358c:10b:877a:0:5dac"; };
                                                                            { "man1-3496-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3615:10b:877a:0:5dac"; };
                                                                            { "man1-3651-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:ff7:10b:877a:0:5dac"; };
                                                                            { "man1-3739-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3796:10b:877a:0:5dac"; };
                                                                            { "man1-4252-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3d75:10b:877a:0:5dac"; };
                                                                            { "man1-4312-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3c98:10b:877a:0:5dac"; };
                                                                            { "man1-4345-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3a90:10b:877a:0:5dac"; };
                                                                            { "man1-4381-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:36f6:10b:877a:0:5dac"; };
                                                                            { "man1-4578-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2684:10b:877a:0:5dac"; };
                                                                            { "man1-4726-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:259:10b:877a:0:5dac"; };
                                                                            { "man1-4779-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:123:10b:877a:0:5dac"; };
                                                                            { "man1-4804-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2d8:10b:877a:0:5dac"; };
                                                                            { "man1-5038-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:13fb:10b:877a:0:5dac"; };
                                                                            { "man1-5213-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:49d:10b:877a:0:5dac"; };
                                                                            { "man1-5585-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:d21:10b:877a:0:5dac"; };
                                                                            { "man1-8208-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2627:10b:877a:0:5dac"; };
                                                                            { "man1-8415-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:1c14:10b:877a:0:5dac"; };
                                                                            { "man1-8466-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:1524:10b:877a:0:5dac"; };
                                                                            { "man2-4817-f92-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:279f:10b:877a:0:5dac"; };
                                                                            { "man2-6083-b55-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2b0d:10b:877a:0:5dac"; };
                                                                            { "man2-6141-9d6-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2698:10b:877a:0:5dac"; };
                                                                            { "man2-6143-038-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:1f28:10b:877a:0:5dac"; };
                                                                            { "man2-6401-720-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:c95:10b:877a:0:5dac"; };
                                                                            { "man2-6445-61e-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:ca7:10b:877a:0:5dac"; };
                                                                            { "man2-6504-21c-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:271a:10b:877a:0:5dac"; };
                                                                            { "man2-6515-8f4-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1d90:10b:877a:0:5dac"; };
                                                                            { "man2-6521-0d1-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1d8d:10b:877a:0:5dac"; };
                                                                            { "man2-6528-4ac-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2525:10b:877a:0:5dac"; };
                                                                            { "man2-6586-959-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2987:10b:877a:0:5dac"; };
                                                                            { "man2-6612-95f-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2597:10b:877a:0:5dac"; };
                                                                            { "man2-6622-562-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1e2f:10b:877a:0:5dac"; };
                                                                            { "man2-6662-bb2-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:3087:10b:877a:0:5dac"; };
                                                                            { "man2-6668-81d-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2809:10b:877a:0:5dac"; };
                                                                            { "man2-6731-d81-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:211a:10b:877a:0:5dac"; };
                                                                            { "man2-6764-78a-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:1019:10b:877a:0:5dac"; };
                                                                            { "man2-6819-58b-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2925:10b:877a:0:5dac"; };
                                                                            { "man2-6844-fd6-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:2220:10b:877a:0:5dac"; };
                                                                            { "man2-6941-8c6-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2824:10b:877a:0:5dac"; };
                                                                            { "man2-6983-5c3-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:308f:10b:877a:0:5dac"; };
                                                                            { "man2-7033-d70-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:3092:10b:877a:0:5dac"; };
                                                                            { "man2-7224-31f-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2e04:10b:877a:0:5dac"; };
                                                                            { "man2-7271-e16-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2dae:10b:877a:0:5dac"; };
                                                                            { "man2-7317-b1e-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:240b:10b:877a:0:5dac"; };
                                                                            { "man2-7382-9f7-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1e08:10b:877a:0:5dac"; };
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
                                                                              "204";
                                                                            }; -- status_code_blacklist
                                                                          }))
                                                                        }; -- dynamic
                                                                        attempts_rate_limiter = {
                                                                          limit = 0.200;
                                                                          coeff = 0.990;
                                                                          switch_default = true;
                                                                        }; -- attempts_rate_limiter
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- dynamicbalancing_on
                                                                  dynamicbalancing_off = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "non_dynamic_requests_news_to_man";
                                                                      ranges = get_str_var("default_ranges");
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        timeout_policy = {
                                                                          timeout = "500ms";
                                                                          unique_policy = {};
                                                                        }; -- timeout_policy
                                                                        attempts = 56;
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
                                                                            { "man1-0723-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3423:10b:877a:0:5dac"; };
                                                                            { "man1-0925-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:1f81:10b:877a:0:5dac"; };
                                                                            { "man1-1493-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2a1d:10b:877a:0:5dac"; };
                                                                            { "man1-1666-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:17a1:10b:877a:0:5dac"; };
                                                                            { "man1-1750-2a7-man-news-ah-http-ad-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:5cca:10b:877a:0:5dac"; };
                                                                            { "man1-1867-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2817:10b:877a:0:5dac"; };
                                                                            { "man1-2214-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:361:10b:877a:0:5dac"; };
                                                                            { "man1-3204-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:94:10b:877a:0:5dac"; };
                                                                            { "man1-3293-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3c20:10b:877a:0:5dac"; };
                                                                            { "man1-3487-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3325:10b:877a:0:5dac"; };
                                                                            { "man1-3488-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:310d:10b:877a:0:5dac"; };
                                                                            { "man1-3492-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3225:10b:877a:0:5dac"; };
                                                                            { "man1-3494-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:358c:10b:877a:0:5dac"; };
                                                                            { "man1-3496-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3615:10b:877a:0:5dac"; };
                                                                            { "man1-3651-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:ff7:10b:877a:0:5dac"; };
                                                                            { "man1-3739-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3796:10b:877a:0:5dac"; };
                                                                            { "man1-4252-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3d75:10b:877a:0:5dac"; };
                                                                            { "man1-4312-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3c98:10b:877a:0:5dac"; };
                                                                            { "man1-4345-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:3a90:10b:877a:0:5dac"; };
                                                                            { "man1-4381-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:36f6:10b:877a:0:5dac"; };
                                                                            { "man1-4578-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2684:10b:877a:0:5dac"; };
                                                                            { "man1-4726-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:259:10b:877a:0:5dac"; };
                                                                            { "man1-4779-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:123:10b:877a:0:5dac"; };
                                                                            { "man1-4804-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2d8:10b:877a:0:5dac"; };
                                                                            { "man1-5038-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:13fb:10b:877a:0:5dac"; };
                                                                            { "man1-5213-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:49d:10b:877a:0:5dac"; };
                                                                            { "man1-5585-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:d21:10b:877a:0:5dac"; };
                                                                            { "man1-8208-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:2627:10b:877a:0:5dac"; };
                                                                            { "man1-8415-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:1c14:10b:877a:0:5dac"; };
                                                                            { "man1-8466-man-news-ah-http-adapter-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0b:1524:10b:877a:0:5dac"; };
                                                                            { "man2-4817-f92-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:279f:10b:877a:0:5dac"; };
                                                                            { "man2-6083-b55-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2b0d:10b:877a:0:5dac"; };
                                                                            { "man2-6141-9d6-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2698:10b:877a:0:5dac"; };
                                                                            { "man2-6143-038-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:1f28:10b:877a:0:5dac"; };
                                                                            { "man2-6401-720-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:c95:10b:877a:0:5dac"; };
                                                                            { "man2-6445-61e-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:ca7:10b:877a:0:5dac"; };
                                                                            { "man2-6504-21c-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:271a:10b:877a:0:5dac"; };
                                                                            { "man2-6515-8f4-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1d90:10b:877a:0:5dac"; };
                                                                            { "man2-6521-0d1-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1d8d:10b:877a:0:5dac"; };
                                                                            { "man2-6528-4ac-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2525:10b:877a:0:5dac"; };
                                                                            { "man2-6586-959-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2987:10b:877a:0:5dac"; };
                                                                            { "man2-6612-95f-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2597:10b:877a:0:5dac"; };
                                                                            { "man2-6622-562-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1e2f:10b:877a:0:5dac"; };
                                                                            { "man2-6662-bb2-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:3087:10b:877a:0:5dac"; };
                                                                            { "man2-6668-81d-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2809:10b:877a:0:5dac"; };
                                                                            { "man2-6731-d81-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:211a:10b:877a:0:5dac"; };
                                                                            { "man2-6764-78a-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:1019:10b:877a:0:5dac"; };
                                                                            { "man2-6819-58b-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2925:10b:877a:0:5dac"; };
                                                                            { "man2-6844-fd6-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:2220:10b:877a:0:5dac"; };
                                                                            { "man2-6941-8c6-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2824:10b:877a:0:5dac"; };
                                                                            { "man2-6983-5c3-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:308f:10b:877a:0:5dac"; };
                                                                            { "man2-7033-d70-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:3092:10b:877a:0:5dac"; };
                                                                            { "man2-7224-31f-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2e04:10b:877a:0:5dac"; };
                                                                            { "man2-7271-e16-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:2dae:10b:877a:0:5dac"; };
                                                                            { "man2-7317-b1e-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c0a:240b:10b:877a:0:5dac"; };
                                                                            { "man2-7382-9f7-man-news-ah-http-824-23980.gencfg-c.yandex.net"; 23980; 8.000; "2a02:6b8:c13:1e08:10b:877a:0:5dac"; };
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
                                                                              "204";
                                                                            }; -- status_code_blacklist
                                                                          }))
                                                                        }; -- weighted2
                                                                        attempts_rate_limiter = {
                                                                          limit = 0.200;
                                                                          coeff = 0.990;
                                                                          switch_default = true;
                                                                        }; -- attempts_rate_limiter
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- dynamicbalancing_off
                                                                }; -- rr
                                                              }; -- balancer2
                                                              refers = "requests_news_to_man";
                                                            }; -- report
                                                          }; -- news_man
                                                          news_vla = {
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
                                                                attempts = 5;
                                                                rr = {
                                                                  weights_file = "./controls/traffic_control.weights";
                                                                  dynamicbalancing_on = {
                                                                    weight = -1.000;
                                                                    report = {
                                                                      uuid = "dynamic_requests_news_to_vla";
                                                                      ranges = get_str_var("default_ranges");
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        timeout_policy = {
                                                                          timeout = "500ms";
                                                                          unique_policy = {};
                                                                        }; -- timeout_policy
                                                                        attempts = 36;
                                                                        dynamic = {
                                                                          max_pessimized_share = 0.100;
                                                                          min_pessimization_coeff = 0.100;
                                                                          weight_increase_step = 0.100;
                                                                          history_interval = "10s";
                                                                          backends_name = "http_adapter_dynamic_requests_to_vla";
                                                                          unpack(gen_proxy_backends({
                                                                            { "vla1-0254-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4f04:10b:8778:0:7c64"; };
                                                                            { "vla1-0255-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4f0e:10b:8778:0:7c64"; };
                                                                            { "vla1-0485-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a23:10b:8778:0:7c64"; };
                                                                            { "vla1-0490-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a01:10b:8778:0:7c64"; };
                                                                            { "vla1-0498-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a04:10b:8778:0:7c64"; };
                                                                            { "vla1-0499-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1396:10b:8778:0:7c64"; };
                                                                            { "vla1-0541-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:912:10b:8778:0:7c64"; };
                                                                            { "vla1-0860-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:220a:10b:8778:0:7c64"; };
                                                                            { "vla1-1137-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4681:10b:8778:0:7c64"; };
                                                                            { "vla1-1263-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4693:10b:8778:0:7c64"; };
                                                                            { "vla1-1291-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:339b:10b:8778:0:7c64"; };
                                                                            { "vla1-1498-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3889:10b:8778:0:7c64"; };
                                                                            { "vla1-1586-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:291b:10b:8778:0:7c64"; };
                                                                            { "vla1-1627-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a0d:10b:8778:0:7c64"; };
                                                                            { "vla1-1704-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:38a0:10b:8778:0:7c64"; };
                                                                            { "vla1-1925-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:2a04:10b:8778:0:7c64"; };
                                                                            { "vla1-1968-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4998:10b:8778:0:7c64"; };
                                                                            { "vla1-2068-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3391:10b:8778:0:7c64"; };
                                                                            { "vla1-2463-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a9a:10b:8778:0:7c64"; };
                                                                            { "vla1-2476-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a0a:10b:8778:0:7c64"; };
                                                                            { "vla1-2587-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:f85:10b:8778:0:7c64"; };
                                                                            { "vla1-3845-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3f8f:10b:8778:0:7c64"; };
                                                                            { "vla1-3993-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:439f:10b:8778:0:7c64"; };
                                                                            { "vla1-4095-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4b8d:10b:8778:0:7c64"; };
                                                                            { "vla1-4345-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a91:10b:8778:0:7c64"; };
                                                                            { "vla1-4355-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4807:10b:8778:0:7c64"; };
                                                                            { "vla1-4356-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4511:10b:8778:0:7c64"; };
                                                                            { "vla1-4369-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:480a:10b:8778:0:7c64"; };
                                                                            { "vla1-4397-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a84:10b:8778:0:7c64"; };
                                                                            { "vla1-4414-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4393:10b:8778:0:7c64"; };
                                                                            { "vla1-4426-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3c8e:10b:8778:0:7c64"; };
                                                                            { "vla1-4427-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:438e:10b:8778:0:7c64"; };
                                                                            { "vla1-4473-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4d97:10b:8778:0:7c64"; };
                                                                            { "vla1-4484-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4301:10b:8778:0:7c64"; };
                                                                            { "vla1-4486-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4c9d:10b:8778:0:7c64"; };
                                                                            { "vla1-4524-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3600:10b:8778:0:7c64"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "5000ms";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 0;
                                                                            need_resolve = true;
                                                                            status_code_blacklist = {
                                                                              "204";
                                                                            }; -- status_code_blacklist
                                                                          }))
                                                                        }; -- dynamic
                                                                        attempts_rate_limiter = {
                                                                          limit = 0.200;
                                                                          coeff = 0.990;
                                                                          switch_default = true;
                                                                        }; -- attempts_rate_limiter
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- dynamicbalancing_on
                                                                  dynamicbalancing_off = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "non_dynamic_requests_news_to_vla";
                                                                      ranges = get_str_var("default_ranges");
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        timeout_policy = {
                                                                          timeout = "500ms";
                                                                          unique_policy = {};
                                                                        }; -- timeout_policy
                                                                        attempts = 36;
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
                                                                            { "vla1-0254-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4f04:10b:8778:0:7c64"; };
                                                                            { "vla1-0255-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4f0e:10b:8778:0:7c64"; };
                                                                            { "vla1-0485-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a23:10b:8778:0:7c64"; };
                                                                            { "vla1-0490-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a01:10b:8778:0:7c64"; };
                                                                            { "vla1-0498-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a04:10b:8778:0:7c64"; };
                                                                            { "vla1-0499-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1396:10b:8778:0:7c64"; };
                                                                            { "vla1-0541-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:912:10b:8778:0:7c64"; };
                                                                            { "vla1-0860-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:220a:10b:8778:0:7c64"; };
                                                                            { "vla1-1137-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4681:10b:8778:0:7c64"; };
                                                                            { "vla1-1263-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4693:10b:8778:0:7c64"; };
                                                                            { "vla1-1291-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:339b:10b:8778:0:7c64"; };
                                                                            { "vla1-1498-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3889:10b:8778:0:7c64"; };
                                                                            { "vla1-1586-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:291b:10b:8778:0:7c64"; };
                                                                            { "vla1-1627-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:1a0d:10b:8778:0:7c64"; };
                                                                            { "vla1-1704-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:38a0:10b:8778:0:7c64"; };
                                                                            { "vla1-1925-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:2a04:10b:8778:0:7c64"; };
                                                                            { "vla1-1968-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4998:10b:8778:0:7c64"; };
                                                                            { "vla1-2068-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3391:10b:8778:0:7c64"; };
                                                                            { "vla1-2463-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a9a:10b:8778:0:7c64"; };
                                                                            { "vla1-2476-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a0a:10b:8778:0:7c64"; };
                                                                            { "vla1-2587-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:f85:10b:8778:0:7c64"; };
                                                                            { "vla1-3845-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3f8f:10b:8778:0:7c64"; };
                                                                            { "vla1-3993-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:439f:10b:8778:0:7c64"; };
                                                                            { "vla1-4095-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4b8d:10b:8778:0:7c64"; };
                                                                            { "vla1-4345-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a91:10b:8778:0:7c64"; };
                                                                            { "vla1-4355-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4807:10b:8778:0:7c64"; };
                                                                            { "vla1-4356-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4511:10b:8778:0:7c64"; };
                                                                            { "vla1-4369-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:480a:10b:8778:0:7c64"; };
                                                                            { "vla1-4397-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4a84:10b:8778:0:7c64"; };
                                                                            { "vla1-4414-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4393:10b:8778:0:7c64"; };
                                                                            { "vla1-4426-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3c8e:10b:8778:0:7c64"; };
                                                                            { "vla1-4427-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:438e:10b:8778:0:7c64"; };
                                                                            { "vla1-4473-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4d97:10b:8778:0:7c64"; };
                                                                            { "vla1-4484-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4301:10b:8778:0:7c64"; };
                                                                            { "vla1-4486-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:4c9d:10b:8778:0:7c64"; };
                                                                            { "vla1-4524-vla-news-ah-http-adapter-31844.gencfg-c.yandex.net"; 31844; 8.000; "2a02:6b8:c0d:3600:10b:8778:0:7c64"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "5000ms";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 0;
                                                                            need_resolve = true;
                                                                            status_code_blacklist = {
                                                                              "204";
                                                                            }; -- status_code_blacklist
                                                                          }))
                                                                        }; -- weighted2
                                                                        attempts_rate_limiter = {
                                                                          limit = 0.200;
                                                                          coeff = 0.990;
                                                                          switch_default = true;
                                                                        }; -- attempts_rate_limiter
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- dynamicbalancing_off
                                                                }; -- rr
                                                              }; -- balancer2
                                                              refers = "requests_news_to_vla";
                                                            }; -- report
                                                          }; -- news_vla
                                                        }; -- rr
                                                        attempts_rate_limiter = {
                                                          limit = 0.200;
                                                          coeff = 0.990;
                                                          switch_default = true;
                                                        }; -- attempts_rate_limiter
                                                      }; -- balancer2
                                                    }; -- shared
                                                  }; -- default
                                                }; -- rr
                                                on_error = {
                                                  shared = {
                                                    uuid = "6280662600044536250";
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      rr = {
                                                        default = {
                                                          weight = 1.000;
                                                          shared = {
                                                            uuid = "go_to_tkva";
                                                          }; -- shared
                                                        }; -- default
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- shared
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- headers
                                          }; -- threshold
                                        }; -- request_replier
                                      }; -- shared
                                    }; -- default
                                  }; -- regexp
                                }; -- shared
                              }; -- report
                            }; -- default
                          }; -- regexp
                        }; -- shared
                      }; -- on_error
                    }; -- balancer2
                  }; -- shared
                }; -- headers
              }; -- report
            }; -- int_tkva_fetch
            default = {
              priority = 1;
              report = {
                uuid = "news";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
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
                  regexp = {
                    she_exp = {
                      priority = 2;
                      match_and = {
                        {
                          match_fsm = {
                            host = "(www\\.)?(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_fsm = {
                            URI = "/mirror.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_and
                      shared = {
                        uuid = "without-exp";
                      }; -- shared
                    }; -- she_exp
                    default = {
                      priority = 1;
                      shared = {
                        uuid = "without-exp";
                        request_replier = {
                          sink = {
                            shared = {
                              uuid = "2869946922261744982";
                            }; -- shared
                          }; -- sink
                          enable_failed_requests_replication = false;
                          rate = 0.000;
                          rate_file = "./controls/request_repl.ratefile";
                          regexp = {
                            bad_methods = {
                              priority = 3;
                              match_not = {
                                match_fsm = {
                                  match = "(POST|GET) .*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              }; -- match_not
                              errordocument = {
                                status = 405;
                                force_conn_close = false;
                              }; -- errordocument
                            }; -- bad_methods
                            tkva_exp = {
                              priority = 2;
                              match_fsm = {
                                header = {
                                  name = "X-Yandex-ExpBoxes";
                                  value = "(155613|.*;155613),.*";
                                }; -- header
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              shared = {
                                uuid = "rev_tkva";
                              }; -- shared
                            }; -- tkva_exp
                            default = {
                              priority = 1;
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  revtkva_off = {
                                    weight = 1.000;
                                    shared = {
                                      uuid = "normal-report-req";
                                    }; -- shared
                                  }; -- revtkva_off
                                  revtkva_on = {
                                    weight = -1.000;
                                    shared = {
                                      uuid = "rev_tkva";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          default = {
                                            weight = 1.000;
                                            shared = {
                                              uuid = "tkva_balancer";
                                            }; -- shared
                                          }; -- default
                                        }; -- rr
                                        on_error = {
                                          errordocument = {
                                            status = 404;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- revtkva_on
                                }; -- rr
                              }; -- balancer2
                            }; -- default
                          }; -- regexp
                        }; -- request_replier
                      }; -- shared
                    }; -- default
                  }; -- regexp
                }; -- geobase
              }; -- report
            }; -- default
          }; -- regexp
        }; -- shared
      }; -- http
    }; -- fake_section
    internal_section_80 = {
      ips = {
        "2a02:6b8:0:3400::2:38";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "14004065561096680";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15092, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15092, "/place/db/www/logs");
              report = {
                uuid = "internal-http";
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
                    ["X-Req-Id"] = "reqid";
                    ["X-Start-Time"] = "starttime";
                    ["X-Yandex-RandomUID"] = "yuid";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Forwarded-For-Y"] = "realip";
                    ["X-Source-Port-Y"] = "realport";
                  }; -- create_func_weak
                  log_headers = {
                    name_re = "X-Req-Id";
                    response_headers = {
                      create_weak = {
                        ["X-Content-Type-Options"] = "nosniff";
                        ["X-XSS-Protection"] = "1; mode=block";
                      }; -- create_weak
                      icookie = {
                        use_default_keys = true;
                        domains = ".yandex.az,.yandex.by,.yandex.co.il,.yandex.com,.yandex.com.am,.yandex.com.ge,.yandex.com.ua,.yandex.ee,.yandex.fr,.yandex.kg,.yandex.kz,.yandex.lt,.yandex.lv,.yandex.md,.yandex.ru,.yandex.tj,.yandex.tm,.yandex.uz,.yandex.ua";
                        trust_parent = false;
                        trust_children = false;
                        enable_set_cookie = true;
                        enable_decrypting = true;
                        decrypted_uid_header = "X-Yandex-ICookie";
                        error_header = "X-Yandex-ICookie-Error";
                        take_randomuid_from = "X-Yandex-RandomUID";
                        force_equal_to_yandexuid = true;
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
                          slb_ping = {
                            priority = 2;
                            match_fsm = {
                              URI = "/slb_ping";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            shared = {
                              uuid = "466145883168241203";
                              report = {
                                uuid = "slb_pings";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
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
                              }; -- report
                            }; -- shared
                          }; -- slb_ping
                          default = {
                            priority = 1;
                            shared = {
                              uuid = "knoss_exp_upstream";
                              report = {
                                uuid = "knoss_exp_checks";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                regexp = {
                                  she_exp = {
                                    priority = 3;
                                    match_and = {
                                      {
                                        match_fsm = {
                                          host = "(www\\.)?(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          URI = "/mirror.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_and
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
                                            uuid = "1241113434539155937";
                                            exp_getter = {
                                              trusted = false;
                                              file_switch = "./controls/expgetter.switch";
                                              service_name = "news-she";
                                              service_name_header = "Y-Service";
                                              uaas = {
                                                shared = {
                                                  uuid = "8988526946134060236";
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
                                                                  { "man1-0551-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:3372:10e:b563:0:43d1"; };
                                                                  { "man1-3722-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:37e8:10e:b563:0:43d1"; };
                                                                  { "man1-4352-a48-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:3cda:10e:b563:0:43d1"; };
                                                                  { "man1-4648-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:168e:10e:b563:0:43d1"; };
                                                                  { "man1-5661-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:deb:10e:b563:0:43d1"; };
                                                                  { "man1-6670-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:172:10e:b563:0:43d1"; };
                                                                  { "man1-7202-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:a16:10e:b563:0:43d1"; };
                                                                  { "man1-8284-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:315:10e:b563:0:43d1"; };
                                                                  { "man1-8314-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:301:10e:b563:0:43d1"; };
                                                                  { "man2-0395-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4415:10e:b563:0:43d1"; };
                                                                  { "man2-0510-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4d8c:10e:b563:0:43d1"; };
                                                                  { "man2-0584-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4105:10e:b563:0:43d1"; };
                                                                  { "man2-0971-af4-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:22a3:10e:b563:0:43d1"; };
                                                                  { "man2-1463-c9d-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:26a7:10e:b563:0:43d1"; };
                                                                  { "man2-1680-ca9-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:2617:10e:b563:0:43d1"; };
                                                                  { "man2-3519-d99-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4b02:10e:b563:0:43d1"; };
                                                                  { "man2-3535-57b-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:5989:10e:b563:0:43d1"; };
                                                                  { "man2-4159-92f-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:5720:10e:b563:0:43d1"; };
                                                                  { "man2-4167-a09-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:571a:10e:b563:0:43d1"; };
                                                                  { "man2-4667-250-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:1aa7:10e:b563:0:43d1"; };
                                                                  { "man2-4689-8c8-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:1b8c:10e:b563:0:43d1"; };
                                                                  { "man2-4806-07c-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:2786:10e:b563:0:43d1"; };
                                                                  { "man2-6550-5da-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2704:10e:b563:0:43d1"; };
                                                                  { "man2-6586-c86-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2987:10e:b563:0:43d1"; };
                                                                  { "man2-6943-60c-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2584:10e:b563:0:43d1"; };
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
                                                                  { "sas1-0322-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1092:10e:b566:0:43f7"; };
                                                                  { "sas1-0370-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:a03:10e:b566:0:43f7"; };
                                                                  { "sas1-0375-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:929:10e:b566:0:43f7"; };
                                                                  { "sas1-0730-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1405:10e:b566:0:43f7"; };
                                                                  { "sas1-1127-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:3515:10e:b566:0:43f7"; };
                                                                  { "sas1-1693-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:4810:10e:b566:0:43f7"; };
                                                                  { "sas1-1786-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:8e2f:10e:b566:0:43f7"; };
                                                                  { "sas1-2165-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:37a0:10e:b566:0:43f7"; };
                                                                  { "sas1-2335-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1d93:10e:b566:0:43f7"; };
                                                                  { "sas1-2491-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:162b:10e:b566:0:43f7"; };
                                                                  { "sas1-2511-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1804:10e:b566:0:43f7"; };
                                                                  { "sas1-2535-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:18a8:10e:b566:0:43f7"; };
                                                                  { "sas1-2607-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1603:10e:b566:0:43f7"; };
                                                                  { "sas1-2659-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1724:10e:b566:0:43f7"; };
                                                                  { "sas1-2769-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1812:10e:b566:0:43f7"; };
                                                                  { "sas1-2802-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:168d:10e:b566:0:43f7"; };
                                                                  { "sas1-4343-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:3487:10e:b566:0:43f7"; };
                                                                  { "sas1-4612-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:24af:10e:b566:0:43f7"; };
                                                                  { "sas1-4621-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:211a:10e:b566:0:43f7"; };
                                                                  { "sas1-4814-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:219f:10e:b566:0:43f7"; };
                                                                  { "sas1-4898-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:218a:10e:b566:0:43f7"; };
                                                                  { "sas1-4903-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:2a4:10e:b566:0:43f7"; };
                                                                  { "sas1-4906-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:930e:10e:b566:0:43f7"; };
                                                                  { "sas1-5003-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:4881:10e:b566:0:43f7"; };
                                                                  { "sas1-5414-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:2106:10e:b566:0:43f7"; };
                                                                  { "sas1-5538-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:369f:10e:b566:0:43f7"; };
                                                                  { "sas1-6006-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a29:10e:b566:0:43f7"; };
                                                                  { "sas1-7522-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:43af:10e:b566:0:43f7"; };
                                                                  { "sas1-9397-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:711e:10e:b566:0:43f7"; };
                                                                  { "sas1-9493-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:7115:10e:b566:0:43f7"; };
                                                                  { "sas2-0148-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c11:213:10e:b566:0:43f7"; };
                                                                  { "sas2-0528-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c11:e9a:10e:b566:0:43f7"; };
                                                                  { "sas2-1143-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:b621:10e:b566:0:43f7"; };
                                                                  { "sas2-3214-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:ed1c:10e:b566:0:43f7"; };
                                                                  { "sas2-4113-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:7584:10e:b566:0:43f7"; };
                                                                  { "sas2-4687-f96-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:692c:10e:b566:0:43f7"; };
                                                                  { "sas2-6078-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c14:439d:10e:b566:0:43f7"; };
                                                                  { "sas2-6514-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c16:31d:10e:b566:0:43f7"; };
                                                                  { "sas2-8852-7e7-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c16:1d9c:10e:b566:0:43f7"; };
                                                                  { "slovo012-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:72a2:10e:b566:0:43f7"; };
                                                                  { "slovo045-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6d8a:10e:b566:0:43f7"; };
                                                                  { "slovo055-5be-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6422:10e:b566:0:43f7"; };
                                                                  { "slovo080-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6b14:10e:b566:0:43f7"; };
                                                                  { "slovo103-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a8f:10e:b566:0:43f7"; };
                                                                  { "slovo126-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6b18:10e:b566:0:43f7"; };
                                                                  { "slovo143-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a9c:10e:b566:0:43f7"; };
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
                                                                  { "vla1-0141-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1f82:10e:b569:0:37d2"; };
                                                                  { "vla1-0299-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4c09:10e:b569:0:37d2"; };
                                                                  { "vla1-0487-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1a02:10e:b569:0:37d2"; };
                                                                  { "vla1-0606-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1391:10e:b569:0:37d2"; };
                                                                  { "vla1-0660-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:918:10e:b569:0:37d2"; };
                                                                  { "vla1-0724-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1e08:10e:b569:0:37d2"; };
                                                                  { "vla1-0732-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:29a2:10e:b569:0:37d2"; };
                                                                  { "vla1-0969-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:218c:10e:b569:0:37d2"; };
                                                                  { "vla1-1523-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:261b:10e:b569:0:37d2"; };
                                                                  { "vla1-1538-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2987:10e:b569:0:37d2"; };
                                                                  { "vla1-1560-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3492:10e:b569:0:37d2"; };
                                                                  { "vla1-1600-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:379d:10e:b569:0:37d2"; };
                                                                  { "vla1-1674-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3499:10e:b569:0:37d2"; };
                                                                  { "vla1-1776-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4084:10e:b569:0:37d2"; };
                                                                  { "vla1-1844-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2b9b:10e:b569:0:37d2"; };
                                                                  { "vla1-2047-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:349b:10e:b569:0:37d2"; };
                                                                  { "vla1-2051-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:261a:10e:b569:0:37d2"; };
                                                                  { "vla1-2083-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2a0d:10e:b569:0:37d2"; };
                                                                  { "vla1-2192-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:228e:10e:b569:0:37d2"; };
                                                                  { "vla1-2439-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a93:10e:b569:0:37d2"; };
                                                                  { "vla1-2467-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a01:10e:b569:0:37d2"; };
                                                                  { "vla1-2474-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a87:10e:b569:0:37d2"; };
                                                                  { "vla1-2482-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3c9a:10e:b569:0:37d2"; };
                                                                  { "vla1-2526-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a98:10e:b569:0:37d2"; };
                                                                  { "vla1-3220-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1912:10e:b569:0:37d2"; };
                                                                  { "vla1-3454-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:c90:10e:b569:0:37d2"; };
                                                                  { "vla1-3715-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:5084:10e:b569:0:37d2"; };
                                                                  { "vla1-3819-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0f:1d89:10e:b569:0:37d2"; };
                                                                  { "vla1-3876-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4302:10e:b569:0:37d2"; };
                                                                  { "vla1-4007-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4817:10e:b569:0:37d2"; };
                                                                  { "vla1-4362-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:439d:10e:b569:0:37d2"; };
                                                                  { "vla1-4408-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:431a:10e:b569:0:37d2"; };
                                                                  { "vla1-4580-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3b96:10e:b569:0:37d2"; };
                                                                  { "vla1-5539-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0f:1e10:10e:b569:0:37d2"; };
                                                                  { "vla2-1001-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:498:10e:b569:0:37d2"; };
                                                                  { "vla2-1003-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:49a:10e:b569:0:37d2"; };
                                                                  { "vla2-1008-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:49d:10e:b569:0:37d2"; };
                                                                  { "vla2-1015-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1ba1:10e:b569:0:37d2"; };
                                                                  { "vla2-1017-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1b83:10e:b569:0:37d2"; };
                                                                  { "vla2-1019-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:48a:10e:b569:0:37d2"; };
                                                                  { "vla2-1067-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1ba2:10e:b569:0:37d2"; };
                                                                  { "vla2-1071-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1b9f:10e:b569:0:37d2"; };
                                                                  { "vla2-5945-62c-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:620:10e:b569:0:37d2"; };
                                                                  { "vla2-5963-9a4-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:612:10e:b569:0:37d2"; };
                                                                  { "vla2-7970-d06-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:398d:10e:b569:0:37d2"; };
                                                                  { "vla2-7992-190-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:1422:10e:b569:0:37d2"; };
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
                                                                { "uaas.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::2:48"; };
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
                                                }; -- shared
                                              }; -- uaas
                                              shared = {
                                                uuid = "196154789330579942";
                                                shared = {
                                                  uuid = "knoss_after_expgetter";
                                                }; -- shared
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
                                            uuid = "1241113434539155937";
                                          }; -- shared
                                        }; -- headers
                                      }; -- default
                                    }; -- regexp
                                  }; -- she_exp
                                  sport_exp = {
                                    priority = 2;
                                    match_or = {
                                      {
                                        match_fsm = {
                                          host = "(.+\\.)?yandexsport\\..*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          host = "sportyandex\\..*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_and = {
                                          {
                                            match_fsm = {
                                              host = "(www\\.)?(m\\.)?yandex\\.(az|by|co\\.il|com|com\\.am|com\\.ge|com\\.ua|ee|fr|kg|kz|lt|lv|md|ru|tj|tm|uz|ua)";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              path = "/sport.*";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                        }; -- match_and
                                      };
                                    }; -- match_or
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
                                            uuid = "3125602454098319159";
                                            exp_getter = {
                                              trusted = false;
                                              file_switch = "./controls/expgetter.switch";
                                              service_name = "news-sport";
                                              service_name_header = "Y-Service";
                                              uaas = {
                                                shared = {
                                                  uuid = "8988526946134060236";
                                                }; -- shared
                                              }; -- uaas
                                              shared = {
                                                uuid = "196154789330579942";
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
                                            uuid = "3125602454098319159";
                                          }; -- shared
                                        }; -- headers
                                      }; -- default
                                    }; -- regexp
                                  }; -- sport_exp
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
                                            uuid = "8403558470811605423";
                                            exp_getter = {
                                              trusted = false;
                                              file_switch = "./controls/expgetter.switch";
                                              service_name = "news";
                                              service_name_header = "Y-Service";
                                              uaas = {
                                                shared = {
                                                  uuid = "8988526946134060236";
                                                }; -- shared
                                              }; -- uaas
                                              shared = {
                                                uuid = "knoss_after_expgetter";
                                                regexp = {
                                                  knoss_balancer_exp = {
                                                    priority = 2;
                                                    match_fsm = {
                                                      header = {
                                                        name = "X-Yandex-ExpBoxes";
                                                        value = "(166759|.*;166759),.*";
                                                      }; -- header
                                                      case_insensitive = true;
                                                      surround = false;
                                                    }; -- match_fsm
                                                    report = {
                                                      uuid = "requests_to_knoss_balancer";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      threshold = {
                                                        lo_bytes = 512;
                                                        hi_bytes = 1024;
                                                        recv_timeout = "1s";
                                                        pass_timeout = "2s";
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 1;
                                                          rr = {
                                                            default = {
                                                              weight = 1.000;
                                                              balancer2 = {
                                                                watermark_policy = {
                                                                  lo = 0.500;
                                                                  hi = 0.600;
                                                                  params_file = "./controls/watermark_policy.params_file";
                                                                  unique_policy = {};
                                                                }; -- watermark_policy
                                                                attempts = 2;
                                                                attempts_file = "./controls/news.attempts";
                                                                rr = {
                                                                  weights_file = "./controls/traffic_control.weights";
                                                                  news_sas = {
                                                                    weight = 1.000;
                                                                    balancer2 = {
                                                                      timeout_policy = {
                                                                        timeout = "500ms";
                                                                        unique_policy = {};
                                                                      }; -- timeout_policy
                                                                      attempts = 1;
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
                                                                          { "bfupni35xqi5agsg.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:10a:100:0:2027:0"; };
                                                                          { "ddiqsnhkavgq3gi5.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:186:100:0:b26d:0"; };
                                                                          { "df6tg6536uoib7gp.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:92:100:0:6206:0"; };
                                                                          { "do2eoyfdrnpcgbns.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:c584:100:0:2a3:0"; };
                                                                          { "ehbeszqbizk4sfax.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:3e9f:100:0:bb6f:0"; };
                                                                          { "epxtjotrbvcqq2we.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:11d:100:0:f950:0"; };
                                                                          { "ghjbtic7r6o5qraq.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:204:100:0:9f43:0"; };
                                                                          { "gpenrmwz4m6zfexs.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:94:100:0:eac4:0"; };
                                                                          { "jnxrlha5pk4ctjys.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:3812:100:0:2812:0"; };
                                                                          { "joyp4wwekqzltb2x.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:301:100:0:f1c4:0"; };
                                                                          { "kkxfselzu7vaoogv.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:249e:100:0:2f6e:0"; };
                                                                          { "lw5yhn3y3mxyonla.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:3:100:0:6b9f:0"; };
                                                                          { "nebw76yxk2yypq7b.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:1d:100:0:3dde:0"; };
                                                                          { "pxaejrbaou7gtn4k.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:30e:100:0:356d:0"; };
                                                                          { "so6q6ys6rewarfdz.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:30f:100:0:c29c:0"; };
                                                                          { "sui33ofennpd4mr3.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:3814:100:0:2eef:0"; };
                                                                          { "uvuuttvjhyqmd5cf.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:197:100:0:a5ea:0"; };
                                                                          { "wck7eknps6xlj77g.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:2716:100:0:7c0a:0"; };
                                                                          { "ykil62m6a6gi2emw.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:8f:0:46a9:8592:0"; };
                                                                          { "ylpjmzz6dgoapin5.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:321c:0:46a9:56b4:0"; };
                                                                        }, {
                                                                          resolve_timeout = "10ms";
                                                                          connect_timeout = "50ms";
                                                                          backend_timeout = "5000ms";
                                                                          fail_on_5xx = true;
                                                                          http_backend = true;
                                                                          buffering = false;
                                                                          keepalive_count = 0;
                                                                          need_resolve = true;
                                                                          status_code_blacklist = {
                                                                            "204";
                                                                          }; -- status_code_blacklist
                                                                        }))
                                                                      }; -- weighted2
                                                                    }; -- balancer2
                                                                  }; -- news_sas
                                                                  news_man = {
                                                                    weight = 1.000;
                                                                    balancer2 = {
                                                                      timeout_policy = {
                                                                        timeout = "500ms";
                                                                        unique_policy = {};
                                                                      }; -- timeout_policy
                                                                      attempts = 1;
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
                                                                          { "aym53wfqpmatzoap.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38a0:100:0:3a43:0"; };
                                                                          { "eupsiuyep4v46mt3.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:31c1:100:0:cfb0:0"; };
                                                                          { "g3fyu7koug5aunrv.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:31a0:100:0:da93:0"; };
                                                                          { "g6ibv7jhmeyhlkyw.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3eaa:100:0:a620:0"; };
                                                                          { "jmdtg57yp7tblanj.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:a2a:0:46a9:c24a:0"; };
                                                                          { "lalbx4ailvhc7id7.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38af:100:0:919:0"; };
                                                                          { "msqeqds2eyi336kf.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3c21:100:0:6f86:0"; };
                                                                          { "orxslf5tqnfqvwik.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38bb:100:0:eced:0"; };
                                                                          { "pfctsfq6tkaaljuc.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3c2e:100:0:354e:0"; };
                                                                          { "rwdz7iuqldinpu2i.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3e37:100:0:722e:0"; };
                                                                          { "rxlela6pwi6s3oby.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:38c2:100:0:b963:0"; };
                                                                          { "t4mg6s34gxzvjjts.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:2fb4:100:0:ea40:0"; };
                                                                          { "tdv2tw6uxc3oi3l3.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3b25:100:0:37bf:0"; };
                                                                          { "tjelbiqy26el673z.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:35ae:100:0:d0f4:0"; };
                                                                          { "tqmvdvjtr5r5ewis.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3c3b:100:0:5c5d:0"; };
                                                                          { "uwukk3webxdkiwfy.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:262e:100:0:f8ed:0"; };
                                                                          { "vpmgk5intodvda46.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:363a:100:0:7440:0"; };
                                                                          { "vyy7ydw4q5k5vfal.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:32bd:100:0:60a1:0"; };
                                                                          { "wkhp7csxyomlisru.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:31bd:100:0:c755:0"; };
                                                                          { "wpcpazrfis7fyrcw.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3533:100:0:fc1b:0"; };
                                                                        }, {
                                                                          resolve_timeout = "10ms";
                                                                          connect_timeout = "50ms";
                                                                          backend_timeout = "5000ms";
                                                                          fail_on_5xx = true;
                                                                          http_backend = true;
                                                                          buffering = false;
                                                                          keepalive_count = 0;
                                                                          need_resolve = true;
                                                                          status_code_blacklist = {
                                                                            "204";
                                                                          }; -- status_code_blacklist
                                                                        }))
                                                                      }; -- weighted2
                                                                    }; -- balancer2
                                                                  }; -- news_man
                                                                  news_vla = {
                                                                    weight = 1.000;
                                                                    balancer2 = {
                                                                      timeout_policy = {
                                                                        timeout = "500ms";
                                                                        unique_policy = {};
                                                                      }; -- timeout_policy
                                                                      attempts = 1;
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
                                                                          { "aeeake35s6zdsgj2.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c17:100:0:a312:0"; };
                                                                          { "anpbys2v63zob57y.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:319d:100:0:4060:0"; };
                                                                          { "bwohgic7so2ybo3k.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:3006:100:0:6435:0"; };
                                                                          { "ctnpgupqclv3dqxb.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a03:100:0:7edb:0"; };
                                                                          { "etomwsjkhy5ym5ik.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0f:1803:100:0:89f6:0"; };
                                                                          { "fbmphd7l4b6x2tjm.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:321e:100:0:c3b1:0"; };
                                                                          { "hrnjery3piamxjpd.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:301a:100:0:e2a5:0"; };
                                                                          { "jv32shj4cuusis6r.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a15:100:0:5619:0"; };
                                                                          { "k5pecfb3zow45hbz.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2315:100:0:1734:0"; };
                                                                          { "l4jgwz3jd3awemkj.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a05:100:0:fa07:0"; };
                                                                          { "lgj2eqcidfi6nlfj.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:378d:100:0:61d:0"; };
                                                                          { "m6wc7xbvle7nq7xp.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:398b:100:0:9bf0:0"; };
                                                                          { "mflts7kcuu5pjqww.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c17:8a0:100:0:cad5:0"; };
                                                                          { "psnwd53pacps5bqh.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2a95:100:0:e11:0"; };
                                                                          { "qrh5kwb767fyjm6y.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:3002:100:0:50:0"; };
                                                                          { "sfztpmoj5tet3dzg.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:1319:100:0:4a82:0"; };
                                                                          { "ulxv2exnqfn4ohot.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:371e:100:0:f88b:0"; };
                                                                          { "woj7zsllyggh563l.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:301d:100:0:3ba:0"; };
                                                                          { "yh3bqmq4tupmef7p.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:3812:100:0:ae8c:0"; };
                                                                          { "zog4nv7qsiusksju.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:2c8e:100:0:ab7f:0"; };
                                                                        }, {
                                                                          resolve_timeout = "10ms";
                                                                          connect_timeout = "50ms";
                                                                          backend_timeout = "5000ms";
                                                                          fail_on_5xx = true;
                                                                          http_backend = true;
                                                                          buffering = false;
                                                                          keepalive_count = 0;
                                                                          need_resolve = true;
                                                                          status_code_blacklist = {
                                                                            "204";
                                                                          }; -- status_code_blacklist
                                                                        }))
                                                                      }; -- weighted2
                                                                    }; -- balancer2
                                                                  }; -- news_vla
                                                                }; -- rr
                                                              }; -- balancer2
                                                            }; -- default
                                                          }; -- rr
                                                          on_error = {
                                                            shared = {
                                                              uuid = "6280662600044536250";
                                                            }; -- shared
                                                          }; -- on_error
                                                        }; -- balancer2
                                                      }; -- threshold
                                                    }; -- report
                                                  }; -- knoss_balancer_exp
                                                  default = {
                                                    priority = 1;
                                                    shared = {
                                                      uuid = "ext_upstreams";
                                                    }; -- shared
                                                  }; -- default
                                                }; -- regexp
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
                                            uuid = "8403558470811605423";
                                          }; -- shared
                                        }; -- headers
                                      }; -- default
                                    }; -- regexp
                                  }; -- default
                                }; -- regexp
                              }; -- report
                            }; -- shared
                          }; -- default
                        }; -- regexp
                      }; -- icookie
                    }; -- response_headers
                  }; -- log_headers
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- internal_section_80
    internal_section_15090 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15090;
      }; -- ports
      shared = {
        uuid = "14004065561096680";
      }; -- shared
    }; -- internal_section_15090
  }; -- ipdispatch
}