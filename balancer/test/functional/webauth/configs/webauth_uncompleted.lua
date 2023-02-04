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

              role = "some_role";
              auth_path = "/check_oauth_token";

              on_forbidden = {
                errordocument = {
                  status = 200;
                  content = "on_forbidden";
                };
              };

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
