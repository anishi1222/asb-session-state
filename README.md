# asb-session-state
A sample function app to retrieve message session state in Azure Service Bus.

## Note
This function app uses managed identity for authentication.
When using this function app, 
- Enable managed identities (it is okay to enable either system assigned identities or user assigned identities).
- Configure RBAC for Service Bus instance with the managed identities above.

## Usage
```
GET https://{FunctionApp FQDN}/api/state/{namespace}/{queueTopic}/{name}/{sessionId}
```
- __*namespace*__: Service Bus Namespace
- __*queueTopic*__: media type (specify `queue` or `topic`)
- __*name*__: Queue or Topic name
- __*sessionId*__: session ID to be retrieved

Output image is as follows.
```json
[
    {
        "messageId": "f7be05a9a73a4c9989975f8c1de4e852",
        "sessionId": "12345",
        "messageBody": "2023-02-24T18:26:47.399014200",
        "sessionState": "initialized"
    },
    {
        "messageId": "1d9efb558b6d4032b80bd70726b44c7e",
        "sessionId": "12345",
        "messageBody": "2023-02-24T18:26:47.399014200",
        "sessionState": "batch operation is done"
    },
    ...
]
```
