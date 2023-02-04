export const getSavedNoteState = () => ({
    offerNotes: {
        notes: {
            1: {
                value: 'note',
                savedValue: 'note',
                isLoading: false
            }
        }
    }
});

export const getEmptyNoteState = () => ({
    offerNotes: {
        notes: {
            1: {
                value: '',
                savedValue: 'note',
                isLoading: false
            }
        }
    }
});

export const getNotSavedNoteState = () => ({
    offerNotes: {
        notes: {
            1: {
                value: 'note!',
                savedValue: '',
                isLoading: false
            }
        }
    }
});

export const getNotChangedNoteState = () => ({
    offerNotes: {
        notes: {
            1: {
                value: 'note!',
                savedValue: 'note!',
                isLoading: false
            }
        }
    }
});

export const getLoadingNoteState = () => ({
    offerNotes: {
        notes: {
            1: {
                value: 'note!',
                savedValue: 'note!!',
                isLoading: true
            }
        }
    }
});
