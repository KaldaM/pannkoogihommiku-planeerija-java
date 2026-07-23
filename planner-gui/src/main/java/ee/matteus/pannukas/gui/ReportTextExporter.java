package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.AreaObject;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.LineObject;
import ee.matteus.pannukas.core.model.MarkerObject;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.PowerConnection;
import ee.matteus.pannukas.core.model.PowerConsumer;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.TextObject;
import ee.matteus.pannukas.core.model.Tent;
import ee.matteus.pannukas.core.model.CustomObject;
import ee.matteus.pannukas.core.service.PowerSummary;
import ee.matteus.pannukas.core.service.PowerSummaryService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReportTextExporter {
    private static final Pattern CABLE_LENGTH_PATTERN = Pattern.compile("\\d+(?:[,.]\\d+)?");
    private static final Comparator<CableSummaryRow> CABLE_SUMMARY_ROW_COMPARATOR = Comparator
            .comparing((CableSummaryRow row) -> row.connection().connectorType())
            .thenComparing(row -> row.tent().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(row -> row.source().name(), String.CASE_INSENSITIVE_ORDER);

    private final PowerSummaryService powerSummaryService;

    ReportTextExporter(PowerSummaryService powerSummaryService) {
        this.powerSummaryService = powerSummaryService;
    }

    String export(EventPlan plan, ReportExportScope reportScope, boolean includePower, boolean includeCables, boolean includeGroups) {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append(plan.name()).append(lineSeparator);
        builder.append("=".repeat(plan.name().length())).append(lineSeparator);
        builder.append(lineSeparator);
        appendPlanInfoReport(builder, plan, lineSeparator);

        if (includePower) {
            appendPowerReport(builder, plan, reportScope, lineSeparator);
        }

        if (includeCables) {
            appendCableReport(builder, plan, lineSeparator);
        }

        if (includeGroups) {
            appendGroupReport(builder, plan, lineSeparator);
        }
        appendTextObjectReport(builder, plan, lineSeparator);
        return builder.toString();
    }

    private void appendPlanInfoReport(StringBuilder builder, EventPlan plan, String lineSeparator) {
        builder.append("Plaani andmed").append(lineSeparator);
        builder.append("  Plaan: ").append(plan.name()).append(lineSeparator);
        builder.append("  Mõõtkava: ").append(formatMeters(plan.pixelsPerMeter())).append(" px/m").append(lineSeparator);
        builder.append("  Kaart: ").append(plan.mapImagePath().isBlank() ? "määramata" : plan.mapImagePath()).append(lineSeparator);
        builder.append("  Objekte: ").append(plan.objects().size()).append(lineSeparator);
        builder.append("  Vooluühendusi: ").append(plan.powerConnections().size()).append(lineSeparator);
        builder.append(lineSeparator);
    }

    private void appendPowerReport(StringBuilder builder, EventPlan plan, ReportExportScope reportScope, String lineSeparator) {
        builder.append("Voolu kokkuvõte pesade kaupa").append(lineSeparator);
        builder.append(lineSeparator);
        for (PowerSource source : plan.powerSources()) {
            int sourceUsedWatts = powerSummaryService.summaries(plan).stream()
                    .filter(summary -> summary.sourceId().equals(source.id()))
                    .findFirst()
                    .map(PowerSummary::usedWatts)
                    .orElse(0);
            builder.append(source.name())
                    .append(": ")
                    .append(sourceUsedWatts)
                    .append(" W kasutusel, ")
                    .append(remainingWattsText(source.totalCapacityWatts() - sourceUsedWatts))
                    .append(lineSeparator);

            if (source.outlets().isEmpty()) {
                builder.append("  Väljundeid pole").append(lineSeparator);
            }

            for (int index = 0; index < source.outlets().size(); index++) {
                PowerOutlet outlet = source.outlets().get(index);
                appendOutletReport(builder, plan, source, outlet, index, reportScope, lineSeparator);
            }
            builder.append(lineSeparator);
        }
        appendUnconnectedTentsReport(builder, plan, lineSeparator);
    }

    private void appendOutletReport(
            StringBuilder builder,
            EventPlan plan,
            PowerSource source,
            PowerOutlet outlet,
            int index,
            ReportExportScope reportScope,
            String lineSeparator
    ) {
        int usedWatts = usedWatts(plan, outlet.id());
        List<Tent> tents = connectedTents(plan, source.id(), outlet.id());
        if (reportScope == ReportExportScope.COMPACT && usedWatts == 0 && tents.isEmpty()) {
            return;
        }
        builder.append("  ")
                .append(outletDisplayName(outlet, outletTypeIndex(source, outlet, index)))
                .append(": ")
                .append(outlet.capacityWatts())
                .append(" W mahutavus, ")
                .append(usedWatts)
                .append(" W kasutusel, ")
                .append(remainingWattsText(outlet.capacityWatts() - usedWatts))
                .append(lineSeparator);

        if (tents.isEmpty()) {
            builder.append("    Tarbijaid pole").append(lineSeparator);
            return;
        }

        for (Tent tent : tents) {
            builder.append("    - ")
                    .append(tent.name())
                    .append(": ")
                    .append(tent.requiredWatts())
                    .append(" W");
            if (!tent.groupName().isBlank()) {
                builder.append(" (").append(tent.groupName()).append(")");
            }
            builder.append(lineSeparator);
            for (Equipment equipment : tent.equipment()) {
                builder.append("      * ")
                        .append(equipment.name())
                        .append(": ")
                        .append(equipment.requiredWatts())
                        .append(" W")
                        .append(lineSeparator);
            }
        }
    }

    private List<Tent> connectedTents(EventPlan plan, String sourceId, String outletId) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.sourceId().equals(sourceId))
                .filter(connection -> connection.outletId().equals(outletId))
                .map(connection -> plan.findObject(connection.consumerId()))
                .flatMap(optional -> optional.stream())
                .filter(Tent.class::isInstance)
                .map(Tent.class::cast)
                .toList();
    }

    private void appendUnconnectedTentsReport(StringBuilder builder, EventPlan plan, String lineSeparator) {
        List<Tent> unconnectedTents = plan.tents().stream()
                .filter(tent -> plan.findPowerConnectionForConsumer(tent.id()).isEmpty())
                .toList();
        if (unconnectedTents.isEmpty()) {
            return;
        }

        builder.append("Ühendamata telgid").append(lineSeparator);
        for (Tent tent : unconnectedTents) {
            builder.append("  - ")
                    .append(tent.name())
                    .append(": ")
                    .append(tent.requiredWatts())
                    .append(" W")
                    .append(lineSeparator);
        }
        builder.append(lineSeparator);
    }

    private void appendCableReport(StringBuilder builder, EventPlan plan, String lineSeparator) {
        if (plan.powerConnections().isEmpty()) {
            return;
        }

        double totalLengthMeters = 0.0;
        double totalNotedLengthMeters = 0.0;
        boolean hasNotedLength = false;
        List<CableSummaryRow> cableRows = new ArrayList<>();
        Map<ConnectorType, CableTypeSummary> summariesByType = new EnumMap<>(ConnectorType.class);
        for (Tent tent : plan.tents()) {
            PowerConnection connection = plan.findPowerConnectionForConsumer(tent.id()).orElse(null);
            if (connection == null) {
                continue;
            }
            PowerSource source = plan.findObject(connection.sourceId())
                    .filter(PowerSource.class::isInstance)
                    .map(PowerSource.class::cast)
                    .orElse(null);
            if (source == null) {
                continue;
            }

            double lengthMeters = CableDisplayHelper.lengthMeters(
                    CablePathHelper.cablePath(tent, source, connection, plan.pixelsPerMeter()),
                    plan.pixelsPerMeter()
            );
            totalLengthMeters += lengthMeters;
            OptionalDouble notedLengthMeters = notedCableLengthMeters(connection);
            CableTypeSummary typeSummary = summariesByType.computeIfAbsent(
                    connection.connectorType(),
                    ignored -> new CableTypeSummary()
            );
            typeSummary.addMapLength(lengthMeters);
            if (notedLengthMeters.isPresent()) {
                totalNotedLengthMeters += notedLengthMeters.getAsDouble();
                typeSummary.addNotedLength(notedLengthMeters.getAsDouble());
                typeSummary.addPieces(cableLengthPieces(connection));
                hasNotedLength = true;
            }
            cableRows.add(new CableSummaryRow(tent, source, connection, lengthMeters, notedLengthMeters));
        }

        if (cableRows.isEmpty()) {
            return;
        }

        builder.append("Kaablid").append(lineSeparator);
        cableRows.stream()
                .sorted(CABLE_SUMMARY_ROW_COMPARATOR)
                .map(this::cableSummaryRow)
                .forEach(row -> builder.append(row).append(lineSeparator));
        if (hasNotedLength) {
            builder.append("Kokku: %.1f m märgitud, %.1f m kaardil".formatted(totalNotedLengthMeters, totalLengthMeters)).append(lineSeparator);
        } else {
            builder.append("Kokku: %.1f m".formatted(totalLengthMeters)).append(lineSeparator);
        }
        for (String row : cableTypeSummaryRows(summariesByType)) {
            builder.append(row).append(lineSeparator);
        }
        builder.append(lineSeparator);
    }

    private String cableNotesText(PowerConnection connection) {
        return connection.cableNotes().isBlank() ? "" : " [%s]".formatted(connection.cableNotes());
    }

    private List<String> cableTypeSummaryRows(Map<ConnectorType, CableTypeSummary> summariesByType) {
        List<String> rows = new ArrayList<>();
        if (!summariesByType.isEmpty()) {
            rows.add("Tüübi kaupa:");
        }
        for (ConnectorType connectorType : ConnectorType.values()) {
            CableTypeSummary summary = summariesByType.get(connectorType);
            if (summary == null) {
                continue;
            }
            rows.add(cableTypeSummaryRow(connectorType, summary));
            if (summary.hasPieces()) {
                rows.add("    tükid: %s".formatted(cablePieceCountText(summary.pieceCounts())));
            }
        }
        return rows;
    }

    private String cableTypeSummaryRow(ConnectorType connectorType, CableTypeSummary summary) {
        if (summary.hasNotedLength()) {
            return "  %s: %.1f m märgitud, %.1f m kaardil".formatted(
                    CableDisplayHelper.shortTypeName(connectorType),
                    summary.notedLengthMeters(),
                    summary.mapLengthMeters()
            );
        }
        return "  %s: %.1f m kaardil".formatted(CableDisplayHelper.shortTypeName(connectorType), summary.mapLengthMeters());
    }

    private String cableSummaryRow(CableSummaryRow row) {
        String lengthText = row.notedLengthMeters().isPresent()
                ? "%.1f m kaardil, %.1f m märgitud".formatted(row.mapLengthMeters(), row.notedLengthMeters().getAsDouble())
                : "%.1f m".formatted(row.mapLengthMeters());
        return "  - %s -> %s (%s): %s%s".formatted(
                row.tent().name(),
                row.source().name(),
                row.connection().connectorType().displayName(),
                lengthText,
                cableNotesText(row.connection()) + cableNoteWarningText(row.connection())
        );
    }

    private String cableNoteWarningText(PowerConnection connection) {
        return cableNoteNeedsReview(connection) ? " (tükid kontrollida)" : "";
    }

    private boolean cableNoteNeedsReview(PowerConnection connection) {
        String notes = connection.cableLengthNotes();
        if (notes.isBlank() || !notes.contains("+")) {
            return false;
        }

        for (String part : notes.split("\\+")) {
            if (!part.isBlank() && !CABLE_LENGTH_PATTERN.matcher(part).find()) {
                return true;
            }
        }
        return false;
    }

    private OptionalDouble notedCableLengthMeters(PowerConnection connection) {
        List<Double> pieces = cableLengthPieces(connection);
        if (pieces.isEmpty()) {
            return OptionalDouble.empty();
        }

        double totalLengthMeters = 0.0;
        for (double piece : pieces) {
            totalLengthMeters += piece;
        }
        return OptionalDouble.of(totalLengthMeters);
    }

    private List<Double> cableLengthPieces(PowerConnection connection) {
        List<Double> pieces = new ArrayList<>();
        Matcher matcher = CABLE_LENGTH_PATTERN.matcher(connection.cableLengthNotes());
        while (matcher.find()) {
            pieces.add(Double.parseDouble(matcher.group().replace(',', '.')));
        }
        return pieces;
    }

    private String cablePieceCountText(Map<Double, Integer> pieceCounts) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<Double, Integer> entry : pieceCounts.entrySet()) {
            rows.add("%s m x %d".formatted(formatCablePieceLength(entry.getKey()), entry.getValue()));
        }
        return String.join(", ", rows);
    }

    private String formatCablePieceLength(double lengthMeters) {
        if (Math.abs(lengthMeters - Math.rint(lengthMeters)) < 0.0001) {
            return Integer.toString((int) Math.rint(lengthMeters));
        }
        return "%.1f".formatted(lengthMeters);
    }

    private void appendGroupReport(StringBuilder builder, EventPlan plan, String lineSeparator) {
        if (plan.objects().isEmpty()) {
            return;
        }

        builder.append("Grupid").append(lineSeparator);
        for (Map.Entry<String, List<PlannerObject>> entry : objectsByGroup(plan).entrySet()) {
            builder.append(entry.getKey()).append(lineSeparator);
            for (PlannerObject object : entry.getValue()) {
                builder.append("  - ")
                        .append(object.name())
                        .append(" (")
                        .append(objectTypeName(object))
                        .append(")")
                        .append(lineSeparator);
            }
        }
        builder.append(lineSeparator);
    }

    private Map<String, List<PlannerObject>> objectsByGroup(EventPlan plan) {
        Map<String, List<PlannerObject>> objectsByGroup = new TreeMap<>();
        for (PlannerObject object : plan.objects()) {
            String groupName = object.groupName().isBlank() ? "Määramata" : object.groupName();
            objectsByGroup.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(object);
        }
        return objectsByGroup;
    }

    private void appendTextObjectReport(StringBuilder builder, EventPlan plan, String lineSeparator) {
        List<TextObject> textObjects = plan.objects().stream()
                .filter(TextObject.class::isInstance)
                .map(TextObject.class::cast)
                .filter(textObject -> !textObject.notes().isBlank())
                .toList();
        if (textObjects.isEmpty()) {
            return;
        }

        builder.append("Tekstimärkmed").append(lineSeparator);
        for (TextObject textObject : textObjects) {
            builder.append(textObject.name());
            if (!textObject.groupName().isBlank()) {
                builder.append(" (").append(textObject.groupName()).append(")");
            }
            builder.append(lineSeparator);
            for (String line : textObject.notes().split("\\R")) {
                builder.append("  ").append(line).append(lineSeparator);
            }
            builder.append(lineSeparator);
        }
    }

    private int usedWatts(EventPlan plan, String outletId) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.outletId().equals(outletId))
                .map(connection -> plan.findObject(connection.consumerId()))
                .flatMap(optional -> optional.stream())
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .mapToInt(PowerConsumer::requiredWatts)
                .sum();
    }

    private String outletDisplayName(PowerOutlet outlet, int matchingIndex) {
        if (!outlet.name().isBlank()) {
            return "%s (%s %d)".formatted(outlet.name(), outlet.type().displayName(), matchingIndex);
        }
        return "%s %d".formatted(outlet.type().displayName(), matchingIndex);
    }

    private int outletTypeIndex(PowerSource source, PowerOutlet targetOutlet, int targetIndex) {
        int matchingIndex = 0;
        for (int index = 0; index <= targetIndex; index++) {
            if (source.outlets().get(index).type() == targetOutlet.type()) {
                matchingIndex++;
            }
        }
        return matchingIndex;
    }

    private String remainingWattsText(int remainingWatts) {
        if (remainingWatts < 0) {
            return "ÜLEKOORMUS %d W".formatted(Math.abs(remainingWatts));
        }
        return "%d W alles".formatted(remainingWatts);
    }

    private String objectTypeName(PlannerObject object) {
        if (object instanceof Tent) {
            return "Telk";
        }
        if (object instanceof PowerSource) {
            return "Elektrikapp";
        }
        if (object instanceof TextObject) {
            return "Tekst";
        }
        if (object instanceof MarkerObject) {
            return "Marker";
        }
        if (object instanceof AreaObject) {
            return "Ala";
        }
        if (object instanceof LineObject) {
            return "Joon";
        }
        if (object instanceof CustomObject) {
            return "Objekt";
        }
        return "Objekt";
    }

    private String formatMeters(double meters) {
        if (meters == Math.rint(meters)) {
            return "%.0f".formatted(meters);
        }
        return "%.2f".formatted(meters);
    }

    private record CableSummaryRow(
            Tent tent,
            PowerSource source,
            PowerConnection connection,
            double mapLengthMeters,
            OptionalDouble notedLengthMeters
    ) {
    }

    private static class CableTypeSummary {
        private double mapLengthMeters;
        private double notedLengthMeters;
        private boolean hasNotedLength;
        private final Map<Double, Integer> pieceCounts = new TreeMap<>();

        void addMapLength(double lengthMeters) {
            mapLengthMeters += lengthMeters;
        }

        void addNotedLength(double lengthMeters) {
            notedLengthMeters += lengthMeters;
            hasNotedLength = true;
        }

        void addPieces(List<Double> pieces) {
            for (double piece : pieces) {
                pieceCounts.merge(piece, 1, Integer::sum);
            }
        }

        double mapLengthMeters() {
            return mapLengthMeters;
        }

        double notedLengthMeters() {
            return notedLengthMeters;
        }

        boolean hasNotedLength() {
            return hasNotedLength;
        }

        boolean hasPieces() {
            return !pieceCounts.isEmpty();
        }

        Map<Double, Integer> pieceCounts() {
            return pieceCounts;
        }
    }
}
