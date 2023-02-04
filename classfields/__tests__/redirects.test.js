import { handleRedirects } from '../handle-redirects';

describe('redirects', () => {
    it('should redirect from main page to clients page', () => {
        expect(handleRedirects({ page: 'main', params: {} })).toEqual({ page: 'clients', params: {} });
    });

    it('should redirect from user page to user clients page', () => {
        expect(handleRedirects({ page: 'user', params: { userId: '123' } }))
            .toEqual({ page: 'userClients', params: { userId: '123' } });
    });

    it('should not redirect from clients page', () => {
        expect(handleRedirects({ page: 'clients', params: {} })).toEqual({ page: 'clients', params: {} });
    });
});
