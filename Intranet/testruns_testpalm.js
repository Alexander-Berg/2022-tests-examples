const { fitcher } = require('@yandex-int/fitcher');

fitcher({
    repoOwner: 'idm',
    repoName: 'backend',
    testPalmProjectId: 'idm_backend_palmsync',
    testPalmProjects: [
        {
            id: 'idm-www',
            tagsAttributeId: '5c00f00e798633871cbb0b98'
        },
        {
            id: 'idm_backend_palmsync',
            tagsAttributeId: '60111c9838e7c60140f6613b'
        }
    ]
}).catch(e => {
    console.log('Создание Test Runs в Testpalm завершилось с ошибкой', e);
    process.exit(1);
});
