package main

import (
	vlog "github.com/YandexClassifieds/go-common/i/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/vertis-st-updater/pkg/config"
	"github.com/YandexClassifieds/vertis-st-updater/pkg/duty"
	"github.com/YandexClassifieds/vertis-st-updater/pkg/tracker"
	"testing"
	"time"
)

func TestBot(t *testing.T) {
	var logger vlog.Logger
	logger = vlogrus.New()

	config.Init(logger)

	duty := duty.NewDutyConf()
	duty, err := duty.GetDutyPerson(logger)
	if err != nil {
		t.Errorf("Failed to get duty person: %s", err)
	}

	if duty.CurrentAdmin != "spooner" {
		t.Errorf("Got wrong duty person")
	}

	stConf := tracker.NewConf()
	trekClient := tracker.NewClient(stConf.StartrekApiUrl, stConf.StartrekApiToken, logger)

	issueKey, err := trekClient.CreateUnassignedIssue()
	if err != nil {
		t.Error("Failed to create unassigned issue")
	}

	// Waiting for Startrek to get issue
	var attempt int
	for i := 0; i == 0; {
		attempt += 1
		i, err = trekClient.GetEmptyIssue(issueKey)
		if err != nil {
			t.Errorf("Failed to get empty issue: %s", err)
		}
		if attempt == 10 {
			t.Error("Too many attempts to get empty issue...")
		}
		time.Sleep(2 * time.Second)
	}

	unassignedIssues, err := trekClient.GetIssuesByQuery()
	if err != nil {
		t.Errorf("Cannot get unassigned issues:\n%v", err)
		return
	}

	err = trekClient.SetAssigneeToIssues(unassignedIssues, duty.CurrentAdmin)
	if err != nil {
		t.Errorf("Cannot assign issues:\n%v", err)
	}

	gotAssignee, err := trekClient.GetAssignedIssue(issueKey)
	if err != nil {
		t.Errorf("Failed to get assigned issue: %s", err)
	}
	if gotAssignee != "spooner" {
		t.Errorf("Assigned wrong person: %s", gotAssignee)
	}
}
