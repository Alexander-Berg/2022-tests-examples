package api

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"github.com/YandexClassifieds/vertis-admin-bot/bot_config"
	"github.com/YandexClassifieds/vertis-admin-bot/duty"
	"github.com/YandexClassifieds/vertis-admin-bot/logger"
)

var (
	ClStates      bot_config.ClassifiedStates
	DutyPersons   []bot_config.DutyPerson
	AdminDutyInfo map[string]duty.DutyInfo
)

func setVars() {
	vertisPerson := bot_config.DutyPerson{557547, 36842633, "spooner@", "spooner"}
	autoruPerson := bot_config.DutyPerson{554460, 127911338, "b0fur@", "b0fur"}
	DutyPersons = []bot_config.DutyPerson{
		vertisPerson,
		autoruPerson,
	}

	adminState := bot_config.State{
		Name:           "vertis",
		DutyPersonsIDs: []int{36842633, 127911338},
		GroupPhone:     1585,
		CalendarInfo: bot_config.CalendarInfo{
			PrivateToken:      "dc8f9a3efcd94095ab134e6e88be8650e2174d21",
			RobotEmail:        "spooner@yandex-team.ru",
			CreateEvents:      true,
			ReadDutiesFromCal: false,
		},
		DutyFile: "",
	}
	adminClass := bot_config.StateClass{
		States: []bot_config.State{
			adminState,
		},
		Default: true,
		Name:    "vertis",
	}
	ClStates = bot_config.ClassifiedStates{
		"admin": adminClass,
	}

	AdminDutyInfo = map[string]duty.DutyInfo{
		"vertis": {
			OccuredError: nil,
			Login:        "spooner@",
			TgLogin:      "spooner",
		},
		"autoru": {
			OccuredError: nil,
			Login:        "b0fur@",
			TgLogin:      "b0fur",
		},
	}
}

func mockGetAllDuties(states []bot_config.State, dutyPersons duty.MappedDutyPersons) map[string]duty.DutyInfo {
	_ = states
	_ = dutyPersons
	return AdminDutyInfo
}

func testV1GetAdmin(apiPort string, adminType string) (string, int, error) {
	resp, err := http.Get("http://127.0.0.1:" + apiPort + "/v1/admin/" + adminType)
	if err != nil {
		return "", 000, err
	}

	var adminData AdminBotResponse
	var adminId string
	adminDataRaw, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", resp.StatusCode, fmt.Errorf("Cannot read response:\t%v", err)
	}

	err = json.Unmarshal(adminDataRaw, &adminData)
	if err != nil {
		adminId = string(adminDataRaw)
		logger.Logger.Infof("Response is not a JSON, passing raw response string:\t%s\nError:\t%v", adminId, err)
	} else {
		adminId = adminData.Login
	}
	return adminId, resp.StatusCode, nil
}

func testV1GetPing(apiPort string) (int, error) {
	resp, err := http.Get("http://127.0.0.1:" + apiPort + "/v1/ping")
	if err != nil {
		return 000, err
	}
	return resp.StatusCode, nil
}

func TestInit(t *testing.T) {
	setVars()
	logger.Init()
	dutyGetAllDuties = mockGetAllDuties
	go Init(DutyPersons, ClStates)
	time.Sleep(1 * time.Second)

	// Test /v1/ping healthcheck
	code, err := testV1GetPing(apiPort)
	if err != nil {
		logger.Logger.Fatalf("FAILED getting response for ping:\t%v", err)
	}
	if code != 200 {
		logger.Logger.Fatalf("FAILED response code for ping:\t%d", code)
	} else {
		logger.Logger.Infof("CORRECT response for ping:\t%d", code)
	}

	// Test for valid admin values
	for key, value := range AdminDutyInfo {
		resp, code, err := testV1GetAdmin(apiPort, key)
		if err != nil {
			logger.Logger.Fatalf("FAILED getting response for admin type <%s>:\t%v", key, err)
		}
		if code != 200 {
			logger.Logger.Fatalf("FAILED response code for admin type <%s>:\t%d", key, code)
		} else {
			if resp == value.Login {
				logger.Logger.Infof("CORRECT response for admin type <%s>:\t%s", key, resp)
			} else {
				logger.Logger.Fatalf("FAILED response for admin type <%s>:\t%s, should be:\t%s", key, resp, value.Login)
			}
		}
	}

	// Test for valid error for missing admin
	key := "fakeAdmin#123"
	_, code, err = testV1GetAdmin(apiPort, key)
	if err != nil {
		logger.Logger.Fatalf("FAILED getting response for admin type <%s>:\t%v", key, err)
	} else {
		if code == 404 {
			logger.Logger.Infof("CORRECT response code <%d> for missing admin <%s>", code, key)
		} else {
			logger.Logger.Fatalf("FAILED response code <%d> for missing admin <%s>", code, key)
		}
	}
}
