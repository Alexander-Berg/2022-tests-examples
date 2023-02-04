import * as chai from 'chai';
import Feedback from '../src/feedback';

chai.should();

describe('Feedback', () => {
    const defaultOptions = {subject: 'test', recipientEmail: 'WW'};
    const defaultMessage = 'email: noreply@yandex-team.ru';

    describe('getSender()', () => {

        it('should return default email without `senderEmail` option', () => {
            const feedback = new Feedback(defaultOptions);
            feedback.getSender().should.equal('noreply@yandex-team.ru');
        });

        it('should return name of sender and default email', () => {
            const feedback = new Feedback({...defaultOptions, senderName: 'user'});
            feedback.getSender().should.equal('user <noreply@yandex-team.ru>');
        });

        it('should return email of sender', () => {
            const senderEmail = 'test@example.com';
            const feedback = new Feedback({...defaultOptions, senderEmail});
            feedback.getSender().should.equal(senderEmail);
        });

        it('should return name and email of sender', () => {
            const feedback = new Feedback({
                ...defaultOptions,
                senderName: 'user',
                senderEmail: 'user@yandex-team.ru'
            });
            feedback.getSender().should.equal('user <user@yandex-team.ru>');
        });
    });

    describe('getSubject()', () => {
        it('should return subject of feedback', () => {
            const subject = 'some subject';
            const feedback = new Feedback({
                ...defaultOptions,
                subject
            });
            feedback.getSubject().should.equal(subject);
        });
    });

    describe('getRecipient()', () => {
        it('should return email of recipient', () => {
            const feedback = new Feedback({...defaultOptions, recipientEmail: 'support@maps.yandex.ru'});
            feedback.getRecipient().should.equal('support@maps.yandex.ru');
        });
    });

    describe('getMessage()', () => {
        it('should return message without comment', () => {
            const feedback = new Feedback(defaultOptions);

            const message = feedback.getMessage();
            message.should.equal([
                '----',
                defaultMessage
            ].join('\n'));
        });

        it('should return message with comment and email', () => {
            const comment = 'my comment\nfoo\nbar';
            const feedback = new Feedback({
                ...defaultOptions,
                comment
            });

            const message = feedback.getMessage();
            message.should.equal([
                comment,
                '',
                '----',
                defaultMessage
            ].join('\n'));
        });

        it('should insert actual sender\'s email to additional fields', () => {
            const feedback = new Feedback({
                ...defaultOptions,
                comment: 'comment',
                senderEmail: 'user@example.com'
            });

            const message = feedback.getMessage();
            message.should.equal([
                'comment',
                '',
                '----',
                'email: user@example.com'
            ].join('\n'));
        });

        it('should insert sender\'s name to additional fields', () => {
            const feedback = new Feedback({
                ...defaultOptions,
                comment: 'comment',
                senderName: 'user name'
            });

            const message = feedback.getMessage();
            message.should.equal([
                'comment',
                '',
                '----',
                defaultMessage,
                'name: user name'
            ].join('\n'));
        });

        it('should return message with fields', () => {
            const feedback = new Feedback({
                ...defaultOptions,
                comment: 'comment',
                fields: {
                    rating: 5,
                    browser: 'firefox'
                }
            });

            const message = feedback.getMessage();
            message.should.equal([
                'comment',
                '',
                'rating: 5',
                'browser: firefox',
                '',
                '----',
                defaultMessage
            ].join('\n'));
        });

        it('should return message with additional fields', () => {
            const feedback = new Feedback({
                ...defaultOptions,
                comment: 'comment',
                additionalFields: {
                    foo: 1,
                    bar: 'yes'
                }
            });

            const message = feedback.getMessage();
            message.should.equal([
                'comment',
                '',
                '----',
                defaultMessage,
                'foo: 1',
                'bar: yes'
            ].join('\n'));
        });
    });
});
