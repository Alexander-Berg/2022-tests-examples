package rules

import (
	"context"
	"path/filepath"
	"testing"

	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/cucumber/godog"

	"a.yandex-team.ru/payplatform/fes/core/schemas"
)

var featurePathBase = "payplatform/fes/fes/pkg/rules/features"

type testState struct {
	Event schemas.Event
	Skip  bool
	Cause string
}

var TestState testState

func defaultEvent() schemas.Event {
	return schemas.Event{
		Payment: schemas.Payment{
			Currency:     "RUB",
			IsAutoRefund: false,
		},
		Firm:      schemas.Firm{RegionID: 225},
		ServiceID: 0,
	}
}

func regularEvent() {}

func bindingEvent() {
	TestState.Event.Payment.IsAutoRefund = true
}

func eventWithCurrency(currency string) {
	TestState.Event.Payment.Currency = currency
}

func eventWithRegion(regionID int) {
	TestState.Event.Firm.RegionID = regionID
}

func weCheckEventAgainstTheRules() {
	TestState.Skip, TestState.Cause = TrustRuleSet.CheckReceiptNecessity(TestState.Event)
}

func eventIsNotSkipped() error {
	if TestState.Skip {
		return xerrors.Errorf(`event should not be skipped, but it is with reason "%s"`, TestState.Cause)
	}
	return nil
}

func eventIsSkippedWithReason(cause string) error {
	if !TestState.Skip {
		return xerrors.Errorf(`event should be skipped, but it is not with reason "%s"`, TestState.Cause)
	}
	if TestState.Cause != cause {
		return xerrors.Errorf(`event skipping reason should be "%s", but it is "%s"`, cause, TestState.Cause)
	}
	return nil
}

type stepOption func(sc *godog.ScenarioContext)

func NewTestSuite(t *testing.T, featureFileName string, stepOptions ...stepOption) godog.TestSuite {
	return godog.TestSuite{
		ScenarioInitializer: func(sc *godog.ScenarioContext) {
			sc.Before(func(ctx context.Context, _ *godog.Scenario) (context.Context, error) {
				TestState = testState{Event: defaultEvent()}
				return ctx, nil
			})

			for _, op := range stepOptions {
				op(sc)
			}
		},
		Options: &godog.Options{
			Format:   "pretty",
			Paths:    []string{yatest.SourcePath(filepath.Join(featurePathBase, featureFileName))},
			TestingT: t, // Testing instance that will run subtests.
		},
	}
}

func withStep(expr, stepFunc interface{}) stepOption {
	return func(sc *godog.ScenarioContext) {
		sc.Step(expr, stepFunc)
	}
}

func withCommonSteps() stepOption {
	return func(sc *godog.ScenarioContext) {
		sc.Step(`^we check event against the rules$`, weCheckEventAgainstTheRules)

		sc.Step(`^event is not skipped$`, eventIsNotSkipped)
		sc.Step(`^event is skipped with reason "(.+)"$`, eventIsSkippedWithReason)

		sc.Step(`^regular event$`, regularEvent)
		sc.Step(`^binding payment event$`, bindingEvent)
		sc.Step(`^event with currency (\w+)$`, eventWithCurrency)
		sc.Step(`^event with region id (\d+)$`, eventWithRegion)
	}
}

func TestCommonRules(t *testing.T) {
	suite := NewTestSuite(t, "common.feature", withCommonSteps())

	if suite.Run() != 0 {
		t.Fatal("non-zero status returned, failed to run feature tests")
	}
}
