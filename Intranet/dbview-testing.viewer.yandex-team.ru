upstream dbview-testing-viewer-upstream {
server xya.search.yandex.net:80;
}

server {
    server_name dbview-testing.viewer.*;
    include includes/server-section-with-acl;

    location / {
         proxy_pass http://dbview-testing-viewer-upstream;
         include includes/location-section;
    }
}
