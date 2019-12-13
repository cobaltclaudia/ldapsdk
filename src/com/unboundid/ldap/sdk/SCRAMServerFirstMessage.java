/*
 * Copyright 2019 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2019 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk;



import java.io.Serializable;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.util.Base64;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;



/**
 * This class holds the elements associated with the server first message in a
 * SCRAM authentication sequence.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
final class SCRAMServerFirstMessage
      implements Serializable
{
  /**
   * The minimum allowed iteration count value.
   */
  private static final int MINIMUM_ALLOWED_ITERATION_COUNT = 4096;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 3888813341685523286L;



  // The bind result containing the server first message.
  private final BindResult bindResult;

  // The salt to use with the password.
  private final byte[] salt;

  // The iteration count.
  private final int iterationCount;

  // The bind request being processed.
  private final SCRAMBindRequest bindRequest;

  // The client first message that was sent to the server.
  private final SCRAMClientFirstMessage clientFirstMessage;

  // The string representation of the server first message included in the bind
  // result.
  private final String serverFirstMessage;

  // A string containing the client nonce concatenated with the server nonce.
  private final String combinedNonce;

  // The portion of the nonce that was generated by the server.
  private final String serverNonce;



  /**
   * Creates a new server first message with the provided information.
   *
   * @param  bindRequest         The SCRAM bind request being processed.  It
   *                             must not be {@code null}.
   * @param  clientFirstMessage  The client first message that was sent to the
   *                             server.  It must not be {@code null}.
   * @param  bindResult          The bind result from which to extract the
   *                             server first message.  It must not be
   *                             {@code null}.
   *
   * @throws  LDAPBindException  If a problem is encountered while parsing the
   *                             server first message from the provided bind
   *                             result.
   */
  SCRAMServerFirstMessage(final SCRAMBindRequest bindRequest,
                          final SCRAMClientFirstMessage clientFirstMessage,
                          final BindResult bindResult)
       throws LDAPBindException
  {
    this.bindRequest = bindRequest;
    this.clientFirstMessage = clientFirstMessage;
    this.bindResult = bindResult;


    // Make sure that the bind result included server SASL credentials.
    final ASN1OctetString serverSASLCredentials =
         bindResult.getServerSASLCredentials();
    if (serverSASLCredentials == null)
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_NO_CREDS.get(
                bindRequest.getSASLMechanismName()),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }


    // The first component of the message must be the combined nonce, which will
    // be preceded by "r=".
    serverFirstMessage = serverSASLCredentials.stringValue();
    if (!serverFirstMessage.startsWith("r="))
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_NO_NONCE.get(
                bindRequest.getSASLMechanismName(), serverFirstMessage),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }


    // The combined nonce will be followed by a base64-encoded salt, which will
    // be preceded by a comma and "s=".
    final int commaSEqualsPos = serverFirstMessage.indexOf(",s=");
    if (commaSEqualsPos < 0)
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_NO_SALT.get(
                bindRequest.getSASLMechanismName(), serverFirstMessage),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }


    // The salt will be followed by the iteration count, which will be preceded
    // by a comma and "i=".
    final int commaIEqualsPos =
         serverFirstMessage.indexOf(",i=", commaSEqualsPos);
    if (commaIEqualsPos < 0)
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_NO_ITERATION_COUNT.get(
                bindRequest.getSASLMechanismName(), serverFirstMessage),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }


    // Extract the combined nonce.  Make sure that it starts with the expected
    // client nonce, and that a server nonce was appended to it.
    combinedNonce = serverFirstMessage.substring(2, commaSEqualsPos);
    if (!combinedNonce.startsWith(clientFirstMessage.getClientNonce()))
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_NONCE_MISSING_CLIENT.get(
                bindRequest.getSASLMechanismName(), serverFirstMessage,
                combinedNonce, clientFirstMessage.getClientNonce(),
                clientFirstMessage.getClientFirstMessage()),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }

    if (combinedNonce.equals(clientFirstMessage.getClientNonce()))
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_NONCE_MISSING_SERVER.get(
                bindRequest.getSASLMechanismName(), serverFirstMessage,
                combinedNonce, clientFirstMessage.getClientFirstMessage()),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }

    serverNonce = combinedNonce.substring(
         clientFirstMessage.getClientNonce().length());


    // Extract the salt.  Make sure it can be base64 decoded.
    final String saltString = serverFirstMessage.substring(
         (commaSEqualsPos + 3), commaIEqualsPos);
    if (saltString.isEmpty())
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_SALT_EMPTY.get(
                bindRequest.getSASLMechanismName(), serverFirstMessage),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }

    try
    {
      salt = Base64.decode(saltString);
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_SALT_NOT_BASE64.get(
                bindRequest.getSASLMechanismName(), saltString,
                serverFirstMessage),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }


    // Extract the iteration count.  Make sure that it is an integer value that
    // is greater than or equal to the minimum allowed value.  Note that it's
    // possible that there may be extensions after the iteration count, and if
    // there are then we will ignore them.
    final String iterationCountString;
    final int extensionCommaPos =
         serverFirstMessage.indexOf(',', (commaIEqualsPos + 1));
    if (extensionCommaPos > 0)
    {
      iterationCountString = serverFirstMessage.substring(
           (commaIEqualsPos + 3), extensionCommaPos);
    }
    else
    {
      iterationCountString = serverFirstMessage.substring(commaIEqualsPos + 3);
    }

    try
    {
      iterationCount = Integer.parseInt(iterationCountString);
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_ITERATION_COUNT_NOT_INTEGER.get(
                bindRequest.getSASLMechanismName(), iterationCountString,
                serverFirstMessage),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }

    if (iterationCount < MINIMUM_ALLOWED_ITERATION_COUNT)
    {
      throw new LDAPBindException(new BindResult(bindResult.getMessageID(),
           ResultCode.DECODING_ERROR,
           ERR_SCRAM_SERVER_FIRST_MESSAGE_ITERATION_COUNT_BELOW_MINIMUM.get(
                bindRequest.getSASLMechanismName(), iterationCount,
                serverFirstMessage, MINIMUM_ALLOWED_ITERATION_COUNT),
           bindResult.getMatchedDN(), bindResult.getReferralURLs(),
           bindResult.getResponseControls(), serverSASLCredentials));
    }
  }



  /**
   * Retrieves the SCRAM bind request being processed.
   *
   * @return  The SCRAM bind request being processed.
   */
  SCRAMBindRequest getBindRequest()
  {
    return bindRequest;
  }



  /**
   * Retrieves the client first message with which this server first message
   * is associated.
   *
   * @return  The client first message with which this server first message is
   *          associated.
   */
  SCRAMClientFirstMessage getClientFirstMessage()
  {
    return clientFirstMessage;
  }



  /**
   * Retrieves the bind result that contained this server first message.
   *
   * @return  The bind result that contained this server first message.
   */
  BindResult getBindResult()
  {
    return bindResult;
  }



  /**
   * Retrieves the combined nonce for this server first message.
   *
   * @return  The combined nonce for this server first message.
   */
  String getCombinedNonce()
  {
    return combinedNonce;
  }



  /**
   * Retrieves the portion of the nonce that was generated by the server.
   *
   * @return  The portion of the nonce that was generated by the server.
   */
  String getServerNonce()
  {
    return serverNonce;
  }



  /**
   * Retrieves the salt for this server first message.
   *
   * @return  The salt for this server first message.
   */
  byte[] getSalt()
  {
    return salt;
  }



  /**
   * Retrieves the iteration count for this server first message.
   *
   * @return  The iteration count for this server first message.
   */
  int getIterationCount()
  {
    return iterationCount;
  }



  /**
   * Retrieves a string representation of the server first message.
   *
   * @return  A string representation of the server first message.
   */
  String getServerFirstMessage()
  {
    return serverFirstMessage;
  }



  /**
   * Retrieves a string representation of this SCRAM server first message.
   *
   * @return  A string representation of this SCRAM server first message.
   */
  @Override()
  public String toString()
  {
    return serverFirstMessage;
  }
}