const getHookOptionsFromMetadata = require('./getHookOptionsFromMetadata');

const TESTS = [
    {
        userMetadata: '',
        result: null,
    },
    {
        userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release',
        result: {
            repo: 'autoru-frontend', head: 'release_12.09.2018_forms', base: 'master',
            labels: [ 'release' ],
            addBranchLabel: false,
            runAutotests: false,
        },
    },
    {
        // eslint-disable-next-line max-len
        userMetadata: '* Build #213 (release_12.09.2018_forms:89d72992fc55b720b77c260061c313659852de53) by natix\\n* AUTORUFRONT-12107\\n* DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release',
        result: {
            repo: 'autoru-frontend', head: 'release_12.09.2018_forms', base: 'master',
            labels: [ 'release' ],
            addBranchLabel: false,
            runAutotests: false,
        },
    },
    {
        userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_branch_label',
        result: {
            repo: 'autoru-frontend', head: 'release_12.09.2018_forms', base: 'master',
            labels: [ ],
            addBranchLabel: true,
            runAutotests: false,
        },
    },
    {
        userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release,release1 add_branch_label',
        result: {
            repo: 'autoru-frontend',
            head: 'release_12.09.2018_forms', base: 'master',
            labels: [ 'release', 'release1' ],
            addBranchLabel: true,
            runAutotests: false,
        },
    },
    {
        userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release,release1 add_branch_label run_autotests',
        result: {
            repo: 'autoru-frontend', head: 'release_12.09.2018_forms', base: 'master',
            labels: [ 'release', 'release1' ],
            addBranchLabel: true,
            runAutotests: true,
        },
    },
    {
        userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release,release1 run_autotests',
        result: {
            repo: 'autoru-frontend', head: 'release_12.09.2018_forms', base: 'master',
            labels: [ 'release', 'release1' ],
            addBranchLabel: false,
            runAutotests: true,
        },
    },
    {
        userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master run_autotests',
        result: {
            repo: 'autoru-frontend', head: 'release_12.09.2018_forms', base: 'master',
            labels: [ ],
            addBranchLabel: false,
            runAutotests: true,
        },
    },
];

TESTS.forEach((testCase) => {
    it(`should parse ${ testCase.userMetadata } to ${ JSON.stringify(testCase.result) }`, () => {
        expect(
            getHookOptionsFromMetadata(testCase.userMetadata),
        ).toEqual(testCase.result);
    });
});
