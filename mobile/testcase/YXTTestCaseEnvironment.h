
#import <Foundation/Foundation.h>

@interface YXTTestCaseEnvironment : NSObject

@property (nonatomic, copy, readonly) NSString *caseId;

@property (nonatomic, copy, readonly) NSString *commandRequestUrl;
@property (nonatomic, assign, readonly) NSTimeInterval commandRequestTimeout;
@property (nonatomic, assign, readonly) NSTimeInterval commandRequestRetryInterval;

@property (nonatomic, copy, readonly) NSString *commandResultUrl;
@property (nonatomic, assign, readonly) NSTimeInterval commandResultTimeout;

@property (nonatomic, copy, readonly) NSString *commandCallbackUrl;
@property (nonatomic, assign, readonly) NSTimeInterval commandCallbackTimeout;

- (instancetype)initWithTestCaseParameters:(NSDictionary *)parameters;

@end

