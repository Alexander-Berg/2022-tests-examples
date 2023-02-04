const getGroupBreadcrumbsData = require('./getGroupBreadcrumbsData');

it('должен вернуть правильный текст в крошке типа кузова когда нет notice', () => {
    const data = {
        mark: { name: 'MARK' },
        model: { name: 'MODEL' },
        configuration: { humanBodyType: 'SEDAN' },
    };
    expect(getGroupBreadcrumbsData(data)[2].caption).toEqual('SEDAN');
});

it('должен вернуть правильный текст в крошке типа кузова когда есть notice и не передается withNotice=true', () => {
    const data = {
        mark: { name: 'MARK' },
        model: { name: 'MODEL' },
        configuration: { humanBodyType: 'SEDAN', notice: 'NOTICE' },
    };
    expect(getGroupBreadcrumbsData(data)[2].caption).toEqual('SEDAN');
});

it('должен вернуть правильный текст в крошке типа кузова когда есть notice и передается withNotice=true', () => {
    const data = {
        mark: { name: 'MARK' },
        model: { name: 'MODEL' },
        configuration: { humanBodyType: 'SEDAN', notice: 'NOTICE' },
    };
    expect(getGroupBreadcrumbsData(data, true)[2].caption).toEqual('SEDAN NOTICE');
});
