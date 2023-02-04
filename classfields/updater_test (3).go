package users_config_loader

import (
	"testing"

	"github.com/YandexClassifieds/vertis-admin-bot/person"
	uc "github.com/YandexClassifieds/vertis-admin-bot/users_config"
)

func checkConformUsersConfigWithEmployeesResult(t *testing.T, config uc.UsersConfig, employees map[string]person.Person, expectedConfig uc.UsersConfig, expectedErr error) {
	newConfig := conformUsersConfigWithEmployees(config, employees)

	if !uc.ConfigEquals(newConfig, expectedConfig) {
		t.Errorf("Failed to update config '%v' with employees '%v': got config '%v', expected '%v'\n",
			config, employees, newConfig, expectedConfig)
	}
}

func TestConformUsersConfigWithEmployees(t *testing.T) {
	config1 := uc.UsersConfig{}
	config1.AddUser("alex234", "alex34")                // with new telegram login
	config1.AddUserWithId("nastia143", "nastia143", 34) // no change
	config1.AddUser("slava", "slava23")                 // dismissed
	config1.AddUser("masha34", "masha34444")            // deleted telegram login

	employees1 := make(map[string]person.Person, 0)
	employees1["alex34"] = person.Person{Login: "alex34", Accounts: []person.Account{{Type: "telegram", Value: "alex888"}}}
	employees1["nastia143"] = person.Person{Login: "nastia143", Accounts: []person.Account{{Type: "telegram", Value: "nastia143"}}}
	employees1["masha34444"] = person.Person{Login: "masha34444", Accounts: []person.Account{{Type: "personal_email", Value: "mascha5@yandex.ru"}}}

	expectedConfig1 := uc.UsersConfig{}
	expectedConfig1.AddUser("alex888", "alex34")
	expectedConfig1.AddUserWithId("nastia143", "nastia143", 34)
	checkConformUsersConfigWithEmployeesResult(t, config1, employees1, expectedConfig1, nil)
}
