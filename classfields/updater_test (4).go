package users_config_loader

import (
	"testing"
	uc "github.com/YandexClassifieds/vertis-announcement-bot/users_config"
	sc "github.com/YandexClassifieds/vertis-announcement-bot/staff_client"
)

func checkConformUsersConfigWithEmployeesResult(t *testing.T, config uc.UsersConfig, employees map[string]sc.Person, expectedConfig uc.UsersConfig, expectedErr error) {
	newConfig := conformUsersConfigWithEmployees(config, employees)

	if !uc.ConfigEquals(newConfig, expectedConfig) {
		t.Errorf("Failed to update config '%v' with employees '%v': got config '%v', expected '%v'\n",
			config, employees, newConfig, expectedConfig)
	}
}

func TestConformUsersConfigWithEmployees(t *testing.T) {
	config1 := uc.UsersConfig{}
	config1.AddUser("alex234", "alex34", []string{"event1", "event2", "event3"}) // with new telegram login
	config1.AddUserWithId("nastia143", "nastia143", []string{"event1", "event3"}, 34) // no change
	config1.AddUser("slava", "slava23", []string{"event2"}) // dismissed
	config1.AddUser("masha34", "masha34444", []string{}) // deleted telegram login

	employees1 := make(map[string]sc.Person, 0)
	employees1["alex34"] = sc.Person{Login: "alex34", Accounts: []sc.Account{{Type: "telegram", Value: "alex888"}}}
	employees1["nastia143"] = sc.Person{Login: "nastia143", Accounts: []sc.Account{{Type: "telegram", Value: "nastia143"}}}
	employees1["masha34444"] = sc.Person{Login: "masha34444", Accounts: []sc.Account{{Type: "personal_email", Value: "mascha5@yandex.ru"}}}

	expectedConfig1 := uc.UsersConfig{}
	expectedConfig1.AddUser("alex888", "alex34", []string{"event1", "event2", "event3"})
	expectedConfig1.AddUserWithId("nastia143", "nastia143", []string{"event1", "event3"}, 34)
	checkConformUsersConfigWithEmployeesResult(t, config1, employees1, expectedConfig1, nil)
}
