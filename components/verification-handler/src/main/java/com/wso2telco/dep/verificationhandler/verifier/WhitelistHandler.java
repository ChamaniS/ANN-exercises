/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 *
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.dep.verificationhandler.verifier;

import com.google.gson.Gson;
import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.verificationhandler.model.payment.PaymentRequestWrap;
import com.wso2telco.dep.verificationhandler.model.smsmessaging.SMSMessagingRequestWrap;
import com.wso2telco.dep.verificationhandler.model.ussd.InboundUSSDMessageRequestWrap;
import com.wso2telco.dep.verificationhandler.model.ussd.OutboundUSSDRequestWrap;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.wso2.carbon.apimgt.gateway.handlers.Utils;
import org.wso2.carbon.apimgt.gateway.handlers.security.APISecurityConstants;

import javax.naming.NamingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// TODO: Auto-generated Javadoc

/**
 * The Class WhitelistHandler.
 */
public class WhitelistHandler extends AbstractHandler implements ManagedLifecycle {

	/**
	 * The Constant log.
	 */
	private static final Log log = LogFactory.getLog(WhitelistHandler.class);

	/**
	 * The whitelist numbers.
	 */
	private List<String> whitelistNumbers;

	/**
	 * The subscription list.
	 */
	private List<String> subscriptionList;

	static final String URL_MSISDN_NOT_MATCHED ="URL MSISDN not match Body ";

	//Entry point to the White list Mediator
	public boolean handleRequest(MessageContext messageContext) {

		ValidationRegexDTO validationRegexDTO = null;

		try {
			validationRegexDTO = ValidationRegexClient.getValidationRegex();
		} catch (BusinessException e) {
			log.error("Error in retrieving validation Regex");
			return false;
		}

		try {


			String resourceUrl = (String) messageContext.getProperty("REST_FULL_REQUEST_PATH");

			String msisdn = null;
			ArrayList<String> msisdns = null;

			String apiName = APIUtil.getAPI(messageContext);

			if (apiName == null) {
				return true;
			}

			String apiVersion = (String) messageContext.getProperty("api.ut.version");//


			if (apiName.equalsIgnoreCase(APINameConstant.PAYMENT)) {
				//msisdn = str_piece(resourceUrl, '/', 4);
				String urlMSISDN = null;

				try {
					String rgx = ".*/(.+?)/transactions";
					Pattern pattern = Pattern.compile(rgx);
					Matcher matcher = pattern.matcher(resourceUrl);
					matcher.find();
					urlMSISDN = (matcher.group(1));
				} catch (Exception e) {
					log.error("RegX: " + e.getMessage(), e);
				}

				if (resourceUrl.contains("transactions/amount/balance")) {
					log.info("Payment : Balance");

					msisdn = urlMSISDN;

				} else {
					log.info("Payment : amount");

					Gson gson = new Gson();
					try {

						Mediator sequence = messageContext.getSequence("_build_");
						sequence.mediate(messageContext);
						String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext)
								messageContext).getAxis2MessageContext());

						PaymentRequestWrap payment = gson.fromJson(jsonPayloadToString, PaymentRequestWrap.class);
						msisdn = payment.getAmountTransaction().getEndUserId();

						if (!FormatMsisdn.getInstance().splitMsisdn(msisdn).equalsIgnoreCase(FormatMsisdn.getInstance().splitMsisdn(urlMSISDN))) {
							log.warn(URL_MSISDN_NOT_MATCHED + urlMSISDN + ":" + msisdn);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}


			} else if (apiName.equalsIgnoreCase(APINameConstant.LOCATION)) {
				//Retrieving MSISDN from the incoming request
				log.info("Location");
//                msisdn = str_piece(str_piece(resourceUrl, '=', 2), '&', 1);
//                msisdn=filterMSISDN(msisdn);

				try {
					String rgx = ".*address=(.+?)(&.*|$)";
					Pattern pattern = Pattern.compile(rgx);
					Matcher matcher = pattern.matcher(resourceUrl);
					matcher.find();
					msisdn = (matcher.group(1));
				} catch (Exception e) {
					log.error("RegX: " + e.getMessage(), e);
				}

			} else if (apiName.equalsIgnoreCase(APINameConstant.MESSAGING)) {
				log.info("Messaging");

				String urlMSISDN = null;

				try {
					String rgx = ".*/outbound/(.+?)/requests";
					Pattern pattern = Pattern.compile(rgx);
					Matcher matcher = pattern.matcher(resourceUrl);
					matcher.find();
					urlMSISDN = (matcher.group(1));
				} catch (Exception e) {
					//log.error("RegX: " + e.getMessage(), e);
				}

				//bypass whitelist for other API call in SMS messaging.
				// Only need check whitelist for outbound request
				if (urlMSISDN == null) {
					log.info("Whitelist : [MESSAGING] NOT A SMS Request : " + resourceUrl);
					return true;
				}

				Gson gson = new Gson();

				try {
					Mediator sequence = messageContext.getSequence("_build_");
					sequence.mediate(messageContext);
					String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) messageContext)
                            .getAxis2MessageContext());

