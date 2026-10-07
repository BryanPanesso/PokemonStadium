package stadium.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Paleta, tipografías y utilidades de dibujo compartidas por toda la interfaz. */
final class Theme {
    static final Color BG_TOP = new Color(0x0B1020);
    static final Color BG_BOTTOM = new Color(0x1B2440);
    static final Color CARD = new Color(0x141C33);
    static final Color CARD_BORDER = new Color(0x2A3657);
    static final Color SURFACE = new Color(0x0D1326);
    static final Color FIELD = new Color(0x0E152B);
    static final Color TEXT = new Color(0xE8ECF6);
    static final Color TEXT_MUTED = new Color(0x8C98B8);
    static final Color ACCENT = new Color(0xFFCB05);
    static final Color PLAYER_1 = new Color(0xE3350D);
    static final Color PLAYER_2 = new Color(0x2A75BB);
    static final Color NEUTRAL_BUTTON = new Color(0x34426A);
    static final Color DISABLED = new Color(0x252D48);
    static final Color DISABLED_TEXT = new Color(0x5F6A8C);
    static final Color ERROR = new Color(0xFF6B6B);
    static final Color SUCCESS = new Color(0x4ADE80);
    static final Color HP_HIGH = new Color(0x22C55E);
    static final Color HP_MID = new Color(0xFACC15);
    static final Color HP_LOW = new Color(0xEF4444);

    private static final String SANS = pickFamily("Segoe UI", "Helvetica Neue", "Arial", Font.SANS_SERIF);
    private static final String MONO = pickFamily("Cascadia Mono", "Consolas", "Menlo", Font.MONOSPACED);

    private static final Map<String, Color> TYPE_COLORS = new HashMap<>();
    private static final Map<String, String> TYPE_NAMES = new HashMap<>();

    static {
        type("normal", 0xA8A77A, "Normal");
        type("fire", 0xEE8130, "Fuego");
        type("water", 0x6390F0, "Agua");
        type("electric", 0xF7D02C, "Eléctrico");
        type("grass", 0x7AC74C, "Planta");
        type("ice", 0x96D9D6, "Hielo");
        type("fighting", 0xC22E28, "Lucha");
        type("poison", 0xA33EA1, "Veneno");
        type("ground", 0xE2BF65, "Tierra");
        type("flying", 0xA98FF3, "Volador");
        type("psychic", 0xF95587, "Psíquico");
        type("bug", 0xA6B91A, "Bicho");
        type("rock", 0xB6A136, "Roca");
        type("ghost", 0x735797, "Fantasma");
        type("dragon", 0x6F35FC, "Dragón");
        type("dark", 0x705746, "Siniestro");
        type("steel", 0xB7B7CE, "Acero");
        type("fairy", 0xD685AD, "Hada");
    }

    private Theme() {
    }

    private static void type(String id, int rgb, String spanish) {
        TYPE_COLORS.put(id, new Color(rgb));
        TYPE_NAMES.put(id, spanish);
    }

    static Font sans(int style, float size) {
        return new Font(SANS, style, 12).deriveFont(size);
    }

    static Font mono(float size) {
        return new Font(MONO, Font.PLAIN, 12).deriveFont(size);
    }

    static Color typeColor(String type) {
        return TYPE_COLORS.getOrDefault(type, new Color(0x68A090));
    }

    static String typeName(String type) {
        String name = TYPE_NAMES.get(type);
        if (name != null) {
            return name;
        }
        return type.isEmpty() ? type : Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }

    /** Texto oscuro sobre colores claros (p. ej. Eléctrico) y blanco sobre oscuros. */
    static Color readableTextOn(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue()) / 255;
        return luminance > 0.65 ? new Color(0x1A1A1A) : Color.WHITE;
    }

    static Color hpColor(double ratio) {
        if (ratio > 0.5) {
            return HP_HIGH;
        }
        return ratio > 0.2 ? HP_MID : HP_LOW;
    }

    static Color mix(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k));
    }

    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static String upper(String text) {
        return text.toUpperCase(new Locale("es"));
    }

    private static String pickFamily(String... candidates) {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String family : candidates) {
            if (available.contains(family)) {
                return family;
            }
        }
        return candidates[candidates.length - 1];
    }
}
