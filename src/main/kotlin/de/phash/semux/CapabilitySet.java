/**
 * Copyright (c) 2017-2018 The Semux Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package de.phash.semux;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable set of capabilities.
 */
public class CapabilitySet {

    private final Set<Capability> capabilities;

    private CapabilitySet(Collection<Capability> capabilities) {
        /* Use TreeSet to maintain capability order, for deterministic hashcode */
        this.capabilities = Collections.unmodifiableSet(new TreeSet<>(capabilities));
    }

    /**
     * Creates an empty set.
     */
    public static CapabilitySet emptySet() {
        return new CapabilitySet(Collections.emptySet());
    }

    /**
     * Converts an array of capability into capability set.
     *
     * @param capabilities
     *            the specified capabilities
     */
    public static CapabilitySet of(Capability... capabilities) {
        return new CapabilitySet(Stream.of(capabilities).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * Converts an array of capability into capability set.
     *
     * @param capabilities
     *            the specified capabilities
     * @ImplNode unknown capabilities are ignored
     */
    public static CapabilitySet of(String... capabilities) {
        return new CapabilitySet(
                Stream.of(capabilities).map(Capability::of).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * Checks whether the capability is supported by the ${@link CapabilitySet}.
     *
     * @param capability
     *            the capability to be checked.
     * @return true if the capability is supported, false if not
     */
    public boolean isSupported(Capability capability) {
        return capabilities.contains(capability);
    }

    /**
     * Returns the size of the capability set.
     */
    public int size() {
        return capabilities.size();
    }

    /**
     * Converts the capability set to an list of String.
     */
    public List<String> toList() {
        return capabilities.stream().map(Capability::name).collect(Collectors.toList());
    }

    /**
     * Converts the capability set to an array of String.
     */
    public String[] toArray() {
        return capabilities.stream().map(Capability::name).toArray(String[]::new);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof CapabilitySet
                && Arrays.equals(toArray(), ((CapabilitySet) object).toArray());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toArray());
    }
}
