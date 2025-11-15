package app.boundary;

import app.controller.workspace.WorkspaceControllerBase;

/**
 * Thin JavaFX boundary that delegates workspace behaviour to {@link WorkspaceControllerBase}.
 */
public final class WorkspaceBoundary extends WorkspaceControllerBase {
    public WorkspaceBoundary() {
        super();
    }
}
