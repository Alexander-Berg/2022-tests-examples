import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Event landing', () => {
    describe('should return event landing', () => {
        landingWithTlds(
            () => '/event/a5834700-8b76-431d-906d-a49de79aefe1',
            () => ({
                eventId: 'a5834700-8b76-431d-906d-a49de79aefe1',
                id: 'event'
            })
        );
    });

    describe('should return undefined for extra sublanding', () => {
        landingWithTlds(
            () => '/event/a5834700-8b76-431d-906d-a49de79aefe1/sublanding',
            () => undefined
        );
    });

    describe('should return undefined with region', () => {
        landingWithTlds(
            () => '/213/moscow/event/a5834700-8b76-431d-906d-a49de79aefe1/sublanding',
            () => undefined
        );
    });
});
