const userPreparer = require('./user');

it('должен привести ответ /user в правильный вид для авторизованного пользователя', () => {
    const result = {
        user: {
            id: '19201127',
            profile: {
                autoru: {
                    alias: 'Natal naf',
                    birthday: '1899-11-25',
                    about: 'привет!',
                    show_card: true,
                    show_mail: true,
                    allow_messages: true,
                    driving_year: 1963,
                    country_id: '1',
                    region_id: '87',
                    city_id: '1123',
                    full_name: 'кукукукукуукукуруз',
                    geo_id: '213',
                },
            },
            registration_date: '2016-10-25',
            active: true,
            emails: [
                {
                    email: 'natix@yandex-team.ru',
                    confirmed: true,
                },
            ],
            phones: [
                {
                    phone: '71234567890',
                    added: '2018-02-22T17:21:27Z',
                },
            ],
            social_profiles: [
                {
                    provider: 'MAILRU',
                    social_user_id: '6013748787654302824',
                    added: '2018-05-31T14:24:20Z',
                    nickname: 'natix',
                    first_name: 'Игорь',
                    last_name: 'Стуев',
                },
                {
                    provider: 'YANDEX',
                    social_user_id: '730613650',
                    added: '2018-09-25T10:07:51Z',
                    nickname: 'koti4.k',
                    first_name: 'котя',
                    last_name: 'котя',
                },
                {
                    provider: 'GOOGLE',
                    social_user_id: '102400941953673457105',
                    added: '2018-11-29T13:36:28Z',
                    first_name: 'Natix',
                    last_name: 'Naf',
                },
            ],
            registration_ip: '91.213.144.71',
            yandex_staff_login: 'natix',
        },
        tied_cards: [
            {
                id: '555554444',
                card_mask: '5555554444',
                ps_id: 'YANDEXKASSA_V3',
                properties: {
                    cdd_pan_mask: '555555|4444',
                    brand: 'MASTERCARD',
                    expire_year: '2020',
                    expire_month: '12',
                },
            },
            {
                id: '444444448',
                card_mask: '4444444448',
                ps_id: 'YANDEXKASSA',
                properties: {
                    cdd_pan_mask: '444444|4448',
                    brand: 'VISA',
                },
            },
        ],
        user_balance: '3',
        allow_code_login: false,
        access: {},
        status: 'SUCCESS',
    };

    expect(userPreparer({ result: { result } })).toEqual({
        id: '19201127',
        profile: {
            autoru: {
                allow_code_login: false,
                alias: 'Natal naf',
                birthday: '1899-11-25',
                about: 'привет!',
                show_card: true,
                show_mail: true,
                allow_messages: true,
                driving_year: 1963,
                country_id: '1',
                region_id: '87',
                city_id: '1123',
                full_name: 'кукукукукуукукуруз',
                geo_id: 213,
                geo_name: 'Москва',
            },
        },
        yandex_staff_login: 'natix',
        auth: true,
        emails: [ { email: 'natix@yandex-team.ru', confirmed: true } ],
        tied_cards: [
            {
                id: '555554444',
                card_mask: '5555554444',
                ps_id: 'YANDEXKASSA_V3',
                properties: {
                    cdd_pan_mask: '555555|4444',
                    brand: 'MASTERCARD',
                    expire_year: '2020',
                    expire_month: '12',
                },
            },
            {
                id: '444444448',
                card_mask: '4444444448',
                ps_id: 'YANDEXKASSA',
                properties: {
                    cdd_pan_mask: '444444|4448',
                    brand: 'VISA',
                },
            },
        ],
        balance: 3,
        user_balance: '3',
        phones: [
            {
                phone: '71234567890',
                added: '2018-02-22T17:21:27Z',
            },
        ],
        registration_date: '2016-10-25',
        social_profiles: [
            {
                provider: 'MAILRU',
                social_user_id: '6013748787654302824',
                added: '2018-05-31T14:24:20Z',
                nickname: 'natix',
                first_name: 'Игорь',
                last_name: 'Стуев',
            },
            {
                provider: 'YANDEX',
                social_user_id: '730613650',
                added: '2018-09-25T10:07:51Z',
                nickname: 'koti4.k',
                first_name: 'котя',
                last_name: 'котя',
            },
            {
                provider: 'GOOGLE',
                social_user_id: '102400941953673457105',
                added: '2018-11-29T13:36:28Z',
                first_name: 'Natix',
                last_name: 'Naf',
            },
        ],
    });
});

