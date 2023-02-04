import React from 'react';
import { MemoryRouter } from 'react-router';

import { Page, PageProps, MockData } from 'common/__tests__/common.page';

import { RootContainer } from '../containers/RootContainer';
import { rootSaga } from '../sagas';
import { rootReducer } from '../reducers';
import { receiveArchivePerson, receivedPerson, requestPerson } from '../actions';

interface PersonsPersonPageProps extends PageProps {
    fetchGetMocks: MockData[];
    requestGetMocks: MockData[];
}

export class PersonsPersonPage extends Page {
    constructor(personsPersonPage: PersonsPersonPageProps) {
        let { fetchGetMocks, requestGetMocks, ...props } = personsPersonPage;

        props.mocks = props.mocks ?? {};
        props.mocks.fetchGet = fetchGetMocks;
        props.mocks.requestGet = requestGetMocks;

        super({
            ...props,
            rootSaga,
            reducers: { ...rootReducer },
            RootContainer: () => (
                <MemoryRouter initialEntries={['/']}>
                    <RootContainer />
                </MemoryRouter>
            ),
            initialState: {
                isAdmin: false,
                perms: props.perms
            }
        });
    }

    async initializePage() {
        this.sagaTester.dispatch(requestPerson('19031126'));
        await this.sagaTester.waitFor(receivedPerson.type);
        this.wrapper.update();
    }

    async archivePerson() {
        this.wrapper.find('Button.yb-persons-person-title__button-archive').at(0).simulate('click');
        this.wrapper.update();

        this.wrapper
            .find('Button.yb-persons-person-archive-popup__button-yes')
            .at(0)
            .simulate('click');
        this.wrapper.update();

        await this.sagaTester.waitFor(receiveArchivePerson.type);
    }
}
