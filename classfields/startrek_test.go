package startrek

import (
	"cloud.google.com/go/civil"
	gostartrek "github.com/YandexClassifieds/go-startrek"
	sf "github.com/YandexClassifieds/hub/standard_functions"
	"testing"
)

func testEqualStatuses(status1 Status, status2 Status) bool {
	return status1.Id == status2.Id && status1.Key == status2.Key && status1.Display == status2.Display
}

func testEqualStatusTransitionSlices(transitions1 []StatusTransition, transitions2 []StatusTransition) bool {
	for i, transition1 := range transitions1 {
		transition2 := transitions2[i]
		if transition1.DateTime != transition2.DateTime || !testEqualStatuses(transition1.To, transition2.To) || !testEqualStatuses(transition1.From, transition2.From) {
			return false
		}
	}
	return true
}

func testEqualStatusSlices(statuses1 []Status, statuses2 []Status) bool {
	for i, status1 := range statuses1 {
		status2 := statuses2[i]
		if !testEqualStatuses(status1, status2) {
			return false
		}
	}
	return true
}

func checkGetStatusTransitions(t *testing.T, changelog []gostartrek.ChangelogTransition, expectedStatusTransitions []StatusTransition, expectedErr error) {
	statusTransitions, err := getStatusTransitions(changelog)

	if !sf.TestEqualErrors(err, expectedErr) {
		t.Errorf("Failed to get status transitions from changelog '%v': got error '%v', expected '%v'\n", changelog, err, expectedErr)
	}

	if !testEqualStatusTransitionSlices(statusTransitions, expectedStatusTransitions) {
		t.Errorf("Failed to get status transitions from changelog '%v': \ngot:      '%v', \nexpected: '%v'\n", changelog, statusTransitions, expectedStatusTransitions)
	}
}

