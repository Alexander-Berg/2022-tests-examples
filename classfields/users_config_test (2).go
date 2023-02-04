package users_config

import (
	"errors"
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

func checkUpdateUserResult(t *testing.T, config UsersConfig, telegramLogin string, eventsToSubscribe []string,
	eventsToUnsubscribe []string, telegramId int64, expectedConfig UsersConfig, expectedErr error) {
	err := config.UpdateUser(telegramLogin, eventsToSubscribe, eventsToUnsubscribe, telegramId)

	// If have different values (object vs nil) or are not nil and have different messages
	if ((err == nil) != (expectedErr == nil)) || (err != nil && expectedErr != nil && err.Error() != expectedErr.Error()) {
		t.Errorf("Failed to update user '%s' in config, got error '%v', expected '%v'\n", telegramLogin, err, expectedErr)
	}

	if !ConfigEquals(config, expectedConfig) {
		t.Errorf("Failed to update user '%s' in config, got config '%v', expected '%v'\n", telegramLogin, config, expectedConfig)
	}
}

func TestUpdateUser(t *testing.T) {
	config1 := UsersConfig{}
	config1.AddUser("nastia143", "nastia143", []string{"event1", "event3"})
	expectedConfig1 := UsersConfig{}
	expectedConfig1.AddUserWithId("nastia143", "nastia143", []string{"event1", "event2"}, 1)
	checkUpdateUserResult(t, config1, "nastia143", []string{"event2"}, []string{"event3"}, 1, expectedConfig1, nil)

	config2 := UsersConfig{}
	config2.AddUser("alex234", "staff", []string{"event1", "event2", "event3"})
	config2.AddUser("nastia143", "nastia143", []string{"event1", "event3"})
	expectedConfig2 := UsersConfig{}
	expectedConfig2.AddUserWithId("alex234", "staff", []string{"event1", "event3", "event4", "event5"}, 34)
	expectedConfig2.AddUser("nastia143", "nastia143", []string{"event1", "event3"})
	checkUpdateUserResult(t, config2, "alex234", []string{"event4", "event5"}, []string{"event2"}, 34, expectedConfig2, nil)

	checkUpdateUserResult(t, config2, "notemployee", []string{"event4", "event5"}, []string{"event2"},
		34, expectedConfig2, errors.New("User 'notemployee' is not Yandex Vertis employee. Please allow 1 hour until new users will be pulled from Staff"))
}

func checkGetAllActiveUsersResult(t *testing.T, config UsersConfig, event string, expectedUsers map[int64]User) {
	users := config.GetAllActiveUsers()
	if !userMapsEquals(users, expectedUsers) {
		t.Errorf("Failed to get all active users, got '%v', expected '%v'\n", users, expectedUsers)
	}
}

func TestGetAllActiveUsers(t *testing.T) {
	config := UsersConfig{}

	config.AddUserWithId("nastia143", "nastia143", []string{"event1", "event3"}, 1)
	config.AddUser("spooner", "spooner", []string{})
	config.AddUserWithId("wf1nder", "wf1nder", []string{"event3", "event4", "event5"}, 3)
	config.AddUserWithId("TheSpbra1n", "kasev", []string{"event5"}, 4)
	config.AddUser("someoneelse", "someine", []string{})

	checkGetAllActiveUsersResult(t, config, "event1", map[int64]User{
		1: {TelegramLogin: "nastia143", StaffLogin: "nastia143"},
		3: {TelegramLogin: "wf1nder", StaffLogin: "wf1nder"},
		4: {TelegramLogin: "TheSpbra1n", StaffLogin: "kasev"}})
}

func checkGetSubscribedForEventUsersResult(t *testing.T, config UsersConfig, event string, expectedSubscribedUsers map[int64]User) {
	subscribedUsers := config.GetSubscribedForEventUsers(event)
	if !userMapsEquals(subscribedUsers, expectedSubscribedUsers) {
		t.Errorf("Failed to get subscribed for event '%s' users, got '%v', expected '%v'\n", event, subscribedUsers, expectedSubscribedUsers)
	}
}

func TestGetSubscribedForEventUsers(t *testing.T) {
	config := UsersConfig{}

	config.AddUserWithId("nastia143", "nastia143", []string{"event1", "event3"}, 1)
	config.AddUserWithId("spooner", "spooner", []string{"event1", "event2"}, 2)
	config.AddUserWithId("wf1nder", "wf1nder", []string{"event3", "event4", "event5"}, 3)
	config.AddUserWithId("TheSpbra1n", "kasev", []string{"event5"}, 4)

	checkGetSubscribedForEventUsersResult(t, config, "event1", map[int64]User{
		1: {TelegramLogin: "nastia143", StaffLogin: "nastia143"},
		2: {TelegramLogin: "spooner", StaffLogin: "spooner"}})

	checkGetSubscribedForEventUsersResult(t, config, "event2", map[int64]User{
		2: {TelegramLogin: "spooner", StaffLogin: "spooner"}})

	checkGetSubscribedForEventUsersResult(t, config, "event3", map[int64]User{
		1: {TelegramLogin: "nastia143", StaffLogin: "nastia143"},
		3: {TelegramLogin: "wf1nder", StaffLogin: "wf1nder"}})

	checkGetSubscribedForEventUsersResult(t, config, "event4", map[int64]User{
		3: {TelegramLogin: "wf1nder", StaffLogin: "wf1nder"}})

	checkGetSubscribedForEventUsersResult(t, config, "event5", map[int64]User{
		3: {TelegramLogin: "wf1nder", StaffLogin: "wf1nder"},
		4: {TelegramLogin: "TheSpbra1n", StaffLogin: "kasev"}})
}

func checkSubscribedToAllEvents(t *testing.T, user User, availableEvents []string, expectedResult bool) {
	result := user.SubscribedToAllEvents(availableEvents)

	if result != expectedResult {
		t.Errorf("Failed to check whether user '%v' is subscribed for all events: got %v, expected %v\n", user, result, expectedResult)
	}
}

func checkIsSubscribedToAnyEvents(t *testing.T, user User, availableEvents []string, expectedResult bool) {
	result := user.IsSubscribedToAnyEvents(availableEvents)

	if result != expectedResult {
		t.Errorf("Failed to check whether user '%v' is subscribed for any events: got %v, expected %v\n", user, result, expectedResult)
	}
}

func TestSubscribedToAllEvents(t *testing.T) {
	availableEvents := []string{"a", "b", "c", "d"}

	user1 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"a", "b", "c", "d"}}
	checkSubscribedToAllEvents(t, user1, availableEvents, true)

	user3 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"a", "d", "b", "c"}}
	checkSubscribedToAllEvents(t, user3, availableEvents, true)

	user4 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"a", "c"}}
	checkSubscribedToAllEvents(t, user4, availableEvents, false)

	user5 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"t"}}
	checkSubscribedToAllEvents(t, user5, availableEvents, false)
}

func TestIsNotSubscribedToAnyEvents(t *testing.T) {
	availableEvents := []string{"a", "b", "c", "d"}

	user1 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"a", "b", "c", "d"}}
	checkIsSubscribedToAnyEvents(t, user1, availableEvents, true)

	user2 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"t"}}
	checkIsSubscribedToAnyEvents(t, user2, availableEvents, false)

	user3 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{}}
	checkIsSubscribedToAnyEvents(t, user3, availableEvents, false)

	user4 := User{TelegramLogin: "nastia143", StaffLogin: "nastia143", TelegramId: 1, SubscribedEvents: []string{"r", "e", "a"}}
	checkIsSubscribedToAnyEvents(t, user4, availableEvents, true)
}
