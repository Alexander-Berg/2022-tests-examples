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
        uuid = "7119709780618884049";
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
                  delete = "X-Real-IP";
                  create_func = {
                    ["X-Forwarded-For-Y"] = "realip";
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
                            url = "/ping\\.xml";
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
        uuid = "7119709780618884049";
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
        uuid = "2389236004252053926";
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
            contexts = {
              ["music.ya.ru"] = {
                priority = 5;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14541, "/place/db/www/logs");
                priv = get_private_cert_path("music.ya.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-music.ya.ru.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "music.ya.ru";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.music.ya.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.music.ya.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.music.ya.ru.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["music.ya.ru"]
              ["music.yandex"] = {
                priority = 4;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14541, "/place/db/www/logs");
                priv = get_private_cert_path("music.yandex.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-music.yandex.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "music.yandex";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.music.yandex.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.music.yandex.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.music.yandex.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["music.yandex"]
              ["music.yandex.uz"] = {
                priority = 3;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14541, "/place/db/www/logs");
                priv = get_private_cert_path("music.yandex.uz.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-music.yandex.uz.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "music.yandex.uz";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.music.yandex.uz.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.music.yandex.uz.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.music.yandex.uz.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["music.yandex.uz"]
              ["music.yandex.com"] = {
                priority = 2;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14541, "/place/db/www/logs");
                priv = get_private_cert_path("music.yandex.com.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-music.yandex.com.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "music.yandex.com";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.music.yandex.com.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.music.yandex.com.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.music.yandex.com.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["music.yandex.com"]
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
                  headers = {
                    delete = "X-Real-IP";
                    create_func = {
                      ["X-Forwarded-For-Y"] = "realip";
                      ["X-Request-Id"] = "reqid";
                      ["X-Scheme"] = "scheme";
                      ["X-Source-Port-Y"] = "realport";
                      ["X-Start-Time"] = "starttime";
                    }; -- create_func
                    create = {
                      ["X-Yandex-HTTPS"] = "yes";
                      ["X-Yandex-L7"] = "yes";
                    }; -- create
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
                          priority = 7;
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
                          priority = 6;
                          match_fsm = {
                            url = "/ping\\.xml";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "1683180570280210434";
                          }; -- shared
                        }; -- slbping
                        https_ya_redirect = {
                          priority = 5;
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
                          priority = 4;
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
                          priority = 3;
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
                        ["https_music-partner_yandex_ru"] = {
                          priority = 2;
                          match_fsm = {
                            host = "music-partner.yandex.ru";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "music_partner_upstreams";
                            headers = {
                              create_func = {
                                ["X-Forwarded-For"] = "realip";
                              }; -- create_func
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  music_partner_sas = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_music_partner_to_sas";
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
                                        connection_attempts = 8;
                                        active = {
                                          delay = "3s";
                                          request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music-ping.music.yandex.ru\n\n";
                                          steady = true;
                                          unpack(gen_proxy_backends({
                                            { "musfront20h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe21:47cf"; };
                                            { "musfront21h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe03:5586"; };
                                            { "musfront22h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe03:cf19"; };
                                            { "musfront23h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe8d:ad23"; };
                                            { "musfront24h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe3d:47"; };
                                            { "musfront25h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:feb8:72e5"; };
                                            { "musfront26h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fed8:53c5"; };
                                            { "musfront27h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe89:ec8f"; };
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
                                  }; -- music_partner_sas
                                  music_partner_man = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_music_partner_to_man";
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
                                        connection_attempts = 8;
                                        active = {
                                          delay = "3s";
                                          request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music-ping.music.yandex.ru\n\n";
                                          steady = true;
                                          unpack(gen_proxy_backends({
                                            { "musfront01i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:7100:216:3eff:fec9:74b7"; };
                                            { "musfront02i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fec6:9778"; };
                                            { "musfront03i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4601:216:3eff:fe53:9c2b"; };
                                            { "musfront04i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fe64:9246"; };
                                            { "musfront05i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fed7:91a2"; };
                                            { "musfront06i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fe8e:b047"; };
                                            { "musfront07i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:febe:2268"; };
                                            { "musfront08i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fe28:8ff"; };
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
                                  }; -- music_partner_man
                                  music_partner_myt = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_music_partner_to_myt";
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
                                        connection_attempts = 8;
                                        active = {
                                          delay = "3s";
                                          request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music-ping.music.yandex.ru\n\n";
                                          steady = true;
                                          unpack(gen_proxy_backends({
                                            { "musfront01f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fed1:f33a"; };
                                            { "musfront02f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe2c:7a3"; };
                                            { "musfront03f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fedc:f51a"; };
                                            { "musfront04f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fedc:7db"; };
                                            { "musfront05f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe7f:372d"; };
                                            { "musfront06f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe86:e517"; };
                                            { "musfront07f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe66:ad46"; };
                                            { "musfront08f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fede:3838"; };
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
                                  }; -- music_partner_myt
                                }; -- rr
                                on_error = {
                                  report = {
                                    uuid = "onerror_music_partner";
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
                                        music_partner_sas = {
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
                                              connection_attempts = 8;
                                              rr = {
                                                unpack(gen_proxy_backends({
                                                  { "musfront20h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe21:47cf"; };
                                                  { "musfront21h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe03:5586"; };
                                                  { "musfront22h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe03:cf19"; };
                                                  { "musfront23h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe8d:ad23"; };
                                                  { "musfront24h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe3d:47"; };
                                                  { "musfront25h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:feb8:72e5"; };
                                                  { "musfront26h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fed8:53c5"; };
                                                  { "musfront27h.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1a1b:216:3eff:fe89:ec8f"; };
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
                                            refers = "requests_music_partner_to_sas";
                                          }; -- report
                                        }; -- music_partner_sas
                                        music_partner_man = {
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
                                              connection_attempts = 8;
                                              rr = {
                                                unpack(gen_proxy_backends({
                                                  { "musfront01i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:7100:216:3eff:fec9:74b7"; };
                                                  { "musfront02i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fec6:9778"; };
                                                  { "musfront03i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4601:216:3eff:fe53:9c2b"; };
                                                  { "musfront04i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fe64:9246"; };
                                                  { "musfront05i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fed7:91a2"; };
                                                  { "musfront06i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fe8e:b047"; };
                                                  { "musfront07i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:febe:2268"; };
                                                  { "musfront08i.music.yandex.net"; 80; 1.000; "2a02:6b8:b011:4600:216:3eff:fe28:8ff"; };
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
                                            refers = "requests_music_partner_to_man";
                                          }; -- report
                                        }; -- music_partner_man
                                        music_partner_myt = {
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
                                              connection_attempts = 8;
                                              rr = {
                                                unpack(gen_proxy_backends({
                                                  { "musfront01f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fed1:f33a"; };
                                                  { "musfront02f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe2c:7a3"; };
                                                  { "musfront03f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fedc:f51a"; };
                                                  { "musfront04f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fedc:7db"; };
                                                  { "musfront05f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe7f:372d"; };
                                                  { "musfront06f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe86:e517"; };
                                                  { "musfront07f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fe66:ad46"; };
                                                  { "musfront08f.music.yandex.net"; 80; 1.000; "2a02:6b8:0:1444:216:3eff:fede:3838"; };
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
                                            refers = "requests_music_partner_to_myt";
                                          }; -- report
                                        }; -- music_partner_myt
                                      }; -- rr
                                    }; -- balancer2
                                  }; -- report
                                }; -- on_error
                              }; -- balancer2
                            }; -- headers
                          }; -- shared
                        }; -- ["https_music-partner_yandex_ru"]
                        default = {
                          priority = 1;
                          report = {
                            uuid = "music";
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
                              }; -- geo
                              threshold = {
                                lo_bytes = 102400;
                                hi_bytes = 1024000;
                                recv_timeout = "1s";
                                pass_timeout = "10s";
                                regexp = {
                                  aab_proxy = {
                                    priority = 5;
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
                                    priority = 4;
                                    match_fsm = {
                                      header = {
                                        name = "x-aab-http-check";
                                        value = ".*";
                                      }; -- header
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    headers = {
                                      create_func = {
                                        ["X-Forwarded-For"] = "realip";
                                      }; -- create_func
                                      shared = {
                                        uuid = "main_cryprox_section";
                                      }; -- shared
                                    }; -- headers
                                  }; -- aab_http_check
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
                                      }; -- create_func
                                      shared = {
                                        uuid = "6882737606313467076";
                                      }; -- shared
                                    }; -- headers
                                  }; -- http_post
                                  cookie_bltsr = {
                                    priority = 2;
                                    match_fsm = {
                                      cookie = "bltsr|qgZTpupNMGJBM|mcBaGDt|BgeeyNoBJuyII|orrXTfJaS|FgkKdCjPqoMFm|EIXtkCTlX|JPIqApiY|KIykI|HgGedof|ancQTZw|involved|instruction|engineering|telecommunications|discussion|computer|substantial|specific|engineer|adequate";
                                      case_insensitive = true;
                                      surround = true;
                                    }; -- match_fsm
                                    shared = {
                                      uuid = "main_cryprox_section";
                                      headers = {
                                        create_func = {
                                          ["X-Forwarded-For"] = "realip";
                                        }; -- create_func
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
                                      }; -- headers
                                    }; -- shared
                                  }; -- cookie_bltsr
                                  default = {
                                    priority = 1;
                                    headers = {
                                      create_func = {
                                        ["X-Forwarded-For"] = "realip";
                                      }; -- create_func
                                      shared = {
                                        uuid = "music_upstreams";
                                        regexp = {
                                          translate_s3 = {
                                            priority = 3;
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
                                                  uuid = "5420764705516164323";
                                                  shared = {
                                                    uuid = "music_upstreams_direct";
                                                  }; -- shared
                                                }; -- shared
                                              }; -- on_error
                                            }; -- balancer2
                                          }; -- translate_s3
                                          translate = {
                                            priority = 2;
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
                                                  uuid = "5420764705516164323";
                                                }; -- shared
                                              }; -- on_error
                                            }; -- balancer2
                                          }; -- translate
                                          default = {
                                            priority = 1;
                                            shared = {
                                              uuid = "music_upstreams_direct";
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
                                                        connection_attempts = 13;
                                                        active = {
                                                          delay = "3s";
                                                          request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music.yandex.ru\n\n";
                                                          steady = true;
                                                          unpack(gen_proxy_backends({
                                                            { "sas1-0359-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:1083:10d:4bfc:0:23dc"; };
                                                            { "sas1-0516-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:1d1f:10d:4bfc:0:23dc"; };
                                                            { "sas1-0897-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:917:10d:4bfc:0:23dc"; };
                                                            { "sas1-0975-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:eac:10d:4bfc:0:23dc"; };
                                                            { "sas1-1956-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:2695:10d:4bfc:0:23dc"; };
                                                            { "sas1-2799-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:1698:10d:4bfc:0:23dc"; };
                                                            { "sas1-4158-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:7f12:10d:4bfc:0:23dc"; };
                                                            { "sas1-4575-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:a004:10d:4bfc:0:23dc"; };
                                                            { "sas1-4596-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:2180:10d:4bfc:0:23dc"; };
                                                            { "sas1-6995-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:621b:10d:4bfc:0:23dc"; };
                                                            { "sas1-7779-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:679f:10d:4bfc:0:23dc"; };
                                                            { "sas2-5168-a6a-sas-music-stable-mus-dc4-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c14:2ba7:10d:4bfc:0:23dc"; };
                                                            { "slovo009-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:729e:10d:4bfc:0:23dc"; };
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
                                                        connection_attempts = 13;
                                                        active = {
                                                          delay = "3s";
                                                          request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music.yandex.ru\n\n";
                                                          steady = true;
                                                          unpack(gen_proxy_backends({
                                                            { "man1-1348-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0b:20ed:10d:4bfe:0:5aff"; };
                                                            { "man1-2073-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0b:28ee:10d:4bfe:0:5aff"; };
                                                            { "man1-3729-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0b:2321:10d:4bfe:0:5aff"; };
                                                            { "man1-7443-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0b:182d:10d:4bfe:0:5aff"; };
                                                            { "man2-0486-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0b:4f27:10d:4bfe:0:5aff"; };
                                                            { "man2-1061-2c0-man-music-stable-mu-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:531:10d:4bfe:0:5aff"; };
                                                            { "man2-1066-617-man-music-stable-mu-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:53a:10d:4bfe:0:5aff"; };
                                                            { "man2-1594-4a8-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c13:1505:10d:4bfe:0:5aff"; };
                                                            { "man2-1608-4ca-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c13:1512:10d:4bfe:0:5aff"; };
                                                            { "man2-2307-cae-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c13:1383:10d:4bfe:0:5aff"; };
                                                            { "man2-4990-fcf-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:110c:10d:4bfe:0:5aff"; };
                                                            { "man2-5527-b1f-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:b17:10d:4bfe:0:5aff"; };
                                                            { "man2-6527-e5f-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0a:f9d:10d:4bfe:0:5aff"; };
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
                                                        connection_attempts = 13;
                                                        active = {
                                                          delay = "3s";
                                                          request = "GET /handlers/ping-all.jsx HTTP/1.0\nHost: music.yandex.ru\n\n";
                                                          steady = true;
                                                          unpack(gen_proxy_backends({
                                                            { "myt1-0255-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:9d2:10e:ae1:0:5c97"; };
                                                            { "myt1-0421-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:a84:10e:ae1:0:5c97"; };
                                                            { "myt1-0458-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1b91:10e:ae1:0:5c97"; };
                                                            { "myt1-0476-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:688:10e:ae1:0:5c97"; };
                                                            { "myt1-0662-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1d59:10e:ae1:0:5c97"; };
                                                            { "myt1-0663-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1d03:10e:ae1:0:5c97"; };
                                                            { "myt1-0667-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1d1a:10e:ae1:0:5c97"; };
                                                            { "myt1-1463-66b-msk-myt-music-st-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c12:3220:10e:ae1:0:5c97"; };
                                                            { "myt1-1503-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:11a8:10e:ae1:0:5c97"; };
                                                            { "myt1-3075-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1af:10e:ae1:0:5c97"; };
                                                            { "myt1-3082-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:183:10e:ae1:0:5c97"; };
                                                            { "myt1-3172-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1621:10e:ae1:0:5c97"; };
                                                            { "myt1-3210-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1615:10e:ae1:0:5c97"; };
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
                                                              connection_attempts = 13;
                                                              rr = {
                                                                unpack(gen_proxy_backends({
                                                                  { "sas1-0359-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:1083:10d:4bfc:0:23dc"; };
                                                                  { "sas1-0516-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:1d1f:10d:4bfc:0:23dc"; };
                                                                  { "sas1-0897-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:917:10d:4bfc:0:23dc"; };
                                                                  { "sas1-0975-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:eac:10d:4bfc:0:23dc"; };
                                                                  { "sas1-1956-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:2695:10d:4bfc:0:23dc"; };
                                                                  { "sas1-2799-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:1698:10d:4bfc:0:23dc"; };
                                                                  { "sas1-4158-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:7f12:10d:4bfc:0:23dc"; };
                                                                  { "sas1-4575-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:a004:10d:4bfc:0:23dc"; };
                                                                  { "sas1-4596-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:2180:10d:4bfc:0:23dc"; };
                                                                  { "sas1-6995-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c08:621b:10d:4bfc:0:23dc"; };
                                                                  { "sas1-7779-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:679f:10d:4bfc:0:23dc"; };
                                                                  { "sas2-5168-a6a-sas-music-stable-mus-dc4-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c14:2ba7:10d:4bfc:0:23dc"; };
                                                                  { "slovo009-sas-music-stable-musfront-9180.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c08:729e:10d:4bfc:0:23dc"; };
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
                                                              connection_attempts = 13;
                                                              rr = {
                                                                unpack(gen_proxy_backends({
                                                                  { "man1-1348-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0b:20ed:10d:4bfe:0:5aff"; };
                                                                  { "man1-2073-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0b:28ee:10d:4bfe:0:5aff"; };
                                                                  { "man1-3729-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0b:2321:10d:4bfe:0:5aff"; };
                                                                  { "man1-7443-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0b:182d:10d:4bfe:0:5aff"; };
                                                                  { "man2-0486-man-music-stable-musfront-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0b:4f27:10d:4bfe:0:5aff"; };
                                                                  { "man2-1061-2c0-man-music-stable-mu-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:531:10d:4bfe:0:5aff"; };
                                                                  { "man2-1066-617-man-music-stable-mu-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:53a:10d:4bfe:0:5aff"; };
                                                                  { "man2-1594-4a8-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c13:1505:10d:4bfe:0:5aff"; };
                                                                  { "man2-1608-4ca-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c13:1512:10d:4bfe:0:5aff"; };
                                                                  { "man2-2307-cae-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c13:1383:10d:4bfe:0:5aff"; };
                                                                  { "man2-4990-fcf-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:110c:10d:4bfe:0:5aff"; };
                                                                  { "man2-5527-b1f-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 321.000; "2a02:6b8:c0a:b17:10d:4bfe:0:5aff"; };
                                                                  { "man2-6527-e5f-man-music-stable-b0a-23295.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0a:f9d:10d:4bfe:0:5aff"; };
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
                                                              connection_attempts = 13;
                                                              rr = {
                                                                unpack(gen_proxy_backends({
                                                                  { "myt1-0255-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:9d2:10e:ae1:0:5c97"; };
                                                                  { "myt1-0421-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:a84:10e:ae1:0:5c97"; };
                                                                  { "myt1-0458-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1b91:10e:ae1:0:5c97"; };
                                                                  { "myt1-0476-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:688:10e:ae1:0:5c97"; };
                                                                  { "myt1-0662-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1d59:10e:ae1:0:5c97"; };
                                                                  { "myt1-0663-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1d03:10e:ae1:0:5c97"; };
                                                                  { "myt1-0667-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1d1a:10e:ae1:0:5c97"; };
                                                                  { "myt1-1463-66b-msk-myt-music-st-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c12:3220:10e:ae1:0:5c97"; };
                                                                  { "myt1-1503-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:11a8:10e:ae1:0:5c97"; };
                                                                  { "myt1-3075-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1af:10e:ae1:0:5c97"; };
                                                                  { "myt1-3082-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:183:10e:ae1:0:5c97"; };
                                                                  { "myt1-3172-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1621:10e:ae1:0:5c97"; };
                                                                  { "myt1-3210-msk-myt-music-stable-428-23703.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c00:1615:10e:ae1:0:5c97"; };
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
                                          }; -- default
                                        }; -- regexp
                                      }; -- shared
                                    }; -- headers
                                  }; -- default
                                }; -- regexp
                              }; -- threshold
                            }; -- geobase
                          }; -- report
                        }; -- default
                      }; -- regexp
                    }; -- response_headers
                  }; -- headers
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
        uuid = "2389236004252053926";
      }; -- shared
    }; -- https_section_14541
  }; -- ipdispatch
}