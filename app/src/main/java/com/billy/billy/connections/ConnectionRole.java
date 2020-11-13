package com.billy.billy.connections;

/**
 * The possible roles of connecting devices.
 */
public enum ConnectionRole {
    /**
     * Role not yet decided.
     */
    UNKNOWN,
    /**
     * Advertises itself so that others nearby can discover it.
     */
    ADVERTISER,
    /**
     * Constantly listens for a device to advertise near it.
     */
    DISCOVERER
}