it('должен привести ответ /login в правильный вид для авторизованного пользователя', () => {
    const result = {
        session: {
            id: '19201127|1566811825155.7776000.2H0qjE95ZP4I0j4hb-Cjkg.nBvsI7wAzsHWcPw1zAJm2uuVWGRNlNje_7oPoD0MzSI',
            user_id: '19201127',
            device_uid: 'g5d63a6b127sr5ofare7a6r5747lg8uk.6c2337f10ff2a4a76317414f00a58302',
            creation_timestamp: '2019-08-26T09:30:25.155Z',
            expire_timestamp: '2019-11-24T09:30:25.155Z',
            ttl_sec: 7776000,
        },
        user: {
            id: '19201127',
            profile: {
                autoru: {
                    alias: 'Natal naf',
                    birthday: '1899-11-25',
                    about: 'привет!',
                    show_card: true,
                    show_mail: true,
                    allow_messages: true,
                    driving_year: 1963,
                    country_id: '1',
                    region_id: '87',
                    city_id: '1123',
                    full_name: 'кукукукукуукукуруз',
                    geo_id: '213',
                },
            },
            registration_date: '2016-10-25',
            active: true,
            emails: [
                {
                    email: 'natix@yandex-team.ru',
                    confirmed: true,
                },
            ],
            phones: [
                {
                    phone: '71234567890',
                    added: '2018-02-22T17:21:27Z',
                },
            ],
            social_profiles: [
                {
                    provider: 'MAILRU',
                    social_user_id: '6013748787654302824',
                    added: '2018-05-31T14:24:20Z',
                    nickname: 'natix',
                    first_name: 'Игорь',
                    last_name: 'Стуев',
                },
                {
                    provider: 'YANDEX',
                    social_user_id: '730613650',
                    added: '2018-09-25T10:07:51Z',
                    nickname: 'koti4.k',
                    first_name: 'котя',
                    last_name: 'котя',
                },
                {
                    provider: 'GOOGLE',
                    social_user_id: '102400941953673457105',
                    added: '2018-11-29T13:36:28Z',
                    first_name: 'Natix',
                    last_name: 'Naf',
                },
            ],
            registration_ip: '91.213.144.71',
            yandex_staff_login: 'natix',
        },
        status: 'SUCCESS',
    };

    expect(userPreparer({ result: { result } })).toEqual({
        id: '19201127',
        profile: {
            autoru: {
                alias: 'Natal naf',
                birthday: '1899-11-25',
                about: 'привет!',
                show_card: true,
                show_mail: true,
                allow_messages: true,
                driving_year: 1963,
                country_id: '1',
                region_id: '87',
                city_id: '1123',
                full_name: 'кукукукукуукукуруз',
                geo_id: 213,
                geo_name: 'Москва',
            },
        },
        tied_cards: undefined,
        user_balance: undefined,
        yandex_staff_login: 'natix',
        auth: true,
        emails: [ { email: 'natix@yandex-team.ru', confirmed: true } ],
        phones: [
            {
                phone: '71234567890',
                added: '2018-02-22T17:21:27Z',
            },
        ],
        registration_date: '2016-10-25',
        social_profiles: [
            {
                provider: 'MAILRU',
                social_user_id: '6013748787654302824',
                added: '2018-05-31T14:24:20Z',
                nickname: 'natix',
                first_name: 'Игорь',
                last_name: 'Стуев',
            },
            {
                provider: 'YANDEX',
                social_user_id: '730613650',
                added: '2018-09-25T10:07:51Z',
                nickname: 'koti4.k',
                first_name: 'котя',
                last_name: 'котя',
            },
            {
                provider: 'GOOGLE',
                social_user_id: '102400941953673457105',
                added: '2018-11-29T13:36:28Z',
                first_name: 'Natix',
                last_name: 'Naf',
            },
        ],
    });
});

it('должен привести ответ /user в правильный вид для неавторизованного пользователя', () => {
    const result = { status: 'SUCCESS' };

    expect(userPreparer({ result: { result } })).toEqual({
        auth: false,
    });
});
