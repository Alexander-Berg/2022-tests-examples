const refinePhotos = require('./refinePhotos');

it('должен вернуть пустой массив, если нет фотографий', () => {
    expect(refinePhotos()).toEqual([]);
});

it('должен вернуть пустой массив, если фотографии есть, но нет sizes (не смогли загрузить их на сервер)', () => {
    expect(refinePhotos([ { name: '0.jpg' } ]))
        .toEqual([]);
});

it('должен вернуть Photo[], если фотографии есть', () => {
    expect(refinePhotos([
        {
            isUploading: false,
            name: '1043613-2ba53afd2f3bc53bdb8ff446bd7f3afb',
            preview: 'blob:https://auto.ru/9e8ea61e-94b9-4934-b42e-790a6aa6738d',
            file: {
                preview: 'blob:https://auto.ru/9e8ea61e-94b9-4934-b42e-790a6aa6738d',
            },
            sizes: {
                thumb_m: '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/thumb_m',
                '60x45': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/60x45',
                '832x624': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/832x624',
                full: '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/full',
                '320x240': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/320x240',
                '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/1200x900',
                small: '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/small',
                '120x90': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/120x90',
                '92x69': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/92x69',
                '456x342': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/456x342',
            },
            transform: {
                angle: 0,
                blur: false,
            },
            isUploadRejected: false,
        },
        {
            isUploading: false,
            name: '1376323-2ab7c9e722f5ddba7bb5bc23d8044ce5',
            preview: 'blob:https://auto.ru/933a0297-4e6b-47fb-bc33-4c80b9173103',
            file: {
                preview: 'blob:https://auto.ru/933a0297-4e6b-47fb-bc33-4c80b9173103',
            },
            sizes: {
                thumb_m: '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/thumb_m',
                '60x45': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/60x45',
                '832x624': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/832x624',
                full: '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/full',
                '320x240': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/320x240',
                '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/1200x900',
                small: '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/small',
                '120x90': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/120x90',
                '92x69': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/92x69',
                '456x342': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/456x342',
            },
            transform: {
                angle: 0,
                blur: false,
            },
            isUploadRejected: false,
        },
    ],
    )).toEqual([
        {
            name: '1043613-2ba53afd2f3bc53bdb8ff446bd7f3afb',
            sizes: {
                thumb_m: '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/thumb_m',
                '60x45': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/60x45',
                '832x624': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/832x624',
                full: '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/full',
                '320x240': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/320x240',
                '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/1200x900',
                small: '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/small',
                '120x90': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/120x90',
                '92x69': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/92x69',
                '456x342': '//avatars.mds.yandex.net/get-autoru-all/1043613/2ba53afd2f3bc53bdb8ff446bd7f3afb/456x342',
            },
            transform: {
                angle: 0,
                blur: false,
            },
        },
        {
            name: '1376323-2ab7c9e722f5ddba7bb5bc23d8044ce5',
            sizes: {
                thumb_m: '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/thumb_m',
                '60x45': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/60x45',
                '832x624': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/832x624',
                full: '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/full',
                '320x240': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/320x240',
                '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/1200x900',
                small: '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/small',
                '120x90': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/120x90',
                '92x69': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/92x69',
                '456x342': '//avatars.mds.yandex.net/get-autoru-all/1376323/2ab7c9e722f5ddba7bb5bc23d8044ce5/456x342',
            },
            transform: {
                angle: 0,
                blur: false,
            },
        },
    ]);
});
