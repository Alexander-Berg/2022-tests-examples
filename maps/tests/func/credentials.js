module.exports = {
    ONE_MAP: {
        // Пользователь с одной картой
        login: 'autotests1',
        pass: 'testtest1'
    },
    // Пользователь без карт
    NO_MAP: {
        login: 'autotests2',
        pass: 'testtest2'
    },
    // Пользователь с неограниченным количеством карт
    MANY_MAPS: {
        login: 'autotests3',
        pass: 'testtest3'
    },
    // Пользователь с двумя 'Моими картами' и одной картой конструктора
    WITH_MY_MAPS: {
        login: 'autotests4',
        pass: 'testtest4'
    },
    // Пользователь без карт, для удаления единственной карты
    FOR_DELETE_SINGLE_MAP: {
        login: 'autotests5',
        pass: 'testtest5'
    },
    // MANY_MAPS
    linkMap: '/?um=constructor%3A05e1525d5605c82a6573e8b352271c878b8712f2b3d9c3b41e2185d33a3cb1c4',
    // MANY_MAPS
    linkMapPrint: '/?um=constructor%3A7dc577989a6ef1c032a5a1f37d53e19282ec57996df9991e13f24a57834a1eb3',
    umForCopy: '/?um=constructor%3A43e578f4fb228446934acf8c074109f604462c81f6dcccf4dd960cd2b27eafbf',
    umForShare: '/?um=constructor%3A7bdfbb4f414634ca22ee78f9926eb93a89568d0f7d6eb1ab2cf26d0a146376a3',
    sidForShare: 'constructor:7bdfbb4f414634ca22ee78f9926eb93a89568d0f7d6eb1ab2cf26d0a146376a3',
    umForPreview: '?um=constructor%3A4b04bacde5ce8f7d13e5285cc453b36fbadb3bedd22c70e9c43a96c99d5496f2',
    umTraffic: '?um=constructor%3Aa16c54f35718f63f46bb4925ca3898357a5dc3459a642bde94c77d6dc7f38d03',
    umHybrid: '?um=constructor%3Ae7ca6669e20d672d6ed7583edccc7579429bf5ef52b7b8dcafd5a447cc739554',
    umSatellite: '?um=constructor%3Ad1bebae5136afbd7b0385cafdc0dbced5d267de9376965cdda715ab498f09b55',
    umMinZoom: '?um=constructor%3A872c430d68f928c2b7808a7f8c17ab28a07f1aaac5d13549a48893e81f9fdc60',
    umForExportAll: '?um=constructor%3Ad5c6f9b9494c53296fb8fd15f69406151f9aaa7a5499f62fe0ccb34bf15ec8aa',
    umVertexLimit: '?um=constructor%3A1f7fc3c2e5fd25a2f3e6fe99ae39b10073a563f4d1a0414c9392d26dfc1317d6',
    umObjectsLimit: '?um=constructor%3Aa8735e517f11f73101e885b44a346de0752c03b5662ca064e111941872be1d9b',
    codeInteractiveScript: '\<script type="text/javascript" charset="utf-8" async src="https://api-maps.tst.c.maps.yandex.ru/services/constructor/1.0/js/?um=constructor%3A4b04bacde5ce8f7d13e5285cc453b36fbadb3bedd22c70e9c43a96c99d5496f2&amp;width=600&amp;height=550&amp;lang=ru_RU&amp;scroll=true"></script>',
    codeInteractiveIframe: '\<iframe src="https://l7test.yandex.ru/map-widget/v1/?um=constructor%3A4b04bacde5ce8f7d13e5285cc453b36fbadb3bedd22c70e9c43a96c99d5496f2&amp;source=constructor" width="600" height="550" frameborder="0"></iframe>',
    codeStatic: '\<a href="https://l7test.yandex.ru/maps/?um=constructor%3A4b04bacde5ce8f7d13e5285cc453b36fbadb3bedd22c70e9c43a96c99d5496f2&amp;source=constructorStatic" target="_blank"><img src="https://api-maps.tst.c.maps.yandex.ru/services/constructor/1.0/static/?um=constructor%3A4b04bacde5ce8f7d13e5285cc453b36fbadb3bedd22c70e9c43a96c99d5496f2&amp;width=600&amp;height=450&amp;lang=ru_RU" alt="" style="border: 0;" /></a>',
    // WITH_MY_MAPS
    umPrivateMap: '?um=mymaps%3Aeebdb5abc242a8871cb1947d490526b62829544c447c5b1d55a4e4b5e54fb1a5&source=' +
    'constructorLink',
    mapState: {
        new: 'newmap',
        list: 'openmap'
    }
};
