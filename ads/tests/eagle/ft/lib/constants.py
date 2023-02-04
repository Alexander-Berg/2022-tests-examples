# -*- coding: utf-8 -*-

TSAR_SEARCH_VECTORS = {
    # model_id: vector_size
    5: 50,  # EVectorIds::JAMSHID_SEARCH_DSSM
    6: 50,  # EVectorIds::FUTURE_PRGG_DSSM
    11: 33,  # EVectorIds::QUERY_BANNER_DSSM
    12: 50,  # EVectorIds::AB_DSSM
    14: 1,  # EVectorIds::ORGANIC_CONV_SEARCH_DSSM
}

TSAR_BOTH_SEARCH_RSYA_VECTORS = {
    6: 50,  # EVectorIds::FUTURE_PRGG_DSSM
    12: 50,  # EVectorIds::AB_DSSM
}

TORCH_V2_VECTOR = {26: 64}

SEARCH_PERS_FAIL = 1 << 4
LT_SEARCH_FAIL = 1 << 5
LT_ADV_FAIL = 1 << 6
MARKET_DJ_FAIL = 1 << 7
MARKET_KVSAAS_FAIL = 1 << 13
