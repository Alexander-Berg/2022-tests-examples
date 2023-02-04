# upstream callisto-test-viewer-upstream {
# server jupiter-beta.haze.yandex.net:9121;
# }

# server {
#     server_name callisto-test.viewer.*;

#     include includes/server-section-with-acl;

#     location / {
#          proxy_pass http://callisto-test-viewer-upstream;
#          include includes/location-section;
#     }
# }
