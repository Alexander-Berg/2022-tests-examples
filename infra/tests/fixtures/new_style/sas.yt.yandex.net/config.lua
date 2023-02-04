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
  maxconn = 10000;
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
  log = get_log_path("childs_log", 14730, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 14730;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 14730;
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
  addrs = {
    {
      port = 14730;
      ip = "127.0.0.4";
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1:132";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "5.255.240.132";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14730;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14730;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::1:132";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "5.255.240.132";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14731;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14731;
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
        14730;
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
        14730;
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
        "2a02:6b8:0:3400::1:132";
        "5.255.240.132";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "6690911244652164459";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14730, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 14730, "/place/db/www/logs");
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
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_14730 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14730;
      }; -- ports
      shared = {
        uuid = "6690911244652164459";
      }; -- shared
    }; -- http_section_14730
    https_section_443 = {
      ips = {
        "2a02:6b8:0:3400::1:132";
        "5.255.240.132";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "5588931940787760461";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14731, "/place/db/www/logs");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            contexts = {
              ["idm.yt.yandex.net"] = {
                priority = 25;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                ca = get_ca_cert_path("YandexInternalCA.pem", "./");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("idm.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-idm.yt.yandex.net.pem", "/dev/shm/balancer");
                client = {
                  verify_peer = true;
                  verify_depth = 3;
                  verify_once = true;
                  fail_if_no_peer_cert = false;
                }; -- client
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "idm\\.(.+\\.)?yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.idm.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.idm.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.idm.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["idm.yt.yandex.net"]
              ["arnold.yt.yandex.net"] = {
                priority = 24;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("arnold.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-arnold.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "arnold\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.arnold.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.arnold.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.arnold.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["arnold.yt.yandex.net"]
              ["bohr.yt.yandex.net"] = {
                priority = 23;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("bohr.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-bohr.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "bohr\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.bohr.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.bohr.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.bohr.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["bohr.yt.yandex.net"]
              ["flux.yt.yandex.net"] = {
                priority = 22;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("flux.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-flux.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "flux\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.flux.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.flux.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.flux.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["flux.yt.yandex.net"]
              ["freud.yt.yandex.net"] = {
                priority = 21;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("freud.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-freud.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "freud\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.freud.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.freud.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.freud.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["freud.yt.yandex.net"]
              ["hahn.yt.yandex.net"] = {
                priority = 20;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("hahn.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-hahn.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "hahn\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.hahn.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.hahn.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.hahn.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["hahn.yt.yandex.net"]
              ["hume.yt.yandex.net"] = {
                priority = 19;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("hume.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-hume.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "hume\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.hume.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.hume.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.hume.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["hume.yt.yandex.net"]
              ["locke.yt.yandex.net"] = {
                priority = 18;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("locke.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-locke.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "locke\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.locke.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.locke.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.locke.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["locke.yt.yandex.net"]
              ["markov.yt.yandex.net"] = {
                priority = 17;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("markov.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-markov.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "markov\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.markov.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.markov.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.markov.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["markov.yt.yandex.net"]
              ["perelman.yt.yandex.net"] = {
                priority = 16;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("perelman.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-perelman.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "perelman\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.perelman.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.perelman.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.perelman.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["perelman.yt.yandex.net"]
              ["pythia.yt.yandex.net"] = {
                priority = 15;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("pythia.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-pythia.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "pythia\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.pythia.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.pythia.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.pythia.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["pythia.yt.yandex.net"]
              ["seneca-man.yt.yandex.net"] = {
                priority = 14;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("seneca-man.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-seneca-man.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "seneca-man\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.seneca-man.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.seneca-man.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.seneca-man.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["seneca-man.yt.yandex.net"]
              ["seneca-sas.yt.yandex.net"] = {
                priority = 13;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("seneca-sas.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-seneca-sas.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "seneca-sas\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.seneca-sas.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.seneca-sas.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.seneca-sas.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["seneca-sas.yt.yandex.net"]
              ["seneca-vla.yt.yandex.net"] = {
                priority = 12;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("seneca-vla.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-seneca-vla.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "seneca-vla\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.seneca-vla.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.seneca-vla.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.seneca-vla.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["seneca-vla.yt.yandex.net"]
              ["socrates.yt.yandex.net"] = {
                priority = 11;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("socrates.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-socrates.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "socrates\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.socrates.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.socrates.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.socrates.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["socrates.yt.yandex.net"]
              ["vanga.yt.yandex.net"] = {
                priority = 10;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("vanga.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-vanga.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "vanga\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.vanga.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.vanga.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.vanga.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["vanga.yt.yandex.net"]
              ["yp-man.yt.yandex.net"] = {
                priority = 9;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("yp-man.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yp-man.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "yp-man\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yp-man.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yp-man.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yp-man.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["yp-man.yt.yandex.net"]
              ["yp-man-pre.yt.yandex.net"] = {
                priority = 8;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("yp-man-pre.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yp-man-pre.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "yp-man-pre\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yp-man-pre.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yp-man-pre.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yp-man-pre.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["yp-man-pre.yt.yandex.net"]
              ["yp-sas.yt.yandex.net"] = {
                priority = 7;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("yp-sas.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yp-sas.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "yp-sas\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yp-sas.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yp-sas.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yp-sas.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["yp-sas.yt.yandex.net"]
              ["yp-sas-test.yt.yandex.net"] = {
                priority = 6;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("yp-sas-test.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yp-sas-test.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "yp-sas-test\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yp-sas-test.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yp-sas-test.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yp-sas-test.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["yp-sas-test.yt.yandex.net"]
              ["yp-vla.yt.yandex.net"] = {
                priority = 5;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("yp-vla.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yp-vla.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "yp-vla\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yp-vla.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yp-vla.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yp-vla.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["yp-vla.yt.yandex.net"]
              ["zeno.yt.yandex.net"] = {
                priority = 4;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("zeno.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-zeno.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "zeno\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.zeno.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.zeno.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.zeno.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["zeno.yt.yandex.net"]
              ["tm-userdata.yt.yandex-team.ru"] = {
                priority = 3;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("tm-userdata.yt.yandex-team.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-tm-userdata.yt.yandex-team.ru.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "tm-userdata\\.yt\\.yandex(-team\\.ru|\\.net)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.tm-userdata.yt.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.tm-userdata.yt.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.tm-userdata.yt.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["tm-userdata.yt.yandex-team.ru"]
              ["transfer-manager.yt.yandex.net"] = {
                priority = 2;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("transfer-manager.yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-transfer-manager.yt.yandex.net.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "transfer-manager\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.transfer-manager.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.transfer-manager.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.transfer-manager.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["transfer-manager.yt.yandex.net"]
              default = {
                priority = 1;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 14731, "/place/db/www/logs");
                priv = get_private_cert_path("yt.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yt.yandex.net.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yt.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yt.yandex.net.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 14731, "/place/db/www/logs");
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
                    headers = {
                      create_func = {
                        ["X-Forwarded-For"] = "realip";
                        ["X-Forwarded-For-Y"] = "realip";
                        ["X-Req-Id"] = "reqid";
                        ["X-SSL-Client-CN"] = "ssl_client_cert_cn";
                        ["X-SSL-Client-Subject"] = "ssl_client_cert_subject";
                        ["X-SSL-Client-Verify"] = "ssl_client_cert_verify_result";
                        ["X-Scheme"] = "scheme";
                        ["X-Source-Port-Y"] = "realport";
                        ["X-Start-Time"] = "starttime";
                      }; -- create_func
                      log_headers = {
                        name_re = "X-Req-Id";
                        regexp = {
                          ["awacs-balancer-health-check"] = {
                            priority = 70;
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
                            priority = 69;
                            match_fsm = {
                              url = "/ping";
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
                          freud_clickhouse = {
                            priority = 68;
                            match_fsm = {
                              host = "clickhouse\\.\\w+\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "clickhouse_proxy";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "3600s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 1;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "3600s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "yxtoy23mndlywpqn.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a58:10d:cbf4:b5c7:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "3600s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- freud_clickhouse
                          yt_idm_testing_v2 = {
                            priority = 67;
                            match_fsm = {
                              host = "idm-test\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yt_idm_testing_v2";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "60s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "ss2qvfkzey5nlxnr.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:3996:0:49f5:28a8:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_idm_testing_v2
                          yt_oauth = {
                            priority = 66;
                            match_fsm = {
                              host = "oauth.yt.yandex.net";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yt_oauth";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
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
                                    { "su3iduhcqlblnarl.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4884:10d:d45e:5763:0"; };
                                    { "ukzbewfnwwjv4s27.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:7b8e:10d:d45e:e064:0"; };
                                    { "vz5qai5cunc7huzm.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:f92:10d:d45e:ec8a:0"; };
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
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- yt_oauth
                          yt_cloud_adapter_markov = {
                            priority = 65;
                            match_fsm = {
                              host = "cloud-adapter-markov.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-markov";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8090; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-7678.search.yandex.net"; 8090; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "vla1-3208.search.yandex.net"; 8090; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_markov
                          yt_cloud_adapter_zeno = {
                            priority = 64;
                            match_fsm = {
                              host = "cloud-adapter-zeno.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-zeno";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8110; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8110; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8110; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_zeno
                          yt_cloud_adapter_pythia = {
                            priority = 63;
                            match_fsm = {
                              host = "cloud-adapter-pythia.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-pythia";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8100; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8100; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8100; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_pythia
                          yt_cloud_adapter_freud = {
                            priority = 62;
                            match_fsm = {
                              host = "cloud-adapter-freud.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-freud";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8030; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8030; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8030; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_freud
                          yt_cloud_adapter_hahn = {
                            priority = 61;
                            match_fsm = {
                              host = "cloud-adapter-hahn.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-hahn";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8120; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "man1-9552.search.yandex.net"; 8120; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-7678.search.yandex.net"; 8120; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "sas1-8860.search.yandex.net"; 8120; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8120; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                            { "vla1-3208.search.yandex.net"; 8128; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_hahn
                          yt_cloud_adapter_arnold = {
                            priority = 60;
                            match_fsm = {
                              host = "cloud-adapter-arnold.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-arnold";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8240; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "man1-5466.search.yandex.net"; 8240; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-3675.search.yandex.net"; 8240; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "sas1-8860.search.yandex.net"; 8240; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8240; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                            { "vla1-3208.search.yandex.net"; 8248; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_arnold
                          yt_cloud_adapter_yp_xdc = {
                            priority = 59;
                            match_fsm = {
                              host = "cloud-adapter-yp-xdc.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-xdc";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8360; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-8860.search.yandex.net"; 8360; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8360; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_xdc
                          yt_cloud_adapter_socrates = {
                            priority = 58;
                            match_fsm = {
                              host = "cloud-adapter-socrates.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-socrates";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8060; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8060; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8060; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_socrates
                          yt_cloud_adapter_landau = {
                            priority = 57;
                            match_fsm = {
                              host = "cloud-adapter-landau.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-landau";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8270; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8270; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8270; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_landau
                          yt_cloud_adapter_seneca_man = {
                            priority = 56;
                            match_fsm = {
                              host = "cloud-adapter-seneca-man.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-seneca-man";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8140; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-7678.search.yandex.net"; 8140; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "vla1-3208.search.yandex.net"; 8140; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_seneca_man
                          yt_cloud_adapter_seneca_vla = {
                            priority = 55;
                            match_fsm = {
                              host = "cloud-adapter-seneca-vla.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-seneca-vla";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8260; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-7678.search.yandex.net"; 8260; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "vla1-3208.search.yandex.net"; 8260; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_seneca_vla
                          yt_cloud_adapter_seneca_sas = {
                            priority = 54;
                            match_fsm = {
                              host = "cloud-adapter-seneca-sas.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-seneca-sas";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8150; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-8860.search.yandex.net"; 8150; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8150; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_seneca_sas
                          yt_cloud_adapter_hume = {
                            priority = 53;
                            match_fsm = {
                              host = "cloud-adapter-hume.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-hume";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8020; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8020; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8020; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_hume
                          yt_cloud_adapter_bohr = {
                            priority = 52;
                            match_fsm = {
                              host = "cloud-adapter-bohr.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-bohr";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8210; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-3675.search.yandex.net"; 8210; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8210; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_bohr
                          yt_cloud_adapter_locke = {
                            priority = 51;
                            match_fsm = {
                              host = "cloud-adapter-locke.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-locke";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8200; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-8860.search.yandex.net"; 8200; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8200; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_locke
                          yt_cloud_adapter_vanga = {
                            priority = 50;
                            match_fsm = {
                              host = "cloud-adapter-vanga.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-vanga";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8170; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-7678.search.yandex.net"; 8170; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "vla1-3208.search.yandex.net"; 8170; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_vanga
                          yt_cloud_adapter_flux = {
                            priority = 49;
                            match_fsm = {
                              host = "cloud-adapter-flux.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-flux";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8190; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-7678.search.yandex.net"; 8190; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "vla1-3208.search.yandex.net"; 8190; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_flux
                          yt_cloud_adapter_ofd = {
                            priority = 48;
                            match_fsm = {
                              host = "cloud-adapter-ofd.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-ofd";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8280; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-3675.search.yandex.net"; 8280; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8280; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_ofd
                          yt_cloud_adapter_yp_sas = {
                            priority = 47;
                            match_fsm = {
                              host = "cloud-adapter-yp-sas.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-sas";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8180; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-8860.search.yandex.net"; 8180; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8180; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_sas
                          yt_cloud_adapter_yp_sas_test = {
                            priority = 46;
                            match_fsm = {
                              host = "cloud-adapter-yp-sas-test.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-sas-test";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8330; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-3675.search.yandex.net"; 8330; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8330; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_sas_test
                          yt_cloud_adapter_yp_man = {
                            priority = 45;
                            match_fsm = {
                              host = "cloud-adapter-yp-man.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-man";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8300; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-7678.search.yandex.net"; 8300; 40.000; "2a02:6b8:b000:695:922b:34ff:fecf:2cf8"; };
                                            { "vla1-3208.search.yandex.net"; 8300; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_man
                          yt_cloud_adapter_yp_man_pre = {
                            priority = 44;
                            match_fsm = {
                              host = "cloud-adapter-yp-man-pre.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-man-pre";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8310; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-3675.search.yandex.net"; 8310; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8310; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_man_pre
                          yt_cloud_adapter_yp_myt = {
                            priority = 43;
                            match_fsm = {
                              host = "cloud-adapter-yp-myt.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-myt";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5446.search.yandex.net"; 8370; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:9222"; };
                                            { "sas1-8860.search.yandex.net"; 8370; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8370; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_myt
                          yt_cloud_adapter_yp_vla = {
                            priority = 42;
                            match_fsm = {
                              host = "cloud-adapter-yp-vla.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-vla";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8340; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-3675.search.yandex.net"; 8340; 40.000; "2a02:6b8:b000:101:225:90ff:fe88:37fa"; };
                                            { "vla1-3208.search.yandex.net"; 8340; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_vla
                          yt_cloud_adapter_sentry = {
                            priority = 41;
                            match_fsm = {
                              host = "cloud-adapter-sentry.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-sentry";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552.search.yandex.net"; 8350; 40.000; "2a02:6b8:b000:6009:feaa:14ff:feea:8e9a"; };
                                            { "sas1-8860.search.yandex.net"; 8350; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8350; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                  on_fast_error = {
                                    errordocument = {
                                      status = 505;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_fast_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_sentry
                          yt_cloud_adapter_yp_iva = {
                            priority = 40;
                            match_fsm = {
                              host = "cloud-adapter-yp-iva.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-yp-iva";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466.search.yandex.net"; 8380; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:92aa"; };
                                            { "sas1-8860.search.yandex.net"; 8380; 40.000; "2a02:6b8:b000:65a:922b:34ff:fecf:33b6"; };
                                            { "vla1-3208.search.yandex.net"; 8380; 40.000; "2a02:6b8:c0e:74:0:604:db7:9904"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_yp_iva
                          yt_cloud_adapter_ofd_xdc = {
                            priority = 39;
                            match_fsm = {
                              host = "cloud-adapter-ofd-xdc.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "cloud-adapter-ofd-xdc";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-9552-man-yt-cplane-cloud-a-805-8290.gencfg-c.yandex.net"; 8290; 40.000; "2a02:6b8:c0b:4419:10d:ef04:0:2062"; };
                                            { "sas1-7678-sas-yt-cplane-cloud-a-2b0-8290.gencfg-c.yandex.net"; 8290; 40.000; "2a02:6b8:c08:6216:10d:ef03:0:2062"; };
                                            { "vla1-3208-vla-yt-cplane-cloud-a-ed4-8290.gencfg-c.yandex.net"; 8290; 40.000; "2a02:6b8:c0d:3a01:10d:ef06:0:2062"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_cloud_adapter_ofd_xdc
                          yt_linter_test = {
                            priority = 38;
                            match_fsm = {
                              host = "linter-test.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "linter-test";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466-man-yt-cplane-linter-test-8070.gencfg-c.yandex.net"; 8070; 40.000; "2a02:6b8:c09:238c:10e:ae82:0:1f86"; };
                                            { "sas1-7678-sas-yt-cplane-linter-test-8070.gencfg-c.yandex.net"; 8070; 40.000; "2a02:6b8:c08:6216:10e:ae81:0:1f86"; };
                                            { "vla1-3208-vla-yt-cplane-linter-test-8070.gencfg-c.yandex.net"; 8070; 40.000; "2a02:6b8:c0d:3a01:10e:ae83:0:1f86"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_linter_test
                          yt_linter_prod = {
                            priority = 37;
                            match_fsm = {
                              host = "linter.yt.yandex-team.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "linter-prod";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    main = {
                                      weight = 100.000;
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "10s";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-5466-man-yt-cplane-linter-prod-8220.gencfg-c.yandex.net"; 8220; 40.000; "2a02:6b8:c09:238c:10e:ae85:0:201c"; };
                                            { "man1-9552-man-yt-cplane-linter-prod-8220.gencfg-c.yandex.net"; 8220; 40.000; "2a02:6b8:c0b:4419:10e:ae85:0:201c"; };
                                            { "sas1-7678-sas-yt-cplane-linter-prod-8220.gencfg-c.yandex.net"; 8220; 40.000; "2a02:6b8:c08:6216:10e:ae84:0:201c"; };
                                            { "sas1-8860-sas-yt-cplane-linter-prod-8220.gencfg-c.yandex.net"; 8220; 40.000; "2a02:6b8:c08:5c0a:10e:ae84:0:201c"; };
                                            { "vla1-3208-vla-yt-cplane-linter-prod-8220.gencfg-c.yandex.net"; 8220; 40.000; "2a02:6b8:c0d:3a01:10e:ae87:0:201c"; };
                                            { "vla1-3208-vla-yt-cplane-linter-prod-8228.gencfg-c.yandex.net"; 8228; 40.000; "2a02:6b8:c0d:3a01:10e:ae87:0:2024"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- main
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_linter_prod
                          yt_idm_testing = {
                            priority = 36;
                            match_fsm = {
                              host = "idm\\.(socrates|hume|freud)\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yt_idm_testing";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  Authorization = get_str_env_var("IDM_TOKEN");
                                }; -- create
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "60s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "yt-idm-testing-1.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:2411:10d:d45e:b28c:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_idm_testing
                          yt_idm_legacy_integration = {
                            priority = 35;
                            match_fsm = {
                              host = "idm\\.(locke|vanga|pythia|hume|hahn|freud|seneca-sas|seneca-vla|seneca-myt|seneca-man|zeno|socrates|banach|yp-msk|landau|yp-man-pre|markov|yp-sas|yp-man|ofd|ofd-myt)\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yt_idm_legacy_integration";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  Authorization = get_str_env_var("IDM_TOKEN");
                                }; -- create
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "60s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "ipcvvwr3pajcqceo.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:381c:10d:ca02:33c8:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_idm_legacy_integration
                          hume = {
                            priority = 34;
                            match_fsm = {
                              host = "(idm\\.)?hume\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "hume";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: hume.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-4316-8b1.hume.yt.gencfg-c.yandex.net"; 80; 630.000; "2a02:6b8:c0a:1e2d:10e:ad64:0:50"; };
                                          { "man2-4352-14b.hume.yt.gencfg-c.yandex.net"; 80; 630.000; "2a02:6b8:c0a:a8c:10e:ad64:0:50"; };
                                          { "man2-4355-d49.hume.yt.gencfg-c.yandex.net"; 80; 630.000; "2a02:6b8:c0a:1e97:10e:ad64:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: hume.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-4316-8b1.hume.yt.gencfg-c.yandex.net"; 80; 630.000; "2a02:6b8:c0a:1e2d:10e:ad64:0:50"; };
                                          { "man2-4352-14b.hume.yt.gencfg-c.yandex.net"; 80; 630.000; "2a02:6b8:c0a:a8c:10e:ad64:0:50"; };
                                          { "man2-4355-d49.hume.yt.gencfg-c.yandex.net"; 80; 630.000; "2a02:6b8:c0a:1e97:10e:ad64:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- hume
                          yt_odin_web = {
                            priority = 33;
                            match_fsm = {
                              host = "odin\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "odin";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["x-yt-omit-trailers"] = "true";
                                }; -- create
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "3s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "man1-7792-man-yt-odin-webservice-29550.gencfg-c.yandex.net"; 29550; 40.000; "2a02:6b8:c0b:1a8c:10c:b333:0:736e"; };
                                      { "sas1-4228-sas-yt-odin-webservice-13170.gencfg-c.yandex.net"; 13170; 40.000; "2a02:6b8:c08:7f11:10c:b335:0:3372"; };
                                      { "vla1-3033-vla-yt-odin-webservice-11200.gencfg-c.yandex.net"; 11200; 40.000; "2a02:6b8:c0d:3a0a:10c:a659:0:2bc0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "10s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_odin_web
                          yt_transfer_manager = {
                            priority = 32;
                            match_fsm = {
                              host = "transfer-manager\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "transfer_manager";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: transfer-manager.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-7351-man-yt-transfer-manager-27570.gencfg-c.yandex.net"; 27570; 160.000; "2a02:6b8:c0b:1ceb:10c:5f19:0:6bb2"; };
                                          { "sas2-3213-sas-yt-transfer-manager-9170.gencfg-c.yandex.net"; 9170; 160.000; "2a02:6b8:c08:c889:10c:b555:0:23d2"; };
                                          { "vla1-4565-vla-yt-transfer-manager-22350.gencfg-c.yandex.net"; 22350; 160.000; "2a02:6b8:c0d:3b95:10c:b558:0:574e"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: transfer-manager.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-7351-man-yt-transfer-manager-27570.gencfg-c.yandex.net"; 27570; 160.000; "2a02:6b8:c0b:1ceb:10c:5f19:0:6bb2"; };
                                          { "sas2-3213-sas-yt-transfer-manager-9170.gencfg-c.yandex.net"; 9170; 160.000; "2a02:6b8:c08:c889:10c:b555:0:23d2"; };
                                          { "vla1-4565-vla-yt-transfer-manager-22350.gencfg-c.yandex.net"; 22350; 160.000; "2a02:6b8:c0d:3b95:10c:b558:0:574e"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 4;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yt_transfer_manager
                          freud = {
                            priority = 31;
                            match_fsm = {
                              host = "(idm\\.)?freud\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "freud";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                log_headers = {
                                  name_re = "X-YT-Correlation-Id";
                                  regexp = {
                                    post_method = {
                                      priority = 2;
                                      match_fsm = {
                                        match = "(POST|PUT).*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        connection_attempts = 5;
                                        active = {
                                          delay = "1s";
                                          request = "GET /ping/ HTTP/1.1\nHost: freud.yt.yandex.net\n\n";
                                          steady = false;
                                          unpack(gen_proxy_backends({
                                            { "man3-1403-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3b9d:0:49b5:5584:0"; };
                                            { "man3-1744-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:2311:0:49b5:dc2f:0"; };
                                            { "man3-1782-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:2f0d:0:49b5:8c65:0"; };
                                            { "man3-1810-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:1625:0:49b5:bf3d:0"; };
                                            { "man3-1874-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:168f:0:49b5:a1ca:0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "5s";
                                            backend_timeout = "600s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- active
                                        on_error = {
                                          errordocument = {
                                            status = 503;
                                            file = "./503.json";
                                            force_conn_close = false;
                                            remain_headers = "X-Req-Id";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- post_method
                                    default = {
                                      priority = 1;
                                      balancer2 = {
                                        retry_policy = {
                                          unique_policy = {};
                                        }; -- retry_policy
                                        attempts = 5;
                                        active = {
                                          delay = "5s";
                                          request = "GET /ping/ HTTP/1.1\nHost: freud.yt.yandex.net\n\n";
                                          steady = false;
                                          unpack(gen_proxy_backends({
                                            { "man3-1403-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:3b9d:0:49b5:5584:0"; };
                                            { "man3-1744-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:2311:0:49b5:dc2f:0"; };
                                            { "man3-1782-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:2f0d:0:49b5:8c65:0"; };
                                            { "man3-1810-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:1625:0:49b5:bf3d:0"; };
                                            { "man3-1874-proxy-freud.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:168f:0:49b5:a1ca:0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "5s";
                                            backend_timeout = "600s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 16;
                                            need_resolve = true;
                                            status_code_blacklist = {
                                              "503";
                                            }; -- status_code_blacklist
                                          }))
                                        }; -- active
                                        on_error = {
                                          errordocument = {
                                            status = 503;
                                            file = "./503.json";
                                            force_conn_close = false;
                                            remain_headers = "X-Req-Id";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- default
                                  }; -- regexp
                                }; -- log_headers
                              }; -- headers
                            }; -- report
                          }; -- freud
                          yt_skynet_integration = {
                            priority = 30;
                            match_fsm = {
                              host = "skynet-manager\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yt_skynet_integration";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["x-yt-omit-trailers"] = "true";
                                }; -- create
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "60s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "yt-skynet-manager-2.man.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c1a:2e1f:10d:cd5f:199b:0"; };
                                      { "yt-skynet-manager-2.sas.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c1c:2aa7:10d:cd5f:de1d:0"; };
                                      { "yt-skynet-manager-2.vla.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c1d:2183:10d:cd5f:f1f5:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_skynet_integration
                          banach = {
                            priority = 29;
                            match_fsm = {
                              host = "(idm\\.)?banach\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "banach";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 7;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: banach.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3010:e61d:2dff:fe6d:1970"; };
                                          { "c02i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3031:92e2:baff:fea1:7bc4"; };
                                          { "c03i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3001:feaa:14ff:fea7:ba91"; };
                                          { "c04i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3036:92e2:baff:fea1:7dc8"; };
                                          { "c05i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3020:92e2:baff:fe9c:6ed6"; };
                                          { "c06i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3066:feaa:14ff:fea7:bcc1"; };
                                          { "c07i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3068:feaa:14ff:fea7:b958"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 7;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: banach.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3010:e61d:2dff:fe6d:1970"; };
                                          { "c02i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3031:92e2:baff:fea1:7bc4"; };
                                          { "c03i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3001:feaa:14ff:fea7:ba91"; };
                                          { "c04i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3036:92e2:baff:fea1:7dc8"; };
                                          { "c05i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3020:92e2:baff:fe9c:6ed6"; };
                                          { "c06i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3066:feaa:14ff:fea7:bcc1"; };
                                          { "c07i.banach.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:3068:feaa:14ff:fea7:b958"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- banach
                          hahn = {
                            priority = 28;
                            match_fsm = {
                              host = "(idm\\.)?hahn\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "hahn";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 12;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: hahn.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a6f:feaa:14ff:fed9:3a79"; };
                                          { "c02-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a54:feaa:14ff:fed9:3a3b"; };
                                          { "c03-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a2f:c654:44ff:fe23:c6bc"; };
                                          { "c04-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a6e:c654:44ff:fe23:c996"; };
                                          { "c05-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a1c:e61d:2dff:fe13:6560"; };
                                          { "c06-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a46:f652:14ff:fe7a:8600"; };
                                          { "n0041-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a26:feaa:14ff:fed9:3930"; };
                                          { "n0043-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a70:feaa:14ff:fed9:3b28"; };
                                          { "n0044-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a70:feaa:14ff:fed9:3b87"; };
                                          { "n0107-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a6f:feaa:14ff:fed9:38cf"; };
                                          { "n0133-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a54:feaa:14ff:fed9:3b81"; };
                                          { "n0219-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a3c:feaa:14ff:fed9:3944"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 12;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: hahn.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a6f:feaa:14ff:fed9:3a79"; };
                                          { "c02-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a54:feaa:14ff:fed9:3a3b"; };
                                          { "c03-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a2f:c654:44ff:fe23:c6bc"; };
                                          { "c04-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a6e:c654:44ff:fe23:c996"; };
                                          { "c05-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a1c:e61d:2dff:fe13:6560"; };
                                          { "c06-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a46:f652:14ff:fe7a:8600"; };
                                          { "n0041-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a26:feaa:14ff:fed9:3930"; };
                                          { "n0043-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a70:feaa:14ff:fed9:3b28"; };
                                          { "n0044-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a70:feaa:14ff:fed9:3b87"; };
                                          { "n0107-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a6f:feaa:14ff:fed9:38cf"; };
                                          { "n0133-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a54:feaa:14ff:fed9:3b81"; };
                                          { "n0219-sas.hahn.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a3c:feaa:14ff:fed9:3944"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- hahn
                          seneca_man = {
                            priority = 27;
                            match_fsm = {
                              host = "(idm\\.)?seneca-man\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "seneca-man";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-man.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01i-25f.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a18:10d:f651:0:50"; };
                                          { "m02i-7b9.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a12:10d:f651:0:50"; };
                                          { "m03i-742.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a10:10d:f651:0:50"; };
                                          { "m04i-94b.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a0b:10d:f651:0:50"; };
                                          { "m05i-620.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a09:10d:f651:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-man.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01i-25f.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a18:10d:f651:0:50"; };
                                          { "m02i-7b9.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a12:10d:f651:0:50"; };
                                          { "m03i-742.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a10:10d:f651:0:50"; };
                                          { "m04i-94b.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a0b:10d:f651:0:50"; };
                                          { "m05i-620.seneca-man.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c09:3a09:10d:f651:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- seneca_man
                          seneca_myt = {
                            priority = 26;
                            match_fsm = {
                              host = "(idm\\.)?seneca-myt\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "seneca-myt";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-myt.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:3eb4"; };
                                          { "m02-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:37fc"; };
                                          { "m03-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:3aa6"; };
                                          { "m04-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:3ac6"; };
                                          { "m05-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:39fc"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-myt.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:3eb4"; };
                                          { "m02-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:37fc"; };
                                          { "m03-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:3aa6"; };
                                          { "m04-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:3ac6"; };
                                          { "m05-myt.seneca-myt.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5200:92e2:baff:fea2:39fc"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- seneca_myt
                          seneca_sas = {
                            priority = 25;
                            match_fsm = {
                              host = "(idm\\.)?seneca-sas\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "seneca-sas";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-sas.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01h-9fb.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:aa2:10d:c945:0:50"; };
                                          { "m02h-f0f.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:a02:10d:c945:0:50"; };
                                          { "m03h-cae.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:78d:10d:c945:0:50"; };
                                          { "m04h-d48.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:722:10d:c945:0:50"; };
                                          { "m05h-3a5.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:79e:10d:c945:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-sas.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01h-9fb.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:aa2:10d:c945:0:50"; };
                                          { "m02h-f0f.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:a02:10d:c945:0:50"; };
                                          { "m03h-cae.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:78d:10d:c945:0:50"; };
                                          { "m04h-d48.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:722:10d:c945:0:50"; };
                                          { "m05h-3a5.seneca-sas.yt.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c16:79e:10d:c945:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- seneca_sas
                          zeno = {
                            priority = 24;
                            match_fsm = {
                              host = "(idm\\.)?zeno\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "zeno";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: zeno.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:400:feaa:14ff:fedd:f193"; };
                                          { "m02i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:401:feaa:14ff:fedd:f087"; };
                                          { "m03i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:401:feaa:14ff:fedd:f08c"; };
                                          { "m04i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:401:feaa:14ff:fedd:f08a"; };
                                          { "m05i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:400:feaa:14ff:fedd:f0b3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: zeno.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:400:feaa:14ff:fedd:f193"; };
                                          { "m02i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:401:feaa:14ff:fedd:f087"; };
                                          { "m03i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:401:feaa:14ff:fedd:f08c"; };
                                          { "m04i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:401:feaa:14ff:fedd:f08a"; };
                                          { "m05i.zeno.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:400:feaa:14ff:fedd:f0b3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- zeno
                          markov = {
                            priority = 23;
                            match_fsm = {
                              host = "(idm\\.)?markov\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "markov";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: markov.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-man.markov.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:4f00:215:b2ff:feaa:31a"; };
                                          { "m02-sas.markov.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a41:92e2:baff:fea2:31b6"; };
                                          { "m03-myt.markov.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5102:215:b2ff:fea9:66be"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: markov.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-man.markov.yt.yandex.net"; 80; 1.000; "2a02:6b8:b011:4f00:215:b2ff:feaa:31a"; };
                                          { "m02-sas.markov.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a41:92e2:baff:fea2:31b6"; };
                                          { "m03-myt.markov.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5102:215:b2ff:fea9:66be"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- markov
                          perelman = {
                            priority = 22;
                            match_fsm = {
                              host = "(idm\\.)?perelman\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "perelman";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 2;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: perelman.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01i.perelman.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:300c:feaa:14ff:fea7:ba22"; };
                                          { "c02i.perelman.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:300a:feaa:14ff:fea7:ba81"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 2;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: perelman.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01i.perelman.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:300c:feaa:14ff:fea7:ba22"; };
                                          { "c02i.perelman.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:300a:feaa:14ff:fea7:ba81"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- perelman
                          vanga = {
                            priority = 21;
                            match_fsm = {
                              host = "(idm\\.)?vanga\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "vanga";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: vanga.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m02i.vanga.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:322a:e61d:2dff:fe6d:cb0"; };
                                          { "m03f.vanga.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5000:225:90ff:fe92:44f4"; };
                                          { "m04h.vanga.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a2c:76d4:35ff:fe61:61fd"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: vanga.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m02i.vanga.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:322a:e61d:2dff:fe6d:cb0"; };
                                          { "m03f.vanga.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:5000:225:90ff:fe92:44f4"; };
                                          { "m04h.vanga.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a2c:76d4:35ff:fe61:61fd"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- vanga
                          pythia = {
                            priority = 20;
                            match_fsm = {
                              host = "(idm\\.)?pythia\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "pythia";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: pythia.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-sas-f13.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c10:108a:10d:f7ef:0:50"; };
                                          { "m02-myt-5b7.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c00:419f:10d:f7f8:0:50"; };
                                          { "m03-man-4c5.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0a:1683:10d:f7ea:0:50"; };
                                          { "m04-sas-e01.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c10:148f:10d:f7ef:0:50"; };
                                          { "m05-man-17d.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0a:1605:10d:f7ea:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: pythia.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-sas-f13.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c10:108a:10d:f7ef:0:50"; };
                                          { "m02-myt-5b7.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c00:419f:10d:f7f8:0:50"; };
                                          { "m03-man-4c5.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0a:1683:10d:f7ea:0:50"; };
                                          { "m04-sas-e01.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c10:148f:10d:f7ef:0:50"; };
                                          { "m05-man-17d.pythia.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0a:1605:10d:f7ea:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- pythia
                          locke = {
                            priority = 19;
                            match_fsm = {
                              host = "(idm\\.)?locke\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "locke";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 9;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: locke.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man3-4083-proxy-locke.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:37a5:0:549:d048:0"; };
                                          { "man3-4085-proxy-locke.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:37b7:0:549:6f49:0"; };
                                          { "man3-4086-proxy-locke.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:37bd:0:549:e2d7:0"; };
                                          { "sas5-5400-proxy-locke.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:1923:0:549:94c4:0"; };
                                          { "sas5-5402-proxy-locke.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:190f:0:549:cf82:0"; };
                                          { "sas5-5403-proxy-locke.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:190d:0:549:a99f:0"; };
                                          { "vla1-0061-proxy-locke.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2db9:0:549:b3d6:0"; };
                                          { "vla1-0062-proxy-locke.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2db1:0:549:538f:0"; };
                                          { "vla2-2924-proxy-locke.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0f:38a:0:549:f70f:0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 9;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: locke.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man3-4083-proxy-locke.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:37a5:0:549:d048:0"; };
                                          { "man3-4085-proxy-locke.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:37b7:0:549:6f49:0"; };
                                          { "man3-4086-proxy-locke.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:37bd:0:549:e2d7:0"; };
                                          { "sas5-5400-proxy-locke.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:1923:0:549:94c4:0"; };
                                          { "sas5-5402-proxy-locke.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:190f:0:549:cf82:0"; };
                                          { "sas5-5403-proxy-locke.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:190d:0:549:a99f:0"; };
                                          { "vla1-0061-proxy-locke.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2db9:0:549:b3d6:0"; };
                                          { "vla1-0062-proxy-locke.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2db1:0:549:538f:0"; };
                                          { "vla2-2924-proxy-locke.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0f:38a:0:549:f70f:0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- locke
                          flux = {
                            priority = 18;
                            match_fsm = {
                              host = "(idm\\.)?flux\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "flux";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: flux.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "iva01.flux.yt.yandex.net"; 80; 1.000; "2a02:6b8:0:161c:f66d:4ff:fe2b:a988"; };
                                          { "man01.flux.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:320c:feaa:14ff:fedd:ef3b"; };
                                          { "sas10.flux.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a1f:76d4:35ff:feca:7130"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: flux.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "iva01.flux.yt.yandex.net"; 80; 1.000; "2a02:6b8:0:161c:f66d:4ff:fe2b:a988"; };
                                          { "man01.flux.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:320c:feaa:14ff:fedd:ef3b"; };
                                          { "sas10.flux.yt.yandex.net"; 80; 1.000; "2a02:6b8:b040:1a1f:76d4:35ff:feca:7130"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- flux
                          socrates = {
                            priority = 17;
                            match_fsm = {
                              host = "(idm\\.)?socrates\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "socrates";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 6;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: socrates.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-man-089.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:4911:10e:b15:0:50"; };
                                          { "m02-man-77f.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:5113:10e:b15:0:50"; };
                                          { "m03-man-095.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:491a:10e:b15:0:50"; };
                                          { "m11-man-172.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:5111:10e:b15:0:50"; };
                                          { "m12-man-2b0.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:491b:10e:b15:0:50"; };
                                          { "m13-man-c04.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:5119:10e:b15:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 6;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: socrates.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-man-089.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:4911:10e:b15:0:50"; };
                                          { "m02-man-77f.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:5113:10e:b15:0:50"; };
                                          { "m03-man-095.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:491a:10e:b15:0:50"; };
                                          { "m11-man-172.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:5111:10e:b15:0:50"; };
                                          { "m12-man-2b0.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:491b:10e:b15:0:50"; };
                                          { "m13-man-c04.socrates.yt.gencfg-c.yandex.net"; 80; 160.000; "2a02:6b8:c0b:5119:10e:b15:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- socrates
                          yp_sas = {
                            priority = 16;
                            match_fsm = {
                              host = "(idm\\.)?yp-sas\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-sas";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-sas.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "sas2-5082.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:44c:0:42ba:5e19:497c"; };
                                          { "sas2-5083.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:42b:0:42ba:5e19:48aa"; };
                                          { "sas2-5084.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:435:0:42ba:5e19:48f3"; };
                                          { "sas2-5085.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:43d:0:42ba:5e19:492b"; };
                                          { "sas2-5086.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:429:0:42ba:5e18:e6d"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-sas.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "sas2-5082.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:44c:0:42ba:5e19:497c"; };
                                          { "sas2-5083.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:42b:0:42ba:5e19:48aa"; };
                                          { "sas2-5084.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:435:0:42ba:5e19:48f3"; };
                                          { "sas2-5085.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:43d:0:42ba:5e19:492b"; };
                                          { "sas2-5086.yp.yandex.net"; 80; 160.000; "2a02:6b8:c02:429:0:42ba:5e18:e6d"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_sas
                          yt_transfer_manager_userdata = {
                            priority = 15;
                            match_fsm = {
                              host = "tm-userdata\\.yt\\.yandex(-team\\.ru|\\.net)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "transfer_manager_userdata";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: tm-userdata.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-0382-man-yt-transfer-manager-266-23825.gencfg-c.yandex.net"; 23825; 160.000; "2a02:6b8:c0b:43b1:10d:1315:0:5d11"; };
                                          { "sas1-2834-sas-yt-transfer-manager-a75-24795.gencfg-c.yandex.net"; 24795; 160.000; "2a02:6b8:c08:41a:10d:1317:0:60db"; };
                                          { "vla1-1529-vla-yt-transfer-manager-21d-23905.gencfg-c.yandex.net"; 23905; 160.000; "2a02:6b8:c0d:3512:10d:1319:0:5d61"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: tm-userdata.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-0382-man-yt-transfer-manager-266-23825.gencfg-c.yandex.net"; 23825; 160.000; "2a02:6b8:c0b:43b1:10d:1315:0:5d11"; };
                                          { "sas1-2834-sas-yt-transfer-manager-a75-24795.gencfg-c.yandex.net"; 24795; 160.000; "2a02:6b8:c08:41a:10d:1317:0:60db"; };
                                          { "vla1-1529-vla-yt-transfer-manager-21d-23905.gencfg-c.yandex.net"; 23905; 160.000; "2a02:6b8:c0d:3512:10d:1319:0:5d61"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 4;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yt_transfer_manager_userdata
                          yt_transfer_manager_testing = {
                            priority = 14;
                            match_fsm = {
                              host = "tm-testing\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "transfer_manager_testing";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 1;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: tm-testing.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-5025-man-yt-transfer-manager-8c7-24805.gencfg-c.yandex.net"; 24805; 160.000; "2a02:6b8:c0b:13a4:10c:b547:0:60e5"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 1;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: tm-testing.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-5025-man-yt-transfer-manager-8c7-24805.gencfg-c.yandex.net"; 24805; 160.000; "2a02:6b8:c0b:13a4:10c:b547:0:60e5"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 4;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yt_transfer_manager_testing
                          bohr = {
                            priority = 13;
                            match_fsm = {
                              host = "(idm\\.)?bohr\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "bohr";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 2;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: bohr.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01-sas.bohr.yt.yandex.net"; 80; 1.000; "2a02:6b8:c02:433:0:4396:bc0c:6f29"; };
                                          { "c02-sas.bohr.yt.yandex.net"; 80; 1.000; "2a02:6b8:c02:439:0:4396:506e:fcf2"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 2;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: bohr.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "c01-sas.bohr.yt.yandex.net"; 80; 1.000; "2a02:6b8:c02:433:0:4396:bc0c:6f29"; };
                                          { "c02-sas.bohr.yt.yandex.net"; 80; 1.000; "2a02:6b8:c02:439:0:4396:506e:fcf2"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- bohr
                          yp_man = {
                            priority = 12;
                            match_fsm = {
                              host = "(idm\\.)?yp-man\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-man";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-man.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man3-0432.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:761:0:604:dbc:a268"; };
                                          { "man3-0433.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:838:0:604:5cf5:be5f"; };
                                          { "man3-0436.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:764:0:604:dbc:a456"; };
                                          { "man3-0438.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:75f:0:604:dbc:a2fd"; };
                                          { "man3-0439.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:75e:0:604:dbc:a2e3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-man.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man3-0432.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:761:0:604:dbc:a268"; };
                                          { "man3-0433.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:838:0:604:5cf5:be5f"; };
                                          { "man3-0436.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:764:0:604:dbc:a456"; };
                                          { "man3-0438.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:75f:0:604:dbc:a2fd"; };
                                          { "man3-0439.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:75e:0:604:dbc:a2e3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_man
                          yp_man_pre = {
                            priority = 11;
                            match_fsm = {
                              host = "(idm\\.)?yp-man-pre\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-man-pre";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 3;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-man-pre.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-8965.yp.yandex.net"; 80; 80.000; "2a02:6b8:c01:4c:0:604:2d00:97d0"; };
                                          { "man1-8979.yp.yandex.net"; 80; 80.000; "2a02:6b8:c01:80:0:604:2d04:1970"; };
                                          { "man2-1671.yp.yandex.net"; 80; 80.000; "2a02:6b8:c01:7e:0:604:14f5:cfd0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 3;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-man-pre.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-8965.yp.yandex.net"; 80; 80.000; "2a02:6b8:c01:4c:0:604:2d00:97d0"; };
                                          { "man1-8979.yp.yandex.net"; 80; 80.000; "2a02:6b8:c01:80:0:604:2d04:1970"; };
                                          { "man2-1671.yp.yandex.net"; 80; 80.000; "2a02:6b8:c01:7e:0:604:14f5:cfd0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_man_pre
                          arnold = {
                            priority = 10;
                            match_fsm = {
                              host = "(idm\\.)?arnold\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "arnold";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 7;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: arnold.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "vla1-8002-1ff-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:f1f:10d:5b4d:0:50"; };
                                          { "vla1-8032-7ec-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:d84:10d:5b4d:0:50"; };
                                          { "vla1-8166-f25-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:1195:10d:5b4d:0:50"; };
                                          { "vla1-8997-10b-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:e11:10d:5b4d:0:50"; };
                                          { "vla1-9054-8e5-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:d08:10d:5b4d:0:50"; };
                                          { "vla1-9080-60d-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:109c:10d:5b4d:0:50"; };
                                          { "vla1-9596-4d0-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:e85:10d:5b4d:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 7;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: arnold.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "vla1-8002-1ff-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:f1f:10d:5b4d:0:50"; };
                                          { "vla1-8032-7ec-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:d84:10d:5b4d:0:50"; };
                                          { "vla1-8166-f25-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:1195:10d:5b4d:0:50"; };
                                          { "vla1-8997-10b-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:e11:10d:5b4d:0:50"; };
                                          { "vla1-9054-8e5-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:d08:10d:5b4d:0:50"; };
                                          { "vla1-9080-60d-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:109c:10d:5b4d:0:50"; };
                                          { "vla1-9596-4d0-vla-yt-arnold-contr-827-80.gencfg-c.yandex.net"; 80; 2320.000; "2a02:6b8:c15:e85:10d:5b4d:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- arnold
                          yp_sas_test = {
                            priority = 9;
                            match_fsm = {
                              host = "(idm\\.)?yp-sas-test\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-sas-test";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-sas-test.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "sas1-5535.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:43d:0:604:5e19:4a42"; };
                                          { "sas1-5546.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:44c:0:604:5e19:4883"; };
                                          { "sas3-2980.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:c17:0:604:5cf5:b0aa"; };
                                          { "sas3-2981.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:c17:0:604:5cf4:8a71"; };
                                          { "sas3-2982.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:c17:0:604:5cf4:9158"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-sas-test.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "sas1-5535.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:43d:0:604:5e19:4a42"; };
                                          { "sas1-5546.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:44c:0:604:5e19:4883"; };
                                          { "sas3-2980.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:c17:0:604:5cf5:b0aa"; };
                                          { "sas3-2981.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:c17:0:604:5cf4:8a71"; };
                                          { "sas3-2982.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:c17:0:604:5cf4:9158"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_sas_test
                          yp_iva = {
                            priority = 8;
                            match_fsm = {
                              host = "(idm\\.)?yp-iva\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-iva";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-iva.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "iva1-6956.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1a0:0:604:c935:e8c0"; };
                                          { "iva1-6957.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1c9:0:604:c935:e290"; };
                                          { "iva1-6958.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1a2:0:604:c935:e7c0"; };
                                          { "iva1-6959.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1a5:0:604:143e:d50"; };
                                          { "iva1-6960.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:27c:0:604:c935:ece0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-iva.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "iva1-6956.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1a0:0:604:c935:e8c0"; };
                                          { "iva1-6957.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1c9:0:604:c935:e290"; };
                                          { "iva1-6958.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1a2:0:604:c935:e7c0"; };
                                          { "iva1-6959.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:1a5:0:604:143e:d50"; };
                                          { "iva1-6960.yp.yandex.net"; 80; 40.000; "2a02:6b8:c04:27c:0:604:c935:ece0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_iva
                          yp_vla = {
                            priority = 7;
                            match_fsm = {
                              host = "(idm\\.)?yp-vla\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-vla";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-vla.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "vla1-6056.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:9e:0:604:d8f:eb25"; };
                                          { "vla1-6057.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:9a:0:604:db7:a87c"; };
                                          { "vla1-6058.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:1f:0:604:db7:9fde"; };
                                          { "vla1-6059.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:97:0:604:db7:a7bf"; };
                                          { "vla1-6060.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:1f:0:604:db7:9c5a"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-vla.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "vla1-6056.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:9e:0:604:d8f:eb25"; };
                                          { "vla1-6057.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:9a:0:604:db7:a87c"; };
                                          { "vla1-6058.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:1f:0:604:db7:9fde"; };
                                          { "vla1-6059.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:97:0:604:db7:a7bf"; };
                                          { "vla1-6060.yp.yandex.net"; 80; 160.000; "2a02:6b8:c0e:1f:0:604:db7:9c5a"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_vla
                          seneca_vla = {
                            priority = 6;
                            match_fsm = {
                              host = "(idm\\.)?seneca-vla\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "seneca-vla";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-vla.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-vla-335.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:3313:10d:6ae1:0:50"; };
                                          { "m02-vla-9d5.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0f:4195:10d:6ae1:0:50"; };
                                          { "m03-vla-3ee.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:309e:10d:6ae1:0:50"; };
                                          { "m04-vla-306.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:331a:10d:6ae1:0:50"; };
                                          { "m05-vla-123.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0f:5793:10d:6ae1:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: seneca-vla.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-vla-335.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:3313:10d:6ae1:0:50"; };
                                          { "m02-vla-9d5.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0f:4195:10d:6ae1:0:50"; };
                                          { "m03-vla-3ee.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:309e:10d:6ae1:0:50"; };
                                          { "m04-vla-306.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:331a:10d:6ae1:0:50"; };
                                          { "m05-vla-123.seneca-vla.yt.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0f:5793:10d:6ae1:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- seneca_vla
                          yt_solomon_resolver = {
                            priority = 5;
                            match_fsm = {
                              host = "solomon-resolver\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yt_solomon_resolver";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["x-yt-omit-trailers"] = "true";
                                }; -- create
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "60s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "44qmd2yjntoeagnm.vla.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c0d:4a15:0:44e8:d5e3:0"; };
                                      { "u3mfiopr2q6j2eo4.sas.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c08:828c:0:44e8:a7e2:0"; };
                                      { "xkbhubax2gkcw7dm.man.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c1a:591:0:44e8:8821:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- headers
                            }; -- report
                          }; -- yt_solomon_resolver
                          landau = {
                            priority = 4;
                            match_fsm = {
                              host = "(idm\\.)?landau\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "landau";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: landau.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-vla-bfb-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:3213:10d:c97a:0:50"; };
                                          { "m02-vla-4dd-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:2492:10d:c97a:0:50"; };
                                          { "m03-vla-620-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:d18:10d:c97a:0:50"; };
                                          { "m04-vla-400-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:2522:10d:c97a:0:50"; };
                                          { "m05-vla-14e-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:1289:10d:c97a:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: landau.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "m01-vla-bfb-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:3213:10d:c97a:0:50"; };
                                          { "m02-vla-4dd-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:2492:10d:c97a:0:50"; };
                                          { "m03-vla-620-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:d18:10d:c97a:0:50"; };
                                          { "m04-vla-400-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:2522:10d:c97a:0:50"; };
                                          { "m05-vla-14e-vla-yt-landau-default-29c-80.gencfg-c.yandex.net"; 80; 320.000; "2a02:6b8:c0d:1289:10d:c97a:0:50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- landau
                          yp_xdc = {
                            priority = 3;
                            match_fsm = {
                              host = "(idm\\.)?yp-xdc\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "yp-xdc";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                regexp = {
                                  post_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "(POST|PUT).*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      connection_attempts = 5;
                                      active = {
                                        delay = "1s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-xdc.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-7465.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:738:0:604:b2a9:714e"; };
                                          { "man2-7775.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:709:0:604:d83:e225"; };
                                          { "sas1-7851.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:676:0:604:5cf5:b94d"; };
                                          { "sas1-7924.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:67e:0:604:5cf6:5d1"; };
                                          { "vla2-9104.yp.yandex.net"; 80; 40.000; "2a02:6b8:c0e:ac:0:604:db7:a4a4"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- post_method
                                  default = {
                                    priority = 1;
                                    balancer2 = {
                                      retry_policy = {
                                        unique_policy = {};
                                      }; -- retry_policy
                                      attempts = 5;
                                      active = {
                                        delay = "5s";
                                        request = "GET /ping/ HTTP/1.1\nHost: yp-xdc.yt.yandex.net\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-7465.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:738:0:604:b2a9:714e"; };
                                          { "man2-7775.yp.yandex.net"; 80; 40.000; "2a02:6b8:c01:709:0:604:d83:e225"; };
                                          { "sas1-7851.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:676:0:604:5cf5:b94d"; };
                                          { "sas1-7924.yp.yandex.net"; 80; 40.000; "2a02:6b8:c02:67e:0:604:5cf6:5d1"; };
                                          { "vla2-9104.yp.yandex.net"; 80; 40.000; "2a02:6b8:c0e:ac:0:604:db7:a4a4"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "5s";
                                          backend_timeout = "600s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 16;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "503";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- active
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          file = "./503.json";
                                          force_conn_close = false;
                                          remain_headers = "X-Req-Id";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- default
                                }; -- regexp
                              }; -- headers
                            }; -- report
                          }; -- yp_xdc
                          walle_cms_testing = {
                            priority = 2;
                            match_fsm = {
                              host = "walle-cms-testing.yt.yandex-team.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "walle_cms_testing";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                balancer2 = {
                                  timeout_policy = {
                                    timeout = "60s";
                                    unique_policy = {};
                                  }; -- timeout_policy
                                  attempts = 2;
                                  weighted2 = {
                                    slow_reply_time = "1s";
                                    correction_params = {
                                      max_weight = 5.000;
                                      min_weight = 0.050;
                                      history_time = "60s";
                                      feedback_time = "30s";
                                      plus_diff_per_sec = 0.050;
                                      minus_diff_per_sec = 0.050;
                                    }; -- correction_params
                                    unpack(gen_proxy_backends({
                                      { "yt-cms-testing-1.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1a:25bf:0:47f8:a979:0"; };
                                      { "yt-cms-testing-2.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:43af:0:47f8:dbf2:0"; };
                                      { "yt-cms-testing-3.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:40b8:0:47f8:f946:0"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "1s";
                                      backend_timeout = "60s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 16;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                  attempts_rate_limiter = {
                                    limit = 0.300;
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
                              }; -- headers
                            }; -- report
                          }; -- walle_cms_testing
                          ofd_xdc = {
                            priority = 1;
                            match_fsm = {
                              host = "(idm\\.)?ofd-xdc\\.yt\\.yandex(\\.net|-team\\.ru)(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "ofd-xdc";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create_func_weak = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Scheme"] = "scheme";
                                  ["X-Source-Port-Y"] = "realport";
                                }; -- create_func_weak
                                create = {
                                  ["X-YT-Omit-Trailers"] = "true";
                                }; -- create
                                log_headers = {
                                  name_re = "X-YT-Correlation-Id";
                                  regexp = {
                                    post_method = {
                                      priority = 2;
                                      match_fsm = {
                                        match = "(POST|PUT).*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        connection_attempts = 6;
                                        active = {
                                          delay = "1s";
                                          request = "GET /ping/ HTTP/1.1\nHost: ofd-xdc.yt.yandex.net\n\n";
                                          steady = false;
                                          unpack(gen_proxy_backends({
                                            { "myt1-1588-proxy-ofd-xdc.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:1914:0:4c5d:c17b:0"; };
                                            { "myt1-5321-proxy-ofd-xdc.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:1919:0:4c5d:4fa3:0"; };
                                            { "sas3-0495-proxy-ofd-xdc.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:604:0:4c5d:ee61:0"; };
                                            { "sas3-0496-proxy-ofd-xdc.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:615:0:4c5d:5f25:0"; };
                                            { "vla1-0051-proxy-ofd-xdc.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2dc3:0:4c5d:eee1:0"; };
                                            { "vla1-0052-proxy-ofd-xdc.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2db3:0:4c5d:b66e:0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "5s";
                                            backend_timeout = "600s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- active
                                        on_error = {
                                          errordocument = {
                                            status = 503;
                                            file = "./503.json";
                                            force_conn_close = false;
                                            remain_headers = "X-Req-Id";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- post_method
                                    default = {
                                      priority = 1;
                                      balancer2 = {
                                        retry_policy = {
                                          unique_policy = {};
                                        }; -- retry_policy
                                        attempts = 6;
                                        active = {
                                          delay = "5s";
                                          request = "GET /ping/ HTTP/1.1\nHost: ofd-xdc.yt.yandex.net\n\n";
                                          steady = false;
                                          unpack(gen_proxy_backends({
                                            { "myt1-1588-proxy-ofd-xdc.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:1914:0:4c5d:c17b:0"; };
                                            { "myt1-5321-proxy-ofd-xdc.myt.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c12:1919:0:4c5d:4fa3:0"; };
                                            { "sas3-0495-proxy-ofd-xdc.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:604:0:4c5d:ee61:0"; };
                                            { "sas3-0496-proxy-ofd-xdc.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1b:615:0:4c5d:5f25:0"; };
                                            { "vla1-0051-proxy-ofd-xdc.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2dc3:0:4c5d:eee1:0"; };
                                            { "vla1-0052-proxy-ofd-xdc.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2db3:0:4c5d:b66e:0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "5s";
                                            backend_timeout = "600s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 16;
                                            need_resolve = true;
                                            status_code_blacklist = {
                                              "503";
                                            }; -- status_code_blacklist
                                          }))
                                        }; -- active
                                        on_error = {
                                          errordocument = {
                                            status = 503;
                                            file = "./503.json";
                                            force_conn_close = false;
                                            remain_headers = "X-Req-Id";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- default
                                  }; -- regexp
                                }; -- log_headers
                              }; -- headers
                            }; -- report
                          }; -- ofd_xdc
                        }; -- regexp
                      }; -- log_headers
                    }; -- headers
                  }; -- shared
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_14731 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14731;
      }; -- ports
      shared = {
        uuid = "5588931940787760461";
      }; -- shared
    }; -- https_section_14731
  }; -- ipdispatch
}