const sessionPreparer = require('./session');

let context;
beforeEach(() => {
    context = {
        req: {
            query: {},
        },
    };
});

it('должен привести ответ /session в правильный вид для авторизованного пользователя', () => {
    const result = {
        session: {
            id: '19201127|1566811328284.7776000.NMB1JDqoLawQ6_Xo0gjpBA.X7gcY00HVS0Riwp4tDHqGL-yY1D2jB0_VJbKq5nZdPk',
            user_id: '19201127',
            device_uid: 'g5d5d70a32ncpoipgmipstf8v2uojsfb.cb5e442949619ec6294b1ece2e06b6bf',
            creation_timestamp: '2019-08-26T09:22:08.284Z',
            expire_timestamp: '2019-11-24T09:22:08.284Z',
            ttl_sec: 7776000,
        },
        user: {
            id: '19201127',
            email: 'natix@yandex-team.ru',
            profile: { alias: 'Natal naf', full_name: 'кукукукукуукукуруз' },
            registration_date: '2016-10-25',
            registration_ip: '91.213.144.71',
            yandex_staff_login: 'natix',
            yandex_social: true,
            phones: [ '79619997766' ],
        },
        trusted: true,
        grants: {
            grants: [
                'MODERATOR_REALTY',
                'MODERATOR_AUTORU',
                'MODERATOR_USERS_AUTORU',
                'MODERATOR_TELEPONY',
                'MODERATOR_USERS_REALTY',
                'MODERATOR_STO_MODERATOR',
            ],
        },
    };

    expect(sessionPreparer({ context, result: { result } })).toEqual({
        id: '19201127',
        profile: { autoru: { alias: 'Natal naf', full_name: 'кукукукукуукукуруз' } },
        yandex_staff_login: 'natix',
        yandex_social: true,
        auth: true,
        emails: [ { email: 'natix@yandex-team.ru', confirmed: true } ],
        hashEmail: 'd276affa980695571c77efeb5db1c1209e2fa6d3ea76f460b7bd045ede84edf4',
        name: 'Natal naf',
        isModerator: true,
        isUserProfileModerator: true,
        phones: [ { phone: '79619997766' } ],
    });
});

it('должен привести ответ /session в правильный вид для неавторизованного пользователя', () => {
    const result = {
        session: {
            id: 'a:g5d63ae732bliv5l5a9ssb6oa75m73at.770dbe55018fff96e3da488d2cabc9a5|1566813811672.',
            device_uid: 'g5d63ae732bliv5l5a9ssb6oa75m73at.770dbe55018fff96e3da488d2cabc9a5',
            creation_timestamp: '2019-08-26T10:03:31.672Z',
            expire_timestamp: '2019-09-02T10:03:31.672Z',
            ttl_sec: 604800,
        },
        trusted: true,
    };

    expect(sessionPreparer({ context, result: { result } })).toEqual({
        auth: false,
    });
});
