package de.turing85.quarkus.camel.hashtable.deserialization;

import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.ConnectionFactory;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import io.quarkus.logging.Log;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.angus.mail.util.BASE64DecoderStream;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.jms;

@ApplicationScoped
@RequiredArgsConstructor
public class MyRoute extends RouteBuilder {
  private final ConnectionFactory connectionFactory;

  @Override
  public void configure() {
    // @formatter:off
    from(
        jms("topic:data::data")
            .connectionFactory(connectionFactory)).log("${body}")
        .process(exchange -> {
          MimeMultipart multipart =
              new MimeMultipart(new ByteArrayDataSource(exchange.getMessage().getBody(byte[].class),
                  "application/octet-stream"));
          for (int idx = 0; idx < multipart.getCount(); ++idx) {
            BodyPart bodyPart = multipart.getBodyPart(idx);
            String[] contentIdHeaders = bodyPart.getHeader("Content-ID");
            if (Objects.nonNull(contentIdHeaders) && contentIdHeaders.length > 0
                && Objects.equals("table", contentIdHeaders[0])) {
              BASE64DecoderStream ds = (BASE64DecoderStream) bodyPart.getContent();
              ObjectInputStream ois = new ObjectInputStream(ds);
              @SuppressWarnings("unchecked")
              Hashtable<String, String> table = (Hashtable<String, String>) ois.readObject();
              Log.infof("table: %s", table);
            }
          }
        });
    // @formatter:on
  }
}
