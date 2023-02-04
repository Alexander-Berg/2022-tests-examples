<#-- @ftlvariable name="data" type="ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData" -->
{
  "moto_info": {
    "atv_type": "TOURIST",
    "displacement": 40000,
    "engine": "GASOLINE_INJECTOR",
    "cylinder_amount": "CYLINDERS_3",
    "horse_power": 2000,
    "gear": "BACK",
    "transmission": "MECHANICAL",
    "cylinder_order": "V_TYPE",
    "stroke_amount": "STROKES_2",
    "moto_category": "ATV",
    "mark": "ABM",
    "model": "ATV_90"
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
    "unconfirmed_email": "moto@blah.ru",
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