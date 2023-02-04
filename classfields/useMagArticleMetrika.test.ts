import { renderHook, act } from '@testing-library/react-hooks';

import contextMock from 'autoru-frontend/mocks/contextMock';

import useMagArticleMetrika from './useMagArticleMetrika';

beforeEach(() => {
    contextMock.metrika.sendParams.mockClear();
});

const defaultProps = {
    articlesUrl: [ 'test1' ],
    block: 'block',
    page: 'page',
};

it('отправит метрику при загрузке страницы', () => {
    render();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams.mock.calls).toMatchSnapshot();
});

it('не отправит метрику если по какой-то статье уже была отправлена', () => {
    let articlesUrl = [ 'test1', 'test2' ];

    const { rerender } = render({
        ...defaultProps,
        articlesUrl,
    });

    act(() => {
        articlesUrl = [ 'test3' ];

        rerender();
    });

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
});

it('должен правильно формировать пропсы для линка', () => {
    const { result } = render();
    expect(result.current.getArticleLinkProps({
        urlPart: 'test3',
        key: 4,
    })).toMatchSnapshot();
});

it('должен правильно формировать ссылку на журнальный тэг', () => {
    const { result } = render();
    expect(result.current.getTagLink('electrocars')).toMatchSnapshot();
});

function render(props = defaultProps) {
    return renderHook(() => useMagArticleMetrika(props, contextMock));
}
