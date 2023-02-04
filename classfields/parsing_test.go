package parsing

import (
	"net/url"
	"testing"
)

func stringInBoolMap(str string, m map[string]bool) bool {
	_, ok := m[str]
	return ok
}

func stringInStringMap(str string, m map[string]string) bool {
	_, ok := m[str]
	return ok
}

func testEqualBoolMap(slice1, slice2 map[string]bool) bool {
	if slice1 == nil && slice2 == nil {
		return true
	}

	if slice1 == nil || slice2 == nil {
		return false
	}

	if len(slice1) != len(slice2) {
		return false
	}

	for key1, _ := range slice1 {
		if !stringInBoolMap(key1, slice2) {
			return false
		}
	}
	return true
}

func testEqualStringMap(slice1, slice2 map[string]string) bool {
	if slice1 == nil && slice2 == nil {
		return true
	}

	if slice1 == nil || slice2 == nil {
		return false
	}

	if len(slice1) != len(slice2) {
		return false
	}

	for key1, _ := range slice1 {
		if !stringInStringMap(key1, slice2) {
			return false
		}
	}
	return true
}

func checkGetPullRequestKeyResult(repositoryName string, pullRequestId int, expected string, t *testing.T) {
	got := GetPullRequestKey(repositoryName, pullRequestId)
	if got != expected {
		t.Errorf(
			"Failed to get pull request key for repository name: %s, pull request id: %d, got: %s, expected: %s",
			repositoryName,
			pullRequestId,
			got,
			expected)
	}
}

func checkGetStartrekTicketKeyResult(stringForParse string, expectedKeys map[string]bool, t *testing.T) {
	foundKeys := GetStartrekTicketKeys(stringForParse)

	if !testEqualBoolMap(foundKeys, expectedKeys) {
		t.Errorf(
			"Failed to get startrek ticket for the string '%s', got: %v, expected: %v",
			stringForParse,
			foundKeys,
			expectedKeys)
	}
}

func checkGetStartrekTicketKeyWithLinksResult(stringForParse string, expectedKeys map[string]bool, t *testing.T) {
	foundKeys := GetStartrekTicketKeysWithLinks(stringForParse)

	if !testEqualBoolMap(foundKeys, expectedKeys) {
		t.Errorf(
			"Failed to get startrek ticket link for the string '%s', got: %v, expected: %v",
			stringForParse,
			foundKeys,
			expectedKeys)
	}
}

func checkGetPropertiesFromUrlResult(params url.Values, ignoredProperties []string, expectedResult map[string]string, t *testing.T) {
	result := GetPropertiesFromUrl(params, ignoredProperties)

	if !testEqualStringMap(result, expectedResult) {
		t.Errorf(
			"Failed to get properties for params '%v', ignored properties '%v', got: %v, expected: %v",
			params,
			ignoredProperties,
			result,
			expectedResult)
	}
}

func TestGetStartrekTicketKey(t *testing.T) {
	expectedKeys1 := map[string]bool{"SOMETHING-2334": true, "SALESMAN-115": true}
	checkGetStartrekTicketKeyResult(
		"SALESMAN-115: common pr title SOMETHING-2334",
		expectedKeys1,
		t)

	expectedKeys2 := map[string]bool{"STARTREK-20": true}
	checkGetStartrekTicketKeyResult(
		"Here is ticketSTARTREK-20in the middle of title",
		expectedKeys2,
		t)

	expectedKeys3 := map[string]bool{"HITMANSUP-1089": true, "STARTREK-20": true, "HITMANSUP-1088": true, "STARTREK-767": true}
	checkGetStartrekTicketKeyResult(
		"Many tickets HITMANSUP-1089HITMANSUP-1088 STARTREK-767pull request :!STARTREK-20",
		expectedKeys3,
		t)

	expectedKeys4 := make(map[string]bool)
	checkGetStartrekTicketKeyResult(
		"Pull request with no tickets",
		expectedKeys4,
		t)
}

func TestGetStartrekTicketKeysWithLinks(t *testing.T) {
	expectedKeys1 := map[string]bool{"SOMETHING-2334": true}
	checkGetStartrekTicketKeyWithLinksResult(
		"SALESMAN-115: common pr title [SOMETHING-2334](https://st.yandex-team.ru/SOMETHING-2334)",
		expectedKeys1,
		t)

	expectedKeys2 := map[string]bool{"STARTREK-20": true}
	checkGetStartrekTicketKeyWithLinksResult(
		"Here is ticketSTARTREK-21in the middle of title [STARTREK-20](https://st.yandex-team.ru/STARTREK-20)",
		expectedKeys2,
		t)

	expectedKeys3 := map[string]bool{"HITMANSUP-1089": true, "STARTREK-20": true, "HITMANSUP-1088": true, "STARTREK-767": true}
	checkGetStartrekTicketKeyWithLinksResult(
		"Many tickets [HITMANSUP-1089](https://st.yandex-team.ru/HITMANSUP-1089)[HITMANSUP-1088](https://st.yandex-team.ru/HITMANSUP-1088) [STARTREK-767](https://st.yandex-team.ru/STARTREK-767)pull request :![STARTREK-20](https://st.yandex-team.ru/STARTREK-20)",
		expectedKeys3,
		t)

	expectedKeys4 := make(map[string]bool)
	checkGetStartrekTicketKeyWithLinksResult(
		"Pull request with no tickets",
		expectedKeys4,
		t)
}

func TestGetPullRequestKey(t *testing.T) {
	checkGetPullRequestKeyResult(
		"nastia143/repo2",
		4,
		"pull:nastia143/repo2/4",
		t)

	checkGetPullRequestKeyResult(
		"YandexClassifieds/vertis-ansible",
		133848,
		"pull:YandexClassifieds/vertis-ansible/133848",
		t)
}

func TestGetPropertiesFromUrl(t *testing.T) {
	params := make(url.Values)
	params.Add("key1", "value1")
	params.Add("buildTypeId", "BuildId")
	params.Add("key2", "value2")

	ignoredProperties := []string{"buildTypeId"}
	expectedResult := map[string]string{"key1": "value1", "key2": "value2"}

	checkGetPropertiesFromUrlResult(params, ignoredProperties, expectedResult, t)

	params2 := make(url.Values)
	params2.Add("key1", "value1")
	params2.Add("buildTypeId", "BuildId")
	params2.Add("key2", "value2")

	ignoredProperties2 := []string{"key2", "buildTypeId"}
	expectedResult2 := map[string]string{"key1": "value1"}

	checkGetPropertiesFromUrlResult(params2, ignoredProperties2, expectedResult2, t)

}
