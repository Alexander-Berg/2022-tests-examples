package pipeline

import (
	"fmt"
	"strings"
	"testing"
	"time"

	"github.com/YandexClassifieds/drills-helper/app/downtime/juggler"
	"github.com/stretchr/testify/assert"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/pipeline"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type testCase struct {
	action             string
	layer              string
	choice             string
	switchSwitchCount  int
	deployEnableCount  int
	deployDisableCount int
	lbOpenCount        int
	lbCloseCount       int
	dtSetDowntimeCount int
	walleEnableCount   int
	walleDisableCount  int
	hbfStartCount      int
	hbfStopCount       int
	cmeHbfCloseCount   int
	cmeHbfOpenCount    int
	cmsEnableCount     int
	cmsDisableCount    int
}

var (
	dc = common.DCSas

	isAnswerYesTestCases = map[string]bool{
		"yes": true,
		"y":   true,
		"no":  false,
		"n":   false,
	}

	doSwitchActionsTC = map[string]testCase{
		"yes": {
			switchSwitchCount: 1,
		},
		"y": {
			switchSwitchCount: 1,
		},
		"no": {},
		"n":  {},
	}

	doDeployActionsTCOpen = map[string]testCase{
		"prodYes": {
			action:            "enable",
			choice:            "yes",
			layer:             common.LayerProd,
			deployEnableCount: 1,
		},
		"prodY": {
			action:            "enable",
			choice:            "y",
			layer:             common.LayerProd,
			deployEnableCount: 1,
		},
		"prodNo": {
			action: "open",
			choice: "no",
			layer:  common.LayerProd,
		},
		"prodN": {
			action: "open",
			choice: "n",
			layer:  common.LayerProd,
		},
		"allYes": {
			action:            "enable",
			choice:            "yes",
			layer:             "all",
			deployEnableCount: 2,
		},
		"allY": {
			action:            "enable",
			choice:            "y",
			layer:             "all",
			deployEnableCount: 2,
		},
		"allNo": {
			action: "open",
			choice: "no",
			layer:  "all",
		},
		"allN": {
			action: "open",
			choice: "n",
			layer:  "all",
		},
	}

	doDeployActionsTCClose = map[string]testCase{
		"prodYes": {
			action:             "disable",
			choice:             "yes",
			layer:              common.LayerProd,
			deployDisableCount: 1,
		},
		"prodY": {
			action:             "disable",
			choice:             "y",
			layer:              common.LayerProd,
			deployDisableCount: 1,
		},
		"prodNo": {
			action: "open",
			choice: "no",
			layer:  common.LayerProd,
		},
		"prodN": {
			action: "open",
			choice: "n",
			layer:  common.LayerProd,
		},
		"allYes": {
			action:             "disable",
			choice:             "yes",
			layer:              "all",
			deployDisableCount: 2,
		},
		"allY": {
			action:             "disable",
			choice:             "y",
			layer:              "all",
			deployDisableCount: 2,
		},
		"allNo": {
			action: "open",
			choice: "no",
			layer:  "all",
		},
		"allN": {
			action: "open",
			choice: "n",
			layer:  "all",
		},
	}

	doLBActionsTCOpen = map[string]testCase{
		"yes": {
			action:      "open",
			lbOpenCount: 1,
		},
		"y": {
			action:      "open",
			lbOpenCount: 1,
		},
		"no": {
			action: "open",
		},
		"n": {
			action: "open",
		},
	}

	doLBActionsTCClose = map[string]testCase{
		"yes": {
			action:       "close",
			lbCloseCount: 1,
		},
		"y": {
			action:       "close",
			lbCloseCount: 1,
		},
		"no": {
			action: "close",
		},
		"n": {
			action: "close",
		},
	}

	doWalleActionsTCEnable = map[string]testCase{
		"yes": {
			action:           "enable",
			walleEnableCount: 1,
		},
		"y": {
			action:           "enable",
			walleEnableCount: 1,
		},
		"no": {
			action: "enable",
		},
		"n": {
			action: "enable",
		},
	}

	doWalleActionsTCDisable = map[string]testCase{
		"yes": {
			action:            "disable",
			walleDisableCount: 1,
		},
		"y": {
			action:            "disable",
			walleDisableCount: 1,
		},
		"no": {
			action: "disable",
		},
		"n": {
			action: "disable",
		},
	}

	doCMSActionsTCEnable = map[string]testCase{
		"yes": {
			action:         "enable",
			cmsEnableCount: 1,
		},
		"y": {
			action:         "enable",
			cmsEnableCount: 1,
		},
		"no": {
			action: "enable",
		},
		"n": {
			action: "enable",
		},
	}

	doCMSActionsTCDisable = map[string]testCase{
		"yes": {
			action:          "disable",
			cmsDisableCount: 1,
		},
		"y": {
			action:          "disable",
			cmsDisableCount: 1,
		},
		"no": {
			action: "disable",
		},
		"n": {
			action: "disable",
		},
	}

	doHBFActionsTCStart = map[string]testCase{
		"yes": {
			hbfStartCount: 1,
		},
		"y": {
			hbfStartCount: 1,
		},
		"no": {},
		"n":  {},
	}

	doHBFActionsTCStop = map[string]testCase{
		"yes": {
			hbfStopCount: 1,
		},
		"y": {
			hbfStopCount: 1,
		},
		"no": {},
		"n":  {},
	}

	doCMEHBFActionsTCClose = map[string]testCase{
		"yes": {
			cmeHbfCloseCount: 1,
		},
		"y": {
			cmeHbfCloseCount: 1,
		},
		"no": {},
		"n":  {},
	}

	doCMEHBFActionsTCOpen = map[string]testCase{
		"yes": {
			cmeHbfOpenCount: 1,
		},
		"y": {
			cmeHbfOpenCount: 1,
		},
		"no": {},
		"n":  {},
	}

	doDTActionsTC = map[string]testCase{
		"prodYes": {
			choice:             "yes",
			layer:              common.LayerProd,
			dtSetDowntimeCount: 1,
		},
		"prodY": {
			choice:             "y",
			layer:              common.LayerProd,
			dtSetDowntimeCount: 1,
		},
		"prodNo": {
			choice: "no",
			layer:  common.LayerProd,
		},
		"prodN": {
			choice: "n",
			layer:  common.LayerProd,
		},
		"allYes": {
			choice:             "yes",
			layer:              "all",
			dtSetDowntimeCount: 2,
		},
		"allY": {
			choice:             "y",
			layer:              "all",
			dtSetDowntimeCount: 2,
		},
		"allNo": {
			choice: "no",
			layer:  "all",
		},
		"allN": {
			choice: "n",
			layer:  "all",
		},
	}
)

