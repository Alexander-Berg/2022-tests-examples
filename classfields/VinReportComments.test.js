const React = require('react');
const { shallow } = require('enzyme');
const { Provider } = require('react-redux');

const deleteReportComment = require('auto-core/react/dataDomain/vinReport/actions/deleteReportComment');
const putReportComment = require('auto-core/react/dataDomain/vinReport/actions/putReportComment');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const VinReportComments = require('./VinReportComments');

jest.mock('auto-core/react/dataDomain/vinReport/actions/deleteReportComment');
jest.mock('auto-core/react/dataDomain/vinReport/actions/putReportComment');

const editableComment = {
    id: '2',
    block_id: 'pts',
    text: 'коммент2',
    user: {
        id: 'user:5396640',
    },
    create_time: '1575551744565',
    update_time: '1575551744565',
    is_deleted: false,
    actions: { edit: true, 'delete': true },
};
const storeParams = {
    config: {
        data: {
            pageType: 'proauto-report',
        },
    },
    uploadUrl: {
        upload_url: 'url',
    },
    vinReport: {
        data: {
            report: {
                pts_info: {
                    vin: '111',
                },
                comments: [
                    {
                        id: '1',
                        block_id: 'pts',
                        text: 'коммент1',
                        user: {
                            id: 'user:5396640',
                        },
                        create_time: '1575551744565',
                        update_time: '1575551744565',
                        is_deleted: false,
                    },
                    editableComment,
                ],
            },
        },
    },
};

const originalHasExperiment = contextMock.hasExperiment;

beforeEach(() => {
    contextMock.hasExperiment = originalHasExperiment;
});

afterEach(() => {
    deleteReportComment.mockClear();
    putReportComment.mockClear();
});

it('должен показать форму добавления коммента после удаления коммента, если можно', () => {
    const pr = Promise.resolve({ add_new_comment: true });
    deleteReportComment.mockImplementation(jest.fn(() => () => pr));
    const component = shallow(
        <Provider store={ mockStore(storeParams) }>
            <VinReportComments
                commentable={{ block_id: 'pts' }}
            />
        </Provider>,
    ).dive().dive();
    component.find('.VinReportComments__commentButton:last-child Link').simulate('click');
    expect(deleteReportComment).toHaveBeenCalledTimes(1);
    return pr.then(() => {
        expect(component.state().showAddForm).toBe(true);
    });
});

it('не должен показать форму добавления коммента после удаления коммента, если нельзя', () => {
    const pr = Promise.resolve();
    deleteReportComment.mockImplementation(jest.fn(() => () => pr));
    const component = shallow(
        <Provider store={ mockStore(storeParams) }>
            <VinReportComments
                commentable={{ block_id: 'pts' }}
            />
        </Provider>,
    ).dive().dive();
    component.find('.VinReportComments__commentButton:last-child Link').simulate('click');
    expect(deleteReportComment).toHaveBeenCalledTimes(1);
    return pr.then(() => {
        expect(component.state().showAddForm).toBeUndefined();
    });
});

it('должен поменять стейт для редактирования коммента', () => {
    const component = shallow(
        <Provider store={ mockStore(storeParams) }>
            <VinReportComments
                commentable={{ block_id: 'pts' }}
            />
        </Provider>,
    ).dive().dive();
    component.find('.VinReportComments__commentButton:first-child Link').simulate('click');
    expect(component.state().editComment).toEqual(editableComment);
});

it('должен поменять стейт после редактирования коммента', () => {
    const pr = Promise.resolve();
    putReportComment.mockImplementation(jest.fn(() => () => pr));
    const component = shallow(
        <Provider store={ mockStore(storeParams) }>
            <VinReportComments
                commentable={{ block_id: 'pts' }}
            />
        </Provider>,
    ).dive().dive();
    component.find('.VinReportComments__commentButton:first-child Link').simulate('click');
    component.find('Connect(VinReportCommentForm)').dive().dive().find('Button').simulate('click');
    expect(putReportComment).toHaveBeenCalledWith({ block_id: 'pts', photos: [], text: 'коммент2', vin: '111' });
    return pr.then(() => {
        expect(component.state().editComment).toEqual({});
    });

});

