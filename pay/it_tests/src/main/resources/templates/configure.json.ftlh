{
  "ofd_info": {
    "ofd_addr": "192.168.1.10:6666",
    "check_url": "nalog.ru",
    "transport": "0"
  },
  "reg_info": {
    "user_inn": "${firm.inn}",
    "rnm": "${rnm}",
    "tax_mode": ["OSN"],
    "work_mode": [<#list work_mode as wm>"${wm}"<#sep>, </#sep></#list>],
    "user_name": "${firm.name?no_esc}",
    "account_address": "${firm.address}",
    "representative_for_registration": "${firm.representative}",
    "ofd_inn": "7709364346",
    "representative_inn": "",
    "account_address_name": "Стойка номер 666",
    "ofd_name": "АО \"ЭНЕРГЕТИЧЕСКИЕ СИСТЕМЫ И КОММУНИКАЦИИ\"",
    "agent_mode": ["none_agent"],
    "terminal_number": "6660666",
    "user_reply_email": "noname@yandex-team.ru"
  },
  "device_payload": {
    "password": 666666
  },
  "mysecret": "${mysecret}"
}
