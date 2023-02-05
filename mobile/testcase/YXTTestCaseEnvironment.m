
#import "YXTTestCaseEnvironment.h"
#import "YXTTestCaseEnvironmentParser.h"

@interface YXTTestCaseEnvironment ()

@property (nonatomic, copy, readwrite) NSString *commandRequestUrl;
@property (nonatomic, assign, readwrite) NSTimeInterval commandRequestTimeout;
@property (nonatomic, assign, readwrite) NSTimeInterval commandRequestRetryInterval;

@property (nonatomic, copy, readwrite) NSString *commandResultUrl;
@property (nonatomic, assign, readwrite) NSTimeInterval commandResultTimeout;

@property (nonatomic, copy, readwrite) NSString *commandCallbackUrl;
@property (nonatomic, assign, readwrite) NSTimeInterval commandCallbackTimeout;

@end

@implementation YXTTestCaseEnvironment

- (instancetype)initWithTestCaseParameters:(NSDictionary *)parameters
{
    self = [super init];
    if (self != nil) {
        _caseId = [YXTTestCaseEnvironmentParser caseIdForParameters:parameters];
        [self parseCommandRequestWithParameters:parameters];
        [self parseCommandResultWithParameters:parameters];
        [self parseCommandCallbackWithParameters:parameters];
    }
    return self;
}

- (void)parseCommandRequestWithParameters:(NSDictionary *)parameters
{
    NSDictionary *commandRequest =
        [YXTTestCaseEnvironmentParser parameterForKey:@"commandRequest" parameters:parameters];
    self.commandRequestUrl = [YXTTestCaseEnvironmentParser baseUrlForParameter:commandRequest];
    self.commandRequestTimeout =
        [YXTTestCaseEnvironmentParser timeoutForParameter:commandRequest
                                           defaultTimeout:10.0];
    self.commandRequestRetryInterval =
        [YXTTestCaseEnvironmentParser retryIntervalForParameter:commandRequest
                                           defaultRetryInterval:0.05];
}

- (void)parseCommandResultWithParameters:(NSDictionary *)parameters
{
    NSDictionary *commandResult =
        [YXTTestCaseEnvironmentParser parameterForKey:@"commandResult" parameters:parameters];
    self.commandResultUrl = [YXTTestCaseEnvironmentParser baseUrlForParameter:commandResult];
    self.commandResultTimeout =
        [YXTTestCaseEnvironmentParser timeoutForParameter:commandResult
                                           defaultTimeout:1.0];
}

- (void)parseCommandCallbackWithParameters:(NSDictionary *)parameters
{
    NSDictionary *commandCallback =
        [YXTTestCaseEnvironmentParser parameterForKey:@"commandCallback" parameters:parameters];
    self.commandCallbackUrl = [YXTTestCaseEnvironmentParser baseUrlForParameter:commandCallback];
    self.commandCallbackTimeout =
        [YXTTestCaseEnvironmentParser timeoutForParameter:commandCallback
                                           defaultTimeout:1.0];
}

@end

