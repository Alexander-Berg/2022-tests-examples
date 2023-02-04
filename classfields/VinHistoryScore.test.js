const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const VinHistoryScore = require('./VinHistoryScoreDumb');

const eventObj = {
    currentTarget: {
        getAttribute: () => 'NEGATIVE',
    },
};

let context;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
});

describe('Правильно рисует компонент VinHistoryScore:', () => {
    it('компактный вид', () => {
        const component = shallow(<VinHistoryScore/>, { context });
        expect(component.instance().render()).toMatchSnapshot();
    });

    it('расширенный вид', () => {
        const component = shallow(<VinHistoryScore/>, { context });
        component.instance().onSetScore(eventObj);
        expect(component.instance().render()).toMatchSnapshot();
    });

    it('расширенный вид c текстами про возврат денег', () => {
        const component = shallow(
            <VinHistoryScore
                disclaimer="Test disclaimer text."
                moneyBack={ true }
                subtitle="Test subtitle text."
                title="Test title"
            />, { context },
        );
        component.instance().onSetScore(eventObj);
        expect(component.instance().render()).toMatchSnapshot();
    });

    it('финальный вид (отзыв отправлен)', () => {
        const component = shallow(<VinHistoryScore/>, { context });
        expect(component.instance().renderSuccess()).toMatchSnapshot();
    });

    it('финальный вид (отзыв отправлен) c текстом про возврат денег', () => {
        const component = shallow(
            <VinHistoryScore
                disclaimer="Test disclaimer text."
                moneyBack={ true }
            />, { context },
        );
        component.instance().onToggleCheckbox();
        expect(component.instance().renderSuccess()).toMatchSnapshot();
    });
});

describe('отправляет метрики VinHistoryScore', () => {
    it('клика на оценку', () => {
        const component = shallow(<VinHistoryScore/>, { context });
        component.find('.VinHistoryScore__scoreButton').first().simulate('click', eventObj);

        const expectedResult = [ 'report_score', 'click', 'negative' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('сабмита оценки', () => {
        const component = shallow(<VinHistoryScore sendHistoryScore={ () => Promise.resolve() }/>, { context });
        component.find('.VinHistoryScore__scoreButton').first().simulate('click', eventObj);
        component.find('Button').simulate('click');

        const expectedResult = [ 'report_score', 'submit', 'negative' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual(expectedResult);
    });

    it('ошибки сабмита оценки', () => {
        const component = shallow(<VinHistoryScore sendHistoryScore={ () => Promise.reject() }/>, { context });
        component.find('.VinHistoryScore__scoreButton').first().simulate('click', eventObj);
        component.find('Button').simulate('click');

        return Promise.resolve(() => {
            const expectedResult = [ 'report_score', 'submit_error' ];
            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(3);
            expect(context.metrika.sendPageEvent.mock.calls[2][0]).toEqual(expectedResult);
        });
    });
});
