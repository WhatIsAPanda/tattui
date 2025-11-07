package app.entity;
public record Proportions(double torso, double arm, double leg, double head) {
    public static final Proportions DEFAULT = new Proportions(1,1,1,1);
}
