
#import <Foundation/Foundation.h>

extern double yxt_getTime(void);
extern NSInteger yxt_getTimezoneOffset(void);
extern void yxt_updateTime(double millis);
extern void yxt_resetTime(void);
extern NSTimeInterval yxt_getUptime(void);

@interface YXTTimeManager : NSObject

+ (double)time;
+ (void)updateTimeWithDelta:(double)millis;
+ (void)resetTime;
+ (NSTimeInterval)uptime;
+ (NSInteger)timezoneOffset;

@end
