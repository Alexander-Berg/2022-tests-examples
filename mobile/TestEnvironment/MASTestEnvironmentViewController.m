
#import "MASTestEnvironmentViewController.h"
#import "MASAlertController.h"
#import "MASKeyboardVisibilityEventHandler.h"
#import "MASStringLoader.h"
#import "MASTestEnvironmentStorage.h"
#import "MASJSONValidator.h"

static NSString *const kMASTestEnvironmentViewControllerTitle = @"Test Environment";

@interface MASTestEnvironmentViewController () <UITextViewDelegate, UITextFieldDelegate, MASStringLoaderDelegate>

@property (nonatomic, strong, readonly) MASKeyboardVisibilityEventHandler *keyboardEventHandler;
@property (nonatomic, strong, readonly) MASStringLoader *stringLoader;
@property (nonatomic, strong, readonly) MASTestEnvironmentStorage *testEnvironmentStorage;

@property (nonatomic, weak) IBOutlet UIActivityIndicatorView *activityIndicator;
@property (nonatomic, weak) IBOutlet NSLayoutConstraint *bottomConstraint;
@property (nonatomic, weak) IBOutlet UITextView *environmentTextView;
@property (nonatomic, weak) IBOutlet UILabel *environmentPlaceholderLabel;
@property (nonatomic, weak) IBOutlet UITextField *environmentURLTextField;
@property (nonatomic, weak) IBOutlet UIButton *loadButton;

@end

@implementation MASTestEnvironmentViewController

- (instancetype)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self != nil) {
        _keyboardEventHandler = [[MASKeyboardVisibilityEventHandler alloc] init];
        _stringLoader = [[MASStringLoader alloc] init];
        _testEnvironmentStorage = [[MASTestEnvironmentStorage alloc] init];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = NSLocalizedString(kMASTestEnvironmentViewControllerTitle, "");
    self.environmentTextView.text = [self.testEnvironmentStorage testEnvironment];
    [self configureKeyboard];
    [self configurePlaceholder];
    self.stringLoader.delegate = self;
}

- (IBAction)onLoadClicked:(id)sender
{
    [self configureActivityIndicatorWithHiddenState:NO];
    [self.stringLoader loadStringFromURL:[NSURL URLWithString:self.environmentURLTextField.text]];
}

- (IBAction)onSaveClicked:(id)sender
{
    NSString *testEnvironment = self.environmentTextView.text;

    if ([MASJSONValidator validateJSON:testEnvironment]) {
        [self.testEnvironmentStorage saveTestEnvironment:testEnvironment];
        [self.environmentTextView resignFirstResponder];
    } else {
        MASAlertController *alertController =
            [MASAlertController alertControllerWithTitle:@"Error" message:@"Invalid JSON"];
        [alertController present];
    }
}

- (void)configureKeyboard
{
    [self.keyboardEventHandler subscribeToNotificationsWithView:self.view];
    __typeof(self) __weak weakSelf = self;
    self.keyboardEventHandler.keyboardWillShowCallback = ^(CGFloat keyboardHeight) {
        weakSelf.bottomConstraint.constant = keyboardHeight;
    };
    self.keyboardEventHandler.keyboardWillHideCallback = ^{
        weakSelf.bottomConstraint.constant = 0;
    };
}

- (void)configurePlaceholder
{
    if (self.environmentTextView.text.length == 0) {
        self.environmentPlaceholderLabel.hidden = NO;
    }
    else {
        self.environmentPlaceholderLabel.hidden = YES;
    }
}

- (void)configureActivityIndicatorWithHiddenState:(BOOL)hidden
{
    self.activityIndicator.hidden = hidden;
    self.loadButton.hidden = hidden == NO;
}

#pragma mark - UITextFieldDelegate

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    return [textField resignFirstResponder];
}

#pragma mark - UITextViewDelegate

- (void)textViewDidChange:(UITextView *)textView
{
    [self configurePlaceholder];
}

- (BOOL)textViewShouldEndEditing:(UITextView *)textView
{
    [self configurePlaceholder];
    return [textView resignFirstResponder];
}

#pragma mark - MASStringLoaderDelegate

- (void)loader:(nonnull MASStringLoader *)loader didFailToLoadStringWithError:(nonnull NSError *)error
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self configureActivityIndicatorWithHiddenState:YES];
        MASAlertController *alertController =
            [MASAlertController alertControllerWithTitle:@"Error" message:error.localizedDescription];
        [alertController present];
    });
}

- (void)loader:(nonnull MASStringLoader *)loader didLoadString:(nonnull NSString *)string
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self configureActivityIndicatorWithHiddenState:YES];
        self.environmentTextView.text = string;
        [self configurePlaceholder];
    });
}

@end
