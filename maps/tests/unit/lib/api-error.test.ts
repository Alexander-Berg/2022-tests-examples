import {expect} from 'chai';
import {ApiError} from 'src/lib/api-errors';

describe('ApiError', () => {
    it('should provide default message', () => {
        let error = new ApiError(500);
        expect(error.statusCode).to.equal(500);
        expect(error.message).to.equal('Internal Server Error');

        error = new ApiError(400);
        expect(error.statusCode).to.equal(400);
        expect(error.message).to.equal('Bad Request');
    });

    it('should allow custom message', () => {
        const error = new ApiError(400, 'Something bad');
        expect(error.statusCode).to.equal(400);
        expect(error.message).to.equal('Something bad');
    });

    it('should check invalid status code', () => {
        const errorMsg = 'Status code must be a number >= 400';
        expect(() => new ApiError('400' as any)).to.throw(Error, errorMsg);
        expect(() => new ApiError(300)).to.throw(Error, errorMsg);
        expect(() => new ApiError(-1)).to.throw(Error, errorMsg);
    });

    it('should provide message for unkown status code', () => {
        const error = new ApiError(600);
        expect(error.statusCode).to.equal(600);
        expect(error.message).to.equal('Unknown error');
    });

    describe('toJSON()', () => {
        it('should render statusCopde, message and type', () => {
            const error = new ApiError(400, 'Validation failed');
            expect(JSON.parse(JSON.stringify(error))).to.deep.equal({
                statusCode: 400,
                message: 'Validation failed',
                type: 'ApiError'
            });
        });

        it('should render data', () => {
            const error = new ApiError(400, 'Validation failed', {param: 'error msg'});
            expect(JSON.parse(JSON.stringify(error))).to.deep.equal({
                statusCode: 400,
                message: 'Validation failed',
                type: 'ApiError',
                data: {
                    param: 'error msg'
                }
            });
        });

        it('should render type corresponding to derived error class', () => {
            class DerivedError extends ApiError {}
            const error = new DerivedError(400);
            expect(JSON.parse(JSON.stringify(error))).to.deep.equal({
                statusCode: 400,
                message: 'Bad Request',
                type: 'DerivedError'
            });
        });
    });
});
