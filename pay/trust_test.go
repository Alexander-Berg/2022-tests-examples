package rules

import (
	"testing"

	"a.yandex-team.ru/payplatform/fes/core/schemas"
)

func eventWithServiceID(serviceID int) {
	TestState.Event.ServiceID = schemas.ServiceID(serviceID)
}

func eventWithTerminalID(terminalID int) {
	TestState.Event.TrustSpecific.TerminalID = terminalID
}

func TestTrustRules(t *testing.T) {
	suite := NewTestSuite(t, "trust.feature",
		withCommonSteps(),
		withStep(`^event with service id (\d+)$`, eventWithServiceID),
		withStep(`^event has service id (\d+)$`, eventWithServiceID),
		withStep(`^event has terminal id (\d+)$`, eventWithTerminalID),
	)

	if suite.Run() != 0 {
		t.Fatal("non-zero status returned, failed to run feature tests")
	}
}
