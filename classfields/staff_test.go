package staff

import (
	"os"
	"testing"

	"github.com/YandexClassifieds/vertis-admin-bot/logger"
	"github.com/YandexClassifieds/vertis-admin-bot/person"
	"github.com/stretchr/testify/assert"
)

func checkFindDiffResuls(t *testing.T, cachedPersons map[string]*person.Person, persons map[string]*person.Person, expectedDifference []*person.Person) {
	difference := findDiff(cachedPersons, persons)

	assert.ElementsMatch(t, difference, expectedDifference)
}

func TestFindDiff(t *testing.T) {
	cachedPersons1 := map[string]*person.Person{
		"dismissed1": {
			Login: "dismissed1",
		},
		"nastia143": {
			Login: "nastia143",
		},
		"spooner": {
			Login: "spooner",
		},
		"kasev": {
			Login: "kasev",
		},
		"dismissed2": {
			Login: "dismissed2",
		},
		"wf1nder": {
			Login: "wf1nder",
		},
		"dismissed3": {
			Login: "dismissed3",
		},
		"dismissed4": {
			Login: "dismissed4",
		},
	}
	staffPersons1 := map[string]*person.Person{
		"nastia143": {
			Login: "nastia143",
		},
		"spooner": {
			Login: "spooner",
		},
		"kasev": {
			Login: "kasev",
		},
		"wf1nder": {
			Login: "wf1nder",
		},
	}
	expectedDifference1 := []*person.Person{
		{
			Login: "dismissed1",
		},
		{
			Login: "dismissed2",
		},
		{
			Login: "dismissed3",
		},
		{
			Login: "dismissed4",
		},
	}
	checkFindDiffResuls(t, cachedPersons1, staffPersons1, expectedDifference1)
}

func TestValidateTelegram(t *testing.T) {
	logger.Init()
	token := os.Getenv("OAUTH_TOKEN")

	testLogins := []string{
		"spooner",
		"b0fur",
		"TheSpbra1n",
	}
	for _, login := range testLogins {
		err := ValidateTelegram(login, token)
		if err != nil {
			t.Errorf("Error in ValidateTelegram for login: %s: %v", login, err)
		}
	}

	testBadLogins := []string{
		"spooner123",
		"nagibator666",
		"your_mama",
	}

	for _, login := range testBadLogins {
		err := ValidateTelegram(login, token)
		if err == nil {
			t.Errorf("Error in ValidateTelegram for bad login: %s: %v", login, err)
		}
	}
}
