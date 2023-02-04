const getOptionsFromParams = require('./getOptionsFromEvaluationRequest');

const TESTS = [
    {
        request: require('./evaluationRequest_to_options1.json'),
        result: [
            'multi-wheel',
            'xenon',
            'airbag-passenger',
            'lock',
            'door-sill-panel',
            'electro-mirrors',
            'mirrors-heat',
            'start-stop-function',
            'automatic-lighting-control',
            'computer',
            'seat-transformation',
            'light-cleaner',
            'fabric-seats',
            'airbag-side',
            'abs',
            'wheel-leather',
            'roller-blind-for-rear-window',
            'esp',
            'usb',
            'audiopreparation',
            'front-centre-armrest',
            'servo',
            'steering-wheel-gear-shift-paddles',
            'electro-window-back',
            '17-inch-wheels',
            'climate-control',
            'park-assist-r',
            'airbag-driver',
            'isofix',
            'aux',
            'electro-window-front',
            'light-sensor',
            'hcc',
            'airbag-curtain',
            'leather-gear-stick',
            'rain-sensor',
            'tyre-pressure',
            'voice-recognition',
            'audiosystem-cd',
            'front-seats-heat',
            'bluetooth',
            'wheel-configuration1',
            'immo',
            '12v-socket',
            'third-rear-headrest',
        ],
    },
];

TESTS.forEach((testCase, i) => {
    it(`should return options from request, case ${ i }`, () => {
        expect(getOptionsFromParams(testCase.request)).toEqual(testCase.result);
    });
});
