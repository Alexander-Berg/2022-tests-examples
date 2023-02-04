const getHookOptionsFromMetadata = require('./getHookOptionsFromMetadata');

const TESTS = [
    {
        userMetadata: '',
        result: null,
    },
    {
        userMetadata: 'DEPLOY_HOOK autotest',
        result: { name: 'autotest', params: [] },
    },
    {
        userMetadata: 'DEPLOY_HOOK autotest param1=value1 param.2=value2 param3 value3 some_other_info',
        result: { params: [ 'param1=value1', 'param.2=value2' ], name: 'autotest' },
    },
    {
        userMetadata: 'DEPLOY_HOOK perfomance-test param1=value1',
        result: { params: [ 'param1=value1' ], name: 'perfomance-test' },
    },
];

TESTS.forEach((testCase) => {
    it(`should parse ${ testCase.userMetadata } to ${ JSON.stringify(testCase.result) }`, () => {
        expect(
            getHookOptionsFromMetadata(testCase.userMetadata),
        ).toEqual(testCase.result);
    });
});
