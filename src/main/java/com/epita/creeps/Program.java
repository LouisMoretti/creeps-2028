package com.epita.creeps;

import com.epita.creeps.given.json.Json;
import com.epita.creeps.given.vo.geometry.Point;
import com.epita.creeps.given.vo.parameter.FireParameter;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.ObjectMapper;
import kong.unirest.core.Unirest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Program {
    static String url;
    static String port;
    static String login;
    static String serverUri;

    private static String getCitizenId(HttpResponse<JsonNode> setup, int n) {
        return setup.getBody().getObject().get("citizen" + n + "Id").toString();
    }

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

    public static void main(String[] args) {
        url = args[0];
        port = args[1];
        login = args[2];
        serverUri = "http://" + url + ":" + port + "/";

        String init = serverUri + "init/" + login;
        var setup = Unirest.post(init).body("{}").asJson();
        System.out.println(setup.getBody().toPrettyString());

        String moveLeft = getCmd(getCitizenId(setup, 1), "move:left");
        String moveRight = getCmd(getCitizenId(setup, 1), "move:left");
        String moveUp = getCmd(getCitizenId(setup, 1), "move:up");
        String moveDown = getCmd(getCitizenId(setup, 1), "move:down");
        String buildRoad = getCmd(getCitizenId(setup, 1), "build:road");
        String gather = getCmd(getCitizenId(setup, 1), "gather");
        String spawnKhalil = getCmd(getCitizenId(setup, 1), "spawn:bomber-bot");

        var response = Unirest.get(serverUri + "statistics").asJson(); // Init response with right type
        String reportId;

        for (int i = 0; i < 100; i++) {
            response = Unirest.post(moveLeft).body("{}").asJson();
            waitMaison(response.getBody().getObject().get("reportId").toString(), true);
            response = Unirest.post(gather).body("{}").asJson();
            waitMaison(response.getBody().getObject().get("reportId").toString(), true);
            response = Unirest.post(buildRoad).body("{}").asJson();
            waitMaison(response.getBody().getObject().get("reportId").toString(), true);
        }

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

        response = Unirest.get(serverUri + "statistics").asJson();
        System.out.println(response.getBody().toPrettyString());
    }
}
