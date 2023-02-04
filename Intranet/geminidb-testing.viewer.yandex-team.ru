upstream geminidb-testing-viewer-upstream {
server gemini-dev2.search.yandex.net:4140;
}

server { 
    server_name geminidb-testing.viewer.*;
    include includes/server-section-with-acl;

    location / {
         proxy_pass http://geminidb-testing-viewer-upstream;
         include includes/location-section;
    }
}
