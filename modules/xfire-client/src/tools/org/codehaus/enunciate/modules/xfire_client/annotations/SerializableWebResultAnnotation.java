/*
 * Copyright 2006 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 */

package org.codehaus.enunciate.modules.xfire_client.annotations;

import org.codehaus.xfire.annotations.WebResultAnnotation;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Ryan Heaton
 */
public class SerializableWebResultAnnotation extends WebResultAnnotation implements Serializable {
  
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.writeBoolean(isHeader());
    out.writeObject(getName());
    out.writeObject(getPartName());
    out.writeObject(getTargetNamespace());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    setHeader(in.readBoolean());
    setName((String) in.readObject());
    setPartName((String) in.readObject());
    setTargetNamespace((String) in.readObject());
  }

}