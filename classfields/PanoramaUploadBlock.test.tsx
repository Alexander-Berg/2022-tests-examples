import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import { STATUSES, REMOVE_REASONS } from '../consts';

import type { Props } from './PanoramaUploadBlock';
import PanoramaUploadBlock from './PanoramaUploadBlock';

let props: Props;

beforeEach(() => {
    props = {
        status: STATUSES.PROCESSED,
        onPanoramaRemove: jest.fn(),
        setConfirmModalState: jest.fn(),
        type: 'exterior',
        isEdit: false,
        data: panoramaExteriorMock.value(),
        isConfirmModalVisible: false,
        loadingProgress: 0,
        onDrop: jest.fn(),
        onRemoveLinkClick: jest.fn(),
        onRepeatLinkClick: jest.fn(),
        onUploadAreaClick: jest.fn(),
    };
});

it('при удалении панорамы передает в коллбэк выбранную причину', () => {
    props.isConfirmModalVisible = true;
    const page = shallowRenderComponent({ props });

    const radioGroup = page.find('.PanoramaUploadBlock__radioGroup');
    radioGroup.simulate('change', REMOVE_REASONS[2].value);

    const confirmButton = page.find('Button[children="Удалить панораму"]');
    confirmButton.simulate('click', { currentTarget: { classList: { contains: () => false } } });

    expect(props.setConfirmModalState).toHaveBeenCalledTimes(1);
    expect(props.onPanoramaRemove).toHaveBeenCalledTimes(1);
    expect(props.onPanoramaRemove).toHaveBeenCalledWith(REMOVE_REASONS[2].value);
});

it('если пользователь передумал удалять панораму, просто закроет модал', () => {
    props.isConfirmModalVisible = true;
    const page = shallowRenderComponent({ props });

    const confirmButton = page.find('Button[children="Оставить"]');
    confirmButton.simulate('click', { currentTarget: { classList: { contains: () => true } } });

    expect(props.setConfirmModalState).toHaveBeenCalledTimes(1);
    expect(props.onPanoramaRemove).toHaveBeenCalledTimes(0);
});

describe('во время обработки панорамы', () => {
    it('покажет правильный текст для внешней панорамы', () => {
        props.status = STATUSES.IN_PROCESSING;
        const page = shallowRenderComponent({ props });
        const noticeText = page.find('.PanoramaUploadBlock__noticeText');

        expect(shallowToJson(noticeText)).toMatchSnapshot();
    });

    it('покажет правильный текст для внутренней панорамы', () => {
        props.status = STATUSES.IN_PROCESSING;
        props.isEdit = true;
        props.type = 'interior';
        const page = shallowRenderComponent({ props });
        const noticeText = page.find('.PanoramaUploadBlock__noticeText');

        expect(shallowToJson(noticeText)).toMatchSnapshot();
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    class ComponentMock extends PanoramaUploadBlock {
        thumbCaption = 'some caption'

        renderDescription(): JSX.Element | Element {
            return <span>my description</span>;
        }
    }

    return shallow(
        <ContextProvider>
            <ComponentMock { ...props }/>
        </ContextProvider>
        ,
    ).dive();
}
