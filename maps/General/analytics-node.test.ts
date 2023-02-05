import AnalyticsNode from './analytics-node';
import AnalyticsLogger from './analytics-logger';
import {Config} from 'types/config';

const ConfigMock = jest.fn<Config>(() => ({}));
const configStub: Config = new ConfigMock();
const logFn = jest.fn(() => {});
const MockAnalyticsLogger = jest.fn<AnalyticsLogger>(() => ({log: logFn}));
const analyticsLoggerStub = new MockAnalyticsLogger();

describe('AnalyticsNode', () => {
    const analyticsNodeState = {foo: 'bar'};
    let analyticsNode: AnalyticsNode;

    const rootNode = new AnalyticsNode(
        'maps_www',
        null,
        {},
        undefined,
        undefined,
        undefined,
        undefined,
        analyticsLoggerStub,
        configStub
    );

    beforeEach(() => {
        logFn.mockReset();
        analyticsNode = new AnalyticsNode(
            'test',
            rootNode,
            analyticsNodeState,
            undefined,
            undefined,
            undefined,
            undefined,
            analyticsLoggerStub,
            configStub
        );
    });

    describe('attach()', () => {
        it('should log "show" event when attached to root', () => {
            analyticsNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'show', vars: analyticsNodeState}),
                expect.anything()
            );
        });

        it('should not log "show" event if already attached', () => {
            analyticsNode.attach();
            logFn.mockReset();

            analyticsNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);
        });

        it('should not log "show" event if parent is not attached to root', () => {
            const childNode = new AnalyticsNode(
                'child',
                analyticsNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            childNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);
        });

        it('should log "show" event for every attached child when attached to root', () => {
            const childNode1 = new AnalyticsNode(
                'child1',
                analyticsNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            const childNode2 = new AnalyticsNode(
                'child2',
                analyticsNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            childNode1.attach();
            childNode2.attach();

            analyticsNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(3);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'show'}),
                expect.anything()
            );
        });
    });

    describe('detach()', () => {
        it('should log "hide" event when detached from root', () => {
            analyticsNode.attach();
            logFn.mockReset();

            analyticsNode.detach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'hide', vars: analyticsNodeState}),
                expect.anything()
            );
        });

        it('should not log "hide" event if not attached to root', () => {
            analyticsNode.detach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);
        });

        it('should not log "hide" event if parent is not attached to root', () => {
            const childNode = new AnalyticsNode(
                'child',
                analyticsNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            childNode.attach();
            logFn.mockReset();

            childNode.detach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);
        });

        it('should log "hide" event for every attached child when detached from root', () => {
            const childNode1 = new AnalyticsNode(
                'child1',
                analyticsNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            const childNode2 = new AnalyticsNode(
                'child2',
                analyticsNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            childNode1.attach();
            childNode2.attach();

            analyticsNode.attach();
            logFn.mockReset();

            analyticsNode.detach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(3);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'hide'}),
                expect.anything()
            );
        });
    });

    describe('changeState()', () => {
        it('should change state of the node', () => {
            const newState = {qwe: 'qwe'};
            analyticsNode.changeState(newState);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);

            analyticsNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'show', vars: newState}),
                expect.anything()
            );
        });

        it('should log "change_state" event if node is attached to root', () => {
            const newState = {qwe: 'qwe'};
            analyticsNode.attach();
            logFn.mockReset();

            analyticsNode.changeState(newState);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'change_state', vars: newState}),
                expect.anything()
            );
        });
    });

    describe('logEvent()', () => {
        it('should not pass events to logger if node is not attached', () => {
            analyticsNode.logEvent('some_event');
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);
        });

        it('should pass events to logger if node is attached to root', () => {
            analyticsNode.attach();
            logFn.mockReset();

            analyticsNode.logEvent('blah');
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({event_type: 'blah', vars: analyticsNodeState}),
                expect.anything()
            );
        });

        it('should merge event params with current state of the node', () => {
            const newState = {one: 1};
            analyticsNode.changeState(newState);
            analyticsNode.attach();
            logFn.mockReset();

            analyticsNode.logEvent('some_event', {two: 2});
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'some_event',
                    vars: {one: 1, two: 2}
                }),
                expect.anything()
            );
        });
    });

    describe('creation', () => {
        it('should pass parent_id to logger', () => {
            const childNode = new AnalyticsNode(
                'child',
                rootNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            childNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'show',
                    path: 'maps_www.child',
                    parent_id: 1
                }),
                expect.anything()
            );
        });

        it('should combine path using parent`s path', () => {
            const childNode1 = new AnalyticsNode(
                'child_one',
                rootNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            const childNode2 = new AnalyticsNode(
                'child_two',
                childNode1,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            childNode1.attach();
            logFn.mockReset();

            childNode2.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'show',
                    path: 'maps_www.child_one.child_two'
                }),
                expect.anything()
            );
        });
    });

    describe('hidden child node', () => {
        it('should not show new attached node if it is initially hidden', () => {
            const child = new AnalyticsNode(
                'child',
                rootNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            child.attach(true);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);
        });

        it('should not show hidden child node when parent is being shown', () => {
            const parent = rootNode.createChild('parent', {}).attach(true);
            parent.createChild('child', {}).attach(true);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);

            parent.setHidden(false);

            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'show',
                    path: 'maps_www.parent'
                }),
                expect.anything()
            );
        });

        it('should not log "show" when parent is hidden', () => {
            const parent = rootNode.createChild('parent', {}).attach(true);
            const child = parent.createChild('child', {}).attach(true);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);

            child.setHidden(false);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(0);

            parent.setHidden(false);
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(2);
        });
    });

    describe('group node', () => {
        it('should not affect the analytics path', () => {
            const groupNode = createGroupChild();
            groupNode.attach();
            const nextChildNode = new AnalyticsNode(
                'child',
                groupNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            nextChildNode.attach();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(1);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'show',
                    path: 'maps_www.child'
                }),
                expect.anything()
            );
        });

        it('should not log any events in hidden subtree', () => {
            const firstChild = new AnalyticsNode(
                'child1',
                rootNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            firstChild.attach();
            firstChild.hide();
            const secondChild = new AnalyticsNode(
                'child2',
                firstChild,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub
            );
            secondChild.attach();
            secondChild.show();
            expect(analyticsLoggerStub.log).toHaveBeenCalledTimes(2);
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'show',
                    path: 'maps_www.child1'
                }),
                expect.anything()
            );
            expect(analyticsLoggerStub.log).toHaveBeenCalledWith(
                expect.objectContaining({
                    event_type: 'hide',
                    path: 'maps_www.child1'
                }),
                expect.anything()
            );
        });

        function createGroupChild(): AnalyticsNode {
            return new AnalyticsNode(
                undefined,
                rootNode,
                {},
                undefined,
                undefined,
                undefined,
                undefined,
                analyticsLoggerStub,
                configStub,
                undefined,
                true
            );
        }
    });
});
