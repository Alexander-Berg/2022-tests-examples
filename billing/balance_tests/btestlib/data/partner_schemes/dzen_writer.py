# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

true, false = True, False

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('ph',
{"contracts": [
    {
      "ctype": "SPENDABLE",
      "tag": "dzen_writer_ph",
      "firm": [28, 1],
      "services": {
        "mandatory": [
          134
        ]
      },
      "_rules_constants": {
        "flags": {"is_zen": false}
      },
      "_params": {
        "enable_setting_attributes": 1,
        "enable_validating_attributes": 1
      }
    }
  ],
  "thirdparty_processing": [
    {
      "service_ids": [
        134
      ],
      "pipeline": [
        "GetId",
        "PartnerUnit",
        {
          "unit": "ContractUnit",
          "params": {
            "multicurrency": true,
            "contract_filters": [
                        {"filter": {"name": "service"}},
                        {"filter": {"name": "partner"}},
                        {"filter": {"name": "transaction_dt"}},
                        {
                            "filter": {
                                "name": "balalayka",
                                "params": {
                                    "firm_resident_id": {"$in": [28, 1]},
                                    "firm_nonresident_id": 66666666
                                }
                            }
                        },
                        {"filter": {"name": "contract_type", "params": {"contract_type": "SPENDABLE"}}}
                    ]
          }
        },
        "AmountUnit",
        {"if":  {"rr.cs.firm":  {"$eq":  28}},
         "then": "AddNDFL2amount" },
        "OEBSOrgID",
        "ExtractSideTaxiPromoIDs"
      ]
    }
  ],
  "close_month": [
    {
      "contract_tag": "dzen_writer_ph",
      "month_close_generator": "NullGenerator"
    }
  ]
}
)

register('non_resident',
{
  "contracts": [
    {
      "ctype": "SPENDABLE",
      "tag": "dzen_writer_nonresident",
      "firm": 16,
      "services": {
        "mandatory": [
          134
        ]
      },
      "_rules_constants": {
        "flags": {"is_zen": false}
      },
      "_params": {
        "enable_setting_attributes": 1,
        "enable_validating_attributes": 1
      }
    }
  ],
  "thirdparty_processing": [
    {
      "service_ids": [
        134
      ],
      "pipeline": [
        "GetId",
        "PartnerUnit",
        {
          "unit": "ContractUnit",
          "params": {
            "multicurrency": true,
            "contract_filters": [
                        {"filter": {"name": "service"}},
                        {"filter": {"name": "partner"}},
                        {"filter": {"name": "transaction_dt"}},
                        {"filter": {"name": "contract_type", "params": {"contract_type": "SPENDABLE"}}}
                    ]
          }
        },
        "SetInternal",
        "AmountUnit",
        "OEBSOrgID",
        "ExtractSideTaxiPromoIDs"
      ]
    }
  ],
  "close_month": [
    {
      "contract_tag": "dzen_writer_nonresident",
      "month_close_generator": "NullGenerator"
    }
  ]
}
)

register('ur_ip',
{
  "contracts": [
    {
      "ctype": "SPENDABLE",
      "tag": "dzen_writer_ur_ip",
      "firm": [28, 1],
      "services": {
        "mandatory": [
          696
        ]
      },
      "_rules_constants": {
        "flags": {"is_zen_ur": false}
      },
      "_params": {
        "enable_setting_attributes": 1,
        "enable_validating_attributes": 1
      }
    }
  ],
  "thirdparty_processing": [
    {
      "service_ids": [
        696
      ],
      "pipeline": [
        "GetId",
        "PartnerUnit",
        {
          "unit": "ContractUnit",
          "params": {
            "contract_filters": [
                        {"filter": {"name": "service"}},
                        {"filter": {"name": "partner"}},
                        {"filter": {"name": "transaction_dt"}},
                        {"filter": {"name": "contract_type", "params": {"contract_type": "SPENDABLE"}}}
                    ]
          }
        },
        "AmountUnit",
        "AddContractNds2amount",
        "OEBSOrgID",
        "ExtractSideTaxiPromoIDs"
      ]
    }
  ],
  "close_month": [
    {
      "contract_tag": "dzen_writer_ur_ip",
      "month_close_generator": "SpendablePartnerProductGenerator"
    }
  ]
}
)

register('ur_org',
{
  "contracts": [
    {
      "ctype": "SPENDABLE",
      "tag": "dzen_writer_ur_org",
      "firm": [28, 1],
      "services": {
        "mandatory": [
          695
        ]
      },
      "_rules_constants": {
        "flags": {"is_zen_ur": false}
      },
      "_params": {
        "enable_setting_attributes": 1,
        "enable_validating_attributes": 1
      }
    }
  ],
  "thirdparty_processing": [
    {
      "service_ids": [
        695
      ],
      "pipeline": [
        "GetId",
        "PartnerUnit",
        {
          "unit": "ContractUnit",
          "params": {
            "contract_filters": [
                        {"filter": {"name": "service"}},
                        {"filter": {"name": "partner"}},
                        {"filter": {"name": "transaction_dt"}},
                        {"filter": {"name": "contract_type", "params": {"contract_type": "SPENDABLE"}}}
                    ]
          }
        },
        "AmountUnit",
        "AddContractNds2amount",
        "SetInternal",
        "OEBSOrgID",
        "ExtractSideTaxiPromoIDs"
      ]
    }
  ],
  "close_month": [
    {
      "contract_tag": "dzen_writer_ur_org",
      "month_close_generator": "SpendablePartnerProductGenerator"
    }
  ]
}
)
