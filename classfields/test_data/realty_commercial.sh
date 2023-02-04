#!/usr/bin/env bash

HOST="back-rt-01-sas.test.vertis.yandex.net"
SERVICE="realty_commercial"
BASE_URL="http://$HOST:34100/api/1.x/service/$SERVICE"

H_CT="Content-Type: application/json"
H_USER="X-Billing-User: puid:211377877"

CLIENT_ID="38846435"
ORDER_ID="100500"

CAMPAIGN_URL="$BASE_URL/customer/client/$CLIENT_ID/campaign"

CUSTOMER_BODY="{
  \"capaPartnerId\": \"1069059670\"
}"

ATTACH_BODY="{
  \"text\": \"Яндекс.Комм.Недвижимость, Dizzy realty\"
}"

CODE=$(curl -o /dev/null -s -w "%{http_code}" -X POST -d "$CUSTOMER_BODY" -H "$H_CT" -H "$H_USER" "$BASE_URL/customer/client/$CLIENT_ID")

if [[ $CODE != 200 ]]; then
  echo "Can't create customer"
  exit 0
fi

CODE=$(curl -o /dev/null -s -w "%{http_code}" -X POST -d "$ATTACH_BODY" -H "$H_CT" -H "$H_USER" "$BASE_URL/customer/client/$CLIENT_ID/order/$ORDER_ID/attach")

if [[ $CODE != 200 ]]; then
  echo "Can't attach order"
  exit 0
fi

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "3",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "id": "feed_promotion",
        "cost": {
          "costPerIndexing": 300
        }
      }
    ]
  },
  "settings": {
    "enabled": true
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "1",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "placement": {
          "costPerIndexing": 100
        }
      }
    ]
  },
  "settings": {
    "enabled": true
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "объявление",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "id": "feed_placement",
        "cost": {
          "costPerIndexing": 100
        }
      }
    ]
  },
  "settings": {
    "enabled": true
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "39",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "id": "feed_premium",
        "cost": {
          "costPerIndexing": 3900
        }
      }
    ]
  },
  "settings": {
    "enabled": true
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "19",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "id": "feed_raise",
        "cost": {
          "costPerIndexing": 1900
        }
      }
    ]
  },
  "settings": {
    "enabled": true
  }
}
EOF

