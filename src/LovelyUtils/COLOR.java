package LovelyUtils;

public enum COLOR {
    A1("rgba(255, 235, 238, 1)"), A2("rgba(225, 245, 254, 1)"),
    A3("rgba(241, 248, 233, 1)"), A4("rgba(236, 239, 241, 1)");

    private final String col;

    COLOR(String c) {this.col = c;}

    public static COLOR getRandom() { return values()[(int) (Math.random() * values().length)];}

    public String toString() {return col;}

}
