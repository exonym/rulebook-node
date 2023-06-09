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

import java.net.URI;
import java.net.URISyntaxException;

import org.w3c.dom.Element;

public class MyAttributeValueUri extends MyAttributeValue {

  private URI value;
  
  public MyAttributeValueUri(Object attributeValue,  /*IsNull*/ EnumAllowedValues allowedValues) {
    super(allowedValues);
    if(attributeValue instanceof String) {
      try {
        value = new URI((String)attributeValue);
        
      } catch (URISyntaxException e) {
        throw new RuntimeException("Cannot parse attribute value as URI: " + e.getMessage());
        
      }
    } else if (attributeValue instanceof URI) {
      value = (URI) attributeValue;
      
    } else if(attributeValue instanceof Element) {
      String svalue = ((Element)attributeValue).getTextContent();
      
      try {
        value = new URI(svalue);
        
      } catch (URISyntaxException e) {
        throw new RuntimeException("Cannot parse attribute value as URI: " + e.getMessage());
        
      }
    } else {
      throw new RuntimeException("Cannot parse attribute value as URI: " + attributeValue.getClass());
      
    }
  }

  @Override
  public boolean isEquals(MyAttributeValue lhs) {
    if(lhs instanceof MyAttributeValueUri) {
      return ((MyAttributeValueUri)lhs).value.equals(value);
    } else {
      return false;
    }
  }

  @Override
  public boolean isLess(MyAttributeValue myAttributeValue) {
    throw new UnsupportedOperationException("Can't call 'less' on an URI");
  }
  
  protected URI getValue() {
    return value;
  }
  
  @Override
  public Object getValueAsObject() {
    return getValue();
  }

}
