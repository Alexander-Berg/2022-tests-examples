
#import "YXTCommandDelegate.h"
#import "YXTCommandInput.h"
#import "YXTCommandArgumentsProvider.h"
#import "YXTCommandCallbackReporter.h"
#import "YXTTestObjectsStorage.h"

@interface YXTCommandDelegate ()

@property (nonatomic, strong, readonly) NSString *objectId;
@property (nonatomic, strong, readonly) YXTCommandCallbackReporter *callbackReporter;

@end

@implementation YXTCommandDelegate

+ (instancetype)delegateWithCommandInput:(YXTCommandInput *)commandInput
{
    NSString *objectId = [YXTCommandArgumentsProvider listenerIdForInput:commandInput];
    YXTCommandCallbackReporter *callbackReporter = commandInput.callbackReporter;
    YXTTestObjectsStorage *storage = [YXTTestObjectsStorage sharedInstance];
    
    YXTCommandDelegate *delegate = [storage objectForKey:objectId];
    if (delegate == nil) {
        delegate = [[self alloc] initWithObjectId:objectId
                                 callbackReporter:callbackReporter];
        [storage setObject:delegate forKey:objectId];
    }
    return delegate;
}

- (instancetype)initWithObjectId:(NSString *)objectId
                callbackReporter:(YXTCommandCallbackReporter *)callbackReporter
{
    self = [super init];
    if (self != nil) {
        _objectId = objectId;
        _callbackReporter = callbackReporter;
    }
    return self;
}

- (void)completeWithCallback:(NSString *)callback
{
    [self completeWithCallback:callback completion:nil];
}

- (void)completeWithCallback:(NSString *)callback
                  completion:(void(^)(NSDictionary *))completion
{
    [self completeWithCallback:callback result:nil completion:completion];
}

- (void)completeWithCallback:(NSString *)callback
                      result:(NSDictionary *)result
{
    [self completeWithCallback:callback result:result completion:nil];
}

- (void)completeWithCallback:(NSString *)callback
                      result:(NSDictionary *)result
                  completion:(void(^)(NSDictionary *))completion
{
    [self.callbackReporter reportResult:result
                            forObjectId:self.objectId
                               callback:callback
                             completion:completion];
}

@end