func TestGetStatusTransitions(t *testing.T) {
	// 1 transition, IssueWorkflow with statuses
	changelog1 := []gostartrek.ChangelogTransition{
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-04-17T10:08:46.269+0000",
			Type:      "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "status",
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "6",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/6",
							Key:     "new",
							Display: "Новый",
						},
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "25",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/25",
							Key:     "approved",
							Display: "Утвержден",
						},
					},
				},
			},
		},
	}
	expectedTransitions1 := []StatusTransition{
		StatusTransition{
			To: Status{
				Id:      "6",
				Key:     "new",
				Display: "Новый",
			},
			From: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 4,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
	}
	checkGetStatusTransitions(t, changelog1, expectedTransitions1, nil)

	// multiple transitions, some of them IssueWorkflow with statuses, some of them not IssueWorkflow or not with statuses
	changelog2 := []gostartrek.ChangelogTransition{
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-04-17T10:08:46.269+0000",
			Type:      "IssueCommentAdded", // Here should be "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "status",
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "6",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/6",
							Key:     "new",
							Display: "Новый",
						},
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "25",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/25",
							Key:     "approved",
							Display: "Утвержден",
						},
					},
				},
			},
		},
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-04-17T10:08:46.269+0000",
			Type:      "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "status",
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "6",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/6",
							Key:     "new",
							Display: "Новый",
						},
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "25",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/25",
							Key:     "approved",
							Display: "Утвержден",
						},
					},
				},
			},
		},
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-04-17T10:08:46.269+0000",
			Type:      "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "comment", // Here should be "status"
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "6",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/6",
							Key:     "new",
							Display: "Новый",
						},
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "25",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/25",
							Key:     "approved",
							Display: "Утвержден",
						},
					},
				},
			},
		},
	}
	checkGetStatusTransitions(t, changelog2, expectedTransitions1, nil)

	// 1 transition, IssueWorkflow with statuses
	changelog3 := []gostartrek.ChangelogTransition{
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-04-17T10:08:46.269+0000",
			Type:      "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "status",
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "6",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/6",
							Key:     "new",
							Display: "Новый",
						},
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "25",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/25",
							Key:     "approved",
							Display: "Утвержден",
						},
					},
				},
			},
		},
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-05-17T10:08:46.269+0000",
			Type:      "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "status",
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "25",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/25",
							Key:     "approved",
							Display: "Утвержден",
						},
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "7",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/7",
							Key:     "resolved",
							Display: "Решен",
						},
					},
				},
			},
		},
		gostartrek.ChangelogTransition{
			ID:        "5ad5c7ae14f7ea001a83f3f6",
			Self:      "https://st-api.yandex-team.ru/v2/issues/VERTISADMIN-18581/changelog/5ad5c7ae14f7ea001a83f3f6",
			UpdatedAt: "2018-05-17T10:08:48.269+0000",
			Type:      "IssueWorkflow",
			Fields: []gostartrek.Field{
				gostartrek.Field{
					Field: gostartrek.FieldId{
						ID:      "status",
						Self:    "https://st-api.yandex-team.ru/v2/fields/status",
						Display: "Статус",
					},
					From: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "7",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/7",
							Key:     "resolved",
							Display: "Решен",
						},
					},
					To: gostartrek.FieldChangeWrapper{
						SingleFieldChange: gostartrek.FieldChange{
							ID:      "10",
							Self:    "https://st-api.yandex-team.ru/v2/statuses/10",
							Key:     "closed",
							Display: "Закрыт",
						},
					},
				},
			},
		},
	}
	expectedTransitions3 := []StatusTransition{
		StatusTransition{
			From: Status{
				Id:      "6",
				Key:     "new",
				Display: "Новый",
			},
			To: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 4,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
		StatusTransition{
			From: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			To: Status{
				Id:      "7",
				Key:     "resolved",
				Display: "Решен",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 5,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
		StatusTransition{
			From: Status{
				Id:      "7",
				Key:     "resolved",
				Display: "Решен",
			},
			To: Status{
				Id:      "10",
				Key:     "closed",
				Display: "Закрыт",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 5,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 48,
				},
			},
		},
	}
	checkGetStatusTransitions(t, changelog3, expectedTransitions3, nil)
}

func checkGetStatusesInOrder(t *testing.T, transitions []StatusTransition, expectedStatuses []Status, expectedErr error) {
	statuses, err := getStatusesInOrder(transitions)

	if !sf.TestEqualErrors(err, expectedErr) {
		t.Errorf("Failed to get statuses from transitions '%v': got error '%v', expected '%v'\n", transitions, err, expectedErr)
	}

	if !testEqualStatusSlices(statuses, expectedStatuses) {
		t.Errorf("Failed to get statuses from transitions '%v': \ngot:      '%v', \nexpected: '%v'\n", transitions, statuses, expectedStatuses)
	}
}

func TestGetStatusesInOrder(t *testing.T) {
	// in order
	transitions1 := []StatusTransition{
		StatusTransition{
			From: Status{
				Id:      "6",
				Key:     "new",
				Display: "Новый",
			},
			To: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 4,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
		StatusTransition{
			From: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			To: Status{
				Id:      "7",
				Key:     "resolved",
				Display: "Решен",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 5,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
		StatusTransition{
			From: Status{
				Id:      "7",
				Key:     "resolved",
				Display: "Решен",
			},
			To: Status{
				Id:      "10",
				Key:     "closed",
				Display: "Закрыт",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 5,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 48,
				},
			},
		},
	}
	expectedStatuses1 := []Status{
		Status{
			Id:      "6",
			Key:     "new",
			Display: "Новый",
		},
		Status{
			Id:      "25",
			Key:     "approved",
			Display: "Утвержден",
		},
		Status{
			Id:      "7",
			Key:     "resolved",
			Display: "Решен",
		},
		Status{
			Id:      "10",
			Key:     "closed",
			Display: "Закрыт",
		},
	}
	checkGetStatusesInOrder(t, transitions1, expectedStatuses1, nil)

	// not in order
	transitions2 := []StatusTransition{
		StatusTransition{
			From: Status{
				Id:      "7",
				Key:     "resolved",
				Display: "Решен",
			},
			To: Status{
				Id:      "10",
				Key:     "closed",
				Display: "Закрыт",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 5,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 48,
				},
			},
		},
		StatusTransition{
			From: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			To: Status{
				Id:      "7",
				Key:     "resolved",
				Display: "Решен",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 5,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
		StatusTransition{
			From: Status{
				Id:      "6",
				Key:     "new",
				Display: "Новый",
			},
			To: Status{
				Id:      "25",
				Key:     "approved",
				Display: "Утвержден",
			},
			DateTime: civil.DateTime{
				Date: civil.Date{
					Year:  2018,
					Month: 4,
					Day:   17,
				},
				Time: civil.Time{
					Hour:   10,
					Minute: 8,
					Second: 46,
				},
			},
		},
	}
	checkGetStatusesInOrder(t, transitions2, expectedStatuses1, nil)
}
