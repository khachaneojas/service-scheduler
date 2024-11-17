package com.sprk.service.scheduler.config;

import com.sprk.service.scheduler.properties.amqp.AMQPConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;


@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    public final AMQPConfigProperties amqpConfiguration;

    @Bean("standardQueue")
    public Queue standardQueue() {
        return new Queue(amqpConfiguration.getQueue().getStandard());
    }

    @Bean("mailerQueue")
    public Queue mailerQueue() {
        return new Queue(amqpConfiguration.getQueue().getMailer());
    }
    @Bean("releaseCriteriaMarkerQueue")
    public Queue releaseCriteriaMarkerQueue() {
        return new Queue(amqpConfiguration.getQueue().getReleaseCriteriaMarker());
    }
    @Bean("certificateReleaserQueue")
    public Queue certificateReleaserQueue() {
        return new Queue(amqpConfiguration.getQueue().getCertificateReleaser());
    }

    @Bean("updateExpiryStatusQueue")
    public Queue updateExpiryStatusQueue() {
        return new Queue(amqpConfiguration.getQueue().getUpdateExpiryStatus());
    }

    @Bean("expiryReminderMailQueue")
    public Queue expiryReminderMailQueue() {
        return new Queue(amqpConfiguration.getQueue().getExpiryReminderMail());
    }

    @Bean("updateStudentStatusQueue")
    public Queue updateStudentStatusQueue() {
        return new Queue(amqpConfiguration.getQueue().getUpdateStudentStatus());
    }

    @Bean("notifyBookingStartQueue")
    public Queue notifyBookingStartQueue() {
        return new Queue(amqpConfiguration.getQueue().getNotifyBookingStart());
    }


    @Bean("websiteDataTransferQueue")
    public Queue websiteDataTransferQueue(){
        return new Queue(amqpConfiguration.getQueue().getWebsiteDataTransfer());
    }





    @Bean("standardBinding")
    public Binding standardBinding(@Qualifier("standardQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getStandard());
    }
    @Bean("mailerBinding")
    Binding mailerBinding(@Qualifier("mailerQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getMailer());
    }
    @Bean("releaseCriteriaMarkerBinding")
    Binding releaseCriteriaMarkerBinding(@Qualifier("releaseCriteriaMarkerQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getReleaseCriteriaMarker());
    }

    @Bean("certificateReleaserBinding")
    Binding certificateReleaserBinding(@Qualifier("certificateReleaserQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getCertificateReleaser());
    }

    @Bean("updateExpiryStatusBinding")
    Binding updateExpiryStatusBinding(@Qualifier("updateExpiryStatusQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getUpdateExpiryStatus());
    }

    @Bean("expiryReminderMailBinding")
    Binding expiryReminderMailBinding(@Qualifier("expiryReminderMailQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getExpiryReminderMail());
    }

    @Bean("updateStudentStatusBinding")
    Binding updateStudentStatusBinding(@Qualifier("updateStudentStatusQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getUpdateStudentStatus());
    }

    @Bean("notifyBookingStartBinding")
    Binding notifyBookingStartBinding(@Qualifier("notifyBookingStartQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getNotifyBookingStart());
    }


    @Bean("websiteDataTransferBinding")
    Binding websiteDataTransferBinding(@Qualifier("websiteDataTransferQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange){
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getWebsiteDataTransfer());
    }



    @Bean("exchange")
    public TopicExchange exchange() {
        return new TopicExchange(amqpConfiguration.getExchange());
    }

    @Bean("converter")
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean("template")
    public AmqpTemplate template(ConnectionFactory connectionFactory, @Qualifier("converter") MessageConverter converter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }

}
