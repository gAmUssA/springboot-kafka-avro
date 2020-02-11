package io.confluent.developer.kafkaworkshop.streams;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import io.confluent.developer.User;
import lombok.extern.apachecommons.CommonsLog;

import static io.confluent.developer.kafkaworkshop.streams.Bindings.USERS;
import static io.confluent.developer.kafkaworkshop.streams.Bindings.USERS_FILTERED;

@EnableBinding(Bindings.class)
@SpringBootApplication
public class KafkaStreamsApp {

  @Value("${topic.name}-filtered")
  private String topicName;

  @Value("${topic.partitions-num}")
  private Integer partitions;

  @Value("${topic.replication-factor}")
  private short replicationFactor;


  public static void main(String[] args) {
    final SpringApplication application = new SpringApplication(KafkaStreamsApp.class);
    // we don't need web/rest interface in this app 
    // but CF has default health check that will hit http port
    // https://docs.cloudfoundry.org/devguide/deploy-apps/healthchecks.html#understand-healthchecks
    application.setWebApplicationType(WebApplicationType.SERVLET);
    application.run(args);
  }

  @Bean
  NewTopic filteredTopic() {
    return new NewTopic(topicName, partitions, replicationFactor);
  }
}

interface Bindings {

  String USERS_FILTERED = "users-filtered";
  String USERS = "users";

  @Input(USERS)
  KStream<String, User> usersI();

  @Output(USERS_FILTERED)
  KStream<String, User> filteredUsers();
}

@Component
@CommonsLog(topic = "Streams Logger")
class UserProcessor {

  @StreamListener
  @SendTo(USERS_FILTERED)
  KStream<String, User> processUsers(@Input(USERS) KStream<String, User> inputStream) {
    return inputStream
        .filter((key, user) -> user.getAge() < 40)
        .mapValues(user -> new User(user.getName().toUpperCase(), user.getAge()))
        .peek((key, user) -> log.info("New entry in filtered stream => Key = " + key + " Value = " + user));
  }
}