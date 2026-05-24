package com.domina.cycle.domain.pregnancy

object ChecklistDefaults {
    const val BAG = "BAG"
    const val BIRTH_PLAN = "BIRTH_PLAN"

    val bag = listOf(
        "ID & insurance / hospital papers", "Phone + charger", "Comfy going-home clothes",
        "Toiletries & lip balm", "Nursing bra & comfy underwear", "Snacks & drinks",
        "Outfit for baby", "Newborn diapers & wipes", "Cozy socks & slippers",
    )
    val birthPlan = listOf(
        "Pain-relief preferences", "Who will be in the room", "Movement & positions during labor",
        "Delayed cord clamping", "Skin-to-skin right after birth", "Feeding plan (breast / bottle)",
        "Photos / videos preferences",
    )
}
