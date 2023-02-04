{
  "ofd_info": {
    "ofd_addr": "test.kkt.ofd.yandex.net:12345",
    "check_url": "nalog.ru",
    "transport": "0",
    "oism_addr": "test.kkt.ofd.yandex.net:54321",
    "oism_timeout": 60,
    "okp_addr": "prod01.okp-fn.ru:26101",
    "okp_timeout": 30
  },
  "reg_info": {
    "user_inn": "${firm.inn}",
    "rnm": "${rnm}",
    "tax_mode": ["OSN"],
    "work_mode": [<#list work_mode as wm>"${wm}"<#sep>, </#sep></#list>],
    "extended_work_mode": ["marking"],
    "user_name": "${firm.name?no_esc}",
    "account_address": "${firm.address}",
    "representative_for_registration": "${firm.representative}",
    "ofd_inn": "7704358518",
    "representative_inn": "",
    "account_address_name": "Стойка номер 666",
    "ofd_name": "ОФД Яндекс",
    "agent_mode": ["none_agent"],
    "terminal_number": "6660666",
    "user_reply_email": "noname@yandex-team.ru"
  },
  "custom_data": {
    "use_complex": true
  },
  "device_payload": {
    "password": 666666
  },
  "mysecret": "${mysecret}"
}
