# 3D Analytics Dashboard

## Overview

The 3D Analytics Dashboard is an interactive visualization component that
displays graduate program application statistics using JavaFX 3D graphics. It
provides an engaging, data-driven interface for users to explore their
application portfolio through a dynamic 3D bar chart.

## Purpose & Rationale

### Why 3D Visualization?

1. **Enhanced Data Comprehension**: 3D representations make it easier to compare
   multiple data categories at a glance
2. **User Engagement**: Interactive 3D graphics create a more engaging user
   experience compared to traditional 2D charts
3. **Visual Hierarchy**: Cylinder heights provide an intuitive representation of
   status distribution, making patterns immediately recognizable
4. **Modern UI/UX**: Demonstrates advanced JavaFX capabilities and modern
   application design
5. **Educational Value**: Serves as a learning reference for implementing 3D
   graphics in JavaFX applications

### What Does It Represent?

The dashboard visualizes the following application statistics:

- **Total Programs**: Count of all graduate programs in the user's tracker
- **Total Documents**: Count of all uploaded documents across all programs
- **Active Applications**: Number of programs with status other than "Rejected"
- **Status Distribution**: Visual breakdown of programs by their current status:
  - **Accepted** (Green): Programs where you've been accepted
  - **Applied** (Blue): Programs where application is submitted and pending
  - **In Progress** (Orange): Programs with ongoing/incomplete applications
  - **Rejected** (Red): Programs where application was not successful
  - **Other** (Gray): Programs in any other status or unknown state

Each status category is represented by a 3D cylinder whose height corresponds to
the count of programs in that status. The tallest cylinder represents the most
common status in your application portfolio.

## User Interactions

### Mouse Controls

1. **Drag to Rotate**

   - Click and drag anywhere in the 3D scene to rotate the view
   - Horizontal drag: Rotates around Y-axis (left/right rotation)
   - Vertical drag: Rotates around X-axis (up/down tilt)
   - Useful for viewing the chart from different angles

2. **Click to View Details**

   - Click on any cylinder to display detailed information in the right panel
   - Details shown:
     - Status name
     - Exact count of programs
     - Percentage of total programs
   - Selected cylinder remains highlighted

3. **Hover Effects**
   - Hover over any cylinder to see interactive effects:
     - Color brightens (50% brighter)
     - Cylinder scales up (1.2x larger)
   - Effects reset when mouse moves away

### Other Controls

- **Refresh Button**: Reload dashboard data from backend API
- **Automatic Rotation**: 3D scene continuously rotates on Y-axis for visual
  appeal

## Technical Implementation

### Architecture

```
Backend (Spring Boot)
├── DashboardController (@RestController)
│   └── GET /api/dashboard/stats
│       └── Returns DashboardStatsDTO
│
├── ProgramService (@Service)
│   └── getDashboardStats(userId)
│       └── Aggregates program statistics
│
└── DashboardStatsDTO
    ├── totalPrograms: int
    ├── totalDocuments: int
    ├── activeApplications: int
    └── statusCounts: Map<String, Integer>

Frontend (JavaFX)
├── Dashboard3DView.fxml (FXML Layout)
│   ├── SubScene container
│   ├── Statistics cards
│   ├── Color legend
│   └── Details panel
│
├── Dashboard3DController (@FXML Controller)
│   ├── 3D Scene setup
│   ├── Camera & lighting
│   ├── Bar creation & data mapping
│   ├── Animations
│   └── Event handlers
│
└── DashboardServiceFx (API Client)
    └── getDashboardStats() → DashboardStatsDTO
```

### Key Classes & Components

#### Backend Components

1. **`DashboardController.java`**

   - REST endpoint: `GET /api/dashboard/stats`
   - Extracts authenticated user ID from JWT
   - Delegates to `ProgramService` for statistics aggregation
   - Returns JSON response with `DashboardStatsDTO`

2. **`ProgramService.getDashboardStats()`**

   - Queries all programs for authenticated user
   - Counts total documents using `DocumentRepository`
   - Calculates active applications (excludes "Rejected" status)
   - Groups programs by status and counts each category
   - Returns `DashboardStatsDTO` with aggregated data

3. **`DashboardStatsDTO.java`**
   - Data transfer object for dashboard statistics
   - Fields: `totalPrograms`, `totalDocuments`, `activeApplications`,
     `statusCounts`
   - Serialized to/from JSON for backend-frontend communication

#### Frontend Components

4. **`Dashboard3DView.fxml`**

   - Layout structure:
     - `BorderPane` root
     - `StackPane` center containing `SubScene` (3D container)
     - `VBox` right panel with summary cards, legend, details
   - Binds to `Dashboard3DController`

5. **`Dashboard3DController.java`**

   - **3D Scene Setup**: Creates `SubScene`, `PerspectiveCamera`, `Group` root
   - **Camera Configuration**:
     - `PerspectiveCamera` with `nearClip=1`, `farClip=10000`
     - Positioned at Z=-800 for optimal viewing distance
   - **Lighting**:
     - `AmbientLight` (white, intensity 0.3) for base illumination
     - Two `PointLight` instances (positioned at different angles) for depth
   - **3D Cylinders**:
     - Created using `javafx.scene.shape.Cylinder`
     - Height scaled based on status count (max height 300 pixels)
     - Radius: 30 pixels
     - Material: `PhongMaterial` with status-specific colors
   - **Animations**:
     - `RotateTransition`: Continuous Y-axis rotation (360°, 5 seconds, infinite
       loop)
   - **Event Handlers**:
     - `onMousePressed`: Capture initial drag position
     - `onMouseDragged`: Update `rotateX`/`rotateY` transforms based on mouse
       delta
     - `onMouseClicked`: Display status details in right panel
     - `onMouseEntered`: Brighten color, scale up cylinder
     - `onMouseExited`: Restore original color and scale

