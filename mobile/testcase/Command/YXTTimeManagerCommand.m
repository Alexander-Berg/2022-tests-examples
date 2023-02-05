
#import "YXTCommandCore.h"
#import "YXTTimeManager.h"

@interface YXTTimeManagerCommand : NSObject

@end

@implementation YXTTimeManagerCommand

+ (void)load
{
    REGISTER_CLASS(@"TimeManager", {
        WITH_COMMAND(@"getTime", [self getTime:input]);
        WITH_COMMAND(@"updateTime", [self updateTime:input]);
        WITH_COMMAND(@"resetTime", [self resetTime:input]);
        WITH_COMMAND(@"getUptime", [self getUptime:input]);
        WITH_COMMAND(@"getTimezoneOffset", [self getTimezoneOffset:input]);
    });
}

#pragma mark - Commands

+ (void)getTime:(YXTCommandInput *)input
{
    double time = [YXTTimeManager time];
    [input completeWithResult:@{ @"value": @(time) }];
}

+ (void)updateTime:(YXTCommandInput *)input
{
    double delta = [input.arguments[@"delta"] doubleValue];
    [YXTTimeManager updateTimeWithDelta:delta];
    [input complete];
}

+ (void)resetTime:(YXTCommandInput *)input
{
    [YXTTimeManager resetTime];
    [input complete];
}

+ (void)getUptime:(YXTCommandInput *)input
{
    double uptime = [YXTTimeManager uptime];
    [input completeWithResult:@{ @"value": @(uptime) }];
}

+ (void)getTimezoneOffset:(YXTCommandInput *)input
{
    NSInteger timezoneOffset = [YXTTimeManager timezoneOffset];
    [input completeWithResult:@{ @"value": @(timezoneOffset) }];
}

@end
