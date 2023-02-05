
#import "YXTTestCaseRunner.h"
#import "YXTTestCaseEnvironment.h"
#import "YXTCommandOperation.h"
#import "YXTCommandsRequestOperation.h"
#import "YXTReporter.h"
#import "YXTCommandResultReporter.h"
#import "YXTCommandCallbackReporter.h"
#import "YXTLog.h"

@interface YXTTestCaseRunner () <YXTCommandsRequestOperationDelegate>

@property (nonatomic, strong, readonly) YXTTestCaseEnvironment *environment;
@property (nonatomic, strong, readonly) NSOperationQueue *networkQueue;
@property (nonatomic, strong, readonly) NSOperationQueue *commandsQueue;
@property (nonatomic, strong, readonly) YXTCommandResultReporter *resultReporter;
@property (nonatomic, strong, readonly) YXTCommandCallbackReporter *callbackReporter;

@end

@implementation YXTTestCaseRunner

- (instancetype)initWithEnvironment:(YXTTestCaseEnvironment *)environment
{
    self = [super init];
    if (self != nil) {
        NSOperationQueue *networkQueue = [[self class] networkQueueForEnvironment:environment];
        NSOperationQueue *commandsQueue = [[self class] commandsQueueForEnvironment:environment];
        
        _resultReporter = [[self class] resultReporterForEnvironment:environment
                                                        networkQueue:networkQueue];
        _callbackReporter = [[self class] callbackReporterForEnvironment:environment
                                                            networkQueue:networkQueue
                                                           commandsQueue:commandsQueue];
        _environment = environment;
        _networkQueue = networkQueue;
        _commandsQueue = commandsQueue;
    }
    return self;
}

+ (NSOperationQueue *)networkQueueForEnvironment:(YXTTestCaseEnvironment *)environment
{
    NSOperationQueue *networkQueue = [[NSOperationQueue alloc] init];
    networkQueue.name = @"TestCaseRun:Network";
    return networkQueue;
}

+ (NSOperationQueue *)commandsQueueForEnvironment:(YXTTestCaseEnvironment *)environment
{
    NSOperationQueue *commandsQueue = [[NSOperationQueue alloc] init];
    commandsQueue.name = @"TestCaseRun";
    commandsQueue.maxConcurrentOperationCount = 1;
    return commandsQueue;
}

+ (YXTCommandResultReporter *)resultReporterForEnvironment:(YXTTestCaseEnvironment *)environment
                                              networkQueue:(NSOperationQueue *)networkQueue
{
    YXTReporter *resultReporter = [[YXTReporter alloc] initWithBaseURL:environment.commandResultUrl
                                                          networkQueue:networkQueue
                                                               timeout:environment.commandResultTimeout];
    return [[YXTCommandResultReporter alloc] initWithReporter:resultReporter];
}

+ (YXTCommandCallbackReporter *)callbackReporterForEnvironment:(YXTTestCaseEnvironment *)environment
                                                 networkQueue:(NSOperationQueue *)networkQueue
                                                 commandsQueue:(NSOperationQueue *)commandsQueue
{
    YXTReporter *callbackReporter = [[YXTReporter alloc] initWithBaseURL:environment.commandCallbackUrl
                                                            networkQueue:networkQueue
                                                                 timeout:environment.commandCallbackTimeout];
    return [[YXTCommandCallbackReporter alloc] initWithReporter:callbackReporter commandsQueue:commandsQueue];
}

- (void)run
{
    [self logAppVersion];
    [self enqueueCommandsRequestWithLastCommandId:nil];
}

- (void)logAppVersion
{
    NSBundle *bundle = [NSBundle mainBundle];
    NSString *buildNumber = bundle.infoDictionary[@"CFBundleVersion"];
    NSString *version = bundle.infoDictionary[@"CFBundleShortVersionString"];
    [YXTLog logFormat:@"App version: %@ (%@)", version, buildNumber];
}

- (void)enqueueCommandsRequestWithLastCommandId:(NSString *)lastCommandId
{
    YXTCommandsRequestOperation *commandsRequestOperation =
        [[YXTCommandsRequestOperation alloc] initWithBaseUrl:self.environment.commandRequestUrl
                                               lastCommandId:lastCommandId
                                                networkQueue:self.networkQueue
                                                     timeout:self.environment.commandRequestTimeout
                                               retryInterval:self.environment.commandRequestRetryInterval
                                                    delegate:self];
    [self.commandsQueue addOperation:commandsRequestOperation];
}

- (NSArray *)commandsForParameters:(NSArray *)parameters
{
    NSMutableArray *commands = [NSMutableArray array];
    for (NSDictionary *commandParameters in parameters) {
        YXTCommandOperation *commandOperation =
            [YXTCommandOperationFactory operationWithParameters:commandParameters
                                                 resultReporter:self.resultReporter
                                               callbackReporter:self.callbackReporter];
        if (commandOperation != nil) {
            [commands addObject:commandOperation];
        }
    }
    return [commands copy];
}

- (void)enqueueCommands:(NSArray<YXTCommandOperation *> *)commands
{
    for (YXTCommandOperation *commandOperation in commands) {
        [YXTLog logFormat:@"Enqueue command: %@", [commandOperation logString]];
        [self.commandsQueue addOperation:commandOperation];
    }
}

#pragma mark - YXTCommandsRequestOperationDelegate

- (void)commandsRequestOperation:(YXTCommandsRequestOperation *)operation
                 didLoadCommands:(NSDictionary *)commands
{
    NSArray<YXTCommandOperation *> *additionalCommands = [self commandsForParameters:commands[@"commands"]];
    
    [YXTLog logMessage:@"Enqueue additional commands from server"];
    [self enqueueCommands:additionalCommands];
    [self enqueueCommandsRequestWithLastCommandId:additionalCommands.lastObject.commandId];
}

@end
