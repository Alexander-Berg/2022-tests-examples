const { getAgencyClientPath } = require('realty-core/app/lib/get-agency-client-path');

describe('getAgencyClientPath', () => {
    it('при отсутствии параметра client должна вернуть переданное в нее значение', () => {
        const paths = [ 'path', 'without', 'clint-x' ];
        const result = getAgencyClientPath([ 'path', 'without', 'clint-x' ], '1337');

        expect(result).toEqual(paths);
    });

    it('при наличии client корректно форматирует paths в конце урла', () => {
        const paths = [ 'service', '123', 'client', '228' ];

        expect(getAgencyClientPath(paths, '1337'))
            .toEqual([ 'service', '123', 'agency', '228', 'client', '1337' ]);
    });

    it('при наличии client корректно форматирует paths в середине урла', () => {
        const paths = [ 'service', '123', 'client', '228', 'param', '321' ];

        expect(getAgencyClientPath(paths, '1337'))
            .toEqual([ 'service', '123', 'agency', '228', 'client', '1337', 'param', '321' ]);
    });

    it('при наличии client корректно форматирует paths в начале урла', () => {
        const paths = [ 'client', '228', 'param', '321' ];

        expect(getAgencyClientPath(paths, '1337'))
            .toEqual([ 'agency', '228', 'client', '1337', 'param', '321' ]);
    });
});
