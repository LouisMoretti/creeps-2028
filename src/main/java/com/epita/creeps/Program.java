package com.epita.creeps;

import kong.unirest.core.Unirest;

public class Program {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        String serverUri = "http://" + args[0] + ":" + args[1] + "/";
        String init = serverUri + "init/" + args[2];
        var setup = Unirest.post(init).body("{}").asJson();
        System.out.println(setup.getBody().toPrettyString());

        String citizen1Id = setup.getBody().getObject().get("citizen1Id").toString();
        String baseNoopCmd = serverUri + "command/" + args[2] + "/" + citizen1Id + "/noop";
        var response = Unirest.post(baseNoopCmd).body("{}").asJson();
        System.out.println(response.getBody().toPrettyString());

        response = Unirest.get(serverUri + "statistics").asJson();
        System.out.println(response.getBody().toPrettyString());
    }
}
