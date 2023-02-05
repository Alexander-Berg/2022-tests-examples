
#import "YXTTimeManager.h"
#import "NSDate+YXTUpdateTime.h"

double yxt_getTime()
{
    return [YXTTimeManager time];
}

void yxt_updateTime(double millis)
{
    [YXTTimeManager updateTimeWithDelta:millis];
}

void yxt_resetTime()
{
    [YXTTimeManager resetTime];
}

NSTimeInterval yxt_getUptime()
{
    return [YXTTimeManager uptime];
}

NSInteger yxt_getTimezoneOffset()
{
    return [YXTTimeManager timezoneOffset];
}

@implementation YXTTimeManager

+ (double)time
{
    return [[NSDate date] timeIntervalSince1970] * 1000;
}

+ (void)updateTimeWithDelta:(double)millis
{
    [NSDate yxt_updateTimeWithInterval:millis / 1000];
}

+ (void)resetTime
{
    [NSDate yxt_resetTime];
}

+ (NSTimeInterval)uptime
{
    return [[NSProcessInfo processInfo] systemUptime];
}

+ (NSInteger)timezoneOffset
{
    return [[NSTimeZone systemTimeZone] secondsFromGMT] * 1000;
}

@end
