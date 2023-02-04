instance = {
  thread_mode = thread_mode; set_no_file = false;
  addrs = {
    { ip = "localhost"; port = port; }
  };
  admin_addrs = {
    { ip = "localhost"; port = admin_port; }
  };
  ipdispatch = {
    admin = {
      ip = "localhost"; port = admin_port;
      http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;
        admin = {};
      };
    };
    default = {
      http = {
        maxlen = 65536; maxreq = 65536;
        accesslog = {
            log = access_log;
            webauth = {
              checker = {
                proxy = {
                  host = "localhost";
                  port = backend_port;
                };
              }; --checker

              role = role;
              auth_path = path;

              on_forbidden = {
                errordocument = {
                  status = 200;
                  content = "on_forbidden";
                };
              };
              on_error = {
                errordocument = {
                  status = 200;
                  content = "on_error";
                };
              };

              unauthorized_redirect = redir_url;
              unauthorized_set_cookie = set_cookie;

              allow_options_passthrough = allow_options_passthrough;
              header_name_redirect_bypass = header_name_redirect_bypass;

              errordocument = {
                status = 200;
                content = "ok";
              };
            }; -- webauth
        }; -- accesslog
      }; -- http
    }; --default
  }; --ipdispatch
}; --instance
