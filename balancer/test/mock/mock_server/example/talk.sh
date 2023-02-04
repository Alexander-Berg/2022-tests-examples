GREEN='\033[0;32m'
NC='\033[0m'
printf "${GREEN}"
echo Need to reload server to make it read responses on any address
echo curl http://127.0.0.1:17000/admin?action=reload_static_responses
printf "${NC}"
curl http://127.0.0.1:17000/admin?action=reload_static_responses -v

printf "${GREEN}"
echo curl http://127.0.0.2:17000/hello
printf "${NC}"
curl http://127.0.0.2:17000/hello -v

printf "${GREEN}"
echo Get all requests to this address
echo curl http://127.0.0.2:17000/admin?action=get_all_requests
printf "${NC}"
curl http://127.0.0.2:17000/admin?action=get_all_requests -v
