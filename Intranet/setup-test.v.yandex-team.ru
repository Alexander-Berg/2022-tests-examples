server { 
    server_name setup-test.v.*;
    include includes/server-section-with-acl;

    location / { return 301 https://setup-test.yandex-team.ru/; }
}
