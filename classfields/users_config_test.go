package users_config

import (
	"testing"
)

func userMapsEquals(users1 map[int64]User, users2 map[int64]User) bool {
	if users1 == nil && users2 == nil {
		return true
	}

	if users1 == nil || users2 == nil {
		return false
	}

	if len(users1) != len(users2) {
		return false
	}

	for telegramLogin, user1 := range users1 {
		if user2, ok := users2[telegramLogin]; !ok || user1.TelegramLogin != user2.TelegramLogin || user1.StaffLogin != user2.StaffLogin {
			return false
		}
	}

	return true
}

func checkGetAllActiveUsersResult(t *testing.T, config UsersConfig, event string, expectedUsers map[int64]User) {
	users := config.GetAllActiveUsers()
	if !userMapsEquals(users, expectedUsers) {
		t.Errorf("Failed to get all active users in event '%v' , got '%v', expected '%v'\n", event, users, expectedUsers)
	}
}

func TestGetAllActiveUsers(t *testing.T) {
	config := UsersConfig{}

	config.AddUserWithId("nastia143", "nastia143", 1)
	config.AddUser("spooner", "spooner")
	config.AddUserWithId("wf1nder", "wf1nder", 3)
	config.AddUserWithId("TheSpbra1n", "kasev", 4)
	config.AddUser("someoneelse", "someine")

	checkGetAllActiveUsersResult(t, config, "event1", map[int64]User{
		1: {TelegramLogin: "nastia143", StaffLogin: "nastia143"},
		3: {TelegramLogin: "wf1nder", StaffLogin: "wf1nder"},
		4: {TelegramLogin: "TheSpbra1n", StaffLogin: "kasev"}})
}
