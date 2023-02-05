
#import <Foundation/Foundation.h>

@class YXTCommandDescriptor;
@class YXTCommandCallbackReporter;

@protocol YXTCommandResultDelegate;

@interface YXTCommandInput : NSObject

@property (nonatomic, copy, readonly) NSDictionary *arguments;
@property (nonatomic, strong, readonly) YXTCommandDescriptor *descriptor;
@property (nonatomic, strong, readonly) YXTCommandCallbackReporter *callbackReporter;
@property (nonatomic, weak) id<YXTCommandResultDelegate> delegate;

- (NSString *)shortLogString;
- (NSString *)detailedLogString;

- (void)complete;
- (void)completeWithResult:(NSDictionary *)result;
- (void)completeWithError:(NSString *)error withResult:(NSDictionary *)result;

@end

@protocol YXTCommandResultDelegate <NSObject>

- (void)commandInputDidFinishRun;

@end
