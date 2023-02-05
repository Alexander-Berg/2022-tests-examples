module.exports = {
    filePatterns: {
        yaml: /\.yml$/
    },
    retry: {
        maxRetryAfter: 1000
    },
    sets: {
        yaml: {
            'mobile-app': {
                specs: [
                    '../../features/mobile-app/scenarios/**/*.yml',
                    '../../../common/features/scenarios/**/*.yml'
                ],
                envs: [{
                    SET_NAME: 'mobile-app',
                }],
                browsers: ['none']
            },
        },
    },
    stepsGroupPaths: [
        '../../features/mobile-app/steps/**/*.yml',
        '../../../common/features/steps/**/*.yml'
        ],
    synchronizationOpts: {
        supportStepsGroup: true,
        formatter: 'withExpectationSyntax',
        skip: [
            'geminiReportTests',
            'hermioneReportTests',
            'hermioneE2eReportTests',
            'hermioneE2eTests',
            'hermioneTests',
            'urlsByShowCounters'
        ],
        attachBugFilterLinkToTestCasePreconditions: false
    },
    validationOpts: {
        mode: ['steps-group-syntax']
    },
    plugins: {
        validate: {
            'extensions-validator': {},
            'restrictions-validator': {},
        }
    }
};
