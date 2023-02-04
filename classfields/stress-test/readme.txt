curl -vvv -H "Host: realty-recommender.vrts-slb.test.vertis.yandex.net" http://lb-int-01-myt.test.vertis.yandex.net:80/ping

curl -vvv -H "Host: realty-recommender.vrts-slb.test.vertis.yandex.net" http://lb-int-01-myt.test.vertis.yandex.net:80/api/v1/get_recommendations/uid/655264198


curl -vvv -H "Host: realty-recommender.vrts-slb.test.vertis.yandex.net" http://2a02:6b8:c03:7b0:0:1459:730b:aeee:21152/api/v1/get_recommendations/uid/655264198

curl -vvv -H "Host: docker-01-myt.test.vertis.yandex.net" http://docker-01-myt.test.vertis.yandex.net:21152/api/v1/get_recommendations/uid/655264198

curl -vvv -H "Host: realty-recommender.vrts-slb.test.vertis.yandex.net" http://docker-01-myt.test.vertis.yandex.net:21152/api/v1/get_recommendations/uid/655264198