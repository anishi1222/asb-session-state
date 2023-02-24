package org.example.functions;

import com.azure.core.util.IterableStream;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.AbandonOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

public class ASBFunction {

    @FunctionName("state")
    public HttpResponseMessage getMessageSessionState(
            @HttpTrigger(
                route = "state/{namespace}/{queueTopic}/{name}/{sessionId}",
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            @BindingName("namespace") String namespace,
            @BindingName("queueTopic") String type,
            @BindingName("name") String name,
            @BindingName("sessionId") String sessionId,
            final ExecutionContext context) {
        // Initial message
        context.getLogger().info("[getState] Java HTTP trigger processed a request.");

        DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder().build();

        HttpResponseMessage httpResponseMessage = null;
        // Parameter validation
        if(type.equalsIgnoreCase("queue")) {

            try (ServiceBusSessionReceiverClient sessionReceiverClient = new ServiceBusClientBuilder()
                .credential(namespace+".servicebus.windows.net", defaultAzureCredential)
                .sessionReceiver()
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .queueName(name)
                .buildClient()) {
                // Queue
                context.getLogger().info("[getState] Credential -Queue");
                try (ServiceBusReceiverClient receiverClient = sessionReceiverClient.acceptSession(sessionId)) {
                    ArrayList<MessageInfo> messageInfos = collect(receiverClient);
                    httpResponseMessage = request.createResponseBuilder(HttpStatus.OK)
                        .header("content-type","application/json")
                        .body(messageInfos)
                        .build();
                }
            }
            catch(IllegalArgumentException | ServiceBusException e) {
                e.getLocalizedMessage();
            }
        }
        else if(type.equalsIgnoreCase("topic") ) {
            // Topic
            try (ServiceBusSessionReceiverClient sessionReceiverClient = new ServiceBusClientBuilder()
                .credential(namespace+".servicebus.windows.net", defaultAzureCredential)
                .sessionReceiver()
                .topicName(name)
                .buildClient()) {

                context.getLogger().info("[getState] Credential -Topic");
                try (ServiceBusReceiverClient receiverClient = sessionReceiverClient.acceptSession(sessionId)) {
                    ArrayList<MessageInfo> messageInfos = collect(receiverClient);
                    httpResponseMessage = request.createResponseBuilder(HttpStatus.OK)
                        .header("content-type","application/json")
                        .body(messageInfos)
                        .build();
                }
            }
            catch(IllegalArgumentException | ServiceBusException e) {
                e.getLocalizedMessage();
            }
        }
        else {
            String message = """
                {
                    "message": "type (topic or queue) should be specified."
                }
                """;
            httpResponseMessage = request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(message)
                .build();
        }
        return httpResponseMessage;
    }

    private ArrayList<MessageInfo> collect(ServiceBusReceiverClient receiverClient) {
        ArrayList<MessageInfo> messageInfos = new ArrayList<>();
        IterableStream<ServiceBusReceivedMessage> messages = receiverClient.receiveMessages(100, Duration.ofSeconds(1));
        for(ServiceBusReceivedMessage message: messages) {
            messageInfos.add(
                new MessageInfo(message.getMessageId(),
                message.getSessionId(),
                new String(Optional.ofNullable(receiverClient.getSessionState()).orElse("NULL".getBytes(StandardCharsets.UTF_8))),
                message.getBody().toString()));
            receiverClient.abandon(message, new AbandonOptions());
        }
        return messageInfos;
    }
}
