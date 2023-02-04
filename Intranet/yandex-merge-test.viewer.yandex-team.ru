upstream yandex-merge-test-viewer-upstream {
server lya.search.yandex.net:5160;
}

server {
    server_name yandex-merge-test.viewer.*;
    include includes/server-section-with-acl;

    location / {
         proxy_pass http://yandex-merge-test-viewer-upstream;
         include includes/location-section;
    }
}
