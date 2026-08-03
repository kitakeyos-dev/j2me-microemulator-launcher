package me.kitakeyos.j2me.domain.network.model;

/**
 * A rule that rewrites part of an outbound connection.
 * <p>
 * Rules are grouped and ordered by their own declarations rather than by a
 * hard-coded sequence in the connector: within a {@link #group()} the first
 * matching rule wins, and groups are applied in ascending {@link #order()}.
 * Supporting a new kind of rewrite therefore means adding a rule class and a
 * codec, with no edit to the connection path.
 */
public interface SocketRule {

    /**
     * Rules sharing a group are mutually exclusive — once one matches, the
     * rest of the group is skipped for that connection.
     */
    String group();

    /**
     * Relative position of this rule's group in the pipeline. Lower runs first.
     */
    int order();

    /**
     * Whether the user has this rule switched on.
     */
    boolean isEnabled();

    /**
     * The instance this rule is scoped to, or {@link #ALL_INSTANCES}.
     */
    int getInstanceId();

    /**
     * Whether this rule should act on the given route. Implementations must
     * return false when {@link #isEnabled()} is false.
     */
    boolean matches(SocketRoute route);

    /**
     * Rewrite the route. Only invoked when {@link #matches} returned true.
     */
    void apply(SocketRoute route);

    /**
     * Scope value meaning "every emulator instance".
     */
    int ALL_INSTANCES = -1;
}
