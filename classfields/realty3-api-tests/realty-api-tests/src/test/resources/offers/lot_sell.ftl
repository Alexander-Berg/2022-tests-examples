<#-- @ftlvariable name="data" type="ru.auto.tests.realtyapi.adaptor.offer.OfferTemplateData" -->
<#setting number_format="computer">
{
  "offer": {
    "currency": "RUR",
    "dealType": "DIRECT",
    "haggle": true,
    "lotArea": 111.0,
    "lotAreaUnit": "SOTKA",
    "lotType": "GARDEN",
    "mortgage": true,
    "price": ${data.price},
    "category": "LOT",
    "description": "текст",
    "location": {
      "address": "Россия, Санкт-Петербург, Евгеньевская улица, 2И",
      "country": 225,
      "latitude": 59.931232,
      "longitude": 30.378984,
      "rgid": 417899,
      "shortAddress": "Евгеньевская улица, 2И"
    },
    "type": "SELL",
    "dealStatus": "SALE",
    "photo": [
      "avatars.mdst.yandex.net/get-realty/2941/add.fd408b8150a55472961221f63a6802af.realty-api-vos",
      "avatars.mdst.yandex.net/get-realty/2941/add.d3345450002388ff74600a3d7a294140.realty-api-vos",
      "avatars.mdst.yandex.net/get-realty/2941/add.2fad932b1ab0d70adea96fa121ecec55.realty-api-vos",
      "avatars.mdst.yandex.net/get-realty/2941/add.13e6716b4cbc971a3f298555074eadee.realty-api-vos"
    ]
  }
}