6. **`DashboardServiceFx.java`**
   - Extends `ApiClient` base class
   - `getDashboardStats()`: HTTP GET request to `/api/dashboard/stats`
   - Parses JSON response into `DashboardStatsDTO` using Jackson `ObjectMapper`
   - Handles authentication via JWT token (added by `ApiClient.GET()`)

### JavaFX 3D APIs Used

- **`SubScene`**: Container for 3D content, separate from 2D UI elements
- **`PerspectiveCamera`**: Creates realistic 3D perspective projection
- **`Group`**: Root node for organizing 3D shapes
- **`AmbientLight`**: Global illumination affecting all shapes equally
- **`PointLight`**: Directional light source positioned in 3D space
- **`Cylinder`**: 3D cylindrical shape representing bar chart columns
- **`PhongMaterial`**: Realistic material with diffuse color and specular
  highlights
- **`Rotate`**: Transform for rotating nodes around X/Y/Z axes
- **`RotateTransition`**: Timeline-based animation for smooth rotation

### Data Flow

1. User navigates to "3D Analytics" from main menu
2. `MainLayoutController` loads `Dashboard3DView.fxml`
3. `Dashboard3DController.initialize()` is called:
   - Sets up status colors map
   - Creates 3D scene, camera, lighting
   - Calls `loadDashboardData()`
4. `loadDashboardData()` executes async:
   - `DashboardServiceFx.getDashboardStats()` sends HTTP GET to backend
   - Backend authenticates user via JWT
   - `DashboardController` extracts user ID
   - `ProgramService.getDashboardStats()` queries database
   - Returns `DashboardStatsDTO` as JSON
5. Frontend receives response:
   - Updates summary cards (total programs, documents, active apps)
   - Calls `create3DBars()` to generate cylinders based on status counts
   - Attaches event handlers and animations
6. User interacts with 3D scene (drag, click, hover)
7. Event handlers respond with visual feedback

## Color Coding

| Status      | Color  | RGB Value       | Meaning                           |
| ----------- | ------ | --------------- | --------------------------------- |
| Accepted    | Green  | (0, 200, 0)     | Application successfully accepted |
| Applied     | Blue   | (0, 100, 255)   | Application submitted, pending    |
| In Progress | Orange | (255, 140, 0)   | Application in progress           |
| Rejected    | Red    | (255, 50, 50)   | Application rejected              |
| Other       | Gray   | (150, 150, 150) | Unknown or miscellaneous status   |

## Performance Considerations

- **Asynchronous Data Loading**: Backend API calls run on background thread to
  prevent UI blocking
- **Efficient Rendering**: JavaFX 3D scene graph optimized for smooth 60 FPS
  rendering
- **Limited Bar Count**: Maximum of 5 status categories ensures manageable scene
  complexity
- **Material Reuse**: `PhongMaterial` instances reused where possible
- **Transform Caching**: Rotation transforms stored as instance variables to
  avoid recreation

## Future Enhancements

Potential improvements for future versions:

1. **Timeline View**: Add temporal dimension showing status changes over time
2. **Drill-Down**: Click on cylinder to show list of programs in that status
3. **Export**: Save 3D view as image or export statistics as PDF
4. **Customization**: Allow users to configure colors, chart type, or data
   filters
5. **Advanced Metrics**: Add average application duration, success rate trends
6. **Comparison Mode**: Side-by-side comparison of statistics across different
   time periods

## Screenshots

_[Placeholder: Add screenshots showing different views and interactions of the
3D dashboard]_

### Recommended Screenshots

1. Default view: Full 3D dashboard with all cylinders
2. Hover effect: Cylinder brightened and scaled up
3. Details panel: Showing selected status information
4. Rotated view: Dashboard viewed from different angle
5. Empty state: Dashboard with no data (default visualization)

## Developer Notes

### Adding New Status Categories

To add a new status category to the visualization:

1. Update `setupStatusColors()` in `Dashboard3DController.java`:

   ```java
   statusColors.put("NewStatus", Color.rgb(R, G, B));
   ```

2. Update the `statuses` array in `create3DBars()`:

   ```java
   String[] statuses = {"Accepted", "Applied", "In Progress", "Rejected", "NewStatus", "Other"};
   ```

3. Update the legend in `Dashboard3DView.fxml` (optional)

### Testing Locally

1. Ensure backend is running on `http://localhost:8080`
2. Login with valid credentials to obtain JWT token
3. Navigate to "3D Analytics" from main menu
4. Verify:
   - Statistics cards show correct counts
   - Cylinders render with appropriate heights
   - Animations are smooth
   - Mouse interactions respond correctly

### Common Issues

**Issue**: 3D scene appears black or empty  
**Solution**: Verify lighting is properly configured (AmbientLight +
PointLights)

**Issue**: Cylinders not visible  
**Solution**: Check camera position (Z-axis) and ensure cylinders have non-zero
height

**Issue**: API call fails with 401 Unauthorized  
**Solution**: Verify JWT token is valid and not expired in `UserSession`

**Issue**: Rotation animation stutters  
**Solution**: Check for blocking operations on JavaFX Application Thread

## License & Attribution

This component is part of the GradAppTracker application. All JavaFX 3D APIs are
provided by OpenJFX under the GPL v2 + Classpath Exception license.

---

**Last Updated**: 2024  
**Author**: GradAppTracker Development Team  
**JavaFX Version**: 17+  
**Backend**: Spring Boot 3.x  
**Database**: PostgreSQL
