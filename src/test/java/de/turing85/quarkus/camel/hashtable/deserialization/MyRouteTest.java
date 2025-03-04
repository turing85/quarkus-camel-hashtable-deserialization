package de.turing85.quarkus.camel.hashtable.deserialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Hashtable;

import jakarta.activation.DataHandler;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Topic;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MyRouteTest {
  @Test
  void test() throws MessagingException, IOException, InterruptedException {
    MimeMultipart multipart = new MimeMultipart();
    MimeBodyPart zeroBodyPart = new MimeBodyPart();
    zeroBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(
        "Hello, world!".getBytes(StandardCharsets.UTF_8), "application/octet-stream")));
    zeroBodyPart.setHeader("Content-Type", "text/plain");
    multipart.addBodyPart(zeroBodyPart);
    MimeBodyPart oneBodyPart = new MimeBodyPart();
    Hashtable<String, String> table = new Hashtable<>();
    table.put("foo", "bar");
    table.put("baz", "bang");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(table);
    oneBodyPart.setDataHandler(
        new DataHandler(new ByteArrayDataSource(baos.toByteArray(), "application/octet-stream")));
    oneBodyPart.setHeader("Content-Type", "application/octet-stream");
    oneBodyPart.setHeader("Content-Transfer-Encoding", "base64");
    oneBodyPart.setHeader("Content-ID", "table");
    multipart.addBodyPart(oneBodyPart);
    try (
        ActiveMQConnectionFactory connectionFactory =
            new ActiveMQConnectionFactory("tcp://localhost:5432", "artemis", "artemis");
        JMSContext context = connectionFactory.createContext()) {
      JMSProducer producer = context.createProducer();
      Topic topic = context.createTopic("data");
      ByteArrayOutputStream multipartOut = new ByteArrayOutputStream();
      multipart.writeTo(multipartOut);
      producer.send(topic, multipartOut.toByteArray());
    }
    Thread.sleep(Duration.ofSeconds(5).toMillis());
  }
}
