
#import "YXTCommandArgumentsProvider.h"
#import "YXTCommandInput.h"

@implementation YXTCommandArgumentsProvider

+ (NSString *)objectIdForParameters:(NSDictionary *)parameters
{
    return parameters[@"objectId"];
}

+ (NSString *)objectIdForInput:(YXTCommandInput *)input
{
    return [self objectIdForParameters:input.arguments];
}

+ (NSString *)listenerIdForInput:(YXTCommandInput *)input
{
    return input.arguments[@"listenerId"];
}

+ (NSString *)logStringForInput:(YXTCommandInput *)input
{
    NSString *argumentsDescription = @"";
    if (input.arguments.count > 0) {
        NSData *argumentsDescriptionData = [NSJSONSerialization dataWithJSONObject:input.arguments
                                                                           options:0
                                                                             error:nil];
        argumentsDescription = [[NSString alloc] initWithData:argumentsDescriptionData
                                                     encoding:NSUTF8StringEncoding];
    }
    return argumentsDescription;
}

@end
