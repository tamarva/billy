package com.billy.billy.connections;

/**
 * The possible states of connection.
 */
enum ConnectionState {
    /**
     * Not trying to connect to anything.
     */
    IDLE,
    /**
     * We're trying to connect to an {@link Endpoint}.
     * While at this state, we won't allow any other connection requests, and {@link ConnectionRole#DISCOVERER}s
     * stop discovering.
     */
    CONNECTING,
    /**
     *  We've connected to another device, and can now send them information.
     *  {@link ConnectionRole#DISCOVERER}s will still not discover.
     */
    CONNECTED
}
