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

/**
 * XJC Fluent API Extensions Plugin.
 * 
 * @author Jérôme Delagnes
 */
public final class FluentApiExtPlugin extends Plugin {

   /**
    * {@inheritDoc}
    */
   public String getOptionName() {
      return "Xfluent-api-ext";
   }

   /**
    * {@inheritDoc}
    */
   public String getUsage() {
      return " Fluent API Extensions";
   }

   /**
    * {@inheritDoc}
    */
   public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
      for (ClassOutline classOutline : outline.getClasses()) {
         for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
            switch (getFieldType(fieldOutline)) {
               case MANAGED_FIELD:
                  createWithMethod(fieldOutline);
                  break;
               case MANAGED_LIST:
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

   /** Field type. */
   private static enum FieldType {
      /** JAXB generated class. */
      MANAGED_FIELD,
      /** List of JAXB generated class. */
      MANAGED_LIST,
      /** Other */
      OTHER
   }

   /**
    * Analyzes the field and returns:
    * <ul>
    * <li>{@link FieldType#MANAGED_FIELD} if the field is a managed class (see
    * {@link #isManagedClass(JClass)}.
    * <li>{@link FieldType#MANAGED_LIST} if the field is a managed list (see
    * {@link #isManagedList(JClass)}.
    * <li>{@link FieldType#OTHER} else.
    * </ul>
    * @param fieldOutline - the field.
    * @return the type of the field.
    */
   private FieldType getFieldType(FieldOutline fieldOutline) {
      JClass jClass = fieldOutline.getRawType().boxify();
      return isManagedClass(jClass) ? FieldType.MANAGED_FIELD : isManagedList(jClass) ? FieldType.MANAGED_LIST
         : FieldType.OTHER;
   }

   /**
    * The {@link JClass} object is managed if:
    * <ul>
    * <li>it is a JAXB generated class.
    * <li>it has a constructor with no argument or no constructor.
    * </ul>
    * @param jClass - the {@link JClass} object.
    * @return true if the class is managed.
    */
   private boolean isManagedClass(JClass jClass) {
      if (jClass instanceof JDefinedClass) {
         JDefinedClass definedClass = (JDefinedClass) jClass;
         if (ClassType.CLASS.equals(definedClass.getClassType()) && !definedClass.isAbstract()) {
            // TODO Check the class is not private ?
            Iterator<JMethod> constructors = definedClass.constructors();
            if (constructors.hasNext() == false) {
               return true;
            }
            while (constructors.hasNext()) {
               JMethod constructor = constructors.next();
               if (constructor.listParams().length == 0) {
                  // TODO Check the constructor is not private ?
                  return true;
               }
            }
         }
      }
      return false;
   }

   /**
    * The {@link JClass} object is a list if:
    * <ul>
    * <li>the class is a {@link List}.
    * <li>the class implements {@link List}.
    * </ul>
    * @param jClass - the {@link JClass} to anayze.
    * @return true if the {@link JClass} object is or extends {@link List}.
    */
   private boolean isList(JClass jClass) {
      // XXX Not recursive implementation. Only check base class.
      return jClass.getBaseClass(List.class) != null;
   }

   /**
    * The {@link JClass} object is a managed list if:
    * <ul>
    * <li>the class is a list (see {@link #isList(JClass)}).
    * <li>the parameter type is a managed class (see
    * {@link #isManagedClass(JClass)}.
    * </ul>
    * @param jClass - the class object.
    * @return true if it is a managed list.
    */
   private boolean isManagedList(JClass jClass) {
      return isList(jClass) && isManagedClass(jClass.getBaseClass(List.class).getTypeParameters().get(0));
   }

   /**
    * <p>
    * Generates the <code>with&lt;property&gt;()</code> method. The generated
    * body method looks like:
    * 
    * <pre>
    * //...
    * PropertyClass property;
    * 
    * //...
    * public PropertyClass withProperty() {
    *    if (this.property == null) {
    *       this.property = new PropertyClass();
    *    }
    *    return this.property;
    * }
    * //...
    * </pre>
    * @param fieldOutline - the field outline.
    */
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

   /**
    * <p>
    * Generates the <code>with&lt;property&gt;(int i)</code> method. The
    * generated body method looks like:
    * 
    * <pre>
    * //...
    * PropertyClass property;
    * 
    * //...
    * public PropertyClass withItem(int index) {
    *    List&lt;PropertyClass&gt; list = this.getItem();
    *    if (list.size() &lt;= index) {
    *       for (int i = list.size(); (i &lt;= index); i++) {
    *          list.add(null);
    *       }
    *    }
    *    PropertyClass value = list.get(index);
    *    if (value == null) {
    *       value = new PropertyClass();
    *       list.set(index, value);
    *    }
    *    return value;
    * }
    * //...
    * </pre>
    * @param fieldOutline
    */
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

   /**
    * <p>
    * Generates the <code>withNew&lt;property&gt;()</code> method. The generated
    * body method looks like:
    * 
    * <pre>
    * //...
    * PropertyClass property;
    * 
    * //...
    * public PropertyClass withNewItem() {
    *    PropertyClass value = new PropertyClass();
    *    this.getItem().add(value);
    *    return value;
    * }
    * //...
    * </pre>
    * @param fieldOutline
    */
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
