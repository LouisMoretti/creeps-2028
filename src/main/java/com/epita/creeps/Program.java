package com.epita.creeps;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;

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

        while (!response.getBody().getObject().get("opcode").toString().equals(opcode))
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
        String opcode = "move:left";
        String baseBuildCmd = getCmd(getCitizenId(setup, 1), opcode);
        var response = Unirest.post(baseBuildCmd).body("{}").asJson();
        System.out.println(response.getBody().toPrettyString());

        String reportId = response.getBody().getObject().get("reportId").toString();
        waitMaison(reportId, opcode, true);

        opcode = "build:road";
        baseBuildCmd = getCmd(getCitizenId(setup, 1), opcode);
        response = Unirest.post(baseBuildCmd).body("{}").asJson();
        System.out.println(response.getBody().toPrettyString());

        reportId = response.getBody().getObject().get("reportId").toString();
        waitMaison(reportId, opcode, true);

        response = Unirest.get(serverUri + "statistics").asJson();
        System.out.println(response.getBody().toPrettyString());
    }
}
