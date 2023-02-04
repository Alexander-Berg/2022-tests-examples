const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { noop } = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const OfferNoteDumb = require('./OfferNoteDumb');

it('Должен правильно отрендерить форму заметки editable для легкового авто', () => {
    const wrapper = shallow(
        <OfferNoteDumb
            addNote={ noop }
            deleteNote={ noop }
            editable={ true }
            offer={{ category: 'cars' }}
            visible={ true }
        />,
        { context: { ...contextMock } },
    );
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('Должен правильно отрендерить плейсхолдер заметки editable для мото', () => {
    const wrapper = shallow(
        <OfferNoteDumb
            addNote={ noop }
            deleteNote={ noop }
            editable={ true }
            offer={{ category: 'moto', vehicle_info: { moto_category: 'atv' } }}
            visible={ true }
        />,
        { context: { ...contextMock } },
    );
    const placeholder = wrapper.find('TextInput').props().placeholder;
    expect(placeholder).toEqual('Заметка об этом мотовездеходе (её увидите только вы)');
});

it('Должен правильно отрендерить заметку editable', () => {
    const wrapper = shallow(
        <OfferNoteDumb
            addNote={ noop }
            deleteNote={ noop }
            editable={ true }
            note="Привет!"
            offer={{ category: 'cars' }}
            visible={ true }
        />,
        { context: { ...contextMock } },
    );
    const placeholder = wrapper.find('TextInput').props().value;
    expect(placeholder).toEqual('Привет!');
});

it('Должен правильно отрендерить форму заметки для легкового авто', () => {
    const wrapper = shallow(
        <OfferNoteDumb
            addNote={ noop }
            deleteNote={ noop }
            offer={{ category: 'cars' }}
            visible={ true }
        />,
        { context: { ...contextMock } },
    );
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

describe('действия', () => {
    it('должен вызвать сохранение заметки при клике на кнопку', () => {
        let addNotePromise;
        const addNote = jest.fn(() => {
            addNotePromise = Promise.resolve();
            return addNotePromise;
        });

        const wrapper = shallow(
            <OfferNoteDumb
                addNote={ addNote }
                deleteNote={ noop }
                editable={ true }
                isAuth={ true }
                note="Привет!"
                offer={{ category: 'cars' }}
                visible={ true }
            />,
            { context: { ...contextMock } },
        );

        wrapper.find('Button').simulate('click');

        return addNotePromise.then(() => {
            expect(addNote).toHaveBeenCalled();
        });
    });

    it('должен нарисовать ошибку, если сохранение завершилось неудачно', () => {
        let addNotePromise;
        const addNote = jest.fn(() => {
            addNotePromise = Promise.reject();
            return addNotePromise;
        });

        const wrapper = shallow(
            <OfferNoteDumb
                addNote={ addNote }
                deleteNote={ noop }
                editable={ true }
                isAuth={ true }
                note="Привет!"
                offer={{ category: 'cars' }}
                visible={ true }
            />,
            { context: { ...contextMock } },
        );

        wrapper.find('Button').simulate('click');

        return addNotePromise
            .then(null, () => Promise.resolve())
            .then(() => {
                expect(wrapper.find('TextInput')).toHaveProp('error', 'Что-то пошло не так, попробуйте еще раз');
            });
    });
});
