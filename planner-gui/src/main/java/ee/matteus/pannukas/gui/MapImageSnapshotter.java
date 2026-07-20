package ee.matteus.pannukas.gui;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

final class MapImageSnapshotter {
    private static final Color MAP_BACKGROUND = Color.web("#eef1ec");

    private MapImageSnapshotter() {
    }

    static WritableImage snapshot(
            Pane mapPane,
            ScrollPane mapScrollPane,
            Scale mapScale,
            MapImageExportScope scope,
            double mapWidth,
            double mapHeight,
            double zoomLevel,
            Runnable afterScaleRestored
    ) {
        double previousScaleX = mapScale.getX();
        double previousScaleY = mapScale.getY();
        try {
            Rectangle2D viewport = exportViewport(scope, mapScrollPane, mapWidth, mapHeight, zoomLevel);
            mapScale.setX(1.0);
            mapScale.setY(1.0);
            mapPane.applyCss();
            mapPane.layout();

            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setViewport(viewport);
            parameters.setFill(MAP_BACKGROUND);
            WritableImage image = new WritableImage(
                    (int) Math.ceil(viewport.getWidth()),
                    (int) Math.ceil(viewport.getHeight())
            );
            mapPane.snapshot(parameters, image);
            return image;
        } finally {
            mapScale.setX(previousScaleX);
            mapScale.setY(previousScaleY);
            if (afterScaleRestored != null) {
                afterScaleRestored.run();
            }
        }
    }

    private static Rectangle2D exportViewport(
            MapImageExportScope scope,
            ScrollPane mapScrollPane,
            double mapWidth,
            double mapHeight,
            double zoomLevel
    ) {
        if (scope == MapImageExportScope.CURRENT_VIEW && mapScrollPane != null) {
            Bounds viewportBounds = mapScrollPane.getViewportBounds();
            double safeZoomLevel = Math.max(0.0001, zoomLevel);
            double visibleWidth = Math.min(mapWidth, viewportBounds.getWidth() / safeZoomLevel);
            double visibleHeight = Math.min(mapHeight, viewportBounds.getHeight() / safeZoomLevel);
            double horizontalRange = Math.max(0, mapWidth - visibleWidth);
            double verticalRange = Math.max(0, mapHeight - visibleHeight);
            double x = horizontalRange * mapScrollPane.getHvalue();
            double y = verticalRange * mapScrollPane.getVvalue();
            return new Rectangle2D(x, y, Math.max(1, visibleWidth), Math.max(1, visibleHeight));
        }
        return new Rectangle2D(0, 0, mapWidth, mapHeight);
    }
}
