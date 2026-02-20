package fr.kazotaruumc72.elbram.model;

import java.util.Locale;

/**
 * Taxonomie des niveaux de rareté des Informations.
 *
 * <table>
 *   <tr><th>Identifiant</th><th>Transférable</th><th>Vulnérable à la perte</th><th>Facteur de puissance</th></tr>
 *   <tr><td>COMMON</td>     <td>Oui</td>        <td>Oui</td>                   <td>1.00</td></tr>
 *   <tr><td>UNCOMMON</td>   <td>Oui</td>        <td>Oui</td>                   <td>1.25</td></tr>
 *   <tr><td>RARE</td>       <td>Oui</td>        <td>Oui</td>                   <td>1.60</td></tr>
 *   <tr><td>EPIC</td>       <td>Oui</td>        <td>Oui</td>                   <td>2.15</td></tr>
 *   <tr><td>LEGENDARY</td>  <td>Non</td>        <td>Non</td>                   <td>3.50</td></tr>
 *   <tr><td>TOP_SECRET</td> <td>Non</td>        <td>Non</td>                   <td>5.00</td></tr>
 * </table>
 */
public enum Rarity {

    COMMON    ("Commun",     "<white>",       "§f", true,  true,  1.00),
    UNCOMMON  ("Peu Commun", "<green>",       "§a", true,  true,  1.25),
    RARE      ("Rare",       "<blue>",        "§9", true,  true,  1.60),
    EPIC      ("Épique",     "<dark_purple>", "§5", true,  true,  2.15),
    LEGENDARY ("Légendaire", "<gold>",        "§6", false, false, 3.50),
    TOP_SECRET("Classifié",  "<dark_red>",    "§4", false, false, 5.00);

    private final String displayName;
    private final String miniMessageColor;
    private final String legacyColor;
    private final boolean transferable;
    private final boolean lossVulnerable;
    private final double powerFactor;

    Rarity(String displayName, String miniMessageColor, String legacyColor,
           boolean transferable, boolean lossVulnerable, double powerFactor) {
        this.displayName      = displayName;
        this.miniMessageColor = miniMessageColor;
        this.legacyColor      = legacyColor;
        this.transferable     = transferable;
        this.lossVulnerable   = lossVulnerable;
        this.powerFactor      = powerFactor;
    }

    /** Nom affiché en jeu (avec couleur legacy). */
    public String getDisplayName()      { return legacyColor + displayName; }

    /** Balise de couleur MiniMessage (ex. {@code <gold>}). */
    public String getMiniMessageColor() { return miniMessageColor; }

    /** Code couleur legacy (ex. {@code §6}). */
    public String getLegacyColor()      { return legacyColor; }

    /** {@code true} si cette Information peut être transmise à un autre joueur via {@code /apprendre}. */
    public boolean isTransferable()     { return transferable; }

    /** {@code true} si cette Information peut être perdue aléatoirement lors de la réception d'un échange. */
    public boolean isLossVulnerable()   { return lossVulnerable; }

    /** Facteur de puissance multiplicatif associé à ce niveau de rareté. */
    public double getPowerFactor()      { return powerFactor; }

    /**
     * Convertit une chaîne en {@link Rarity} (insensible à la casse).
     * Retourne {@link #COMMON} si la valeur est {@code null} ou inconnue.
     */
    public static Rarity fromString(String name) {
        if (name == null) return COMMON;
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
