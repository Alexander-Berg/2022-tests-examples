const getReasons = require('auto-core/react/lib/auth/getUserBanReasons');
const notImportantDomains = require('auto-core/data/dicts/notImportantForBanDomains.json');

const exampleReasons = {
    reasons: [
        'DO_NOT_EXIST',
    ],
};

const bannedParams = {
    bans: {
        user: exampleReasons,
    },
};

const bannedParamsNotImportant = {
    bans: {},
};

const bannedParamsMixed = {
    bans: {
        user: exampleReasons,
        all: exampleReasons,
    },
};

bannedParamsNotImportant.bans[notImportantDomains[0]] = exampleReasons;
bannedParamsMixed.bans[notImportantDomains[0]] = exampleReasons;

const TEST_CASES = [
    {
        name: 'should return false for not correct params - undefined',
        result: [],
    },
    {
        name: 'should return false for empty params',
        moderationStatus: {},
        result: [],
    },
    {
        name: 'should return false for empty bans in params',
        moderationStatus: { bans: {} },
        result: [],
    },
    {
        name: 'should return false for bans in not Important Domains',
        moderationStatus: bannedParamsNotImportant,
        result: [],
    },
    {
        name: 'should return list of strings for bans',
        moderationStatus: bannedParams,
        result: exampleReasons.reasons,
    },
    {
        name: 'should return list of strings for bans with mixed domains',
        moderationStatus: bannedParamsMixed,
        result: exampleReasons.reasons,
    },
];

TEST_CASES.forEach((testCase) => {
    it(testCase.name, () => {
        expect(getReasons(testCase.moderationStatus)).toEqual(testCase.result);
    });
});
