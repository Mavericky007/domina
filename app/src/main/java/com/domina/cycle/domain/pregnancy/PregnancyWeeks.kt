package com.domina.cycle.domain.pregnancy

/** Bundled week-by-week pregnancy content. Sizes are friendly approximations, not medical data. */
object PregnancyWeeks {
    val all: List<PregnancyWeek> = listOf(
        PregnancyWeek(4, "poppy seed", "⚫", 0.1, 0, "The neural tube is forming.", "Baby is just a tiny dot — but a busy one!"),
        PregnancyWeek(5, "sesame seed", "⚫", 0.2, 0, "The heart begins to beat.", "That first heartbeat starts around now 💛"),
        PregnancyWeek(6, "lentil", "🟢", 0.5, 0, "Tiny limb buds appear.", "Little arm and leg buds are sprouting."),
        PregnancyWeek(7, "blueberry", "🫐", 1.0, 1, "The brain is developing fast.", "Baby's head is growing quickly this week."),
        PregnancyWeek(8, "raspberry", "🍓", 1.6, 1, "Fingers and toes are forming.", "Webbed little fingers are taking shape."),
        PregnancyWeek(9, "cherry", "🍒", 2.3, 2, "Tiny earlobes appear.", "Baby now has the beginnings of ears."),
        PregnancyWeek(10, "strawberry", "🍓", 3.1, 4, "Vital organs are working.", "Tiny nails are starting to form."),
        PregnancyWeek(11, "lime", "🍋", 4.1, 7, "Baby can hiccup.", "Those first practice hiccups begin."),
        PregnancyWeek(12, "plum", "🟣", 5.4, 14, "Reflexes are developing.", "Baby can curl tiny fingers and toes."),
        PregnancyWeek(13, "lemon", "🍋", 7.4, 23, "Fingerprints are forming.", "Unique little fingerprints appear this week."),
        PregnancyWeek(14, "peach", "🍑", 8.7, 43, "Baby can squint and frown.", "Tiny facial expressions are starting!"),
        PregnancyWeek(15, "apple", "🍎", 10.1, 70, "Sensing light through eyelids.", "Baby may sense bright light now."),
        PregnancyWeek(16, "avocado", "🥑", 11.6, 100, "Tiny ears are tuning in.", "Baby is starting to hear your voice 💛"),
        PregnancyWeek(17, "pear", "🍐", 13.0, 140, "The skeleton is hardening.", "Soft cartilage is turning to bone."),
        PregnancyWeek(18, "bell pepper", "🫑", 14.2, 190, "Yawning and stretching.", "Baby is wriggling around in there."),
        PregnancyWeek(19, "mango", "🥭", 15.3, 240, "Vernix coats the skin.", "A creamy protective layer forms."),
        PregnancyWeek(20, "banana", "🍌", 16.4, 300, "Halfway there — and can hear you!", "You're halfway! Sing away 🎵"),
        PregnancyWeek(21, "carrot", "🥕", 26.7, 360, "Eyebrows are forming.", "Baby is practicing little movements."),
        PregnancyWeek(22, "spaghetti squash", "🌟", 27.8, 430, "Sense of touch develops.", "Baby may grab the umbilical cord."),
        PregnancyWeek(23, "grapefruit", "🍊", 28.9, 501, "Hearing is improving.", "Loud sounds may make baby move."),
        PregnancyWeek(24, "ear of corn", "🌽", 30.0, 600, "The face is fully formed.", "Baby has eyelashes now!"),
        PregnancyWeek(25, "rutabaga", "🌟", 34.6, 660, "Responding to your voice.", "Baby may react when you talk 💛"),
        PregnancyWeek(26, "scallion", "🌱", 35.6, 760, "Eyes will open soon.", "Baby is taking practice breaths."),
        PregnancyWeek(27, "cauliflower", "🌿", 36.6, 875, "Sleep and wake cycles begin.", "Baby is settling into a rhythm."),
        PregnancyWeek(28, "eggplant", "🍆", 37.6, 1005, "Dreaming (REM) begins.", "Third trimester starts — you've got this!"),
        PregnancyWeek(29, "butternut squash", "🌟", 38.6, 1153, "Muscles are maturing.", "Those kicks are getting stronger."),
        PregnancyWeek(30, "cabbage", "🥬", 39.9, 1319, "Eyes can track light.", "Baby's brain is growing fast."),
        PregnancyWeek(31, "coconut", "🥥", 41.1, 1502, "All five senses are working.", "Baby can turn their head now."),
        PregnancyWeek(32, "napa cabbage", "🥬", 42.4, 1702, "Practicing breathing.", "Fingernails reach the fingertips."),
        PregnancyWeek(33, "pineapple", "🍍", 43.7, 1918, "Bones are hardening.", "The skull stays soft and flexible for birth."),
        PregnancyWeek(34, "cantaloupe", "🍈", 45.0, 2146, "May recognize songs.", "Play a favorite tune — baby's listening!"),
        PregnancyWeek(35, "honeydew", "🍈", 46.2, 2383, "Kidneys are fully developed.", "Baby is plumping up nicely."),
        PregnancyWeek(36, "romaine lettuce", "🥬", 47.4, 2622, "Getting into position.", "Baby may be settling head-down."),
        PregnancyWeek(37, "swiss chard", "🌿", 48.6, 2859, "Considered early term soon.", "Baby is practicing breathing and sucking."),
        PregnancyWeek(38, "leek", "🌱", 49.8, 3083, "A firm little grasp.", "Baby's grip is surprisingly strong!"),
        PregnancyWeek(39, "mini watermelon", "🍉", 50.7, 3288, "Ready any day now.", "Fully developed and getting cozy."),
        PregnancyWeek(40, "small pumpkin", "🎃", 51.2, 3462, "Welcome, baby! 💛", "Only about 5% of babies arrive on their due date."),
    )

    fun forWeek(week: Int): PregnancyWeek {
        val clamped = week.coerceIn(all.first().week, all.last().week)
        return all.first { it.week == clamped }
    }
}
