jest.mock('auto-core/react/lib/uploadImage', () => {
    return jest.fn().mockImplementation(() => Promise.resolve());
});

jest.mock('auto-core/lib/util/getDataTransferFiles');

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import _ from 'lodash';

import mockStore from 'autoru-frontend/mocks/mockStore';

import getDataTransferFiles from 'auto-core/lib/util/getDataTransferFiles';

import uploadImage from 'auto-core/react/lib/uploadImage';

import StsPhotoUpload from './StsPhotoUpload';

const getDataTransferFilesMock = getDataTransferFiles as jest.MockedFunction<typeof getDataTransferFiles>;
getDataTransferFilesMock.mockReturnValue([]);

const validFileMock = {
    name: 'spyaschii_kotik.jpeg',
    type: 'image/jpeg',
} as File;

const store = mockStore({
    card: { state: { sts_upload_url: '' } },
});

it('отправляет запрос, если поменялся инпут', () => {

    getDataTransferFilesMock.mockReturnValue([ validFileMock ]);
    const wrapper = shallow(
        <Provider store={ store }>
            <StsPhotoUpload
                updateForm={ _.noop }
            />
        </Provider>,
    ).dive().dive();
    const input = wrapper.find('StsPhotoUploadDumb').dive().find('input');

    input.simulate('change');

    expect(uploadImage).toHaveBeenCalledTimes(1);

    expect(input).toExist();
});
