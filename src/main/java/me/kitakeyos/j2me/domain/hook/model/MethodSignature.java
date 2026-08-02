package me.kitakeyos.j2me.domain.hook.model;

/**
 * Immutable identifier of a hookable method: owning class + method name +
 * JVM method descriptor.
 *
 * <p>The owner is always stored in <em>internal</em> form ({@code com/game/Canvas}),
 * so callers may pass either dotted or slashed names.
 *
 * <p>A {@code null} descriptor is a wildcard that matches every overload of
 * {@code name}. Wildcard signatures are only ever used as registration keys —
 * the bytecode always dispatches with a concrete descriptor.
 */
public final class MethodSignature {

    private final String owner;
    private final String name;
    private final String descriptor;

    private MethodSignature(String owner, String name, String descriptor) {
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException("owner must not be empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        this.owner = owner.replace('.', '/');
        this.name = name;
        this.descriptor = (descriptor == null || descriptor.isEmpty()) ? null : descriptor;
    }

    /**
     * Signature matching one specific overload.
     *
     * @param owner      class name, dotted or slashed
     * @param name       method name
     * @param descriptor JVM descriptor, e.g. {@code (I)V}
     */
    public static MethodSignature of(String owner, String name, String descriptor) {
        return new MethodSignature(owner, name, descriptor);
    }

    /**
     * Signature matching every overload of {@code name}.
     */
    public static MethodSignature ofAnyOverload(String owner, String name) {
        return new MethodSignature(owner, name, null);
    }

    /** Owner in internal form, e.g. {@code com/game/Canvas}. */
    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    /** JVM descriptor, or {@code null} when this signature is a wildcard. */
    public String getDescriptor() {
        return descriptor;
    }

    public boolean isWildcard() {
        return descriptor == null;
    }

    /**
     * Wildcard form of this signature — the fallback key looked up when no
     * exact-descriptor hook is registered.
     */
    public MethodSignature toWildcard() {
        return descriptor == null ? this : new MethodSignature(owner, name, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MethodSignature)) {
            return false;
        }
        MethodSignature other = (MethodSignature) o;
        return owner.equals(other.owner)
                && name.equals(other.name)
                && (descriptor == null ? other.descriptor == null : descriptor.equals(other.descriptor));
    }

    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (descriptor == null ? 0 : descriptor.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return owner + "." + name + (descriptor == null ? "(*)" : descriptor);
    }
}
