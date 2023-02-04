<#-- @ftlvariable name="data" type="ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData" -->
{
  "car_info": {
    "body_type": "COUPE",
    "drive": "REAR_DRIVE",
    "equipment": {
      "gbo": false,
      "turnbuckle": true
    },
    "engine_type": "GASOLINE",
    "mark": "FERRARI",
    "model": "F40",
    "super_gen_id": "20346158",
    "tech_param_id": "20467413",
    "transmission": "MECHANICAL",
    "steering_wheel": "LEFT",
    "configuration_id": "20467383"
  },
  "availability": "IN_STOCK",
  "category": "CARS",
  "color_hex": "EE1D19",
  "section": "USED",
  "price_info": {
    "price": "500000",
    "currency": "RUR"
  },
  "description": "",
  "discount_options": {
    "tradein": 0,
    "insurance": 0,
    "credit": 0,
    "max_discount": 0
  },
  "documents": {
    "custom_cleared": true,
    "license_plate": "",
    "owners_number": "1",
    "sts": "",
    "vin": "",
    "year": "1990",
    "pts": "ORIGINAL"
  },
  "state": {
    "condition": "CONDITION_OK",
    "state_not_beaten": true,
    "mileage": 1000,
    "image_urls": [],
    "disable_photo_reorder": false,
    "damages": []
  },
  "additional_info": {
    "exchange": false,
    "not_disturb": true,
    "hidden": false
  },
  "seller": {
    "name": "test test",
    "redirect_phones": false,
    "unconfirmed_email": "cars@blah.ru",
    "location": {
      "address": "address",
      "geobase_id": "213"
    },
    "phones": [{
        "call_hour_start": "9",
        "call_hour_end": "21",
        "phone": "${data.phone}"
    }]
  }
}
