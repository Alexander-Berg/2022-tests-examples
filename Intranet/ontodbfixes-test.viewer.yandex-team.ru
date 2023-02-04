upstream ontodbfixes-test-viewer-upstream {
server sas1-4895.search.yandex.net:1027;
}

server { 
    server_name ontodbfixes-test.viewer.*;
    include includes/server-section-with-acl;

    location / {
         proxy_read_timeout 10m;
         proxy_pass http://ontodbfixes-test-viewer-upstream;
         include includes/location-section;
    }
}
