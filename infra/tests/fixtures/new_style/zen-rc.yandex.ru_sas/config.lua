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
  pinger_required = true;
  tcp_listen_queue = 128;
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
  log = get_log_path("childs_log", get_port_var("port"), "/place/db/www/logs");
  pinger_log = get_log_path("pinger_log", get_port_var("port"), "/place/db/www/logs/");
  config_check = {
    quorums_file = "./controls/backend_check_quorums";
  }; -- config_check
  dynamic_balancing_log = get_log_path("dynamic_balancing_log", get_port_var("port"), "/place/db/www/logs/");
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
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
  }; -- unistat
  cpu_limiter = {
    active_check_subnet_default = true;
    disable_file = "./controls/cpu_limiter_disabled";
    active_check_subnet_file = "./controls/active_check_subnets_list";
  }; -- cpu_limiter
  sd = {
    client_name = "awacs-l7-balancer(zen-rc.yandex.ru:zen-rc.yandex.ru_sas)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  addrs = {
    {
      ip = "127.0.0.4";
      port = get_port_var("port");
    };
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 443;
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
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
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
                uuid = "5676164135556292882";
                cookie_policy = {
                  uuid = "service_total";
                  default_yandex_policies = "stable";
                  log_headers = {
                    name_re = "(Cookie|User-Agent)";
                    log_headers = {
                      response_name_re = "X-Requestid";
                      regexp = {
                        ["awacs-balancer-health-check"] = {
                          priority = 22;
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
                          priority = 21;
                          match_fsm = {
                            url = "/ok.html";
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
                                    status = 404;
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- switch_off
                              }; -- rr
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- slbping
                        adservices = {
                          priority = 20;
                          match_fsm = {
                            host = "([a-z_A-Z]*\\.)?zenadservices\\.net";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-Zen-Host"] = "host";
                            }; -- create_func
                            create = {
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
                            report = {
                              uuid = "to-adservices";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              shared = {
                                uuid = "1550938685721033544";
                                balancer2 = {
                                  retry_policy = {
                                    unique_policy = {};
                                  }; -- retry_policy
                                  attempts = 1;
                                  connection_attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "zen-static.kaizen.yandex.net"; 81; 1.000; "2a02:6b8:0:3400:0:3c7:0:1"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "40ms";
                                      backend_timeout = "60s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = false;
                                    }))
                                  }; -- rr
                                }; -- balancer2
                              }; -- shared
                            }; -- report
                          }; -- headers
                        }; -- adservices
                        redirects = {
                          priority = 19;
                          match_fsm = {
                            host = "((zen\\.ya\\.ru)|(dzen\\.ya\\.ru))";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "redirects";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            response_headers = {
                              create = {
                                Location = "https://zen.yandex.ru/";
                              }; -- create
                              errordocument = {
                                status = 301;
                                force_conn_close = false;
                                remain_headers = "Location";
                              }; -- errordocument
                            }; -- response_headers
                          }; -- report
                        }; -- redirects
                        static = {
                          priority = 18;
                          match_or = {
                            {
                              match_fsm = {
                                URI = "/yabro/service-worker\\.js";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/\\.well-known/assetlinks.json";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/\\.well-known/apple-app-site-association";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/static/.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/static-internal/.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/media/sitemaps/.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/t/.+\\.xml";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/robots.txt";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/nearest[0-9]+\\.js";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/zenkit/.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/.*\\.(html|png|ico|gif)";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                          }; -- match_or
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-Zen-Host"] = "host";
                            }; -- create_func
                            create = {
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
                            report = {
                              uuid = "to-front-static";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              shared = {
                                uuid = "1550938685721033544";
                              }; -- shared
                            }; -- report
                          }; -- headers
                        }; -- static
                        ["antirobot-cryproxy"] = {
                          priority = 17;
                          match_and = {
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    URI = "/t/.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/adv/?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/media/.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/m/.+";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/profile/editor/.+";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                            {
                              match_or = {
                                {
                                  match_and = {
                                    {
                                      match_method = {
                                        methods = { "post"; };
                                      }; -- match_method
                                    };
                                    {
                                      match_fsm = {
                                        header = {
                                          name = "X-AAB-HTTP-CHECK";
                                          value = ".*";
                                        }; -- header
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                  }; -- match_and
                                };
                                {
                                  match_and = {
                                    {
                                      match_method = {
                                        methods = { "post"; "get"; };
                                      }; -- match_method
                                    };
                                    {
                                      match_not = {
                                        match_fsm = {
                                          header = {
                                            name = "X-AAB-PROXY";
                                            value = "1";
                                          }; -- header
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      }; -- match_not
                                    };
                                    {
                                      match_or = {
                                        {
                                          match_fsm = {
                                            cookie = "bltsr=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "cycada=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "qgZTpupNMGJBM=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "mcBaGDt=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "BgeeyNoBJuyII=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "orrXTfJaS=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "FgkKdCjPqoMFm=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "EIXtkCTlX=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "JPIqApiY=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "KIykI=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "HgGedof=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "ancQTZw=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "involved=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "instruction=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "engineering=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "telecommunications=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "discussion=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "computer=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "substantial=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "specific=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "engineer=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "adequate=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Silver=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Mercury=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Bismuth=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Silicon=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Tennessine=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Zinc=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Sulfur=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Nickel=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Radon=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "Manganese=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "LBCBNrZSu=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "VTouhmwR=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "TbwgcPzRMgzVo=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "liPkbtFdIkYqc=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "HOhdORSx=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "EMCzniGaQ=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "PIwsfZeu=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "FxuGQqNNo=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "sMLIIeQQeFnYt=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "pClnKCSBXcHUp=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "tCTmkfFoXn=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "zmFQeXtI=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "ScSvCIlBC=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "kNAcVGYFWhx=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "jsOmqPGh=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "OqYspIFcUpLY=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "XcfPaDInQpzKj=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "hcxWnzbUzfz=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "MGphYZof=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_fsm = {
                                            cookie = "NBgfDVFir=1";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                      }; -- match_or
                                    };
                                  }; -- match_and
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Forwarded-Zen-Host"] = "host";
                              ["X-Host-Y"] = "host";
                              ["X-Real-IP"] = "realip";
                              ["X-TLS-Cipher-Y"] = "ja3";
                            }; -- create_func
                            create = {
                              ["X-AAB-PartnerToken"] = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBQUIiLCJpYXQiOjE1MjE1NDM3OTYsInN1YiI6Inplbi55YW5kZXgucnUiLCJleHAiOjE1NTMwNzk3OTZ9.cMvobzu1UXxGOyHB3X01MGsQGiXYyJCzwRlBGyoHa9sdeUy8Z-I4g5614YOu4ZsXbNe1Sd0EWo7eBuyW5k3Cy6nLeSTklmYNGvZstwLKpmf11-0zdB8sgbJL4VNbBJrhDJYdNcnPvUB3X2ttyIVGTUJ2xrYx6CFmed_Y0bIbq3XkoWXX7x_HIenHq4aS-YXtNNhCF_T51UXtBfzCtxRmUJykizZlj9IpruAjIzqB0GgpQ7A9EoGbHgetUpx1MrC5k8T3AIu0oi64iyTKAcpHc-NbEjxr0ZMqYKeTJOkMEDjjpxQSGkcJB5k9D2GG9m1AGS23dVIukaR0GhS2-Ao2BA";
                              ["X-Antirobot-Service-Y"] = "zen";
                              ["X-Forwarded-Proto"] = "https";
                              ["X-Yandex-HTTPS"] = "yes";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
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
                                      response_headers_if = {
                                        matcher = {
                                          match_and = {
                                            {
                                              match_header = {
                                                name = "Access-Control-Allow-Origin";
                                                value = ".+";
                                              }; -- match_header
                                            };
                                            {
                                              match_not = {
                                                match_header = {
                                                  name = "Access-Control-Allow-Origin";
                                                  value = "https://(.*\\.)?yandex\\.(ru|ua|kz|by|kg|lt|lv|md|tj|tm|uz|ee|az|fr|com|com\\.tr|com\\.am|com\\.ge|co\\.il)";
                                                }; -- match_header
                                              }; -- match_not
                                            };
                                          }; -- match_and
                                        }; -- matcher
                                        delete_header = "Access-Control-Allow-Origin|Access-Control-Allow-Credentials|Access-Control-Allow-Methods|Access-Control-Allow-Headers|Access-Control-Allow-Age|Vary";
                                        headers_forwarder = {
                                          actions = {
                                            {
                                              request_header = "Origin";
                                              response_header = "Access-Control-Allow-Origin";
                                              erase_from_request = false;
                                              erase_from_response = true;
                                              weak = false;
                                            };
                                          }; -- actions
                                          response_headers = {
                                            create = {
                                              ["Access-Control-Allow-Age"] = "1728000";
                                              ["Access-Control-Allow-Credentials"] = "true";
                                              ["Access-Control-Allow-Headers"] = "Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,X-AAB-HTTP-Check,X-AAB-JSTracer,Yandex-Preload";
                                              ["Access-Control-Allow-Methods"] = "GET, POST, PUT, OPTIONS, HEAD";
                                              Vary = "Origin,Referer,Accept-Encoding";
                                            }; -- create
                                            report = {
                                              uuid = "to-cryprox";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              shared = {
                                                uuid = "6290711570607315659";
                                              }; -- shared
                                            }; -- report
                                          }; -- response_headers
                                        }; -- headers_forwarder
                                      }; -- response_headers_if
                                    }; -- module
                                  }; -- antirobot
                                }; -- cutter
                              }; -- h100
                            }; -- hasher
                          }; -- headers
                        }; -- ["antirobot-cryproxy"]
                        ["antirobot-front"] = {
                          priority = 16;
                          match_and = {
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    URI = "/api/v3/launcher/(subscribers|preferences|suggest)";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/api/v3/launcher/social/(activity-feed|profile)";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Forwarded-Zen-Host"] = "host";
                              ["X-Host-Y"] = "host";
                              ["X-Real-IP"] = "realip";
                              ["X-TLS-Cipher-Y"] = "ja3";
                            }; -- create_func
                            create = {
                              ["X-Antirobot-Service-Y"] = "zen";
                              ["X-Forwarded-Proto"] = "https";
                              ["X-Yandex-HTTPS"] = "yes";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
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
                                      report = {
                                        uuid = "to-front";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        shared = {
                                          uuid = "6561638647756991584";
                                        }; -- shared
                                      }; -- report
                                    }; -- module
                                  }; -- antirobot
                                }; -- cutter
                              }; -- h100
                            }; -- hasher
                          }; -- headers
                        }; -- ["antirobot-front"]
                        ["antirobot-pub"] = {
                          priority = 15;
                          match_and = {
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    URI = "/adv/?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/media/.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/m/.+";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/profile/editor/.+";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                          headers = {
                            create_func = {
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Forwarded-Zen-Host"] = "host";
                              ["X-Host-Y"] = "host";
                              ["X-Original-Host"] = "host";
                              ["X-Real-IP"] = "realip";
                              ["X-TLS-Cipher-Y"] = "ja3";
                              ["X-Zen-Original-Host"] = "host";
                            }; -- create_func
                            create = {
                              Host = "default.publishers.zeta.kaizen.yandex.ru";
                              ["X-Antirobot-Service-Y"] = "zen";
                              ["X-Forwarded-Proto"] = "https";
                              ["X-Yandex-HTTPS"] = "yes";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
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
                                      report = {
                                        uuid = "to-pub";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 1;
                                          connection_attempts = 2;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "default.publishers.zeta.kaizen.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:1:8336"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "300s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- report
                                    }; -- module
                                  }; -- antirobot
                                }; -- cutter
                              }; -- h100
                            }; -- hasher
                          }; -- headers
                        }; -- ["antirobot-pub"]
                        ["antirobot-rezen"] = {
                          priority = 14;
                          match_and = {
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    URI = "/(login|user(/[0-9a-zA-Z\\.-]+)?(/subscriptions|/followers|/edit|/interests-feedback|/deactivate)?(/)?)";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/pages.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/channel/.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    URI = "/t/.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Forwarded-Zen-Host"] = "host";
                              ["X-Host-Y"] = "host";
                              ["X-Real-IP"] = "realip";
                              ["X-TLS-Cipher-Y"] = "ja3";
                            }; -- create_func
                            create = {
                              ["X-Antirobot-Service-Y"] = "zen";
                              ["X-Forwarded-Proto"] = "https";
                              ["X-Yandex-HTTPS"] = "yes";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
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
                                      report = {
                                        uuid = "to-rezen";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        shared = {
                                          uuid = "6561638647756991584";
                                        }; -- shared
                                      }; -- report
                                    }; -- module
                                  }; -- antirobot
                                }; -- cutter
                              }; -- h100
                            }; -- hasher
                          }; -- headers
                        }; -- ["antirobot-rezen"]
                        cryproxy = {
                          priority = 13;
                          match_or = {
                            {
                              match_fsm = {
                                URI = "/lz5XeGt8f/.+";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                URI = "/static-lib/s3/zen-lib/.+\\.(xml|js|jpg|png|css|html|otf|eot|svg|ttfs)";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_and = {
                                {
                                  match_or = {
                                    {
                                      match_fsm = {
                                        URI = "/";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/t/.*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/api/v3/launcher/(export|more|similar|export-cached)";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/adv/?";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/media/.*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/m/.+";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                    {
                                      match_fsm = {
                                        URI = "/profile/editor/.+";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    };
                                  }; -- match_or
                                };
                                {
                                  match_or = {
                                    {
                                      match_and = {
                                        {
                                          match_method = {
                                            methods = { "post"; };
                                          }; -- match_method
                                        };
                                        {
                                          match_fsm = {
                                            header = {
                                              name = "X-AAB-HTTP-CHECK";
                                              value = ".*";
                                            }; -- header
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                      }; -- match_and
                                    };
                                    {
                                      match_and = {
                                        {
                                          match_method = {
                                            methods = { "post"; "get"; };
                                          }; -- match_method
                                        };
                                        {
                                          match_not = {
                                            match_fsm = {
                                              header = {
                                                name = "X-AAB-PROXY";
                                                value = "1";
                                              }; -- header
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          }; -- match_not
                                        };
                                        {
                                          match_or = {
                                            {
                                              match_fsm = {
                                                cookie = "bltsr=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "cycada=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "qgZTpupNMGJBM=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "mcBaGDt=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "BgeeyNoBJuyII=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "orrXTfJaS=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "FgkKdCjPqoMFm=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "EIXtkCTlX=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "JPIqApiY=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "KIykI=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "HgGedof=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "ancQTZw=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "involved=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "instruction=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "engineering=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "telecommunications=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "discussion=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "computer=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "substantial=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "specific=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "engineer=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "adequate=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Silver=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Mercury=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Bismuth=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Silicon=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Tennessine=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Zinc=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Sulfur=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Nickel=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Radon=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "Manganese=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "LBCBNrZSu=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "VTouhmwR=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "TbwgcPzRMgzVo=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "liPkbtFdIkYqc=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "HOhdORSx=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "EMCzniGaQ=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "PIwsfZeu=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "FxuGQqNNo=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "sMLIIeQQeFnYt=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "pClnKCSBXcHUp=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "tCTmkfFoXn=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "zmFQeXtI=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "ScSvCIlBC=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "kNAcVGYFWhx=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "jsOmqPGh=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "OqYspIFcUpLY=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "XcfPaDInQpzKj=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "hcxWnzbUzfz=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "MGphYZof=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                cookie = "NBgfDVFir=1";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                          }; -- match_or
                                        };
                                      }; -- match_and
                                    };
                                  }; -- match_or
                                };
                              }; -- match_and
                            };
                          }; -- match_or
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-Zen-Host"] = "host";
                            }; -- create_func
                            create = {
                              ["X-AAB-PartnerToken"] = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBQUIiLCJpYXQiOjE1MjE1NDM3OTYsInN1YiI6Inplbi55YW5kZXgucnUiLCJleHAiOjE1NTMwNzk3OTZ9.cMvobzu1UXxGOyHB3X01MGsQGiXYyJCzwRlBGyoHa9sdeUy8Z-I4g5614YOu4ZsXbNe1Sd0EWo7eBuyW5k3Cy6nLeSTklmYNGvZstwLKpmf11-0zdB8sgbJL4VNbBJrhDJYdNcnPvUB3X2ttyIVGTUJ2xrYx6CFmed_Y0bIbq3XkoWXX7x_HIenHq4aS-YXtNNhCF_T51UXtBfzCtxRmUJykizZlj9IpruAjIzqB0GgpQ7A9EoGbHgetUpx1MrC5k8T3AIu0oi64iyTKAcpHc-NbEjxr0ZMqYKeTJOkMEDjjpxQSGkcJB5k9D2GG9m1AGS23dVIukaR0GhS2-Ao2BA";
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
                            response_headers_if = {
                              matcher = {
                                match_and = {
                                  {
                                    match_header = {
                                      name = "Access-Control-Allow-Origin";
                                      value = ".+";
                                    }; -- match_header
                                  };
                                  {
                                    match_not = {
                                      match_header = {
                                        name = "Access-Control-Allow-Origin";
                                        value = "https://(.*\\.)?yandex\\.(ru|ua|kz|by|kg|lt|lv|md|tj|tm|uz|ee|az|fr|com|com\\.tr|com\\.am|com\\.ge|co\\.il)";
                                      }; -- match_header
                                    }; -- match_not
                                  };
                                }; -- match_and
                              }; -- matcher
                              delete_header = "Access-Control-Allow-Origin|Access-Control-Allow-Credentials|Access-Control-Allow-Methods|Access-Control-Allow-Headers|Access-Control-Allow-Age|Vary";
                              headers_forwarder = {
                                actions = {
                                  {
                                    request_header = "Origin";
                                    response_header = "Access-Control-Allow-Origin";
                                    erase_from_request = false;
                                    erase_from_response = true;
                                    weak = false;
                                  };
                                }; -- actions
                                response_headers = {
                                  create = {
                                    ["Access-Control-Allow-Age"] = "1728000";
                                    ["Access-Control-Allow-Credentials"] = "true";
                                    ["Access-Control-Allow-Headers"] = "Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,X-AAB-HTTP-Check,X-AAB-JSTracer,Yandex-Preload";
                                    ["Access-Control-Allow-Methods"] = "GET, POST, PUT, OPTIONS, HEAD";
                                    Vary = "Origin,Referer,Accept-Encoding";
                                  }; -- create
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    matcher_map = {
                                      export = {
                                        match_fsm = {
                                          URI = "/(.*)/export";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      }; -- export
                                      export_cached = {
                                        match_fsm = {
                                          URI = "/(.*)/export-cached";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      }; -- export_cached
                                      more = {
                                        match_fsm = {
                                          URI = "/(.*)/more";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      }; -- more
                                      layout = {
                                        match_fsm = {
                                          URI = "/(.*)/layout";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      }; -- layout
                                    }; -- matcher_map
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    rewrite = {
                                      actions = {
                                        {
                                          split = "url";
                                          global = false;
                                          literal = false;
                                          rewrite = "/static/%1";
                                          case_insensitive = false;
                                          regexp = "/static-lib/(.*)";
                                        };
                                      }; -- actions
                                      shared = {
                                        uuid = "6290711570607315659";
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 1;
                                          connection_attempts = 2;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "cryprox-test.yandex.net"; 80; 1.000; "2a02:6b8::197"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "60s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 128;
                                              need_resolve = false;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- shared
                                    }; -- rewrite
                                    refers = "to-cryprox";
                                  }; -- report
                                }; -- response_headers
                              }; -- headers_forwarder
                            }; -- response_headers_if
                          }; -- headers
                        }; -- cryproxy
                        landing = {
                          priority = 12;
                          match_or = {
                            {
                              match_fsm = {
                                URI = "/about";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                          }; -- match_or
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-Zen-Host"] = "host";
                            }; -- create_func
                            create = {
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
                            response_headers = {
                              create = {
                                Location = "https://zen.yandex/about";
                              }; -- create
                              report = {
                                uuid = "to-landing";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                errordocument = {
                                  status = 301;
                                  force_conn_close = false;
                                  remain_headers = "Location";
                                }; -- errordocument
                              }; -- report
                            }; -- response_headers
                          }; -- headers
                        }; -- landing
                        captcha = {
                          priority = 11;
                          match_fsm = {
                            URI = "/(captcha.+|showcaptcha|checkcaptcha)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "to-captcha";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            headers = {
                              create_func = {
                                ["X-Forwarded-For-Y"] = "realip";
                                ["X-Host-Y"] = "host";
                                ["X-Real-IP"] = "realip";
                                ["X-TLS-Cipher-Y"] = "ja3";
                              }; -- create_func
                              create = {
                                ["X-Antirobot-Service-Y"] = "zen";
                                ["X-Yandex-HTTPS"] = "yes";
                              }; -- create
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                connection_attempts = 2;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "antirobot.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::121"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "40ms";
                                    backend_timeout = "60s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- rr
                                attempts_rate_limiter = {
                                  limit = 0.200;
                                  coeff = 0.990;
                                  switch_default = true;
                                }; -- attempts_rate_limiter
                              }; -- balancer2
                            }; -- headers
                          }; -- report
                        }; -- captcha
                        pub = {
                          priority = 10;
                          match_fsm = {
                            URI = "/((media-api/.+)|(editor-api/.+)|(media-api-video/.+)|media)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            headers = {
                              create_func = {
                                ["X-Forwarded-Zen-Host"] = "host";
                                ["X-Original-Host"] = "host";
                                ["X-Zen-Original-Host"] = "host";
                              }; -- create_func
                              create = {
                                Host = "default.publishers.zeta.kaizen.yandex.ru";
                                ["X-Forwarded-Proto"] = "https";
                              }; -- create
                              append_func = {
                                ["X-Forwarded-For"] = "realip";
                                ["Zen-Forwarded-For"] = "realip";
                              }; -- append_func
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                connection_attempts = 2;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "default.publishers.zeta.kaizen.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:1:8336"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "40ms";
                                    backend_timeout = "300s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- rr
                                attempts_rate_limiter = {
                                  limit = 0.200;
                                  coeff = 0.990;
                                  switch_default = true;
                                }; -- attempts_rate_limiter
                              }; -- balancer2
                            }; -- headers
                            refers = "to-pub";
                          }; -- report
                        }; -- pub
                        shortener = {
                          priority = 9;
                          match_fsm = {
                            URI = "/api/shortener.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "to-shortener";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            rewrite = {
                              actions = {
                                {
                                  global = false;
                                  split = "path";
                                  literal = false;
                                  rewrite = "/%1";
                                  case_insensitive = true;
                                  regexp = "/api/shortener/(.*)";
                                };
                                {
                                  global = false;
                                  split = "path";
                                  literal = false;
                                  rewrite = "/%1";
                                  case_insensitive = true;
                                  regexp = "/api/shortener(.*)";
                                };
                              }; -- actions
                              headers = {
                                create_func = {
                                  ["X-Zen-Original-Host"] = "host";
                                }; -- create_func
                                create = {
                                  Host = "zen-shortener.zdevx.yandex.ru";
                                }; -- create
                                append_func = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["Zen-Forwarded-For"] = "realip";
                                }; -- append_func
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  connection_attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "zen-shortener.zdevx.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:1:8235"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "40ms";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- rr
                                  attempts_rate_limiter = {
                                    limit = 0.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- headers
                            }; -- rewrite
                          }; -- report
                        }; -- shortener
                        social = {
                          priority = 8;
                          match_fsm = {
                            URI = "/api/comments.+";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "to-social";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            rewrite = {
                              actions = {
                                {
                                  global = false;
                                  split = "path";
                                  literal = false;
                                  rewrite = "/api/%1";
                                  case_insensitive = true;
                                  regexp = "/api/comments/(.*)";
                                };
                              }; -- actions
                              headers = {
                                create_func = {
                                  ["X-Forwarded-Zen-Host"] = "host";
                                  ["X-Original-Host"] = "host";
                                  ["X-Zen-Original-Host"] = "host";
                                }; -- create_func
                                create = {
                                  Host = "zen-social.zdevx.yandex.ru";
                                }; -- create
                                append_func = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["Zen-Forwarded-For"] = "realip";
                                }; -- append_func
                                response_headers = {
                                  create = {
                                    ["X-Content-Type-Options"] = "nosniff";
                                    ["X-Frame-Options"] = "SAMEORIGIN";
                                    ["X-XSS-Protection"] = "1; mode=block;";
                                  }; -- create
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    connection_attempts = 2;
                                    rr = {
                                      unpack(gen_proxy_backends({
                                        { "zen-social.zdevx.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:1:80da"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "40ms";
                                        backend_timeout = "60s";
                                        fail_on_5xx = false;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- rr
                                    attempts_rate_limiter = {
                                      limit = 0.200;
                                      coeff = 0.990;
                                      switch_default = true;
                                    }; -- attempts_rate_limiter
                                  }; -- balancer2
                                }; -- response_headers
                              }; -- headers
                            }; -- rewrite
                          }; -- report
                        }; -- social
                        stat = {
                          priority = 7;
                          match_fsm = {
                            URI = "(/api/v3/(launcher|desktop-morda|desktop|desktop-web|mobile-morda|visual-bookmarks|celltick)/stats/(bulk|common))|/api/v4/stats/common";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "to-stat";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "3620741824044273534";
                              headers = {
                                create_func = {
                                  Host = "host";
                                  ["X-Forwarded-Zen-Host"] = "host";
                                }; -- create_func
                                create = {
                                  ["X-Forwarded-Proto"] = "https";
                                }; -- create
                                append_func = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["Zen-Forwarded-For"] = "realip";
                                }; -- append_func
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  connection_attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "zeta-balancer.stable.qloud-b.yandex.net"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:1:8336"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "40ms";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- rr
                                  attempts_rate_limiter = {
                                    limit = 0.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- headers
                            }; -- shared
                          }; -- report
                        }; -- stat
                        styx = {
                          priority = 6;
                          match_fsm = {
                            URI = "/api/v3/launcher/similar-publisher";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "to-styx";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            rewrite = {
                              actions = {
                                {
                                  global = false;
                                  split = "path";
                                  literal = false;
                                  case_insensitive = true;
                                  rewrite = "/api/similar-publisher%1";
                                  regexp = "/api/v3/launcher/similar-publisher(.*)";
                                };
                              }; -- actions
                              headers = {
                                create_func = {
                                  ["X-Forwarded-Zen-Host"] = "host";
                                }; -- create_func
                                create = {
                                  Host = "zen-styx.zdevx.yandex.ru";
                                  ["X-Forwarded-Proto"] = "https";
                                }; -- create
                                append_func = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["Zen-Forwarded-For"] = "realip";
                                }; -- append_func
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  connection_attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "zen-styx.zdevx.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:0:e7"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "40ms";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- rr
                                  attempts_rate_limiter = {
                                    limit = 0.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- headers
                            }; -- rewrite
                          }; -- report
                        }; -- styx
                        ["api-apphost"] = {
                          priority = 5;
                          match_and = {
                            {
                              match_fsm = {
                                URI = "/api/v3/launcher/export";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_method = {
                                methods = { "post"; "get"; };
                              }; -- match_method
                            };
                          }; -- match_and
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-Zen-Host"] = "host";
                            }; -- create_func
                            create = {
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
                            report = {
                              ranges = get_str_var("default_ranges");
                              matcher_map = {
                                export = {
                                  match_fsm = {
                                    URI = "/*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                }; -- export
                              }; -- matcher_map
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                retry_policy = {
                                  unique_policy = {};
                                }; -- retry_policy
                                attempts = 1;
                                connection_attempts = 2;
                                rr = {
                                  ["front-node-api"] = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "to-front-node-api";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      shared = {
                                        uuid = "6561638647756991584";
                                      }; -- shared
                                    }; -- report
                                  }; -- ["front-node-api"]
                                  ["front-apphost-api"] = {
                                    weight = 99.000;
                                    report = {
                                      uuid = "to-front-apphost-api";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        retry_policy = {
                                          unique_policy = {};
                                        }; -- retry_policy
                                        attempts = 1;
                                        connection_attempts = 2;
                                        rr = {
                                          ["front-apphost-api-sas"] = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "to-front-apphost-api-sas";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                retry_policy = {
                                                  unique_policy = {};
                                                }; -- retry_policy
                                                attempts = 1;
                                                connection_attempts = 2;
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "sas";
                                                      endpoint_set_id = "awacs-rtc_balancer_zen-ah-testing_kaizen_yandex_net_sas";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "40ms";
                                                    backend_timeout = "60s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                  }; -- proxy_options
                                                  rr = {};
                                                }; -- sd
                                              }; -- balancer2
                                            }; -- report
                                          }; -- ["front-apphost-api-sas"]
                                          ["front-apphost-api-vla"] = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "to-front-apphost-api-vla";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                retry_policy = {
                                                  unique_policy = {};
                                                }; -- retry_policy
                                                attempts = 1;
                                                connection_attempts = 2;
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "vla";
                                                      endpoint_set_id = "awacs-rtc_balancer_zen-ah-testing_kaizen_yandex_net_vla";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "40ms";
                                                    backend_timeout = "60s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                  }; -- proxy_options
                                                  rr = {};
                                                }; -- sd
                                              }; -- balancer2
                                            }; -- report
                                          }; -- ["front-apphost-api-vla"]
                                          ["front-apphost-api-man"] = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "to-front-apphost-api-man";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                retry_policy = {
                                                  unique_policy = {};
                                                }; -- retry_policy
                                                attempts = 1;
                                                connection_attempts = 2;
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "man";
                                                      endpoint_set_id = "awacs-rtc_balancer_zen-ah-testing_kaizen_yandex_net_man";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "40ms";
                                                    backend_timeout = "60s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                  }; -- proxy_options
                                                  rr = {};
                                                }; -- sd
                                              }; -- balancer2
                                            }; -- report
                                          }; -- ["front-apphost-api-man"]
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- report
                                  }; -- ["front-apphost-api"]
                                }; -- rr
                              }; -- balancer2
                              refers = "to-front";
                            }; -- report
                          }; -- headers
                        }; -- ["api-apphost"]
                        ["front-api"] = {
                          priority = 4;
                          match_fsm = {
                            URI = "/((api/.*)|(partner/viber)|widget-loader|menu|onboarding|browser-promo)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          headers = {
                            create_func = {
                              Host = "host";
                              ["X-Forwarded-Zen-Host"] = "host";
                            }; -- create_func
                            create = {
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            append_func = {
                              ["X-Forwarded-For"] = "realip";
                              ["Zen-Forwarded-For"] = "realip";
                            }; -- append_func
                            report = {
                              ranges = get_str_var("default_ranges");
                              matcher_map = {
                                export = {
                                  match_fsm = {
                                    URI = "/(.*)/export";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                }; -- export
                                export_cached = {
                                  match_fsm = {
                                    URI = "/(.*)/export-cached";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                }; -- export_cached
                                more = {
                                  match_fsm = {
                                    URI = "/(.*)/more";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                }; -- more
                                layout = {
                                  match_fsm = {
                                    URI = "/(.*)/layout";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                }; -- layout
                              }; -- matcher_map
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              refers = "to-front";
                              shared = {
                                uuid = "6561638647756991584";
                                balancer2 = {
                                  retry_policy = {
                                    unique_policy = {};
                                  }; -- retry_policy
                                  attempts = 1;
                                  connection_attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "zeta-balancer.stable.qloud-b.yandex.net"; 80; 1.000; "2a02:6b8:0:3400:0:2e5:1:8336"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "40ms";
                                      backend_timeout = "60s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = false;
                                    }))
                                  }; -- rr
                                }; -- balancer2
                              }; -- shared
                            }; -- report
                          }; -- headers
                        }; -- ["front-api"]
                        rezen = {
                          priority = 3;
                          match_fsm = {
                            URI = "/(narrative|native|(search|subscriptions|profile(/blocked|/stories-stats|/feedback|/language|/forget)?|top(/[a-z]+)?)|/|)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            refers = "to-rezen";
                            shared = {
                              uuid = "3620741824044273534";
                            }; -- shared
                          }; -- report
                        }; -- rezen
                        channel = {
                          priority = 2;
                          match_or = {
                            {
                              match_fsm = {
                                URI = "/.+";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                          }; -- match_or
                          rewrite = {
                            actions = {
                              {
                                split = "url";
                                global = false;
                                literal = false;
                                regexp = "(.*)";
                                rewrite = "/channel%1";
                                case_insensitive = false;
                              };
                            }; -- actions
                            headers = {
                              create_func = {
                                Host = "host";
                                ["X-Forwarded-For-Y"] = "realip";
                                ["X-Forwarded-Zen-Host"] = "host";
                                ["X-Host-Y"] = "host";
                                ["X-Real-IP"] = "realip";
                                ["X-TLS-Cipher-Y"] = "ja3";
                              }; -- create_func
                              create = {
                                ["X-Antirobot-Service-Y"] = "zen";
                                ["X-Forwarded-Proto"] = "https";
                                ["X-Yandex-HTTPS"] = "yes";
                              }; -- create
                              append_func = {
                                ["X-Forwarded-For"] = "realip";
                                ["Zen-Forwarded-For"] = "realip";
                              }; -- append_func
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
                                        report = {
                                          uuid = "to-channel";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "6561638647756991584";
                                          }; -- shared
                                        }; -- report
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- headers
                          }; -- rewrite
                        }; -- channel
                        default = {
                          priority = 1;
                          report = {
                            uuid = "default";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 2;
                              connection_attempts = 2;
                              dynamic = {
                                max_pessimized_share = 0.200;
                                min_pessimization_coeff = 0.100;
                                weight_increase_step = 0.100;
                                history_interval = "10s";
                                backends_name = "static-l3";
                                unpack(gen_proxy_backends({
                                  { "zen-static.kaizen.yandex.net"; 81; 1.000; "2a02:6b8:0:3400:0:3c7:0:1"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "70ms";
                                  backend_timeout = "10s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- dynamic
                              attempts_rate_limiter = {
                                limit = 0.150;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                  content = "Service unavailable";
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- report
                        }; -- default
                      }; -- regexp
                    }; -- log_headers
                  }; -- log_headers
                }; -- cookie_policy
              }; -- shared
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
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
          ja3_enabled = true;
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("awacs-balancer.zen.zeta.kaizen.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-awacs-balancer.zen.zeta.kaizen.yandex.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.awacs-balancer.zen.zeta.kaizen.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.awacs-balancer.zen.zeta.kaizen.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.awacs-balancer.zen.zeta.kaizen.yandex.ru.key", "/dev/shm/balancer/priv");
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
                  uuid = "5676164135556292882";
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}