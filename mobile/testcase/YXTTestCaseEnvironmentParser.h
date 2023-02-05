
#import <Foundation/Foundation.h>

@interface YXTTestCaseEnvironmentParser : NSObject

+ (NSString *)caseIdForParameters:(NSDictionary *)parameters;
+ (NSDictionary *)parameterForKey:(NSString *)key parameters:(NSDictionary *)parameters;
+ (NSString *)baseUrlForParameter:(NSDictionary *)parameter;
+ (NSTimeInterval)timeoutForParameter:(NSDictionary *)parameter
                       defaultTimeout:(NSTimeInterval)defaultTimeout;
+ (NSTimeInterval)retryIntervalForParameter:(NSDictionary *)parameter
                       defaultRetryInterval:(NSTimeInterval)defaultRetryInterval;

@end
