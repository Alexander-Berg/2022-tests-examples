<#-- @ftlvariable name="data" type="ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData" -->
{
  "truck_info": {
    "loading": 3000,
    "light_truck_type": "TIPPER",
    "gear": "FRONT",
    "engine": "GASOLINE",
    "transmission": "AUTOMATIC",
    "seats": 16,
    "displacement": 50000,
    "horse_power": 3000,
    "steering_wheel": "LEFT",
    "truck_category": "LCV",
    "mark": "MAZDA",
    "model": "BONGO",
    "equipment": {
      "cruise-control": true,
      "seats-heat2": true,
      "trip-computer": true,
      "airbag2": true,
      "electro-window": true,
      "dvd": true
    }
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
    "unconfirmed_email": "trucks@blah.ru",
    "chats_enabled": true
  },
  "state": {
    "state_not_beaten": true,
    "used": true,
    "mileage": 607733,
    "in_stock": true
  },
  "documents": {
    "year": 2010,
    "warranty": false,
    "custom_cleared": true,
    "pts": "ORIGINAL",
    "owners_number": 1,
    "license_plate": "${data.licensePlate}",
    "vin": "${data.vin}"
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
  "description": "trucks description",
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