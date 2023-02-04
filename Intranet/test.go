package handler

import (
	"fmt"
	"net/http"

	"a.yandex-team.ru/intranet/legacy/staff-api/internal/user"
)

func Test(w http.ResponseWriter, r *http.Request) {
	userID := user.GetUserID(r.Context())
	if userID != 0 {
		_, _ = fmt.Fprintf(w, "Your uid is %v\n", userID)
	}

	serviceID := user.GetServiceID(r.Context())
	if serviceID != 0 {
		_, _ = fmt.Fprintf(w, "Your service id is %v\n", serviceID)
	}
}
