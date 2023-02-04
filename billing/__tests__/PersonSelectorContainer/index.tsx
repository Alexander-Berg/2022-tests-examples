import { fromJS } from 'immutable';
import { watchPersonSelector } from 'common/sagas/person-selector';
import Enzyme, { mount } from 'enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { personSelector } from 'common/reducers/person-selector';
import SagaTester from 'redux-saga-tester';
import { fetchGet } from 'common/utils/old-fetch';
import Adapter from 'enzyme-adapter-react-16';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { PersonSelectorAction } from 'common/actions';
import { HOST } from 'common/utils/test-utils/common';
import { PersonSelectorContainer } from '../../PersonSelectorContainer';
import { combineReducers, Reducer } from 'redux';
import { personTypes, personList } from './data';
import { FilterStateRecord as getFilterInitialState } from 'common/reducers/person-selector/filter';
import { ListStateRecord as getListInitialState } from 'common/reducers/person-selector/list';
import { PersonSelectorListAction } from 'common/actions';
import { all } from 'redux-saga/effects';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

const rootSaga = function* () {
    yield all([watchPersonSelector()]);
};

describe('admin - persons - containers - filter', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('fill all filter fields and press submit button - should make proper request', async () => {
        const initialState = {
            personSelector: {
                filter: getFilterInitialState()
                    .set('personTypes', fromJS(personTypes))
                    .set('isFetching', false),
                list: getListInitialState(),
                isVisible: false
            }
        };

        const rootReducer: Reducer = combineReducers({
            personSelector
        });

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
        });

        sagaTester.start(rootSaga);

        // @ts-ignore
        const store = sagaTester.store;

        // @ts-ignore
        fetchGet.mockResolvedValueOnce(personList);

        const Container = withIntlProvider(() => (
            <Provider store={store}>
                {
                    // @ts-ignore
                    <PersonSelectorContainer SHOULD_SKIP_FETCH_DATA={true} />
                }
            </Provider>
        ));

        const wrapper = mount(<Container />);

        sagaTester.dispatch({ type: PersonSelectorAction.CLICK });
        wrapper.setProps({});

        function getItem(path: string[]) {
            return path.reduce((object, prop) => object.find(prop), wrapper).first();
        }

        let [name, personType, personId, inn, email, kpp, form] = [
            ['FormField[title="Наименование"]', 'Textinput'],
            ['FormField[title="Тип"]', 'Select'],
            ['FormField[title="ID"]', 'Textinput'],
            ['FormField[title="ИНН"]', 'Textinput'],
            ['FormField[title="E-mail"]', 'Textinput'],
            ['FormField[title="ЕГРПОУ(для ЮЛ Украины)"]', 'Textinput'],
            ['form']
        ].map(getItem);

        name.prop('onChange')('Короткое имя организации');
        personType.prop('onChange')('am_jp');
        personId.prop('onChange')('71849778');
        inn.prop('onChange')('7830980084');
        email.prop('onChange')('rrefe44');
        kpp.prop('onChange')('5007010013');

        let prevented = false;
        // @ts-ignore
        form.prop('onSubmit')({
            preventDefault: () => {
                prevented = true;
            }
        });
        expect(prevented).toBe(true);

        await sagaTester.waitFor(PersonSelectorListAction.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/person/list`,
            {
                email: 'rrefe44',
                inn: '7830980084',
                kpp: '5007010013',
                name: 'Короткое имя организации',
                pagination_pn: 1,
                pagination_ps: 10,
                person_id: 71849778,
                person_type: 'am_jp',
                vip_only: false
            },
            false,
            false
        );
    });
});
