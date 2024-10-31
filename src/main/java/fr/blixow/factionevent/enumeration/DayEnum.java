package fr.blixow.factionevent.enumeration;

public enum DayEnum {

    MONDAY("Lundi"),
    TUESDAY("Mardi"),
    WEDNESDAY("Mercredi"),
    THURSDAY("Jeudi"),
    FRIDAY("Vendredi"),
    SATURDAY("Samedi"),
    SUNDAY("Dimanche");

    private final String valeur;

    DayEnum(String valeur) {
        this.valeur = valeur ;
    }

    public String getValeur() {
        return  this.valeur ;
    }
}
