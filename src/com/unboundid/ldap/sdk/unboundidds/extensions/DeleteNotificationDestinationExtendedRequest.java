/*
 * Copyright 2014-2016 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2015-2016 UnboundID Corp.
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
package com.unboundid.ldap.sdk.unboundidds.extensions;



import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.ExtendedRequest;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.Validator;

import static com.unboundid.ldap.sdk.unboundidds.extensions.ExtOpMessages.*;



/**
 * This class provides an extended request that may be used to delete a
 * notification destination.
 * <BR>
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * <BR>
 * The request has an OID of 1.3.6.1.4.1.30221.2.6.37 and a value with the
 * following encoding:
 * <BR><BR>
 * <PRE>
 *   DeleteNotificationDestinationRequest ::= SEQUENCE {
 *        notificationManagerID         OCTET STRING,
 *        notificationDestinationID     OCTET STRING }
 * </PRE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class DeleteNotificationDestinationExtendedRequest
       extends ExtendedRequest
{
  /**
   * The OID (1.3.6.1.4.1.30221.2.6.37) for the delete notification destination
   * extended request.
   */
  public static final String DELETE_NOTIFICATION_DESTINATION_REQUEST_OID =
       "1.3.6.1.4.1.30221.2.6.37";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -2644432176543980784L;



  // The notification destination ID.
  private final String destinationID;

  // The notification manager ID.
  private final String managerID;



  /**
   * Creates a new delete notification destination extended request with the
   * provided information.
   *
   * @param  managerID         The notification manager ID.  It must not be
   *                           {@code null}.
   * @param  destinationID     The notification destination ID.  It must not be
   *                           {@code null}.
   * @param  controls          The set of controls to include in the request.
   *                           It may be {@code null} or empty if no controls
   *                           are needed.
   */
  public DeleteNotificationDestinationExtendedRequest(final String managerID,
              final String destinationID, final Control... controls)
  {
    super(DELETE_NOTIFICATION_DESTINATION_REQUEST_OID,
         encodeValue(managerID, destinationID), controls);

    this.managerID = managerID;
    this.destinationID = destinationID;
  }



  /**
   * Creates a new delete notification destination extended request from the
   * provided generic extended request.
   *
   * @param  extendedRequest  The generic extended request to use to create this
   *                          delete notification destination extended request.
   *
   * @throws  LDAPException  If a problem occurs while decoding the request.
   */
  public DeleteNotificationDestinationExtendedRequest(
              final ExtendedRequest extendedRequest)
         throws LDAPException
  {
    super(extendedRequest);

    final ASN1OctetString value = extendedRequest.getValue();
    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_DEL_NOTIFICATION_DEST_REQ_DECODE_NO_VALUE.get());
    }

    try
    {
      final ASN1Element[] elements =
           ASN1Sequence.decodeAsSequence(value.getValue()).elements();
      managerID =
           ASN1OctetString.decodeAsOctetString(elements[0]).stringValue();
      destinationID =
           ASN1OctetString.decodeAsOctetString(elements[1]).stringValue();
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_DEL_NOTIFICATION_DEST_REQ_ERROR_DECODING_VALUE.get(
                StaticUtils.getExceptionMessage(e)),
           e);
    }
  }



  /**
   * Encodes the provided information into an ASN.1 octet string suitable for
   * use as the value of this extended request.
   *
   * @param  managerID         The notification manager ID.  It must not be
   *                           {@code null}.
   * @param  destinationID     The notification destination ID.  It must not be
   *                           {@code null}.
   *
   * @return  The ASN.1 octet string containing the encoded value.
   */
  private static ASN1OctetString encodeValue(final String managerID,
                      final String destinationID)
  {
    Validator.ensureNotNull(managerID);
    Validator.ensureNotNull(destinationID);

    final ASN1Sequence valueSequence = new ASN1Sequence(
         new ASN1OctetString(managerID),
         new ASN1OctetString(destinationID));
    return new ASN1OctetString(valueSequence.encode());
  }



  /**
   * Retrieves the notification manager ID.
   *
   * @return  The notification manager ID.
   */
  public String getManagerID()
  {
    return managerID;
  }



  /**
   * Retrieves the notification destination ID.
   *
   * @return  The notification destination ID.
   */
  public String getDestinationID()
  {
    return destinationID;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public DeleteNotificationDestinationExtendedRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public DeleteNotificationDestinationExtendedRequest
              duplicate(final Control[] controls)
  {
    final DeleteNotificationDestinationExtendedRequest r =
         new DeleteNotificationDestinationExtendedRequest(managerID,
              destinationID, controls);
    r.setResponseTimeoutMillis(getResponseTimeoutMillis(null));
    return r;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getExtendedRequestName()
  {
    return INFO_EXTENDED_REQUEST_NAME_DEL_NOTIFICATION_DEST.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("DeleteNotificationDestinationExtendedRequest(managerID='");
    buffer.append(managerID);
    buffer.append("', destinationID='");
    buffer.append(destinationID);
    buffer.append('\'');

    final Control[] controls = getControls();
    if (controls.length > 0)
    {
      buffer.append(", controls={");
      for (int i=0; i < controls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(controls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}
