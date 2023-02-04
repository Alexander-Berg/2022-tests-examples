export const personCategories = {
    request: {},
    response: [
        { category: 'am_jp', name: 'ID_Legal_entity_AM' },
        { category: 'am_np', name: 'ID_Individual_AM' },
        { category: 'az_ur', name: 'ID_Legal_entity_AZ' }
    ]
};

export const history = {
    perms: ['AdminAccess', 'ViewPersons'],
    search:
        '?name=%D0%9A%D0%A3%D0%9C%D0%98%D0%A2&type=ur&inn=7707813050&id=5687027&email=coomeetyour%40yandex.ru&is_partner=false&vip_only=true&pn=1&ps=10',
    filter: {
        name: 'КУМИТ',
        personType: 'ur',
        personId: '5687027',
        inn: '7707813050',
        email: 'coomeetyour@yandex.ru',
        isPartner: false,
        vipOnly: true
    },
    personList: {
        request: {
            name: 'КУМИТ',
            inn: '7707813050',
            email: 'coomeetyour@yandex.ru',
            personType: 'ur',
            personId: '5687027',
            isPartner: false,
            paginationPn: 1,
            paginationPs: 10,
            vipOnly: true
        },
        response: {
            total_row_count: 1,
            items: [
                {
                    kpp: '770701001',
                    invoice_count: 3,
                    name: '\u041a\u0423\u041c\u0418\u0422',
                    client_name: '\u041a\u0443\u043c\u0438\u0442',
                    email: 'coomeetyour@yandex.ru',
                    inn: '7707813050',
                    client_id: 42807396,
                    is_partner: false,
                    hidden: false,
                    type: 'ur',
                    id: 5687027
                }
            ]
        }
    }
};