func Test_isAnswerYes(t *testing.T) {
	for choice, expected := range isAnswerYesTestCases {
		t.Run(choice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", choice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

			closeCmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			result, err := closeCmd.isAnswerYes(message)
			require.NoError(t, err)
			require.Equal(t, expected, result)
		})
	}
}

func Test_doSwitchAction(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doSwitchActionsTC {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doSwitchAction(message, "teamcity", "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doDeployAction_onOpen(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for name, tc := range doDeployActionsTCOpen {
		t.Run(name, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", tc.choice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doDeployAction(message, tc.action, tc.layer, "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doDeployAction_onClose(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for name, tc := range doDeployActionsTCClose {
		t.Run(name, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", tc.choice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doDeployAction(message, tc.action, tc.layer, "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doLBAction_onOpen(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doLBActionsTCOpen {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doLBAction(message, tc.action, "ingress", "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doLBAction_onClose(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doLBActionsTCClose {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doLBAction(message, tc.action, "ingress", "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doDowntimeAction(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, 55*time.Minute+juggler.FlappingDelay)

	for name, tc := range doDTActionsTC {
		t.Run(name, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", tc.choice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doDowntimeAction(message, tc.layer, "sas", time.Now().Add(-5*time.Minute), time.Hour)
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doWalleAction_onOpen(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doWalleActionsTCEnable {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doWalleAction(message, tc.action, tc.layer)
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doWalleAction_onClose(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doWalleActionsTCDisable {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doWalleAction(message, tc.action, tc.layer)
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock,
				hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doCMSAction_onOpen(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doCMSActionsTCEnable {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doCMSAction(message, tc.action, tc.layer, dc)
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doCMSAction_onClose(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doCMSActionsTCDisable {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doCMSAction(message, tc.action, tc.layer, dc)
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock,
				hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doHBFAction_onClose(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, 55*time.Minute)

	for userChoice, tc := range doHBFActionsTCStart {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewCloseCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doHBFAction(message, "start", "sas", time.Now().Add(-5*time.Minute), time.Hour, "VERTISADMIN-111")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doHBFAction_onOpen(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doHBFActionsTCStop {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doHBFAction(message, "stop", "sas", time.Now(), time.Hour, "")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock,
				hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doCMEHBFAction_onClose(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doCMEHBFActionsTCClose {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doCMEHBFAction(message, "close", "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func Test_doCMEHBFAction_onOpen(t *testing.T) {
	switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock := makeMocks(t, time.Hour)

	for userChoice, tc := range doCMEHBFActionsTCOpen {
		t.Run(userChoice, func(t *testing.T) {
			userInput := strings.NewReader(fmt.Sprintf("%s\n", userChoice))
			stdout := &strings.Builder{}
			message := "Do you want to continue?"

			cmd := NewOpenCmd(logrus.New(), switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, userInput, stdout)
			err := cmd.doCMEHBFAction(message, "open", "sas")
			require.NoError(t, err)
			checkNumberOfCalls(t, switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock,
				walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock, tc)
		})
	}
}

func makeSwitchActionsMock() *pipeline.ISwitchActions {
	switchActionsMock := &pipeline.ISwitchActions{}
	switchActionsMock.On("Switch", mock.Anything, mock.Anything).Return(nil)

	return switchActionsMock
}

func makeDeployActionsMock() *pipeline.IDeployActions {
	deployActionsMock := &pipeline.IDeployActions{}
	deployActionsMock.On("Enable", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	deployActionsMock.On("Disable", mock.Anything, mock.Anything, mock.Anything).Return(nil)

	return deployActionsMock
}

func makeLBActionsMock() *pipeline.ILBActions {
	lbActionsMock := &pipeline.ILBActions{}
	lbActionsMock.On("Open", mock.Anything, mock.Anything).Return(nil)
	lbActionsMock.On("Close", mock.Anything, mock.Anything).Return(nil)

	return lbActionsMock
}

func makeDowntimeActionsMock(t *testing.T, expectedDuration time.Duration) *pipeline.IDowntimeActions {
	resultF := func(_ string, _ string, duration time.Duration) error {
		assert.InDelta(t, expectedDuration, duration, float64(time.Minute.Nanoseconds()))
		return nil
	}

	dtActionsMock := &pipeline.IDowntimeActions{}
	dtActionsMock.On("SetDowntime", mock.Anything, mock.Anything, mock.Anything).Return(resultF)

	return dtActionsMock
}

func makeWalleActionsMock() *pipeline.IWalleActions {
	wMock := &pipeline.IWalleActions{}
	wMock.On("Enable", mock.Anything).Return(nil)
	wMock.On("Disable", mock.Anything).Return(nil)

	return wMock
}

func makeHBFActionsMock(t *testing.T, expectedDuration time.Duration) *pipeline.IHBFActions {
	startResultF := func(_ string, duration time.Duration, _ string) error {
		assert.InDelta(t, expectedDuration-15*time.Minute, duration, float64(time.Minute.Nanoseconds()))
		return nil
	}

	hMock := &pipeline.IHBFActions{}
	hMock.On("Stop", mock.AnythingOfType("string")).Return(nil)
	hMock.On("Start", mock.AnythingOfType("string"), mock.AnythingOfType("time.Duration"), mock.AnythingOfType("string")).Return(startResultF)

	return hMock
}

func makeCMEHBFActionsMock() *pipeline.ICMEHBFActions {
	hMock := &pipeline.ICMEHBFActions{}
	hMock.On("Close", mock.AnythingOfType("string")).Return(nil)
	hMock.On("Open", mock.AnythingOfType("string")).Return(nil)

	return hMock
}

func makeCMSActionsMock() *pipeline.ICMSActions {
	wMock := &pipeline.ICMSActions{}
	wMock.On("Enable", mock.Anything, mock.Anything).Return(nil)
	wMock.On("Disable", mock.Anything, mock.Anything).Return(nil)

	return wMock
}

func makeMocks(t *testing.T, expectedDuration time.Duration) (*pipeline.ISwitchActions, *pipeline.IDeployActions,
	*pipeline.ILBActions, *pipeline.IDowntimeActions, *pipeline.IWalleActions, *pipeline.IHBFActions,
	*pipeline.ICMEHBFActions, *pipeline.ICMSActions) {
	switchActionsMock := makeSwitchActionsMock()
	deployActionsMock := makeDeployActionsMock()
	lbActionsMock := makeLBActionsMock()
	dtActionsMock := makeDowntimeActionsMock(t, expectedDuration)
	walleActionsMock := makeWalleActionsMock()
	hbfActionsMock := makeHBFActionsMock(t, expectedDuration)
	cmeHbfActionsMock := makeCMEHBFActionsMock()
	cmsActionsMock := makeCMSActionsMock()

	return switchActionsMock, deployActionsMock, lbActionsMock, dtActionsMock, walleActionsMock, hbfActionsMock, cmeHbfActionsMock, cmsActionsMock
}

func checkNumberOfCalls(t *testing.T,
	switchActionsMock *pipeline.ISwitchActions,
	deployActionsMock *pipeline.IDeployActions,
	lbActionsMock *pipeline.ILBActions,
	dtActionsMock *pipeline.IDowntimeActions,
	walleActionsMock *pipeline.IWalleActions,
	hbfActionsMock *pipeline.IHBFActions,
	cmeHbfActionsMock *pipeline.ICMEHBFActions,
	cmsActionsMock *pipeline.ICMSActions,
	tc testCase) {

	switchActionsMock.Mock.AssertNumberOfCalls(t, "Switch", tc.switchSwitchCount)

	deployActionsMock.Mock.AssertNumberOfCalls(t, "Enable", tc.deployEnableCount)
	deployActionsMock.Mock.AssertNumberOfCalls(t, "Disable", tc.deployDisableCount)

	lbActionsMock.Mock.AssertNumberOfCalls(t, "Open", tc.lbOpenCount)
	lbActionsMock.Mock.AssertNumberOfCalls(t, "Close", tc.lbCloseCount)

	dtActionsMock.Mock.AssertNumberOfCalls(t, "SetDowntime", tc.dtSetDowntimeCount)

	walleActionsMock.Mock.AssertNumberOfCalls(t, "Enable", tc.walleEnableCount)
	walleActionsMock.Mock.AssertNumberOfCalls(t, "Disable", tc.walleDisableCount)

	hbfActionsMock.Mock.AssertNumberOfCalls(t, "Start", tc.hbfStartCount)
	hbfActionsMock.Mock.AssertNumberOfCalls(t, "Stop", tc.hbfStopCount)

	cmeHbfActionsMock.Mock.AssertNumberOfCalls(t, "Open", tc.cmeHbfOpenCount)
	cmeHbfActionsMock.Mock.AssertNumberOfCalls(t, "Close", tc.cmeHbfCloseCount)

	cmsActionsMock.Mock.AssertNumberOfCalls(t, "Enable", tc.cmsEnableCount)
	cmsActionsMock.Mock.AssertNumberOfCalls(t, "Disable", tc.cmsDisableCount)

	// clear calls count for the next test
	switchActionsMock.Calls = []mock.Call{}
	deployActionsMock.Calls = []mock.Call{}
	lbActionsMock.Calls = []mock.Call{}
	dtActionsMock.Calls = []mock.Call{}
	walleActionsMock.Calls = []mock.Call{}
	hbfActionsMock.Calls = []mock.Call{}
	cmeHbfActionsMock.Calls = []mock.Call{}
	cmsActionsMock.Calls = []mock.Call{}
}
