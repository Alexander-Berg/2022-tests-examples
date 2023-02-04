export const personCategories = {
    version: { snout: '1.0.290', muzzle: 'UNKNOWN', butils: '2.170' },
    data: [
        { category: 'am_jp', name: 'ID_Legal_entity_AM' },
        { category: 'am_np', name: 'ID_Individual_AM' },
        { category: 'az_ur', name: 'ID_Legal_entity_AZ' },
        { category: 'byp', name: 'ID_Individual_BY' },
        { category: 'byu', name: 'ID_Legal_entity_BY' },
        { category: 'by_ytph', name: 'ID_Individual_nonresident_CIS' },
        { category: 'endbuyer_ph', name: 'ID_Individual' },
        { category: 'endbuyer_ur', name: 'ID_Legal_entity_or_Indiv_entrepr' },
        { category: 'endbuyer_yt', name: 'ID_Legal_entity_or_Indiv_entrepr' },
        { category: 'eu_ur', name: 'ID_Legal_entity_ND' },
        { category: 'eu_yt', name: 'ID_Nonresident_ND' },
        { category: 'hk_ur', name: 'ID_Legal_entity_HK' },
        { category: 'hk_yt', name: 'ID_Nonresident_HK' },
        { category: 'hk_ytph', name: 'ID_Individual_nonresident_HK' },
        { category: 'il_ur', name: 'ID_Legal_entity_IL' },
        { category: 'kzp', name: 'ID_Individual_KZ' },
        { category: 'kzu', name: 'ID_Legal_entity_KZ' },
        { category: 'ph', name: 'ID_Individual' },
        { category: 'ph_autoru', name: 'ID_Individual_AUTORU' },
        { category: 'pu', name: 'ID_Individual_UA' },
        { category: 'sw_ph', name: 'ID_Individual_SW' },
        { category: 'sw_ur', name: 'ID_Legal_entity_SW' },
        { category: 'sw_yt', name: 'ID_Nonresident_SW' },
        { category: 'sw_ytph', name: 'ID_Individual_nonresident_SW' },
        { category: 'trp', name: 'ID_Individual_TUR' },
        { category: 'tru', name: 'ID_Legal_entity_TUR' },
        { category: 'ua', name: 'ID_Legal_entity_UA' },
        { category: 'ur', name: 'ID_Legal_entity_or_Indiv_entrepr' },
        { category: 'ur_autoru', name: 'ID_Legal_entity_or_Indiv_entrepr' },
        { category: 'usp', name: 'ID_Individual_USA' },
        { category: 'usu', name: 'ID_Legal_entity_USA' },
        { category: 'yt', name: 'ID_Nonresident' },
        { category: 'yt_kzp', name: 'ID_Individual_NR_KZ' },
        { category: 'yt_kzu', name: 'ID_Legal_entity_NR_KZ' },
        { category: 'ytph', name: 'ID_Individual_nonresident' }
    ]
};

export const personList = {
    version: { snout: '1.0.312', muzzle: 'UNKNOWN', butils: '2.174' },
    data: {
        total_row_count: 1,
        items: [
            {
                kpp: null,
                invoice_count: 0,
                name: null,
                client_name: 'balance_test 2020-01-15 01:02:36.478286',
                email: null,
                inn: null,
                client_id: 111324722,
                is_partner: true,
                hidden: false,
                type: 'am_np',
                id: 82811711
            }
        ]
    }
};

export const personTypes = [
    {
        value: 'ALL',
        content: 'Все'
    },
    {
        value: 'am_jp',
        content: 'Юр. лицо-резидент, Армения'
    },
    {
        value: 'am_np',
        content: 'Физ. лицо-резидент, Армения'
    },
    {
        value: 'az_ur',
        content: 'Юр.Лицо, Азербайджан'
    },
    {
        value: 'byp',
        content: 'Физ. лицо, Республика Беларусь'
    },
    {
        value: 'byu',
        content: 'Юр. лицо, Республика Беларусь'
    },
    {
        value: 'by_ytph',
        content: 'Физ. лицо-нерезидент, СНГ'
    },
    {
        value: 'endbuyer_ph',
        content: 'Физ. лицо'
    },
    {
        value: 'endbuyer_ur',
        content: 'Юр. лицо или ПБОЮЛ'
    },
    {
        value: 'endbuyer_yt',
        content: 'Юр. лицо или ПБОЮЛ'
    },
    {
        value: 'eu_ur',
        content: 'Юр. лицо, Нидерланды'
    },
    {
        value: 'eu_yt',
        content: 'Нерезидент, Нидерланды'
    },
    {
        value: 'hk_ur',
        content: 'Юр. лицо, Гонконг'
    },
    {
        value: 'hk_yt',
        content: 'Нерезидент, Гонконг'
    },
    {
        value: 'hk_ytph',
        content: 'Физ. лицо-нерезидент, Гонконг'
    },
    {
        value: 'il_ur',
        content: 'Юр.Лицо, Израиль'
    },
    {
        value: 'kzp',
        content: 'Физ. лицо, Казахстан'
    },
    {
        value: 'kzu',
        content: 'Юр. лицо, Казахстан'
    },
    {
        value: 'ph',
        content: 'Физ. лицо'
    },
    {
        value: 'ph_autoru',
        content: 'Физ. лицо Авто.ру'
    },
    {
        value: 'pu',
        content: 'Физ. лицо, Украина'
    },
    {
        value: 'sw_ph',
        content: 'Физ. лицо, Швейцария'
    },
    {
        value: 'sw_ur',
        content: 'Юр. лицо, Швейцария'
    },
    {
        value: 'sw_yt',
        content: 'Нерезидент, Швейцария'
    },
    {
        value: 'sw_ytph',
        content: 'Физ. лицо-нерезидент, Швейцария'
    },
    {
        value: 'trp',
        content: 'Физ. лицо, Турция'
    },
    {
        value: 'tru',
        content: 'Юр. лицо, Турция'
    },
    {
        value: 'ua',
        content: 'Юр. лицо, Украина'
    },
    {
        value: 'ur',
        content: 'Юр. лицо или ПБОЮЛ'
    },
    {
        value: 'ur_autoru',
        content: 'Юр. лицо или ПБОЮЛ'
    },
    {
        value: 'usp',
        content: 'Физ. лицо, США'
    },
    {
        value: 'usu',
        content: 'Юр. лицо, США'
    },
    {
        value: 'yt',
        content: 'Нерезидент'
    },
    {
        value: 'yt_kzp',
        content: 'Физ. лицо, Нерезидент РФ, Казахстан'
    },
    {
        value: 'yt_kzu',
        content: 'Юр. лицо, Нерезидент РФ, Казахстан'
    },
    {
        value: 'ytph',
        content: 'Физ. лицо-нерезидент'
    }
];
