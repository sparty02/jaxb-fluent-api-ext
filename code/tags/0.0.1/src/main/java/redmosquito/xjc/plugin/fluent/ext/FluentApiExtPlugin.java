/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package redmosquito.xjc.plugin.fluent.ext;

import java.util.Iterator;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

public final class FluentApiExtPlugin extends Plugin {

   private static enum FieldType {
      ELEMENT, LIST, OTHER
   }

   public String getOptionName() {
      return "Xfluent-api-ext";
   }

   public String getUsage() {
      return " Fluent API Extension";
   }

   public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
      for (ClassOutline classOutline : outline.getClasses()) {
         for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
            switch (getFieldType(fieldOutline)) {
               case ELEMENT:
                  createWithMethod(fieldOutline);
                  break;
               case LIST:
                  createListWithMethod(fieldOutline);
                  createListWithNewMethod(fieldOutline);
                  break;
               case OTHER:
               default:
                  break;
            }
         }
      }
      return false;
   }

   private FieldType getFieldType(FieldOutline fieldOutline) {
      JClass jClass = fieldOutline.getRawType().boxify();
      return isManagedClass(jClass) ? FieldType.ELEMENT : isManagedList(jClass) ? FieldType.LIST : FieldType.OTHER;
   }

   private boolean isManagedClass(JClass jClass) {
      if (jClass instanceof JDefinedClass) {
         JDefinedClass definedClass = (JDefinedClass) jClass;
         if (definedClass.getClassType() == ClassType.CLASS) {
            @SuppressWarnings("unchecked")
            Iterator<JMethod> constructors = definedClass.constructors();
            if (constructors.hasNext() == false) {
               return true;
            }
            while (constructors.hasNext()) {
               JMethod constructor = constructors.next();
               if (constructor.listParams().length == 0) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private boolean isList(JClass jClass) {
      return jClass.getBaseClass(List.class) != null;
   }

   private boolean isManagedList(JClass jClass) {
      return isList(jClass) && isManagedClass(jClass.getBaseClass(List.class).getTypeParameters().get(0));
   }

   protected void createWithMethod(FieldOutline fieldOutline) {
      final JDefinedClass implClass = fieldOutline.parent().implClass;
      final String fieldName = fieldOutline.getPropertyInfo().getName(false);
      final String propertyName = fieldOutline.getPropertyInfo().getName(true);

      JMethod method = implClass.method(JMod.PUBLIC, fieldOutline.getRawType(), "with" + propertyName);

      JBlock body = method.body();

      JConditional _if = body._if(JExpr.refthis(fieldName).eq(JExpr._null()));
      JBlock _then = _if._then();
      _then.assign(JExpr.ref(fieldName), JExpr._new(fieldOutline.getRawType()));

      body._return(JExpr.ref(fieldName));
   }

   protected void createListWithMethod(FieldOutline fieldOutline) {
      final JDefinedClass implClass = fieldOutline.parent().implClass;
      final String propertyName = fieldOutline.getPropertyInfo().getName(true);
      final JClass elementClass = fieldOutline.getRawType().boxify().getTypeParameters().get(0);
      final JPrimitiveType INT = fieldOutline.parent().parent().getCodeModel().INT;

      JMethod method = implClass.method(JMod.PUBLIC, elementClass, "with" + propertyName);
      JVar index = method.param(INT, "index");

      JBlock body = method.body();

      JVar list = body.decl(fieldOutline.getRawType(), "list", JExpr._this().invoke("get" + propertyName));
      JConditional _ifListIsTooSmall = body._if(list.invoke("size").lte(index));
      JBlock _ifListIsTooSmallThen = _ifListIsTooSmall._then();
      JForLoop _for = _ifListIsTooSmallThen._for();
      JVar i = _for.init(INT, "i", list.invoke("size"));
      _for.test(i.lte(index));
      _for.update(i.incr());
      _for.body().invoke(list, "add").arg(JExpr._null());

      JVar element = body.decl(elementClass, "value", list.invoke("get").arg(index));
      JConditional _ifElementIsNull = body._if(element.eq(JExpr._null()));
      JBlock _ifElementIsNullThen = _ifElementIsNull._then();
      _ifElementIsNullThen.assign(element, JExpr._new(element.type()));
      _ifElementIsNullThen.invoke(list, "set").arg(index).arg(element);

      body._return(element);
   }

   protected void createListWithNewMethod(FieldOutline fieldOutline) {
      final JDefinedClass implClass = fieldOutline.parent().implClass;
      final String propertyName = fieldOutline.getPropertyInfo().getName(true);
      final JClass elementClass = fieldOutline.getRawType().boxify().getTypeParameters().get(0);

      JMethod method = implClass.method(JMod.PUBLIC, elementClass, "withNew" + propertyName);

      JBlock body = method.body();

      JVar element = body.decl(elementClass, "value", JExpr._new(elementClass));

      body.invoke(JExpr._this().invoke("get" + propertyName), "add").arg(element);

      body._return(element);
   }
}
