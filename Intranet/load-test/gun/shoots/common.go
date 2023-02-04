package shoots

type CompositeID struct {
	ID               string            `json:"id"`
	ExternalIdentity *ExternalIdentity `json:"external_identity,omitempty"`
}

type ExternalIdentity struct {
	ExternalID string `json:"external_id"`
	TypeID     string `json:"type_id"`
}

type IdentityData struct {
	Name     string `json:"name"`
	LastName string `json:"lastname"`
	Phone    string `json:"phone"`
	Email    string `json:"email"`
	Slug     string `json:"slug"`
}
