const appWithAutotests = require('./app-with-autotests');

it('должен построить правильные урлы для автотеста', () => {
    const deployment = {
        branch: 'autotest',
    };

    const urls = [];
    appWithAutotests.forEach((builder) => {
        urls.push(builder(deployment));
    });

    expect(urls).toMatchSnapshot();
});

it('должен построить правильные урлы для бранча', () => {
    const deployment = {
        branch: 'AUTORUFRONT-10101',
    };

    const urls = [];
    appWithAutotests.forEach((builder) => {
        urls.push(builder(deployment));
    });

    expect(urls).toMatchSnapshot();
});
