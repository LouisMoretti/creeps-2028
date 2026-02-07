package com.epita.creeps;

import com.epita.creeps.given.exception.NoReportException;
import com.epita.creeps.given.extra.Cartographer;
import com.epita.creeps.given.json.Json;
import com.epita.creeps.given.vo.Tile;
import com.epita.creeps.given.vo.geometry.Point;
import com.epita.creeps.given.vo.report.*;
import com.epita.creeps.given.vo.response.InitResponse;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.RequestBodyEntity;
import kong.unirest.core.Unirest;

import java.util.List;

public class Program {
    static final boolean DEBUG = true;
    static int safeTicks = 20;
    static String url;
    static String port;
    static String login;
    static String serverUri;

    static InitResponse initResponse;
    static List<String> citizens;

    static String moveLeft;
    static String moveRight;
    static String moveUp;
    static String moveDown;
    static String buildRoad;
    static String gather;
    static String spawnKhalil;
    static String observe;
    static String unload;
    static String noop;
    static String buildSmeltery;
    static String refineCopper;
    static String buildSawmill;
    static String refineWoodPlank;

    private static String getCmd(String citizenId, String cmd) {
        return serverUri + "command/" + login + "/" + citizenId + "/" + cmd;
    }

    private static void waitMaison(String reportId, boolean printReport) {
        String cmd = serverUri + "report/" + reportId;
        var response = Unirest.get(cmd).asJson();

        while (response.getBody().getObject().get("opcode").toString().equals("noreport"))
            response = Unirest.get(cmd).asJson();

        if (printReport)
            System.out.println(response.getBody().toPrettyString());
    }

    private static HttpResponse<JsonNode> postResponse(String cmd) {
        return Unirest.post(cmd).body("{}").asJson();
    }

    private static HttpResponse<JsonNode> postResponse(String cmd, RequestBodyEntity body) {
        return Unirest.post(cmd).body(body).asJson();
    }

    private static double twoPointDistance(Point point1, Point point2) {
        int xDelta = point1.x - point2.x;
        int yDelta = point1.y - point2.y;

        double x = xDelta * xDelta;
        double y = yDelta * yDelta;

        return Math.sqrt(x + y);
    }

    private static Point findClosest(Point player, List<Point> objectives) {
        Point closest = objectives.getFirst();
        for (Point objective : objectives) {
            if (twoPointDistance(player, objective) < twoPointDistance(player, closest))
                closest = objective;
        }

        return closest;
    }

    private static Point goToTarget(Point currentPos, Point targetPoint, boolean buildPath) throws NoReportException {
        HttpResponse<JsonNode> response;
        HttpResponse<JsonNode> report;
        Report parsedReport;
        String reportId;

//        if (DEBUG) System.out.println("Original pos:" + currentPos.toString());
//        if (DEBUG) System.out.println("Target pos:" + targetPoint.toString());

        while (!currentPos.equals(targetPoint)) {
            String cmd;
            if (targetPoint.x < currentPos.x) {
                cmd = moveLeft;
            } else if (targetPoint.x > currentPos.x) {
                cmd = moveRight;
            } else if (targetPoint.y < currentPos.y) {
                cmd = moveDown;
            } else {
                cmd = moveUp;
            }

            response = postResponse(cmd);
            reportId = response.getBody().getObject().get("reportId").toString();

            // Loop until report is found
            {
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
                while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
            }

//            System.out.println(response.getBody().toPrettyString());

            parsedReport = Json.parseReport(report.getBody().toString());

            currentPos = ((MoveReport) parsedReport).newPosition;
//            if (DEBUG) System.out.println("New pos:" + currentPos.toString());

            Cartographer.INSTANCE.register((MoveReport) parsedReport);
//            System.out.println(parsedReport.toString());

            if (buildPath) tryBuildRoad(currentPos);
        }

        return currentPos;
    }

