
#import "YXTCommandCallbackReporter.h"
#import "YXTCommandCore.h"
#import "YXTCommandDescriptor.h"
#import "YXTTestCommandCompleteHelper.h"

@implementation YXTTestCommandCompleteHelper

+ (void(^)(NSError *error))onFailureBlockForInput:(YXTCommandInput *)input
{
    return ^(NSError *error) {
        [input.callbackReporter reportResult:[self resultFailureWithError:error]
                                 forObjectId:input.descriptor.commandId
                                    callback:@"failure"
                                  completion:nil];
    };
}

+ (NSDictionary *)resultWithValue:(id)value
{
    return @{ @"value": value ?: [NSNull null] };
}

+ (NSDictionary *)resultSuccess
{
    return @{
             @"status": @"success"
             };
}

+ (NSDictionary *)resultFailureWithError:(NSError *)error
{
    return @{
             @"status": @"failure",
             @"domain": error.domain ?: @"unknown",
             @"code": @(error.code),
             @"message": error.description ?: @"unknown",
             };
}

@end
