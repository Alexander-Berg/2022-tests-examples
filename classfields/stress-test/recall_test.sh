#!/usr/bin/env bash
HOST="realty-recommender.vrts-slb.test.vertis.yandex.net"
# HOST="vgorovoy-02-sas.dev.vertis.yandex.net:8897"


rgids=(734138 42)
offer_types=(RENT SELL)
category_types=(ROOMS APARTMENT HOUSE LOT COMMERCIAL GARAGE)
uids=(65173853 42)



for r in ${rgids[*]}
do
  for ot in ${offer_types[*]}
  do
    for ct in ${category_types[*]}
    do
      for uid in ${uids[*]}
      do
         curl 2>/dev/null -I "http://$HOST/api/v1/get_recommendations/uid/$uid?rgid=$r&offer_type=$ot&category_type=$ct" > "t_"${uid}"_"${r}"_"${ot}"_"${ct}
      done
    done
  done
done

for f in `ls t_*`
do
    err=`cat "$f"|grep "HTTP/1.1 500 Internal Server Error"|wc -l`
    if (( "$err" > 0 ))
    then
        echo "Failed for $f"
    else
        rm "$f"
    fi
done

if (( `ls "t_*" 2>/dev/null |wc -l` == 0 ))
then
    echo "No 5XX found"
fi
