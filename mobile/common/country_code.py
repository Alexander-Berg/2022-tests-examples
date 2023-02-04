# -*- coding: utf-8 -*-
from iso3166 import countries as iso_countries

all_countries_set = frozenset([c.alpha2 for c in iso_countries] + ['AB', 'OS'])

NOT_ISO_COUNTRY_MAP = {
    'UK': {'GB', },  # United Kingdom = Great Britain
    # Europe Union (http://dev.maxmind.com/geoip/legacy/codes/eu_country_list/)
    'EU': {'AD', 'AL', 'AT', 'BA', 'BE', 'BG', 'BY', 'CH', 'ME', 'RS', 'CZ', 'DE', 'DK', 'EE', 'ES', 'FI',
           'FO', 'FR', 'GB', 'GI', 'GR', 'HR', 'HU', 'IE', 'IS', 'IT', 'LI', 'LT', 'LU', 'LV', 'MC', 'MD',
           'MK', 'MT', 'NL', 'NO', 'PL', 'PT', 'RO', 'SE', 'SI', 'SJ', 'SK', 'SM', 'UA', 'VA', 'BQ', 'CW',
           'SX'},
    # Asia/Pacific Region (http://dev.maxmind.com/geoip/legacy/codes/ap_country_list/)
    'AP': {'AE', 'AF', 'AM', 'AU', 'AZ', 'BD', 'BH', 'BN', 'BT', 'CC', 'CN', 'CX', 'CY', 'GE', 'HK', 'ID',
           'IL', 'IN', 'IO', 'IQ', 'IR', 'JO', 'JP', 'KG', 'KH', 'KP', 'KR', 'KW', 'KZ', 'LA', 'LB', 'LK',
           'MM', 'MN', 'MO', 'MV', 'MY', 'NP', 'NZ', 'OM', 'PH', 'PK', 'PS', 'QA', 'RU', 'SA', 'SG', 'SY',
           'TH', 'TJ', 'TL', 'TM', 'TR', 'TW', 'UZ', 'VN', 'YE', },
    # Canary Islands is Spain: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#IC
    'IC': {'ES', },
    'EA': {'ES', },
    # Netherlands Antilles divided into BQ, CW, SX
    'AN': {'BQ', 'CW', 'SX'},
    'ANT': {'BQ', 'CW', 'SX'},
    # Burma changed to Myanmar (MM)
    'BU': {'MM', },
    # Yugoslavia - divided into Serbia and Montenegro
    'YU': {'ME', 'RS'},
    'CS': {'ME', 'RS'},
    # Zaire changed name to Congo, the Democratic Republic of the
    'ZR': {'CD', },
    # International
    'INT': all_countries_set,
    'ALL': all_countries_set,
    # France Metropolitan
    'FX': {'FR', },
    # Portuguese Timor, name changed to Timor-Leste (TL)
    'TP': {'TL', },
}


def normalize_countries(countries_list):
    """
    Ensure country codes to be in legal ISO-3166-1 2-symbol (alpha-2) format
    :param countries_list: countries list
    :return: list of normalized countries
    """
    result = set()
    for country in countries_list:
        if country in iso_countries:
            result.add(iso_countries.get(country).alpha2.upper())
        elif country in NOT_ISO_COUNTRY_MAP:
            result.update(NOT_ISO_COUNTRY_MAP[country])
        else:
            raise AssertionError("Bad country code %s" % str(country))
    return list(result)
