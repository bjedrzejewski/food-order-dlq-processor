package com.e4developer.foodorderdlqprocessor;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.messaging.HeaderBasedMessagingExtractor;
import org.springframework.integration.config.EnableIntegration;

@EnableIntegration
@SpringBootApplication
public class FoodOrderDlqProcessorApplication {

	private static final String ORIGINAL_QUEUE = "foodOrders.foodOrdersIntakeGroup";

	private static final String DLQ = ORIGINAL_QUEUE + ".dlq";

	private static final String X_RETRIES_HEADER = "x-retries";

	public static void main(String[] args) {
		SpringApplication.run(FoodOrderDlqProcessorApplication.class, args);
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	Tracer tracer;

	@RabbitListener(queues = DLQ)
	public void rePublish(Message failedMessage) {
		HeaderBasedMessagingExtractor headerBasedMessagingExtractor = new HeaderBasedMessagingExtractor();
		MySpanTextMap entries = new MySpanTextMap(failedMessage.getMessageProperties().getHeaders());
		Span span = headerBasedMessagingExtractor.joinTrace(entries);
		Span mySpan = tracer.createSpan(":rePublish", span);

		failedMessage = attemptToRepair(failedMessage);

		Integer retriesHeader = (Integer) failedMessage.getMessageProperties().getHeaders().get(X_RETRIES_HEADER);
		if (retriesHeader == null) {
			retriesHeader = Integer.valueOf(0);
		}
		if (retriesHeader < 3) {
			failedMessage.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, retriesHeader + 1);
			this.rabbitTemplate.send(ORIGINAL_QUEUE, failedMessage);
		}
		else {
			System.out.println("Writing to databse: "+failedMessage.toString());
			//we can write to a database or move to a parking lot queue
		}
		tracer.close(mySpan);
	}

	private Message attemptToRepair(Message failedMessage) {
		String messageBody = new String(failedMessage.getBody());

		if(messageBody.contains("vegetables")) {
			System.out.println("Repairing message: "+failedMessage.toString());
			messageBody = messageBody.replace("vegetables", "cakes");
			return MessageBuilder.withBody(messageBody.getBytes()).copyHeaders(failedMessage.getMessageProperties().getHeaders()).build();
		}
		return failedMessage;
	}

}
