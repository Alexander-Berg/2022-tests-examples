import getUtmParams from './getUtmParams';

const testCases = [
    { title: 'правильный utm_term', utmTerm: '60bf2c13b25b201687b30b4bz2305707289x3797966437x3797080587574711x982277662' },
    { title: 'неправильный utm_term (формат)', utmTerm: '123' },
    { title: 'неправильный utm_term (linkId не хеш)', utmTerm: '60pf2c13b25b201687b30b4bz2305707289x3797966437x3797080587574711x982277662' },
    { title: 'неправильный utm_term (отсутствует rid)', utmTerm: '60pf2c13b25b201687b30b4bz' },
];

for (const testCase of testCases) {
    it(`рендерит метки - ${ testCase.title }`, () => {
        const tree = getUtmParams({ utm_term: testCase.utmTerm });

        expect(tree).toMatchSnapshot();
    });
}
