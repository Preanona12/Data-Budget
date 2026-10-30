package com.databudget;

/** Colour palettes + the rules for picking one. No Android classes, so it is unit-tested on a JVM. */
public final class Theme {
    public final String id, name;
    public final boolean dark;
    public final int bg, card, ink, gray, accent, soft, onSoft, heroA, heroB, overA, overB;

    private Theme(String id, String name, boolean dark, int bg, int card, int ink, int gray, int accent, int soft, int onSoft,
                  int heroA, int heroB, int overA, int overB) {
        this.id = id; this.name = name; this.dark = dark; this.bg = bg; this.card = card; this.ink = ink; this.gray = gray;
        this.accent = accent; this.soft = soft; this.onSoft = onSoft; this.heroA = heroA; this.heroB = heroB; this.overA = overA; this.overB = overB;
    }

    static final int RED_A = 0xFFC62828, RED_B = 0xFFEF5350, DEEP_A = 0xFF8E0000, DEEP_B = 0xFFD32F2F;

    /** Concrete themes. "system" and "auto" are choices that resolve to one of these. */
    static final Theme[] ALL = {
        //    id          name         dark   bg          card        ink         gray        accent      soft        onSoft      heroA       heroB       overA   overB
        new Theme("light",     "Light",     false, 0xFFF4F6F8, 0xFFFFFFFF, 0xFF111827, 0xFF5F6B7A, 0xFF00796B, 0xFFE0F2F1, 0xFF00796B, 0xFF00796B, 0xFF2BB5A4, RED_A,  RED_B),
        new Theme("dark",      "Dark",      true,  0xFF0F1419, 0xFF1A222C, 0xFFF3F4F6, 0xFF9CA3AF, 0xFF00897B, 0xFF1F3A38, 0xFF5EEAD4, 0xFF00695C, 0xFF1E9E92, 0xFFB71C1C, 0xFFE53935),
        // moods
        new Theme("calm",      "Calm",      false, 0xFFEAF1F8, 0xFFFFFFFF, 0xFF1E2A3A, 0xFF566377, 0xFF3E6FB5, 0xFFDCE8F5, 0xFF2F5A96, 0xFF3A6BB0, 0xFF5B96D2, RED_A,  RED_B),
        new Theme("energetic", "Energetic", false, 0xFFFFF4E8, 0xFFFFFFFF, 0xFF2B1B0E, 0xFF7A5C42, 0xFFE8590C, 0xFFFFE8D1, 0xFFB34400, 0xFFE8590C, 0xFFE0307F, DEEP_A, DEEP_B),
        new Theme("cozy",      "Cozy",      false, 0xFFF3EADF, 0xFFFFFBF5, 0xFF3B2A20, 0xFF77604F, 0xFF92522F, 0xFFEBDCCB, 0xFF7A4528, 0xFF8A4B2A, 0xFFC27C4E, RED_A,  RED_B),
        new Theme("focus",     "Focus",     true,  0xFF0B0F1A, 0xFF151B2E, 0xFFE5E7EB, 0xFF9AA3B8, 0xFF5457E6, 0xFF1E2342, 0xFFB4BEFF, 0xFF3B35B8, 0xFF6366F1, DEEP_A, DEEP_B),
        // time of day
        new Theme("sunrise",   "Sunrise",   false, 0xFFFFF3E9, 0xFFFFFFFF, 0xFF33200F, 0xFF7F5F45, 0xFFC95A1B, 0xFFFFE6D2, 0xFF9E4310, 0xFFC4521A, 0xFFD97E2B, DEEP_A, DEEP_B),
        new Theme("day",       "Day",       false, 0xFFEEF7FD, 0xFFFFFFFF, 0xFF0F2433, 0xFF53697A, 0xFF0277BD, 0xFFD8EDFA, 0xFF015E96, 0xFF026BAF, 0xFF2399DE, RED_A,  RED_B),
        new Theme("sunset",    "Sunset",    true,  0xFF1D1330, 0xFF2B1D44, 0xFFF6EBFF, 0xFFBBA6D4, 0xFFC93A72, 0xFF3A2858, 0xFFFFB3D1, 0xFF6A2BD9, 0xFFD6237F, DEEP_A, DEEP_B),
        new Theme("night",     "Night",     true,  0xFF090D18, 0xFF111929, 0xFFE2E8F0, 0xFF8D9AB3, 0xFF2F6FDE, 0xFF16243F, 0xFF9CC4FF, 0xFF1A3680, 0xFF3F83F0, DEEP_A, DEEP_B),
    };

    /** What the picker offers, in order: pref id, label. */
    static final String[][] CHOICES = {
        {"light", "Light"}, {"dark", "Dark"}, {"system", "Follow system"},
        {"calm", "Calm"}, {"energetic", "Energetic"}, {"cozy", "Cozy"}, {"focus", "Focus"},
        {"auto", "Auto by time"}, {"sunrise", "Sunrise"}, {"day", "Day"}, {"sunset", "Sunset"}, {"night", "Night"},
    };

    public static Theme byId(String id) {
        for (Theme t : ALL) if (t.id.equals(id)) return t;
        return ALL[0];
    }

    /** Time-coded theme for an hour (0-23): sunrise 5-10, day 10-17, sunset 17-20, night 20-5. */
    public static String forHour(int hour) {
        if (hour >= 5 && hour < 10) return "sunrise";
        if (hour >= 10 && hour < 17) return "day";
        if (hour >= 17 && hour < 20) return "sunset";
        return "night";
    }

    /** Turns the saved choice into a concrete theme id. Unknown or missing choices fall back to light (the v1.x look). */
    public static String resolve(String pref, boolean systemDark, int hour) {
        if ("system".equals(pref)) return systemDark ? "dark" : "light";
        if ("auto".equals(pref)) return forHour(hour);
        for (Theme t : ALL) if (t.id.equals(pref)) return t.id;
        return "light";
    }

    /** Two colours for the little preview swatch of a choice. */
    public static int[] swatch(String pref) {
        if ("system".equals(pref)) return new int[]{byId("light").heroA, byId("dark").bg};
        if ("auto".equals(pref)) return new int[]{byId("sunrise").heroB, byId("night").heroA};
        Theme t = byId(pref); return new int[]{t.heroA, t.heroB};
    }
}