					SMSMessagingRequestWrap sms = gson.fromJson(jsonPayloadToString, SMSMessagingRequestWrap.class);
					msisdn = sms.getOutboundSMSMessageRequest().getAddress().get(0);

					if (!FormatMsisdn.getInstance().splitMsisdn(msisdn).equalsIgnoreCase(FormatMsisdn.getInstance().splitMsisdn(urlMSISDN))) {
						log.warn(URL_MSISDN_NOT_MATCHED + urlMSISDN + ":" + msisdn);
					}

					if (sms.getOutboundSMSMessageRequest().getAddress().size() > 0)
						msisdns = sms.getOutboundSMSMessageRequest().getAddress();

				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (apiName.equalsIgnoreCase(APINameConstant.USSD)) {
				log.info("USSD");
				String urlMSISDN = null;

				if (resourceUrl.contains("outbound") && !resourceUrl.contains("subscriptions")) {

					try {
						String rgx = ".*/outbound/(.+?).$";
						Pattern pattern = Pattern.compile(rgx);
						Matcher matcher = pattern.matcher(resourceUrl);
						matcher.find();
						urlMSISDN = (matcher.group(1));
					} catch (Exception e) {
						log.error("RegX: " + e.getMessage(), e);
					}

					Gson gson = new Gson();
					try {
						Mediator sequence = messageContext.getSequence("_build_");
						sequence.mediate(messageContext);
						String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext)
                                messageContext).getAxis2MessageContext());
						OutboundUSSDRequestWrap ussd = gson.fromJson(jsonPayloadToString, OutboundUSSDRequestWrap
                                .class);
						msisdn = ussd.getOutboundUSSDMessageRequest().getAddress();

						if (!FormatMsisdn.getInstance().splitMsisdn(msisdn).equalsIgnoreCase(FormatMsisdn.getInstance().splitMsisdn(urlMSISDN))) {
							log.warn(URL_MSISDN_NOT_MATCHED + urlMSISDN + ":" + msisdn);
						}
					} catch (Exception e) {
						log.error("Whitelist mediation handling failed: " + e.getMessage(), e);
					}
				} else if (resourceUrl.contains("inbound") && !resourceUrl.contains("subscriptions")) {

					try {
						String rgx = ".*/inbound/(.+?).$";
						Pattern pattern = Pattern.compile(rgx);
						Matcher matcher = pattern.matcher(resourceUrl);
						matcher.find();
						urlMSISDN = (matcher.group(1));
					} catch (Exception e) {
						log.error("RegX: " + e.getMessage(), e);
					}

					Gson gson = new Gson();
					try {
						Mediator sequence = messageContext.getSequence("_build_");
						sequence.mediate(messageContext);
						String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext)
                                messageContext).getAxis2MessageContext());
						InboundUSSDMessageRequestWrap ussd = gson.fromJson(jsonPayloadToString,
                                InboundUSSDMessageRequestWrap.class);
						msisdn = ussd.getInboundUSSDMessageRequest().getAddress();

						if (!FormatMsisdn.getInstance().splitMsisdn(msisdn).equalsIgnoreCase(FormatMsisdn.getInstance().splitMsisdn(urlMSISDN))) {
							log.warn(URL_MSISDN_NOT_MATCHED + urlMSISDN + ":" + msisdn);
						}
					} catch (Exception e) {
						log.error("Whitelist mediation handling failed: " + e.getMessage(), e);
					}
				}
