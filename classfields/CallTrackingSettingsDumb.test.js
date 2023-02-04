const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CallTrackingSettingsDumb = require('./CallTrackingSettingsDumb');
const CallTrackingSettingsItem = require('www-cabinet/react/components/CallTrackingSettingsItem');
const Placeholder = require('www-cabinet/react/components/CallTrackingSettingsPlaceholder');
const TextInputInteger = require('auto-core/react/components/common/TextInputInteger');
const Toggle = require('auto-core/react/components/islands/Toggle/Toggle');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const onSettingsUpdate = jest.fn();

let ContextProvider;
let props;
beforeEach(() => {
    ContextProvider = createContextProvider(contextMock);
    props = {
        canChangeCallTrackingState: true,
        settings: {
            calltracking_enabled: true,
            offers_stat_enabled: true,
            target_call_duration: { seconds: 45 },
            unique_call_period: { days: 10 },
        },
        hasOnlyCallTariff: false,
        showOffersStatSetting: true,
        onSettingsUpdate,
    };
});

it('покажет компонент плейсхолдера, если трекинг не включен', () => {
    const props = { settings: { calltracking_enabled: false }, canChangeCallTrackingState: true };
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(tree.find(Placeholder)).toExist();
});

it('покажет настройки, если трекинг включен', () => {
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('покажет настройки, если есть звонковый тариф, но коллтрекинг не включен', () => {
    const props = {
        settings: { calltracking_enabled: false },
        canChangeCallTrackingState: true,
        hasCallTariff: true,
    };
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(tree.find(CallTrackingSettingsItem)).not.toHaveLength(0);
});

it('покажет описание настройки "трекинг звонков", если есть звонковый тариф', () => {
    props.hasCallTariff = true ;
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();
    const trackingSettingNote = tree.findWhere(node => node.prop('title') === 'Трекинг звонков').prop('note');

    expect(Boolean(trackingSettingNote)).toBe(true);
});

it('задизейблит настройку "трекинг звонков", если она включена и есть только звонковый тариф', () => {
    props.hasOnlyCallTariff = true;
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(tree.findWhere(node => node.prop('title') === 'Трекинг звонков').find(Toggle).prop('disabled')).toEqual(true);
});

it('не покажет настройку статистики, если не передан соответствующий проп', () => {
    props.showOffersStatSetting = false;
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(tree.findWhere(node => node.prop('title') === 'Статистика по объявлениям')).not.toExist();
});

it('задизейблит настройку статистики, если она включена и есть только звонковый тариф', () => {
    props.hasOnlyCallTariff = true;
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(tree.findWhere(node => node.prop('title') === 'Статистика по объявлениям').find(Toggle).prop('disabled')).toEqual(true);
});

it('не покажет настройки целевого и уникального звонков, если у клиента есть только звонковый тариф', () => {
    props.hasOnlyCallTariff = true;
    const tree = shallow(
        <ContextProvider>
            <CallTrackingSettingsDumb { ...props }/>
        </ContextProvider>,
    ).dive();

    expect(tree.findWhere(node => node.prop('title') === 'Уникальный звонок')).not.toExist();
    expect(tree.findWhere(node => node.prop('title') === 'Целевой звонок')).not.toExist();
});

describe('обновит настройки и отправит метрики', () => {
    it('при апдейте свитчера', () => {
        const tree = shallow(
            <ContextProvider>
                <CallTrackingSettingsDumb { ...props }/>
            </ContextProvider>,
        );
        const firstSwitcher = tree.dive().find(Toggle).at(1);

        firstSwitcher.simulate('check', false, { name: 'offersStatEnabled' });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'offersStatEnabled_turn_off' ]);
        expect(onSettingsUpdate).toHaveBeenCalledWith({
            calltracking_classifieds_enabled: false,
            calltracking_enabled: true,
            offers_stat_enabled: false,
            target_call_duration: { seconds: 45 },
            unique_call_period: { days: 10 },
        });
    });

    it('при апдейте инпута', () => {
        const tree = shallow(
            <ContextProvider>
                <CallTrackingSettingsDumb { ...props }/>
            </ContextProvider>,
        );
        const firstInput = tree.dive().find(TextInputInteger).at(1);
        const inputName = firstInput.prop('name');

        firstInput.simulate('blur', { target: { value: 777 } }, { name: inputName });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'targetCallDurationSeconds_change' ]);
        expect(onSettingsUpdate).toHaveBeenCalledWith({
            calltracking_classifieds_enabled: false,
            calltracking_enabled: true,
            offers_stat_enabled: true,
            target_call_duration: { seconds: 777 },
            unique_call_period: { days: 10 },
        });
    });
});
