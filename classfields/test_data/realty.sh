#!/usr/bin/env bash

HOST="back-rt-01-sas.test.vertis.yandex.net"
SERVICE="realty"
BASE_URL="http://$HOST:34100/api/1.x/service/$SERVICE"

H_CT="Content-Type: application/json"
H_USER="X-Billing-User: puid:211377877"

CLIENT_ID="38846439"
ORDER_ID="100500"

CAMPAIGN_URL="$BASE_URL/customer/client/$CLIENT_ID/campaign"

CUSTOMER_BODY="{
  \"developerId\": \"312637\"
}"

ATTACH_BODY="{
  \"text\": \"Яндекс.Недвижимость, Dizzy realty\"
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
  "name": "Дюна",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "placement": {
          "costPerCall": 1105000
        }
      }
    ]
  },
  "settings": {
    "enabled": true,
    "callSettings": {
      "phone": {
        "country": "7",
        "code": "495",
        "phone": "1234567"
      },
      "redirect": {
        "country": "7",
        "code": "495",
        "phone": "3634998"
      },
      "redirectId": "1hOuEeklMe8",
      "source": "Telepony"
    }
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "Ленсоветовский",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "placement": {
          "costPerCall": 9110000
        }
      }
    ]
  },
  "settings": {
    "enabled": true,
    "callSettings": {
      "phone": {
        "country": "7",
        "code": "495",
        "phone": "1234567"
      },
      "redirect": {
        "country": "7",
        "code": "495",
        "phone": "3635431"
      },
      "redirectId": "U4bfNEf3Owk",
      "source": "Telepony"
    }
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "Московский (Красная Горка 2)",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "placement": {
          "costPerCall": 29055000
        }
      }
    ]
  },
  "settings": {
    "enabled": true,
    "callSettings": {
      "phone": {
        "country": "7",
        "code": "495",
        "phone": "2222222"
      },
      "redirect": {
        "country": "7",
        "code": "495",
        "phone": "3635409"
      },
      "redirectId": "KoEQmcV60Ug",
      "source": "Telepony"
    }
  }
}
EOF

curl -o /dev/null -s -X POST -d @- -H "$H_CT" -H "$H_USER" "$CAMPAIGN_URL" << EOF
{
  "name": "Родной Город. Каховская",
  "orderId": $ORDER_ID,
  "product": {
    "goods": [
      {
        "placement": {
          "costPerCall": 17435000
        }
      }
    ]
  },
  "settings": {
    "enabled": true,
    "callSettings": {
      "phone": {
        "country": "7",
        "code": "495",
        "phone": "3333333"
      },
      "redirect": {
        "country": "7",
        "code": "495",
        "phone": "3635406"
      },
      "redirectId": "EGndK1KhyMs",
      "source": "Telepony"
    }
  }
}
EOF