    private static Point farmXResources(Point currentPos, Tile resourceToFarm, int amount, boolean buildPath) throws NoReportException {
        HttpResponse<JsonNode> response;
        HttpResponse<JsonNode> report;
        Report parsedReport;
        String reportId;

        int load = 0;
        int total = 0;

        while (total < amount) {
            // Find resources to farm
            List<Point> pointStream = Cartographer.INSTANCE.requestOfType(resourceToFarm).toList();
            if (pointStream.isEmpty()) {
                System.out.println("Could not find any of the wanted resource");
                return currentPos;
            }

            Point closestPoint = findClosest(currentPos, pointStream);

            System.out.println("New target found: " + closestPoint.toString());

            currentPos = goToTarget(currentPos, closestPoint, buildPath);

            System.out.println("Target found");

            System.out.println("Destroying target ...");

            response = postResponse(gather);
            reportId = response.getBody().getObject().get("reportId").toString();

            // Loop until report is found
            {
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
                while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
            }

            parsedReport = Json.parseReport(report.getBody().toString());
            Cartographer.INSTANCE.register((GatherReport) parsedReport);

//            System.out.println("Tick: " + parsedReport.tick + ", GC in " + (initResponse.setup.gcTickRate -
//            parsedReport.tick % initResponse.setup.gcTickRate) + " ticks");
            if ((initResponse.setup.gcTickRate - parsedReport.tick % initResponse.setup.gcTickRate) < safeTicks)
                currentPos = goToSafePlace(currentPos, buildPath);

            load += ((GatherReport) parsedReport).gathered;
            System.out.println("Gathered: " + ((GatherReport) parsedReport).gathered + "; Total: " + load);

            if (load >= initResponse.setup.maxLoad || load + total >= amount) {
                currentPos = goToTarget(currentPos, initResponse.townHallCoordinates, buildPath);

                response = postResponse(unload);
                reportId = response.getBody().getObject().get("reportId").toString();

                // Loop until report is found
                {
                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
                    while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                        report = Unirest.get(serverUri + "report/" + reportId).asJson();
                }

                total += load;
                load = 0;
            }
        }

        return currentPos;
    }

    private static Point goToSafePlace(Point currentPos, boolean buildPath) throws NoReportException {
        List<Point> pointStream = Cartographer.INSTANCE.requestOfType(Tile.Road).toList();
        if (pointStream.isEmpty())
            pointStream = Cartographer.INSTANCE.requestOfType(Tile.TownHall).toList();
        Point closestPoint = findClosest(currentPos, pointStream);

        currentPos = goToTarget(currentPos, closestPoint, buildPath);

        HttpResponse<JsonNode> response;
        HttpResponse<JsonNode> report;
        Report parsedReport;
        String reportId;

        response = postResponse(noop);
        reportId = response.getBody().getObject().get("reportId").toString();

        // Loop until report is found
        {
            report = Unirest.get(serverUri + "report/" + reportId).asJson();
            while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
        }

        parsedReport = Json.parseReport(report.getBody().toString());

        while (parsedReport.tick % initResponse.setup.gcTickRate > 5) {
            response = postResponse(noop);
            reportId = response.getBody().getObject().get("reportId").toString();

            // Loop until report is found
            {
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
                while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
            }

            parsedReport = Json.parseReport(report.getBody().toString());
        }

        return currentPos;
    }

    private static boolean tryBuildRoad(Point currentPos) throws NoReportException {
        HttpResponse<JsonNode> response;
        HttpResponse<JsonNode> report;
        Report parsedReport;
        String reportId;

        if (Cartographer.INSTANCE.requestTileType(currentPos) != Tile.Empty)
            return false;

        response = postResponse(buildRoad);
        reportId = response.getBody().getObject().get("reportId").toString();

        // Loop until report is found
        {
            report = Unirest.get(serverUri + "report/" + reportId).asJson();
            while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
        }

        parsedReport = Json.parseReport(report.getBody().toString());
        Cartographer.INSTANCE.register((BuildReport) parsedReport);

        return true;
    }

