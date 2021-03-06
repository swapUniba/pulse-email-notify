package com.github.frapontillo.pulse.email;

import com.github.frapontillo.pulse.util.PulseLogger;
import com.github.frapontillo.pulse.rx.PulseSubscriber;
import com.github.frapontillo.pulse.spi.IPlugin;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.logging.log4j.Logger;
import rx.Observable;

/**
 * Plugin that notifies one or more email addresses of the completion or erroring of the pipeline.
 *
 * @author Francesco Pontillo
 * @see EmailNotifierConfig to learn how to configure the plugin.
 */
public class EmailNotifier extends IPlugin<Object, Object, EmailNotifierConfig> {
    private static final String PLUGIN_NAME = "email-notifier";
    private static final Logger logger = PulseLogger.getLogger(EmailNotifier.class);

    @Override public String getName() {
        return PLUGIN_NAME;
    }

    @Override public EmailNotifierConfig getNewParameter() {
        return new EmailNotifierConfig();
    }

    @Override protected Observable.Operator<Object, Object> getOperator(EmailNotifierConfig parameters) {
        return subscriber -> new PulseSubscriber<Object>(subscriber) {
            @Override public void onNext(Object o) {
                subscriber.onNext(o);
            }

            @Override public void onCompleted() {
                if (parameters.getNotifySuccess()) {
                    sendEmail(parameters, true);
                }
                super.onCompleted();
            }

            @Override public void onError(Throwable e) {
                if (parameters.getNotifyError()) {
                    sendEmail(parameters, false);
                }
                super.onError(e);
            }
        };
    }

    /**
     * Send an email, using the provided parameters, notifying if the pipeline succeeded or errored.
     *
     * @param parameters The {@link EmailNotifierConfig} to use.
     * @param isSuccess  {@code true} to report a success, {@code false} to report an error.
     */
    private void sendEmail(EmailNotifierConfig parameters, boolean isSuccess) {
        if (parameters.getAddresses() == null || parameters.getAddresses().length == 0) {
            return;
        }
        try {
            Email email = new SimpleEmail();
            email.setHostName(parameters.getHost());
            email.setSmtpPort(parameters.getPort());
            email.setAuthenticator(new DefaultAuthenticator(parameters.getUsername(), parameters.getPassword()));
            email.setSSLOnConnect(parameters.getUseSsl());
            email.setFrom(parameters.getFrom());
            email.setSubject(parameters.getSubject());
            String body;
            if (isSuccess) {
                body = parameters.getBodySuccess();
            } else {
                body = parameters.getBodyError();
            }
            body = body.replace("{{NAME}}", getProcessInfo().getName());
            email.setMsg(body);
            email.addTo(parameters.getAddresses());
            email.send();
        } catch (EmailException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }
}
