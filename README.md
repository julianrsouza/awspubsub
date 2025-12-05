# awspubsub

Esse projeto utiliza Quarkus juntamente com o Java 21, Postgresql e Maven para gerenciamento de dependências.

Ele possui três módulos:

Core:
Possui a entidade e repository para acesso e persistência dos dados. Este é compartilhado pelo Producer e Consumer.

Producer:
Possui endpoint POST HTTP que recebe um JSON com uma mensagem no seguinte formato:
{
  "text": "Teste AWS SNS → SQS6"
}

Obtem o texto da mensagem e envia a um tópico SNS já criado na conta da AWS.

Consumer:
Realiza um poll na fila SQS a cada 30 segundos, tempo pode ser definido no application.properties deste módulo, faz o processamento das mensagens e persiste no banco removendo da fila SQS para não haver reprocessamento duplicado.

Para subir os módulos, basta ir até o diretório de cada um e rodar o comando mvn quarkus:dev.