    public static void main(String[] args) throws NoReportException {
        // Setup static variables
        {
            url = args[0];
            port = args[1];
            login = args[2];
            serverUri = "http://" + url + ":" + port + "/";
        }

        HttpResponse<JsonNode> response;
        HttpResponse<JsonNode> report;
        Report parsedReport;
        String reportId;

        response = Unirest.post(serverUri + "init/" + login).body("{}").asJson();
        if (DEBUG) System.out.println(response.getBody().toPrettyString());
        initResponse = Json.parse(response.getBody().getObject().toString(), InitResponse.class);

        // Setup basic commands for citizen 1
        {
            moveLeft = getCmd(initResponse.citizen1Id, "move:left");
            moveRight = getCmd(initResponse.citizen1Id, "move:right");
            moveUp = getCmd(initResponse.citizen1Id, "move:up");
            moveDown = getCmd(initResponse.citizen1Id, "move:down");
            buildRoad = getCmd(initResponse.citizen1Id, "build:road");
            buildSmeltery = getCmd(initResponse.citizen1Id, "build:smeltery");
            gather = getCmd(initResponse.citizen1Id, "gather");
            spawnKhalil = getCmd(initResponse.citizen1Id, "spawn:bomber-bot");
            observe = getCmd(initResponse.citizen1Id, "observe");
            unload = getCmd(initResponse.citizen1Id, "unload");
            noop = getCmd(initResponse.citizen1Id, "noop");
            refineCopper = getCmd(initResponse.citizen1Id, "refine:copper");
            buildSawmill = getCmd(initResponse.citizen1Id, "build:sawmill");
            refineWoodPlank = getCmd(initResponse.citizen1Id, "build:wood-plank");
        }

        // ===== OBSERVE NEAR TOWN HALL =====
        response = postResponse(observe);
        reportId = response.getBody().getObject().get("reportId").toString();

        // Loop until report is found
        {
            report = Unirest.get(serverUri + "report/" + reportId).asJson();
            while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
        }

        parsedReport = Json.parseReport(report.getBody().toString());
        Cartographer.INSTANCE.register((ObserveReport) parsedReport);
        // ==================================

        Point currentPos = initResponse.householdCoordinates;

//        currentPos = farmXResources(currentPos, Tile.Rock, 50, false);
//        currentPos = goToSafePlace(currentPos, false);
//
//        currentPos = goToTarget(currentPos, initResponse.townHallCoordinates.plus(1, 0), false);
//        if (Cartographer.INSTANCE.requestTileType(currentPos) != Tile.Empty) {
//            System.out.println("Cannot build ..");
//        }
//
//        response = postResponse(buildSmeltery);
//        reportId = response.getBody().getObject().get("reportId").toString();
//
//        // Loop until report is found
//        {
//            report = Unirest.get(serverUri + "report/" + reportId).asJson();
//            while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
//                report = Unirest.get(serverUri + "report/" + reportId).asJson();
//        }
//
//        parsedReport = Json.parseReport(report.getBody().toString());
//        Cartographer.INSTANCE.register((BuildReport) parsedReport);
//
//
//        for (int i = 0; i < 1; i++) {
//            response = postResponse(refineCopper);
//            System.out.println(response.getBody().toString());
//            reportId = response.getBody().getObject().get("reportId").toString();
//
//            // Loop until report is found
//            {
//                report = Unirest.get(serverUri + "report/" + reportId).asJson();
//                while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
//                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
//            }
//        }



        currentPos = farmXResources(currentPos, Tile.Wood, 40, false);
        currentPos = goToSafePlace(currentPos, false);

        currentPos = goToTarget(currentPos, initResponse.townHallCoordinates.plus(1, 0), false);
        if (Cartographer.INSTANCE.requestTileType(currentPos) != Tile.Empty) {
            System.out.println("Cannot build ..");
        }

        response = postResponse(buildSawmill);
        reportId = response.getBody().getObject().get("reportId").toString();

        // Loop until report is found
        {
            report = Unirest.get(serverUri + "report/" + reportId).asJson();
            while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
        }

        parsedReport = Json.parseReport(report.getBody().toString());
        Cartographer.INSTANCE.register((BuildReport) parsedReport);


        for (int i = 0; i < 1; i++) {
            response = postResponse(refineWoodPlank);
            reportId = response.getBody().getObject().get("reportId").toString();

            // Loop until report is found
            {
                report = Unirest.get(serverUri + "report/" + reportId).asJson();
                while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
            }
        }

//        currentPos = farmXResources(currentPos, Tile.Oil, 1, false);

        System.out.println("Job finished !!!");
/*
//        for (int i = 0; i < 10; i++) {
//            response = postResponse(moveLeft);
//            reportId = response.getBody().getObject().get("reportId").toString();
//
//            // Loop until report is found
//            {
//                report = Unirest.get(serverUri + "report/" + reportId).asJson();
//                while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
//                    report = Unirest.get(serverUri + "report/" + reportId).asJson();
//            }
//
//            System.out.println(response.getBody().toPrettyString());
//
//            parsedReport = Json.parseReport(report.getBody().toString());
//            Cartographer.INSTANCE.register((MoveReport) parsedReport);
//            System.out.println(parsedReport.toString());
//
//        }

//        for (int i = 0; i < 10; i++) {
//            for (String cmd : new String[]{moveDown, moveUp}) {
//                response = Unirest.post(cmd).body("{}").asJson();
//                try {
//                    reportId = response.getBody().getObject().get("reportId").toString();
//
//                    var report = Unirest.get(serverUri + "report/" + reportId).asJson();
//                    while (report.getBody().getObject().get("opcode").toString().equals("noreport"))
//                        report = Unirest.get(serverUri + "report/" + reportId).asJson();
//
//                    System.out.println(response.getBody().toPrettyString());
//
//            var report = Unirest.get(serverUri + "report/" + reportId).asString();
//                    var parsedReport = Json.parseReport(report.getBody().toString());
//                    Cartographer.INSTANCE.register((MoveReport) parsedReport);
//                    System.out.println(parsedReport.toString());
//                } catch (NoReportException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }

//        for (int i = 0; i < 100; i++) {
//            response = Unirest.post(moveLeft).body("{}").asJson();
//            waitMaison(response.getBody().getObject().get("reportId").toString(), true);
//            response = Unirest.post(gather).body("{}").asJson();
//            waitMaison(response.getBody().getObject().get("reportId").toString(), true);
//            response = Unirest.post(buildRoad).body("{}").asJson();
//            waitMaison(response.getBody().getObject().get("reportId").toString(), true);
//        }

//        var response = Unirest.post(baseCmd).body("{}").asJson();
//        System.out.println(response.getBody().toPrettyString());
//
//        String reportId = response.getBody().getObject().get("reportId").toString();
//        waitMaison(reportId, opcode, true);
//
//
//        opcode = "build:road";
//        baseCmd = getCmd(getCitizenId(setup, 1), opcode);
//        response = Unirest.post(baseCmd).body("{}").asJson();
//        System.out.println(response.getBody().toPrettyString());
//        reportId = response.getBody().getObject().get("reportId").toString();
//        waitMaison(reportId, opcode, true);

//        response = Unirest.post(moveDown).body("{}").asJson();
//        System.out.println(response.getBody().toPrettyString());
//        waitMaison(response.getBody().getObject().get("reportId").toString(), false);
//
//        response = Unirest.post(spawnKhalil).body("{}").asJson();
//        System.out.println(response.getBody().toPrettyString());
//        reportId = response.getBody().getObject().get("reportId").toString();
//        waitMaison(reportId, true);
//
//        response = Unirest.get(serverUri + "report/" + reportId).asJson();
//        String spawnedUnitId = response.getBody().getObject().get("spawnedUnitId").toString();
//
//        int x = setup.getBody().getObject().getJSONObject("townHallCoordinates").getInt("x");
//        int y = setup.getBody().getObject().getJSONObject("townHallCoordinates").getInt("y") + 3;
//        FireParameter fireParameter = new FireParameter(new Point(x, y));
//
//        response = Unirest.post(getCmd(spawnedUnitId, "fire:bomber-bot")).body(fireParameter).asJson();
//        System.out.println(response.getBody().toPrettyString());
//
//        reportId = response.getBody().getObject().get("reportId").toString();
//        waitMaison(reportId, true);
 */

        response = Unirest.get(serverUri + "statistics").asJson();
        System.out.println(response.getBody().toPrettyString());
    }
}
