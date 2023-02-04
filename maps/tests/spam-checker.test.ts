import * as url from 'url';
import * as nock from 'nock';
import * as chai from 'chai';
import getSpamCheckHeaders, {
    SPAM_FLAG_HEADER,
    SPAM_CHECK_HEADER,
    SPAM_UID_HEADER,
    SpamCheckOptions
} from '../src/spam-checker';
import Feedback from '../src/feedback';

chai.should();
nock.disableNetConnect();

/*tslint:disable no-identical-functions*/
describe('spam check', () => {
    const CHECK_URL = 'http://checkspam';
    const NEGATIVE_SPAM_MSG = '<spam>0</spam>';
    const POSITIVE_SPAM_MSG = '<spam>1</spam>';
    const UNIQUE_ID = '123';
    const DEFAULT_OPTIONS = {
        serviceUrl: CHECK_URL,
        ip: '127.0.0.1',
        host: 'example.com',
        realpath: '/some/path',
        formName: 'maps',
        uniqueId: UNIQUE_ID
    };
    const FEEDBACK_OPTIONS = {
        subject: 'subject of feedback',
        recipientEmail: 'feedback@yandex.ru',
        senderName: 'maps',
        senderEmail: 'user@example.com',
        comment: 'hello'
    };
    const negativeHeadersHandler = (headers: Record<string, string>): void => {
        headers.should.deep.equal({
            [SPAM_FLAG_HEADER]: 'UNDEF',
            [SPAM_CHECK_HEADER]: 'ERROR'
        });
    };

    describe('getSpamCheckHeaders()', () => {
        it('should send correct params to spam check service', () => {
            // nock сравнивает GET запросы по строкам, поэтому важен порядок параметров
            // https://github.com/pgte/nock/issues/82
            const feedback = new Feedback(FEEDBACK_OPTIONS);
            const query = {
                so_uid: '123',
                so_ip: '127.0.0.1',
                so_host: 'example.com',
                so_realpath: '/some/path',
                so_service: 'FEEDBACK',
                so_comment: feedback.getMessage(),
                so_form_name: 'maps'
            };

            nock(CHECK_URL)
                .get(url.format({pathname: '/check', query}))
                .reply(200, NEGATIVE_SPAM_MSG);

            return getSpamCheckHeaders({
                ...DEFAULT_OPTIONS,
                feedback
            });
        });

        describe('on response handling', () => {
            let options: SpamCheckOptions;
            let spamCheckService: any;

            beforeEach(() => {
                options = {
                    ...DEFAULT_OPTIONS,
                    feedback: new Feedback(FEEDBACK_OPTIONS)
                };

                spamCheckService = nock(CHECK_URL)
                    .filteringPath(() => '/')
                    .get('/');
            });

            it('should resolve promise with undef spam status headers', () => {
                return getSpamCheckHeaders(false).then((headers) => {
                    headers.should.deep.equal({
                        [SPAM_FLAG_HEADER]: 'UNDEF',
                        [SPAM_CHECK_HEADER]: 'IS_DISABLED'
                    });
                });
            });

            it('should resolve promise with positive spam status headers', () => {
                spamCheckService.reply(200, POSITIVE_SPAM_MSG);

                return getSpamCheckHeaders(options).then((headers) => {
                    headers.should.deep.equal({
                        [SPAM_FLAG_HEADER]: 'YES',
                        [SPAM_CHECK_HEADER]: 'OK',
                        [SPAM_UID_HEADER ]: UNIQUE_ID
                    });
                });
            });

            it('should resolve promise with negative spam status headers', () => {
                spamCheckService.reply(200, NEGATIVE_SPAM_MSG);

                return getSpamCheckHeaders(options).then((headers) => {
                    headers.should.deep.equal({
                        [SPAM_FLAG_HEADER]: 'NO',
                        [SPAM_CHECK_HEADER]: 'OK',
                        [SPAM_UID_HEADER ]: UNIQUE_ID
                    });
                });
            });

            it('should return error headers on unsuccess response', () => {
                spamCheckService.reply(500);

                return getSpamCheckHeaders(options).then(negativeHeadersHandler);
            });

            it('should return error headers on response without body', () => {
                spamCheckService.reply(200);

                return getSpamCheckHeaders(options).then(negativeHeadersHandler);
            });

            it('should return error headers promise on invalid response', () => {
                spamCheckService.reply(200, '<spam></a>');

                return getSpamCheckHeaders(options).then(negativeHeadersHandler);
            });

            it('should return error headers on invalid spam status code', () => {
                spamCheckService.reply(200, '<spam>3</spam>');

                return getSpamCheckHeaders(options).then(negativeHeadersHandler);
            });

            it('should return timeout error headers on connection timeout', () => {
                spamCheckService
                    .delayConnection(100)
                    .reply(200, NEGATIVE_SPAM_MSG);

                options.timeout = 10;
                return getSpamCheckHeaders(options).then((headers) => {
                    headers.should.deep.equal({
                        [SPAM_FLAG_HEADER]: 'UNDEF',
                        [SPAM_CHECK_HEADER]: 'REQUEST_TIMEOUT'
                    });
                });
            });
        });
    });
});
