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

    private static void waitMaison(String reportId, String opcode, boolean printReport) {
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


//        System.out.println("Hello World!");
        String init = serverUri + "init/" + login;
        var setup = Unirest.post(init).body("{}").asJson();
        System.out.println(setup.getBody().toPrettyString());

//        String citizen1Id = getCitizenId(setup, 1);
//        String baseNoopCmd = serverUri + "command/" + login + "/" + citizen1Id + "/noop";
//        String baseMoveCmd = getCmd(getCitizenId(setup, 1), "move:left");
//        String opcode = "move:left";

        String moveLeft = getCmd(getCitizenId(setup, 1), "move:left");
        String moveRight = getCmd(getCitizenId(setup, 1), "move:left");
        String buildRoad = getCmd(getCitizenId(setup, 1), "build:road");
        var response = Unirest.get(serverUri + "statistics").asJson();

        for (int i = 0; i < 100; i++) {
            response = Unirest.post(moveLeft).body("{}").asJson();
            waitMaison(response.getBody().getObject().get("reportId").toString(), "opcode", true);
            response = Unirest.post(buildRoad).body("{}").asJson();
            waitMaison(response.getBody().getObject().get("reportId").toString(), "opcode", true);
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

/*
        opcode = "spawn:bomber-bot";
        baseCmd = getCmd(getCitizenId(setup, 1), opcode);
        response = Unirest.post(baseCmd).body("{}").asJson();
        System.out.println(response.getBody().toPrettyString());

        reportId = response.getBody().getObject().get("reportId").toString();
        waitMaison(reportId, opcode, true);

        response = Unirest.get(serverUri + "report/" + reportId).asJson();
        String spawnedUnitId = response.getBody().getObject().get("spawnedUnitId").toString();

        int x = setup.getBody().getObject().getJSONObject("townHallCoordinates").getInt("x");
        int y = setup.getBody().getObject().getJSONObject("townHallCoordinates").getInt("y");
        FireParameter fireParameter = new FireParameter(new Point(x, y));

        opcode = "fire:bomber-bot";
        baseCmd = getCmd(spawnedUnitId, opcode);
        response = Unirest.post(baseCmd).body(fireParameter).asJson();
        System.out.println(response.getBody().toPrettyString());

        reportId = response.getBody().getObject().get("reportId").toString();
        waitMaison(reportId, opcode, true);


            response = Unirest.post(getCmd(getCitizenId(setup, 1), "noop")).body("{}").asJson();
            System.out.println(response.getBody().toPrettyString());

          waitMaison(reportId, opcode, true);

 */

        response = Unirest.get(serverUri + "statistics").asJson();
        System.out.println(response.getBody().toPrettyString());
    }
}
