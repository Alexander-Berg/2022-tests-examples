const getPofferDraftFromRequest = require('./getPofferDraftFromEvaluationRequest');

const TESTS = [
    {
        request: require('./evaluationRequest_to_PofferDraft_request1.json'),
        result: require('./evaluationRequest_to_PofferDraft_response1.json'),
    },
    {
        // нет complectation_id
        request: require('./evaluationRequest_to_PofferDraft_request2.json'),
        result: require('./evaluationRequest_to_PofferDraft_response2.json'),
    },
    {
        // зеленый цвет
        request: require('./evaluationRequest_to_PofferDraft_request3.json'),
        result: require('./evaluationRequest_to_PofferDraft_response3.json'),
    },
];

TESTS.forEach((testCase, i) => {
    it(`should return valid draft, case ${ i }`, () => {
        expect(getPofferDraftFromRequest(testCase.request)).toEqual(testCase.result);
    });
});
