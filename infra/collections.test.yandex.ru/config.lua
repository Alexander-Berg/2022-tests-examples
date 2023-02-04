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
  log = get_log_path("childs_log", 14590, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 14590;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 14590;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 14590;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::109c";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14591;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14591;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::109c";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14590;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14590;
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
        14590;
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
        14590;
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
    https_section_443 = {
      ips = {
        "2a02:6b8:0:3400::109c";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "4363047024904990289";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14591, "/place/db/www/logs");
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
                log = get_log_path("ssl_sni", 14591, "/place/db/www/logs");
                priv = get_private_cert_path("collections.test.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-collections.test.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.collections.test.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.collections.test.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.collections.test.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 14591, "/place/db/www/logs");
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
                  threshold = {
                    lo_bytes = 1048576;
                    hi_bytes = 10485760;
                    recv_timeout = "1s";
                    pass_timeout = "10s";
                    headers = {
                      create_func = {
                        ["X-Collections-Req-Id"] = "reqid";
                        ["X-Forwarded-For-Y"] = "realip";
                      }; -- create_func
                      create_func_weak = {
                        ["X-Req-Id"] = "reqid";
                      }; -- create_func_weak
                      response_headers = {
                        delete = "uWSGI-encoding|uwsgi-encoding";
                        regexp = {
                          slbping = {
                            priority = 5;
                            match_fsm = {
                              URI = "/slb_ping";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            shared = {
                              uuid = "1683180570280210434";
                            }; -- shared
                          }; -- slbping
                          priemka = {
                            priority = 4;
                            match_fsm = {
                              host = "priemka.collections.test.yandex.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            response_headers = {
                              create = {
                                ["X-Section"] = "https:priemka";
                              }; -- create
                              regexp = {
                                priemka_upstream_api = {
                                  priority = 2;
                                  match_fsm = {
                                    URI = "(/collections)?/api(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  rewrite = {
                                    actions = {
                                      {
                                        global = false;
                                        literal = false;
                                        case_insensitive = false;
                                        rewrite = "%1%3captcha=1%4";
                                        regexp = "(/collections)?(/api)(.*)captcha=1(.*)?";
                                      };
                                    }; -- actions
                                    rewrite = {
                                      actions = {
                                        {
                                          global = false;
                                          literal = false;
                                          case_insensitive = false;
                                          rewrite = "%1/api%2captcha=1%3";
                                          regexp = "(/collections)?(/[^a].*)captcha=1(.*)?";
                                        };
                                      }; -- actions
                                      icookie = {
                                        use_default_keys = true;
                                        domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                        trust_parent = false;
                                        trust_children = false;
                                        enable_set_cookie = true;
                                        enable_decrypting = true;
                                        decrypted_uid_header = "X-Yandex-ICookie";
                                        error_header = "X-Yandex-ICookie-Error";
                                        report = {
                                          uuid = "api";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          regexp = {
                                            post_method = {
                                              priority = 2;
                                              match_fsm = {
                                                match = "POST.*";
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                              balancer2 = {
                                                timeout_policy = {
                                                  timeout = "1s";
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
                                                    { "man1-1110-man-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0b:1feb:10d:61d:0:564f"; };
                                                    { "man1-8386-man-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0b:1c18:10d:61d:0:564f"; };
                                                    { "sas1-2982-sas-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c08:2ba1:10d:61e:0:564f"; };
                                                    { "sas1-5977-sas-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c08:192b:10d:61e:0:564f"; };
                                                    { "vla1-0956-vla-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0d:440d:10d:61f:0:564f"; };
                                                    { "vla1-4565-vla-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0d:3b95:10d:61f:0:564f"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "100ms";
                                                    backend_timeout = "4s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 1;
                                                    need_resolve = true;
                                                  }))
                                                }; -- weighted2
                                                on_error = {
                                                  errordocument = {
                                                    status = 504;
                                                    force_conn_close = false;
                                                    content = "Gateway Timeout";
                                                  }; -- errordocument
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- post_method
                                            default = {
                                              priority = 1;
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 3;
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
                                                    { "man1-1110-man-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0b:1feb:10d:61d:0:564f"; };
                                                    { "man1-8386-man-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0b:1c18:10d:61d:0:564f"; };
                                                    { "sas1-2982-sas-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c08:2ba1:10d:61e:0:564f"; };
                                                    { "sas1-5977-sas-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c08:192b:10d:61e:0:564f"; };
                                                    { "vla1-0956-vla-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0d:440d:10d:61f:0:564f"; };
                                                    { "vla1-4565-vla-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0d:3b95:10d:61f:0:564f"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "100ms";
                                                    backend_timeout = "4s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 1;
                                                    need_resolve = true;
                                                  }))
                                                }; -- weighted2
                                                on_error = {
                                                  errordocument = {
                                                    status = 504;
                                                    force_conn_close = false;
                                                    content = "Gateway Timeout";
                                                  }; -- errordocument
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- default
                                          }; -- regexp
                                        }; -- report
                                      }; -- icookie
                                    }; -- rewrite
                                  }; -- rewrite
                                }; -- priemka_upstream_api
                                default = {
                                  priority = 1;
                                  icookie = {
                                    use_default_keys = true;
                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                    trust_parent = false;
                                    trust_children = false;
                                    enable_set_cookie = true;
                                    enable_decrypting = true;
                                    decrypted_uid_header = "X-Yandex-ICookie";
                                    error_header = "X-Yandex-ICookie-Error";
                                    report = {
                                      uuid = "nodejs";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      hasher = {
                                        mode = "text";
                                        balancer2 = {
                                          active_policy = {
                                            unique_policy = {};
                                          }; -- active_policy
                                          attempts = 3;
                                          hashing = {
                                            delay = "30s";
                                            request = "GET /version.json HTTP/1.1\nHost: priemka.collections.test.yandex.ru\n\n";
                                            unpack(gen_proxy_backends({
                                              { "man1-1110-man-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0b:1feb:10d:61d:0:564f"; };
                                              { "man1-8386-man-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0b:1c18:10d:61d:0:564f"; };
                                              { "sas1-2982-sas-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c08:2ba1:10d:61e:0:564f"; };
                                              { "sas1-5977-sas-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c08:192b:10d:61e:0:564f"; };
                                              { "vla1-0956-vla-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0d:440d:10d:61f:0:564f"; };
                                              { "vla1-4565-vla-pdb-nodejs-priemka-22095.gencfg-c.yandex.net"; 22095; 80.000; "2a02:6b8:c0d:3b95:10d:61f:0:564f"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "3s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- hashing
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Gateway Timeout";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- hasher
                                    }; -- report
                                  }; -- icookie
                                }; -- default
                              }; -- regexp
                            }; -- response_headers
                          }; -- priemka
                          pull = {
                            priority = 3;
                            match_fsm = {
                              host = "pull-[0-9]+.collections.test.yandex.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            response_headers = {
                              create = {
                                ["X-Section"] = "https:pull";
                              }; -- create
                              regexp = {
                                common_upstream_api_cards = {
                                  priority = 10;
                                  match_fsm = {
                                    URI = "(/collections)?/api/cards(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "4986077042537244551";
                                  }; -- shared
                                }; -- common_upstream_api_cards
                                common_upstream_api_content = {
                                  priority = 9;
                                  match_fsm = {
                                    URI = "(/collections)?/api/content(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "1387397625658658873";
                                  }; -- shared
                                }; -- common_upstream_api_content
                                common_upstream_api_subscriptions_bulk = {
                                  priority = 8;
                                  match_fsm = {
                                    URI = "(/collections)?/api/subscriptions/bulk(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "816386532865811858";
                                  }; -- shared
                                }; -- common_upstream_api_subscriptions_bulk
                                common_upstream_api_user = {
                                  priority = 7;
                                  match_fsm = {
                                    URI = "(/collections)?/api/user(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "3262949501642823887";
                                  }; -- shared
                                }; -- common_upstream_api_user
                                common_upstream_api_verticals = {
                                  priority = 6;
                                  match_fsm = {
                                    URI = "(/collections)?/api/verticals/detect(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "3641105710701504232";
                                  }; -- shared
                                }; -- common_upstream_api_verticals
                                common_upstream_api_informers = {
                                  priority = 5;
                                  match_fsm = {
                                    URI = "(/collections)?/api/informers(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "7706509318309232490";
                                  }; -- shared
                                }; -- common_upstream_api_informers
                                common_upstream_api = {
                                  priority = 4;
                                  match_fsm = {
                                    URI = "(/collections)?/api(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "3139369152861946465";
                                  }; -- shared
                                }; -- common_upstream_api
                                common_upstream_picture = {
                                  priority = 3;
                                  match_fsm = {
                                    URI = "(/collections)?/picture(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "6982449101941206710";
                                  }; -- shared
                                }; -- common_upstream_picture
                                common_upstream_sitemap = {
                                  priority = 2;
                                  match_fsm = {
                                    URI = "(/collections)?/sitemap(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "5764201120179854379";
                                  }; -- shared
                                }; -- common_upstream_sitemap
                                default = {
                                  priority = 1;
                                  icookie = {
                                    use_default_keys = true;
                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                    trust_parent = false;
                                    trust_children = false;
                                    enable_set_cookie = true;
                                    enable_decrypting = true;
                                    decrypted_uid_header = "X-Yandex-ICookie";
                                    error_header = "X-Yandex-ICookie-Error";
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      hasher = {
                                        mode = "text";
                                        balancer2 = {
                                          active_policy = {
                                            unique_policy = {};
                                          }; -- active_policy
                                          attempts = 3;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "man1-2972-man-pdb-nodejs-feature--789-28460.gencfg-c.yandex.net"; 28460; 40.000; "2a02:6b8:c0b:a:10d:41b4:0:6f2c"; };
                                              { "sas1-1730-sas-pdb-nodejs-feature--5c6-16907.gencfg-c.yandex.net"; 16907; 40.000; "2a02:6b8:c08:2607:10d:41b3:0:420b"; };
                                              { "vla1-5970-vla-pdb-nodejs-feature--e76-18513.gencfg-c.yandex.net"; 18513; 40.000; "2a02:6b8:c0f:158d:10d:41b2:0:4851"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "3s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Gateway Timeout";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- hasher
                                      refers = "nodejs";
                                    }; -- report
                                  }; -- icookie
                                }; -- default
                              }; -- regexp
                            }; -- response_headers
                          }; -- pull
                          exp_testing = {
                            priority = 2;
                            match_fsm = {
                              cgi = "(exp-testing=da|exp_confs=testing)";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                            response_headers = {
                              create = {
                                ["X-Section"] = "https:exp_testing";
                              }; -- create
                              headers = {
                                create = {
                                  ["X-L7-EXP-Testing"] = "true";
                                }; -- create
                                exp_getter = {
                                  trusted = false;
                                  file_switch = "./controls/expgetter.switch";
                                  service_name = "collections";
                                  service_name_header = "Y-Service";
                                  uaas = {
                                    shared = {
                                      uuid = "946526983492352280";
                                    }; -- shared
                                  }; -- uaas
                                  shared = {
                                    uuid = "upstreams";
                                    regexp = {
                                      common_upstream_api_cards = {
                                        priority = 10;
                                        match_fsm = {
                                          URI = "(/collections)?/api/cards(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "4986077042537244551";
                                        }; -- shared
                                      }; -- common_upstream_api_cards
                                      common_upstream_api_content = {
                                        priority = 9;
                                        match_fsm = {
                                          URI = "(/collections)?/api/content(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "1387397625658658873";
                                        }; -- shared
                                      }; -- common_upstream_api_content
                                      common_upstream_api_subscriptions_bulk = {
                                        priority = 8;
                                        match_fsm = {
                                          URI = "(/collections)?/api/subscriptions/bulk(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "816386532865811858";
                                        }; -- shared
                                      }; -- common_upstream_api_subscriptions_bulk
                                      common_upstream_api_user = {
                                        priority = 7;
                                        match_fsm = {
                                          URI = "(/collections)?/api/user(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "3262949501642823887";
                                        }; -- shared
                                      }; -- common_upstream_api_user
                                      common_upstream_api_verticals = {
                                        priority = 6;
                                        match_fsm = {
                                          URI = "(/collections)?/api/verticals/detect(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "3641105710701504232";
                                        }; -- shared
                                      }; -- common_upstream_api_verticals
                                      common_upstream_api_informers = {
                                        priority = 5;
                                        match_fsm = {
                                          URI = "(/collections)?/api/informers(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "7706509318309232490";
                                        }; -- shared
                                      }; -- common_upstream_api_informers
                                      common_upstream_api = {
                                        priority = 4;
                                        match_fsm = {
                                          URI = "(/collections)?/api(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "3139369152861946465";
                                        }; -- shared
                                      }; -- common_upstream_api
                                      common_upstream_picture = {
                                        priority = 3;
                                        match_fsm = {
                                          URI = "(/collections)?/picture(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "6982449101941206710";
                                        }; -- shared
                                      }; -- common_upstream_picture
                                      common_upstream_sitemap = {
                                        priority = 2;
                                        match_fsm = {
                                          URI = "(/collections)?/sitemap(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "5764201120179854379";
                                        }; -- shared
                                      }; -- common_upstream_sitemap
                                      default = {
                                        priority = 1;
                                        shared = {
                                          uuid = "6844481725927961011";
                                        }; -- shared
                                      }; -- default
                                    }; -- regexp
                                  }; -- shared
                                }; -- exp_getter
                              }; -- headers
                            }; -- response_headers
                          }; -- exp_testing
                          default = {
                            priority = 1;
                            response_headers = {
                              create = {
                                ["X-Section"] = "https:default";
                              }; -- create
                              shared = {
                                uuid = "7031677223042445314";
                              }; -- shared
                            }; -- response_headers
                          }; -- default
                        }; -- regexp
                      }; -- response_headers
                    }; -- headers
                  }; -- threshold
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_14591 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14591;
      }; -- ports
      shared = {
        uuid = "4363047024904990289";
      }; -- shared
    }; -- https_section_14591
    http_section_80 = {
      ips = {
        "2a02:6b8:0:3400::109c";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "5209686744144504258";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14590, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 14590, "/place/db/www/logs");
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
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 10485760;
                  recv_timeout = "1s";
                  pass_timeout = "10s";
                  headers = {
                    create_func = {
                      ["X-Collections-Req-Id"] = "reqid";
                      ["X-Forwarded-For-Y"] = "realip";
                    }; -- create_func
                    create_func_weak = {
                      ["X-Req-Id"] = "reqid";
                    }; -- create_func_weak
                    response_headers = {
                      delete = "uWSGI-encoding|uwsgi-encoding";
                      create = {
                        ["X-Section"] = "http:default";
                      }; -- create
                      regexp = {
                        slbping = {
                          priority = 4;
                          match_fsm = {
                            URI = "/slb_ping";
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
                        searchmetanets = {
                          priority = 3;
                          match_source_ip = {
                            source_mask = "5.45.192.0/18,5.255.192.0/18,37.9.64.0/18,37.140.128.0/18,77.88.0.0/18,84.201.128.0/18,87.250.224.0/19,93.158.128.0/18,95.108.128.0/17,100.43.64.0/19,130.193.32.0/19,141.8.128.0/18,178.154.128.0/17,199.21.96.0/22,199.36.240.0/22,213.180.192.0/19,2620:10f:d000::/44,2a02:6b8::/32";
                          }; -- match_source_ip
                          headers = {
                            create_func = {
                              ["X-Collections-Req-Id"] = "reqid";
                              ["X-Forwarded-For-Y"] = "realip";
                            }; -- create_func
                            create_func_weak = {
                              ["X-Req-Id"] = "reqid";
                            }; -- create_func_weak
                            regexp = {
                              upstream_pdb_card_recommender = {
                                priority = 16;
                                match_fsm = {
                                  URI = "/api/card_recommender(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "card_recommender";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
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
                                        { "man1-2893.search.yandex.net"; 14895; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:efe0"; };
                                        { "sas2-4544.search.yandex.net"; 14905; 40.000; "2a02:6b8:b000:6d5:feaa:14ff:fe1d:f66e"; };
                                        { "vla1-0215.search.yandex.net"; 14915; 40.000; "2a02:6b8:c0e:81:0:604:db7:a8cd"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "5s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- report
                              }; -- upstream_pdb_card_recommender
                              upstream_pdb_hot_feed_debug_info = {
                                priority = 15;
                                match_fsm = {
                                  URI = "/pdb/(hot_)?feed/(quality_)?debug_info(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "feed_debug_info";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
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
                                        { "man1-7192.search.yandex.net"; 28580; 40.000; "2a02:6b8:b000:6506:215:b2ff:fea9:62ea"; };
                                        { "sas1-9271.search.yandex.net"; 18340; 40.000; "2a02:6b8:b000:698:428d:5cff:fef4:9489"; };
                                        { "vla1-0886.search.yandex.net"; 27980; 40.000; "2a02:6b8:c0e:5e:0:604:db7:9de4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "5s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- report
                              }; -- upstream_pdb_hot_feed_debug_info
                              upstream_pdb_hot_feed = {
                                priority = 14;
                                match_fsm = {
                                  URI = "/pdb/hot_feed(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "hotfeed";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
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
                                        { "man1-7192.search.yandex.net"; 28580; 40.000; "2a02:6b8:b000:6506:215:b2ff:fea9:62ea"; };
                                        { "sas1-9271.search.yandex.net"; 18340; 40.000; "2a02:6b8:b000:698:428d:5cff:fef4:9489"; };
                                        { "vla1-0886.search.yandex.net"; 27980; 40.000; "2a02:6b8:c0e:5e:0:604:db7:9de4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "1s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- report
                              }; -- upstream_pdb_hot_feed
                              upstream_pdbcg = {
                                priority = 13;
                                match_fsm = {
                                  URI = "/pdb(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "pdbcg";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  regexp = {
                                    post_method = {
                                      priority = 2;
                                      match_fsm = {
                                        match = "POST.*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "1s";
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
                                            { "man1-7223.search.yandex.net"; 1026; 63.000; "2a02:6b8:b000:650c:215:b2ff:fea9:7036"; };
                                            { "sas1-9069.search.yandex.net"; 1025; 70.000; "2a02:6b8:b000:13e:feaa:14ff:fede:435e"; };
                                            { "vla1-2719.search.yandex.net"; 1025; 1.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "4s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                        on_error = {
                                          errordocument = {
                                            status = 504;
                                            force_conn_close = false;
                                            content = "Gateway Timeout";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- post_method
                                    default = {
                                      priority = 1;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
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
                                            { "man1-7223.search.yandex.net"; 1026; 63.000; "2a02:6b8:b000:650c:215:b2ff:fea9:7036"; };
                                            { "sas1-9069.search.yandex.net"; 1025; 70.000; "2a02:6b8:b000:13e:feaa:14ff:fede:435e"; };
                                            { "vla1-2719.search.yandex.net"; 1025; 1.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "4s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                        on_error = {
                                          errordocument = {
                                            status = 504;
                                            force_conn_close = false;
                                            content = "Gateway Timeout";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- default
                                  }; -- regexp
                                }; -- report
                              }; -- upstream_pdbcg
                              common_upstream_api_cards = {
                                priority = 12;
                                match_fsm = {
                                  URI = "(/collections)?/api/cards(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "4986077042537244551";
                                  rewrite = {
                                    actions = {
                                      {
                                        global = false;
                                        literal = false;
                                        case_insensitive = false;
                                        rewrite = "%1%3captcha=1%4";
                                        regexp = "(/collections)?(/api)(.*)captcha=1(.*)?";
                                      };
                                    }; -- actions
                                    rewrite = {
                                      actions = {
                                        {
                                          global = false;
                                          literal = false;
                                          header_name = "Host";
                                          rewrite = "yandex.%2";
                                          case_insensitive = false;
                                          regexp = "(l7test|collections\\.test)\\.yandex\\.(by|ru|kz|com\\.tr|com)";
                                        };
                                      }; -- actions
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
                                                  uuid = "703824871867170956";
                                                  report = {
                                                    uuid = "antirobot";
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
                                                        attempts = 2;
                                                        hashing = {
                                                          unpack(gen_proxy_backends({
                                                            { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                                            { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                                            { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                                            { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                                            { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                                            { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                                            { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                                            { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                                            { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                                            { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
                                                            { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
                                                            { "sas1-0136.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:14a:225:90ff:fe83:aae"; };
                                                            { "sas1-0138.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:14b:225:90ff:fe83:55c"; };
                                                            { "sas1-0152.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:14f:225:90ff:fe83:408"; };
                                                            { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
                                                            { "sas1-0289.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:150:225:90ff:fe83:b20"; };
                                                            { "sas1-0348.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:15b:225:90ff:fe83:9a4"; };
                                                            { "sas1-0484.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:149:225:90ff:fe83:1412"; };
                                                            { "sas1-0515.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:144:225:90ff:fe83:1446"; };
                                                            { "sas1-0533.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:154:225:90ff:fe88:b69a"; };
                                                            { "sas1-0861.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:156:225:90ff:fe83:422"; };
                                                            { "sas1-0936.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:15c:225:90ff:fe82:ff4c"; };
                                                            { "sas1-0963.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:14d:225:90ff:fe83:a9a"; };
                                                            { "sas1-0975.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:156:225:90ff:fe83:78c"; };
                                                            { "sas1-1237.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:14e:225:90ff:fe83:9fc"; };
                                                            { "sas1-1247.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:61a:922b:34ff:fecf:322c"; };
                                                            { "sas1-1583.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:621:922b:34ff:fecf:3dfe"; };
                                                            { "sas1-2218.search.yandex.net"; 13512; 254.000; "2a02:6b8:b000:66a:225:90ff:fe94:1792"; };
                                                            { "sas1-2572.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:607:225:90ff:fe83:161c"; };
                                                            { "sas1-2608.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60e:225:90ff:fe83:1812"; };
                                                            { "sas1-2623.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:607:225:90ff:fe83:15b0"; };
                                                            { "sas1-2624.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:607:225:90ff:fe83:1726"; };
                                                            { "sas1-2658.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60e:225:90ff:fe83:1938"; };
                                                            { "sas1-2659.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60e:225:90ff:fe83:1924"; };
                                                            { "sas1-2687.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:608:225:90ff:fe83:182c"; };
                                                            { "sas1-2688.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:608:225:90ff:fe83:1658"; };
                                                            { "sas1-2689.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:606:225:90ff:fe83:15f4"; };
                                                            { "sas1-2690.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:606:225:90ff:fe83:1620"; };
                                                            { "sas1-2693.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:606:225:90ff:fe83:16bc"; };
                                                            { "sas1-2696.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60b:225:90ff:fe83:14b2"; };
                                                            { "sas1-2698.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60b:225:90ff:fe83:14ba"; };
                                                            { "sas1-2700.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:609:225:90ff:fe83:1842"; };
                                                            { "sas1-2702.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60d:225:90ff:fe83:14d8"; };
                                                            { "sas1-2704.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60d:225:90ff:fe83:11ee"; };
                                                            { "sas1-2706.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:609:225:90ff:fe83:14c6"; };
                                                            { "sas1-2769.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:609:225:90ff:fe83:18a6"; };
                                                            { "sas1-2825.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:60d:225:90ff:fe83:b3e"; };
                                                            { "sas1-2849.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:631:225:90ff:fe88:b678"; };
                                                            { "sas1-2909.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:6ac:225:90ff:fe83:1f14"; };
                                                            { "sas1-2911.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:188:225:90ff:fe83:1f08"; };
                                                            { "sas1-3149.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:188:225:90ff:fe88:4ee6"; };
                                                            { "sas1-3370.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:633:225:90ff:fe88:4ce8"; };
                                                            { "sas1-3387.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:136:225:90ff:fe88:cb90"; };
                                                            { "sas1-3693.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:6ac:225:90ff:fe88:384c"; };
                                                            { "sas1-3778.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:107:225:90ff:fe83:2df0"; };
                                                            { "sas1-3781.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:10f:225:90ff:fe83:2cda"; };
                                                            { "sas1-3999.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:10c:225:90ff:fe83:2d3e"; };
                                                            { "sas1-4157.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:674:96de:80ff:fe81:b14"; };
                                                            { "sas1-4517.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:639:96de:80ff:fe81:1056"; };
                                                            { "sas1-4552.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:651:96de:80ff:fe81:16d8"; };
                                                            { "sas1-4573.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:63b:96de:80ff:fe81:10d8"; };
                                                            { "sas1-4593.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:63b:96de:80ff:fe81:13f8"; };
                                                            { "sas1-4614.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:642:96de:80ff:fe81:c90"; };
                                                            { "sas1-4735.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:642:96de:80ff:fe81:df4"; };
                                                            { "sas1-4783.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:65c:96de:80ff:fe81:11e2"; };
                                                            { "sas1-4869.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:63c:96de:80ff:fe81:110c"; };
                                                            { "sas1-4914.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:66b:96de:80ff:fe81:ca2"; };
                                                            { "sas1-5239.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:6aa:96de:80ff:fe81:10f8"; };
                                                            { "sas1-5407.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:648:96de:80ff:fe81:162c"; };
                                                            { "sas1-5414.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:63b:96de:80ff:fe81:1246"; };
                                                            { "sas1-5617.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:622:225:90ff:fe92:b070"; };
                                                            { "sas1-6917.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:18c:922b:34ff:fecf:2f0c"; };
                                                            { "sas1-6995.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:695:922b:34ff:fecf:294a"; };
                                                            { "sas1-7431.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:16f:922b:34ff:fecf:263a"; };
                                                            { "sas1-7442.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:693:922b:34ff:fecf:2a12"; };
                                                            { "sas1-7505.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:18a:922b:34ff:fecf:2f08"; };
                                                            { "sas1-7631.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:690:922b:34ff:fecf:3938"; };
                                                            { "sas1-7635.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:18d:922b:34ff:fecf:26c4"; };
                                                            { "sas1-7649.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:694:922b:34ff:fecf:2b76"; };
                                                            { "sas1-7663.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:18b:922b:34ff:fecf:226e"; };
                                                            { "sas1-7756.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:692:922b:34ff:fecf:319a"; };
                                                            { "sas1-7957.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:192:922b:34ff:fecf:362e"; };
                                                            { "sas1-8009.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:688:922b:34ff:fecf:2d5c"; };
                                                            { "sas1-8079.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:190:922b:34ff:fecf:398a"; };
                                                            { "sas1-9935.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:682:225:90ff:fe94:2c20"; };
                                                            { "sas1-9936.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:677:225:90ff:fe93:78e8"; };
                                                            { "vla1-1343.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                                            { "vla1-1797.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                                            { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                            { "vla1-3568.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                                            { "vla1-3679.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                                            { "vla1-3709.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                                            { "vla1-3710.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                                            { "vla1-3716.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                                            { "vla1-3863.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                                            { "vla1-3965.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                                            { "vla1-4006.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                                            { "vla1-4025.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                                            { "vla1-4041.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                                            { "vla1-4114.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                                            { "vla1-4117.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                                            { "vla1-4119.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                                            { "vla1-4130.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                                            { "vla1-4153.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                                            { "vla1-4167.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                                            { "vla1-4168.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                                            { "vla1-4177.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                                            { "vla1-4183.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                                            { "vla1-4192.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                                            { "vla1-4200.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                                            { "vla1-4321.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                                            { "vla1-4344.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                                            { "vla1-4354.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                                            { "vla1-4406.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                                            { "vla1-4472.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                                            { "vla1-4553.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                                            { "vla1-4554.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
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
                                                    }; -- stats_eater
                                                  }; -- report
                                                }; -- shared
                                              }; -- checker
                                              module = {
                                                rewrite = {
                                                  actions = {
                                                    {
                                                      global = false;
                                                      literal = false;
                                                      case_insensitive = false;
                                                      rewrite = "%1/api%2captcha=1%3";
                                                      regexp = "(/collections)?(/[^a].*)captcha=1(.*)?";
                                                    };
                                                  }; -- actions
                                                  icookie = {
                                                    use_default_keys = true;
                                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                                    trust_parent = false;
                                                    trust_children = false;
                                                    enable_set_cookie = true;
                                                    enable_decrypting = true;
                                                    decrypted_uid_header = "X-Yandex-ICookie";
                                                    error_header = "X-Yandex-ICookie-Error";
                                                    report = {
                                                      uuid = "api_cards";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      shared = {
                                                        uuid = "4824114884891032442";
                                                        regexp = {
                                                          post_method = {
                                                            priority = 2;
                                                            match_fsm = {
                                                              match = "POST.*";
                                                              case_insensitive = true;
                                                              surround = false;
                                                            }; -- match_fsm
                                                            balancer2 = {
                                                              timeout_policy = {
                                                                timeout = "1s";
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
                                                                  { "man1-6657-man-pdb-backend-test-20449.gencfg-c.yandex.net"; 20449; 92.000; "2a02:6b8:c0b:459:100:178e:0:4fe1"; };
                                                                  { "sas1-8346-sas-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 92.000; "2a02:6b8:c08:56ac:100:178f:0:41f"; };
                                                                  { "vla1-0328-vla-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 10.000; "2a02:6b8:c0d:4492:10b:2ff7:0:41f"; };
                                                                }, {
                                                                  resolve_timeout = "10ms";
                                                                  connect_timeout = "100ms";
                                                                  backend_timeout = "15s";
                                                                  fail_on_5xx = true;
                                                                  http_backend = true;
                                                                  buffering = false;
                                                                  keepalive_count = 1;
                                                                  need_resolve = true;
                                                                }))
                                                              }; -- weighted2
                                                              on_error = {
                                                                errordocument = {
                                                                  status = 504;
                                                                  force_conn_close = false;
                                                                  content = "Gateway Timeout";
                                                                }; -- errordocument
                                                              }; -- on_error
                                                            }; -- balancer2
                                                          }; -- post_method
                                                          default = {
                                                            priority = 1;
                                                            balancer2 = {
                                                              unique_policy = {};
                                                              attempts = 3;
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
                                                                  { "man1-6657-man-pdb-backend-test-20449.gencfg-c.yandex.net"; 20449; 92.000; "2a02:6b8:c0b:459:100:178e:0:4fe1"; };
                                                                  { "sas1-8346-sas-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 92.000; "2a02:6b8:c08:56ac:100:178f:0:41f"; };
                                                                  { "vla1-0328-vla-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 10.000; "2a02:6b8:c0d:4492:10b:2ff7:0:41f"; };
                                                                }, {
                                                                  resolve_timeout = "10ms";
                                                                  connect_timeout = "100ms";
                                                                  backend_timeout = "15s";
                                                                  fail_on_5xx = true;
                                                                  http_backend = true;
                                                                  buffering = false;
                                                                  keepalive_count = 1;
                                                                  need_resolve = true;
                                                                }))
                                                              }; -- weighted2
                                                              on_error = {
                                                                errordocument = {
                                                                  status = 504;
                                                                  force_conn_close = false;
                                                                  content = "Gateway Timeout";
                                                                }; -- errordocument
                                                              }; -- on_error
                                                            }; -- balancer2
                                                          }; -- default
                                                        }; -- regexp
                                                      }; -- shared
                                                    }; -- report
                                                  }; -- icookie
                                                }; -- rewrite
                                              }; -- module
                                            }; -- antirobot
                                          }; -- cutter
                                        }; -- h100
                                      }; -- hasher
                                    }; -- rewrite
                                  }; -- rewrite
                                }; -- shared
                              }; -- common_upstream_api_cards
                              common_upstream_api_content = {
                                priority = 11;
                                match_fsm = {
                                  URI = "(/collections)?/api/content(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "1387397625658658873";
                                  rewrite = {
                                    actions = {
                                      {
                                        global = false;
                                        literal = false;
                                        case_insensitive = false;
                                        rewrite = "%1%3captcha=1%4";
                                        regexp = "(/collections)?(/api)(.*)captcha=1(.*)?";
                                      };
                                    }; -- actions
                                    rewrite = {
                                      actions = {
                                        {
                                          global = false;
                                          literal = false;
                                          header_name = "Host";
                                          rewrite = "yandex.%2";
                                          case_insensitive = false;
                                          regexp = "(l7test|collections\\.test)\\.yandex\\.(by|ru|kz|com\\.tr|com)";
                                        };
                                      }; -- actions
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
                                                  uuid = "703824871867170956";
                                                }; -- shared
                                              }; -- checker
                                              module = {
                                                rewrite = {
                                                  actions = {
                                                    {
                                                      global = false;
                                                      literal = false;
                                                      case_insensitive = false;
                                                      rewrite = "%1/api%2captcha=1%3";
                                                      regexp = "(/collections)?(/[^a].*)captcha=1(.*)?";
                                                    };
                                                  }; -- actions
                                                  icookie = {
                                                    use_default_keys = true;
                                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                                    trust_parent = false;
                                                    trust_children = false;
                                                    enable_set_cookie = true;
                                                    enable_decrypting = true;
                                                    decrypted_uid_header = "X-Yandex-ICookie";
                                                    error_header = "X-Yandex-ICookie-Error";
                                                    report = {
                                                      uuid = "api_content";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      shared = {
                                                        uuid = "4824114884891032442";
                                                      }; -- shared
                                                    }; -- report
                                                  }; -- icookie
                                                }; -- rewrite
                                              }; -- module
                                            }; -- antirobot
                                          }; -- cutter
                                        }; -- h100
                                      }; -- hasher
                                    }; -- rewrite
                                  }; -- rewrite
                                }; -- shared
                              }; -- common_upstream_api_content
                              common_upstream_api_subscriptions_bulk = {
                                priority = 10;
                                match_fsm = {
                                  URI = "(/collections)?/api/subscriptions/bulk(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "816386532865811858";
                                  rewrite = {
                                    actions = {
                                      {
                                        global = false;
                                        literal = false;
                                        case_insensitive = false;
                                        rewrite = "%1%3captcha=1%4";
                                        regexp = "(/collections)?(/api)(.*)captcha=1(.*)?";
                                      };
                                    }; -- actions
                                    rewrite = {
                                      actions = {
                                        {
                                          global = false;
                                          literal = false;
                                          header_name = "Host";
                                          rewrite = "yandex.%2";
                                          case_insensitive = false;
                                          regexp = "(l7test|collections\\.test)\\.yandex\\.(by|ru|kz|com\\.tr|com)";
                                        };
                                      }; -- actions
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
                                                  uuid = "703824871867170956";
                                                }; -- shared
                                              }; -- checker
                                              module = {
                                                rewrite = {
                                                  actions = {
                                                    {
                                                      global = false;
                                                      literal = false;
                                                      case_insensitive = false;
                                                      rewrite = "%1/api%2captcha=1%3";
                                                      regexp = "(/collections)?(/[^a].*)captcha=1(.*)?";
                                                    };
                                                  }; -- actions
                                                  icookie = {
                                                    use_default_keys = true;
                                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                                    trust_parent = false;
                                                    trust_children = false;
                                                    enable_set_cookie = true;
                                                    enable_decrypting = true;
                                                    decrypted_uid_header = "X-Yandex-ICookie";
                                                    error_header = "X-Yandex-ICookie-Error";
                                                    report = {
                                                      uuid = "api_subscriptions_bulk";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      shared = {
                                                        uuid = "4824114884891032442";
                                                      }; -- shared
                                                    }; -- report
                                                  }; -- icookie
                                                }; -- rewrite
                                              }; -- module
                                            }; -- antirobot
                                          }; -- cutter
                                        }; -- h100
                                      }; -- hasher
                                    }; -- rewrite
                                  }; -- rewrite
                                }; -- shared
                              }; -- common_upstream_api_subscriptions_bulk
                              common_upstream_api_user = {
                                priority = 9;
                                match_fsm = {
                                  URI = "(/collections)?/api/user(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "3262949501642823887";
                                  rewrite = {
                                    actions = {
                                      {
                                        global = false;
                                        literal = false;
                                        case_insensitive = false;
                                        rewrite = "%1%3captcha=1%4";
                                        regexp = "(/collections)?(/api)(.*)captcha=1(.*)?";
                                      };
                                    }; -- actions
                                    rewrite = {
                                      actions = {
                                        {
                                          global = false;
                                          literal = false;
                                          header_name = "Host";
                                          rewrite = "yandex.%2";
                                          case_insensitive = false;
                                          regexp = "(l7test|collections\\.test)\\.yandex\\.(by|ru|kz|com\\.tr|com)";
                                        };
                                      }; -- actions
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
                                                  uuid = "703824871867170956";
                                                }; -- shared
                                              }; -- checker
                                              module = {
                                                rewrite = {
                                                  actions = {
                                                    {
                                                      global = false;
                                                      literal = false;
                                                      case_insensitive = false;
                                                      rewrite = "%1/api%2captcha=1%3";
                                                      regexp = "(/collections)?(/[^a].*)captcha=1(.*)?";
                                                    };
                                                  }; -- actions
                                                  icookie = {
                                                    use_default_keys = true;
                                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                                    trust_parent = false;
                                                    trust_children = false;
                                                    enable_set_cookie = true;
                                                    enable_decrypting = true;
                                                    decrypted_uid_header = "X-Yandex-ICookie";
                                                    error_header = "X-Yandex-ICookie-Error";
                                                    report = {
                                                      uuid = "api_user";
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
                                                        shared = {
                                                          uuid = "4412477937095441134";
                                                        }; -- shared
                                                      }; -- geobase
                                                    }; -- report
                                                  }; -- icookie
                                                }; -- rewrite
                                              }; -- module
                                            }; -- antirobot
                                          }; -- cutter
                                        }; -- h100
                                      }; -- hasher
                                    }; -- rewrite
                                  }; -- rewrite
                                }; -- shared
                              }; -- common_upstream_api_user
                              common_upstream_api_verticals = {
                                priority = 8;
                                match_fsm = {
                                  URI = "(/collections)?/api/verticals/detect(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "3641105710701504232";
                                  icookie = {
                                    use_default_keys = true;
                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                    trust_parent = false;
                                    trust_children = false;
                                    enable_set_cookie = true;
                                    enable_decrypting = true;
                                    decrypted_uid_header = "X-Yandex-ICookie";
                                    error_header = "X-Yandex-ICookie-Error";
                                    report = {
                                      uuid = "api_verticals";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      regexp = {
                                        post_method = {
                                          priority = 2;
                                          match_fsm = {
                                            match = "POST.*";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                          balancer2 = {
                                            timeout_policy = {
                                              timeout = "1s";
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
                                                { "man1-4792.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:8ab0"; };
                                                { "man1-6910.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:2230"; };
                                                { "sas1-1540.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cb6"; };
                                                { "sas1-9040.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:3f28"; };
                                                { "vla1-0089.search.yandex.net"; 29580; 272.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                                { "vla1-0275.search.yandex.net"; 29580; 272.000; "2a02:6b8:c0e:7e:0:604:db7:a1b5"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "4s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Gateway Timeout";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- post_method
                                        default = {
                                          priority = 1;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
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
                                                { "man1-4792.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:8ab0"; };
                                                { "man1-6910.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:2230"; };
                                                { "sas1-1540.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cb6"; };
                                                { "sas1-9040.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:3f28"; };
                                                { "vla1-0089.search.yandex.net"; 29580; 272.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                                { "vla1-0275.search.yandex.net"; 29580; 272.000; "2a02:6b8:c0e:7e:0:604:db7:a1b5"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "4s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Gateway Timeout";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- default
                                      }; -- regexp
                                    }; -- report
                                  }; -- icookie
                                }; -- shared
                              }; -- common_upstream_api_verticals
                              common_upstream_api_informers = {
                                priority = 7;
                                match_fsm = {
                                  URI = "(/collections)?/api/informers(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "7706509318309232490";
                                  icookie = {
                                    use_default_keys = true;
                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                    trust_parent = false;
                                    trust_children = false;
                                    enable_set_cookie = true;
                                    enable_decrypting = true;
                                    decrypted_uid_header = "X-Yandex-ICookie";
                                    error_header = "X-Yandex-ICookie-Error";
                                    report = {
                                      uuid = "api_informers";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      regexp = {
                                        post_method = {
                                          priority = 2;
                                          match_fsm = {
                                            match = "POST.*";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                          balancer2 = {
                                            timeout_policy = {
                                              timeout = "1s";
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
                                                { "sas1-2254-sas-pdb-informers-dev-18730.gencfg-c.yandex.net"; 18730; 120.000; "2a02:6b8:c08:290c:10b:89e5:0:492a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "4s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Gateway Timeout";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- post_method
                                        default = {
                                          priority = 1;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
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
                                                { "sas1-2254-sas-pdb-informers-dev-18730.gencfg-c.yandex.net"; 18730; 120.000; "2a02:6b8:c08:290c:10b:89e5:0:492a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "4s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Gateway Timeout";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- default
                                      }; -- regexp
                                    }; -- report
                                  }; -- icookie
                                }; -- shared
                              }; -- common_upstream_api_informers
                              upstream_api_informers = {
                                priority = 6;
                                match_fsm = {
                                  URI = "/api/informers(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "7706509318309232490";
                                }; -- shared
                              }; -- upstream_api_informers
                              upstream_pdb_top_reader = {
                                priority = 5;
                                match_fsm = {
                                  URI = "(/collections)?/api/top(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "topreader";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
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
                                        { "man1-7318.search.yandex.net"; 24765; 80.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e870"; };
                                        { "sas1-3990.search.yandex.net"; 24775; 80.000; "2a02:6b8:b000:64a:225:90ff:fe83:2e7e"; };
                                        { "vla1-0135.search.yandex.net"; 24785; 80.000; "2a02:6b8:c0e:3f:0:604:db7:9f2d"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "1s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- report
                              }; -- upstream_pdb_top_reader
                              common_upstream_api = {
                                priority = 4;
                                match_fsm = {
                                  URI = "(/collections)?/api(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "3139369152861946465";
                                  rewrite = {
                                    actions = {
                                      {
                                        global = false;
                                        literal = false;
                                        case_insensitive = false;
                                        rewrite = "%1%3captcha=1%4";
                                        regexp = "(/collections)?(/api)(.*)captcha=1(.*)?";
                                      };
                                    }; -- actions
                                    rewrite = {
                                      actions = {
                                        {
                                          global = false;
                                          literal = false;
                                          header_name = "Host";
                                          rewrite = "yandex.%2";
                                          case_insensitive = false;
                                          regexp = "(l7test|collections\\.test)\\.yandex\\.(by|ru|kz|com\\.tr|com)";
                                        };
                                      }; -- actions
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
                                                  uuid = "703824871867170956";
                                                }; -- shared
                                              }; -- checker
                                              module = {
                                                rewrite = {
                                                  actions = {
                                                    {
                                                      global = false;
                                                      literal = false;
                                                      case_insensitive = false;
                                                      rewrite = "%1/api%2captcha=1%3";
                                                      regexp = "(/collections)?(/[^a].*)captcha=1(.*)?";
                                                    };
                                                  }; -- actions
                                                  icookie = {
                                                    use_default_keys = true;
                                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                                    trust_parent = false;
                                                    trust_children = false;
                                                    enable_set_cookie = true;
                                                    enable_decrypting = true;
                                                    decrypted_uid_header = "X-Yandex-ICookie";
                                                    error_header = "X-Yandex-ICookie-Error";
                                                    report = {
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      refers = "api";
                                                      shared = {
                                                        uuid = "4412477937095441134";
                                                      }; -- shared
                                                    }; -- report
                                                  }; -- icookie
                                                }; -- rewrite
                                              }; -- module
                                            }; -- antirobot
                                          }; -- cutter
                                        }; -- h100
                                      }; -- hasher
                                    }; -- rewrite
                                  }; -- rewrite
                                }; -- shared
                              }; -- common_upstream_api
                              common_upstream_picture = {
                                priority = 3;
                                match_fsm = {
                                  URI = "(/collections)?/picture(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "6982449101941206710";
                                  icookie = {
                                    use_default_keys = true;
                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                    trust_parent = false;
                                    trust_children = false;
                                    enable_set_cookie = true;
                                    enable_decrypting = true;
                                    decrypted_uid_header = "X-Yandex-ICookie";
                                    error_header = "X-Yandex-ICookie-Error";
                                    report = {
                                      uuid = "picture";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      shared = {
                                        uuid = "4412477937095441134";
                                        regexp = {
                                          post_method = {
                                            priority = 2;
                                            match_fsm = {
                                              match = "POST.*";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                            balancer2 = {
                                              timeout_policy = {
                                                timeout = "1s";
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
                                                  { "man1-6657-man-pdb-backend-test-20449.gencfg-c.yandex.net"; 20449; 92.000; "2a02:6b8:c0b:459:100:178e:0:4fe1"; };
                                                  { "sas1-8346-sas-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 92.000; "2a02:6b8:c08:56ac:100:178f:0:41f"; };
                                                  { "vla1-0328-vla-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 10.000; "2a02:6b8:c0d:4492:10b:2ff7:0:41f"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "4s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- weighted2
                                              on_error = {
                                                errordocument = {
                                                  status = 504;
                                                  force_conn_close = false;
                                                  content = "Gateway Timeout";
                                                }; -- errordocument
                                              }; -- on_error
                                            }; -- balancer2
                                          }; -- post_method
                                          default = {
                                            priority = 1;
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 3;
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
                                                  { "man1-6657-man-pdb-backend-test-20449.gencfg-c.yandex.net"; 20449; 92.000; "2a02:6b8:c0b:459:100:178e:0:4fe1"; };
                                                  { "sas1-8346-sas-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 92.000; "2a02:6b8:c08:56ac:100:178f:0:41f"; };
                                                  { "vla1-0328-vla-pdb-backend-test-1055.gencfg-c.yandex.net"; 1055; 10.000; "2a02:6b8:c0d:4492:10b:2ff7:0:41f"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "4s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- weighted2
                                              on_error = {
                                                errordocument = {
                                                  status = 504;
                                                  force_conn_close = false;
                                                  content = "Gateway Timeout";
                                                }; -- errordocument
                                              }; -- on_error
                                            }; -- balancer2
                                          }; -- default
                                        }; -- regexp
                                      }; -- shared
                                    }; -- report
                                  }; -- icookie
                                }; -- shared
                              }; -- common_upstream_picture
                              common_upstream_sitemap = {
                                priority = 2;
                                match_fsm = {
                                  URI = "(/collections)?/sitemap(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                shared = {
                                  uuid = "5764201120179854379";
                                  report = {
                                    uuid = "sitemap";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    regexp = {
                                      post_method = {
                                        priority = 2;
                                        match_fsm = {
                                          match = "POST.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "1s";
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
                                              { "man1-1256.search.yandex.net"; 22150; 5.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9b30"; };
                                              { "man1-5720.search.yandex.net"; 22150; 5.000; "2a02:6b8:b000:605b:e61d:2dff:fe03:4940"; };
                                              { "sas1-9267.search.yandex.net"; 22370; 5.000; "2a02:6b8:b000:698:428d:5cff:fef4:8c3b"; };
                                              { "sas1-9542.search.yandex.net"; 22370; 5.000; "2a02:6b8:b000:19a:428d:5cff:fef4:8b4d"; };
                                              { "vla1-0847.search.yandex.net"; 18790; 5.000; "2a02:6b8:c0e:44:0:604:db7:9f4d"; };
                                              { "vla1-0998.search.yandex.net"; 18790; 5.000; "2a02:6b8:c0e:84:0:604:db7:aac8"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Gateway Timeout";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- post_method
                                      default = {
                                        priority = 1;
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
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
                                              { "man1-1256.search.yandex.net"; 22150; 5.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9b30"; };
                                              { "man1-5720.search.yandex.net"; 22150; 5.000; "2a02:6b8:b000:605b:e61d:2dff:fe03:4940"; };
                                              { "sas1-9267.search.yandex.net"; 22370; 5.000; "2a02:6b8:b000:698:428d:5cff:fef4:8c3b"; };
                                              { "sas1-9542.search.yandex.net"; 22370; 5.000; "2a02:6b8:b000:19a:428d:5cff:fef4:8b4d"; };
                                              { "vla1-0847.search.yandex.net"; 18790; 5.000; "2a02:6b8:c0e:44:0:604:db7:9f4d"; };
                                              { "vla1-0998.search.yandex.net"; 18790; 5.000; "2a02:6b8:c0e:84:0:604:db7:aac8"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Gateway Timeout";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- default
                                    }; -- regexp
                                  }; -- report
                                }; -- shared
                              }; -- common_upstream_sitemap
                              default = {
                                priority = 1;
                                shared = {
                                  uuid = "6844481725927961011";
                                  icookie = {
                                    use_default_keys = true;
                                    domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                    trust_parent = false;
                                    trust_children = false;
                                    enable_set_cookie = true;
                                    enable_decrypting = true;
                                    decrypted_uid_header = "X-Yandex-ICookie";
                                    error_header = "X-Yandex-ICookie-Error";
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      hasher = {
                                        mode = "text";
                                        regexp = {
                                          post_method = {
                                            priority = 2;
                                            match_fsm = {
                                              match = "POST.*";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                            balancer2 = {
                                              timeout_policy = {
                                                timeout = "1s";
                                                active_policy = {
                                                  unique_policy = {};
                                                }; -- active_policy
                                              }; -- timeout_policy
                                              attempts = 1;
                                              hashing = {
                                                delay = "30s";
                                                request = "GET /version.json HTTP/1.1\nHost: collections.test.yandex.ru\n\n";
                                                unpack(gen_proxy_backends({
                                                  { "man1-6672-man-pdb-nodejs-test-1062.gencfg-c.yandex.net"; 1062; 1.000; "2a02:6b8:c0b:609:100:178b:0:426"; };
                                                  { "man1-8462-man-pdb-nodejs-test-1062.gencfg-c.yandex.net"; 1062; 1.000; "2a02:6b8:c0b:1506:100:178b:0:426"; };
                                                  { "sas1-2173-sas-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c08:2021:100:178a:0:49f"; };
                                                  { "sas1-6873-sas-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c08:4a28:100:178a:0:49f"; };
                                                  { "vla1-0022-vla-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c0d:4f96:10b:300d:0:49f"; };
                                                  { "vla1-0932-vla-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c0d:4c1c:10b:300d:0:49f"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "10s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- hashing
                                              on_error = {
                                                errordocument = {
                                                  status = 200;
                                                  content = "OK";
                                                  force_conn_close = false;
                                                }; -- errordocument
                                              }; -- on_error
                                            }; -- balancer2
                                          }; -- post_method
                                          default = {
                                            priority = 1;
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
                                              balancer2 = {
                                                active_policy = {
                                                  unique_policy = {};
                                                }; -- active_policy
                                                attempts = 3;
                                                hashing = {
                                                  delay = "30s";
                                                  request = "GET /version.json HTTP/1.1\nHost: collections.test.yandex.ru\n\n";
                                                  unpack(gen_proxy_backends({
                                                    { "man1-6672-man-pdb-nodejs-test-1062.gencfg-c.yandex.net"; 1062; 1.000; "2a02:6b8:c0b:609:100:178b:0:426"; };
                                                    { "man1-8462-man-pdb-nodejs-test-1062.gencfg-c.yandex.net"; 1062; 1.000; "2a02:6b8:c0b:1506:100:178b:0:426"; };
                                                    { "sas1-2173-sas-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c08:2021:100:178a:0:49f"; };
                                                    { "sas1-6873-sas-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c08:4a28:100:178a:0:49f"; };
                                                    { "vla1-0022-vla-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c0d:4f96:10b:300d:0:49f"; };
                                                    { "vla1-0932-vla-pdb-nodejs-test-1183.gencfg-c.yandex.net"; 1183; 1.000; "2a02:6b8:c0d:4c1c:10b:300d:0:49f"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "100ms";
                                                    backend_timeout = "3s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 1;
                                                    need_resolve = true;
                                                  }))
                                                }; -- hashing
                                                on_error = {
                                                  balancer2 = {
                                                    active_policy = {
                                                      unique_policy = {};
                                                    }; -- active_policy
                                                    attempts = 3;
                                                    hashing = {
                                                      delay = "1s";
                                                      request = "GET /status HTTP/1.1\nHost: collections.test.yandex.ru\n\n";
                                                      unpack(gen_proxy_backends({
                                                        { "man1-4210.search.yandex.net"; 13070; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:ba86"; };
                                                        { "sas1-6247.search.yandex.net"; 13070; 40.000; "2a02:6b8:b000:118:215:b2ff:fea7:75c0"; };
                                                        { "vla1-0211.search.yandex.net"; 13070; 40.000; "2a02:6b8:c0e:82:0:604:db7:a91f"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "1s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 1;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- hashing
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Gateway Timeout";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- geobase
                                          }; -- default
                                        }; -- regexp
                                      }; -- hasher
                                      refers = "nodejs";
                                    }; -- report
                                  }; -- icookie
                                }; -- shared
                              }; -- default
                            }; -- regexp
                          }; -- headers
                        }; -- searchmetanets
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
                              uuid = "8484580054162036321";
                              exp_getter = {
                                trusted = false;
                                file_switch = "./controls/expgetter.switch";
                                service_name = "collections";
                                service_name_header = "Y-Service";
                                uaas = {
                                  shared = {
                                    uuid = "946526983492352280";
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
                                                    { "vla2-5623-b04-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4f81:10e:b569:0:37d2"; };
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
                                  uuid = "upstreams";
                                }; -- shared
                              }; -- exp_getter
                            }; -- shared
                          }; -- headers
                        }; -- exp_testing
                        default = {
                          priority = 1;
                          shared = {
                            uuid = "7031677223042445314";
                            headers = {
                              create = {
                                ["X-L7-EXP"] = "true";
                              }; -- create
                              shared = {
                                uuid = "8484580054162036321";
                              }; -- shared
                            }; -- headers
                          }; -- shared
                        }; -- default
                      }; -- regexp
                    }; -- response_headers
                  }; -- headers
                }; -- threshold
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_14590 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14590;
      }; -- ports
      shared = {
        uuid = "5209686744144504258";
      }; -- shared
    }; -- http_section_14590
  }; -- ipdispatch
}