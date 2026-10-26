package com.speaksafe.model;

/** What the browser extension should do with a message. */
public enum Action {
    NONE,   // safe message, do nothing
    WARN,   // show a warning to the user
    BLUR    // hide the message behind a blur and flag it for moderators
}
