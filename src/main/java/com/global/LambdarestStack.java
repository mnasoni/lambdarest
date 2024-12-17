package com.global;

import software.constructs.Construct;
import java.util.HashMap;
import org.json.simple.JSONArray;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.Method;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

public class LambdarestStack extends Stack {
    public LambdarestStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public LambdarestStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Environment variable to separate the environments
        String environment = "dev"; 

        //Lambda Environment Variables to pass to the Lambdas
        HashMap<String, String> env = new HashMap <String, String>();
        env.put("ENVIRONMENT", environment);
        //Postgres RDS Configuration
        env.put("DBENDPOINT", "dballarmi.cjsweaewqocu.eu-west-1.rds.amazonaws.com");
        env.put("DATABASENAME", "pvallarmi");
        env.put("USERNAME", "allarmipv");
        env.put("PASSWORD", "allarmi2014coll");
        
        //Graviton based Lambdas
        String key = "Architectures";
        JSONArray values = new JSONArray();
        values.add("arm64");

        int memorySize = 1024;
        
        Function getPosDataLambdaFunction = Function.Builder.create(this, "getPosDataLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getPosDataLambda")
                .timeout(Duration.seconds(30))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/lambdarest-0.1.jar"))
                .handler("com.global.lambda.GetPosDataLambdaHandler::handleRequest")
                .build();
        //configure to run as a graviton2 lambda
        CfnFunction cfnFunction = (CfnFunction)getPosDataLambdaFunction.getNode().getDefaultChild();
        cfnFunction.addPropertyOverride(key, values); 

        //API Gateway Configuration (Allowing Lambdas to be called via the API Gateway
        RestApi api = RestApi.Builder.create(this, "Postgres")
                .restApiName("Postgres").description("Postgres")
                .build();
        
        LambdaIntegration getPosDataIntegration = LambdaIntegration.Builder.create(getPosDataLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();    
        
        //Get RDS
        Resource rdsResource = api.getRoot().addResource("rds");
        Method getPosDataMethod = rdsResource.addMethod("GET", getPosDataIntegration);        

        String urlPrefix = api.getUrl().substring(0, api.getUrl().length()-1);
        
        CfnOutput.Builder.create(this, "ZA GET Pos Lambda")
        .description("")
        .value("Pos Lambda:"+urlPrefix + getPosDataMethod.getResource().getPath())
        .build();
    }
}