it('должен поменять стейт при отправке коммента', () => {
    const store = { ...storeParams };
    store.vinReport.data.report.comments = [];

    const pr = Promise.resolve();
    putReportComment.mockImplementation(jest.fn(() => () => pr));
    const component = shallow(
        <Provider store={ mockStore(store) }>
            <VinReportComments
                commentable={{ block_id: 'pts', add_comment: true }}
            />
        </Provider>,
    ).dive().dive();
    const form = component.find('Connect(VinReportCommentForm)').dive().dive();
    form.setState({ input: 'привет' });
    form.find('Button').simulate('click');
    expect(putReportComment).toHaveBeenCalledWith({ block_id: 'pts', photos: [], text: 'привет', vin: '111' });
    return pr.then(() => {
        expect(component.state().showAddForm).toBe(false);
    });
});

it('не должен закрывать форму при неуспешной отправке', () => {
    const store = { ...storeParams };
    store.vinReport.data.report.comments = [];

    const pr = Promise.reject();
    putReportComment.mockImplementation(jest.fn(() => () => pr));
    const component = shallow(
        <Provider store={ mockStore(store) }>
            <VinReportComments
                commentable={{ block_id: 'pts', add_comment: true }}
            />
        </Provider>,
    ).dive().dive();
    const form = component.find('Connect(VinReportCommentForm)').dive().dive();
    form.setState({ input: 'привет' });
    form.find('Button').simulate('click');
    expect(putReportComment).toHaveBeenCalledWith({ block_id: 'pts', photos: [], text: 'привет', vin: '111' });
    return pr.then(() => {}, () => {
        expect(component.state().showAddForm).toBe(true);
    });
});

it('должен отобрать комменты только для нужного блока', () => {
    const store = { ...storeParams };
    store.vinReport.data.report.comments = [
        {
            id: 'id1',
            block_id: 'pts',
            text: 'коммент1',
        },
        {
            id: 'id2',
            block_id: 'not-pts',
            text: 'коммент2',
        },
    ];

    const component = shallow(
        <Provider store={ mockStore(store) }>
            <VinReportComments
                commentable={{ block_id: 'pts' }}
            />
        </Provider>,
    ).dive().dive();
    const comments = component.find('.VinReportComments__comment');
    expect(comments).toHaveLength(1);
    expect(comments.at(0).find('.VinReportComments__commentText').text()).toBe('коммент1');
});

it('должен удалять сообщение об ошибке после успешной отправки коммента', () => {
    const store = { ...storeParams };
    store.vinReport.data.report.comments = [];

    const reject = Promise.reject([ { message: 'Ты лох!' } ]);
    const resolve = Promise.resolve({});
    putReportComment
        .mockImplementationOnce(jest.fn(() => () => reject))
        .mockImplementationOnce(jest.fn(() => () => resolve));
    const component = shallow(
        <Provider store={ mockStore(store) }>
            <VinReportComments
                commentable={{ block_id: 'pts', add_comment: true }}
            />
        </Provider>,
    ).dive().dive();
    const form = component.find('Connect(VinReportCommentForm)').dive().dive();
    form.setState({ input: 'привет' });
    form.find('Button').simulate('click');
    return reject.then(() => {}, () => {
        expect(component.state().errorMessage).toBe('Ты лох!');
        form.find('Button').simulate('click');
        return resolve.then(() => {
            expect(component.state().errorMessage).toBe('');
        });
    });
});

it('не должен рендерить комменты, если не передан блок отчёта', () => {
    const component = shallow(
        <Provider store={ mockStore(storeParams) }>
            <VinReportComments/>
        </Provider>,
    ).dive().dive();
    expect(component).toBeEmptyRender();
});

it('не должен показывать форму добавления комментариев на странице пдф', () => {
    const store = { ...storeParams };
    store.config.data.pageType = 'pdf-version-history-by-vin';
    const component = shallow(
        <Provider store={ mockStore(store) }>
            <VinReportComments
                commentable={{ block_id: 'pts', add_comment: true }}
                preset="pdf"
            />
        </Provider>,
    ).dive().dive();
    expect(component.find('.VinReportComments__form')).not.toExist();
});
