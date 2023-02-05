
#import "YXTCommandCallbackReporter.h"
#import "YXTReporter.h"

@interface YXTCommandCallbackReporter ()

@property (nonatomic, strong, readonly) YXTReporter *reporter;
@property (nonatomic, strong, readonly) NSOperationQueue *commandsQueue;

@end

@implementation YXTCommandCallbackReporter

- (instancetype)initWithReporter:(YXTReporter *)reporter
                   commandsQueue:(NSOperationQueue *)commandsQueue
{
    self = [super init];
    if (self != nil) {
        _reporter = reporter;
        _commandsQueue = commandsQueue;
    }
    return self;
}

- (void)reportResult:(NSDictionary *)result
         forObjectId:(NSString *)objectId
            callback:(NSString *)callback
          completion:(void(^)(NSDictionary *))completion
{
    if (objectId.length == 0) {
        [NSException raise:NSInvalidArgumentException format:@"Empty objectId parameter"];
    }
    if (callback.length == 0) {
        [NSException raise:NSInvalidArgumentException format:@"Empty callback parameter"];
    }
    NSDictionary *queryParameters = @{
        @"listenerId" : objectId,
        @"callback" : callback
    };
    BOOL hasCompletion = completion != nil;
    if (hasCompletion) {
        //TODO(anamak): ADLIB-2368 fix setting objectId in callback completion.
        //Do not start new command until callback completion is finished
        NSAssert([NSThread isMainThread], @"Callback should be reported only on main thread");
        self.commandsQueue.suspended = YES;
    }
    [self.reporter reportResult:result withQueryParameters:queryParameters completion:^(NSDictionary *results) {
        if (hasCompletion) {
            completion(results);
            self.commandsQueue.suspended = NO;
        }
    }];
}

@end
