
#import <Foundation/Foundation.h>

@interface YXTCommandDescriptor : NSObject

@property (nonatomic, copy, readonly) NSString *commandId;
@property (nonatomic, copy, readonly) NSString *className;
@property (nonatomic, copy, readonly) NSString *name;

- (instancetype)initWithParameters:(NSDictionary *)parameters;

- (NSString *)logString;

@end
