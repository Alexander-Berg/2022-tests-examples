
#import "YXTCommandInput.h"
#import "YXTCommandArgumentsProvider.h"
#import "YXTCommandDescriptor.h"
#import "YXTCommandResultReporter.h"
#import "YXTCommandCallbackReporter.h"

@interface YXTCommandInput ()

@property (nonatomic, strong, readonly) YXTCommandResultReporter *resultReporter;

@end

@implementation YXTCommandInput

- (instancetype)initWithArguments:(NSDictionary *)arguments
                       descriptor:(YXTCommandDescriptor *)descriptor
                   resultReporter:(YXTCommandResultReporter *)resultReporter
                 callbackReporter:(YXTCommandCallbackReporter *)callbackReporter
{
    self = [super init];
    if (self != nil) {
        _arguments = [arguments copy];
        _descriptor = descriptor;
        _resultReporter = resultReporter;
        _callbackReporter = callbackReporter;
    }
    return self;
}

- (NSString *)shortLogString
{
    return [self.descriptor logString];
}

- (NSString *)detailedLogString
{
    NSString *argumentsLogString = [YXTCommandArgumentsProvider logStringForInput:self];
    return [NSString stringWithFormat:@"%@(%@)", [self shortLogString], argumentsLogString];
}


- (void)complete
{
    [self completeWithResult:nil];
}

- (void)completeWithResult:(NSDictionary *)result
{
    [self.resultReporter reportResult:result forCommandWithId:self.descriptor.commandId];
    [self.delegate commandInputDidFinishRun];
}

- (void)completeWithError:(NSString *)error withResult:(NSDictionary *)result
{
    [self.resultReporter reportError:error withResult:result forCommandWithId:self.descriptor.commandId];
    [self.delegate commandInputDidFinishRun];
}

@end
