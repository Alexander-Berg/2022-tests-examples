
#import "YXTTestCaseEnvironmentParser.h"

static NSString *const kYXTCaseIdKey = @"caseId";
static NSString *const kYXTEnvironmentKey = @"environment";

static NSString *const kYXTBaseUrlKey = @"baseUrl";
static NSString *const kYXTTimeoutKey = @"timeout";
static NSString *const kYXTRetryIntervalKey = @"retryInterval";

@implementation YXTTestCaseEnvironmentParser

+ (NSString *)caseIdForParameters:(NSDictionary *)parameters
{
    return parameters[kYXTCaseIdKey];
}

+ (NSDictionary *)parameterForKey:(NSString *)key parameters:(NSDictionary *)parameters
{
    return parameters[kYXTEnvironmentKey][key];
}

+ (NSString *)baseUrlForParameter:(NSDictionary *)parameter
{
    return parameter[kYXTBaseUrlKey];
}

+ (NSTimeInterval)timeoutForParameter:(NSDictionary *)parameter
                       defaultTimeout:(NSTimeInterval)defaultTimeout
{
    NSNumber *timeout = parameter[kYXTTimeoutKey];
    return timeout != nil ? [timeout doubleValue] : defaultTimeout;
}

+ (NSTimeInterval)retryIntervalForParameter:(NSDictionary *)parameter
                       defaultRetryInterval:(NSTimeInterval)defaultRetryInterval
{
    NSNumber *retryInterval = parameter[kYXTRetryIntervalKey];
    return retryInterval != nil ? [retryInterval doubleValue] : defaultRetryInterval;
}

@end
