ADDRESS=http://localhost:7990/bitbucket/plugins/servlet/teamcity-hook
VCS_ROOT_URL=http://localhost:7990/project_1/rep_1.git
sed "s|ssh://git@bb.yandex-team.ru/mobile/monorepo.git|$VCS_ROOT_URL|" ./post_request.json | xargs -0 curl -u admin:admin -H "Content-Type: application/json" -X POST $ADDRESS -d
sed "s|tc://one|tc://two|; s|\"buildResult\": \"success\"|\"buildResult\": \"failure\"|; s|ssh://git@bb.yandex-team.ru/mobile/monorepo.git|$VCS_ROOT_URL|" ./post_request.json | xargs -0 curl -u admin:admin -H "Content-Type: application/json" -X POST $ADDRESS -d
sed "s|tc://one|tc://three|; s|\"buildResult\": \"success\"|\"buildResult\": \"running\"|; s|ssh://git@bb.yandex-team.ru/mobile/monorepo.git|$VCS_ROOT_URL|" ./post_request.json | xargs -0 curl -u admin:admin -H "Content-Type: application/json" -X POST $ADDRESS -d
