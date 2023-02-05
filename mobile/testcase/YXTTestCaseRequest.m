
#import "YXTTestCaseRequest.h"
#import "YXTLog.h"

static NSTimeInterval kYXTTestCaseRequestTimeout = 60.0;
static NSString *const kYXTTestCaseRequestLogPrefix = @"Case Request: ";
static NSTimeInterval const kYXTCaseRequestRetryInterval = 2.0;

@implementation YXTTestCaseRequest

+ (void)requestCaseWithUrl:(NSString *)requestUrl
                  callback:(void(^)(NSDictionary *))callback
{
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:requestUrl]
                                                           cachePolicy:NSURLRequestReloadIgnoringCacheData
                                                       timeoutInterval:kYXTTestCaseRequestTimeout];
    request.HTTPMethod = @"GET";
    void (^completionHandler)(NSURLResponse *, NSData *, NSError *) =
        ^(NSURLResponse *response, NSData *data, NSError *connectionError) {
            [self processRequestWithUrl:requestUrl
                               response:response
                                   data:data
                                  error:connectionError
                               callback:callback];
        };
    
    [NSURLConnection sendAsynchronousRequest:request
                                       queue:[NSOperationQueue mainQueue]
                           completionHandler:completionHandler];
}

+ (void)processRequestWithUrl:(NSString *)requestUrl
                     response:(NSURLResponse *)response
                         data:(NSData *)data
                        error:(NSError *)error
                     callback:(void(^)(NSDictionary *))callback
{
    NSDictionary *result = nil;
    
    NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
    unsigned long statusCode = (unsigned long)httpResponse.statusCode;
    if (statusCode >= 200 && statusCode < 300) {
        result = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
        if ([result isKindOfClass:[NSDictionary class]] == NO) {
            [NSException raise:NSInternalInconsistencyException
                        format:@"Test case parameters object is not a dictionary"];
        }
        [YXTLog logFormat:@"%@[%lu] %@ %@", kYXTTestCaseRequestLogPrefix, statusCode, result, error];
    }
    else {
        [YXTLog logFormat:@"%@[%lu] %@", kYXTTestCaseRequestLogPrefix, statusCode, error];
        int64_t retryInterval = (int64_t)(kYXTCaseRequestRetryInterval * NSEC_PER_SEC);
        dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, retryInterval), queue, ^{
            [YXTLog logFormat:@"Retry case request: %@", requestUrl];
            [self requestCaseWithUrl:requestUrl callback:callback];
        });
    }
    
    callback(result);
}

@end
