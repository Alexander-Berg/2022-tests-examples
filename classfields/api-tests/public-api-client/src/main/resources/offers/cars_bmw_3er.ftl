<#-- @ftlvariable name="data" type="ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData" -->
{
  "car_info": {
    "mark": "BMW",
    "model": "3ER",
    "super_gen_id": 7744658,
    "configuration_id": 7744669,
    "tech_param_id": 20389857,
    "drive": "REAR_DRIVE",
    "body_type": "SEDAN",
    "transmission": "MECHANICAL",
    "engine_type": "GASOLINE",
    "wheel_left": true,
    "armored": false,
    "year": 2012
  },
  "seller": {
    "name": "test test",
    "location": {
      "address": "address",
      "geobase_id": 213
    },
    "phones": [
      {
        "phone": "${data.phone}",
        "call_hour_start": 10,
        "call_hour_end": 20
      }
    ],
    "redirect_phones": false,
    "unconfirmed_email": "cars@blah.ru"
  },
  "state": {
    "state_not_beaten": true,
    "used": true,
    "mileage": 607733,
    "in_stock": true
  },
  "documents": {
    "license_plate": "${data.licensePlate}",
    "vin": "${data.vin}",
    "year": 2012,
    "warranty": false,
    "custom_cleared": true,
    "pts": "ORIGINAL",
    "owners_number": 1
  },
  "price_info": {
    "price": 46442714,
    "currency": "RUR"
  },
  "additional_info": {
    "hidden": false,
    "exchange": false,
    "not_disturb": true
  },
  "description": "description",
  "color_hex": "040001",
  "section": "USED",
  "availability": "IN_STOCK",
  "discount_options": {
    "tradein": 10,
    "insurance": 20,
    "credit": 30,
    "max_discount": 40
  }
}
