
#import <Foundation/Foundation.h>

@class YXTTestCaseEnvironment;

@interface YXTTestCaseRunner : NSObject

- (instancetype)initWithEnvironment:(YXTTestCaseEnvironment *)environment;

- (void)run;

@end
