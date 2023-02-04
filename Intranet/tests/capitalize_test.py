from ..forms import capitalize_english_position as cap


def test_capitalize_english_position():
    assert cap("chief of staff") == 'Chief of Staff'

    assert cap("head boy") == "Head Boy"

    assert cap("officer    of The deck") == "Officer of the Deck"
    assert cap("assistant to the President") == "Assistant to the President"

    assert cap("master !At arms") == "Master At Arms"
    assert cap("!Chief executioner in exile") == "Chief Executioner in Exile"

    assert cap("!!Guy With exclamation marks!") == (
        "!Guy with Exclamation Marks!"
    )

    assert cap("CTO") == "CTO"

    assert cap("The King") == "The King"

    assert cap("c to the e to the o CEO") == "C to the E to the O CEO"
    assert cap("TO and AND manager") == "TO and AND Manager"

    assert cap("son of a gun") == "Son of a Gun"
    assert cap("Middle-east navy seal deployment commander") == (
        "Middle-east Navy Seal Deployment Commander"
    )
    assert cap("mEGa KIllER") == "mEGa KIllER"
    assert cap("McGee Look-alike") == "McGee Look-alike"

    # Using qoutes is not necessarily a good idea, but hey
    assert cap('"Pushkin Gogol" manager') == '"Pushkin Gogol" Manager'

    # No proper support though, don't want to make code too complex.
    # If you use quotes, you're on your own. Use '!'.
    assert cap('"pushkin gogol" manager') == '"pushkin Gogol" Manager'
    # Capitalized prepositions are usually an error
    assert cap("Fan of The Who") == "Fan of the Who"

    # But if you really really want to, you can do it
    assert cap("Fan of !The Who") == "Fan of The Who"

    # Also it will not break multiline
    assert cap("Fan of\n!The Who") == "Fan of\nThe Who"