/*
                try {
                    String rgx = ".*//*
outbound/(.+?).$";
                    Pattern pattern = Pattern.compile(rgx);
                    Matcher matcher = pattern.matcher(resourceUrl);
                    matcher.find();
                    urlMSISDN = (matcher.group(1));
                } catch (Exception e) {
                    log.error("RegX: " + e.getMessage(), e);
                }
                Gson gson = new Gson();
                try {
                    Mediator sequence = messageContext.getSequence("_build_");
                    sequence.mediate(messageContext);
                    String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) messageContext)
                    .getAxis2MessageContext());
                    OutboundUSSDRequestWrap ussd = gson.fromJson(jsonPayloadToString, OutboundUSSDRequestWrap.class);
                    msisdn = ussd.getOutboundUSSDMessageRequest().getAddress();

                    if (!filterMSISDN(msisdn).equals(filterMSISDN(urlMSISDN))) {
                        log.warn("URL MSISDN not match Body " + urlMSISDN + ":" + msisdn);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
*/
			} else if (apiName.equalsIgnoreCase(APINameConstant.BALANCECHECK)) {
				//Retrieving MSISDN from the incoming request
				log.info("balance Check");
				try {
					String rgx = ".*/(.+?)/transactions";
					Pattern pattern = Pattern.compile(rgx);
					Matcher matcher = pattern.matcher(resourceUrl);
					matcher.find();
					msisdn = (matcher.group(1));
					msisdn = FormatMsisdn.getInstance().splitMsisdn(msisdn);
				} catch (Exception e) {
					log.error("RegX: " + e.getMessage(), e);
				}

			} else {
				//nop
			}
			log.info("MSISDN : " + msisdn);

			String msisdnRegex = validationRegexDTO.getValidationRegex();

			Pattern pattern = Pattern.compile(msisdnRegex);
			Matcher matcher = pattern.matcher(msisdn);

			String formattedNumber = null;
			if (matcher.matches()) {
				formattedNumber = matcher.group(Integer.parseInt(validationRegexDTO.getDigitsGroup()));
			} else {
				log.error("Entered Msisdn does not match with given regex:" + validationRegexDTO.getValidationRegex());
				handleValidationResponse(messageContext, validationRegexDTO.getValidationRegex());
				return false;
			}

			String appID = messageContext.getProperty("api.ut.application.id").toString();

			log.info("App ID : " + appID);
			//Retrieving API Name
			// String api = messageContext.getProperty("api.ut.api").toString();
			String apiID = null;
			//String subscriptionID = null;

			try {
				//Retrieving API ID
				String apiContext = (String) messageContext.getProperty("REST_API_CONTEXT");
				apiID = DatabaseUtils.getAPIId(apiContext, apiVersion);
			} catch (NamingException ex) {
				Logger.getLogger(WhitelistHandler.class.getName()).log(Level.SEVERE, null, ex);
			} catch (SQLException ex) {
				Logger.getLogger(WhitelistHandler.class.getName()).log(Level.SEVERE, null, ex);
			}

			int subscriptionID = getSubscriptionID(apiID, appID);
			String subscriptionIDstring = null;

			if (subscriptionID != -1) {
				subscriptionIDstring = String.valueOf(subscriptionID);
			}

			if (!isWhiteListedNumber(formattedNumber, appID, subscriptionIDstring, apiID)) {
				log.info("Not a WhiteListed number");
				hadleNonWhiteListedResponse(messageContext);
			} else if (msisdns != null) {

				//check all numbers are whitelisted
				log.info("Multiple MSISDN");
				for (String cur : msisdns) {
					Matcher numbers = pattern.matcher(cur);
					if (numbers.matches()) {
						cur = numbers.group(Integer.parseInt(validationRegexDTO.getDigitsGroup()));
					} else {
						log.error("Entered Msisdn does not match with given regex:" + validationRegexDTO.getValidationRegex());
						handleValidationResponse(messageContext, validationRegexDTO.getValidationRegex());
						return false;
					}
					if (!isWhiteListedNumber(cur, appID, subscriptionIDstring, apiID)) {
						log.info("Not a WhiteListed number");
						hadleNonWhiteListedResponse(messageContext);
						break;
					}
				}
			}
		} catch (Exception err) {
			log.error("Whitelist error : " + err.getMessage());
			log.error("Whitelist error : ", err);
		}
		return true;
	}


	public boolean handleResponse(MessageContext messageContext) {

		return true;
	}

	/**
	 * Hadle non white listed response.
	 *
	 * @param messageContext the message context
	 */
	//Sending the Response back to Client
	private void hadleNonWhiteListedResponse(MessageContext messageContext) {
		messageContext.setProperty(SynapseConstants.ERROR_CODE, "500");
		messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, "Internal Server Error. Not a whiteListed Number");
		int status = 500;

		org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
				getAxis2MessageContext();
		//  axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/json");

		if (messageContext.isDoingPOX() || messageContext.isDoingGET()) {
			Utils.setFaultPayload(messageContext, getFaultPayload());

		} else {
			Utils.setSOAPFault(messageContext, "Client", "Authentication Failure", "Not a whiteListed Number");
		}

		messageContext.setProperty("error_message_type", "application/json");

		Utils.sendFault(messageContext, status);
	}


	/**
	 * Handling Validation Response
	 * @param messageContext
	 * @param message
	 */
	private void handleValidationResponse(MessageContext messageContext, String message) {
		messageContext.setProperty(SynapseConstants.ERROR_CODE, "500");
		messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, "Internal Server Error. Not a whiteListed Number");
		int status = 500;

		if (messageContext.isDoingPOX() || messageContext.isDoingGET()) {
			Utils.setFaultPayload(messageContext, PayloadFactory.getInstance().getErrorPayload(status, message, "Validation Regex Does not matched"));

		} else {
			Utils.setSOAPFault(messageContext, "Client", "Authentication Failure", "Not a whiteListed Number");
		}

		messageContext.setProperty("error_message_type", "application/json");

		Utils.sendFault(messageContext, status);
	}


	/**
	 * Checks if is white listed number.
	 *
	 * @param msisdn the msisdn
	 * @return true, if is white listed number
	 */
	private boolean isWhiteListedNumber(String msisdn) {

		return whitelistNumbers.contains(msisdn);
	}

	/**
	 * Checks if is white listed number.
	 *
	 * @param MSISDN         the msisdn
	 * @param applicationId  the application id
	 * @param subscriptionId the subscription id
	 * @param apiId          the api id
	 * @return true, if is white listed number
	 * @throws SQLException    the SQL exception
	 * @throws NamingException the naming exception
	 */
	public static boolean isWhiteListedNumber(String MSISDN, String applicationId, String subscriptionId, String
            apiId) throws SQLException, NamingException {
		WhiteListResult whiteListResult = DatabaseUtils.checkWhiteListed(MSISDN, applicationId, subscriptionId, apiId);
		if (whiteListResult == null) {
			log.info("Whitelist not : App " + applicationId + ", API " + apiId + ", Subscription : " + subscriptionId
                    + " , MSISDN " + MSISDN);
			return false;
		} else {
			log.info("Whitelist match : App " + applicationId + ", API " + apiId + ", Subscription : " +
                    subscriptionId + " , MSISDN " + MSISDN);
			return true;

		}


	}

	public static int getSubscriptionID(String apiId, String applicationId)
			throws SQLException, NamingException {

		return DatabaseUtils.getSubscriptionId(apiId, applicationId);
	}


	/**
	 * Filter msisdn.
	 *
	 * @param msisdn the msisdn
	 * @return the string
	 * @deprecated
	 */
	@Deprecated
	private static String filterMSISDN(String msisdn) {
		if (msisdn == null)
			return null;
		msisdn = msisdn.replace("tel:+", "");
		msisdn = msisdn.replace("tel:", "");
		msisdn = msisdn.replace("tel", "");
		msisdn = msisdn.replace("+", "");
		msisdn = msisdn.replace("%3A%2B", "");
		return msisdn;
	}

	/**
	 * Checks if is already subscribed.
	 *
	 * @param msisdn the msisdn
	 * @return true, if is already subscribed
	 */
	private boolean isAlreadySubscribed(String msisdn) {

		return subscriptionList.contains(msisdn);
	}

	/**
	 * Gets the fault payload.
	 *
	 * @return the fault payload
	 */
	//Constructing the Payload
	private OMElement getFaultPayload() {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace ns = fac.createOMNamespace(APISecurityConstants.API_SECURITY_NS,
				APISecurityConstants.API_SECURITY_NS_PREFIX);
		OMElement payload = fac.createOMElement("fault", ns);

		OMElement errorCode = fac.createOMElement("code", ns);
		errorCode.setText("404");
		OMElement errorMessage = fac.createOMElement("message", ns);
		errorMessage.setText("Number is invalid, Number is not whitelisted");
		OMElement errorDetail = fac.createOMElement("description", ns);
		errorDetail.setText("Not a Whitelisted Number");

		payload.addChild(errorCode);
		payload.addChild(errorMessage);
		payload.addChild(errorDetail);
		return payload;
	}

	/**
	 * Str_piece.
	 *
	 * @param str       the str
	 * @param separator the separator
	 * @param index     the index
	 * @return the string
	 */
	private static String str_piece(String str, char separator, int index) {
		String str_result = "";
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == separator) {
				count++;
				if (count == index) {
					break;
				}
			} else {
				if (count == index - 1) {
					str_result += str.charAt(i);
				}
			}
		}
		return str_result;
	}


	/* (non-Javadoc)
	 * @see org.apache.synapse.ManagedLifecycle#init(org.apache.synapse.core.SynapseEnvironment)
	 */
	public void init(SynapseEnvironment synapseEnvironment) {


	}

	/* (non-Javadoc)
	 * @see org.apache.synapse.ManagedLifecycle#destroy()
	 */
	public void destroy() {

	}

}
