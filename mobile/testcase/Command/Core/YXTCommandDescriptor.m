
#import "YXTCommandDescriptor.h"

@implementation YXTCommandDescriptor

- (instancetype)initWithParameters:(NSDictionary *)parameters
{
    self = [super init];
    if (self != nil) {
        _commandId = parameters[@"id"];
        _className = parameters[@"class"];
        _name = parameters[@"name"];
    }
    return self;
}

- (NSString *)logString
{
    return [NSString stringWithFormat:@"%@.%@", self.className, self.name];
}

@end
