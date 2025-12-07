package com.gradapptracker.ui.services;

/**
 * Central service locator for JavaFX controllers.
 * Provides singleton instances of all UI services to avoid tight coupling
 * and direct instantiation in controllers.
 * 
 * This pattern allows for easier testing and service instance management.
 */
public class ServiceLocator {

    private static ServiceLocator instance;

    private final UserServiceFx userService;
    private final ProgramServiceFx programService;
    private final DocumentServiceFx documentService;
    private final ProgramDocumentServiceFx programDocumentService;
    private final DashboardServiceFx dashboardService;

    private ServiceLocator() {
        this.userService = new UserServiceFx();
        this.programService = new ProgramServiceFx();
        this.documentService = new DocumentServiceFx();
        this.programDocumentService = new ProgramDocumentServiceFx();
        this.dashboardService = new DashboardServiceFx();
    }

    /**
     * Get the singleton instance of ServiceLocator.
     */
    public static ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }

    public UserServiceFx getUserService() {
        return userService;
    }

    public ProgramServiceFx getProgramService() {
        return programService;
    }

    public DocumentServiceFx getDocumentService() {
        return documentService;
    }

    public ProgramDocumentServiceFx getProgramDocumentService() {
        return programDocumentService;
    }

    public DashboardServiceFx getDashboardService() {
        return dashboardService;
    }
}
