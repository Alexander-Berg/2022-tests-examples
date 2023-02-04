const {
    redirectForConditions,
    isNotAuthenticated,
    isNatural,
    isNotInExperiment
} = require('../data-provider-common');

const reqMock = {
    req: {
        urlHelper: {
            link: () => {}
        },
        experimentsData: {}
    },
    getParams: () => ({})
};

describe('data-provider-common', () => {
    describe('redirect', () => {
        it('redirects if user is not authenticated', () => {
            expect(() => {
                redirectForConditions([
                    isNotAuthenticated
                ], {}, 'test reason').call(reqMock, {});
            }).toThrowError('test reason');
        });

        it('does not redirect if user is authenticated', () => {
            const dataWithUser = {
                user: {
                    isAuth: true
                }
            };

            expect(() => {
                redirectForConditions([
                    isNotAuthenticated
                ], {}, 'test reason').call(reqMock, dataWithUser);
            }).not.toThrow();
        });

        it('does not redirect if user is juridical', () => {
            const dataWithUser = {
                vosUserData: {
                    paymentType: 'JURIDICAL_PERSON'
                }
            };

            expect(() => {
                redirectForConditions([
                    isNatural
                ], {}, 'test reason').call(reqMock, dataWithUser);
            }).not.toThrow();
        });

        it('redirects if at least one condition is false', () => {
            const dataWithUser = {
                vosUserData: {
                    paymentType: 'JURIDICAL_PERSON'
                }
            };

            expect(() => {
                redirectForConditions([
                    isNotAuthenticated,
                    isNatural
                ], {}, 'test reason').call(reqMock, dataWithUser);
            }).toThrowError('test reason');
        });

        it('redirects user is not in experiment', () => {
            expect(() => {
                redirectForConditions([
                    isNotInExperiment('test-experiment')
                ], {}, 'test reason').call(reqMock, {});
            }).toThrowError('test reason');
        });

        it('does not redirect if user is in experiment', () => {
            const mock = { ...reqMock };

            mock.req.experimentsData = {
                uaas: {
                    expNames: [ 'test-experiment' ]
                }
            };

            expect(() => {
                redirectForConditions([
                    isNotInExperiment('test-experiment')
                ], {}, 'test reason').call(mock, {});
            }).not.toThrow();
        });
    });
});
