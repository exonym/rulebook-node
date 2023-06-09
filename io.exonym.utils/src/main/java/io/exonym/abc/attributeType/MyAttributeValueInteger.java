//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package io.exonym.abc.attributeType;

import java.math.BigInteger;

import org.w3c.dom.Element;

public class MyAttributeValueInteger extends MyAttributeValue {

  private BigInteger value;
  
  public MyAttributeValueInteger(Object attributeValue,  /*IsNull*/ EnumAllowedValues allowedValues) {
    super(allowedValues);
    if(attributeValue instanceof BigInteger) {
      value = ((BigInteger)attributeValue);
    } else if(attributeValue instanceof String) {
        value = new BigInteger((String)attributeValue);
    } else if(attributeValue instanceof Integer) {
      value = BigInteger.valueOf((Integer)attributeValue);
    } else if(attributeValue instanceof Long) {
      value = BigInteger.valueOf((Long)attributeValue);
    } else if(attributeValue instanceof Element) {
      String svalue = ((Element)attributeValue).getTextContent();
      value = new BigInteger(svalue);
    } else {
      throw new RuntimeException("Cannot parse attribute value as integer " + attributeValue.getClass());
    }
  }

  @Override
  public boolean isEquals(MyAttributeValue lhs) {
    if(lhs instanceof MyAttributeValueInteger) {
      return ((MyAttributeValueInteger)lhs).value.equals(value);
    } else {
      return false;
    }
  }
  
  @Override
  public boolean isLess(MyAttributeValue lhs) {
    if(lhs instanceof MyAttributeValueInteger) {
      BigInteger lhsInt = ((MyAttributeValueInteger)lhs).value;
      return (value.compareTo(lhsInt) < 0);
    } else {
      return false;
    }
  }
  
  protected BigInteger getValue() {
    return value;
  }
  
  @Override
  public Object getValueAsObject() {
    return getValue();
  }

}
