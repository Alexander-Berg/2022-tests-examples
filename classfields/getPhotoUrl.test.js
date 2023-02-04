const getPhotoUrl = require('./getPhotoUrl');

const CATTOUCH_MAIN_PHOTO_URL = '//avatars.mds.yandex.net/get-verba/937147/2a0000016093cf55468a649dd20a1e870244/cattouch';
const WIZARDV3MR_MAIN_PHOTO_URL = '//avatars.mds.yandex.net/get-verba/937147/2a0000016093cf55468a649dd20a1e870244/wizardv3mr';
const CATTOUCH_RENDER_URL = '//avatars.mds.yandex.net/get-verba/1604130/2a000001679779c0126f6646468be1cef964/cattouch';
const WIZARDV3MR_RENDER_URL = '//avatars.mds.yandex.net/get-verba/1604130/2a000001679779c0126f6646468be1cef964/wizardv3mr';

const COMPLECTATION = {
    tech_info: {
        configuration: {
            main_photo: {
                sizes: {
                    cattouch: CATTOUCH_MAIN_PHOTO_URL,
                    wizardv3mr: WIZARDV3MR_MAIN_PHOTO_URL,
                },
            },
        },
        complectation: {
            vendor_colors: [
                {
                    photos: [
                        {
                            name: '34-back',
                            sizes: {
                                cattouch: CATTOUCH_RENDER_URL,
                                wizardv3mr: WIZARDV3MR_RENDER_URL,
                            },
                        },
                    ],
                },
            ],
        },
    },
};

it('должен вернуть урл рендера в дефолтном размере при наличии', () => {
    expect(getPhotoUrl(COMPLECTATION)).toBe(CATTOUCH_RENDER_URL);
});

it('должен вернуть урл рендера в указанном размере при наличии', () => {
    expect(getPhotoUrl(COMPLECTATION, 'wizardv3mr')).toBe(WIZARDV3MR_RENDER_URL);
});

it('должен вернуть урл промо фото в дефолтном размере если нет рендеров', () => {
    const complectation = { ... COMPLECTATION, tech_info: { ... COMPLECTATION.tech_info, complectation: {} } };
    expect(getPhotoUrl(complectation)).toBe(CATTOUCH_MAIN_PHOTO_URL);
});

it('должен вернуть урл промо фото в заданном размере если нет рендеров', () => {
    const complectation = { ... COMPLECTATION, tech_info: { ... COMPLECTATION.tech_info, complectation: {} } };
    expect(getPhotoUrl(complectation, 'wizardv3mr')).toBe(WIZARDV3MR_MAIN_PHOTO_URL);
});
