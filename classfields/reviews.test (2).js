jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getResource = require('auto-core/react/lib/gateApi').getResource;

const reviewActions = require('./reviews');

describe('postComment:', () => {
    it('правильно вызывает getResource', () => {
        const responsePromise = Promise.resolve({ status: 'SUCCESS' });
        getResource.mockReturnValueOnce(responsePromise);

        reviewActions.postComments({ foo: 'bar' });

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource.mock.calls[0]).toMatchSnapshot();
    });

    describe('при ошибке', () => {
        it('если есть текст ошибки, передаст его дальше по цепочке', async() => {
            const errorText = 'Слишком короткий комментарий';
            const responsePromise = Promise.reject({ detailed_error: errorText });
            getResource.mockReturnValueOnce(responsePromise);

            const promiseChain = reviewActions.postComments({ foo: 'bar' });

            await expect(promiseChain).rejects.toMatchObject({ error: errorText });
        });

        it('если текста ошибки нет, передаст дефолтный', async() => {
            const responsePromise = Promise.reject({ });
            getResource.mockReturnValueOnce(responsePromise);

            const promiseChain = reviewActions.postComments({ foo: 'bar' });

            await expect(promiseChain).rejects.toMatchObject({ error: 'Произошла ошибка, попробуйте позже' });
        });
    });
});
