/*
 * Copyright 2014 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.servlet.filter;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.AccountStatus;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.authc.AuthenticationResultVisitor;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.resource.ResourceException;
import com.stormpath.sdk.servlet.account.DefaultAccountResolver;
import com.stormpath.sdk.servlet.account.event.RegisteredAccountRequestEvent;
import com.stormpath.sdk.servlet.account.event.impl.DefaultRegisteredAccountRequestEvent;
import com.stormpath.sdk.servlet.config.Config;
import com.stormpath.sdk.servlet.csrf.CsrfTokenManager;
import com.stormpath.sdk.servlet.event.RequestEvent;
import com.stormpath.sdk.servlet.event.impl.Publisher;
import com.stormpath.sdk.servlet.form.DefaultField;
import com.stormpath.sdk.servlet.form.DefaultForm;
import com.stormpath.sdk.servlet.form.Field;
import com.stormpath.sdk.servlet.form.Form;
import com.stormpath.sdk.servlet.http.Saver;
import com.stormpath.sdk.servlet.util.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegisterFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(RegisterFilter.class);

    public static final String FIELD_CONFIG_NAME_PREFIX = "stormpath.web.register.form.fields.";
    public static final String ACCOUNT_SAVER_PROP = "stormpath.web.authc.saver";
    public static final String CSRF_TOKEN_MANAGER = "stormpath.web.csrf.token.manager";
    public static final String EVENT_PUBLISHER = "stormpath.web.request.event.publisher";
    public static final String VIEW_TEMPLATE_PATH = "/WEB-INF/jsp/register.jsp";
    public static final String VERIFY_EMAIL_VIEW_TEMPLATE_PATH = "/WEB-INF/jsp/verifyEmail.jsp";

    //only used if account does not need email verification:
    private Saver<AuthenticationResult> authenticationResultSaver;
    private CsrfTokenManager csrfTokenManager;
    private Publisher<RequestEvent> eventPublisher;

    @Override
    protected void onInit() throws ServletException {
        this.authenticationResultSaver = getConfig().getInstance(ACCOUNT_SAVER_PROP);
        this.csrfTokenManager = getConfig().getInstance(CSRF_TOKEN_MANAGER);
        this.eventPublisher = getConfig().getInstance(EVENT_PUBLISHER);
    }

    /**
     * Returns the context-relative URL where a user can be redirected to login.
     *
     * @return the context-relative URL where a user can be redirected to login.
     */
    public String getRegisterUrl() {
        return getConfig().getRegisterUrl();
    }

    public String getRegisterNextUrl() {
        return getConfig().getRegisterNextUrl();
    }

    public Publisher<RequestEvent> getEventPublisher() {
        return this.eventPublisher;
    }

    @Override
    protected void filter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws Exception {

        String method = request.getMethod();

        if ("GET".equalsIgnoreCase(method)) {
            doGet(request, response);
        } else if ("POST".equalsIgnoreCase(method)) {
            doPost(request, response);
        } else {
            ServletUtils.issueRedirect(request, response, getRegisterUrl(), null, true, true);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        //addConfigProperties(request);
        Form form = createForm(request, false);
        ((DefaultForm)form).setCsrfToken(csrfTokenManager.createCsrfToken(request, response));
        setForm(request, form);
        request.getRequestDispatcher(VIEW_TEMPLATE_PATH).forward(request, response);
    }

    protected Form createForm(HttpServletRequest request, boolean retainPassword) {

        DefaultForm form = new DefaultForm();

        String value = Strings.clean(request.getParameter("csrfToken"));
        form.setCsrfToken(value);

        value = Strings.clean(request.getParameter("next"));
        if (value != null) {
            form.setNext(value);
        }

        Config config = getConfig();

        for (String key : config.keySet()) {

            if (key.startsWith(FIELD_CONFIG_NAME_PREFIX)) {

                value = config.get(key);

                if (value.equalsIgnoreCase("disabled")) {
                    //don't include it in the form:
                    continue;
                }

                boolean required = value.equalsIgnoreCase("required");

                String fieldName = key.substring(FIELD_CONFIG_NAME_PREFIX.length());

                DefaultField field = new DefaultField();
                field.setName(fieldName);
                field.setRequired(required);
                field.setType("text");
                String param = request.getParameter(fieldName);
                field.setValue(param != null ? param : "");

                String label;

                if ("username".equals(fieldName)) {
                    label = "Username";
                } else if ("email".equals(fieldName)) {
                    label = "Email";
                } else if ("givenName".equals(fieldName)) {
                    label = "First Name";
                } else if ("middleName".equals(fieldName)) {
                    label = "Middle Name";
                } else if ("surname".equals(fieldName)) {
                    label = "Last Name";
                } else if ("password".equals(fieldName)) {
                    field.setType("password");
                    label = "Password";
                    if (!retainPassword) {
                        field.setValue("");
                    }
                } else {
                    //unrecognized property
                    continue;
                }

                field.setLabel(label).setPlaceholder(label);

                form.addField(field);
            }
        }

        form.autofocus();

        return form;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        Form form = createForm(request, true);

        try {
            register(request, response, form);
        } catch (Exception e) {
            log.debug("Unable to register account.", e);

            List<String> errors = new ArrayList<String>();

            if (e instanceof ResourceException) {
                errors.add(((ResourceException) e).getStormpathError().getMessage());
            } else {
                errors.add("Oops! Unable to register you!  Please contact support.");
                log.warn("Unable to resister user account: " + e.getMessage(), e);
            }
            request.setAttribute("errors", errors);

            //do not retain submitted password (not safe to have in the DOM text):
            ((DefaultField) form.getField("password")).setValue("");
            setForm(request, form);

            //ensure new csrf token is used:
            ((DefaultForm)form).setCsrfToken(csrfTokenManager.createCsrfToken(request, response));

            request.getRequestDispatcher(VIEW_TEMPLATE_PATH).forward(request, response);
        }
    }

    protected void setForm(HttpServletRequest request, Form form) {
        request.setAttribute("form", form);
    }

    @SuppressWarnings("UnusedParameters")
    protected Account newAccount(HttpServletRequest request) {
        Client client = getClient();
        return client.instantiate(Account.class);
    }

    protected String getValue(Form form, String fieldName) {
        Field field = form.getField(fieldName);
        if (field != null) {
            return getValue(field);
        }
        return null;
    }

    protected String getValue(Field field) {
        return Strings.clean(field.getValue());
    }

    protected void validate(HttpServletRequest request, HttpServletResponse response, Form form)
        throws ServletException, IOException {

        validateCsrfToken(request, response, form);

        //ensure required fields are present:
        List<Field> fields = form.getFields();
        for (Field field : fields) {
            if (field.isRequired()) {
                String value = getValue(field);
                if (value == null) {
                    //TODO: i18n
                    throw new IllegalArgumentException(Strings.capitalize(field.getName()) + " is required.");
                }
            }
        }
    }

    protected void validateCsrfToken(HttpServletRequest request, HttpServletResponse response, Form form) {
        String csrfToken = form.getCsrfToken();
        Assert.isTrue(csrfTokenManager.isValidCsrfToken(request, response, csrfToken), "Invalid CSRF token");
    }

    protected void register(HttpServletRequest req, HttpServletResponse resp, Form form)
        throws ServletException, IOException {

        validate(req, resp, form);

        //Create a new Account instance that will represent the submitted user information:
        Account account = newAccount(req);

        String value = getValue(form, "email");
        if (value != null) {
            account.setEmail(value);
        }

        value = getValue(form, "username");
        if (value != null) {
            account.setUsername(value);
        }

        value = getValue(form, "password");
        if (value != null) {
            account.setPassword(value);
        }

        value = getValue(form, "givenName");
        account.setGivenName(value != null ? value : "UNSPECIFIED");

        value = getValue(form, "middleName");
        if (value != null) {
            account.setMiddleName(value);
        }

        value = getValue(form, "surname");
        account.setSurname(value != null ? value : "UNSPECIFIED");

        //Get the Stormpath Application instance corresponding to this web app:
        Application app = (Application)req.getAttribute(Application.class.getName());

        //now persist the new account, and ensure our account reference points to the newly created/returned instance:
        account = app.createAccount(account);

        AccountStatus status = account.getStatus();

        RequestEvent e = createRegisteredEvent(req, resp, account);
        publish(e);

        if (status == AccountStatus.ENABLED) {

            //the user does not need to verify their email address, so just assume they are authenticated
            //(since they specified their password during registration):

            final Account theAccount = account;
            this.authenticationResultSaver.set(req, resp, new AuthenticationResult() {
                @Override
                public Account getAccount() {
                    return theAccount;
                }

                @Override
                public void accept(AuthenticationResultVisitor visitor) {
                    visitor.visit(this);

                }

                @Override
                public String getHref() {
                    return null;
                }
            });

        } else if (status == AccountStatus.UNVERIFIED) {

            // The user must verify their email address:
            //
            // NOTE: we don't use the authenticationResultSaver here because we don't want the identity
            // to be retained after this request.  To avoid security attack vectors, the end-user must
            // verify their email address and then explicitly authenticate after doing so.  At that time,
            // the authentication state will be saved appropriately via the login filter.
            //
            // We set the account as a request attribute here only for the remainder of the request in case the
            // verifyEmail.jsp view wants to use it to render a user-specific message, for example,
            //
            // 'Thanks for registering Joe!  Almost done - please verify your email...'
            //
            req.setAttribute(DefaultAccountResolver.REQUEST_ATTR_NAME, account);
            req.getRequestDispatcher(VERIFY_EMAIL_VIEW_TEMPLATE_PATH).forward(req, resp);
            return;
        }

        //finish up by showing the 'post register' view:
        String next = form.getNext();

        if (!Strings.hasText(next)) {
            next = getRegisterNextUrl();
        }

        ServletUtils.issueRedirect(req, resp, next, null, true, true);
    }

    protected RegisteredAccountRequestEvent createRegisteredEvent(HttpServletRequest request,
                                                                  HttpServletResponse response,
                                                                  Account account) {
        return new DefaultRegisteredAccountRequestEvent(request, response, account);
    }

    protected void publish(RequestEvent e) throws ServletException {
        try {
            getEventPublisher().publish(e);
        } catch (Exception ex) {
            String msg = "Unable to publish registered account request event: " + ex.getMessage();
            throw new ServletException(msg, ex);
        }
    }
}
